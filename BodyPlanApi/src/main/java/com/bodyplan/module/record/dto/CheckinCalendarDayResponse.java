package com.bodyplan.module.record.dto;

import java.time.LocalDate;

/**
 * 打卡日历中的一天（该月每一天都会返回，没打卡的日期 finishedCount=0、checkedIn=false）。
 *
 * @param date          日期 yyyy-MM-dd
 * @param finishedCount 当天打卡次数
 * @param checkedIn     当天是否有打卡
 */
public record CheckinCalendarDayResponse(
        LocalDate date,
        Integer finishedCount,
        Boolean checkedIn
) {
}
