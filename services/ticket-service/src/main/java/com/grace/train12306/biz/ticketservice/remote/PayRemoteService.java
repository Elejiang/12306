package com.grace.train12306.biz.ticketservice.remote;

import com.grace.train12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import com.grace.train12306.biz.ticketservice.remote.dto.RefundReqDTO;
import com.grace.train12306.biz.ticketservice.remote.dto.RefundRespDTO;
import com.grace.train12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 支付单远程调用服务
 */
@FeignClient(value = "train12306-pay${unique-name:}-service", url = "${remote.pay.url}")
public interface PayRemoteService {

    /**
     * 公共退款接口
     */
    @PostMapping("/api/pay-service/common/refund")
    Result<RefundRespDTO> commonRefund(@RequestBody RefundReqDTO requestParam);
}
