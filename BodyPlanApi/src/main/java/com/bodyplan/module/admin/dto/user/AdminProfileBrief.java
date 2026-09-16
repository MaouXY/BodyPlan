package com.bodyplan.module.admin.dto.user;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端用户详情中的建档要点。
 * 数据库中逗号分隔的字段在此还原为数组，便于前端直接渲染。
 *
 * @param gender       性别：MALE / FEMALE / UNKNOWN
 * @param age          年龄
 * @param heightCm     身高（cm）
 * @param weightKg     体重（kg）
 * @param bodyType     身体类型
 * @param primaryGoal  主目标
 * @param focusParts   重点部位数组
 * @param sportTypes   偏好运动类型数组
 * @param weeklyDays   每周训练日数组（如 ["1","3","5"]）
 * @param dailyMinutes 每日运动时长（分钟）
 * @param injuries     伤病数组
 * @param trainLimits  训练限制数组
 */
public record AdminProfileBrief(
        String gender,
        Integer age,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String bodyType,
        String primaryGoal,
        List<String> focusParts,
        List<String> sportTypes,
        List<String> weeklyDays,
        Integer dailyMinutes,
        List<String> injuries,
        List<String> trainLimits
) {
}
