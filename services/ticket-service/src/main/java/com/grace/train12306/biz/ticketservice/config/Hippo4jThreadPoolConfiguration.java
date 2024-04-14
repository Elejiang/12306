package com.grace.train12306.biz.ticketservice.config;

import cn.hippo4j.common.executor.support.BlockingQueueTypeEnum;
import cn.hippo4j.core.executor.DynamicThreadPool;
import cn.hippo4j.core.executor.support.ThreadPoolBuilder;
import com.grace.train12306.biz.ticketservice.config.proxy.RejectedProxyUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hippo4j 动态线程池配置
 */
@Configuration
public class Hippo4jThreadPoolConfiguration {

    /**
     * 分配一个用户购买不同类型车票的线程池
     */
    @Bean
    @DynamicThreadPool
    public ThreadPoolExecutor selectSeatThreadPoolExecutor() {
        String threadPoolId = "select-seat-thread-pool-executor";
        return ThreadPoolBuilder.builder()
                .threadPoolId(threadPoolId)
                .threadFactory(threadPoolId)
                .workQueue(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE)
                .corePoolSize(12)
                .maximumPoolSize(18)
                .allowCoreThreadTimeOut(true)
                .keepAliveTime(60, TimeUnit.MINUTES)
                .rejected(RejectedProxyUtil.createProxy(new ThreadPoolExecutor.CallerRunsPolicy(), new AtomicLong()))
                .dynamicPool()
                .build();
    }
}
