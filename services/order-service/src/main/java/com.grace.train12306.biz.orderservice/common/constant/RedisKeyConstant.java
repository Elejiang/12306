package com.grace.train12306.biz.orderservice.common.constant;

/**
 * Redis Key 定义常量类
 */
public final class RedisKeyConstant {

    /**
     * 订单状态反转
     */
    public static final String ORDER_STATUS_REVERSAL = "order:status-reversal:order_sn_";

    /**
     * 订单取消
     */
    public static final String ORDER_CANCEL = "order:cancel:order_sn_";

}
