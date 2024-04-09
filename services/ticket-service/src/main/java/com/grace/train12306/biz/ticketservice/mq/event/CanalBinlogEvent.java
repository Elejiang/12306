package com.grace.train12306.biz.ticketservice.mq.event;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Canal Binlog 监听触发时间
 */
@Data
public class CanalBinlogEvent {

    /**
     * 变更数据
     */
    private List<Map<String, Object>> data;

    /**
     * 数据库名称
     */
    private String database;

    /**
     * es 是指 Mysql Binlog 里原始的时间戳，也就是数据原始变更的时间
     * Canal 的消费延迟 = ts - es
     */
    private Long es;

    /**
     * 递增 ID，从 1 开始
     */
    private Long id;

    /**
     * 当前变更是否是 DDL 语句
     */
    private Boolean isDdl;

    /**
     * 表结构字段类型
     */
    private Map<String, Object> mysqlType;

    /**
     * UPDATE 模式下旧数据
     */
    private List<Map<String, Object>> old;

    /**
     * 主键名称
     */
    private List<String> pkNames;

    /**
     * SQL 语句
     */
    private String sql;

    /**
     * SQL 类型
     */
    private Map<String, Object> sqlType;

    /**
     * 表名
     */
    private String table;

    /**
     * ts 是指 Canal 收到这个 Binlog，构造为自己协议对象的时间
     * 应用消费的延迟 = now - ts
     */
    private Long ts;

    /**
     * INSERT（新增）、UPDATE（更新）、DELETE（删除）等等
     */
    private String type;
}
