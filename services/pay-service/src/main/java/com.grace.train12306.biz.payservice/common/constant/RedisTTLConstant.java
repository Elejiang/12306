package com.grace.train12306.biz.payservice.common.constant;

import java.util.concurrent.TimeUnit;

/**
 * Redis TTL 定义常量类
 */
public final class RedisTTLConstant {
    /**
     * token 的 TTL
     */
    public static final int ORDER_PAY_RESULT_INFO_TTL = 10;

    /**
     * token 的 TTL 单位
     */
    public static final TimeUnit ORDER_PAY_RESULT_INFO_TTL_TIMEUNIT = TimeUnit.MINUTES;
}
