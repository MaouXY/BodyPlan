package com.bodyplan.module.profile.dto;

import java.time.LocalDateTime;

/**
 * 身体数据出参（含复测历史）。
 *
 * @param id         记录主键
 * @param heightCm   身高（cm）
 * @param weightKg   体重（kg）
 * @param bmi        BMI（保留 1 位小数）
 * @param bodyType   身体类型
 * @param score      身体评分 0-100
 * @param remark     备注，如“建档数据”“两周后复测”
 * @param recordedAt 记录时间
 */
public record BodyDataResponse(
        Long id,
        Double heightCm,
        Double weightKg,
        Double bmi,
        String bodyType,
        Integer score,
        String remark,
        LocalDateTime recordedAt
) {
}
