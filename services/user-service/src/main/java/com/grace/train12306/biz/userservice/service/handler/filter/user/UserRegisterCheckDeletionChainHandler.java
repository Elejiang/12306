package com.grace.train12306.biz.userservice.service.handler.filter.user;

import com.grace.train12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.grace.train12306.biz.userservice.service.UserService;
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

    private final UserService userService;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        Integer userDeletionNum = userService.queryUserDeletionNum(requestParam.getIdType(), requestParam.getIdCard());
        if (userDeletionNum >= USER_DELETION_MAXIMUM) {
            throw new ClientException("证件号多次注销，账号已被加入黑名单");
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
