package com.bodyplan.module.admin.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.admin.dto.user.AdminUserDetailResponse;
import com.bodyplan.module.admin.dto.user.AdminUserItemResponse;
import com.bodyplan.module.admin.dto.user.AdminUserStatusRequest;
import com.bodyplan.module.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：用户管理。
 */
@Slf4j
@Tag(name = "管理端-用户管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户列表。
     */
    @Operation(summary = "分页查询用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<PageResult<AdminUserItemResponse>> listUsers(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        PageResult<AdminUserItemResponse> result = adminUserService.listUsers(page, size, keyword, status);
        return ApiResponse.success(result);
    }

    /**
     * 启用 / 禁用用户。
     */
    @Operation(summary = "启用或禁用用户")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserItemResponse> updateUserStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminUserStatusRequest request) {
        AdminUserItemResponse result = adminUserService.updateStatus(id, request.status());
        return ApiResponse.success(result);
    }

    /**
     * 用户详情聚合。
     */
    @Operation(summary = "查询用户详情（档案 + 身体数据 + 体能测试 + 计划 + 打卡统计）")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(@PathVariable("id") Long id) {
        AdminUserDetailResponse result = adminUserService.detail(id);
        return ApiResponse.success(result);
    }
}
