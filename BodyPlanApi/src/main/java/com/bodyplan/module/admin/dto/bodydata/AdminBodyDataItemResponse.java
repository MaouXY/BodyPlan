package com.bodyplan.module.admin.dto.bodydata;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端身体数据列表元素。
 *
 * @param id         记录ID
 * @param userId     用户ID
 * @param username   登录账号
 * @param nickname   昵称
 * @param heightCm   身高（cm）
 * @param weightKg   体重（kg）
 * @param bmi        BMI
 * @param bodyType   身体类型
 * @param score      身体评分 0-100
 * @param remark     备注
 * @param recordedAt 记录时间
 */
public record AdminBodyDataItemResponse(
        Long id,
        Long userId,
        String username,
        String nickname,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bmi,
        String bodyType,
        Integer score,
        String remark,
        LocalDateTime recordedAt
) {
}
