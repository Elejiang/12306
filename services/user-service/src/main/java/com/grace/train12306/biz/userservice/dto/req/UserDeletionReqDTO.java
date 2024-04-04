package com.grace.train12306.biz.userservice.dto.req;

import lombok.Data;

/**
 * 用户注销请求参数
 */
@Data
public class UserDeletionReqDTO {

    /**
     * 用户名
     */
    private String username;
}
