package com.grace.train12306.biz.userservice.service.handler.filter.impl;

import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.PhoneUtil;
import com.grace.train12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum;
import com.grace.train12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.grace.train12306.biz.userservice.service.handler.filter.UserRegisterCreateChainFilter;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 用户注册参数是否合法
 */
@Component
public final class UserRegisterParamValidateChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        int length = requestParam.getRealName().length();
        if (!(length >= 2 && length <= 16)) {
            throw new ClientException("用户真实姓名请设置2-16位的长度");
        }
        if (!IdcardUtil.isValidCard(requestParam.getIdCard())) {
            throw new ClientException("用户证件号无效");
        }
        if (!PhoneUtil.isMobile(requestParam.getPhone())) {
            throw new ClientException("用户手机号无效");
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
