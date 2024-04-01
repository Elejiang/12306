package com.grace.train12306.framework.starter.convention.page;

import lombok.Data;

/**
 * 分页请求对象
 */
@Data
public class PageRequest {

    /**
     * 当前页
     */
    private Long current = 1L;

    /**
     * 每页显示条数
     */
    private Long size = 10L;
}
