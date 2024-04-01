package com.grace.train12306.framework.starter.bases.safa;

import org.springframework.beans.factory.InitializingBean;

/**
 * 打开 FastJson 安全模式，开启后关闭类型隐式转换
 */
public class FastJsonSafeMode implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        System.setProperty("fastjson2.parser.safeMode", "true");
    }
}
