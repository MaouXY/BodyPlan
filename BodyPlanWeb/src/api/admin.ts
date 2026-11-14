import type {
  AdminBodyDataItem,
  AdminCheckinRankingItem,
  AdminCheckinTrendPoint,
  AdminExerciseTag,
  AdminOverview,
  AdminUserDetail,
  AdminUserItem,
  PageResult,
  TagForm,
} from '@/types/admin'

import { http } from '@/utils/request'

/* ------------------------------ 打卡数据统计 ------------------------------ */

export function fetchOverview() {
  return http.get<AdminOverview>('/v1/admin/stats/overview')
}

export function fetchCheckinTrend(days = 14) {
  return http.get<AdminCheckinTrendPoint[]>('/v1/admin/stats/checkins/trend', {
    days,
  })
}

export function fetchCheckinRanking(limit = 10) {
  return http.get<AdminCheckinRankingItem[]>(
    '/v1/admin/stats/checkins/ranking',
    { limit },
  )
}

/* -------------------------------- 用户管理 -------------------------------- */

export interface UserQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

export function fetchUsers(query: UserQuery) {
  return http.get<PageResult<AdminUserItem>>('/v1/admin/users', {
    ...query,
  })
}

export function fetchUserDetail(id: string) {
  return http.get<AdminUserDetail>(`/v1/admin/users/${id}`)
}

export function updateUserStatus(id: string, status: number) {
  return http.put<void>(`/v1/admin/users/${id}/status`, { status })
}

/* ------------------------------ 身体数据查看 ------------------------------ */

export interface BodyDataQuery {
  page?: number
  size?: number
  userId?: number
}

export function fetchBodyData(query: BodyDataQuery) {
  return http.get<PageResult<AdminBodyDataItem>>('/v1/admin/body-data', {
    ...query,
  })
}

/* ------------------------------ 动作标签管理 ------------------------------ */

export function fetchTags(tagType?: string) {
  return http.get<AdminExerciseTag[]>(
    '/v1/admin/exercise-tags',
    tagType ? { tagType } : undefined,
  )
}

export function createTag(payload: TagForm) {
  return http.post<AdminExerciseTag>('/v1/admin/exercise-tags', payload)
}

export function updateTag(id: number, payload: TagForm) {
  return http.put<AdminExerciseTag>(`/v1/admin/exercise-tags/${id}`, payload)
}

export function deleteTag(id: number) {
  return http.delete<void>(`/v1/admin/exercise-tags/${id}`)
}
