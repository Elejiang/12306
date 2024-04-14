package com.grace.train12306.biz.orderservice.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.grace.train12306.biz.orderservice.common.enums.OrderCancelErrorCodeEnum;
import com.grace.train12306.biz.orderservice.dao.entity.OrderDO;
import com.grace.train12306.biz.orderservice.dao.entity.OrderItemDO;
import com.grace.train12306.biz.orderservice.dao.mapper.OrderItemMapper;
import com.grace.train12306.biz.orderservice.dao.mapper.OrderMapper;
import com.grace.train12306.biz.orderservice.dto.domain.OrderItemStatusReversalDTO;
import com.grace.train12306.biz.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import com.grace.train12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.grace.train12306.biz.orderservice.service.OrderItemService;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.grace.train12306.biz.orderservice.common.constant.RedisKeyConstant.ORDER_STATUS_REVERSAL;

/**
 * 订单明细接口层实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItemDO> implements OrderItemService {

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public void orderItemStatusReversal(OrderItemStatusReversalDTO requestParam) {
        // 检查订单是否存在
        checkOrderExist(requestParam.getOrderSn());
        // 上分布式锁
        RLock lock = redissonClient.getLock(ORDER_STATUS_REVERSAL + requestParam.getOrderSn());
        if (!lock.tryLock()) {
            log.warn("订单重复修改状态，状态反转请求参数：{}", JSON.toJSONString(requestParam));
        }
        try {
            // 更新订单状态
            updateOrderStatus(requestParam);
            // 给每个子订单更新状态
            updateOrderItemStatus(requestParam);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TicketOrderPassengerDetailRespDTO> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam) {
        LambdaQueryWrapper<OrderItemDO> queryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn())
                .in(OrderItemDO::getId, requestParam.getOrderItemRecordIds());
        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(queryWrapper);
        return BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class);
    }

    private void updateOrderItemStatus(OrderItemStatusReversalDTO requestParam) {
        if (CollectionUtil.isNotEmpty(requestParam.getOrderItemDOList())) {
            List<OrderItemDO> orderItemDOList = requestParam.getOrderItemDOList();
            if (CollectionUtil.isNotEmpty(orderItemDOList)) {
                orderItemDOList.forEach(o -> {
                    OrderItemDO orderItemDO = new OrderItemDO();
                    orderItemDO.setStatus(requestParam.getOrderItemStatus());
                    LambdaUpdateWrapper<OrderItemDO> orderItemUpdateWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                            .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn())
                            .eq(OrderItemDO::getRealName, o.getRealName());
                    int orderItemUpdateResult = orderItemMapper.update(orderItemDO, orderItemUpdateWrapper);
                    if (orderItemUpdateResult <= 0) {
                        throw new ServiceException(OrderCancelErrorCodeEnum.ORDER_ITEM_STATUS_REVERSAL_ERROR);
                    }
                });
            }
        }
    }

    private void updateOrderStatus(OrderItemStatusReversalDTO requestParam) {
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setStatus(requestParam.getOrderStatus());
        LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        int orderUpdateResult = orderMapper.update(updateOrderDO, updateWrapper);
        if (orderUpdateResult <= 0) {
            throw new ServiceException(OrderCancelErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }

    private void checkOrderExist(String orderSn) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        if (orderDO == null) {
            throw new ServiceException(OrderCancelErrorCodeEnum.ORDER_CANCEL_UNKNOWN_ERROR);
        }
    }
}
