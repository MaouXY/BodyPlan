export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
}

export interface AdminOverview {
  userTotal: number
  profileCompletedTotal: number
  planTotal: number
  activePlanTotal: number
  exerciseTotal: number
  exerciseTagTotal: number
  checkinTotal: number
  todayCheckinTotal: number
  activeUserTodayTotal: number
  planItemDoneTotal: number
  planItemPendingTotal: number
}

export interface AdminCheckinTrendPoint {
  date: string
  checkinCount: number
  activeUserCount: number
  newUserCount: number
}

export interface AdminCheckinRankingItem {
  userId: number
  username: string
  nickname?: string | null
  checkinCount: number
  currentStreak?: number | null
  lastCheckinDate?: string | null
}

export interface AdminUserItem {
  id: number
  username: string
  nickname: string
  phone?: string | null
  role: string
  status: number
  createdAt?: string | null
  profileCompleted?: boolean | null
  primaryGoal?: string | null
  latestCheckinDate?: string | null
  checkinCount?: number | null
}

export interface AdminProfileBrief {
  gender?: string | null
  age?: number | null
  heightCm?: number | null
  weightKg?: number | null
  bodyType?: string | null
  primaryGoal?: string | null
  focusParts?: string[] | null
  sportTypes?: string[] | null
  weeklyDays?: number[] | null
  dailyMinutes?: number | null
  injuries?: string[] | null
  trainLimits?: string[] | null
}

export interface AdminBodyDataBrief {
  id: number
  heightCm?: number | null
  weightKg?: number | null
  bmi?: number | null
  bodyType?: string | null
  score?: number | null
  remark?: string | null
  recordedAt?: string | null
}

export interface AdminFitnessTestBrief {
  id: number
  pushUp?: number | null
  squat?: number | null
  crunch?: number | null
  stairStatus?: string | null
  level?: string | null
  testedAt?: string | null
}

export interface AdminPlanBrief {
  planId: number
  name: string
  goal?: string | null
  level?: string | null
  daysPerWeek?: number | null
  minutesPerSession?: number | null
  startDate?: string | null
  status?: string | null
  reason?: string | null
}

export interface AdminCheckinStatsBrief {
  totalCount?: number | null
  todayChecked?: boolean | null
  lastCheckinDate?: string | null
  currentStreak?: number | null
}

export interface AdminUserDetail {
  user: AdminUserItem
  profile?: AdminProfileBrief | null
  latestBodyData?: AdminBodyDataBrief | null
  bodyDataHistory?: AdminBodyDataBrief[] | null
  latestFitnessTest?: AdminFitnessTestBrief | null
  activePlan?: AdminPlanBrief | null
  checkinStats?: AdminCheckinStatsBrief | null
}

export interface AdminBodyDataItem {
  id: number
  userId: number
  username: string
  nickname?: string | null
  heightCm?: number | null
  weightKg?: number | null
  bmi?: number | null
  bodyType?: string | null
  score?: number | null
  remark?: string | null
  recordedAt?: string | null
}

export interface AdminExerciseTag {
  id: number
  tagType: string
  tagCode: string
  tagName: string
  sortNo?: number | null
  status?: number | null
}

export interface TagForm {
  tagType: string
  tagCode: string
  tagName: string
  sortNo?: number
  status?: number
}
