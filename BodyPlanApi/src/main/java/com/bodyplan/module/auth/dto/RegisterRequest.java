package com.bodyplan.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 4, max = 32) String username,
        @NotBlank @Size(min = 6, max = 64) String password,
        @Size(max = 64) String nickname,
        @Pattern(regexp = "^$|^1[0-9]{10}$", message = "手机号格式不正确") String phone
) {
}
