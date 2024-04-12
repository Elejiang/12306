package com.grace.train12306.framework.starter.idempotent.core;

import com.grace.train12306.framework.starter.bases.ApplicationContextHolder;
import com.grace.train12306.framework.starter.idempotent.core.handler.IdempotentByMQExecuteHandler;
import com.grace.train12306.framework.starter.idempotent.core.handler.IdempotentByRestAPIExecuteHandler;
import com.grace.train12306.framework.starter.idempotent.enums.IdempotentSceneEnum;

/**
 * 幂等执行处理器工厂
 */
public final class IdempotentExecuteHandlerFactory {

    /**
     * 获取幂等执行处理器
     *
     * @param scene 指定幂等验证场景类型
     * @return 幂等执行处理器
     */
    public static IdempotentExecuteHandler getInstance(IdempotentSceneEnum scene) {
        IdempotentExecuteHandler result = null;
        switch (scene) {
            case RESTAPI -> result = ApplicationContextHolder.getBean(IdempotentByRestAPIExecuteHandler.class);
            case MQ -> result = ApplicationContextHolder.getBean(IdempotentByMQExecuteHandler.class);
            default -> {
            }
        }
        return result;
    }
}
