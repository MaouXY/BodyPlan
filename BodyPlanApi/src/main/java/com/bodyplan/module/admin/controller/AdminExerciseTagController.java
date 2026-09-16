package com.bodyplan.module.admin.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.module.admin.dto.exercise.AdminExerciseTagResponse;
import com.bodyplan.module.admin.dto.exercise.AdminExerciseTagSaveRequest;
import com.bodyplan.module.admin.service.AdminExerciseTagService;
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

import java.util.List;

/**
 * 管理端：动作标签管理。
 */
@Slf4j
@Tag(name = "管理端-动作标签")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/exercise-tags")
public class AdminExerciseTagController {

    private final AdminExerciseTagService adminExerciseTagService;

    /**
     * 标签列表（按 tagType、sortNo 升序）。
     */
    @Operation(summary = "查询动作标签列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<List<AdminExerciseTagResponse>> listTags(@RequestParam(required = false) String tagType) {
        List<AdminExerciseTagResponse> result = adminExerciseTagService.listTags(tagType);
        return ApiResponse.success(result);
    }

    /**
     * 新增标签。
     */
    @Operation(summary = "新增动作标签")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<AdminExerciseTagResponse> createTag(@Valid @RequestBody AdminExerciseTagSaveRequest request) {
        AdminExerciseTagResponse result = adminExerciseTagService.createTag(request);
        return ApiResponse.success(result);
    }

    /**
     * 更新标签。
     */
    @Operation(summary = "更新动作标签")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<AdminExerciseTagResponse> updateTag(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminExerciseTagSaveRequest request) {
        AdminExerciseTagResponse result = adminExerciseTagService.updateTag(id, request);
        return ApiResponse.success(result);
    }

    /**
     * 删除标签（字典项，物理删除）。
     */
    @Operation(summary = "删除动作标签（物理删除）")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable("id") Long id) {
        adminExerciseTagService.deleteTag(id);
        return ApiResponse.success();
    }
}
