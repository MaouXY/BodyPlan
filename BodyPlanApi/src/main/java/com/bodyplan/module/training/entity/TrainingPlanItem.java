package com.bodyplan.module.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 训练计划明细（计划第几天做什么动作）。无逻辑删除字段，删除计划时按 plan_id 物理清理。
 */
@Getter
@Setter
@TableName("training_plan_item")
public class TrainingPlanItem {

    private Long id;
    private Long planId;
    private Integer dayIndex;
    private LocalDate trainingDate;
    private Long exerciseId;
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private Integer durationSec;
    private Integer restSec;
    private Integer sortNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
