package com.grace.train12306.biz.orderservice;

import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务应用启动器
 */
@SpringBootApplication
@MapperScan("com.grace.train12306.biz.orderservice.dao.mapper")
@EnableFeignClients("com.grace.train12306.biz.orderservice.remote")
@EnableCrane4j(enumPackages = "com.grace.train12306.biz.orderservice.common.enums")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
