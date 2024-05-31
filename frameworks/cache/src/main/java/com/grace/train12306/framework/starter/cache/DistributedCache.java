package com.grace.train12306.framework.starter.cache;

import com.grace.train12306.framework.starter.cache.core.CacheLoader;
import jakarta.validation.constraints.NotBlank;
import org.redisson.api.RBloomFilter;

import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存
 */
public interface DistributedCache {

    /**
     * 放入缓存，自定义超时时间
     */
    void put(@NotBlank String key, Object value, long timeout, TimeUnit timeUnit);

    /**
     * 获取缓存
     */
    <T> T get(@NotBlank String key, Class<T> clazz);

    /**
     * 获取哈希类型缓存
     */
    Object getHash(@NotBlank String key, Object hashKey);

    /**
     * 放入哈希类型缓存
     */
    void putHash(@NotBlank String key, Object hashKey, Object value);

    /**
     * 放入哈希类型缓存
     */
    Boolean putHashIfAbsent(@NotBlank String key, Object hashKey, Object value);

    /**
     * 删除缓存
     */
    Boolean delete(@NotBlank String key);

    /**
     * 判断 key 是否存在
     */
    Boolean hasKey(@NotBlank String key);

    /**
     * 设置过期时间
     */
    Boolean expire(String key, long timeout, TimeUnit timeUnit);

    /**
     * 获取缓存组件实例
     */
    Object getInstance();

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存击穿、缓存雪崩场景，适用于不被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit);

}
