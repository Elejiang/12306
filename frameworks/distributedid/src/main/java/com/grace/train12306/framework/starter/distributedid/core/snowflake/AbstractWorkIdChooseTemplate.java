package com.grace.train12306.framework.starter.distributedid.core.snowflake;

import cn.hutool.core.date.SystemClock;
import com.grace.train12306.framework.starter.distributedid.toolkit.SnowflakeIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 雪花算法模板生成
 */
@Slf4j
public abstract class AbstractWorkIdChooseTemplate {

    /**
     * 是否使用 {@link SystemClock} 获取当前时间戳
     */
    @Value("${framework.distributed.id.snowflake.is-use-system-clock:false}")
    private boolean isUseSystemClock;

    /**
     * 根据自定义策略获取 WorkId 生成器
     *
     * @return
     */
    protected abstract idWrapper chooseId();

    /**
     * 选择 WorkId 并初始化雪花
     */
    public void chooseAndInit() {
        idWrapper idWrapper = chooseId();
        long workId = idWrapper.getWorkId();
        long dataCenterId = idWrapper.getDataCenterId();
        Snowflake snowflake = new Snowflake(workId, dataCenterId, isUseSystemClock);
        log.info("Snowflake type: {}, workId: {}, dataCenterId: {}", this.getClass().getSimpleName(), workId, dataCenterId);
        SnowflakeIdUtil.initSnowflake(snowflake);
    }
}
