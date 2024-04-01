package com.grace.train12306.framework.starter.bases.init;

import org.springframework.context.ApplicationEvent;

/**
 * 自定义事件，应用初始化事件
 */
public class ApplicationInitializingEvent extends ApplicationEvent {
    public ApplicationInitializingEvent(Object source) {
        super(source);
    }
}
