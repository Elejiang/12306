package com.grace.train12306.biz.ticketservice.dto.domain;

import lombok.Data;

/**
 * 普通车实体
 */
@Data
public class RegularTrainDTO {

    /**
     * 软卧数量
     */
    private Integer softSleeperQuantity;

    /**
     * 软卧候选标识
     */
    private Boolean softSleeperCandidate;

    /**
     * 软卧价格
     */
    private Integer softSleeperPrice;

    /**
     * 高级软卧数量
     */
    private Integer deluxeSoftSleeperQuantity;

    /**
     * 高级软卧候选标识
     */
    private Boolean deluxeSoftSleeperCandidate;

    /**
     * 高级软卧价格
     */
    private Integer deluxeSoftSleeperPrice;

    /**
     * 硬卧数量
     */
    private Integer hardSleeperQuantity;

    /**
     * 硬卧候选标识
     */
    private Boolean hardSleeperCandidate;

    /**
     * 硬卧价格
     */
    private Integer hardSleeperPrice;

    /**
     * 硬座数量
     */
    private Integer hardSeatQuantity;

    /**
     * 硬座候选标识
     */
    private Boolean hardSeatCandidate;

    /**
     * 硬座价格
     */
    private Integer hardSeatPrice;

    /**
     * 无座数量
     */
    private Integer noSeatQuantity;

    /**
     * 无座候选标识
     */
    private Boolean noSeatCandidate;

    /**
     * 无座价格
     */
    private Integer noSeatPrice;
}
