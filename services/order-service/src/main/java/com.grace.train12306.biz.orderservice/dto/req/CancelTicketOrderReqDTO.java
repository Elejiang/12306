package com.grace.train12306.biz.orderservice.dto.req;

import lombok.Data;

/**
 * 取消车票订单请求入参
 */
@Data
public class CancelTicketOrderReqDTO {

    /**
     * 订单号
     */
    private String orderSn;
}
