package com.grace.train12306.framework.starter.idempotent.core;

import com.grace.train12306.framework.starter.idempotent.annotation.Idempotent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 幂等参数包装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public final class IdempotentParamWrapper {

    /**
     * 幂等注解
     */
    private Idempotent idempotent;

    /**
     * AOP 处理连接点
     */
    private ProceedingJoinPoint joinPoint;

    /**
     * 锁标识，用于RestAPI场景
     */
    private String lockKey;

    /**
     * 消费id，由消费者持有，消费完成后根据消费id判断是否是自己消费的该条消息
     */
    private String consumptionId;
}
