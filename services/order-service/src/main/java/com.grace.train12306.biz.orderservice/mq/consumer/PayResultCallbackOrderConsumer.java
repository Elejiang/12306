package com.grace.train12306.biz.orderservice.mq.consumer;

import com.grace.train12306.biz.orderservice.common.constant.OrderRocketMQConstant;
import com.grace.train12306.biz.orderservice.common.enums.OrderItemStatusEnum;
import com.grace.train12306.biz.orderservice.common.enums.OrderStatusEnum;
import com.grace.train12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import com.grace.train12306.biz.orderservice.mq.domain.MessageWrapper;
import com.grace.train12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.grace.train12306.biz.orderservice.remote.TicketRemoteService;
import com.grace.train12306.biz.orderservice.service.OrderService;
import com.grace.train12306.framework.starter.idempotent.annotation.Idempotent;
import com.grace.train12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付结果回调订单消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression = OrderRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY,
        consumerGroup = OrderRocketMQConstant.PAY_RESULT_CALLBACK_ORDER_CG_KEY
)
public class PayResultCallbackOrderConsumer implements RocketMQListener<MessageWrapper<PayResultCallbackOrderEvent>> {

    private final OrderService orderService;
    private final TicketRemoteService ticketRemoteService;

    @Idempotent(
            uniqueKeyPrefix = "train12306-order:pay_result_callback:",
            key = "#message.getKeys()+'_'+#message.hashCode()",
            scene = IdempotentSceneEnum.MQ,
            keyTimeout = 7200L
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<PayResultCallbackOrderEvent> message) {
        PayResultCallbackOrderEvent payResultCallbackOrderEvent = message.getMessage();
        OrderStatusReversalDTO orderStatusReversalDTO = OrderStatusReversalDTO.builder()
                .orderSn(payResultCallbackOrderEvent.getOrderSn())
                .orderStatus(OrderStatusEnum.ALREADY_PAID.getStatus())
                .orderItemStatus(OrderItemStatusEnum.ALREADY_PAID.getStatus())
                .build();
        orderService.statusReversal(orderStatusReversalDTO);
        orderService.payCallbackOrder(payResultCallbackOrderEvent);
        ticketRemoteService.updateTicketStatusSold(orderService.queryTicketOrderByOrderSn(payResultCallbackOrderEvent.getOrderSn()));
    }
}
