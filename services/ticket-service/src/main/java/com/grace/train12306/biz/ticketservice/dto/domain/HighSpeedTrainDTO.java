package com.grace.train12306.biz.ticketservice.dto.domain;

import lombok.Data;

/**
 * 动车实体
 */
@Data
public class HighSpeedTrainDTO {

    /**
     * 商务座数量
     */
    private Integer businessClassQuantity;

    /**
     * 商务座候选标识
     */
    private Boolean businessClassCandidate;

    /**
     * 商务座价格
     */
    private Integer businessClassPrice;

    /**
     * 一等座数量
     */
    private Integer firstClassQuantity;

    /**
     * 一等座候选标识
     */
    private Boolean firstClassCandidate;

    /**
     * 一等座价格
     */
    private Integer firstClassPrice;

    /**
     * 二等座数量
     */
    private Integer secondClassQuantity;

    /**
     * 二等座候选标识
     */
    private Boolean secondClassCandidate;

    /**
     * 二等座价格
     */
    private Integer secondClassPrice;
}
