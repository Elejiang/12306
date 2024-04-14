package com.grace.train12306.biz.orderservice.common.enums;

import com.grace.train12306.framework.starter.convention.errorcode.IErrorCode;
import lombok.AllArgsConstructor;

/**
 * 订单取消错误码枚举
 */
@AllArgsConstructor
public enum OrderCancelErrorCodeEnum implements IErrorCode {

    ORDER_CANCEL_UNKNOWN_ERROR("B006001", "订单不存在，请检查相关订单记录"),

    ORDER_CANCEL_STATUS_ERROR("B006002", "订单状态不正确，请检查相关订单记录"),

    ORDER_CANCEL_ERROR("B006003", "订单取消失败，请稍后再试"),

    ORDER_CANCEL_REPETITION_ERROR("B006004", "订单重复取消，请稍后再试"),

    ORDER_STATUS_REVERSAL_ERROR("B006005", "订单状态反转失败，请稍后再试"),

    ORDER_DELETE_ERROR("B006006", "订单状态反转失败，请稍后再试"),

    ORDER_ITEM_STATUS_REVERSAL_ERROR("B006007", "子订单状态反转失败，请稍后再试");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误提示消息
     */
    private final String message;

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
