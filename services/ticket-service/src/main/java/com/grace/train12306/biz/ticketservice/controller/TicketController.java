package com.grace.train12306.biz.ticketservice.controller;

import com.grace.train12306.biz.ticketservice.dto.req.*;
import com.grace.train12306.biz.ticketservice.dto.resp.RefundTicketRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.grace.train12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.grace.train12306.biz.ticketservice.service.SeatService;
import com.grace.train12306.biz.ticketservice.service.TicketService;
import com.grace.train12306.framework.starter.convention.result.Result;
import com.grace.train12306.framework.starter.idempotent.annotation.Idempotent;
import com.grace.train12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import com.grace.train12306.framework.starter.log.annotation.ILog;
import com.grace.train12306.framework.starter.web.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 车票控制层
 */
@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final SeatService seatService;

    /**
     * 根据条件查询车票
     */
    @ILog
    @GetMapping("/api/ticket-service/ticket/query")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(ticketService.pageListTicketQuery(requestParam));
    }

    /**
     * 购买车票
     */
    @ILog
    @Idempotent(
            uniqueKeyPrefix = "train12306-ticket:lock_purchase-tickets:",
            key = "T(com.grace.train12306.framework.starter.bases.ApplicationContextHolder).getBean('environment').getProperty('unique-name', '')"
                    + "+'_'+"
                    + "T(com.grace.train12306.framework.starter.user.core.UserContext).getUsername()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI
    )
    @PostMapping("/api/ticket-service/ticket/purchase")
    public Result<TicketPurchaseRespDTO> purchaseTickets(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTickets(requestParam));
    }

    /**
     * 取消车票订单
     */
    @PostMapping("/api/ticket-service/ticket/cancel")
    public Result<Void> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        ticketService.cancelTicketOrder(requestParam);
        return Results.success();
    }

    /**
     * 公共退款接口
     */
    @PostMapping("/api/ticket-service/ticket/refund")
    public Result<RefundTicketRespDTO> commonTicketRefund(@RequestBody RefundTicketReqDTO requestParam) {
        return Results.success(ticketService.commonTicketRefund(requestParam));
    }

    /**
     * 更改座位状态为已售
     */
    @PostMapping("/api/ticket-service/ticket/update")
    public Result<Void> updateTicketStatusSold(@RequestBody TicketOrderDetailRespDTO requestParam) {
        seatService.updateTicketStatusSold(requestParam);
        return Results.success();
    }
}
