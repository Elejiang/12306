package com.grace.train12306.biz.ticketservice.remote;

import com.grace.train12306.biz.ticketservice.remote.dto.PassengerRespDTO;
import com.grace.train12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户远程服务调用
 */
@FeignClient(value = "train12306-user${unique-name:}-service", url = "${remote.user.url}")
public interface UserRemoteService {

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/inner/passenger/actual/query/ids")
    Result<List<PassengerRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username, @RequestParam("ids") List<String> ids);
}
