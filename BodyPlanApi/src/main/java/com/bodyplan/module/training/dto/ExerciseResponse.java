package com.bodyplan.module.training.dto;

import java.util.List;

public record ExerciseResponse(
        Long id,
        String name,
        String part,
        String sportType,
        Integer difficulty,
        String scene,
        String equipment,
        List<String> contraFlags,
        String videoUrl,
        String coverUrl,
        String keyPoints,
        String cautions,
        String suitableFor,
        Integer status,
        Integer sortNo
) {
}
