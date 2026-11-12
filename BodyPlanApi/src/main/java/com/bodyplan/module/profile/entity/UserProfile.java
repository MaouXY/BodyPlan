package com.bodyplan.module.profile.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户档案（建档问卷合并表，一个用户一条）。
 */
@Getter
@Setter
@TableName("user_profile")
public class UserProfile {

    private Long id;
    private Long userId;
    private String gender;
    private Integer age;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String bodyType;
    private String primaryGoal;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String subGoals;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String focusParts;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sportTypes;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String weeklyDays;
    private Integer dailyMinutes;
    private Integer remindEnabled;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remindTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String injuries;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String trainLimits;
    private Integer profileCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
