package com.grace.train12306.biz.userservice.service;


import com.grace.train12306.biz.userservice.dto.req.UserUpdateReqDTO;
import com.grace.train12306.biz.userservice.dto.resp.UserQueryActualRespDTO;
import com.grace.train12306.biz.userservice.dto.resp.UserQueryRespDTO;
import jakarta.validation.constraints.NotEmpty;

/**
 * 用户信息接口层
 */
public interface UserService {

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param userId 用户 ID
     * @return 用户详细信息
     */
    UserQueryRespDTO queryUserByUserId(@NotEmpty String userId);

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户详细信息
     */
    UserQueryRespDTO queryUserByUsername(@NotEmpty String username);

    /**
     * 根据用户名查询用户无脱敏信息
     *
     * @param username 用户名
     * @return 用户详细信息
     */
    UserQueryActualRespDTO queryActualUserByUsername(@NotEmpty String username);


    /**
     * 根据用户 ID 修改用户信息
     *
     * @param requestParam 用户信息入参
     */
    void update(UserUpdateReqDTO requestParam);
}
