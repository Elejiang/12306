package com.grace.train12306.biz.orderservice.dao.algorithm;

import cn.hutool.core.collection.CollUtil;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.apache.shardingsphere.sharding.exception.algorithm.sharding.ShardingAlgorithmInitializationException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

/**
 * 订单数据库复合分片算法配置
 */
public class OrderCommonDataBaseComplexAlgorithm implements ComplexKeysShardingAlgorithm {
    private static final String SHARDING_COUNT_KEY = "sharding-count";
    private static final String TABLE_SHARDING_COUNT_KEY = "table-sharding-count";

    private int shardingCount;
    private int tableShardingCount;

    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {
        // 得到列名和值
        Map<String, Collection<Comparable<Long>>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());
        if (CollUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            // 首先判断SQL是否包含用户ID，如果包含直接取用户ID后六位
            Collection<Comparable<Long>> userIdCollection = columnNameAndShardingValuesMap.get("user_id");
            if (CollUtil.isNotEmpty(userIdCollection)) {
                // 获取到 SQL 中包含的用户ID对应值
                addResult(result, userIdCollection);
            } else {
                // 如果不包含用户ID，那么就要从订单号中获取后六位，也就是用户ID后六位
                // 流程同用户ID获取流程
                Collection<Comparable<Long>> orderSnCollection = columnNameAndShardingValuesMap.get("order_sn");
                addResult(result, orderSnCollection);
            }
        }
        // 返回的是库名
        return result;
    }

    private void addResult(Collection<String> result, Collection<Comparable<Long>> collection) {
        Comparable<?> comparable = collection.stream().findFirst().get();
        if (comparable instanceof String) {
            String actualString = comparable.toString();
            result.add("ds_" + hashShardingValue(actualString.substring(Math.max(actualString.length() - 6, 0))) % shardingCount / tableShardingCount);
        } else {
            result.add("ds_" + hashShardingValue((Long) comparable % 1000000) % shardingCount / tableShardingCount);
        }
    }

    @Override
    public void init(Properties props) {
        shardingCount = getShardingCount(props);
        tableShardingCount = getTableShardingCount(props);
    }

    private int getShardingCount(final Properties props) {
        ShardingSpherePreconditions.checkState(props.containsKey(SHARDING_COUNT_KEY), () -> new ShardingAlgorithmInitializationException(getType(), "Sharding count cannot be null."));
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    private int getTableShardingCount(final Properties props) {
        ShardingSpherePreconditions.checkState(props.containsKey(TABLE_SHARDING_COUNT_KEY), () -> new ShardingAlgorithmInitializationException(getType(), "Table sharding count cannot be null."));
        return Integer.parseInt(props.getProperty(TABLE_SHARDING_COUNT_KEY));
    }

    private long hashShardingValue(final Comparable<?> shardingValue) {
        return Math.abs((long) shardingValue.hashCode());
    }

    @Override
    public String getType() {
        return "CLASS_BASED";
    }
}
