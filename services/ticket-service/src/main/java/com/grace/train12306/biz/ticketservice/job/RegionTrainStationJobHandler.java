package com.grace.train12306.biz.ticketservice.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.ticketservice.dao.entity.RegionDO;
import com.grace.train12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.RegionMapper;
import com.grace.train12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.common.toolkit.EnvironmentUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.REGION_TRAIN_STATION;
import static com.grace.train12306.biz.ticketservice.common.constant.Train12306Constant.ADVANCE_TICKET_DAY;


/**
 * 地区站点查询定时任务
 */
@RestController
@RequiredArgsConstructor
public class RegionTrainStationJobHandler extends IJobHandler {

    private final RegionMapper regionMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    @XxlJob(value = "regionTrainStationJobHandler")
    @GetMapping("/api/ticket-service/region-train-station/job/cache-init/execute")
    @Override
    public void execute() {
        List<String> regionList = regionMapper.selectList(Wrappers.emptyWrapper())
                .stream()
                .map(RegionDO::getName)
                .collect(Collectors.toList());
        String requestParam = getJobRequestParam();
        var dateTime = StrUtil.isNotBlank(requestParam) ? requestParam : DateUtil.tomorrow().toDateStr();
        for (int i = 0; i < regionList.size(); i++) {
            for (int j = 0; j < regionList.size(); j++) {
                if (i != j) {
                    String startRegion = regionList.get(i);
                    String endRegion = regionList.get(j);
                    LambdaQueryWrapper<TrainStationRelationDO> relationQueryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getStartRegion, startRegion)
                            .eq(TrainStationRelationDO::getEndRegion, endRegion);
                    List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper.selectList(relationQueryWrapper);
                    if (CollUtil.isEmpty(trainStationRelationDOList)) {
                        continue;
                    }
                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                    for (TrainStationRelationDO item : trainStationRelationDOList) {
                        String zSetKey = StrUtil.join("_", item.getTrainId(), item.getDeparture(), item.getArrival());
                        ZSetOperations.TypedTuple<String> tuple = ZSetOperations.TypedTuple.of(zSetKey, (double) item.getDepartureTime().getTime());
                        tuples.add(tuple);
                    }
                    StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                    String buildCacheKey = REGION_TRAIN_STATION + StrUtil.join("_", startRegion, endRegion, dateTime);
                    stringRedisTemplate.opsForZSet().add(buildCacheKey, tuples);
                    stringRedisTemplate.expire(buildCacheKey, ADVANCE_TICKET_DAY, TimeUnit.DAYS);
                }
            }
        }
    }

    private String getJobRequestParam() {
        return EnvironmentUtil.isDevEnvironment()
                ? ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader("requestParam")
                : XxlJobHelper.getJobParam();
    }
}
