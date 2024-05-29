package com.grace.train12306.biz.ticketservice.test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grace.train12306.biz.ticketservice.TicketServiceApplication;
import com.grace.train12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.grace.train12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.grace.train12306.biz.ticketservice.dto.domain.RouteDTO;
import com.grace.train12306.biz.ticketservice.service.TrainStationService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_PURCHASE_TICKETS;

@SpringBootTest(classes = TicketServiceApplication.class)
public class TrainStationServiceTest {
    @Resource
    private TrainStationService trainStationService;
    @Resource
    private TrainStationMapper trainStationMapper;
    @Resource
    private ConfigurableEnvironment environment;

    @Test
    public void test1() {
        List<RouteDTO> routeDTOS = trainStationService.listTrainStationRoute("1", "北京南", "宁波");
        for (RouteDTO routeDTO : routeDTOS) {
            System.out.println(routeDTO.getStartStation() + "_" + routeDTO.getEndStation());
        }
    }

    @Test
    public void test2() {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, "1")
                .eq(TrainStationDO::getDeparture, "北京南")
                .or()
                .eq(TrainStationDO::getTrainId, "1")
                .eq(TrainStationDO::getArrival, "杭州东")
                .orderBy(true, true, TrainStationDO::getSequence);
        List<TrainStationDO> trainStationDOS = trainStationMapper.selectList(queryWrapper);
        int begin = Integer.parseInt(trainStationDOS.get(0).getSequence());
        int end = Integer.parseInt(trainStationDOS.get(1).getSequence());
        for (int i = begin + 1; i <= end; i++) {
            String lockKey = environment.resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS, "1", 1, i));
            System.out.println(lockKey);
        }
        System.out.println(begin + " " + end);
    }
}
