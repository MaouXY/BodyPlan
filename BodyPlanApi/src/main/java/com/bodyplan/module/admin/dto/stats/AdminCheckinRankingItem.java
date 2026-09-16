package com.bodyplan.module.admin.dto.stats;

import java.time.LocalDate;

/**
 * 管理端打卡排行榜条目（按打卡总数倒序）。
 *
 * @param userId          用户ID
 * @param username        登录账号
 * @param nickname        昵称
 * @param checkinCount    打卡总数
 * @param lastCheckinDate 最近打卡日期
 * @param currentStreak   当前连续打卡天数
 */
public record AdminCheckinRankingItem(
        Long userId,
        String username,
        String nickname,
        Long checkinCount,
        LocalDate lastCheckinDate,
        Integer currentStreak
) {
}
