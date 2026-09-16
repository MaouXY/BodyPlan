package com.bodyplan.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.common.security.CurrentUserProvider;
import com.bodyplan.common.security.JwtTokenService;
import com.bodyplan.common.security.LoginType;
import com.bodyplan.module.auth.dto.AuthResponse;
import com.bodyplan.module.auth.dto.LoginRequest;
import com.bodyplan.module.auth.dto.RegisterRequest;
import com.bodyplan.module.auth.dto.UserInfoResponse;
import com.bodyplan.module.user.entity.UserAccount;
import com.bodyplan.module.user.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 认证授权：注册、登录、当前用户信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ROLE_USER = "USER";
    private static final int STATUS_ENABLED = 1;

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Transactional
    public UserInfoResponse register(RegisterRequest request) {
        String username = request.username().trim();
        Long duplicated = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username));
        if (duplicated != null && duplicated > 0) {
            log.warn("register rejected, scenario=auth-register, reason=username-duplicated, username={}", username);
            throw new BusinessException(ErrorCode.AUTH_USERNAME_DUPLICATED, "该用户名已被注册，请更换");
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : username);
        account.setPhone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null);
        account.setRole(ROLE_USER);
        account.setStatus(STATUS_ENABLED);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account.setDeleted(0);
        userAccountMapper.insert(account);

        log.info("user registered, scenario=auth-register, userId={}, username={}", account.getId(), username);
        return toUserInfo(account);
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();
        UserAccount account = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username));
        if (account == null || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            log.warn("login failed, scenario=auth-login, reason=credential-mismatch, username={}", username);
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED, "用户名或密码不正确");
        }
        if (account.getStatus() == null || account.getStatus() != STATUS_ENABLED) {
            log.warn("login failed, scenario=auth-login, reason=account-disabled, userId={}, username={}",
                    account.getId(), username);
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }

        CurrentUser currentUser = CurrentUser.builder()
                .principalId(account.getId())
                .subject(account.getUsername())
                .loginType(LoginType.ACCOUNT)
                .roleCode(account.getRole())
                .build();
        String token = jwtTokenService.generateToken(currentUser);
        log.info("login succeeded, scenario=auth-login, userId={}, username={}, role={}",
                account.getId(), account.getUsername(), account.getRole());
        return new AuthResponse(token, jwtTokenService.getExpirationSeconds(), toUserInfo(account));
    }

    public UserInfoResponse currentUser() {
        CurrentUser currentUser = CurrentUserProvider.require();
        UserAccount account = userAccountMapper.selectById(currentUser.getId());
        if (account == null) {
            log.warn("current user missing, scenario=auth-current-user, userId={}", currentUser.getId());
            throw new BusinessException(ErrorCode.AUTH_CURRENT_USER_NOT_FOUND, "当前用户不存在");
        }
        return toUserInfo(account);
    }

    private UserInfoResponse toUserInfo(UserAccount account) {
        return new UserInfoResponse(
                account.getId(),
                account.getUsername(),
                account.getNickname(),
                account.getPhone(),
                account.getRole(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
