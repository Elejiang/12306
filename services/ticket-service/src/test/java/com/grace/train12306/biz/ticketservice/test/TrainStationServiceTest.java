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
        int[] beginAndEndSequence = trainStationService.getBeginAndEndSequence("1", "北京南", "杭州东");
        int begin = beginAndEndSequence[0];
        int end = beginAndEndSequence[1];
        for (int i = begin + 1; i <= end; i++) {
            String lockKey = environment.resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS, "1", 1, i));
            System.out.println(lockKey);
        }
        System.out.println(begin + " " + end);

    }
}
