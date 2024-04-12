package com.grace.train12306.framework.starter.idempotent.config;

import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.idempotent.core.IdempotentAspect;
import com.grace.train12306.framework.starter.idempotent.core.handler.IdempotentByMQExecuteHandler;
import com.grace.train12306.framework.starter.idempotent.core.handler.IdempotentByRestAPIExecuteHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 幂等自动装配
 */
public class IdempotentAutoConfiguration {

    /**
     * 幂等切面
     */
    @Bean
    public IdempotentAspect idempotentAspect() {
        return new IdempotentAspect();
    }

    /**
     * 幂等实现，基于 RestAPI 场景
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentByRestAPIExecuteHandler idempotentSpELByRestAPIExecuteHandler(RedissonClient redissonClient) {
        return new IdempotentByRestAPIExecuteHandler(redissonClient);
    }

    /**
     * 幂等实现，基于 MQ 场景
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentByMQExecuteHandler idempotentSpELByMQExecuteHandler(DistributedCache distributedCache) {
        return new IdempotentByMQExecuteHandler(distributedCache);
    }
}
