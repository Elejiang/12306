package com.grace.train12306.framework.starter.idempotent.core.spel;

import cn.hutool.core.util.StrUtil;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import com.grace.train12306.framework.starter.distributedid.toolkit.SnowflakeIdUtil;
import com.grace.train12306.framework.starter.idempotent.annotation.Idempotent;
import com.grace.train12306.framework.starter.idempotent.core.*;
import com.grace.train12306.framework.starter.idempotent.enums.IdempotentMQConsumeKeyEnum;
import com.grace.train12306.framework.starter.idempotent.enums.IdempotentMQConsumeStatusEnum;
import com.grace.train12306.framework.starter.idempotent.toolkit.LogUtil;
import com.grace.train12306.framework.starter.idempotent.toolkit.SpELUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.concurrent.TimeUnit;

/**
 * 基于 SpEL 方法验证请求幂等性，适用于 MQ 场景
 */
@RequiredArgsConstructor
public final class IdempotentSpELByMQExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpELService {

    private final static int TIMEOUT = 600;
    private final static String WRAPPER = "wrapper:spEL:MQ";
    private final DistributedCache distributedCache;

    @SneakyThrows
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        // 通过执行 SpEL 表达式获取值
        String key = (String) SpELUtil.parseKey(idempotent.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        // 获取到消费id
        String consumptionId = SnowflakeIdUtil.nextIdStr();
        return IdempotentParamWrapper.builder().lockKey(key).joinPoint(joinPoint).consumptionId(consumptionId).build();
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 拼接前缀和 SpEL 表达式对应的 Key 生成最终放到 Redis 中的唯一标识
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();
        // 取到消费id
        String consumptionId = wrapper.getConsumptionId();
        Boolean setIfAbsent =
                distributedCache.putHashIfAbsent(uniqueKey, IdempotentMQConsumeKeyEnum.CONSUMPTION_ID.getKey(), consumptionId);
        if (setIfAbsent != null && !setIfAbsent) {
            String consumeStatus = distributedCache.get(uniqueKey, IdempotentMQConsumeKeyEnum.STATUS.getKey()).toString();
            boolean error = IdempotentMQConsumeStatusEnum.isError(consumeStatus);
            LogUtil.getLog(wrapper.getJoinPoint()).warn("[{}] MQ repeated consumption, {}.", uniqueKey, error ? "Wait for the client to delay consumption" : "Status is completed");
            // 抛出异常，交给上层判断应该重试还是将消息吞掉
            throw new RepeatConsumptionException(error);
        }
        distributedCache.putHash(uniqueKey, IdempotentMQConsumeKeyEnum.STATUS.getKey(), IdempotentMQConsumeStatusEnum.CONSUMING.getCode());
        distributedCache.expire(uniqueKey, TIMEOUT, TimeUnit.SECONDS);
        IdempotentContext.put(WRAPPER, wrapper);
    }

    @Override
    public void exceptionProcessing() {
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if (wrapper != null) {
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try {
                distributedCache.delete(uniqueKey);
            } catch (Throwable ex) {
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to del MQ anti-heavy token.", uniqueKey);
            }
        }
    }

    @Override
    public void postProcessing() {
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if (wrapper != null) {
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            String consumptionId = wrapper.getConsumptionId();
            boolean isOwn = StrUtil.equals(consumptionId, distributedCache.get(uniqueKey, IdempotentMQConsumeKeyEnum.CONSUMPTION_ID.getKey()).toString());
            if (isOwn) {
                distributedCache.putHash(uniqueKey, IdempotentMQConsumeKeyEnum.STATUS.getKey(), IdempotentMQConsumeStatusEnum.CONSUMED.getCode());
                distributedCache.expire(uniqueKey, idempotent.keyTimeout(), TimeUnit.SECONDS);
            } else {
                throw new ServiceException("消息消费出错");
            }
        }
    }
}
