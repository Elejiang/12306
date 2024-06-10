package com.grace.train12306.biz.orderservice.remote;

import com.grace.train12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.grace.train12306.biz.orderservice.remote.dto.UserQueryActualRespDTO;
import com.grace.train12306.framework.starter.convention.result.Result;
import com.grace.train12306.framework.starter.web.Results;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 车票远程服务调用
 */
@FeignClient(value = "train12306-ticket${unique-name:}-service", url = "${remote.ticket.url}")
public interface TicketRemoteService {

    /**
     * 更改座位状态为已售
     */
    @PostMapping("/api/ticket-service/ticket/update")
    Result<Void> updateTicketStatusSold(@RequestBody TicketOrderDetailRespDTO requestParam);
}
