package com.grace.train12306.biz.ticketservice.service.handler.ticket.filter.purchase;

import com.grace.train12306.biz.ticketservice.common.enums.TicketChainMarkEnum;
import com.grace.train12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.grace.train12306.framework.starter.designpattern.chain.AbstractChainHandler;

/**
 * 列车购买车票过滤器
 */
public interface TrainPurchaseTicketChainFilter<T extends PurchaseTicketReqDTO> extends AbstractChainHandler<PurchaseTicketReqDTO> {

    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name();
    }
}
