package com.grace.train12306.biz.ticketservice.test;

import com.grace.train12306.biz.ticketservice.TicketServiceApplication;
import com.grace.train12306.biz.ticketservice.dto.domain.RouteDTO;
import com.grace.train12306.biz.ticketservice.service.TrainStationService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = TicketServiceApplication.class)
public class TrainStationServiceTest {
    @Resource
    private TrainStationService trainStationService;

    @Test
    public void test1() {
        List<RouteDTO> routeDTOS = trainStationService.listTrainStationRoute("1", "北京南", "宁波");
        for (RouteDTO routeDTO : routeDTOS) {
            System.out.println(routeDTO.getStartStation() + "_" + routeDTO.getEndStation());
        }
    }
}
