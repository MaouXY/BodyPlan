package com.bodyplan.module.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 动作标签字典（筛选与表单选项）。
 */
@Getter
@Setter
@TableName("exercise_tag")
public class ExerciseTag {

    private Long id;
    private String tagType;
    private String tagCode;
    private String tagName;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createdAt;
}
