package com.bodyplan.module.fitness.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.module.fitness.dto.FitnessTestResponse;
import com.bodyplan.module.fitness.dto.FitnessTestSaveRequest;
import com.bodyplan.module.fitness.service.FitnessTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 体能测试接口：录入测试并按固定规则产出 L1/L2/L3 等级。
 * 全部接口都基于当前登录用户（JWT），不接收 userId 参数。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fitness-tests")
@Tag(name = "体能测试", description = "体能测试录入、最近一次记录与历史列表")
public class FitnessTestController {

    private final FitnessTestService fitnessTestService;

    @PostMapping
    @Operation(summary = "录入体能测试", description = "俯卧撑/深蹲/卷腹/上五楼状态折算等级，测试时间取当前时间")
    public ApiResponse<FitnessTestResponse> saveTest(@Valid @RequestBody FitnessTestSaveRequest request) {
        return ApiResponse.success(fitnessTestService.saveTest(request));
    }

    @GetMapping("/latest")
    @Operation(summary = "查询最近一次体能测试", description = "按测试时间倒序取最近一条，无记录时返回 FITNESS_TEST_NOT_FOUND")
    public ApiResponse<FitnessTestResponse> getLatestTest() {
        return ApiResponse.success(fitnessTestService.getLatestTest());
    }

    @GetMapping
    @Operation(summary = "查询体能测试历史", description = "按测试时间倒序，默认 10 条，最多 100 条")
    public ApiResponse<List<FitnessTestResponse>> listTests(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        return ApiResponse.success(fitnessTestService.listTests(limit));
    }
}
