package com.grace.train12306.biz.ticketservice.service;


import com.grace.train12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.grace.train12306.biz.ticketservice.dto.resp.StationQueryRespDTO;

import java.util.List;

/**
 * 地区以及车站接口层
 */
public interface RegionStationService {

    /**
     * 查询车站&城市站点集合信息
     *
     * @param requestParam 车站&站点查询参数
     * @return 车站&站点返回数据集合
     */
    List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam);

    /**
     * 查询所有车站&城市站点集合信息
     *
     * @return 车站返回数据集合
     */
    List<StationQueryRespDTO> listAllStation();
}
