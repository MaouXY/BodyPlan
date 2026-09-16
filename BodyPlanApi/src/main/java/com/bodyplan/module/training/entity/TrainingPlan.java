package com.bodyplan.module.training.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 训练计划（一个用户同时只有一条 ACTIVE）。
 */
@Getter
@Setter
@TableName("training_plan")
public class TrainingPlan {

    private Long id;
    private Long userId;
    private String name;
    private String goal;
    private String level;
    private Integer daysPerWeek;
    private Integer minutesPerSession;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
