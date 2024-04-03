package com.grace.train12306.framework.starter.cache;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 缓存接口
 */
public interface Cache {

    /**
     * 获取缓存
     */
    <T> T get(@NotBlank String key, Class<T> clazz);

    /**
     * 获取哈希类型缓存
     */
    Object get(@NotBlank String key, Object hashKey);

    /**
     * 放入缓存
     */
    void put(@NotBlank String key, Object value);

    /**
     * 放入哈希类型缓存
     */
    void putHash(@NotBlank String key, Object hashKey, Object value);

    /**
     * 放入哈希类型缓存
     */
    Boolean putHashIfAbsent(@NotBlank String key, Object hashKey, Object value);

    /**
     * 如果 keys 全部不存在，则新增，返回 true，反之 false
     */
    Boolean putIfAllAbsent(@NotNull Collection<String> keys);

    /**
     * 删除缓存
     */
    Boolean delete(@NotBlank String key);

    /**
     * 删除 keys，返回删除数量
     */
    Long delete(@NotNull Collection<String> keys);

    /**
     * 判断 key 是否存在
     */
    Boolean hasKey(@NotBlank String key);

    Boolean expire(String key, long timeout, TimeUnit timeUnit);

    /**
     * 获取缓存组件实例
     */
    Object getInstance();
}
