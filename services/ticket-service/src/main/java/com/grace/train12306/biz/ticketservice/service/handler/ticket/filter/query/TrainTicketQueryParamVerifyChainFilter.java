package com.grace.train12306.biz.ticketservice.service.handler.ticket.filter.query;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.grace.train12306.biz.ticketservice.dao.entity.RegionDO;
import com.grace.train12306.biz.ticketservice.dao.entity.StationDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.RegionMapper;
import com.grace.train12306.biz.ticketservice.dao.mapper.StationMapper;
import com.grace.train12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_ALL_REGION_LIST;
import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.QUERY_ALL_REGION_LIST;

/**
 * 查询列车车票流程过滤器之验证数据是否正确
 */
@Component
@RequiredArgsConstructor
public class TrainTicketQueryParamVerifyChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    /**
     * 缓存数据为空并且已经加载过标识
     */
    private static boolean CACHE_DATA_ISNULL_AND_LOAD_FLAG = false;

    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
        // 查询出发站点和到达站点是否存在，如果不存在也一样属于异常数据
        List<Object> actualExistList = hashOperations.multiGet(
                QUERY_ALL_REGION_LIST,
                ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation())
        );
        // 就算为空，也会返回数据，所以通过 filter 判断对象是否为空
        long emptyCount = actualExistList.stream().filter(Objects::isNull).count();
        // 如果为空的记录是0的话，就证明出发站点和到达站点存在，正常返回即可
        if (emptyCount == 0L) {
            return;
        }
        // 如果出发站点和到达站点都不存在或者仅存在一个，直接抛出异常
        // FLAG = true 代表已经加载过一次，此时还是空，证明说数据库也没有这两个站点信息，抛异常
        if (emptyCount == 1L || (emptyCount == 2L && CACHE_DATA_ISNULL_AND_LOAD_FLAG && distributedCache.hasKey(QUERY_ALL_REGION_LIST))) {
            throw new ClientException("出发地或目的地不存在");
        }
        // 如果FLAG=false代表有可能缓存没有数据，但数据库可能有，此时向下查询数据库
        // 为了避免缓存击穿，所以这里是用了分布式锁
        RLock lock = redissonClient.getLock(LOCK_QUERY_ALL_REGION_LIST);
        lock.lock();
        try {
            // 获取完分布式锁，避免重复且无用的加载数据库，通过双重判定锁的形式，在查询一次缓存
            // 如果缓存存在，直接返回即可。
            if (distributedCache.hasKey(QUERY_ALL_REGION_LIST)) {
                actualExistList = hashOperations.multiGet(
                        QUERY_ALL_REGION_LIST,
                        ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation())
                );
                emptyCount = actualExistList.stream().filter(Objects::nonNull).count();
                if (emptyCount != 2L) {
                    throw new ClientException("出发地或目的地不存在");
                }
                return;
            }
            // 因为站点是可以传城市名称的，也就是 Region，所以我们需要查询站点以及城市两张表
            List<RegionDO> regionDOList = regionMapper.selectList(Wrappers.emptyWrapper());
            List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
            HashMap<Object, Object> regionValueMap = Maps.newHashMap();
            for (RegionDO each : regionDOList) {
                regionValueMap.put(each.getCode(), each.getName());
            }
            for (StationDO each : stationDOList) {
                regionValueMap.put(each.getCode(), each.getName());
            }
            // 查询完后，通过 putAll 的形式存入缓存
            hashOperations.putAll(QUERY_ALL_REGION_LIST, regionValueMap);
            CACHE_DATA_ISNULL_AND_LOAD_FLAG = true;
            emptyCount = regionValueMap.keySet().stream()
                    .filter(each -> StrUtil.equalsAny(each.toString(), requestParam.getFromStation(), requestParam.getToStation()))
                    .count();
            if (emptyCount != 2L) {
                throw new ClientException("出发地或目的地不存在");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
