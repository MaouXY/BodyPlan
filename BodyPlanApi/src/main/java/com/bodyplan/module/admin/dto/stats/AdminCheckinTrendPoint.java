package com.bodyplan.module.admin.dto.stats;

/**
 * 管理端打卡趋势点（按日期升序，无数据的日期补 0）。
 *
 * @param date            日期，格式 yyyy-MM-dd
 * @param checkinCount    当日打卡记录数
 * @param activeUserCount 当日打卡的去重用户数
 * @param newUserCount    当日注册用户数
 */
public record AdminCheckinTrendPoint(
        String date,
        Long checkinCount,
        Long activeUserCount,
        Long newUserCount
) {
}
