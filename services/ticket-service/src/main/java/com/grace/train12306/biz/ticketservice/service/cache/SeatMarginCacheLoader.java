package com.grace.train12306.biz.ticketservice.service.cache;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.grace.train12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.grace.train12306.biz.ticketservice.dao.entity.SeatDO;
import com.grace.train12306.biz.ticketservice.dao.entity.TrainDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.grace.train12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.grace.train12306.biz.ticketservice.dto.domain.RouteDTO;
import com.grace.train12306.biz.ticketservice.service.TrainStationService;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.cache.toolkit.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.*;
import static com.grace.train12306.biz.ticketservice.common.constant.Train12306Constant.ADVANCE_TICKET_DAY;
import static com.grace.train12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum.*;

/**
 * 座位余量缓存加载
 */
@Component
@RequiredArgsConstructor
public class SeatMarginCacheLoader {

    private final TrainMapper trainMapper;
    private final SeatMapper seatMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final TrainStationService trainStationService;

    public Map<String, String> load(String trainId, String seatType, String departure, String arrival) {
        // 最终结果，key：某车次从某站到某站，value：key：座位类型，value：余票数量
        Map<String, Map<String, String>> trainStationRemainingTicketMaps = new LinkedHashMap<>();
        String keySuffix = CacheUtil.buildKey(trainId, departure, arrival);
        RLock lock = redissonClient.getLock(String.format(LOCK_SAFE_LOAD_SEAT_MARGIN_GET, keySuffix));
        lock.lock();
        try {
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
            if (CacheUtil.isNullOrBlank(quantityObj)) {
                // 如果缓存中没有该车次、该始发站到终点站、该座位类型的余票数量，则加载至缓存
                // 先获取到车次信息
                TrainDO trainDO = distributedCache.safeGet(
                        TRAIN_INFO + trainId,
                        TrainDO.class,
                        () -> trainMapper.selectById(trainId),
                        ADVANCE_TICKET_DAY,
                        TimeUnit.DAYS
                );
                // 获取开始站点和目的站点及中间站点信息
                List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(trainId, trainDO.getStartStation(), trainDO.getEndStation());
                if (CollUtil.isNotEmpty(routeDTOList)) {
                    // 根据不同车类型加载余票，有高铁、动车、普通车
                    switch (Objects.requireNonNull(VehicleTypeEnum.getByCode(trainDO.getTrainType()))) {
                        case HIGH_SPEED_RAIN -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                trainStationRemainingTicket.put(String.valueOf(BUSINESS_CLASS.getCode()), selectSeatMargin(trainId, BUSINESS_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put(String.valueOf(FIRST_CLASS.getCode()), selectSeatMargin(trainId, FIRST_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put(String.valueOf(SECOND_CLASS.getCode()), selectSeatMargin(trainId, SECOND_CLASS.getCode(), each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                        case BULLET -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                trainStationRemainingTicket.put(String.valueOf(SECOND_CLASS_CABIN_SEAT.getCode()), selectSeatMargin(trainId, SECOND_CLASS_CABIN_SEAT.getCode(), each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put(String.valueOf(FIRST_SLEEPER.getCode()), selectSeatMargin(trainId, FIRST_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put(String.valueOf(SECOND_SLEEPER.getCode()), selectSeatMargin(trainId, SECOND_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put(String.valueOf(NO_SEAT_SLEEPER.getCode()), selectSeatMargin(trainId, NO_SEAT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                        case REGULAR_TRAIN -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                trainStationRemainingTicket.put(String.valueOf(SOFT_SLEEPER.getCode()), selectSeatMargin(trainId, SOFT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put(String.valueOf(HARD_SLEEPER.getCode()), selectSeatMargin(trainId, HARD_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put(String.valueOf(HARD_SEAT.getCode()), selectSeatMargin(trainId, HARD_SEAT.getCode(), each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put(String.valueOf(NO_SEAT_SLEEPER.getCode()), selectSeatMargin(trainId, NO_SEAT_SLEEPER.getCode(), each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                    }
                } else {
                    // 如果该车次没有这个路线
                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                    // 每个座位类型余量设置为0
                    VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType())
                            .forEach(each -> trainStationRemainingTicket.put(String.valueOf(each), "0"));
                    trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + keySuffix, trainStationRemainingTicket);
                }
                trainStationRemainingTicketMaps.forEach(
                        (cacheKey, cacheMap) -> {
                            stringRedisTemplate.opsForHash().putAll(cacheKey, cacheMap);
                            stringRedisTemplate.expire(cacheKey, ADVANCE_TICKET_DAY, TimeUnit.DAYS);
                        }
                );
            }
        } finally {
            lock.unlock();
        }
        return Optional.ofNullable(trainStationRemainingTicketMaps.get(TRAIN_STATION_REMAINING_TICKET + keySuffix))
                .orElse(new LinkedHashMap<>());
    }

    /**
     * 计算座位数量
     *
     * @param trainId   车次id
     * @param type      座位类型
     * @param departure 始发站
     * @param arrival   终点站
     * @return 余票数量
     */
    private String selectSeatMargin(String trainId, Integer type, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, type)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival);
        return Optional.ofNullable(seatMapper.selectCount(queryWrapper))
                .map(String::valueOf)
                .orElse("0");
    }
}
