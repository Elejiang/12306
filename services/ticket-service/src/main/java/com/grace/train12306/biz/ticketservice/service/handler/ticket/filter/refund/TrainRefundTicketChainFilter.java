package com.grace.train12306.biz.ticketservice.service.handler.ticket.filter.refund;

import com.grace.train12306.biz.ticketservice.common.enums.TicketChainMarkEnum;
import com.grace.train12306.biz.ticketservice.dto.req.RefundTicketReqDTO;
import com.grace.train12306.framework.starter.designpattern.chain.AbstractChainHandler;

/**
 * 列车车票退款过滤器
 */
public interface TrainRefundTicketChainFilter<T extends RefundTicketReqDTO> extends AbstractChainHandler<RefundTicketReqDTO> {

    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_REFUND_TICKET_FILTER.name();
    }
}
