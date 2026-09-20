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
} from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const displayName = ref(getDisplayName() || '管理员')

  const isLoggedIn = computed(() => Boolean(token.value))
  const defaultRoute = '/admin/stats'

  async function login(payload: LoginForm) {
    const response = await loginApi(payload)
    if (response.user?.role !== 'ADMIN') {
      ElMessage.error('该账号不是管理员，无法登录管理端')
      throw new Error('NOT_ADMIN')
    }
    token.value = response.token
    displayName.value = response.user.nickname || response.user.username
    setToken(response.token)
    setDisplayName(displayName.value)
  }

  function logout() {
    token.value = ''
    displayName.value = '管理员'
    clearToken()
  }

  return {
    token,
    displayName,
    isLoggedIn,
    defaultRoute,
    login,
    logout,
  }
})
