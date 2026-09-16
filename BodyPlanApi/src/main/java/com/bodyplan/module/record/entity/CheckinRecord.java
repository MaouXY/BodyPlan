package com.bodyplan.module.record.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日打卡记录。(user_id, plan_item_id, training_date) 唯一，天然拦截重复打卡。
 */
@Getter
@Setter
@TableName("checkin_record")
public class CheckinRecord {

    private Long id;
    private Long userId;
    private Long planId;
    private Long planItemId;
    private LocalDate trainingDate;
    private Integer finished;
    private Integer durationMin;
    private String feeling;
    private String remark;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
