package com.bodyplan.module.admin.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.admin.dto.exercise.AdminExerciseResponse;
import com.bodyplan.module.admin.dto.exercise.AdminExerciseSaveRequest;
import com.bodyplan.module.admin.service.AdminExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：动作管理。
 */
@Slf4j
@Tag(name = "管理端-动作管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/exercises")
public class AdminExerciseController {

    private final AdminExerciseService adminExerciseService;

    /**
     * 分页查询动作列表（按 sortNo 升序、id 升序）。
     */
    @Operation(summary = "分页查询动作列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<PageResult<AdminExerciseResponse>> listExercises(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String part,
            @RequestParam(required = false) String sportType,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        PageResult<AdminExerciseResponse> result = adminExerciseService.listExercises(
                page, size, part, sportType, scene, equipment, difficulty, keyword, status);
        return ApiResponse.success(result);
    }

    /**
     * 新增动作。
     */
    @Operation(summary = "新增动作")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<AdminExerciseResponse> createExercise(@Valid @RequestBody AdminExerciseSaveRequest request) {
        AdminExerciseResponse result = adminExerciseService.createExercise(request);
        return ApiResponse.success(result);
    }

    /**
     * 更新动作。
     */
    @Operation(summary = "更新动作")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<AdminExerciseResponse> updateExercise(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminExerciseSaveRequest request) {
        AdminExerciseResponse result = adminExerciseService.updateExercise(id, request);
        return ApiResponse.success(result);
    }

    /**
     * 下架动作（仅置 status=0，不物理删除）。
     */
    @Operation(summary = "下架动作（不物理删除）")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> disableExercise(@PathVariable("id") Long id) {
        adminExerciseService.disableExercise(id);
        return ApiResponse.success();
    }
}
