import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import type { ApiResponse } from '@/types/common'

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 防止重复弹窗的标志
let isShowingAuthError = false
let authErrorTimer: ReturnType<typeof setTimeout> | null = null

// 显示认证错误（防抖）
function showAuthError(message: string) {
  if (isShowingAuthError) return
  isShowingAuthError = true
  ElMessage.error(message)

  // 2秒后重置标志，允许再次显示
  if (authErrorTimer) clearTimeout(authErrorTimer)
  authErrorTimer = setTimeout(() => {
    isShowingAuthError = false
  }, 2000)
}

// 处理登录过期
function handleAuthExpired() {
  const authStore = useAuthStore()
  // 使用logoutAction清除认证状态（不调用后端接口）
  authStore.logoutAction().catch(() => {
    // 忽略退出登录时的错误
  })

  // 只在非登录页时跳转
  if (router.currentRoute.value.path !== '/login') {
    showAuthError('登录已过期，请重新登录')
    router.push('/login')
  }
}

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()

    // 添加认证token
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }

    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 处理 blob 响应（文件下载）
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const { code, message, data } = response.data

    // 请求成功
    if (code === 200) {
      return data
    }

    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  async (error) => {
    const { response } = error

    if (response) {
      const { status, data } = response
      const errorMessage = data?.message || ''

      // 判断是否是认证相关错误（token过期、无效等）
      // 注意：403 "没有权限访问该资源" 是权限不足，不是认证错误，不应触发登出
      const isAuthError = status === 401 ||
        (status === 403 && (
          errorMessage.includes('token') ||
          errorMessage.includes('登录') ||
          errorMessage.includes('认证')
        ))

      if (isAuthError) {
        // 认证错误，统一处理：只弹一次提示，跳转登录页
        handleAuthExpired()
      } else {
        switch (status) {
          case 403:
            // 真正的权限不足（非token过期）
            showAuthError('没有权限执行此操作')
            break

          case 404:
            ElMessage.error('请求的资源不存在')
            break

          case 500:
            ElMessage.error('服务器内部错误')
            break

          default:
            const message = errorMessage || `请求失败 (${status})`
            ElMessage.error(message)
            break
        }
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络连接失败，请检查网络设置')
    }

    return Promise.reject(error)
  }
)

// HTTP请求方法封装类
class HttpClient {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  }

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  }

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  }

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  }

  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.patch(url, data, config)
  }

  upload<T = any>(url: string, file: File, config?: AxiosRequestConfig): Promise<T> {
    const formData = new FormData()
    formData.append('file', file)

    return service.post(url, formData, {
      ...config,
      headers: {
        'Content-Type': 'multipart/form-data',
        ...config?.headers
      }
    })
  }

  download(url: string, filename?: string, config?: AxiosRequestConfig): Promise<void> {
    return service.get(url, {
      ...config,
      responseType: 'blob'
    }).then((blob: Blob) => {
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = filename || 'download'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)
    })
  }
}

// 通用请求方法 (兼容旧API)
export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service(config)
}

// HttpClient实例 (推荐使用)
export const http = new HttpClient()

// 导出axios实例
export default service
