package com.bodyplan.module.training.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 生成计划请求。goal 为空时使用建档问卷的主目标；force=true 表示已有执行中计划时替换它（复测调整场景）。
 */
public record PlanGenerateRequest(
        @Size(max = 30) String goal,
        Boolean force,
        LocalDate startDate
) {
}
