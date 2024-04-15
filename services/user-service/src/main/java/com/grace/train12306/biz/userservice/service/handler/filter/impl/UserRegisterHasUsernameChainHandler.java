package com.grace.train12306.biz.userservice.service.handler.filter.impl;

import com.grace.train12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum;
import com.grace.train12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.grace.train12306.biz.userservice.service.handler.filter.UserRegisterCreateChainFilter;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.grace.train12306.biz.userservice.common.constant.RedisKeyConstant.USER_REGISTER_REUSE_SHARDING;
import static com.grace.train12306.biz.userservice.toolkit.UserReuseUtil.hashShardingIdx;

/**
 * 用户注册用户名唯一检验
 */
@Component
@RequiredArgsConstructor
public final class UserRegisterHasUsernameChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final DistributedCache distributedCache;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        String username = requestParam.getUsername();
        if (userRegisterCachePenetrationBloomFilter.contains(username)) {
            // 布隆过滤器存在该用户名
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            if (Boolean.FALSE.equals(instance.opsForSet().isMember(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username)))
                throw new ClientException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL);
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
