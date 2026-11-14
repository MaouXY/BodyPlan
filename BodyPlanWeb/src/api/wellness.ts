import { http } from '@/utils/request'

export interface Coach { id: string; username: string; nickname: string; status: number }
export interface Candidate { id: string; username: string; nickname: string; coachId?: string; coachName?: string }
export interface Student {
  studentId: string; username: string; nickname: string; coachId: string; coachName: string
  goal?: string; totalCount: number; doneCount: number
}
export interface StudentDay {
  date: string
  tasks: Array<{ id: string; name: string; sets: number; reps?: number; durationSec?: number; done: boolean | number }>
  sports: Array<{ sportType: string; distanceM: number; durationSec: number; startedAt: number }>
  diet: { kcal: number; protein: number; entryCount: number }
}
export interface Food {
  id: string; name: string; category: string; kcal: number; protein: number; fat: number
  carbohydrate: number; fiber: number | null; source: string; enabled: boolean
}
export interface FoodPage { items: Food[]; total: number; page: number; size: number }
export const coachesApi = {
  list: () => http.get<Coach[]>('/v1/admin/coaches'),
  create: (data: { username: string; password: string; nickname: string }) => http.post<string>('/v1/admin/coaches', data),
  candidates: (keyword: string) => http.get<Candidate[]>('/v1/admin/coach-candidates', { keyword }),
  assign: (studentId: string, coachId: string) => http.put('/v1/admin/coach-assignment', { studentId, coachId }),
  unassign: (studentId: string) => http.delete(`/v1/admin/coach-assignment/${studentId}`),
  students: (date: string) => http.get<Student[]>('/v1/coach/students', { date }),
  day: (studentId: string, date: string) => http.get<StudentDay>(`/v1/coach/students/${studentId}`, { date }),
  remind: (studentId: string, content: string) => http.post(`/v1/coach/students/${studentId}/reminders`, { content }),
}
export const foodsApi = {
  list: (keyword: string, page: number) => http.get<FoodPage>('/v1/admin/foods', { keyword, page }),
  save: (food: Omit<Food, 'id'>, id?: string) => id ? http.put(`/v1/admin/foods/${id}`, food) : http.post('/v1/admin/foods', food),
}
export function localDate() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}
