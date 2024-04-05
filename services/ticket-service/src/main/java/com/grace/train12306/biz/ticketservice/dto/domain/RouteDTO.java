package com.grace.train12306.biz.ticketservice.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站点路线实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteDTO {

    /**
     * 出发站点
     */
    private String startStation;

    /**
     * 目的站点
     */
    private String endStation;
}
