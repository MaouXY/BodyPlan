package com.bodyplan.module.training.dto;

import java.time.LocalDateTime;

/**
 * 推荐依据：把“为什么推荐这个计划”暴露给 App 展示。
 */
public record RecommendReasonResponse(
        Long planId,
        String reason,
        String matchedRule,
        Integer excludedCount,
        String inputSnapshot,
        LocalDateTime createdAt
) {
}
