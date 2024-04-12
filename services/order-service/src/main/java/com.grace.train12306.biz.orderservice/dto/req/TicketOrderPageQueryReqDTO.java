package com.grace.train12306.biz.orderservice.dto.req;

import com.grace.train12306.framework.starter.convention.page.PageRequest;
import lombok.Data;

/**
 * 车票订单分页查询
 */
@Data
public class TicketOrderPageQueryReqDTO extends PageRequest {

    /**
     * 用户唯一标识
     */
    private String userId;

    /**
     * 状态类型 0：未完成 1：未出行 2：历史订单
     */
    private Integer statusType;
}
