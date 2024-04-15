package com.grace.train12306.biz.userservice.service.handler.filter.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.userservice.dao.entity.UserDeletionDO;
import com.grace.train12306.biz.userservice.dao.mapper.UserDeletionMapper;
import com.grace.train12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.grace.train12306.biz.userservice.service.handler.filter.UserRegisterCreateChainFilter;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.grace.train12306.biz.userservice.common.constant.Train12306Constant.USER_DELETION_MAXIMUM;

/**
 * 用户注册检查证件号是否多次注销
 */
@Component
@RequiredArgsConstructor
public final class UserRegisterCheckDeletionChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    private final UserDeletionMapper userDeletionMapper;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        LambdaQueryWrapper<UserDeletionDO> queryWrapper = Wrappers.lambdaQuery(UserDeletionDO.class)
                .eq(UserDeletionDO::getIdType, requestParam.getIdType())
                .eq(UserDeletionDO::getIdCard, requestParam.getIdCard());
        Long deletionCount = userDeletionMapper.selectCount(queryWrapper);
        if (deletionCount >= USER_DELETION_MAXIMUM) {
            throw new ClientException("证件号多次注销，账号已被加入黑名单");
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
