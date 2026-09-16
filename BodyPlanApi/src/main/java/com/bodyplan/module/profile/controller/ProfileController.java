package com.bodyplan.module.profile.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.module.profile.dto.BodyDataResponse;
import com.bodyplan.module.profile.dto.BodyDataSaveRequest;
import com.bodyplan.module.profile.dto.GoalSuggestionResponse;
import com.bodyplan.module.profile.dto.ProfileResponse;
import com.bodyplan.module.profile.dto.ProfileSaveRequest;
import com.bodyplan.module.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户档案接口：建档问卷、目标推荐、身体数据复测。
 * 全部接口都基于当前登录用户（JWT），不接收 userId 参数。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profile")
@Tag(name = "用户档案", description = "建档问卷、目标推荐、身体数据复测")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "查询我的档案", description = "档案不存在时惰性创建默认档案（profileCompleted=0）后返回")
    public ApiResponse<ProfileResponse> getMyProfile() {
        return ApiResponse.success(profileService.getMyProfile());
    }

    @PutMapping
    @Operation(
            summary = "保存建档问卷",
            description = "支持分步保存：只覆盖请求里带上的字段（List 传 [] 表示清空），身高体重都齐全时才写入一条身体数据并重算 BMI/评分"
    )
    public ApiResponse<ProfileResponse> saveProfile(@Valid @RequestBody ProfileSaveRequest request) {
        return ApiResponse.success(profileService.saveProfile(request));
    }

    @GetMapping("/goal-suggestion")
    @Operation(summary = "获取训练目标建议", description = "基于当前档案的固定规则推荐主目标，并给出中文理由与备选目标")
    public ApiResponse<GoalSuggestionResponse> getGoalSuggestion() {
        return ApiResponse.success(profileService.suggestGoal());
    }

    @GetMapping("/body-data")
    @Operation(summary = "查询身体数据历史", description = "按记录时间倒序，默认 10 条，最多 100 条")
    public ApiResponse<List<BodyDataResponse>> listBodyData(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        return ApiResponse.success(profileService.listBodyData(limit));
    }

    @PostMapping("/body-data")
    @Operation(summary = "录入复测身体数据", description = "身高为空时取档案身高，BMI/评分重新计算并同步更新档案")
    public ApiResponse<BodyDataResponse> saveBodyData(@Valid @RequestBody BodyDataSaveRequest request) {
        return ApiResponse.success(profileService.saveBodyData(request));
    }
}
