package com.grace.train12306.biz.ticketservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 列车站点查询响应参数
 */
@Data
public class TrainStationQueryRespDTO {

    /**
     * 站序
     */
    private String sequence;

    /**
     * 站名
     */
    private String departure;

    /**
     * 到站时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date arrivalTime;

    /**
     * 出发时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date departureTime;

    /**
     * 停留时间
     */
    private Integer stopoverTime;
}
