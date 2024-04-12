package com.grace.train12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.grace.train12306.framework.starter.database.base.BaseDO;
import lombok.Data;

import java.util.Date;

/**
 * 列车实体
 */
@Data
@TableName("t_train")
public class TrainDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 列车车次
     */
    private String trainNumber;

    /**
     * 列车类型 0：高铁 1：动车 2：普通车
     */
    private Integer trainType;

    /**
     * 列车标签 0：复兴号 1：智能动车组 2：静音车厢 3：支持选铺
     */
    private String trainTag;

    /**
     * 列车品牌类型 0：GC-高铁/城际 1：D-动车 2：Z-直达 3：T-特快 4：K-快速 5：其他 6：复兴号 7：智能动车组
     */
    private String trainBrand;

    /**
     * 起始站
     */
    private String startStation;

    /**
     * 终点站
     */
    private String endStation;

    /**
     * 起始城市
     */
    private String startRegion;

    /**
     * 终点城市
     */
    private String endRegion;

    /**
     * 销售时间
     */
    private Date saleTime;

    /**
     * 销售状态 0：可售 1：不可售 2：未知
     */
    private Integer saleStatus;

    /**
     * 出发时间
     */
    private Date departureTime;

    /**
     * 到达时间
     */
    private Date arrivalTime;
}
