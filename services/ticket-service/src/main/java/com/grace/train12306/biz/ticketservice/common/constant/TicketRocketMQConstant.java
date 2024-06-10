package com.grace.train12306.biz.ticketservice.common.constant;

/**
 * RocketMQ 购票服务常量类
 */
public final class TicketRocketMQConstant {

    /**
     * 订单服务相关业务 Topic Key
     */
    public static final String ORDER_DELAY_CLOSE_TOPIC_KEY = "train12306_order-service_delay-close-order_topic${unique-name:}";

    /**
     * 购票服务创建订单后延时关闭业务 Tag Key
     */
    public static final String ORDER_DELAY_CLOSE_TAG_KEY = "train12306_order-service_delay-close-order_tag${unique-name:}";

    /**
     * 购票服务创建订单后延时关闭业务消费者组 Key
     */
    public static final String TICKET_DELAY_CLOSE_CG_KEY = "train12306_ticket-service_delay-close-order_cg${unique-name:}";

    /**
     * Canal 监听数据库余票变更 Topic Key
     */
    public static final String CANAL_COMMON_SYNC_TOPIC_KEY = "train12306_canal_ticket-service_common-sync_topic${unique-name:}";

    /**
     * Canal 监听数据库余票变更业务消费者组 Key
     */
    public static final String CANAL_COMMON_SYNC_CG_KEY = "train12306_canal_ticket-service_common-sync_cg${unique-name:}";

}
