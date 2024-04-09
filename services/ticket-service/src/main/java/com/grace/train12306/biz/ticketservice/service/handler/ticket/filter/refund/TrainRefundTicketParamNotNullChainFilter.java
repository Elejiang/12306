package com.grace.train12306.biz.ticketservice.service.handler.ticket.filter.refund;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.grace.train12306.biz.ticketservice.common.enums.RefundTypeEnum;
import com.grace.train12306.biz.ticketservice.dto.req.RefundTicketReqDTO;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Component;

/**
 * 列车车票退款流程过滤器之验证数据是否为空或空的字符串
 */
@Component
public class TrainRefundTicketParamNotNullChainFilter implements TrainRefundTicketChainFilter<RefundTicketReqDTO> {

    @Override
    public void handler(RefundTicketReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getOrderSn())) {
            throw new ClientException("订单号不能为空");
        }
        if (requestParam.getType() == null) {
            throw new ClientException("退款类型不能为空");
        }
        if (requestParam.getType().equals(RefundTypeEnum.PARTIAL_REFUND.getType())) {
            if (CollUtil.isEmpty(requestParam.getSubOrderRecordIdReqList())) {
                throw new ClientException("部分退款子订单记录集合不能为空");
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
