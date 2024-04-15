package com.grace.train12306.biz.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.util.StrUtil;
import com.grace.train12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.grace.train12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.grace.train12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import com.grace.train12306.framework.starter.cache.DistributedCache;
import com.grace.train12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.grace.train12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 购票流程过滤器之验证列车站点库存是否充足
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketParamStockChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    private final SeatMarginCacheLoader seatMarginCacheLoader;
    private final DistributedCache distributedCache;

    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // 车次站点是否还有余票。如果用户提交多个乘车人非同一座位类型，拆分验证
        String keySuffix = StrUtil.join("_", requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        // key座位类型，value选择该座位类型的乘车人列表
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
            Object stockObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType));
            int stock = Optional.ofNullable(stockObj).map(each -> Integer.parseInt(each.toString())).orElseGet(() -> {
                // 缓存中没加载余票，加载余票至缓存
                Map<String, String> seatMarginMap = seatMarginCacheLoader.load(String.valueOf(requestParam.getTrainId()), String.valueOf(seatType), requestParam.getDeparture(), requestParam.getArrival());
                return Optional.ofNullable(seatMarginMap.get(String.valueOf(seatType))).map(Integer::parseInt).orElse(0);
            });
            if (stock >= passengerSeatDetails.size()) {
                return;
            }
            throw new ClientException("列车站点余票数量不足");
        });
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
