package com.bodyplan.module.admin.dto.user;

import jakarta.validation.constraints.NotNull;

/**
 * 管理端启用 / 禁用用户请求。
 *
 * @param status 状态：1 启用、0 禁用
 */
public record AdminUserStatusRequest(
        @NotNull(message = "状态不能为空")
        Integer status
) {
}
