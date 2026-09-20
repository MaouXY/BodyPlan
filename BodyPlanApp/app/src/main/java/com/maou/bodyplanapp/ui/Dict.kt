package com.maou.bodyplanapp.ui

/** App 侧枚举 → 中文标签（与管理端、后端枚举保持一致） */

val GENDER_OPTIONS = listOf("MALE" to "男", "FEMALE" to "女")

val BODY_TYPE_OPTIONS = listOf(
    "STRAIGHT" to "直筒型",
    "PEAR" to "梨形",
    "HOURGLASS" to "沙漏型",
    "APPLE" to "苹果型",
)

val GOAL_OPTIONS = listOf(
    "LOSE_FAT" to "减脂瘦身",
    "BUILD_MUSCLE" to "增肌塑形",
    "CARDIO" to "提升心肺",
    "POSTURE" to "改善体态",
    "LOCAL_STRENGTH" to "局部力量",
    "DAILY_FITNESS" to "日常体能",
)

val PART_OPTIONS = listOf(
    "ABDOMEN" to "腹部",
    "LEG" to "腿部",
    "HIP" to "臀部",
    "ARM" to "手臂",
    "BACK" to "背部",
    "SHOULDER_NECK" to "肩颈",
    "FULL_BODY" to "全身",
)

val SPORT_TYPE_OPTIONS = listOf(
    "CARDIO" to "有氧训练",
    "STRENGTH" to "力量训练",
    "STRETCH" to "拉伸放松",
    "RUNNING" to "跑步",
    "HIIT" to "高强度间歇",
    "YOGA" to "瑜伽/低强度",
)

val INJURY_OPTIONS = listOf(
    "KNEE" to "膝盖不适",
    "WAIST" to "腰部不适",
    "SHOULDER" to "肩部不适",
    "WRIST" to "手腕不适",
    "CARDIO" to "心肺较弱",
    "NONE" to "无明显伤病",
)

val TRAIN_LIMIT_OPTIONS = listOf(
    "DORM" to "宿舍环境",
    "SMALL_SPACE" to "小场地",
    "NO_NOISE" to "零噪音",
    "NO_JUMP" to "不能跳",
    "NO_EQUIPMENT" to "无器械",
)

val STAIR_OPTIONS = listOf(
    "EASY" to "轻松",
    "SLIGHT_HARD" to "略喘",
    "HARD" to "明显吃力",
    "FAIL" to "无法完成",
)

val WEEK_DAY_OPTIONS = listOf(
    1 to "周一",
    2 to "周二",
    3 to "周三",
    4 to "周四",
    5 to "周五",
    6 to "周六",
    7 to "周日",
)

val DURATION_OPTIONS = listOf("10" to "10 分钟", "20" to "20 分钟", "30" to "30 分钟", "45" to "45 分钟")

private val GOAL_LABELS = GOAL_OPTIONS.toMap()
private val PART_LABELS = PART_OPTIONS.toMap()
private val SPORT_TYPE_LABELS = SPORT_TYPE_OPTIONS.toMap()
private val BODY_TYPE_LABELS = BODY_TYPE_OPTIONS.toMap()
private val GENDER_LABELS = GENDER_OPTIONS.toMap()
private val INJURY_LABELS = INJURY_OPTIONS.toMap()
private val TRAIN_LIMIT_LABELS = TRAIN_LIMIT_OPTIONS.toMap()
private val STAIR_LABELS = STAIR_OPTIONS.toMap()
private val WEEK_DAY_LABELS = WEEK_DAY_OPTIONS.toMap()

fun goalLabel(code: String?) = GOAL_LABELS[code] ?: code ?: "-"

fun partLabel(code: String?) = PART_LABELS[code] ?: code ?: "-"

fun sportTypeLabel(code: String?) = SPORT_TYPE_LABELS[code] ?: code ?: "-"

fun bodyTypeLabel(code: String?) = BODY_TYPE_LABELS[code] ?: code ?: "-"

fun genderLabel(code: String?) = GENDER_LABELS[code] ?: code ?: "-"

fun injuryLabel(code: String?) = INJURY_LABELS[code] ?: code ?: "-"

fun trainLimitLabel(code: String?) = TRAIN_LIMIT_LABELS[code] ?: code ?: "-"

fun stairLabel(code: String?) = STAIR_LABELS[code] ?: code ?: "-"

fun weekDayLabel(day: Int?) = WEEK_DAY_LABELS[day] ?: "-"

fun levelLabel(code: String?): String = when (code) {
    "L1" -> "L1 入门"
    "L2" -> "L2 进阶"
    "L3" -> "L3 挑战"
    else -> code ?: "-"
}

/** 组次描述：力量类是“组数 × 次数”，计时类是“组数 × 秒数” */
fun planVolumeText(sets: Int?, reps: Int?, durationSec: Int?, restSec: Int?): String {
    val setText = sets?.let { "$it 组" } ?: "-"
    val unitText = when {
        reps != null && reps > 0 -> "× $reps 次"
        durationSec != null && durationSec > 0 -> "× ${durationSec}s"
        else -> ""
    }
    val restText = restSec?.takeIf { it > 0 }?.let { "，组间休息 ${it}s" } ?: ""
    return "$setText $unitText$restText".trim()
}

/** 多选列表切换 */
fun MutableList<String>.toggleValue(code: String) {
    if (contains(code)) remove(code) else add(code)
}

fun MutableList<Int>.toggleValue(day: Int) {
    if (contains(day)) remove(day) else add(day)
}
