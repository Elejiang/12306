package com.grace.train12306.biz.orderservice.dto.domain;

import com.grace.train12306.biz.orderservice.dao.entity.OrderItemDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单状态反转实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class OrderStatusReversalDTO {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 订单反转后状态
     */
    private Integer orderStatus;

    /**
     * 订单明细反转后状态
     */
    private Integer orderItemStatus;

    /**
     * 订单明细集合
     */
    private List<OrderItemDO> orderItemDOList;
}
