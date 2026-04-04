import type { ApiResponse } from '@/types/common'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

export function getToken(): string | null {
  return uni.getStorageSync('accessToken') || null
}

function getRefreshToken(): string | null {
  return uni.getStorageSync('refreshToken') || null
}

export function setTokens(access: string, refresh: string) {
  uni.setStorageSync('accessToken', access)
  uni.setStorageSync('refreshToken', refresh)
}

export function clearTokens() {
  uni.removeStorageSync('accessToken')
  uni.removeStorageSync('refreshToken')
  uni.removeStorageSync('userInfo')
}

function redirectToLogin() {
  clearTokens()
  uni.reLaunch({ url: '/pages/login/index' })
}

async function refreshToken(): Promise<string> {
  const refresh = getRefreshToken()
  if (!refresh) throw new Error('No refresh token')

  const [err, res] = await uni.request({
    url: `${BASE_URL}/auth/refresh`,
    method: 'POST',
    data: { refreshToken: refresh },
    header: { 'Content-Type': 'application/json' },
  })

  if (err || !res) throw new Error('Refresh request failed')

  const body = res.data as ApiResponse<{ accessToken: string; refreshToken: string }>
  if (body.code !== 200 || !body.data) throw new Error('Refresh failed')

  setTokens(body.data.accessToken, body.data.refreshToken)
  return body.data.accessToken
}

function retryRequest<T>(options: RequestOptions, token: string): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...options.header,
        Authorization: `Bearer ${token}`,
      },
      success: (res) => {
        const body = res.data as ApiResponse<T>
        if (body.code === 200) resolve(body.data)
        else reject(new Error(body.message || '请求失败'))
      },
      fail: (err) => reject(err),
    })
  })
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
}

function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options.header,
    }
    if (token) header['Authorization'] = `Bearer ${token}`

    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: async (res) => {
        const statusCode = res.statusCode
        const body = res.data as ApiResponse<T>

        if (statusCode === 401) {
          if (!isRefreshing) {
            isRefreshing = true
            try {
              const newToken = await refreshToken()
              isRefreshing = false
              pendingRequests.forEach((cb) => cb(newToken))
              pendingRequests = []
              const result = await retryRequest<T>(options, newToken)
              resolve(result)
            } catch {
              isRefreshing = false
              pendingRequests = []
              redirectToLogin()
              reject(new Error('登录已过期'))
            }
          } else {
            pendingRequests.push(async (newToken: string) => {
              try {
                const result = await retryRequest<T>(options, newToken)
                resolve(result)
              } catch (e) {
                reject(e)
              }
            })
          }
          return
        }

        if (statusCode >= 200 && statusCode < 300 && body.code === 200) {
          resolve(body.data)
        } else {
          const msg = body.message || `请求失败 (${statusCode})`
          uni.showToast({ title: msg, icon: 'none', duration: 2000 })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none', duration: 2000 })
        reject(err)
      },
    })
  })
}

export const http = {
  get: <T = any>(url: string, data?: any) => request<T>({ url, method: 'GET', data }),
  post: <T = any>(url: string, data?: any) => request<T>({ url, method: 'POST', data }),
  put: <T = any>(url: string, data?: any) => request<T>({ url, method: 'PUT', data }),
  delete: <T = any>(url: string, data?: any) => request<T>({ url, method: 'DELETE', data }),
}
