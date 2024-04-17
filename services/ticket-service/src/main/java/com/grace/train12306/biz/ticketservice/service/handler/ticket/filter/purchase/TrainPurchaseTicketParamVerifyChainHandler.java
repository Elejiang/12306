package com.grace.train12306.biz.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.ticketservice.dao.entity.TrainDO;
import com.grace.train12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.grace.train12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.grace.train12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.common.toolkit.EnvironmentUtil;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_INFO;
import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL;
import static com.grace.train12306.biz.ticketservice.common.constant.Train12306Constant.ADVANCE_TICKET_DAY;

/**
 * 购票流程过滤器之验证参数是否有效
 * 验证参数有效这个流程会大量交互缓存，为了优化性能需要使用 Lua。
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketParamVerifyChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    private final TrainMapper trainMapper;
    private final TrainStationMapper trainStationMapper;
    private final DistributedCache distributedCache;

    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // 查询会员购票车次是否存在，通过封装后安全的 Get 方法
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + requestParam.getTrainId(),
                TrainDO.class,
                () -> trainMapper.selectById(requestParam.getTrainId()),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS);
        if (Objects.isNull(trainDO)) {
            throw new ClientException("请检查车次是否存在");
        }
        // TODO，当前列车数据并没有通过定时任务每天生成最新的，所以需要隔离这个拦截。后期定时生成数据后删除该判断
        if (!EnvironmentUtil.isDevEnvironment()) {
            // 查询车次是否已经发售
            if (new Date().before(trainDO.getSaleTime())) {
                throw new ClientException("列车车次暂未发售");
            }
            // 查询车次是否在有效期内
            if (new Date().after(trainDO.getDepartureTime())) {
                throw new ClientException("列车车次已出发，禁止购票");
            }
        }
        // 车站是否存在车次中，以及车站的顺序是否正确
        String trainStationStopoverDetailStr = distributedCache.safeGet(
                TRAIN_STATION_STOPOVER_DETAIL + requestParam.getTrainId(),
                String.class,
                () -> {
                    LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                            .eq(TrainStationDO::getTrainId, requestParam.getTrainId())
                            .select(TrainStationDO::getDeparture);
                    List<TrainStationDO> actualTrainStationList = trainStationMapper.selectList(queryWrapper);
                    return CollUtil.isNotEmpty(actualTrainStationList) ? JSON.toJSONString(actualTrainStationList) : null;
                },
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
        List<TrainStationDO> trainDOList = JSON.parseArray(trainStationStopoverDetailStr, TrainStationDO.class);
        boolean validateStation = validateStation(
                trainDOList.stream().map(TrainStationDO::getDeparture).toList(),
                requestParam.getDeparture(),
                requestParam.getArrival()
        );
        if (!validateStation) {
            throw new ClientException("列车车站数据错误");
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }

    public boolean validateStation(List<String> stationList, String startStation, String endStation) {
        int index1 = stationList.indexOf(startStation);
        int index2 = stationList.indexOf(endStation);
        if (index1 == -1 || index2 == -1) {
            return false;
        }
        return index2 >= index1;
    }
}
