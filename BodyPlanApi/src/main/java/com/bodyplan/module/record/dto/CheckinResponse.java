package com.bodyplan.module.record.dto;

import com.bodyplan.module.record.entity.CheckinRecord;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录响应（打卡成功返回、打卡历史分页元素共用）。
 *
 * @param id           打卡记录ID
 * @param planId       计划ID（自由训练可能为空）
 * @param planItemId   计划明细ID（自由训练为空）
 * @param trainingDate 训练日期 yyyy-MM-dd
 * @param finished     是否完成计划任务（打卡记录固定为 true，finished=1）
 * @param durationMin  本次训练时长（分钟），可空
 * @param feeling      训练感受：EASY / NORMAL / HARD，可空
 * @param remark       备注，可空
 * @param createdAt    创建时间
 */
public record CheckinResponse(
        Long id,
        Long planId,
        Long planItemId,
        LocalDate trainingDate,
        Boolean finished,
        Integer durationMin,
        String feeling,
        String remark,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {

    /**
     * 实体转响应。finished 由 tinyint 1/0 转成布尔，方便 App 直接判断。
     */
    public static CheckinResponse from(CheckinRecord entity) {
        if (entity == null) {
            return null;
        }
        return new CheckinResponse(
                entity.getId(),
                entity.getPlanId(),
                entity.getPlanItemId(),
                entity.getTrainingDate(),
                entity.getFinished() != null && entity.getFinished() == 1,
                entity.getDurationMin(),
                entity.getFeeling(),
                entity.getRemark(),
                entity.getCreatedAt()
        );
    }
}
