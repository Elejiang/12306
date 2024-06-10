package com.grace.train12306.biz.ticketservice.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.grace.train12306.biz.ticketservice.common.enums.RefundTypeEnum;
import com.grace.train12306.biz.ticketservice.common.enums.SourceEnum;
import com.grace.train12306.biz.ticketservice.common.enums.TicketChainMarkEnum;
import com.grace.train12306.biz.ticketservice.common.enums.TicketStatusEnum;
import com.grace.train12306.biz.ticketservice.dao.entity.*;
import com.grace.train12306.biz.ticketservice.dao.mapper.*;
import com.grace.train12306.biz.ticketservice.dto.domain.*;
import com.grace.train12306.biz.ticketservice.dto.req.*;
import com.grace.train12306.biz.ticketservice.dto.resp.RefundTicketRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.TicketOrderDetailRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.grace.train12306.biz.ticketservice.remote.PayRemoteService;
import com.grace.train12306.biz.ticketservice.remote.OrderRemoteService;
import com.grace.train12306.biz.ticketservice.remote.dto.*;
import com.grace.train12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import com.grace.train12306.biz.ticketservice.service.handler.ticket.dto.TokenResultDTO;
import com.grace.train12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.grace.train12306.biz.ticketservice.service.handler.ticket.select.TrainSeatTypeSelector;
import com.grace.train12306.biz.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import com.grace.train12306.biz.ticketservice.toolkit.DateUtil;
import com.grace.train12306.biz.ticketservice.toolkit.TimeStringComparator;
import com.grace.train12306.framework.starter.bases.ApplicationContextHolder;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.cache.toolkit.CacheUtil;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import com.grace.train12306.framework.starter.convention.result.Result;
import com.grace.train12306.framework.starter.designpattern.chain.AbstractChainContext;
import com.grace.train12306.framework.starter.user.core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.*;
import static com.grace.train12306.biz.ticketservice.common.constant.Train12306Constant.ADVANCE_TICKET_DAY;
import static com.grace.train12306.biz.ticketservice.toolkit.DateUtil.convertDateToLocalTime;

/**
 * 车票接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService extends ServiceImpl<TicketMapper, TicketDO> implements CommandLineRunner {
    private final TrainMapper trainMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final DistributedCache distributedCache;
    private final OrderRemoteService orderRemoteService;
    private final PayRemoteService payRemoteService;
    private final StationMapper stationMapper;
    private final SeatService seatService;
    private final TrainStationService trainStationService;
    private final TrainSeatTypeSelector trainSeatTypeSelector;
    private final SeatMarginCacheLoader seatMarginCacheLoader;
    private final AbstractChainContext<TicketPageQueryReqDTO> ticketPageQueryAbstractChainContext;
    private final AbstractChainContext<PurchaseTicketReqDTO> purchaseTicketAbstractChainContext;
    private final AbstractChainContext<RefundTicketReqDTO> refundReqDTOAbstractChainContext;
    private final RedissonClient redissonClient;
    private final ConfigurableEnvironment environment;
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;
    private final Cache<String, ReentrantLock> localLockMap = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();
    private TicketService ticketService;

    @Value("${ticket.availability.cache-update.type:}")
    private String ticketAvailabilityCacheUpdateType;

    public TicketPageQueryRespDTO pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        // 责任链模式 验证城市名称是否存在、不存在加载缓存以及出发日期不能小于当前日期等等
        ticketPageQueryAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(), requestParam);
        // 查找到始发站和终点站对应的地区
        List<Object> stationDetails = safeGetRegion(requestParam);
        // 得到车次车票信息
        List<TicketListDTO> seatResults = new ArrayList<>();
        seatResults = safeGetTicketInformation(stationDetails, seatResults);
        // 填充车次车票的价格和余票
        fillSeatPriceAndRemain(seatResults);
        return TicketPageQueryRespDTO.builder()
                .trainList(seatResults)
                .departureStationList(buildDepartureStationList(seatResults))
                .arrivalStationList(buildArrivalStationList(seatResults))
                .trainBrandList(buildTrainBrandList(seatResults))
                .seatClassTypeList(buildSeatClassList(seatResults))
                .build();
    }

    public TicketPurchaseRespDTO purchaseTickets(PurchaseTicketReqDTO requestParam) {
        // 责任链模式，验证 1：参数必填 2：参数正确性 3：乘客是否已买当前车次等...
        purchaseTicketAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(), requestParam);
        // 获取令牌
        TokenResultDTO tokenResult = ticketAvailabilityTokenBucket.takeTokenFromBucket(requestParam);
        if (tokenResult.getTokenIsNull() && !tokenIsNullRefreshToken(requestParam, tokenResult)) {
            // 获取不到令牌，重新加载令牌，如果上锁失败，或者数据库中实际的车票不满足本次购票需求，则抛异常
                throw new ServiceException("列车站点已无余票");
        }
        // 本地锁和分布式锁
        List<ReentrantLock> localLockList = new ArrayList<>();
        List<RLock> distributedLockList = new ArrayList<>();
        // 计算出需要获取的锁
        addLock(requestParam, localLockList, distributedLockList);
        // 上锁
        try {
            localLockList.forEach(ReentrantLock::lock);
            distributedLockList.forEach(RLock::lock);
            return ticketService.executePurchaseTickets(requestParam);
        } finally {
            localLockList.forEach(localLock -> {
                try {
                    localLock.unlock();
                } catch (Throwable ignored) {
                }
            });
            distributedLockList.forEach(distributedLock -> {
                try {
                    distributedLock.unlock();
                } catch (Throwable ignored) {
                }
            });
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public TicketPurchaseRespDTO executePurchaseTickets(PurchaseTicketReqDTO requestParam) {
        String trainId = requestParam.getTrainId();
        // 订单结果
        List<TicketOrderDetailRespDTO> ticketOrderDetailResults = new ArrayList<>();
        // 获取车次信息
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + trainId,
                TrainDO.class,
                () -> trainMapper.selectById(trainId),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS);
        // 选座
        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = trainSeatTypeSelector.select(trainDO.getTrainType(), requestParam);
        // 保存车票信息
        saveTicketInformation(requestParam, trainPurchaseTicketResults);
        // 调用远程订单服务新增订单
        String orderSn = callRemoteOrderService(requestParam, ticketOrderDetailResults, trainDO, trainPurchaseTicketResults);
        return new TicketPurchaseRespDTO(orderSn, ticketOrderDetailResults);
    }

    public void cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {
        try {
            // 远程调用order模块关闭订单
            Result<Boolean> cancelOrderResult = orderRemoteService.cancelTicketOrder(new CancelTicketOrderReqDTO(requestParam.getOrderSn()));
            if (!cancelOrderResult.isSuccess()) throw new ServiceException("延迟关闭订单出错");
        } catch (Throwable ex) {
            log.error("[延迟关闭订单] 订单号：{} 远程调用订单服务失败", requestParam.getOrderSn(), ex);
            throw ex;
        }
        if (!StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            // 如果没开启binlog，手动进行数据回滚
            Result<com.grace.train12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO> ticketOrderDetailResult = orderRemoteService.queryTicketOrderByOrderSn(requestParam.getOrderSn());
            com.grace.train12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO ticketOrderDetail = ticketOrderDetailResult.getData();
            String trainId = String.valueOf(ticketOrderDetail.getTrainId());
            String departure = ticketOrderDetail.getDeparture();
            String arrival = ticketOrderDetail.getArrival();
            List<TicketOrderPassengerDetailRespDTO> trainPurchaseTicketResults = ticketOrderDetail.getPassengerDetails();
            try {
                // 解锁车票
                seatService.unlock(trainId, departure, arrival, BeanUtil.convert(trainPurchaseTicketResults, TrainPurchaseTicketRespDTO.class));
            } catch (Throwable ex) {
                log.error("[取消订单] 订单号：{} 回滚列车DB座位状态失败", requestParam.getOrderSn(), ex);
                throw ex;
            }
            try {
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // 更新缓存余票
                Map<Integer, List<TicketOrderPassengerDetailRespDTO>> seatTypeMap = trainPurchaseTicketResults.stream()
                        .collect(Collectors.groupingBy(TicketOrderPassengerDetailRespDTO::getSeatType));
                List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);
                routeDTOList.forEach(each -> {
                    String keySuffix = StrUtil.join("_", trainId, each.getStartStation(), each.getEndStation());
                    seatTypeMap.forEach((seatType, ticketOrderPassengerDetailRespDTOList) -> {
                        stringRedisTemplate.opsForHash()
                                .increment(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType), ticketOrderPassengerDetailRespDTOList.size());
                    });
                });
                // 回滚令牌
                ticketAvailabilityTokenBucket.rollbackInBucket(ticketOrderDetail);
            } catch (Throwable ex) {
                log.error("[取消关闭订单] 订单号：{} 回滚列车Cache余票失败", requestParam.getOrderSn(), ex);
                throw ex;
            }
        }
    }

    public RefundTicketRespDTO commonTicketRefund(RefundTicketReqDTO requestParam) {
        // 责任链模式进行前置处理
        refundReqDTOAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_REFUND_TICKET_FILTER.name(), requestParam);
        // 远程调用Order获取到订单乘车人信息
        List<TicketOrderPassengerDetailRespDTO> passengerDetails = getRemoteTicketOrderPassengerDetail(requestParam);
        // 封装退款请求
        RefundReqDTO refundReqDTO = getRefundReqDto(requestParam, passengerDetails);
        // 远程调用pay模块
        Result<RefundRespDTO> refundRespDTOResult = payRemoteService.commonRefund(refundReqDTO);
        if (!refundRespDTOResult.isSuccess() && Objects.isNull(refundRespDTOResult.getData())) {
            throw new ServiceException("车票订单退款失败");
        }
        return null; // 暂时返回空实体
    }

    private List<String> buildDepartureStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getDeparture).distinct().collect(Collectors.toList());
    }

    private List<String> buildArrivalStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getArrival).distinct().collect(Collectors.toList());
    }

    private List<Integer> buildSeatClassList(List<TicketListDTO> seatResults) {
        Set<Integer> resultSeatClassList = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            for (SeatClassDTO item : each.getSeatClassList()) {
                resultSeatClassList.add(item.getType());
            }
        }
        return resultSeatClassList.stream().toList();
    }

    private List<Integer> buildTrainBrandList(List<TicketListDTO> seatResults) {
        Set<Integer> trainBrandSet = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            if (StrUtil.isNotBlank(each.getTrainBrand())) {
                trainBrandSet.addAll(StrUtil.split(each.getTrainBrand(), ",").stream().map(Integer::parseInt).toList());
            }
        }
        return trainBrandSet.stream().toList();
    }

    /**
     * 保存车票信息
     */
    private void saveTicketInformation(PurchaseTicketReqDTO requestParam, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults) {
        List<TicketDO> ticketDOList = trainPurchaseTicketResults.stream()
                .map(each -> TicketDO.builder()
                        .username(UserContext.getUsername())
                        .trainId(Long.parseLong(requestParam.getTrainId()))
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .passengerId(each.getPassengerId())
                        .ticketStatus(TicketStatusEnum.UNPAID.getCode())
                        .build())
                .toList();
        saveBatch(ticketDOList);
    }

    /**
     * 计算需要获取的车票锁，包括本地锁和分布式锁
     */
    private void addLock(PurchaseTicketReqDTO requestParam, List<ReentrantLock> localLockList, List<RLock> distributedLockList) {
        // 始发站和终点站的序列号
        int[] beginAndEndSequence = trainStationService.getBeginAndEndSequence(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());
        // key：座位类型，value：乘客购票信息
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        // 对于每个座位类型，添加锁
        seatTypeMap.forEach((searType, count) -> {
            // 每种座位类型，根据始发站和终点站序号加锁，没有重叠区间的购买请求可以并发
            for (int i = beginAndEndSequence[0] + 1; i <= beginAndEndSequence[1]; i++) {
                String lockKey = environment.resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS, requestParam.getTrainId(), searType, i));
                ReentrantLock localLock = localLockMap.getIfPresent(lockKey);
                if (localLock == null) {
                    synchronized (TicketService.class) {
                        if ((localLock = localLockMap.getIfPresent(lockKey)) == null) {
                            localLock = new ReentrantLock(true);
                            localLockMap.put(lockKey, localLock);
                        }
                    }
                }
                localLockList.add(localLock);
                RLock distributedLock = redissonClient.getFairLock(lockKey);
                distributedLockList.add(distributedLock);
            }
        });
    }

    /**
     * 从缓存中获取到始发站和终点站对应的地区，如果没有，则从数据库中读取添加至缓存
     */
    private List<Object> safeGetRegion(TicketPageQueryReqDTO requestParam) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        List<Object> stationDetails = stringRedisTemplate.opsForHash()
                .multiGet(REGION_TRAIN_STATION_MAPPING, Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
        long count = stationDetails.stream().filter(Objects::isNull).count();
        if (count > 0) {
            // 如果有没查到的数据，将地区映射表添加到缓存
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION_MAPPING);
            lock.lock();
            try {
                // 双重if检验
                stationDetails = stringRedisTemplate.opsForHash()
                        .multiGet(REGION_TRAIN_STATION_MAPPING, Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
                count = stationDetails.stream().filter(Objects::isNull).count();
                if (count > 0) {
                    List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
                    Map<String, String> regionTrainStationMap = new HashMap<>();
                    stationDOList.forEach(each -> regionTrainStationMap.put(each.getCode(), each.getRegionName()));
                    stringRedisTemplate.opsForHash().putAll(REGION_TRAIN_STATION_MAPPING, regionTrainStationMap);
                    stationDetails = new ArrayList<>();
                    stationDetails.add(regionTrainStationMap.get(requestParam.getFromStation()));
                    stationDetails.add(regionTrainStationMap.get(requestParam.getToStation()));
                }
            } finally {
                lock.unlock();
            }
        }
        return stationDetails;
    }

    /**
     * 调用远程订单服务
     */
    private String callRemoteOrderService(PurchaseTicketReqDTO requestParam, List<TicketOrderDetailRespDTO> ticketOrderDetailResults, TrainDO trainDO, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults) {
        Result<String> ticketOrderResult;
        try {
            List<TicketOrderItemCreateRemoteReqDTO> orderItemCreateRemoteReqDTOList = new ArrayList<>();
            trainPurchaseTicketResults.forEach(each -> {
                TicketOrderItemCreateRemoteReqDTO orderItemCreateRemoteReqDTO = TicketOrderItemCreateRemoteReqDTO.builder()
                        .amount(each.getAmount())
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .idCard(each.getIdCard())
                        .idType(each.getIdType())
                        .phone(each.getPhone())
                        .seatType(each.getSeatType())
                        .ticketType(each.getUserType())
                        .realName(each.getRealName())
                        .build();
                TicketOrderDetailRespDTO ticketOrderDetailRespDTO = TicketOrderDetailRespDTO.builder()
                        .amount(each.getAmount())
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .idCard(each.getIdCard())
                        .idType(each.getIdType())
                        .seatType(each.getSeatType())
                        .ticketType(each.getUserType())
                        .realName(each.getRealName())
                        .build();
                orderItemCreateRemoteReqDTOList.add(orderItemCreateRemoteReqDTO);
                ticketOrderDetailResults.add(ticketOrderDetailRespDTO);
            });
            LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, requestParam.getTrainId())
                    .eq(TrainStationRelationDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationRelationDO::getArrival, requestParam.getArrival());
            TrainStationRelationDO trainStationRelationDO = trainStationRelationMapper.selectOne(queryWrapper);
            TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO = TicketOrderCreateRemoteReqDTO.builder()
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderTime(new Date())
                    .source(SourceEnum.INTERNET.getCode())
                    .trainNumber(trainDO.getTrainNumber())
                    .departureTime(trainStationRelationDO.getDepartureTime())
                    .arrivalTime(trainStationRelationDO.getArrivalTime())
                    .ridingDate(trainStationRelationDO.getDepartureTime())
                    .userId(UserContext.getUserId())
                    .username(UserContext.getUsername())
                    .trainId(Long.parseLong(requestParam.getTrainId()))
                    .ticketOrderItems(orderItemCreateRemoteReqDTOList)
                    .build();
            ticketOrderResult = orderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
            if (!ticketOrderResult.isSuccess() || StrUtil.isBlank(ticketOrderResult.getData())) {
                log.error("订单服务调用失败，返回结果：{}", ticketOrderResult.getMessage());
                throw new ServiceException("订单服务调用失败");
            }
        } catch (Throwable ex) {
            log.error("远程调用订单服务创建错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
            throw ex;
        }
        return ticketOrderResult.getData();
    }

    /**
     * 从缓存中获取到始发地区和终点地区所有的车次，如果没有，则从数据库中读取添加至缓存
     */
    private Map<Object, Object> addRegionTrainStation(List<Object> stationDetails, List<TicketListDTO> seatResults, String buildRegionTrainStationHashKey) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        Map<Object, Object> regionTrainStationAllMap;
        RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION);
        lock.lock();
        try {
            regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
            // 双重if判断
            if (MapUtil.isEmpty(regionTrainStationAllMap)) {
                // 根据起始地区和终点地区查询，而非起始站点和终点站点
                LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                        .eq(TrainStationRelationDO::getStartRegion, stationDetails.get(0))
                        .eq(TrainStationRelationDO::getEndRegion, stationDetails.get(1));
                List<TrainStationRelationDO> trainStationRelationList = trainStationRelationMapper.selectList(queryWrapper);
                for (TrainStationRelationDO each : trainStationRelationList) {
                    TrainDO trainDO = distributedCache.safeGet(
                            TRAIN_INFO + each.getTrainId(),
                            TrainDO.class,
                            () -> trainMapper.selectById(each.getTrainId()),
                            ADVANCE_TICKET_DAY,
                            TimeUnit.DAYS);
                    TicketListDTO result = new TicketListDTO();
                    result.setTrainId(String.valueOf(trainDO.getId()));
                    result.setTrainNumber(trainDO.getTrainNumber());
                    result.setDepartureTime(convertDateToLocalTime(each.getDepartureTime(), "HH:mm"));
                    result.setArrivalTime(convertDateToLocalTime(each.getArrivalTime(), "HH:mm"));
                    result.setDuration(DateUtil.calculateHourDifference(each.getDepartureTime(), each.getArrivalTime()));
                    result.setDeparture(each.getDeparture());
                    result.setArrival(each.getArrival());
                    result.setDepartureFlag(each.getDepartureFlag());
                    result.setArrivalFlag(each.getArrivalFlag());
                    result.setTrainType(trainDO.getTrainType());
                    result.setTrainBrand(trainDO.getTrainBrand());
                    if (StrUtil.isNotBlank(trainDO.getTrainTag())) {
                        result.setTrainTags(StrUtil.split(trainDO.getTrainTag(), ","));
                    }
                    long betweenDay = cn.hutool.core.date.DateUtil.betweenDay(each.getDepartureTime(), each.getArrivalTime(), false);
                    result.setDaysArrived((int) betweenDay);
                    result.setSaleStatus(new Date().after(trainDO.getSaleTime()) ? 0 : 1);
                    result.setSaleTime(convertDateToLocalTime(trainDO.getSaleTime(), "MM-dd HH:mm"));
                    seatResults.add(result);
                    // key:trainID_出发站_终点站，value:车次信息
                    regionTrainStationAllMap.put(CacheUtil.buildKey(String.valueOf(each.getTrainId()), each.getDeparture(), each.getArrival()), JSON.toJSONString(result));
                }
                stringRedisTemplate.opsForHash().putAll(buildRegionTrainStationHashKey, regionTrainStationAllMap);
            }
        } finally {
            lock.unlock();
        }
        return regionTrainStationAllMap;
    }

    /**
     * 得到车次车票信息
     */
    private List<TicketListDTO> safeGetTicketInformation(List<Object> stationDetails, List<TicketListDTO> seatResults) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        String buildRegionTrainStationHashKey = String.format(REGION_TRAIN_STATION, stationDetails.get(0), stationDetails.get(1));
        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
        if (MapUtil.isEmpty(regionTrainStationAllMap)) {
            // 如果缓存中没有数据，加载数据到缓存
            // 查询北京到南京的车票，会查询北京所有的车站到南京所有的车站，如北京南到南京南，北京到南京
            regionTrainStationAllMap = addRegionTrainStation(stationDetails, seatResults, buildRegionTrainStationHashKey);
        }
        seatResults = CollUtil.isEmpty(seatResults)
                ? regionTrainStationAllMap.values().stream().map(each -> JSON.parseObject(each.toString(), TicketListDTO.class)).toList()
                : seatResults;
        seatResults = seatResults.stream().sorted(new TimeStringComparator()).toList();
        return seatResults;
    }

    /**
     * 填充价格和余票
     */
    private void fillSeatPriceAndRemain(List<TicketListDTO> seatResults) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        for (TicketListDTO each : seatResults) {
            //计算每个车次的不同座位类型的价格、余票
            String trainStationPriceStr = distributedCache.safeGet(
                    String.format(TRAIN_STATION_PRICE, each.getTrainId(), each.getDeparture(), each.getArrival()),
                    String.class,
                    () -> {
                        LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                                .eq(TrainStationPriceDO::getDeparture, each.getDeparture())
                                .eq(TrainStationPriceDO::getArrival, each.getArrival())
                                .eq(TrainStationPriceDO::getTrainId, each.getTrainId());
                        return JSON.toJSONString(trainStationPriceMapper.selectList(trainStationPriceQueryWrapper));
                    },
                    ADVANCE_TICKET_DAY,
                    TimeUnit.DAYS
            );
            List<TrainStationPriceDO> trainStationPriceDOList = JSON.parseArray(trainStationPriceStr, TrainStationPriceDO.class);
            List<SeatClassDTO> seatClassList = new ArrayList<>();
            trainStationPriceDOList.forEach(item -> {
                // 计算车次的每个类型的座位的车票
                String seatType = String.valueOf(item.getSeatType());
                String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
                int quantity = Optional.ofNullable(quantityObj)
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElseGet(() -> {
                            // 如果缓存中不存在余票，从数据库加载至缓存
                            Map<String, String> seatMarginMap = seatMarginCacheLoader.load(String.valueOf(each.getTrainId()), seatType, item.getDeparture(), item.getArrival());
                            return Optional.ofNullable(seatMarginMap.get(String.valueOf(item.getSeatType()))).map(Integer::parseInt).orElse(0);
                        });
                seatClassList.add(new SeatClassDTO(item.getSeatType(), quantity, new BigDecimal(item.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP), false));
            });
            each.setSeatClassList(seatClassList);
        }
    }

    /**
     * 远程调用Order获取到订单乘车人信息
     */
    private List<TicketOrderPassengerDetailRespDTO> getRemoteTicketOrderPassengerDetail(RefundTicketReqDTO requestParam) {
        Result<com.grace.train12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO> orderDetailRespDTOResult = orderRemoteService.queryTicketOrderByOrderSn(requestParam.getOrderSn());
        if (!orderDetailRespDTOResult.isSuccess() && Objects.isNull(orderDetailRespDTOResult.getData())) {
            throw new ServiceException("车票订单不存在");
        }
        List<TicketOrderPassengerDetailRespDTO> passengerDetails = orderDetailRespDTOResult.getData().getPassengerDetails();
        if (CollectionUtil.isEmpty(passengerDetails)) {
            throw new ServiceException("车票子订单不存在");
        }
        return passengerDetails;
    }

    /**
     * 封装退款请求
     */
    private RefundReqDTO getRefundReqDto(RefundTicketReqDTO requestParam, List<TicketOrderPassengerDetailRespDTO> passengerDetails) {
        RefundReqDTO refundReqDTO = new RefundReqDTO();
        if (RefundTypeEnum.PARTIAL_REFUND.getType().equals(requestParam.getType())) {
            TicketOrderItemQueryReqDTO ticketOrderItemQueryReqDTO = new TicketOrderItemQueryReqDTO();
            ticketOrderItemQueryReqDTO.setOrderSn(requestParam.getOrderSn());
            ticketOrderItemQueryReqDTO.setOrderItemRecordIds(requestParam.getSubOrderRecordIdReqList());
            // 根据子订单记录id查询车票子订单详情
            Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById = orderRemoteService.queryTicketItemOrderById(ticketOrderItemQueryReqDTO);
            List<TicketOrderPassengerDetailRespDTO> partialRefundPassengerDetails = passengerDetails.stream()
                    .filter(item -> queryTicketItemOrderById.getData().contains(item))
                    .collect(Collectors.toList());
            refundReqDTO.setRefundTypeEnum(RefundTypeEnum.PARTIAL_REFUND);
            refundReqDTO.setRefundDetailReqDTOList(partialRefundPassengerDetails);
        } else if (RefundTypeEnum.FULL_REFUND.getType().equals(requestParam.getType())) {
            refundReqDTO.setRefundTypeEnum(RefundTypeEnum.FULL_REFUND);
            refundReqDTO.setRefundDetailReqDTOList(passengerDetails);
        }
        if (CollectionUtil.isNotEmpty(passengerDetails)) {
            Integer partialRefundAmount = passengerDetails.stream()
                    .mapToInt(TicketOrderPassengerDetailRespDTO::getAmount)
                    .sum();
            refundReqDTO.setRefundAmount(partialRefundAmount);
        }
        refundReqDTO.setOrderSn(requestParam.getOrderSn());
        return refundReqDTO;
    }

    /**
     * 令牌获取失败，删除token与数据库不一致的key
     */
    private boolean tokenIsNullRefreshToken(PurchaseTicketReqDTO requestParam, TokenResultDTO tokenResult) {
        boolean hasTicket = true;
        // 上分布式锁
        RLock lock = redissonClient.getLock(String.format(LOCK_TOKEN_BUCKET_ISNULL, requestParam.getTrainId()));
        if (!lock.tryLock()) {
            return false;
        }
        try {
            // 选票的座位类型和对应需求数量
            Map<Integer, Long> passengersSelectTypeAndCount = requestParam.getPassengers().stream().collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType, Collectors.counting()));
            // redis中每个座位类型的令牌数量
            Map<Integer, Integer> tokenCountMap = new HashMap<>();
            tokenResult.getTokenIsNullSeatTypeCounts().stream()
                    .map(each -> each.split("_"))
                    .forEach(split -> {
                        tokenCountMap.put(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                    });
            // 数据库中实际的座位类型对应的车票数量
            List<SeatTypeCountDTO> seatTypeCountDTOList = seatService.listSeatTypeCount(Long.parseLong(requestParam.getTrainId()), requestParam.getDeparture(), requestParam.getArrival(), tokenCountMap.keySet().stream().toList());
            for (SeatTypeCountDTO each : seatTypeCountDTOList) {
                if (tokenCountMap.get(each.getSeatType()) < each.getSeatCount()) {
                    // 缓存中的令牌数小于实际票数，直接把令牌删了
                    ticketAvailabilityTokenBucket.delTokenInBucket(requestParam.getTrainId());
                }
                if (passengersSelectTypeAndCount.get(each.getSeatType()) > each.getSeatCount()) {
                    // 数据库中的票一旦有至少一种座位类型不满足选座需求，就返回false
                    hasTicket = false;
                }
            }
            return hasTicket;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        ticketService = ApplicationContextHolder.getBean(TicketService.class);
    }
}
