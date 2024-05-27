package com.grace.train12306.biz.userservice.service;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.userservice.common.enums.UserChainMarkEnum;
import com.grace.train12306.biz.userservice.dao.entity.*;
import com.grace.train12306.biz.userservice.dao.mapper.*;
import com.grace.train12306.biz.userservice.dto.req.UserDeletionReqDTO;
import com.grace.train12306.biz.userservice.dto.req.UserLoginReqDTO;
import com.grace.train12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.grace.train12306.biz.userservice.dto.resp.UserLoginRespDTO;
import com.grace.train12306.biz.userservice.dto.resp.UserQueryRespDTO;
import com.grace.train12306.biz.userservice.dto.resp.UserRegisterRespDTO;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import com.grace.train12306.framework.starter.designpattern.chain.AbstractChainContext;
import com.grace.train12306.framework.starter.user.core.UserContext;
import com.grace.train12306.framework.starter.user.core.UserInfoDTO;
import com.grace.train12306.framework.starter.user.toolkit.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

import static com.grace.train12306.biz.userservice.common.constant.RedisKeyConstant.*;
import static com.grace.train12306.biz.userservice.common.constant.RedisTTLConstant.TOKEN_TTL;
import static com.grace.train12306.biz.userservice.common.constant.RedisTTLConstant.TOKEN_TTL_TIMEUNIT;
import static com.grace.train12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum.*;
import static com.grace.train12306.biz.userservice.toolkit.UserReuseUtil.hashShardingIdx;


/**
 * 用户登录接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserReuseMapper userReuseMapper;
    private final UserDeletionMapper userDeletionMapper;
    private final UserPhoneMapper userPhoneMapper;
    private final UserMailMapper userMailMapper;
    private final RedissonClient redissonClient;
    private final DistributedCache distributedCache;
    private final AbstractChainContext<UserRegisterReqDTO> abstractChainContext;
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        String username = getUsername(requestParam);
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getPassword, requestParam.getPassword())
                .select(UserDO::getId, UserDO::getUsername, UserDO::getRealName);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ServiceException("账号不存在或密码错误");
        }
        UserInfoDTO userInfo = UserInfoDTO.builder()
                .userId(String.valueOf(userDO.getId()))
                .username(userDO.getUsername())
                .realName(userDO.getRealName())
                .build();
        String accessToken = JWTUtil.generateAccessToken(userInfo);
        UserLoginRespDTO actual = new UserLoginRespDTO(userInfo.getUserId(), requestParam.getUsernameOrMailOrPhone(), userDO.getRealName(), accessToken);
        distributedCache.put(accessToken, JSON.toJSONString(actual), TOKEN_TTL, TOKEN_TTL_TIMEUNIT);
        return actual;
    }

    public UserLoginRespDTO checkLogin(String accessToken) {
        return distributedCache.get(accessToken, UserLoginRespDTO.class);
    }

    public void logout(String accessToken) {
        if (StrUtil.isNotBlank(accessToken)) {
            distributedCache.delete(accessToken);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public UserRegisterRespDTO register(UserRegisterReqDTO requestParam) {
        abstractChainContext.handler(UserChainMarkEnum.USER_REGISTER_FILTER.name(), requestParam);
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER + requestParam.getUsername());
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ServiceException(HAS_USERNAME_NOTNULL);
        }
        try {
            insertUser(requestParam);
            insertPhone(requestParam);
            insertMail(requestParam);
            usernameUsed(requestParam);
        } finally {
            lock.unlock();
        }
        return BeanUtil.convert(requestParam, UserRegisterRespDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletion(UserDeletionReqDTO requestParam) {
        String username = UserContext.getUsername();
        if (!Objects.equals(username, requestParam.getUsername())) {
            log.error("注销账号{}与登录账号{}不一致", requestParam.getUsername(), username);
            throw new ClientException("注销账号与登录账号不一致");
        }
        RLock lock = redissonClient.getLock(USER_DELETION + requestParam.getUsername());
        lock.lock();
        try {
            UserQueryRespDTO userQueryRespDTO = userService.queryUserByUsername(username);
            insertUserDeletion(userQueryRespDTO);
            deleteUser(username);
            deleteUserPhone(userQueryRespDTO);
            deleteUserMail(userQueryRespDTO);
            UsernameReused(username);
        } finally {
            lock.unlock();
        }
    }

    private boolean isMail(String string) {
        for (char c : string.toCharArray()) {
            if (c == '@') {
                return true;
            }
        }
        return false;
    }

    private String getUsername(UserLoginReqDTO requestParam) {
        String usernameOrMailOrPhone = requestParam.getUsernameOrMailOrPhone();
        String username = null;
        if (isMail(usernameOrMailOrPhone)) {
            // 使用邮箱登录
            LambdaQueryWrapper<UserMailDO> queryWrapper = Wrappers.lambdaQuery(UserMailDO.class)
                    .eq(UserMailDO::getMail, usernameOrMailOrPhone);
            username = Optional.ofNullable(userMailMapper.selectOne(queryWrapper))
                    .map(UserMailDO::getUsername)
                    .orElseThrow(() -> new ClientException("用户名/手机号/邮箱不存在"));
        } else if (PhoneUtil.isMobile(usernameOrMailOrPhone)){
            // 因为分不清手机号和用户名，如果符合手机号正则，先尝试手机号查询
            LambdaQueryWrapper<UserPhoneDO> queryWrapper = Wrappers.lambdaQuery(UserPhoneDO.class)
                    .eq(UserPhoneDO::getPhone, usernameOrMailOrPhone);
            username = Optional.ofNullable(userPhoneMapper.selectOne(queryWrapper))
                    .map(UserPhoneDO::getUsername)
                    .orElse(null);
        }
        return Optional.ofNullable(username).orElse(requestParam.getUsernameOrMailOrPhone());
    }

    private void insertUser(UserRegisterReqDTO requestParam) {
        try {
            userMapper.insert(BeanUtil.convert(requestParam, UserDO.class));
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(HAS_USERNAME_NOTNULL);
        }
    }

    private void insertPhone(UserRegisterReqDTO requestParam) {
        UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                .phone(requestParam.getPhone())
                .username(requestParam.getUsername())
                .build();
        try {
            userPhoneMapper.insert(userPhoneDO);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(PHONE_REGISTERED);
        }
    }

    private void insertMail(UserRegisterReqDTO requestParam) {
        if (StrUtil.isNotBlank(requestParam.getMail())) {
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(requestParam.getMail())
                    .username(requestParam.getUsername())
                    .build();
            try {
                userMailMapper.insert(userMailDO);
            } catch (DuplicateKeyException dke) {
                throw new ServiceException(MAIL_REGISTERED);
            }
        }
    }

    private void usernameUsed(UserRegisterReqDTO requestParam) {
        String username = requestParam.getUsername();
        userReuseMapper.delete(Wrappers.update(new UserReuseDO(username)));
        StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
        instance.opsForSet().remove(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
        userRegisterCachePenetrationBloomFilter.add(username);
    }

    private void UsernameReused(String username) {
        distributedCache.delete(UserContext.getToken());
        userReuseMapper.insert(new UserReuseDO(username));
        StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
        instance.opsForSet().add(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
    }

    private void deleteUserMail(UserQueryRespDTO userQueryRespDTO) {
        if (StrUtil.isNotBlank(userQueryRespDTO.getMail())) {
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(userQueryRespDTO.getMail())
                    .deletionTime(System.currentTimeMillis())
                    .build();
            userMailMapper.deletionUser(userMailDO);
        }
    }

    private void deleteUserPhone(UserQueryRespDTO userQueryRespDTO) {
        UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                .phone(userQueryRespDTO.getPhone())
                .deletionTime(System.currentTimeMillis())
                .build();
        userPhoneMapper.deletionUser(userPhoneDO);
    }

    private void deleteUser(String username) {
        UserDO userDO = new UserDO();
        userDO.setDeletionTime(System.currentTimeMillis());
        userDO.setUsername(username);
        userMapper.deletionUser(userDO);
    }

    private void insertUserDeletion(UserQueryRespDTO userQueryRespDTO) {
        UserDeletionDO userDeletionDO = UserDeletionDO.builder()
                .idType(userQueryRespDTO.getIdType())
                .idCard(userQueryRespDTO.getIdCard())
                .build();
        userDeletionMapper.insert(userDeletionDO);
    }
}
