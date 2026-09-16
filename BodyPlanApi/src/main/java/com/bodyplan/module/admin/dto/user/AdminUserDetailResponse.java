package com.bodyplan.module.admin.dto.user;

import java.util.List;

/**
 * 管理端用户详情聚合响应。
 *
 * @param user              用户基础信息（与列表元素同结构）
 * @param profile           建档要点，未建档为 null
 * @param latestBodyData    最近一次身体数据，无记录为 null
 * @param bodyDataHistory   身体数据历史（按时间倒序，最多 10 条）
 * @param latestFitnessTest 最近一次体能测试，无记录为 null
 * @param activePlan        当前执行中的计划，无则为 null
 * @param checkinStats      打卡统计
 */
public record AdminUserDetailResponse(
        AdminUserItemResponse user,
        AdminProfileBrief profile,
        AdminBodyDataBrief latestBodyData,
        List<AdminBodyDataBrief> bodyDataHistory,
        AdminFitnessTestBrief latestFitnessTest,
        AdminPlanBrief activePlan,
        AdminCheckinStatsBrief checkinStats
) {
}
