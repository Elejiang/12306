package com.grace.train12306.framework.starter.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存穿透布隆过滤器
 */
@Data
@ConfigurationProperties(prefix = BloomFilterPenetrateProperties.PREFIX)
public class BloomFilterPenetrateProperties {

    public static final String PREFIX = "framework.cache.redis.bloom-filter.default";

    /**
     * 布隆过滤器默认实例名称
     */
    private String name = "cache_penetration_bloom_filter";

    /**
     * 每个元素的预期插入量
     */
    private Long expectedInsertions = 64L;

    /**
     * 预期错误概率
     */
    private Double falseProbability = 0.03D;
}
