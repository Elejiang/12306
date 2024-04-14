package com.grace.train12306.biz.orderservice.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.orderservice.common.enums.OrderCancelErrorCodeEnum;
import com.grace.train12306.biz.orderservice.common.enums.OrderItemStatusEnum;
import com.grace.train12306.biz.orderservice.common.enums.OrderStatusEnum;
import com.grace.train12306.biz.orderservice.dao.entity.OrderDO;
import com.grace.train12306.biz.orderservice.dao.entity.OrderItemDO;
import com.grace.train12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import com.grace.train12306.biz.orderservice.dao.mapper.OrderItemMapper;
import com.grace.train12306.biz.orderservice.dao.mapper.OrderMapper;
import com.grace.train12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import com.grace.train12306.biz.orderservice.dto.req.*;
import com.grace.train12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.grace.train12306.biz.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.grace.train12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.grace.train12306.biz.orderservice.mq.event.DelayCloseOrderEvent;
import com.grace.train12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.grace.train12306.biz.orderservice.mq.produce.DelayCloseOrderSendProduce;
import com.grace.train12306.biz.orderservice.remote.UserRemoteService;
import com.grace.train12306.biz.orderservice.remote.dto.UserQueryActualRespDTO;
import com.grace.train12306.biz.orderservice.service.OrderItemService;
import com.grace.train12306.biz.orderservice.service.OrderPassengerRelationService;
import com.grace.train12306.biz.orderservice.service.OrderService;
import com.grace.train12306.biz.orderservice.service.orderid.OrderIdGeneratorManager;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import com.grace.train12306.framework.starter.convention.page.PageResponse;
import com.grace.train12306.framework.starter.convention.result.Result;
import com.grace.train12306.framework.starter.database.toolkit.PageUtil;
import com.grace.train12306.framework.starter.user.core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.grace.train12306.biz.orderservice.common.constant.RedisKeyConstant.ORDER_CANCEL;
import static com.grace.train12306.biz.orderservice.common.constant.RedisKeyConstant.ORDER_STATUS_REVERSAL;

/**
 * 订单服务接口层实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderItemService orderItemService;
    private final OrderPassengerRelationService orderPassengerRelationService;
    private final RedissonClient redissonClient;
    private final DelayCloseOrderSendProduce delayCloseOrderSendProduce;
    private final UserRemoteService userRemoteService;

    @Override
    public TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        TicketOrderDetailRespDTO result = BeanUtil.convert(orderDO, TicketOrderDetailRespDTO.class);
        LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, orderSn);
        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
        result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
        return result;
    }

    @Override
    public PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, requestParam.getUserId())
                .in(OrderDO::getStatus, buildOrderStatusList(requestParam))
                .orderByDesc(OrderDO::getOrderTime);
        IPage<OrderDO> orderPage = orderMapper.selectPage(PageUtil.convert(requestParam), queryWrapper);
        return PageUtil.convert(orderPage, each -> {
            TicketOrderDetailRespDTO result = BeanUtil.convert(each, TicketOrderDetailRespDTO.class);
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn());
            List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
            result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
            return result;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String createTicketOrder(TicketOrderCreateReqDTO requestParam) {
        // 插入主订单
        String orderSn = insertOrder(requestParam);
        // 添加子订单和乘车人订单
        insertOrderItem(requestParam, orderSn);
        // 发送 RocketMQ 延时消息，指定时间后取消订单
        sendMessage(requestParam, orderSn);
        return orderSn;
    }

    @Override
    public boolean closeTicketOrder(CancelTicketOrderReqDTO requestParam) {
        return cancelTicketOrder(requestParam);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {
        String orderSn = requestParam.getOrderSn();
        // 对订单进行检验，如订单是否存在、订单状态
        checkOrder(requestParam.getOrderSn());
        // 上分布式锁
        RLock lock = redissonClient.getLock(ORDER_CANCEL + orderSn);
        if (!lock.tryLock()) {
            throw new ClientException(OrderCancelErrorCodeEnum.ORDER_CANCEL_REPETITION_ERROR);
        }
        try {
            // 更新主订单状态
            updateOrderStatus(orderSn, OrderStatusEnum.CLOSED.getStatus());
            // 更新子订单状态
            updateOrderItemStatus(orderSn, OrderItemStatusEnum.CLOSED.getStatus(), null);
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void statusReversal(OrderStatusReversalDTO requestParam) {
        // 对订单进行检验，如订单是否存在、订单状态
        checkOrder(requestParam.getOrderSn());
        // 上分布式锁
        RLock lock = redissonClient.getLock(ORDER_STATUS_REVERSAL + requestParam.getOrderSn());
        if (!lock.tryLock()) {
            log.warn("订单重复修改状态，状态反转请求参数：{}", JSON.toJSONString(requestParam));
        }
        try {
            // 更新主订单状态
            updateOrderStatus(requestParam.getOrderSn(), requestParam.getOrderStatus());
            // 更新子订单状态
            updateOrderItemStatus(requestParam.getOrderSn(), requestParam.getOrderStatus(), requestParam.getOrderItemDOList());
        } finally {
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void payCallbackOrder(PayResultCallbackOrderEvent requestParam) {
        // 修改支付时间
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setPayTime(requestParam.getGmtPayment());
        updateOrderDO.setPayType(requestParam.getChannel());
        LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
        if (updateResult <= 0) {
            throw new ServiceException(OrderCancelErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }

    @Override
    public PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        // 远程调用获取个人信息
        Result<UserQueryActualRespDTO> userActualResp = userRemoteService.queryActualUserByUsername(UserContext.getUsername());
        // 根据idcard（分片键）查询本人车票
        LambdaQueryWrapper<OrderItemPassengerDO> queryWrapper = Wrappers.lambdaQuery(OrderItemPassengerDO.class)
                .eq(OrderItemPassengerDO::getIdCard, userActualResp.getData().getIdCard())
                .orderByDesc(OrderItemPassengerDO::getCreateTime);
        IPage<OrderItemPassengerDO> orderItemPassengerPage = orderPassengerRelationService.page(PageUtil.convert(requestParam), queryWrapper);
        return PageUtil.convert(orderItemPassengerPage, each -> {
            LambdaQueryWrapper<OrderDO> orderQueryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                    .eq(OrderDO::getOrderSn, each.getOrderSn());
            OrderDO orderDO = orderMapper.selectOne(orderQueryWrapper);
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn())
                    .eq(OrderItemDO::getIdCard, each.getIdCard());
            OrderItemDO orderItemDO = orderItemMapper.selectOne(orderItemQueryWrapper);
            TicketOrderDetailSelfRespDTO actualResult = BeanUtil.convert(orderDO, TicketOrderDetailSelfRespDTO.class);
            BeanUtil.convertIgnoreNullAndBlank(orderItemDO, actualResult);
            return actualResult;
        });
    }

    private List<Integer> buildOrderStatusList(TicketOrderPageQueryReqDTO requestParam) {
        List<Integer> result = new ArrayList<>();
        switch (requestParam.getStatusType()) {
            case 0 -> result = ListUtil.of(
                    OrderStatusEnum.PENDING_PAYMENT.getStatus()
            );
            case 1 -> result = ListUtil.of(
                    OrderStatusEnum.ALREADY_PAID.getStatus(),
                    OrderStatusEnum.PARTIAL_REFUND.getStatus(),
                    OrderStatusEnum.FULL_REFUND.getStatus()
            );
            case 2 -> result = ListUtil.of(
                    OrderStatusEnum.COMPLETED.getStatus()
            );
        }
        return result;
    }

    private String insertOrder(TicketOrderCreateReqDTO requestParam) {
        String orderSn = OrderIdGeneratorManager.generateId(requestParam.getUserId());
        OrderDO orderDO = OrderDO.builder().orderSn(orderSn)
                .orderTime(requestParam.getOrderTime())
                .departure(requestParam.getDeparture())
                .departureTime(requestParam.getDepartureTime())
                .ridingDate(requestParam.getRidingDate())
                .arrivalTime(requestParam.getArrivalTime())
                .trainNumber(requestParam.getTrainNumber())
                .arrival(requestParam.getArrival())
                .trainId(requestParam.getTrainId())
                .source(requestParam.getSource())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .username(requestParam.getUsername())
                .userId(String.valueOf(requestParam.getUserId()))
                .build();
        orderMapper.insert(orderDO);
        return orderSn;
    }

    private void insertOrderItem(TicketOrderCreateReqDTO requestParam, String orderSn) {
        List<TicketOrderItemCreateReqDTO> ticketOrderItems = requestParam.getTicketOrderItems();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        List<OrderItemPassengerDO> orderPassengerRelationDOList = new ArrayList<>();
        ticketOrderItems.forEach(each -> {
            OrderItemDO orderItemDO = OrderItemDO.builder()
                    .trainId(requestParam.getTrainId())
                    .seatNumber(each.getSeatNumber())
                    .carriageNumber(each.getCarriageNumber())
                    .realName(each.getRealName())
                    .orderSn(orderSn)
                    .phone(each.getPhone())
                    .seatType(each.getSeatType())
                    .username(requestParam.getUsername()).amount(each.getAmount()).carriageNumber(each.getCarriageNumber())
                    .idCard(each.getIdCard())
                    .ticketType(each.getTicketType())
                    .idType(each.getIdType())
                    .userId(String.valueOf(requestParam.getUserId()))
                    .status(0)
                    .build();
            orderItemDOList.add(orderItemDO);
            OrderItemPassengerDO orderPassengerRelationDO = OrderItemPassengerDO.builder()
                    .idType(each.getIdType())
                    .idCard(each.getIdCard())
                    .orderSn(orderSn)
                    .build();
            orderPassengerRelationDOList.add(orderPassengerRelationDO);
        });
        orderItemService.saveBatch(orderItemDOList);
        orderPassengerRelationService.saveBatch(orderPassengerRelationDOList);
    }

    private void sendMessage(TicketOrderCreateReqDTO requestParam, String orderSn) {
        try {
            DelayCloseOrderEvent delayCloseOrderEvent = DelayCloseOrderEvent.builder()
                    .trainId(String.valueOf(requestParam.getTrainId()))
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderSn(orderSn)
                    .trainPurchaseTicketResults(requestParam.getTicketOrderItems())
                    .build();
            SendResult sendResult = delayCloseOrderSendProduce.sendMessage(delayCloseOrderEvent);
            if (!Objects.equals(sendResult.getSendStatus(), SendStatus.SEND_OK)) {
                throw new ServiceException("投递延迟关闭订单消息队列失败");
            }
        } catch (Throwable ex) {
            log.error("延迟关闭订单消息队列发送错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
            throw ex;
        }
    }

    private void checkOrder(String orderSn) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        if (orderDO == null) {
            throw new ServiceException(OrderCancelErrorCodeEnum.ORDER_CANCEL_UNKNOWN_ERROR);
        } else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCancelErrorCodeEnum.ORDER_CANCEL_STATUS_ERROR);
        }
    }

    private void updateOrderStatus(String orderSn, Integer orderStatus) {
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setStatus(orderStatus);
        LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
        if (updateResult <= 0) {
            throw new ServiceException(OrderCancelErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }

    private void updateOrderItemStatus(String orderSn, Integer orderStatus, List<OrderItemDO> orderItemDOList) {
        OrderItemDO orderItemDO = new OrderItemDO();
        orderItemDO.setStatus(orderStatus);
        LambdaUpdateWrapper<OrderItemDO> orderItemUpdateWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, orderSn);
        if (CollectionUtil.isNotEmpty(orderItemDOList)) {
            orderItemUpdateWrapper.in(OrderItemDO::getUserId, orderItemDOList.stream().map(OrderItemDO::getUserId).toList());
        }
        int orderItemUpdateResult = orderItemMapper.update(orderItemDO, orderItemUpdateWrapper);
        if (orderItemUpdateResult <= 0) {
            throw new ServiceException(OrderCancelErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }
}
