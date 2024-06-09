package com.grace.train12306.biz.ticketservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.grace.train12306.biz.ticketservice.dto.domain.RouteDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import com.grace.train12306.biz.ticketservice.toolkit.StationCalculateUtil;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 列车站点接口实现层
 */
@Service
@RequiredArgsConstructor
public class TrainStationService {

    private final TrainStationMapper trainStationMapper;

    public List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId);
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        return BeanUtil.convert(trainStationDOList, TrainStationQueryRespDTO.class);
    }

    public List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival) {
        List<String> trainStationAllList = getStationAllList(trainId);
        return StationCalculateUtil.throughStation(trainStationAllList, departure, arrival);
    }

    public List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival) {
        List<String> trainStationAllList = getStationAllList(trainId);
        return StationCalculateUtil.takeoutStation(trainStationAllList, departure, arrival);
    }

    public int[] getBeginAndEndSequence(String trainId, String departure, String arrival) {
        List<String> stationAllList = getStationAllList(trainId);
        int begin = stationAllList.indexOf(departure);
        int end = stationAllList.indexOf(arrival);
        return new int[]{begin, end};
    }

    private List<String> getStationAllList(String trainId) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .orderBy(true, true, TrainStationDO::getSequence)
                .select(TrainStationDO::getDeparture);
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        return trainStationDOList.stream().map(TrainStationDO::getDeparture).collect(Collectors.toList());
    }
}
