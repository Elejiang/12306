package com.grace.train12306.framework.starter.idempotent.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * MQ 消费key枚举
 */
@RequiredArgsConstructor
public enum IdempotentMQConsumeKeyEnum {
    /**
     * 消息消费的状态
     */
    STATUS("status"),

    /**
     * 消息被消费的id，由消费者持有
     */
    CONSUMPTION_ID("consumptionId");

    @Getter
    private final String key;
}
