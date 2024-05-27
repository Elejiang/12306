package com.grace.train12306.biz.orderservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.grace.train12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import com.grace.train12306.biz.orderservice.dao.mapper.OrderItemPassengerMapper;
import org.springframework.stereotype.Service;

/**
 * 乘车人订单关系接口层实现
 */
@Service
public class OrderPassengerRelationService extends ServiceImpl<OrderItemPassengerMapper, OrderItemPassengerDO> implements IService<OrderItemPassengerDO> {
}
