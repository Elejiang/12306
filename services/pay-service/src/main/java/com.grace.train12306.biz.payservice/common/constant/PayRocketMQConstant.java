package com.grace.train12306.biz.payservice.common.constant;

/**
 * RocketMQ 支付服务常量类
 */
public final class PayRocketMQConstant {

    /**
     * 支付服务相关业务 Topic Key
     */
    public static final String PAY_GLOBAL_TOPIC_KEY = "train12306_pay-service_topic${unique-name:}";

    /**
     * 支付结果回调订单 Tag Key
     */
    public static final String PAY_RESULT_CALLBACK_TAG_KEY = "train12306_pay-service_pay-result-callback_tag${unique-name:}";

    /**
     * 退款结果回调订单 Tag Key
     */
    public static final String REFUND_RESULT_CALLBACK_TAG_KEY = "train12306_pay-service_refund-result-callback_tag${unique-name:}";
}
