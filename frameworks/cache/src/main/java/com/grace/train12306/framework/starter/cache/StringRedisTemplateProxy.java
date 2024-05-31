package com.grace.train12306.framework.starter.cache;

import com.alibaba.fastjson2.JSON;
import com.grace.train12306.framework.starter.cache.core.CacheLoader;
import com.grace.train12306.framework.starter.cache.toolkit.CacheUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存之操作 Redis 模版代理
 */
@RequiredArgsConstructor
public class StringRedisTemplateProxy implements DistributedCache {

    private static final String SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX = "safe_get_distributed_lock_get:";
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    @Override
    public <T> T get(String key, Class<T> clazz) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (String.class.isAssignableFrom(clazz)) {
            return (T) value;
        }
        return JSON.parseObject(value, clazz);
    }

    @Override
    public Object getHash(String key, Object hashKey) {
        return stringRedisTemplate.opsForHash().get(key, hashKey);
    }

    @Override
    public void putHash(String key, Object hashKey, Object value) {
        stringRedisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    public Boolean putHashIfAbsent(String key, Object hashKey, Object value) {
        return stringRedisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
    }

    @Override
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    @Override
    public void put(String key, Object value, long timeout, TimeUnit timeUnit) {
        String actual = value instanceof String ? (String) value : JSON.toJSONString(value);
        stringRedisTemplate.opsForValue().set(key, actual, timeout, timeUnit);
    }

    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public Object getInstance() {
        return stringRedisTemplate;
    }


    @Override
    public Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return stringRedisTemplate.expire(key, timeout, timeUnit);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit) {
        T result = get(key, clazz);
        // 缓存结果不等于空或空字符串直接返回
        if (!CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        RLock lock = redissonClient.getLock(SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX + key);
        lock.lock();
        try {
            // 双重判定锁
            if (CacheUtil.isNullOrBlank(result = get(key, clazz))) {
                // 如果访问 cacheLoader 加载数据为空，执行后置函数操作
                result = loadAndSet(key, cacheLoader, timeout, timeUnit);
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    private <T> T loadAndSet(String key, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit) {
        T result = cacheLoader.load();
        if (CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        put(key, result, timeout, timeUnit);
        return result;
    }
}
