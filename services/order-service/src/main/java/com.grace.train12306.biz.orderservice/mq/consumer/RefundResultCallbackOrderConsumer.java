package com.grace.train12306.biz.orderservice.mq.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import com.grace.train12306.biz.orderservice.common.constant.OrderRocketMQConstant;
import com.grace.train12306.biz.orderservice.common.enums.OrderItemStatusEnum;
import com.grace.train12306.biz.orderservice.common.enums.OrderStatusEnum;
import com.grace.train12306.biz.orderservice.dao.entity.OrderItemDO;
import com.grace.train12306.biz.orderservice.dto.domain.OrderItemStatusReversalDTO;
import com.grace.train12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.grace.train12306.biz.orderservice.mq.domain.MessageWrapper;
import com.grace.train12306.biz.orderservice.mq.event.RefundResultCallbackOrderEvent;
import com.grace.train12306.biz.orderservice.service.OrderItemService;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.idempotent.annotation.Idempotent;
import com.grace.train12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import com.grace.train12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 退款结果回调订单消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_TAG_KEY,
        consumerGroup = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_ORDER_CG_KEY
)
public class RefundResultCallbackOrderConsumer implements RocketMQListener<MessageWrapper<RefundResultCallbackOrderEvent>> {

    private final OrderItemService orderItemService;

    @Idempotent(
            uniqueKeyPrefix = "train12306-order:refund_result_callback:",
            key = "#message.getKeys()+'_'+#message.hashCode()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.MQ,
            keyTimeout = 7200L
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<RefundResultCallbackOrderEvent> message) {
        RefundResultCallbackOrderEvent refundResultCallbackOrderEvent = message.getMessage();
        Integer status = refundResultCallbackOrderEvent.getRefundTypeEnum().getCode();
        String orderSn = refundResultCallbackOrderEvent.getOrderSn();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList = refundResultCallbackOrderEvent.getPartialRefundTicketDetailList();
        partialRefundTicketDetailList.forEach(partial -> {
            OrderItemDO orderItemDO = new OrderItemDO();
            BeanUtil.convert(partial, orderItemDO);
            orderItemDOList.add(orderItemDO);
        });
        OrderItemStatusReversalDTO refundOrderItemStatusReversalDTO = OrderItemStatusReversalDTO.builder()
                .orderSn(orderSn)
                .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus())
                .orderItemDOList(orderItemDOList)
                .build();
        if (status.equals(OrderStatusEnum.PARTIAL_REFUND.getStatus())) refundOrderItemStatusReversalDTO.setOrderStatus(OrderStatusEnum.PARTIAL_REFUND.getStatus());
        else if (status.equals(OrderStatusEnum.FULL_REFUND.getStatus())) refundOrderItemStatusReversalDTO.setOrderStatus(OrderStatusEnum.FULL_REFUND.getStatus());
        orderItemService.orderItemStatusReversal(refundOrderItemStatusReversalDTO);
    }
}