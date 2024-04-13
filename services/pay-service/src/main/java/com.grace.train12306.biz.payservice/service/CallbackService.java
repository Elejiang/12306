package com.grace.train12306.biz.payservice.service;

import com.grace.train12306.biz.payservice.dto.PayCallbackReqDTO;

/**
 * 支付回调接口层
 */
public interface CallbackService {

    /**
     * 支付单回调
     *
     * @param requestParam 回调支付单实体
     */
    void callbackPay(PayCallbackReqDTO requestParam);
}
