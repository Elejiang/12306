package com.grace.train12306.framework.starter.idempotent.enums;

/**
 * 幂等验证类型枚举
 */
public enum IdempotentTypeEnum {
    
    /**
     * 基于方法参数方式验证，一般用于接口幂等
     */
    PARAM,
    
    /**
     * 基于 SpEL 表达式方式验证，接口幂等与MQ幂等均可使用
     */
    SPEL
}
