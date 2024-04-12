package com.grace.train12306.biz.payservice.mq.event;

import com.grace.train12306.biz.payservice.common.enums.RefundTypeEnum;
import com.grace.train12306.biz.payservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 退款结果回调订单服务事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class RefundResultCallbackOrderEvent {
    /**
     * 订单号
     */
    private String orderSn;
    /**
     * 退款类型
     */
    private RefundTypeEnum refundTypeEnum;

    /**
     * 部分退款车票详情
     */
    private List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList;
}
