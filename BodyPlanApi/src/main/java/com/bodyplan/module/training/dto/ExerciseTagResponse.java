package com.bodyplan.module.training.dto;

public record ExerciseTagResponse(
        Long id,
        String tagType,
        String tagCode,
        String tagName,
        Integer sortNo
) {
}
