import type { LoginForm, LoginResponse, UserInfo } from '@/types/auth'

import { http } from '@/utils/request'

export function loginApi(payload: LoginForm) {
  return http.post<LoginResponse>('/v1/auth/login', payload)
}

export function fetchCurrentUser() {
  return http.get<UserInfo>('/v1/auth/me')
}
