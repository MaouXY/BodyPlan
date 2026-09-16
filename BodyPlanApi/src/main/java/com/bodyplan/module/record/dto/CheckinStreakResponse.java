package com.bodyplan.module.record.dto;

/**
 * 连续打卡统计响应（按去重后的 trainingDate 计算，同一天多次打卡只算一天）。
 *
 * @param currentStreak 当前连续打卡天数：今天已打卡则从今天往前算，今天没打卡则从昨天往前算
 * @param maxStreak     历史最长连续打卡天数
 * @param totalDays     有打卡的总天数
 */
public record CheckinStreakResponse(
        Integer currentStreak,
        Integer maxStreak,
        Integer totalDays
) {
}
