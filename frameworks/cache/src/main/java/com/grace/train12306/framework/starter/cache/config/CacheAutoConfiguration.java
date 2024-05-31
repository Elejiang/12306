package com.grace.train12306.framework.starter.cache.config;

import com.grace.train12306.framework.starter.cache.RedisKeySerializer;
import com.grace.train12306.framework.starter.cache.StringRedisTemplateProxy;
import lombok.AllArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存配置自动装配
 */
@AllArgsConstructor
@EnableConfigurationProperties({RedisDistributedProperties.class})
public class CacheAutoConfiguration {

    private final RedisDistributedProperties redisDistributedProperties;

    /**
     * 创建 Redis Key 序列化器，可自定义 Key Prefix
     */
    @Bean
    public RedisKeySerializer redisKeySerializer() {
        String prefix = redisDistributedProperties.getPrefix();
        String prefixCharset = redisDistributedProperties.getPrefixCharset();
        return new RedisKeySerializer(prefix, prefixCharset);
    }

    @Bean
    // 静态代理模式: Redis 客户端代理类增强
    public StringRedisTemplateProxy stringRedisTemplateProxy(RedisKeySerializer redisKeySerializer,
                                                             StringRedisTemplate stringRedisTemplate,
                                                             RedissonClient redissonClient) {
        stringRedisTemplate.setKeySerializer(redisKeySerializer);
        return new StringRedisTemplateProxy(stringRedisTemplate, redissonClient);
    }
}
