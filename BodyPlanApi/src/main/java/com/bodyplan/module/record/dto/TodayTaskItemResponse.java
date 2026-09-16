package com.bodyplan.module.record.dto;

/**
 * 今日任务中的一个动作明细。
 *
 * @param itemId       计划明细ID（打卡时回传这个值）
 * @param exerciseId   动作ID
 * @param exerciseName 动作名称快照
 * @param sets         组数
 * @param reps         每组次数，可空（计时类动作为空）
 * @param durationSec  每组时长（秒），可空
 * @param restSec      组间休息（秒）
 * @param itemStatus   明细状态：PENDING 未完成 / DONE 已完成（今天已打卡时强制为 DONE，库里也可能是 SKIPPED）
 * @param checkedIn    今天是否已有该明细的打卡记录
 * @param checkinId    今天的打卡记录ID，未打卡为空
 * @param durationMin  打卡时记录的训练时长（分钟），未打卡为空
 * @param feeling      打卡时记录的训练感受，未打卡为空
 */
public record TodayTaskItemResponse(
        Long itemId,
        Long exerciseId,
        String exerciseName,
        Integer sets,
        Integer reps,
        Integer durationSec,
        Integer restSec,
        String itemStatus,
        Boolean checkedIn,
        Long checkinId,
        Integer durationMin,
        String feeling
) {
}
