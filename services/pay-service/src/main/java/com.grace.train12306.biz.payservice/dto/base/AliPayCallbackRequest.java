package com.grace.train12306.biz.payservice.dto.base;

import com.grace.train12306.biz.payservice.common.enums.PayChannelEnum;
import lombok.Data;

import java.util.Date;

/**
 * 支付宝回调请求入参
 */
@Data
public final class AliPayCallbackRequest extends AbstractPayCallbackRequest {

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 支付状态
     */
    private String tradeStatus;

    /**
     * 支付凭证号
     */
    private String tradeNo;

    /**
     * 买家付款时间
     */
    private Date gmtPayment;

    /**
     * 买家付款金额
     */
    private Integer buyerPayAmount;

    @Override
    public AliPayCallbackRequest getAliPayCallBackRequest() {
        return this;
    }

    @Override
    public String buildMark() {
        return PayChannelEnum.ALI_PAY.getName();
    }
}
