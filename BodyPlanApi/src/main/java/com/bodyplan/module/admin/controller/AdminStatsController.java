package com.bodyplan.module.admin.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.module.admin.dto.stats.AdminCheckinRankingItem;
import com.bodyplan.module.admin.dto.stats.AdminCheckinTrendPoint;
import com.bodyplan.module.admin.dto.stats.AdminOverviewResponse;
import com.bodyplan.module.admin.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：打卡数据统计。
 */
@Slf4j
@Tag(name = "管理端-数据统计")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    /**
     * 数据概览。
     */
    @Operation(summary = "数据概览")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> overview() {
        AdminOverviewResponse result = adminStatsService.overview();
        return ApiResponse.success(result);
    }

    /**
     * 打卡趋势（日期升序，缺失日期补 0）。
     */
    @Operation(summary = "打卡趋势（按天，缺失日期补 0）")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/checkins/trend")
    public ApiResponse<List<AdminCheckinTrendPoint>> checkinTrend(@RequestParam(required = false) Integer days) {
        List<AdminCheckinTrendPoint> result = adminStatsService.checkinTrend(days);
        return ApiResponse.success(result);
    }

    /**
     * 打卡排行榜（按打卡总数倒序）。
     */
    @Operation(summary = "打卡排行榜")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/checkins/ranking")
    public ApiResponse<List<AdminCheckinRankingItem>> checkinRanking(@RequestParam(required = false) Integer limit) {
        List<AdminCheckinRankingItem> result = adminStatsService.checkinRanking(limit);
        return ApiResponse.success(result);
    }
}
