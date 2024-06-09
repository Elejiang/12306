package com.grace.train12306.framework.starter.distributedid.toolkit;

import com.grace.train12306.framework.starter.distributedid.core.snowflake.Snowflake;

/**
 * 分布式雪花 ID 生成器
 */
public final class SnowflakeIdUtil {

    /**
     * 雪花算法对象
     */
    private static Snowflake SNOWFLAKE;

    /**
     * 初始化雪花算法
     */
    public static void initSnowflake(Snowflake snowflake) {
        SnowflakeIdUtil.SNOWFLAKE = snowflake;
    }

    /**
     * 获取雪花算法下一个 ID
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 获取雪花算法下一个字符串类型 ID
     */
    public static String nextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }
}
