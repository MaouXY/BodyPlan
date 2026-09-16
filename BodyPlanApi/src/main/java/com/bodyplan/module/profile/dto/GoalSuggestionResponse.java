package com.bodyplan.module.profile.dto;

import java.util.List;

/**
 * 目标推荐出参（固定规则、可解释）。
 *
 * @param recommendedGoal 推荐主目标编码（与 primaryGoal 取值一致）
 * @param reason          中文解释，说明命中哪条规则
 * @param bmi             当前 BMI，可能为 null（档案缺身高体重）
 * @param bmiLevel        中文分级：偏瘦 / 正常 / 超重 / 肥胖 / 未知
 * @param alternatives    备选目标编码列表（不含推荐目标）
 */
public record GoalSuggestionResponse(
        String recommendedGoal,
        String reason,
        Double bmi,
        String bmiLevel,
        List<String> alternatives
) {
}
