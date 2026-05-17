/**
 * Token 安全存储工具
 *
 * 安全策略:
 * - Access Token: 存储在内存 + sessionStorage (短期，页面刷新后恢复)
 * - Refresh Token: 存储在 localStorage (长期)
 * - User Info: 存储在 localStorage (非敏感信息)
 *
 * K1 (2026-05-17): 所有 localStorage/sessionStorage 调用走 safeStorage,
 * Safari 隐私模式 / 配额耗尽时静默 fallback, 不再 throw QuotaExceededError 崩登录流.
 *
 * 注意: 理想情况下 Refresh Token 应存储在 HttpOnly Cookie 中
 * TODO: 未来版本考虑迁移到 HttpOnly Cookie
 */
import { safeLocalStorage, safeSessionStorage } from '@/utils/safeStorage'

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
    const sessionToken = safeSessionStorage.getItem(ACCESS_TOKEN_KEY)
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
    // 备份到 sessionStorage (页面刷新后恢复) — 隐私模式失败时仍保留内存 token
    safeSessionStorage.setItem(ACCESS_TOKEN_KEY, token)
  },

  /**
   * 获取 Refresh Token
   */
  getRefreshToken(): string | null {
    return safeLocalStorage.getItem(REFRESH_TOKEN_KEY)
  },

  /**
   * 设置 Refresh Token
   * TODO: 未来应改为 HttpOnly Cookie
   */
  setRefreshToken(token: string): void {
    safeLocalStorage.setItem(REFRESH_TOKEN_KEY, token)
  },

  /**
   * 获取用户信息
   */
  getUserInfo<T = unknown>(): T | null {
    return safeLocalStorage.getJSON<T>(USER_INFO_KEY)
  },

  /**
   * 设置用户信息
   */
  setUserInfo(userInfo: unknown): void {
    safeLocalStorage.setJSON(USER_INFO_KEY, userInfo)
  },

  /**
   * 清除所有认证信息
   */
  clearAll(): void {
    memoryAccessToken = null
    safeSessionStorage.removeItem(ACCESS_TOKEN_KEY)
    safeLocalStorage.removeItem(REFRESH_TOKEN_KEY)
    safeLocalStorage.removeItem(USER_INFO_KEY)
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
