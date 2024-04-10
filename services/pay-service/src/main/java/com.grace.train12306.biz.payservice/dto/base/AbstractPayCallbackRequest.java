package com.grace.train12306.biz.payservice.dto.base;

import lombok.Getter;
import lombok.Setter;

/**
 * 抽象支付回调入参实体
 */
public abstract class AbstractPayCallbackRequest implements PayCallbackRequest {

    @Getter
    @Setter
    private String orderRequestId;

    @Override
    public AliPayCallbackRequest getAliPayCallBackRequest() {
        return null;
    }

    @Override
    public String buildMark() {
        return null;
    }
}
