package com.grace.train12306.biz.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.grace.train12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.grace.train12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 购票流程过滤器之验证参数必填
 */
@Component
public class TrainPurchaseTicketParamNotNullChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getTrainId())) {
            throw new ClientException("列车标识不能为空");
        }
        if (StrUtil.isBlank(requestParam.getDeparture())) {
            throw new ClientException("出发站点不能为空");
        }
        if (StrUtil.isBlank(requestParam.getArrival())) {
            throw new ClientException("到达站点不能为空");
        }
        if (CollUtil.isEmpty(requestParam.getPassengers())) {
            throw new ClientException("乘车人至少选择一位");
        }
        for (PurchaseTicketPassengerDetailDTO each : requestParam.getPassengers()) {
            if (StrUtil.isBlank(each.getPassengerId())) {
                throw new ClientException("乘车人不能为空");
            }
            if (Objects.isNull(each.getSeatType())) {
                throw new ClientException("座位类型不能为空");
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
