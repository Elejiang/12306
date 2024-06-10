package com.grace.train12306.biz.payservice.remote;

import com.grace.train12306.biz.payservice.remote.dto.TicketOrderDetailRespDTO;
import com.grace.train12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 车票订单远程服务调用
 */
@FeignClient(value = "train12306-order${unique-name:}-service", url = "${remote.order.url}")
public interface OrderRemoteService {

    /**
     * 跟据订单号查询车票订单
     *
     * @param orderSn 列车订单号
     * @return 列车订单记录
     */
    @GetMapping("/api/order-service/order/ticket/query")
    Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn);
}
