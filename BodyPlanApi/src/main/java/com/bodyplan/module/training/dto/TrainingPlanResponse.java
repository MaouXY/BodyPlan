package com.bodyplan.module.training.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingPlanResponse(
        Long id,
        String name,
        String goal,
        String level,
        Integer daysPerWeek,
        Integer minutesPerSession,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String reason,
        LocalDateTime createdAt,
        Integer totalItems,
        Integer doneItems,
        List<PlanDayResponse> days
) {
}
