package com.grace.train12306.biz.ticketservice.common.enums;

import cn.hutool.core.collection.ListUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.grace.train12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum.*;

/**
 * 交通工具类型
 */
@RequiredArgsConstructor
public enum VehicleTypeEnum {

    /**
     * 高铁
     */
    HIGH_SPEED_RAIN(0, "HIGH_SPEED_RAIN", "高铁", ListUtil.of(BUSINESS_CLASS.getCode(), FIRST_CLASS.getCode(), SECOND_CLASS.getCode())),

    /**
     * 动车
     */
    BULLET(1, "BULLET", "动车", ListUtil.of(SECOND_CLASS_CABIN_SEAT.getCode(), FIRST_SLEEPER.getCode(), SECOND_SLEEPER.getCode(), NO_SEAT_SLEEPER.getCode())),

    /**
     * 普通车
     */
    REGULAR_TRAIN(2, "REGULAR_TRAIN", "普通车", ListUtil.of(SOFT_SLEEPER.getCode(), HARD_SLEEPER.getCode(), HARD_SEAT.getCode(), NO_SEAT_SLEEPER.getCode())),

    /**
     * 汽车
     */
    CAR(3, "CAR", "汽车", null),

    /**
     * 飞机
     */
    AIRPLANE(4, "AIRPLANE", "飞机", null);

    @Getter
    private final Integer code;

    @Getter
    private final String name;

    @Getter
    private final String value;

    @Getter
    private final List<Integer> seatTypes;

    /**
     * 根据编码查找名称
     */
    public static String findNameByCode(Integer code) {
        return Arrays.stream(VehicleTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(VehicleTypeEnum::getName)
                .orElse(null);
    }

    /**
     * 根据编码查找座位类型集合
     */
    public static List<Integer> findSeatTypesByCode(Integer code) {
        return Arrays.stream(VehicleTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(VehicleTypeEnum::getSeatTypes)
                .orElse(null);
    }

    /**
     * 根据code获取到枚举对象
     */
    public static VehicleTypeEnum getByCode(int code) {
        for (VehicleTypeEnum status : VehicleTypeEnum.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
