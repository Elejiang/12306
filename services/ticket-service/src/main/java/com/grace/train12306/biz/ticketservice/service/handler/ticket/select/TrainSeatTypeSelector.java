package com.grace.train12306.biz.ticketservice.service.handler.ticket.select;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.grace.train12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.grace.train12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import com.grace.train12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.grace.train12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.grace.train12306.biz.ticketservice.remote.UserRemoteService;
import com.grace.train12306.biz.ticketservice.remote.dto.PassengerRespDTO;
import com.grace.train12306.biz.ticketservice.service.SeatService;
import com.grace.train12306.biz.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.grace.train12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.grace.train12306.framework.starter.convention.exception.RemoteException;
import com.grace.train12306.framework.starter.convention.exception.ServiceException;
import com.grace.train12306.framework.starter.convention.result.Result;
import com.grace.train12306.framework.starter.designpattern.strategy.AbstractStrategyChoose;
import com.grace.train12306.framework.starter.user.core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 购票时列车座位选择器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainSeatTypeSelector {

    private final SeatService seatService;
    private final UserRemoteService userRemoteService;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final ThreadPoolExecutor selectSeatThreadPoolExecutor;

    @Transactional
    public List<TrainPurchaseTicketRespDTO> select(Integer trainType, PurchaseTicketReqDTO requestParam) {
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        // 根据用户购买的座位类型进行分组
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        // 分配好用户相关的座位信息
        List<TrainPurchaseTicketRespDTO> actualResult = selectSeat(trainType, requestParam, passengerDetails, seatTypeMap);
        // 根据乘车人id远程调用获取乘车人信息
        List<String> passengerIds = actualResult.stream()
                .map(TrainPurchaseTicketRespDTO::getPassengerId)
                .collect(Collectors.toList());
        List<PassengerRespDTO> passengerRemoteResultList = getPassengerRemote(passengerIds);
        // 返回结果填写上乘车人信息和价格
        fillInformation(requestParam, actualResult, passengerRemoteResultList);
        // 锁座位
        seatService.lockSeat(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival(), actualResult);
        return actualResult;
    }

    /**
     * 选座
     */
    private List<TrainPurchaseTicketRespDTO> selectSeat(Integer trainType, PurchaseTicketReqDTO requestParam, List<PurchaseTicketPassengerDetailDTO> passengerDetails, Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap) {
        List<TrainPurchaseTicketRespDTO> actualResult = new CopyOnWriteArrayList<>();
        if (seatTypeMap.size() > 1) {
            // 如果选择了多个类型的座位，使用线程池对每个类型座位进行选座
            List<Future<List<TrainPurchaseTicketRespDTO>>> futureResults = new ArrayList<>();
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                Future<List<TrainPurchaseTicketRespDTO>> completableFuture = selectSeatThreadPoolExecutor
                        .submit(() -> distributeSeats(trainType, seatType, requestParam, passengerSeatDetails));
                futureResults.add(completableFuture);
            });
            // 获取座位分配结果
            futureResults.forEach(completableFuture -> {
                try {
                    actualResult.addAll(completableFuture.get());
                } catch (Exception e) {
                    throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
                }
            });
        } else {
            // 选择的是单一类型座位，直接执行选座
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                List<TrainPurchaseTicketRespDTO> aggregationResult = distributeSeats(trainType, seatType, requestParam, passengerSeatDetails);
                actualResult.addAll(aggregationResult);
            });
        }
        if (CollUtil.isEmpty(actualResult) || !Objects.equals(actualResult.size(), passengerDetails.size())) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
        }
        return actualResult;
    }

    private void fillInformation(PurchaseTicketReqDTO requestParam, List<TrainPurchaseTicketRespDTO> actualResult, List<PassengerRespDTO> passengerRemoteResultList) {
        actualResult.forEach(each -> {
            String passengerId = each.getPassengerId();
            passengerRemoteResultList.stream()
                    .filter(item -> Objects.equals(item.getId(), passengerId))
                    .findFirst()
                    .ifPresent(passenger -> {
                        each.setIdCard(passenger.getIdCard());
                        each.setPhone(passenger.getPhone());
                        each.setUserType(passenger.getDiscountType());
                        each.setIdType(passenger.getIdType());
                        each.setRealName(passenger.getRealName());
                    });
            LambdaQueryWrapper<TrainStationPriceDO> lambdaQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                    .eq(TrainStationPriceDO::getTrainId, requestParam.getTrainId())
                    .eq(TrainStationPriceDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationPriceDO::getArrival, requestParam.getArrival())
                    .eq(TrainStationPriceDO::getSeatType, each.getSeatType())
                    .select(TrainStationPriceDO::getPrice);
            TrainStationPriceDO trainStationPriceDO = trainStationPriceMapper.selectOne(lambdaQueryWrapper);
            each.setAmount(trainStationPriceDO.getPrice());
        });
    }

    private List<PassengerRespDTO> getPassengerRemote(List<String> passengerIds) {
        List<PassengerRespDTO> passengerRemoteResultList;
        try {
            Result<List<PassengerRespDTO>> passengerRemoteResult = userRemoteService.listPassengerQueryByIds(UserContext.getUsername(), passengerIds);
            if (!passengerRemoteResult.isSuccess() || CollUtil.isEmpty(passengerRemoteResultList = passengerRemoteResult.getData())) {
                throw new RemoteException("用户服务远程调用查询乘车人相信信息错误");
            }
        } catch (Throwable ex) {
            if (ex instanceof RemoteException) {
                log.error("用户服务远程调用查询乘车人相信信息错误，当前用户：{}，请求参数：{}", UserContext.getUsername(), passengerIds);
            } else {
                log.error("用户服务远程调用查询乘车人相信信息错误，当前用户：{}，请求参数：{}", UserContext.getUsername(), passengerIds, ex);
            }
            throw ex;
        }
        return passengerRemoteResultList;
    }

    private List<TrainPurchaseTicketRespDTO> distributeSeats(Integer trainType, Integer seatType, PurchaseTicketReqDTO requestParam, List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails) {
        String buildStrategyKey = VehicleTypeEnum.findNameByCode(trainType) + VehicleSeatTypeEnum.findNameByCode(seatType);
        SelectSeatDTO selectSeatDTO = SelectSeatDTO.builder()
                .seatType(seatType)
                .passengerSeatDetails(passengerSeatDetails)
                .requestParam(requestParam)
                .build();
        try {
            // 执行相关的选座策略，如高铁商务座选座、高铁一等座选座、高铁二等座选座
            return abstractStrategyChoose.chooseAndExecuteResp(buildStrategyKey, selectSeatDTO);
        } catch (ServiceException ex) {
            // TODO 目前只实现了高铁商务座、高铁一等座、高铁二等座的购票逻辑，但采用了策略模式，需要新增逻辑直接实现对应策略即可
            throw new ServiceException("当前车次列车类型暂未适配");
        }
    }
}
