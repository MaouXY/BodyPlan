package com.bodyplan.module.fitness.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 体能测试记录（可多次，产出 L1/L2/L3 等级）。
 */
@Getter
@Setter
@TableName("fitness_test")
public class FitnessTest {

    private Long id;
    private Long userId;
    private Integer pushUp;
    private Integer squat;
    private Integer crunch;
    private String stairStatus;
    private String level;
    private LocalDateTime testedAt;
    private LocalDateTime createdAt;
}
