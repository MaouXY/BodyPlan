package com.bodyplan.module.admin.dto.user;

import java.time.LocalDate;

/**
 * 管理端用户详情中的训练计划要点。
 *
 * @param planId            计划ID
 * @param name              计划名称
 * @param goal              计划目标
 * @param level             计划难度：L1 / L2 / L3
 * @param daysPerWeek       每周训练天数
 * @param minutesPerSession 每次训练时长（分钟）
 * @param startDate         开始日期
 * @param status            状态：ACTIVE / FINISHED / REPLACED
 * @param reason            推荐依据
 */
public record AdminPlanBrief(
        Long planId,
        String name,
        String goal,
        String level,
        Integer daysPerWeek,
        Integer minutesPerSession,
        LocalDate startDate,
        String status,
        String reason
) {
}
