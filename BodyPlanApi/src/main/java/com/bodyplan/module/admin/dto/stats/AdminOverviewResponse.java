package com.bodyplan.module.admin.dto.stats;

/**
 * 管理端数据概览。
 *
 * @param userTotal             用户总数
 * @param profileCompletedTotal 已完成建档的用户数
 * @param planTotal             训练计划总数
 * @param activePlanTotal       执行中（ACTIVE）的训练计划数
 * @param exerciseTotal         动作总数
 * @param exerciseTagTotal      动作标签总数
 * @param checkinTotal          打卡记录总数
 * @param todayCheckinTotal     今日打卡记录数
 * @param activeUserTodayTotal  今日有打卡的去重用户数
 * @param planItemDoneTotal     计划明细已完成数（status=DONE）
 * @param planItemPendingTotal  计划明细未完成数（status=PENDING）
 */
public record AdminOverviewResponse(
        Long userTotal,
        Long profileCompletedTotal,
        Long planTotal,
        Long activePlanTotal,
        Long exerciseTotal,
        Long exerciseTagTotal,
        Long checkinTotal,
        Long todayCheckinTotal,
        Long activeUserTodayTotal,
        Long planItemDoneTotal,
        Long planItemPendingTotal
) {
}
