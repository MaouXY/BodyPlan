package com.bodyplan.module.training.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingPlanSummaryResponse(
        Long id,
        String name,
        String goal,
        String level,
        Integer daysPerWeek,
        Integer minutesPerSession,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime createdAt
) {
}
