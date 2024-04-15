package com.grace.train12306.biz.ticketservice.toolkit;

import com.grace.train12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 座位号转换工具
 */
public final class SeatNumberUtil {

    /**
     * 高铁-商务座
     */
    private static final Map<Integer, String> TRAIN_BUSINESS_CLASS_SEAT_NUMBER_MAP = new HashMap<>();

    /**
     * 高铁-一等座
     */
    private static final Map<Integer, String> TRAIN_FIRST_CLASS_SEAT_NUMBER_MAP = new HashMap<>();

    /**
     * 高铁-二等座
     */
    private static final Map<Integer, String> TRAIN_SECOND_CLASS_SEAT_NUMBER_MAP = new HashMap<>();

    static {
        TRAIN_BUSINESS_CLASS_SEAT_NUMBER_MAP.put(1, "A");
        TRAIN_BUSINESS_CLASS_SEAT_NUMBER_MAP.put(2, "C");
        TRAIN_BUSINESS_CLASS_SEAT_NUMBER_MAP.put(3, "F");
        TRAIN_FIRST_CLASS_SEAT_NUMBER_MAP.put(1, "A");
        TRAIN_FIRST_CLASS_SEAT_NUMBER_MAP.put(2, "C");
        TRAIN_FIRST_CLASS_SEAT_NUMBER_MAP.put(3, "D");
        TRAIN_FIRST_CLASS_SEAT_NUMBER_MAP.put(4, "F");
        TRAIN_SECOND_CLASS_SEAT_NUMBER_MAP.put(1, "A");
        TRAIN_SECOND_CLASS_SEAT_NUMBER_MAP.put(2, "B");
        TRAIN_SECOND_CLASS_SEAT_NUMBER_MAP.put(3, "C");
        TRAIN_SECOND_CLASS_SEAT_NUMBER_MAP.put(4, "D");
        TRAIN_SECOND_CLASS_SEAT_NUMBER_MAP.put(5, "F");
    }

    /**
     * 根据类型转换座位号
     *
     * @param type 列车座位类型：0商务座，1一等座，2二等座
     * @param num  座位号：1，2，3，4，5
     * @return 座位编号
     */
    public static String convert(int type, int num) {
        String serialNumber = null;
        switch (Objects.requireNonNull(VehicleSeatTypeEnum.getByCode(type))) {
            case BUSINESS_CLASS -> serialNumber = TRAIN_BUSINESS_CLASS_SEAT_NUMBER_MAP.get(num);
            case FIRST_CLASS -> serialNumber = TRAIN_FIRST_CLASS_SEAT_NUMBER_MAP.get(num);
            case SECOND_CLASS -> serialNumber = TRAIN_SECOND_CLASS_SEAT_NUMBER_MAP.get(num);
        }
        return serialNumber;
    }
}
