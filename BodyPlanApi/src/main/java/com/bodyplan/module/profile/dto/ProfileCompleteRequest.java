package com.bodyplan.module.profile.dto;

import com.bodyplan.module.fitness.dto.FitnessTestSaveRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProfileCompleteRequest(
        @NotNull @Valid ProfileSaveRequest profile,
        @NotNull @Valid FitnessTestSaveRequest fitness
) {
}
