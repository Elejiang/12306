package com.grace.train12306.biz.ticketservice.canal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.grace.train12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.grace.train12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.grace.train12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.designpattern.strategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 列车余票缓存更新组件
 */
@Component
@RequiredArgsConstructor
public class TicketAvailabilityCacheUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent, Void> {

    private final DistributedCache distributedCache;

    @Override
    public void execute(CanalBinlogEvent message) {
        List<Map<String, Object>> messageDataList = new ArrayList<>();
        List<Map<String, Object>> actualOldDataList = new ArrayList<>();
        for (int i = 0; i < message.getOld().size(); i++) {
            Map<String, Object> oldDataMap = message.getOld().get(i);
            if (oldDataMap.get("seat_status") != null && StrUtil.isNotBlank(oldDataMap.get("seat_status").toString())) {
                Map<String, Object> currentDataMap = message.getData().get(i);
                if (StrUtil.equalsAny(currentDataMap.get("seat_status").toString(), String.valueOf(SeatStatusEnum.AVAILABLE.getCode()), String.valueOf(SeatStatusEnum.LOCKED.getCode()))) {
                    // 更新后的的车座是可售的或者锁定的
                    actualOldDataList.add(oldDataMap);
                    messageDataList.add(currentDataMap);
                }
            }
        }
        if (CollUtil.isEmpty(messageDataList) || CollUtil.isEmpty(actualOldDataList)) {
            return;
        }
        // 计算哪列车、哪些座位需要更改余票，key：车次_起始站_终点站，value：座位类型，更改的数量
        Map<String, Map<Integer, Integer>> cacheChangeKeyMap = new HashMap<>();
        for (int i = 0; i < messageDataList.size(); i++) {
            Map<String, Object> each = messageDataList.get(i);
            Map<String, Object> actualOldData = actualOldDataList.get(i);
            // 旧数据是可售的，说明新数据是锁定了座位，余票-1；旧数据是锁定的，说明新数据释放了座位，余票+1
            int increment = Objects.equals(actualOldData.get("seat_status").toString(), "0") ? -1 : 1;
            String trainId = each.get("train_id").toString();
            String hashCacheKey = TRAIN_STATION_REMAINING_TICKET + trainId + "_" + each.get("start_station") + "_" + each.get("end_station");
            Map<Integer, Integer> seatTypeMap = cacheChangeKeyMap.getOrDefault(hashCacheKey, new HashMap<>());
            Integer seatType = Integer.parseInt(each.get("seat_type").toString());
            Integer num = seatTypeMap.get(seatType);
            seatTypeMap.put(seatType, num == null ? increment : num + increment);
            cacheChangeKeyMap.put(hashCacheKey, seatTypeMap);
        }
        StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
        cacheChangeKeyMap.forEach((cacheKey, cacheVal) -> cacheVal.forEach((seatType, num) -> instance.opsForHash().increment(cacheKey, String.valueOf(seatType), num)));
    }

    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_SEAT.getActualTable();
    }
}
