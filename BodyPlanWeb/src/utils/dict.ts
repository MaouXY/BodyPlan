/** 后端枚举值 → 中文标签，三端（管理端 / App / 文档）保持一致 */

export const GENDER_LABELS: Record<string, string> = {
  MALE: '男',
  FEMALE: '女',
  UNKNOWN: '未知',
}

export const BODY_TYPE_LABELS: Record<string, string> = {
  STRAIGHT: '直筒型',
  PEAR: '梨形',
  HOURGLASS: '沙漏型',
  APPLE: '苹果型',
}

export const GOAL_LABELS: Record<string, string> = {
  LOSE_FAT: '减脂瘦身',
  BUILD_MUSCLE: '增肌塑形',
  CARDIO: '提升心肺',
  POSTURE: '改善体态',
  LOCAL_STRENGTH: '局部力量',
  DAILY_FITNESS: '日常体能',
}

export const PART_LABELS: Record<string, string> = {
  ABDOMEN: '腹部',
  LEG: '腿部',
  HIP: '臀部',
  ARM: '手臂',
  BACK: '背部',
  SHOULDER_NECK: '肩颈',
  FULL_BODY: '全身综合',
}

export const SPORT_TYPE_LABELS: Record<string, string> = {
  CARDIO: '有氧训练',
  STRENGTH: '力量训练',
  STRETCH: '拉伸放松',
  RUNNING: '跑步',
  HIIT: '高强度间歇',
  YOGA: '瑜伽/低强度',
}

export const SCENE_LABELS: Record<string, string> = {
  HOME: '居家',
  DORM: '宿舍',
  GYM: '健身房',
  OUTDOOR: '户外',
}

export const EQUIPMENT_LABELS: Record<string, string> = {
  NONE: '无器械',
  MAT: '瑜伽垫',
  DUMBBELL: '哑铃',
  BAND: '弹力带',
}

export const DIFFICULTY_LABELS: Record<string, string> = {
  '1': '入门',
  '2': '进阶',
  '3': '挑战',
}

export const LEVEL_LABELS: Record<string, string> = {
  L1: 'L1 入门',
  L2: 'L2 进阶',
  L3: 'L3 挑战',
}

export const INJURY_LABELS: Record<string, string> = {
  KNEE: '膝盖不适',
  WAIST: '腰部不适',
  SHOULDER: '肩部不适',
  WRIST: '手腕不适',
  CARDIO: '心肺较弱',
  NONE: '无明显伤病',
}

export const TRAIN_LIMIT_LABELS: Record<string, string> = {
  SMALL_SPACE: '小场地',
  NO_NOISE: '零噪音',
  NO_JUMP: '不能跳',
  NO_EQUIPMENT: '无器械',
  DORM: '宿舍环境',
}

export const STAIR_LABELS: Record<string, string> = {
  EASY: '轻松',
  SLIGHT_HARD: '略喘',
  HARD: '明显吃力',
  FAIL: '无法完成',
}

export const TAG_TYPE_LABELS: Record<string, string> = {
  PART: '部位',
  SPORT_TYPE: '运动类型',
  SCENE: '场景',
  EQUIPMENT: '器械',
  DIFFICULTY: '难度',
}

export const PLAN_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '执行中',
  FINISHED: '已完成',
  REPLACED: '已替换',
}

const DAY_LABELS: Record<string, string> = {
  '1': '周一',
  '2': '周二',
  '3': '周三',
  '4': '周四',
  '5': '周五',
  '6': '周六',
  '7': '周日',
}

const TAG_TYPE_MAPS: Record<string, Record<string, string>> = {
  PART: PART_LABELS,
  SPORT_TYPE: SPORT_TYPE_LABELS,
  SCENE: SCENE_LABELS,
  EQUIPMENT: EQUIPMENT_LABELS,
  DIFFICULTY: DIFFICULTY_LABELS,
}

export function tagLabelOf(tagType: string, tagCode: string) {
  return TAG_TYPE_MAPS[tagType]?.[tagCode] ?? tagCode
}

export function labelOf(map: Record<string, string>, code?: string | null) {
  if (!code) {
    return '-'
  }
  return map[code] ?? code
}

export function labelsOf(map: Record<string, string>, codes?: string[] | null) {
  if (!codes || codes.length === 0) {
    return '-'
  }
  return codes.map((code) => map[code] ?? code).join('、')
}

export function weekDaysOf(days?: number[] | null) {
  if (!days || days.length === 0) {
    return '-'
  }
  return days.map((day) => DAY_LABELS[String(day)] ?? String(day)).join('、')
}
