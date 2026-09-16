package com.bodyplan.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户账号。
 */
@Getter
@Setter
@TableName("user_account")
public class UserAccount {

    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String phone;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
