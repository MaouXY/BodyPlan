package com.bodyplan.module.profile.dto;

import java.util.List;

/**
 * 用户档案出参（建档问卷 + 派生指标）。
 *
 * <p>说明：
 * <ul>
 *   <li>子目标 / 重点部位 / 运动偏好 / 训练日 / 伤病 / 训练限制在库中是逗号分隔字符串，出参统一还原成列表；</li>
 *   <li>bmi 由身高体重实时计算（保留 1 位小数），档案尚未填写身高或体重时为 null；</li>
 *   <li>bmiLevel 为 bmi 的中文分级：偏瘦 / 正常 / 超重 / 肥胖，无 bmi 时为「未知」，便于 App 建档页直接展示；</li>
 *   <li>score 为该用户最近一条 body_data 的身体评分，从未录入过身体数据时为 null。</li>
 * </ul>
 */
public record ProfileResponse(
        Long userId,
        String gender,
        Integer age,
        Double heightCm,
        Double weightKg,
        Double bmi,
        String bmiLevel,
        Integer score,
        String bodyType,
        String primaryGoal,
        List<String> subGoals,
        List<String> focusParts,
        List<String> sportTypes,
        List<Integer> weeklyDays,
        Integer dailyMinutes,
        Integer remindEnabled,
        String remindTime,
        List<String> injuries,
        List<String> trainLimits,
        Integer profileCompleted
) {
}
