package com.bodyplan.module.admin.dto.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端用户详情中的身体数据条目（首条建档数据 / 复测历史）。
 *
 * @param id         记录ID
 * @param heightCm   身高（cm）
 * @param weightKg   体重（kg）
 * @param bmi        BMI
 * @param bodyType   身体类型
 * @param score      身体评分 0-100
 * @param remark     备注
 * @param recordedAt 记录时间
 */
public record AdminBodyDataBrief(
        Long id,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bmi,
        String bodyType,
        Integer score,
        String remark,
        LocalDateTime recordedAt
) {
}
