package com.grace.train12306.framework.starter.idempotent.enums;

/**
 * 幂等验证场景枚举
 */
public enum IdempotentSceneEnum {
    
    /**
     * 基于 RestAPI 场景验证
     */
    RESTAPI,
    
    /**
     * 基于 MQ 场景验证
     */
    MQ
}
