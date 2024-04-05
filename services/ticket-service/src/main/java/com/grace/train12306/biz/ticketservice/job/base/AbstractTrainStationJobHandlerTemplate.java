package com.grace.train12306.biz.ticketservice.job.base;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grace.train12306.biz.ticketservice.dao.entity.TrainDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.grace.train12306.framework.starter.bases.ApplicationContextHolder;
import com.grace.train12306.framework.starter.common.toolkit.EnvironmentUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * 抽象列车&车票相关定时任务
 */
public abstract class AbstractTrainStationJobHandlerTemplate extends IJobHandler {

    /**
     * 模板方法模式具体实现子类执行定时任务
     *
     * @param trainDOPageRecords 列车信息分页记录
     */
    protected abstract void actualExecute(List<TrainDO> trainDOPageRecords);

    @Override
    public void execute() {
        var currentPage = 1L;
        var size = 1000L;
        var requestParam = getJobRequestParam();
        var dateTime = StrUtil.isNotBlank(requestParam) ? DateUtil.parse(requestParam, "yyyy-MM-dd") : DateUtil.tomorrow();
        var trainMapper = ApplicationContextHolder.getBean(TrainMapper.class);
        for (; ; currentPage++) {
            var queryWrapper = Wrappers.lambdaQuery(TrainDO.class)
                    .between(TrainDO::getDepartureTime, DateUtil.beginOfDay(dateTime), DateUtil.endOfDay(dateTime));
            var trainDOPage = trainMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
            if (trainDOPage == null || CollUtil.isEmpty(trainDOPage.getRecords())) {
                break;
            }
            var trainDOPageRecords = trainDOPage.getRecords();
            actualExecute(trainDOPageRecords);
        }
    }

    private String getJobRequestParam() {
        return EnvironmentUtil.isDevEnvironment()
                ? Optional.ofNullable(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())).map(ServletRequestAttributes::getRequest).map(each -> each.getHeader("requestParam")).orElse(null)
                : XxlJobHelper.getJobParam();
    }
}
