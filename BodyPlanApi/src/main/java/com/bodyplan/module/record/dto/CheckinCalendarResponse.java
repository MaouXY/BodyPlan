package com.bodyplan.module.record.dto;

import java.util.List;

/**
 * 打卡日历响应。
 *
 * @param month              月份 yyyy-MM（请求月，缺省为当月）
 * @param days               该月每一天的打卡情况，按日期升序
 * @param monthFinishedCount 该月打卡总次数
 * @param activeDays         该月有打卡的天数
 */
public record CheckinCalendarResponse(
        String month,
        List<CheckinCalendarDayResponse> days,
        Integer monthFinishedCount,
        Integer activeDays
) {
}
