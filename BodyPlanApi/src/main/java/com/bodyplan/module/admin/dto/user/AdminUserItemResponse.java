package com.bodyplan.module.admin.dto.user;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端用户列表 / 用户详情中的用户条目。
 * 注意：不包含 passwordHash 等敏感字段。
 *
 * @param id                 用户ID
 * @param username           登录账号
 * @param nickname           昵称
 * @param phone              手机号
 * @param role               角色：USER / ADMIN / COACH
 * @param status             状态：1 启用、0 禁用
 * @param createdAt          注册时间
 * @param profileCompleted   是否已完成建档（无档案记 false）
 * @param primaryGoal        主目标（无档案为 null）
 * @param latestCheckinDate  最近打卡日期（无打卡为 null）
 * @param checkinCount       打卡总数
 */
public record AdminUserItemResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String username,
        String nickname,
        String phone,
        String role,
        Integer status,
        LocalDateTime createdAt,
        Boolean profileCompleted,
        String primaryGoal,
        LocalDate latestCheckinDate,
        Long checkinCount
) {
}
