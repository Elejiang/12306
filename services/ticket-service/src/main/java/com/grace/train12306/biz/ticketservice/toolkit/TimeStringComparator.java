package com.grace.train12306.biz.ticketservice.toolkit;


import com.grace.train12306.biz.ticketservice.dto.domain.TicketListDTO;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * 自定义时间比较器
 */
public class TimeStringComparator implements Comparator<TicketListDTO> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public int compare(TicketListDTO ticketList1, TicketListDTO ticketList2) {
        LocalTime localTime1 = LocalTime.parse(ticketList1.getDepartureTime(), FORMATTER);
        LocalTime localTime2 = LocalTime.parse(ticketList2.getDepartureTime(), FORMATTER);
        return localTime1.compareTo(localTime2);
    }
}
