package com.grace.train12306.biz.ticketservice.service;


import com.grace.train12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.grace.train12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.grace.train12306.biz.ticketservice.dto.req.RefundTicketReqDTO;
import com.grace.train12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.RefundTicketRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.grace.train12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 车票接口
 */
public interface TicketService {

    /**
     * 根据条件分页查询车票
     *
     * @param requestParam 分页查询车票请求参数
     * @return 查询车票返回结果
     */
    TicketPageQueryRespDTO pageListTicketQuery(TicketPageQueryReqDTO requestParam);

    /**
     * 购买车票
     *
     * @param requestParam 车票购买请求参数
     * @return 订单号
     */
    TicketPurchaseRespDTO purchaseTickets(@RequestBody PurchaseTicketReqDTO requestParam);

    /**
     * 支付单详情查询
     *
     * @param orderSn 订单号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfo(String orderSn);

    /**
     * 取消车票订单
     *
     * @param requestParam 取消车票订单入参
     */
    void cancelTicketOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 公共退款接口
     *
     * @param requestParam 退款请求参数
     * @return 退款返回详情
     */
    RefundTicketRespDTO commonTicketRefund(RefundTicketReqDTO requestParam);
}
