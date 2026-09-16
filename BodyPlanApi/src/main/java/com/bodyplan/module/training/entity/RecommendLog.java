package com.bodyplan.module.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 推荐记录：保留推荐输入与命中规则，支持解释“计划为什么这样生成/变化”。
 */
@Getter
@Setter
@TableName("recommend_log")
public class RecommendLog {

    private Long id;
    private Long userId;
    private Long planId;
    private String inputSnapshot;
    private String matchedRule;
    private Integer excludedCount;
    private String reason;
    private LocalDateTime createdAt;
}
