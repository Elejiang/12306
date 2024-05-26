package com.grace.train12306.framework.starter.idempotent.core;

import com.grace.train12306.framework.starter.idempotent.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 幂等注解 AOP 拦截器
 */
@Aspect
public final class IdempotentAspect {

    /**
     * 增强方法标记 {@link Idempotent} 注解逻辑
     */
    @Around("@annotation(com.grace.train12306.framework.starter.idempotent.annotation.Idempotent)")
    public Object idempotentHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        Idempotent idempotent = getIdempotent(joinPoint);
        // 拿到对应的幂等注解处理器
        IdempotentExecuteHandler instance = IdempotentExecuteHandlerFactory.getInstance(idempotent.scene());
        Object resultObj;
        try {
            // 对方法进行幂等处理
            instance.execute(joinPoint, idempotent);
            // 执行方法
            resultObj = joinPoint.proceed();
            // 一些后处理，如释放锁资源，修改消费状态，设置幂等标识
            instance.postProcessing();
        } catch (RepeatConsumptionException ex) {
            if (!ex.getError()) {
                // 消息处理成功了，直接返回
                return null;
            }
            // 消息正在处理中，抛出异常，交给上层处理
            throw ex;
        } catch (Throwable ex) {
            // 客户端消费存在异常，进行异常处理
            instance.exceptionProcessing();
            throw ex;
        } finally {
            IdempotentContext.clean();
        }
        return resultObj;
    }

    public static Idempotent getIdempotent(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getMethod().getParameterTypes());
        return targetMethod.getAnnotation(Idempotent.class);
    }
}
