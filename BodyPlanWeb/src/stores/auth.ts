import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'

import type { LoginForm } from '@/types/auth'
import { loginApi } from '@/api/auth'
import {
  clearToken,
  getDisplayName,
  getToken,
  setDisplayName,
  setToken,
  getRole,
  setRole,
} from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const displayName = ref(getDisplayName() || '管理员')
  const role = ref(getRole())

  const isLoggedIn = computed(() => Boolean(token.value))
  const defaultRoute = computed(() => role.value === 'COACH' ? '/coach/students' : '/admin/stats')

  async function login(payload: LoginForm) {
    const response = await loginApi(payload)
    if (!['ADMIN', 'COACH'].includes(response.user?.role)) {
      ElMessage.error('请使用管理员或教练账号登录')
      throw new Error('ROLE_NOT_ALLOWED')
    }
    token.value = response.token
    displayName.value = response.user.nickname || response.user.username
    setToken(response.token)
    setDisplayName(displayName.value)
    role.value = response.user.role
    setRole(role.value)
  }

  function logout() {
    token.value = ''
    displayName.value = '管理员'
    clearToken()
    role.value = ''
  }

  return {
    token,
    displayName,
    role,
    isLoggedIn,
    defaultRoute,
    login,
    logout,
  }
})
