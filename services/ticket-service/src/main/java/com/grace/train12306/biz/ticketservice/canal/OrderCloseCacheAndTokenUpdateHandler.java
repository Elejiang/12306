package com.grace.train12306.biz.ticketservice.canal;

import cn.hutool.core.collection.CollUtil;
import com.grace.train12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.grace.train12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import com.grace.train12306.biz.ticketservice.remote.OrderRemoteService;
import com.grace.train12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.grace.train12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.grace.train12306.biz.ticketservice.service.SeatService;
import com.grace.train12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.grace.train12306.biz.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.convention.result.Result;
import com.grace.train12306.framework.starter.designpattern.strategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单关闭或取消后置处理组件，解锁车票，回滚令牌
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseCacheAndTokenUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent, Void> {

    private final OrderRemoteService orderRemoteService;
    private final SeatService seatService;
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;

    @Override
    public void execute(CanalBinlogEvent message) {
        List<Map<String, Object>> messageDataList = message.getData().stream()
                .filter(each -> each.get("status") != null)
                // order status 30 代表取消的订单
                .filter(each -> Objects.equals(each.get("status"), "30"))
                .toList();
        if (CollUtil.isEmpty(messageDataList)) {
            return;
        }
        for (Map<String, Object> each : messageDataList) {
            // 得到订单详细
            Result<TicketOrderDetailRespDTO> orderDetailResult = orderRemoteService.queryTicketOrderByOrderSn(each.get("order_sn").toString());
            TicketOrderDetailRespDTO orderDetailResultData = orderDetailResult.getData();
            if (orderDetailResult.isSuccess() && orderDetailResultData != null) {
                String trainId = String.valueOf(orderDetailResultData.getTrainId());
                List<TicketOrderPassengerDetailRespDTO> passengerDetails = orderDetailResultData.getPassengerDetails();
                // 解锁座位
                seatService.unlock(trainId, orderDetailResultData.getDeparture(), orderDetailResultData.getArrival(), BeanUtil.convert(passengerDetails, TrainPurchaseTicketRespDTO.class));
                // 回滚令牌和余票
                ticketAvailabilityTokenBucket.rollbackInBucket(orderDetailResultData);
            }
        }
    }

    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getActualTable();
    }

    @Override
    public String patternMatchMark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getPatternMatchTable();
    }
}
