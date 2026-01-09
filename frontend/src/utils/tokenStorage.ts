/**
 * Token 安全存储工具
 *
 * 安全策略:
 * - Access Token: 存储在内存 + sessionStorage (短期，页面刷新后恢复)
 * - Refresh Token: 存储在 localStorage (长期)
 * - User Info: 存储在 localStorage (非敏感信息)
 *
 * 注意: 理想情况下 Refresh Token 应存储在 HttpOnly Cookie 中
 * TODO: 未来版本考虑迁移到 HttpOnly Cookie
 */

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const USER_INFO_KEY = 'userInfo'

// 内存中的 Access Token (XSS 攻击时比 localStorage 更安全)
let memoryAccessToken: string | null = null

export const tokenStorage = {
  /**
   * 获取 Access Token
   * 优先从内存获取，页面刷新后从 sessionStorage 恢复
   */
  getAccessToken(): string | null {
    if (memoryAccessToken) {
      return memoryAccessToken
    }
    // 页面刷新后从 sessionStorage 恢复
    const sessionToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
    if (sessionToken) {
      memoryAccessToken = sessionToken
    }
    return sessionToken
  },

  /**
   * 设置 Access Token
   * 同时存储到内存和 sessionStorage
   */
  setAccessToken(token: string): void {
    memoryAccessToken = token
    // 备份到 sessionStorage (页面刷新后恢复)
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
  },

  /**
   * 获取 Refresh Token
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },

  /**
   * 设置 Refresh Token
   * TODO: 未来应改为 HttpOnly Cookie
   */
  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
  },

  /**
   * 获取用户信息
   */
  getUserInfo<T = unknown>(): T | null {
    const info = localStorage.getItem(USER_INFO_KEY)
    if (info) {
      try {
        return JSON.parse(info) as T
      } catch {
        return null
      }
    }
    return null
  },

  /**
   * 设置用户信息
   */
  setUserInfo(userInfo: unknown): void {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  },

  /**
   * 清除所有认证信息
   */
  clearAll(): void {
    memoryAccessToken = null
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  },

  /**
   * 检测是否有有效 Token
   */
  hasTokens(): boolean {
    return !!(this.getAccessToken() || this.getRefreshToken())
  },

  /**
   * 检测是否已登录（有 access token）
   */
  isLoggedIn(): boolean {
    return !!this.getAccessToken()
  }
}

// 导出默认实例
export default tokenStorage
