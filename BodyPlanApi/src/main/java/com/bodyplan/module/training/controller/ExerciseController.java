package com.bodyplan.module.training.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.training.dto.ExerciseResponse;
import com.bodyplan.module.training.dto.ExerciseTagResponse;
import com.bodyplan.module.training.service.ExerciseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端动作库与标签字典（只读，仅上架动作）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ExerciseController {

    private final ExerciseQueryService exerciseQueryService;

    @GetMapping("/exercises")
    public ApiResponse<PageResult<ExerciseResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String part,
            @RequestParam(required = false) String sportType,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(exerciseQueryService.list(page, size, part, sportType, scene, equipment, difficulty, keyword));
    }

    @GetMapping("/exercises/{id}")
    public ApiResponse<ExerciseResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(exerciseQueryService.detail(id));
    }

    @GetMapping("/exercise-tags")
    public ApiResponse<List<ExerciseTagResponse>> tags(@RequestParam(required = false) String tagType) {
        return ApiResponse.success(exerciseQueryService.tags(tagType));
    }
}
