package com.grace.train12306.biz.ticketservice.common.enums;

import cn.hutool.core.collection.ListUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


import java.util.List;

/**
 * 地区&站点类型枚举
 */
@RequiredArgsConstructor
public enum RegionStationQueryTypeEnum {

    /**
     * 热门查询
     */
    HOT(0, null),

    /**
     * A to E
     */
    A_E(1, ListUtil.of("A", "B", "C", "D", "E")),

    /**
     * F to J
     */
    F_J(2, ListUtil.of("F", "G", "H", "R", "J")),

    /**
     * K to O
     */
    K_O(3, ListUtil.of("K", "L", "M", "N", "O")),

    /**
     * P to T
     */
    P_T(4, ListUtil.of("P", "Q", "R", "S", "T")),

    /**
     * U to Z
     */
    U_Z(5, ListUtil.of("U", "V", "W", "X", "Y", "Z"));

    /**
     * 类型
     */
    @Getter
    private final Integer type;

    /**
     * 拼音列表
     */
    @Getter
    private final List<String> spells;

    /**
     * 根据code获取到枚举对象
     */
    public static RegionStationQueryTypeEnum getByCode(int code) {
        for (RegionStationQueryTypeEnum status : RegionStationQueryTypeEnum.values()) {
            if (status.getType() == code) {
                return status;
            }
        }
        return null;
    }
}
