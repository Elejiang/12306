package com.grace.train12306.biz.ticketservice.dto.domain;

import lombok.Data;

import java.util.List;

/**
 * 车次集合实体
 */
@Data
public class TicketListDTO {

    /**
     * 列车 ID
     */
    private String trainId;

    /**
     * 车次
     */
    private String trainNumber;

    /**
     * 出发时间
     */
    private String departureTime;

    /**
     * 到达时间
     */
    private String arrivalTime;

    /**
     * 历时
     */
    private String duration;

    /**
     * 到达天数
     */
    private Integer daysArrived;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 始发站标识
     */
    private Boolean departureFlag;

    /**
     * 终点站标识
     */
    private Boolean arrivalFlag;

    /**
     * 列车类型 0：高铁 1：动车 2：普通车
     */
    private Integer trainType;

    /**
     * 可售时间
     */
    private String saleTime;

    /**
     * 销售状态 0：可售 1：不可售 2：未知
     */
    private Integer saleStatus;

    /**
     * 列车标签集合 0：复兴号 1：智能动车组 2：静音车厢 3：支持选铺
     */
    private List<String> trainTags;

    /**
     * 列车品牌类型 0：GC-高铁/城际 1：D-动车 2：Z-直达 3：T-特快 4：K-快速 5：其他 6：复兴号 7：智能动车组
     */
    private String trainBrand;

    /**
     * 席别实体集合
     */
    private List<SeatClassDTO> seatClassList;
}
