package com.bodyplan.module.training.dto;

import java.time.LocalDate;

public record PlanItemResponse(
        Long id,
        Integer dayIndex,
        LocalDate trainingDate,
        Long exerciseId,
        String exerciseName,
        Integer sets,
        Integer reps,
        Integer durationSec,
        Integer restSec,
        Integer sortNo,
        String status
) {
}
