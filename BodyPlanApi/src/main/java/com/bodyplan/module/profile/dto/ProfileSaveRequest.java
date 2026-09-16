package com.bodyplan.module.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 建档问卷保存入参（支持分步保存）。
 *
 * <p><b>所有字段一律可空</b>，字段形态的语义由 ProfileService 统一解释（空值一律放行，不做非空校验）：
 * <ul>
 *   <li>普通字段传 null（或空串）→ 不修改库中原值；传具体值 → 覆盖；</li>
 *   <li>List 字段传 null → 不修改原值；传空数组 [] → 清空（落库为 null）；传非空数组 → 覆盖；</li>
 *   <li>heightCm / weightKg 只传其一时档案照常保存，但不会写入 body_data。</li>
 * </ul>
 *
 * <p>校验分两层，且只校验「有值」的情况：
 * <ol>
 *   <li>本 DTO 上的 jakarta.validation 注解负责格式/范围快速拦截（返回 BAD_REQUEST + 字段中文提示）；</li>
 *   <li>ProfileService 会对身高/体重/年龄再校验一次并抛 BODY_DATA_INVALID + 中文提示，
 *       保证服务层被其它模块直接调用时也不会写入非法身高体重。</li>
 * </ol>
 * 数组类字段落库时统一使用逗号分隔字符串（如 "ABDOMEN,LEG"、"1,3,5"），空数组存 null。
 */
public record ProfileSaveRequest(

        /** 性别：MALE / FEMALE / UNKNOWN，可空（新建档为空时兜底 UNKNOWN） */
        @Pattern(regexp = "MALE|FEMALE|UNKNOWN", message = "性别只能是 MALE、FEMALE 或 UNKNOWN")
        String gender,

        /** 年龄：10-100，可空 */
        @Min(value = 10, message = "年龄不能小于 10 岁")
        @Max(value = 100, message = "年龄不能大于 100 岁")
        Integer age,

        /** 身高（cm）：100-250，可空 */
        @Min(value = 100, message = "身高必须大于等于 100cm")
        @Max(value = 250, message = "身高必须小于等于 250cm")
        Double heightCm,

        /** 体重（kg）：25-250，可空 */
        @Min(value = 25, message = "体重必须大于等于 25kg")
        @Max(value = 250, message = "体重必须小于等于 250kg")
        Double weightKg,

        /** 身体类型：STRAIGHT / PEAR / HOURGLASS / APPLE，可空 */
        @Pattern(regexp = "STRAIGHT|PEAR|HOURGLASS|APPLE", message = "身体类型只能是 STRAIGHT、PEAR、HOURGLASS 或 APPLE")
        String bodyType,

        /** 主目标：LOSE_FAT / BUILD_MUSCLE / CARDIO / POSTURE / LOCAL_STRENGTH / DAILY_FITNESS，可空 */
        @Pattern(
                regexp = "LOSE_FAT|BUILD_MUSCLE|CARDIO|POSTURE|LOCAL_STRENGTH|DAILY_FITNESS",
                message = "训练目标取值不合法"
        )
        String primaryGoal,

        /** 次目标（字符串数组，最多 5 个，单个不超过 30 字） */
        @Size(max = 5, message = "次目标最多选择 5 个")
        List<@Size(max = 30, message = "单个次目标不能超过 30 个字符") String> subGoals,

        /** 重点部位：ABDOMEN / LEG / HIP / ARM / BACK / SHOULDER_NECK / FULL_BODY */
        @Size(max = 7, message = "重点部位最多选择 7 个")
        List<@Pattern(
                regexp = "ABDOMEN|LEG|HIP|ARM|BACK|SHOULDER_NECK|FULL_BODY",
                message = "重点部位取值不合法"
        ) String> focusParts,

        /** 运动偏好：CARDIO / STRENGTH / STRETCH / RUNNING / HIIT / YOGA */
        @Size(max = 6, message = "运动偏好最多选择 6 个")
        List<@Pattern(
                regexp = "CARDIO|STRENGTH|STRETCH|RUNNING|HIIT|YOGA",
                message = "运动偏好取值不合法"
        ) String> sportTypes,

        /** 每周训练日：1-7，可空（元素取值必须合法，重复值由服务层去重升序） */
        @Size(max = 7, message = "每周训练日最多 7 天")
        List<@Min(value = 1, message = "训练日只能是 1-7（周一到周日）")
             @Max(value = 7, message = "训练日只能是 1-7（周一到周日）") Integer> weeklyDays,

        /** 每日运动时长（分钟）：1-300，可空 */
        @Min(value = 1, message = "每日运动时长必须大于 0 分钟")
        @Max(value = 300, message = "每日运动时长不能超过 300 分钟")
        Integer dailyMinutes,

        /** 是否开启训练提醒：1 开、0 关 */
        @Min(value = 0, message = "提醒开关只能是 0 或 1")
        @Max(value = 1, message = "提醒开关只能是 0 或 1")
        Integer remindEnabled,

        /** 提醒时间，格式 HH:mm */
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "提醒时间格式必须为 HH:mm，例如 20:30")
        String remindTime,

        /** 伤病：KNEE / WAIST / SHOULDER / WRIST / CARDIO / NONE */
        @Size(max = 6, message = "伤病最多选择 6 项")
        List<@Pattern(
                regexp = "KNEE|WAIST|SHOULDER|WRIST|CARDIO|NONE",
                message = "伤病取值不合法"
        ) String> injuries,

        /** 训练限制：SMALL_SPACE / NO_NOISE / NO_JUMP / NO_EQUIPMENT / DORM */
        @Size(max = 5, message = "训练限制最多选择 5 项")
        List<@Pattern(
                regexp = "SMALL_SPACE|NO_NOISE|NO_JUMP|NO_EQUIPMENT|DORM",
                message = "训练限制取值不合法"
        ) String> trainLimits
) {
}
