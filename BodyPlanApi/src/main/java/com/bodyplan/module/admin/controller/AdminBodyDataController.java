package com.bodyplan.module.admin.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.admin.dto.bodydata.AdminBodyDataItemResponse;
import com.bodyplan.module.admin.service.AdminBodyDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：身体数据查看。
 */
@Slf4j
@Tag(name = "管理端-身体数据")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/body-data")
public class AdminBodyDataController {

    private final AdminBodyDataService adminBodyDataService;

    /**
     * 分页查询身体数据记录（按记录时间倒序）。
     */
    @Operation(summary = "分页查询身体数据记录")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<PageResult<AdminBodyDataItemResponse>> listBodyData(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Long userId) {
        PageResult<AdminBodyDataItemResponse> result = adminBodyDataService.listBodyData(page, size, userId);
        return ApiResponse.success(result);
    }
}
