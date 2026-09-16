package com.bodyplan.module.admin.dto.user;

import java.time.LocalDate;

/**
 * 管理端用户详情中的打卡统计。
 *
 * @param totalCount      打卡总数
 * @param todayChecked    今日是否已打卡
 * @param lastCheckinDate 最近打卡日期（无打卡为 null）
 * @param currentStreak   当前连续打卡天数
 */
public record AdminCheckinStatsBrief(
        Long totalCount,
        Boolean todayChecked,
        LocalDate lastCheckinDate,
        Integer currentStreak
) {
}
