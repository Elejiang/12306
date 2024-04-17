package com.grace.train12306.biz.ticketservice.service.handler.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 令牌扣减返回参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResultDTO {

    /**
     * Token 为空
     */
    private Boolean tokenIsNull;

    /**
     * 获取 Token 为空站点座位类型和数量
     */
    private List<String> tokenIsNullSeatTypeCounts;
}
