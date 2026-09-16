package com.bodyplan.module.training.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.module.training.dto.PlanGenerateRequest;
import com.bodyplan.module.training.dto.RecommendReasonResponse;
import com.bodyplan.module.training.dto.TrainingPlanResponse;
import com.bodyplan.module.training.dto.TrainingPlanSummaryResponse;
import com.bodyplan.module.training.service.TrainingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 训练计划：生成、当前计划、详情、历史与推荐依据。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final TrainingPlanService trainingPlanService;

    @PostMapping("/generate")
    public ApiResponse<TrainingPlanResponse> generate(@Valid @RequestBody(required = false) PlanGenerateRequest request) {
        return ApiResponse.success(trainingPlanService.generate(request));
    }

    @GetMapping("/current")
    public ApiResponse<TrainingPlanResponse> current() {
        return ApiResponse.success(trainingPlanService.current());
    }

    @GetMapping("/current/reason")
    public ApiResponse<RecommendReasonResponse> currentReason() {
        return ApiResponse.success(trainingPlanService.currentReason());
    }

    @GetMapping("/history")
    public ApiResponse<List<TrainingPlanSummaryResponse>> history() {
        return ApiResponse.success(trainingPlanService.history());
    }

    @GetMapping("/{id}")
    public ApiResponse<TrainingPlanResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(trainingPlanService.detail(id));
    }
}
