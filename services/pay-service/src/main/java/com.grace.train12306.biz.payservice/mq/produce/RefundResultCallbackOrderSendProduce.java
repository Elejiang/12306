package com.grace.train12306.biz.payservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import com.grace.train12306.biz.payservice.mq.domain.MessageWrapper;
import com.grace.train12306.biz.payservice.mq.event.RefundResultCallbackOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.grace.train12306.biz.payservice.common.constant.PayRocketMQConstant.PAY_GLOBAL_TOPIC_KEY;
import static com.grace.train12306.biz.payservice.common.constant.PayRocketMQConstant.REFUND_RESULT_CALLBACK_TAG_KEY;

/**
 * 退款结果回调订单生产者
 */
@Slf4j
@Component
public class RefundResultCallbackOrderSendProduce extends AbstractCommonSendProduceTemplate<RefundResultCallbackOrderEvent> {

    private final ConfigurableEnvironment environment;

    public RefundResultCallbackOrderSendProduce(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(RefundResultCallbackOrderEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("全部退款或部分退款结果回调订单")
                .keys(messageSendEvent.getOrderSn())
                .topic(environment.resolvePlaceholders(PAY_GLOBAL_TOPIC_KEY))
                .tag(environment.resolvePlaceholders(REFUND_RESULT_CALLBACK_TAG_KEY))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(RefundResultCallbackOrderEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
