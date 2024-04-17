package com.grace.train12306.biz.ticketservice.test;

import com.grace.train12306.biz.ticketservice.dto.resp.StationQueryRespDTO;
import com.grace.train12306.biz.ticketservice.service.RegionStationService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class RegionStationTest {
    @Resource
    private RegionStationService regionStationService;

    @Test
    public void test() {
        long begin = System.currentTimeMillis();
        List<StationQueryRespDTO> stationQueryRespDTOS = regionStationService.listAllStation();
        System.out.println(System.currentTimeMillis() - begin);
        for (StationQueryRespDTO stationQueryRespDTO : stationQueryRespDTOS) {
            System.out.println(stationQueryRespDTO.toString());
        }
    }
}
