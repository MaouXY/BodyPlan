package com.bodyplan.module.record.controller;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.record.dto.CheckinCalendarResponse;
import com.bodyplan.module.record.dto.CheckinResponse;
import com.bodyplan.module.record.dto.CheckinSaveRequest;
import com.bodyplan.module.record.dto.CheckinStreakResponse;
import com.bodyplan.module.record.dto.TodayTaskResponse;
import com.bodyplan.module.record.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打卡记录接口。只做参数透传与统一响应包装，业务逻辑全部在 CheckinService。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/checkins")
@Tag(name = "打卡记录", description = "今日任务、每日打卡、撤销打卡、打卡日历、连续打卡、打卡历史")
public class CheckinController {

    private final CheckinService checkinService;

    @GetMapping("/today")
    @Operation(summary = "今日任务", description = "当前用户 ACTIVE 计划中 training_date=今天的明细；没有 ACTIVE 计划时返回空任务而不报错")
    public ApiResponse<TodayTaskResponse> today() {
        return ApiResponse.success(checkinService.todayTask());
    }

    @PostMapping
    @Operation(summary = "打卡", description = "幂等：同一用户+明细+日期只允许一条记录；撤销后重新打卡会复活原记录（不新增行）。请求体可为空（等价于按当前 ACTIVE 计划今天打卡）；带 planItemId 时以明细的 training_date 为准")
    public ApiResponse<CheckinResponse> create(
            @Parameter(description = "打卡请求体，所有字段均可空")
            @RequestBody(required = false) CheckinSaveRequest request) {
        return ApiResponse.success(checkinService.create(request));
    }

    @PostMapping("/free")
    @Operation(summary = "记录自选运动", description = "必须选择已上架运动，保存运动名称快照；仍沿用每天一次自由训练的规则")
    public ApiResponse<CheckinResponse> createFree(@RequestBody CheckinSaveRequest request) {
        return ApiResponse.success(checkinService.createFree(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "撤销打卡", description = "逻辑删除打卡记录并把明细状态回置 PENDING；非 ADMIN 只能撤销自己的记录")
    public ApiResponse<Void> cancel(
            @Parameter(description = "打卡记录ID", required = true)
            @PathVariable Long id) {
        checkinService.cancel(id);
        return ApiResponse.success();
    }

    @GetMapping("/calendar")
    @Operation(summary = "打卡日历", description = "返回该月每一天的打卡情况，month 缺省为当月")
    public ApiResponse<CheckinCalendarResponse> calendar(
            @Parameter(description = "月份，格式 yyyy-MM，缺省当月")
            @RequestParam(required = false) String month) {
        return ApiResponse.success(checkinService.calendar(month));
    }

    @GetMapping("/streak")
    @Operation(summary = "连续打卡", description = "当前连续天数（今天没打卡则从昨天起算）、历史最长连续天数、有打卡的总天数")
    public ApiResponse<CheckinStreakResponse> streak() {
        return ApiResponse.success(checkinService.streak());
    }

    @GetMapping
    @Operation(summary = "打卡历史分页", description = "按训练日期倒序、创建时间倒序")
    public ApiResponse<PageResult<CheckinResponse>> page(
            @Parameter(description = "页码，从 1 开始")
            @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数，1-100")
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(checkinService.page(page, size));
    }
}
