package com.bodyplan.common.security;

import com.bodyplan.common.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrentUser {

    private final Long principalId;
    private final String subject;
    private final LoginType loginType;
    private final String roleCode;

    public Long getId() {
        return principalId;
    }

    public UserRole getRole() {
        return UserRole.fromCode(roleCode);
    }
}

