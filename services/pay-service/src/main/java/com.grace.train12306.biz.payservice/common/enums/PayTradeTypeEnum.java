package com.grace.train12306.biz.payservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * 交易环境枚举
 */
@RequiredArgsConstructor
public enum PayTradeTypeEnum {

    /**
     * 扫码支付环境
     */
    NATIVE(0),

    /**
     * 移动端 Web 应用中支付环境
     */
    JSAPI(1),

    /**
     * 手机浏览器中打开的H5网页支付环境
     */
    MWEB(2),

    /**
     * 去中心化应用程序中支付环境
     */
    DAPP(3);

    @Getter
    private final Integer code;

    /**
     * 根据名称查找码值
     */
    public static String findNameByCode(Integer code) {
        return Arrays.stream(PayTradeTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(PayTradeTypeEnum::name)
                .orElse(null);
    }
}
