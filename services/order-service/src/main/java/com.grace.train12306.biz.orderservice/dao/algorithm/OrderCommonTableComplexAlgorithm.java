package com.grace.train12306.biz.orderservice.dao.algorithm;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

/**
 * 订单表相关复合分片算法配置
 */
public class OrderCommonTableComplexAlgorithm implements ComplexKeysShardingAlgorithm {

    @Getter
    private Properties props;

    private int shardingCount;

    private static final String SHARDING_COUNT_KEY = "sharding-count";

    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {
        Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());
        if (CollUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            Collection<Comparable<?>> customerUserIdCollection = columnNameAndShardingValuesMap.get("user_id");
            if (CollUtil.isNotEmpty(customerUserIdCollection)) {
                getUserId(shardingValue, result, customerUserIdCollection);
            } else {
                Collection<Comparable<?>> orderSnCollection = columnNameAndShardingValuesMap.get("order_sn");
                getUserId(shardingValue, result, orderSnCollection);
            }
        }
        return result;
    }

    private void getUserId(ComplexKeysShardingValue shardingValue, Collection<String> result, Collection<Comparable<?>> customerUserIdCollection) {
        Comparable<?> comparable = customerUserIdCollection.stream().findFirst().get();
        if (comparable instanceof String) {
            String actualUserId = comparable.toString();
            result.add(shardingValue.getLogicTableName() + "_" + hashShardingValue(actualUserId.substring(Math.max(actualUserId.length() - 6, 0))) % shardingCount);
        } else {
            result.add(shardingValue.getLogicTableName() + "_" + hashShardingValue((Long) comparable % 1000000) % shardingCount);
        }
    }

    @Override
    public void init(Properties props) {
        this.props = props;
        shardingCount = getShardingCount(props);
    }

    private int getShardingCount(final Properties props) {
        Preconditions.checkArgument(props.containsKey(SHARDING_COUNT_KEY), "Sharding count cannot be null.");
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    private long hashShardingValue(final Comparable<?> shardingValue) {
        return Math.abs((long) shardingValue.hashCode());
    }
}
