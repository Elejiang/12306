package com.grace.train12306.biz.ticketservice.test;

import com.grace.train12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.StationQueryRespDTO;
import com.grace.train12306.biz.ticketservice.service.RegionStationService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    public void test1() {
        long begin = System.currentTimeMillis();
        RegionStationQueryReqDTO request = new RegionStationQueryReqDTO();
//        request.setName("北京");
        request.setQueryType(1);
        List<RegionStationQueryRespDTO> regionStationQueryRespDTOS = regionStationService.listRegionStation(request);
        System.out.println(System.currentTimeMillis() - begin);
        for (RegionStationQueryRespDTO regionStationQueryRespDTO : regionStationQueryRespDTOS) {
            System.out.println(regionStationQueryRespDTO.toString());
        }
    }
}
