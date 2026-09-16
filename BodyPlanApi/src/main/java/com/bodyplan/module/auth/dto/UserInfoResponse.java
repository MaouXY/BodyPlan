package com.bodyplan.module.auth.dto;

import java.time.LocalDateTime;

/**
 * 当前用户信息（不包含密码散列）。
 */
public record UserInfoResponse(
        Long id,
        String username,
        String nickname,
        String phone,
        String role,
        Integer status,
        LocalDateTime createdAt
) {
}
