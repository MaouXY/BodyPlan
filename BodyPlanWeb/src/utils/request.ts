import axios from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import { clearToken, getToken } from '@/utils/storage'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** 后端统一返回 ApiResponse：code = 0 表示成功（ErrorCode.SUCCESS） */
const SUCCESS_CODE = 0

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (typeof payload?.code === 'number' && payload.code !== SUCCESS_CODE) {
      console.error('scenario=api-business-error', {
        method: response.config.method, url: response.config.url, code: payload.code,
      }, new Error(payload.message))
      ElMessage.error(payload.message || '请求失败')
      return Promise.reject(payload)
    }
    return payload?.data ?? response.data
  },
  (error) => {
    // 不记录 config/body/header，避免密码与 token 进入浏览器日志。
    console.error('scenario=api-request-error', {
      method: error?.config?.method, url: error?.config?.url, status: error?.response?.status,
      message: error?.response?.data?.message,
    }, error?.stack || error?.message || '网络异常')
    const status = error?.response?.status
    if (status === 401) {
      clearToken()
      ElMessage.error('登录已失效，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(
        error?.response?.data?.message || error.message || '网络异常',
      )
    }
    return Promise.reject(error)
  },
)

/**
 * 带类型的请求助手。
 * 响应拦截器已经把 ApiResponse.data 解包，所以这里把返回类型补回业务类型。
 */
export const http = {
  get: <T>(url: string, params?: Record<string, unknown>) =>
    request.get(url, { params }) as unknown as Promise<T>,
  post: <T>(url: string, data?: unknown) =>
    request.post(url, data) as unknown as Promise<T>,
  put: <T>(url: string, data?: unknown) =>
    request.put(url, data) as unknown as Promise<T>,
  delete: <T>(url: string) => request.delete(url) as unknown as Promise<T>,
}

export default request
