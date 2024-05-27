package com.grace.train12306.biz.ticketservice.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.ticketservice.common.enums.RegionStationQueryTypeEnum;
import com.grace.train12306.biz.ticketservice.dao.entity.RegionDO;
import com.grace.train12306.biz.ticketservice.dao.entity.StationDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.RegionMapper;
import com.grace.train12306.biz.ticketservice.dao.mapper.StationMapper;
import com.grace.train12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.StationQueryRespDTO;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.cache.core.CacheLoader;
import com.grace.train12306.framework.starter.cache.toolkit.CacheUtil;
import com.grace.train12306.framework.starter.common.enums.FlagEnum;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.*;
import static com.grace.train12306.biz.ticketservice.common.constant.Train12306Constant.ADVANCE_TICKET_DAY;

/**
 * 地区以及车站接口实现层
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RegionStationService {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    public List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam) {
        if (StrUtil.isNotBlank(requestParam.getName())) {
            // 如果是根据名称查询
            return safeGetRegionStation(
                    REGION_STATION + requestParam.getName(),
                    () -> {
                        LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                                .likeRight(StationDO::getName, requestParam.getName())
                                .or()
                                .likeRight(StationDO::getSpell, requestParam.getName());
                        List<StationDO> stationDOList = stationMapper.selectList(queryWrapper);
                        return JSON.toJSONString(BeanUtil.convert(stationDOList, RegionStationQueryRespDTO.class));
                    },
                    requestParam.getName()
            );
        }
        LambdaQueryWrapper<RegionDO> queryWrapper = switch (Objects.requireNonNull(RegionStationQueryTypeEnum.getByCode(requestParam.getQueryType()))) {
            case HOT -> Wrappers.lambdaQuery(RegionDO.class)
                    .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());
            case A_E -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.A_E.getSpells());
            case F_J -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.F_J.getSpells());
            case K_O -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.K_O.getSpells());
            case P_T -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.P_T.getSpells());
            case U_Z -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.U_Z.getSpells());
        };
        return safeGetRegionStation(
                REGION_STATION + requestParam.getQueryType(),
                () -> {
                    List<RegionDO> regionDOList = regionMapper.selectList(queryWrapper);
                    return JSON.toJSONString(BeanUtil.convert(regionDOList, RegionStationQueryRespDTO.class));
                },
                String.valueOf(requestParam.getQueryType())
        );
    }

    public List<StationQueryRespDTO> listAllStation() {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        Set<String> keys = stringRedisTemplate.keys(STATION_ALL + "*");
        if (!Objects.requireNonNull(keys).isEmpty()) {
            // 缓存中存在，返回
            return Objects.requireNonNull(stringRedisTemplate.opsForValue().multiGet(
                    keys.stream()
                            .map(s -> s.replaceFirst(System.getenv("framework.cache.redis.prefix"), ""))
                            .toList()
            )).stream().map(each -> JSON.parseObject(each, StationQueryRespDTO.class)).toList();
        }
        // 缓存中不存在，加载至缓存
        List<StationQueryRespDTO> resp = BeanUtil.convert(stationMapper.selectList(Wrappers.emptyWrapper()), StationQueryRespDTO.class);
        stringRedisTemplate.opsForValue().multiSet(resp.stream().collect(Collectors.toMap((each -> STATION_ALL + each.getCode()), JSON::toJSONString)));
        return resp;
    }

    private List<RegionStationQueryRespDTO> safeGetRegionStation(final String key, CacheLoader<String> loader, String param) {
        List<RegionStationQueryRespDTO> result;
        if (CollUtil.isNotEmpty(result = JSON.parseArray(distributedCache.get(key, String.class), RegionStationQueryRespDTO.class))) {
            return result;
        }
        String lockKey = String.format(LOCK_QUERY_REGION_STATION_LIST, param);
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            if (CollUtil.isEmpty(result = JSON.parseArray(distributedCache.get(key, String.class), RegionStationQueryRespDTO.class))) {
                if (CollUtil.isEmpty(result = loadAndSet(key, loader))) {
                    return Collections.emptyList();
                }
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    private List<RegionStationQueryRespDTO> loadAndSet(final String key, CacheLoader<String> loader) {
        String result = loader.load();
        if (CacheUtil.isNullOrBlank(result)) {
            return Collections.emptyList();
        }
        List<RegionStationQueryRespDTO> respDTOList = JSON.parseArray(result, RegionStationQueryRespDTO.class);
        distributedCache.put(
                key,
                result,
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
        return respDTOList;
    }
}
