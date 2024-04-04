package com.grace.train12306.biz.userservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grace.train12306.biz.userservice.dao.entity.UserDO;

/**
 * 用户信息持久层
 */
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 注销用户
     *
     * @param userDO 注销用户入参
     */
    void deletionUser(UserDO userDO);
}
