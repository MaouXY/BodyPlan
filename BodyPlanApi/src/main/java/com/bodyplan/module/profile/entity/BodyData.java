package com.bodyplan.module.profile.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 身体数据记录（可多条，用于复测趋势与身体评分）。
 */
@Getter
@Setter
@TableName("body_data")
public class BodyData {

    private Long id;
    private Long userId;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal bmi;
    private String bodyType;
    private Integer score;
    private String remark;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
