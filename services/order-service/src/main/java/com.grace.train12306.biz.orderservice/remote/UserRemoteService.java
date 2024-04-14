package com.grace.train12306.biz.orderservice.remote;

import com.grace.train12306.biz.orderservice.remote.dto.UserQueryActualRespDTO;
import com.grace.train12306.framework.starter.convention.result.Result;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户远程服务调用
 */
@FeignClient(value = "train12306-user${unique-name:}-service", url = "${remote.user.url}")
public interface UserRemoteService {

    /**
     * 根据 username 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/actual/query")
    Result<UserQueryActualRespDTO> queryActualUserByUsername(@RequestParam("username") @NotEmpty String username);
}
