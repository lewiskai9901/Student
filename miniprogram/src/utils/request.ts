/**
 * 请求封装
 */
import { config } from './config'
import { getStorage, setStorage, removeStorage } from './storage'

// 请求配置接口
interface RequestConfig {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  showLoading?: boolean
  showError?: boolean
  timeout?: number
}

// API响应接口
interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 是否正在刷新Token
let isRefreshing = false
// 等待刷新的请求队列
let requestQueue: Array<() => void> = []

/**
 * 获取Token
 */
function getToken(): string {
  return getStorage<string>(config.TOKEN_KEY) || ''
}

/**
 * 获取RefreshToken
 */
function getRefreshToken(): string {
  return getStorage<string>(config.REFRESH_TOKEN_KEY) || ''
}

/**
 * 保存Token
 */
function saveToken(accessToken: string, refreshToken?: string): void {
  setStorage(config.TOKEN_KEY, accessToken)
  if (refreshToken) {
    setStorage(config.REFRESH_TOKEN_KEY, refreshToken)
  }
}

/**
 * 清除Token
 */
function clearToken(): void {
  removeStorage(config.TOKEN_KEY)
  removeStorage(config.REFRESH_TOKEN_KEY)
  removeStorage(config.USER_INFO_KEY)
}

/**
 * 刷新Token
 */
async function refreshTokenRequest(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false

  try {
    const response = await uni.request({
      url: config.BASE_URL + '/auth/refresh',
      method: 'POST',
      data: { refreshToken },
      header: { 'Content-Type': 'application/json' }
    })

    const result = response.data as ApiResponse<{
      accessToken: string
      refreshToken: string
    }>

    if (result.code === 200 && result.data) {
      saveToken(result.data.accessToken, result.data.refreshToken)
      return true
    }
    return false
  } catch {
    return false
  }
}

/**
 * 处理Token过期
 * @param autoRedirect 是否自动跳转登录页，默认 false
 */
async function handleTokenExpired(autoRedirect: boolean = false): Promise<boolean> {
  // 如果没有 refreshToken，直接返回失败
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    clearToken()
    if (autoRedirect) {
      uni.reLaunch({ url: '/pages/login/index' })
    }
    return false
  }

  if (isRefreshing) {
    // 等待刷新完成
    return new Promise((resolve) => {
      requestQueue.push(() => resolve(true))
    })
  }

  isRefreshing = true

  const success = await refreshTokenRequest()

  isRefreshing = false

  if (success) {
    // 执行等待队列
    requestQueue.forEach((callback) => callback())
    requestQueue = []
    return true
  } else {
    // 刷新失败
    clearToken()
    if (autoRedirect) {
      uni.reLaunch({ url: '/pages/login/index' })
    }
    return false
  }
}

/**
 * 统一请求方法
 */
export async function request<T = any>(options: RequestConfig): Promise<T> {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    showLoading = true,
    showError = true,
    timeout = config.REQUEST_TIMEOUT
  } = options

  // 显示加载
  if (showLoading) {
    uni.showLoading({ title: '加载中...', mask: true })
  }

  // 添加Token
  const token = getToken()
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }
  header['Content-Type'] = header['Content-Type'] || 'application/json'

  try {
    const response = await uni.request({
      url: config.BASE_URL + url,
      method,
      data,
      header,
      timeout
    })

    // 隐藏加载
    if (showLoading) {
      uni.hideLoading()
    }

    // 检查 HTTP 状态码
    const statusCode = response.statusCode
    if (statusCode === 401 || statusCode === 403) {
      const refreshed = await handleTokenExpired()
      if (refreshed) {
        return request<T>(options)
      }
      return Promise.reject(new Error('请先登录'))
    }

    if (statusCode >= 400) {
      const errData = response.data as any
      const errMsg = errData?.message || `请求失败 (${statusCode})`
      if (showError) {
        uni.showToast({ title: errMsg, icon: 'none', duration: 2000 })
      }
      return Promise.reject(new Error(errMsg))
    }

    const result = response.data as ApiResponse<T>

    // 处理业务错误
    if (result.code !== 200) {
      // Token过期
      if (result.code === 401) {
        const refreshed = await handleTokenExpired()
        if (refreshed) {
          // 重试请求
          return request<T>(options)
        }
        return Promise.reject(new Error('登录已过期'))
      }

      // 显示错误提示
      if (showError) {
        uni.showToast({
          title: result.message || '请求失败',
          icon: 'none',
          duration: 2000
        })
      }

      return Promise.reject(new Error(result.message))
    }

    return result.data
  } catch (error: any) {
    // 隐藏加载
    if (showLoading) {
      uni.hideLoading()
    }

    // 网络错误
    const errorMsg = error.errMsg || error.message || '网络请求失败'
    if (showError) {
      uni.showToast({
        title: errorMsg,
        icon: 'none',
        duration: 2000
      })
    }

    return Promise.reject(new Error(errorMsg))
  }
}

/**
 * GET请求
 */
export function get<T = any>(url: string, data?: any, options?: Partial<RequestConfig>): Promise<T> {
  return request<T>({ url, method: 'GET', data, ...options })
}

/**
 * POST请求
 */
export function post<T = any>(url: string, data?: any, options?: Partial<RequestConfig>): Promise<T> {
  return request<T>({ url, method: 'POST', data, ...options })
}

/**
 * PUT请求
 */
export function put<T = any>(url: string, data?: any, options?: Partial<RequestConfig>): Promise<T> {
  return request<T>({ url, method: 'PUT', data, ...options })
}

/**
 * DELETE请求
 */
export function del<T = any>(url: string, data?: any, options?: Partial<RequestConfig>): Promise<T> {
  return request<T>({ url, method: 'DELETE', data, ...options })
}

/**
 * 上传文件
 */
export function uploadFile(
  filePath: string,
  url: string = '/files/upload',
  formData?: Record<string, any>
): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.showLoading({ title: '上传中...', mask: true })

    const token = getToken()

    uni.uploadFile({
      url: config.BASE_URL + url,
      filePath,
      name: 'file',
      formData,
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        uni.hideLoading()
        try {
          const data = JSON.parse(res.data) as ApiResponse<{ url: string }>
          if (data.code === 200 && data.data?.url) {
            resolve(data.data.url)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch {
          reject(new Error('解析响应失败'))
        }
      },
      fail: (err) => {
        uni.hideLoading()
        reject(new Error(err.errMsg || '上传失败'))
      }
    })
  })
}

/**
 * 批量上传文件
 */
export async function uploadFiles(filePaths: string[]): Promise<string[]> {
  const urls: string[] = []
  for (const path of filePaths) {
    const url = await uploadFile(path)
    urls.push(url)
  }
  return urls
}

export { saveToken, clearToken, getToken }
