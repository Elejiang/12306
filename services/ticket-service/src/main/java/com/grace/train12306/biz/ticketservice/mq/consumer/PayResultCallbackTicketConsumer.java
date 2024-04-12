package com.grace.train12306.biz.ticketservice.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.ticketservice.common.constant.TicketRocketMQConstant;
import com.grace.train12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.grace.train12306.biz.ticketservice.dao.entity.SeatDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.grace.train12306.biz.ticketservice.mq.domain.MessageWrapper;
import com.grace.train12306.biz.ticketservice.mq.event.PayResultCallbackTicketEvent;
import com.grace.train12306.biz.ticketservice.remote.TicketOrderRemoteService;
import com.grace.train12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.grace.train12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import com.grace.train12306.framework.starter.convention.result.Result;
import com.grace.train12306.framework.starter.idempotent.annotation.Idempotent;
import com.grace.train12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 支付结果回调购票消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression = TicketRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY,
        consumerGroup = TicketRocketMQConstant.PAY_RESULT_CALLBACK_TICKET_CG_KEY
)
public class PayResultCallbackTicketConsumer implements RocketMQListener<MessageWrapper<PayResultCallbackTicketEvent>> {

    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final SeatMapper seatMapper;

    @Idempotent(
            uniqueKeyPrefix = "train12306-ticket:pay_result_callback:",
            key = "#message.getKeys()+'_'+#message.hashCode()",
            scene = IdempotentSceneEnum.MQ,
            keyTimeout = 7200L
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<PayResultCallbackTicketEvent> message) {
        Result<TicketOrderDetailRespDTO> ticketOrderDetailResult;
        try {
            ticketOrderDetailResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(message.getMessage().getOrderSn());
            if (!ticketOrderDetailResult.isSuccess() && Objects.isNull(ticketOrderDetailResult.getData())) {
                throw new ServiceException("支付结果回调查询订单失败");
            }
        } catch (Throwable ex) {
            log.error("支付结果回调查询订单失败", ex);
            throw ex;
        }
        TicketOrderDetailRespDTO ticketOrderDetail = ticketOrderDetailResult.getData();
        for (TicketOrderPassengerDetailRespDTO each : ticketOrderDetail.getPassengerDetails()) {
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, ticketOrderDetail.getTrainId())
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber())
                    .eq(SeatDO::getSeatType, each.getSeatType())
                    .eq(SeatDO::getStartStation, ticketOrderDetail.getDeparture())
                    .eq(SeatDO::getEndStation, ticketOrderDetail.getArrival());
            SeatDO updateSeatDO = new SeatDO();
            updateSeatDO.setSeatStatus(SeatStatusEnum.SOLD.getCode());
            seatMapper.update(updateSeatDO, updateWrapper);
        }
    }
}
