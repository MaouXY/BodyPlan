package com.bodyplan.module.admin.dto.exercise;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 管理端新增 / 更新动作请求。
 * 落库时 contraFlags 会按逗号拼接为单字段存储。
 *
 * @param name        动作名称
 * @param part        目标部位：ABDOMEN/LEG/HIP/ARM/BACK/SHOULDER_NECK/FULL_BODY
 * @param sportType   运动类型：CARDIO/STRENGTH/STRETCH/RUNNING/HIIT/YOGA
 * @param difficulty  难度：1-3
 * @param scene       场景：HOME/DORM/GYM/OUTDOOR
 * @param equipment   器械：NONE/MAT/DUMBBELL/BAND
 * @param contraFlags 禁忌标签数组：KNEE/WAIST/SHOULDER/WRIST/CARDIO
 * @param videoUrl    视频地址
 * @param coverUrl    封面地址
 * @param keyPoints   动作要点
 * @param cautions    注意事项
 * @param suitableFor 适用人群
 * @param status      状态：1 上架、0 下架，默认 1
 * @param sortNo      排序号
 */
public record AdminExerciseSaveRequest(
        @NotBlank(message = "动作名称不能为空")
        String name,
        String part,
        String sportType,
        Integer difficulty,
        String scene,
        String equipment,
        List<String> contraFlags,
        String videoUrl,
        String coverUrl,
        String keyPoints,
        String cautions,
        String suitableFor,
        Integer status,
        Integer sortNo
) {
}
