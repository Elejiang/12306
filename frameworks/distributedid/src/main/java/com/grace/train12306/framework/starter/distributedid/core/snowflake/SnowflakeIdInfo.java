package com.grace.train12306.framework.starter.distributedid.core.snowflake;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 雪花算法组成部分，通常用来反解析使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnowflakeIdInfo {

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 工作机器节点 ID
     */
    private Integer workerId;

    /**
     * 数据中心 ID
     */
    private Integer dataCenterId;

    /**
     * 自增序号，当高频模式下时，同一毫秒内生成 N 个 ID，则这个序号在同一毫秒下，自增以避免 ID 重复
     */
    private Integer sequence;

    /**
     * 通过基因法生成的序号，会和 {@link SnowflakeIdInfo#sequence} 共占 12 bit
     */
    private Integer gene;
}
