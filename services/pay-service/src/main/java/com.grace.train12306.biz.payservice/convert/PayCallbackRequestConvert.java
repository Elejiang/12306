package com.grace.train12306.biz.payservice.convert;

import com.grace.train12306.biz.payservice.common.enums.PayChannelEnum;
import com.grace.train12306.biz.payservice.dto.PayCallbackCommand;
import com.grace.train12306.biz.payservice.dto.base.AliPayCallbackRequest;
import com.grace.train12306.biz.payservice.dto.base.PayCallbackRequest;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;

import java.util.Objects;

/**
 * 支付回调请求入参转换器
 */
public final class PayCallbackRequestConvert {

    /**
     * {@link PayCallbackCommand} to {@link PayCallbackRequest}
     *
     * @param payCallbackCommand 支付回调请求参数
     * @return {@link PayCallbackRequest}
     */
    public static PayCallbackRequest command2PayCallbackRequest(PayCallbackCommand payCallbackCommand) {
        PayCallbackRequest payCallbackRequest = null;
        if (Objects.equals(payCallbackCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            payCallbackRequest = BeanUtil.convert(payCallbackCommand, AliPayCallbackRequest.class);
            ((AliPayCallbackRequest) payCallbackRequest).setOrderRequestId(payCallbackCommand.getOrderRequestId());
        }
        return payCallbackRequest;
    }
}
