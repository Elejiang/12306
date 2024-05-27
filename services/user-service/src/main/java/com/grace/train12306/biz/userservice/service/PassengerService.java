package com.grace.train12306.biz.userservice.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.PhoneUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.userservice.common.enums.VerifyStatusEnum;
import com.grace.train12306.biz.userservice.dao.entity.PassengerDO;
import com.grace.train12306.biz.userservice.dao.mapper.PassengerMapper;
import com.grace.train12306.biz.userservice.dto.req.PassengerRemoveReqDTO;
import com.grace.train12306.biz.userservice.dto.req.PassengerReqDTO;
import com.grace.train12306.biz.userservice.dto.resp.PassengerActualRespDTO;
import com.grace.train12306.biz.userservice.dto.resp.PassengerRespDTO;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import com.grace.train12306.framework.starter.user.core.UserContext;
import io.jsonwebtoken.lang.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.grace.train12306.biz.userservice.common.constant.RedisKeyConstant.USER_PASSENGER_LIST;
import static com.grace.train12306.biz.userservice.common.constant.RedisTTLConstant.PASSENGER_LIST_TTL;
import static com.grace.train12306.biz.userservice.common.constant.RedisTTLConstant.PASSENGER_LIST_TTL_TIMEUNIT;

/**
 * 乘车人接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerMapper passengerMapper;
    private final DistributedCache distributedCache;

    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        List<PassengerDO> passengerDOList = getPassengerDOList(username);
        if (Collections.isEmpty(passengerDOList)) {
            return null;
        }
        return passengerDOList
                .stream().map(each -> BeanUtil.convert(each, PassengerRespDTO.class))
                .collect(Collectors.toList());
    }

    public List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        List<PassengerDO> passengerDOList = getPassengerDOList(username);
        if (Collections.isEmpty(passengerDOList)) {
            return null;
        }
        return passengerDOList
                .stream().filter(passengerDO -> ids.contains(passengerDO.getId()))
                .map(each -> BeanUtil.convert(each, PassengerActualRespDTO.class))
                .collect(Collectors.toList());
    }

    public void savePassenger(PassengerReqDTO requestParam) {
        verifyPassenger(requestParam);
        String username = UserContext.getUsername();
        PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
        passengerDO.setUsername(username);
        passengerDO.setCreateDate(new Date());
        passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());
        int inserted = passengerMapper.insert(passengerDO);
        if (inserted < 1) {
            throw new ServiceException(String.format("[%s] 新增乘车人失败", username));
        }
        savePassengerCache(passengerDO);
    }

    public void updatePassenger(PassengerReqDTO requestParam) {
        verifyPassenger(requestParam);
        String username = UserContext.getUsername();
        PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
        passengerDO.setUsername(username);
        LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .eq(PassengerDO::getId, requestParam.getId());
        int updated = passengerMapper.update(passengerDO, updateWrapper);
        if (updated < 1) {
            throw new ServiceException(String.format("[%s] 修改乘车人失败", username));
        }
        updatePassengerCache(passengerDO);
    }

    public void removePassenger(PassengerRemoveReqDTO requestParam) {
        String username = UserContext.getUsername();
        LambdaUpdateWrapper<PassengerDO> deleteWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .eq(PassengerDO::getId, requestParam.getId());
        int deleted = passengerMapper.delete(deleteWrapper);
        if (deleted < 1) {
            throw new ServiceException(String.format("[%s] 删除乘车人失败", username));
        }
        removePassengerCache(Long.valueOf(requestParam.getId()));
    }

    private void savePassengerCache(PassengerDO passengerDO) {
        List<PassengerDO> passengerDOList = getPassengerDOList(passengerDO.getUsername());
        passengerDOList.add(passengerDO);
        distributedCache.put(USER_PASSENGER_LIST + passengerDO.getUsername(),
                JSON.toJSONString(passengerDOList),
                PASSENGER_LIST_TTL,
                PASSENGER_LIST_TTL_TIMEUNIT);
    }

    private void updatePassengerCache(PassengerDO passengerDO) {
        List<PassengerDO> passengerDOList = getPassengerDOList(passengerDO.getUsername());
        PassengerDO oldPassengerDO = passengerDOList.stream().filter(each -> Objects.equals(each.getId(), passengerDO.getId())).toList().get(0);
        passengerDOList.remove(oldPassengerDO);
        passengerDOList.add(passengerDO);
        distributedCache.put(USER_PASSENGER_LIST + passengerDO.getUsername(),
                JSON.toJSONString(passengerDOList),
                PASSENGER_LIST_TTL,
                PASSENGER_LIST_TTL_TIMEUNIT);
    }

    private void removePassengerCache(Long id) {
        List<PassengerDO> passengerDOList = getPassengerDOList(UserContext.getUsername());
        PassengerDO oldPassengerDO = passengerDOList.stream().filter(each -> Objects.equals(each.getId(), id)).toList().get(0);
        passengerDOList.remove(oldPassengerDO);
        distributedCache.put(USER_PASSENGER_LIST + UserContext.getUsername(),
                JSON.toJSONString(passengerDOList),
                PASSENGER_LIST_TTL,
                PASSENGER_LIST_TTL_TIMEUNIT);
    }

    private List<PassengerDO> getPassengerDOList(String username) {
        return JSON.parseArray(getPassengerDOListStr(username), PassengerDO.class);
    }


    private String getPassengerDOListStr(String username) {
        return distributedCache.safeGet(
                USER_PASSENGER_LIST + username,
                String.class,
                () -> {
                    LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername, username);
                    List<PassengerDO> passengerDOList = passengerMapper.selectList(queryWrapper);
                    return CollUtil.isNotEmpty(passengerDOList) ? JSON.toJSONString(passengerDOList) : null;
                },
                PASSENGER_LIST_TTL,
                PASSENGER_LIST_TTL_TIMEUNIT
        );
    }

    private void verifyPassenger(PassengerReqDTO requestParam) {
        int length = requestParam.getRealName().length();
        if (!(length >= 2 && length <= 16)) {
            throw new ClientException("乘车人名称请设置2-16位的长度");
        }
        if (!IdcardUtil.isValidCard(requestParam.getIdCard())) {
            throw new ClientException("乘车人证件号错误");
        }
        if (!PhoneUtil.isMobile(requestParam.getPhone())) {
            throw new ClientException("乘车人手机号错误");
        }
    }
}
