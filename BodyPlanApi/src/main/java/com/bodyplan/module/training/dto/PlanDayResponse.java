package com.bodyplan.module.training.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 计划中的一天：weekdayLabel 为“周一”这类中文标签，便于 App 直接展示。
 */
public record PlanDayResponse(
        Integer dayIndex,
        LocalDate trainingDate,
        String weekdayLabel,
        List<PlanItemResponse> items
) {
}
