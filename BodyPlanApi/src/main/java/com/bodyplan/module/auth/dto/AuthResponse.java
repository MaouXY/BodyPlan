package com.bodyplan.module.auth.dto;

/**
 * 登录响应：令牌 + 有效期（秒）+ 当前用户信息。
 */
public record AuthResponse(
        String token,
        Long expiresIn,
        UserInfoResponse user
) {
}
