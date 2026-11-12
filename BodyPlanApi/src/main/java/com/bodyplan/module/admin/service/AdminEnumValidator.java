package com.bodyplan.module.admin.service;

import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 管理端枚举值校验工具。
 * 管理端写入动作 / 标签时集中校验取值，非法值统一抛 BAD_REQUEST 并给出中文提示。
 */
@Slf4j
public final class AdminEnumValidator {

    /** 目标部位 */
    public static final Set<String> PART_VALUES = Set.of(
            "ABDOMEN", "LEG", "HIP", "ARM", "BACK", "SHOULDER_NECK", "FULL_BODY");

    /** 运动类型 */
    public static final Set<String> SPORT_TYPE_VALUES = Set.of(
            "CARDIO", "STRENGTH", "STRETCH", "RUNNING", "HIIT", "YOGA");

    /** 场景 */
    public static final Set<String> SCENE_VALUES = Set.of(
            "HOME", "DORM", "GYM", "OUTDOOR");

    /** 器械 */
    public static final Set<String> EQUIPMENT_VALUES = Set.of(
            "NONE", "MAT", "DUMBBELL", "BAND");

    /** 禁忌标签 */
    public static final Set<String> CONTRA_FLAG_VALUES = Set.of(
            "KNEE", "WAIST", "SHOULDER", "WRIST", "CARDIO");

    /** 标签类型 */
    public static final Set<String> TAG_TYPE_VALUES = Set.of(
            "PART", "SPORT_TYPE", "SCENE", "EQUIPMENT", "DIFFICULTY");

    private AdminEnumValidator() {
    }

    /** 校验目标部位，为空则跳过（视为不限） */
    public static void validatePart(String part) {
        validateOptionalIn(part, PART_VALUES, "目标部位", "ABDOMEN/LEG/HIP/ARM/BACK/SHOULDER_NECK/FULL_BODY");
    }

    /** 校验运动类型，为空则跳过（视为不限） */
    public static void validateSportType(String sportType) {
        validateOptionalIn(sportType, SPORT_TYPE_VALUES, "运动类型", "CARDIO/STRENGTH/STRETCH/RUNNING/HIIT/YOGA");
    }

    /** 校验场景，为空则跳过（视为不限） */
    public static void validateScene(String scene) {
        validateOptionalIn(scene, SCENE_VALUES, "训练场景", "HOME/DORM/GYM/OUTDOOR");
    }

    /** 校验器械，为空则跳过（视为不限） */
    public static void validateEquipment(String equipment) {
        validateOptionalIn(equipment, EQUIPMENT_VALUES, "训练器械", "NONE/MAT/DUMBBELL/BAND");
    }

    /** 校验标签类型，为空则跳过（列表查询时 tagType 可空） */
    public static void validateTagType(String tagType) {
        validateOptionalIn(tagType, TAG_TYPE_VALUES, "标签类型", "PART/SPORT_TYPE/SCENE/EQUIPMENT/DIFFICULTY");
    }

    /** 校验难度，为空则跳过（视为不限） */
    public static void validateDifficulty(Integer difficulty) {
        if (difficulty == null) {
            return;
        }
        if (difficulty < 1 || difficulty > 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "难度取值非法，允许值：1/2/3");
        }
    }

    /** 校验难度（查询参数为字符串时使用，空白视为不限） */
    public static void validateDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return;
        }
        try {
            validateDifficulty(Integer.valueOf(difficulty.trim()));
        } catch (NumberFormatException exception) {
            log.warn("invalid difficulty, scenario=admin-exercise-validation, difficulty={}", difficulty, exception);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "难度取值非法，允许值：1/2/3");
        }
    }

    /** 校验状态取值必须为 0 或 1 */
    public static void validateStatus(Integer status, String fieldName) {
        if (status == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + "不能为空");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + "取值非法，允许值：1 启用、0 禁用");
        }
    }

    /** 校验禁忌标签数组，空集合视为无禁忌 */
    public static void validateContraFlags(Collection<String> contraFlags) {
        if (contraFlags == null || contraFlags.isEmpty()) {
            return;
        }
        List<String> invalid = contraFlags.stream()
                .filter(flag -> !StringUtils.hasText(flag) || !CONTRA_FLAG_VALUES.contains(flag.trim()))
                .toList();
        if (!invalid.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "禁忌标签取值非法：" + String.join(",", invalid) + "，允许值：KNEE/WAIST/SHOULDER/WRIST/CARDIO");
        }
    }

    private static void validateOptionalIn(String value, Set<String> allowed, String fieldName, String allowedText) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!allowed.contains(value.trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    fieldName + "取值非法：" + value.trim() + "，允许值：" + allowedText);
        }
    }
}
