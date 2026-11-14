package com.bodyplan.module.activity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class SportModels {
    private SportModels() {}

    public record Point(
            @DecimalMin("-90") @DecimalMax("90") double latitude,
            @DecimalMin("-180") @DecimalMax("180") double longitude,
            @Positive long timestampMs,
            @Min(0) int segment,
            @DecimalMin("0") @DecimalMax("50") double accuracy) {}

    public record Save(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{16,64}") String clientRecordId,
            @NotBlank String sportType,
            @NotBlank String trainingMode,
            @Min(100) @Max(200000) Integer targetDistanceM,
            @Min(120) @Max(3600) Integer targetPaceSec,
            @Positive long startedAt,
            @Positive long endedAt,
            @Min(1) @Max(86400) int durationSec,
            @NotNull @Size(max = 30000) List<@NotNull @Valid Point> points,
            @Size(max = 255) String remark) {}

    public record Summary(Long id, String clientRecordId, String sportType, String trainingMode,
                          Integer targetDistanceM, Integer targetPaceSec, long startedAt, long endedAt,
                          int durationSec, BigDecimal distanceM, Integer paceSecPerKm,
                          BigDecimal averageSpeedKmh, int pointCount, boolean targetReached, String remark) {}

    public record Detail(Summary summary, List<Point> points) {}
}
