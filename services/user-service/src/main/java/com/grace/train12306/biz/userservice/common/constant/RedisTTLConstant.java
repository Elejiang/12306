package com.grace.train12306.biz.userservice.common.constant;

import java.util.concurrent.TimeUnit;

/**
 * Redis TTL 定义常量类
 */
public final class RedisTTLConstant {
    /**
     * token 的 TTL
     */
    public static final int TOKEN_TTL = 30;

    /**
     * token 的 TTL 单位
     */
    public static final TimeUnit TOKEN_TTL_TIMEUNIT = TimeUnit.MINUTES;

    /**
     * passenger list 的 TTL
     */
    public static final int PASSENGER_LIST_TTL = 24;

    /**
     * passenger list 的 TTL 单位
     */
    public static final TimeUnit PASSENGER_LIST_TTL_TIMEUNIT = TimeUnit.HOURS;
}
