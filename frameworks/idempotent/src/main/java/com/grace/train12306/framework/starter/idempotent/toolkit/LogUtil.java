package com.grace.train12306.framework.starter.idempotent.toolkit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类
 */
public class LogUtil {
    
    /**
     * 获取 Logger
     */
    public static Logger getLog(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        return LoggerFactory.getLogger(methodSignature.getDeclaringType());
    }
}
