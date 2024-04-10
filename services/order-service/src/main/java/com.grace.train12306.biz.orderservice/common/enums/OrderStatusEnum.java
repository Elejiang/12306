package com.grace.train12306.biz.orderservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatusEnum {

    /**
     * 待支付：用户选好车票下单，但还未付款的状态
     */
    PENDING_PAYMENT(0),

    /**
     * 已支付：用户支付订单费用
     */
    ALREADY_PAID(10),

    /**
     * 部分退款：用户支付订单费用后部分车票退款
     */
    PARTIAL_REFUND(11),

    /**
     * 全部退款：用户支付订单费用后全部车票退款
     */
    FULL_REFUND(12),

    /**
     * 已完成：用户车票已过上站时间，订单完成
     */
    COMPLETED(20),

    /**
     * 已取消：用户选好车票下单，未支付状态下取消订单
     */
    CLOSED(30);

    private final int status;
}
