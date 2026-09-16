package com.bodyplan.module.training.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 动作库。contra_flags 为用户伤病/限制的“排除式”匹配依据。
 */
@Getter
@Setter
@TableName("exercise")
public class Exercise {

    private Long id;
    private String name;
    private String part;
    private String sportType;
    private Integer difficulty;
    private String scene;
    private String equipment;
    private String contraFlags;
    private String videoUrl;
    private String coverUrl;
    private String keyPoints;
    private String cautions;
    private String suitableFor;
    private Integer status;
    private Integer sortNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
