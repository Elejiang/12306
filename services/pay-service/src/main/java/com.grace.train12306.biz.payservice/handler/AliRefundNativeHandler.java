package com.grace.train12306.biz.payservice.handler;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.grace.train12306.biz.payservice.common.enums.PayChannelEnum;
import com.grace.train12306.biz.payservice.common.enums.PayTradeTypeEnum;
import com.grace.train12306.biz.payservice.common.enums.TradeStatusEnum;
import com.grace.train12306.biz.payservice.config.AliPayProperties;
import com.grace.train12306.biz.payservice.dto.base.AliRefundRequest;
import com.grace.train12306.biz.payservice.dto.base.RefundRequest;
import com.grace.train12306.biz.payservice.dto.base.RefundResponse;
import com.grace.train12306.biz.payservice.handler.base.AbstractRefundHandler;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import com.grace.train12306.framework.starter.designpattern.strategy.AbstractExecuteStrategy;
import com.grace.train12306.framework.starter.distributedid.toolkit.SnowflakeIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 阿里支付组件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliRefundNativeHandler extends AbstractRefundHandler implements AbstractExecuteStrategy<RefundRequest, RefundResponse> {

    private final AliPayProperties aliPayProperties;

    private final static String SUCCESS_CODE = "10000";

    private final static String FUND_CHANGE = "Y";

    @Retryable(value = {ServiceException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 1.5))
    @SneakyThrows(value = AlipayApiException.class)
    @Override
    public RefundResponse refund(RefundRequest payRequest) {
        AliRefundRequest aliRefundRequest = payRequest.getAliRefundRequest();
        AlipayConfig alipayConfig = BeanUtil.convert(aliPayProperties, AlipayConfig.class);
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(aliRefundRequest.getOrderSn());
        model.setTradeNo(aliRefundRequest.getTradeNo());
        BigDecimal payAmount = aliRefundRequest.getPayAmount();
        BigDecimal refundAmount = payAmount.divide(new BigDecimal(100));
        model.setRefundAmount(refundAmount.toString());
        model.setOutRequestNo(SnowflakeIdUtil.nextIdStr());
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizModel(model);
        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            String responseJson = JSONObject.toJSONString(response);
            log.info("发起支付宝退款，订单号：{}，交易凭证号：{}，退款金额：{} \n调用退款响应：\n\n{}\n",
                    aliRefundRequest.getOrderSn(),
                    aliRefundRequest.getTradeNo(),
                    aliRefundRequest.getPayAmount(),
                    responseJson);
            if (!StrUtil.equals(SUCCESS_CODE, response.getCode()) || !StrUtil.equals(FUND_CHANGE, response.getFundChange())) {
                throw new ServiceException("退款失败");
            }
            return new RefundResponse(TradeStatusEnum.TRADE_CLOSED.tradeCode(), response.getTradeNo());
        } catch (AlipayApiException e) {
            throw new ServiceException("调用支付宝退款异常");
        }
    }

    @Override
    public String mark() {
        return StrBuilder.create()
                .append(PayChannelEnum.ALI_PAY.name())
                .append("_")
                .append(PayTradeTypeEnum.NATIVE.name())
                .append("_")
                .append(TradeStatusEnum.TRADE_CLOSED.tradeCode())
                .toString();
    }

    @Override
    public RefundResponse executeResp(RefundRequest requestParam) {
        return refund(requestParam);
    }
}
