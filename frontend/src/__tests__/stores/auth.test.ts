/**
 * F2: useAuthStore 单测
 *
 * 覆盖 login / logout / refresh / hasPermission / hasRole / initAuth
 * 关键路径与边界 (401, 缺 refresh token, /me 失败 fallback).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock api/auth & utils — 必须在 import store 之前
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  refreshToken: vi.fn(),
  getCurrentUser: vi.fn(),
}))

vi.mock('@/utils/token', () => ({
  getToken: vi.fn(),
  setToken: vi.fn(),
  isTokenExpired: vi.fn(),
}))

vi.mock('@/utils/tokenStorage', () => ({
  tokenStorage: {
    setRefreshToken: vi.fn(),
    getRefreshToken: vi.fn(),
    setUserInfo: vi.fn(),
    getUserInfo: vi.fn(),
    clearAll: vi.fn(),
  },
}))

// Bootstrap 路由插件加载 stub — login 后会动态 import @/router 和 @/router/bootstrap
vi.mock('@/router', () => ({
  default: { addRoute: vi.fn(), getRoutes: () => [] },
}))
vi.mock('@/router/bootstrap', () => ({
  loadEnabledPlugins: vi.fn().mockResolvedValue(undefined),
}))

import { useAuthStore } from '@/stores/auth'
import * as authApi from '@/api/auth'
import * as tokenUtil from '@/utils/token'
import { tokenStorage } from '@/utils/tokenStorage'

const mockUserInfo = {
  id: 1,
  username: 'admin',
  realName: '管理员',
  permissions: ['user:read', 'user:write'],
  roles: ['ADMIN', 'TEACHER'],
  tenantId: 100,
  tenantName: '默认租户',
} as any

const mockLoginResponse = {
  accessToken: 'access-xyz',
  refreshToken: 'refresh-xyz',
  userInfo: mockUserInfo,
} as any

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('loginAction', () => {
    it('成功登录后写入 token / user / permissions', async () => {
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      const store = useAuthStore()
      await store.loginAction({ username: 'admin', password: 'admin123' } as any)
      expect(store.token).toBe('access-xyz')
      expect(store.user).toEqual(mockUserInfo)
      expect(store.permissions).toEqual(['user:read', 'user:write'])
      expect(store.tenantId).toBe(100)
    })

    it('登录调用 setToken 与 tokenStorage.setRefreshToken', async () => {
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      const store = useAuthStore()
      await store.loginAction({ username: 'a', password: 'b' } as any)
      expect(tokenUtil.setToken).toHaveBeenCalledWith('access-xyz')
      expect(tokenStorage.setRefreshToken).toHaveBeenCalledWith('refresh-xyz')
      expect(tokenStorage.setUserInfo).toHaveBeenCalledWith(mockUserInfo)
    })

    it('登录失败抛错且不写状态', async () => {
      vi.mocked(authApi.login).mockRejectedValueOnce(new Error('bad creds'))
      const store = useAuthStore()
      await expect(store.loginAction({ username: 'a', password: 'b' } as any)).rejects.toThrow('bad creds')
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
    })

    it('登录返回值是原始 response', async () => {
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      const store = useAuthStore()
      const result = await store.loginAction({ username: 'a', password: 'b' } as any)
      expect(result).toBe(mockLoginResponse)
    })
  })

  describe('logoutAction', () => {
    it('成功 logout 后清空所有状态', async () => {
      vi.mocked(authApi.logout).mockResolvedValueOnce(undefined)
      const store = useAuthStore()
      // 先注入 state
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)

      await store.logoutAction()
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(store.permissions).toEqual([])
      expect(tokenStorage.clearAll).toHaveBeenCalled()
    })

    it('logout 401 视为已失效, 仍清空本地状态', async () => {
      const store = useAuthStore()
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)

      const err: any = new Error('expired')
      err.response = { status: 401 }
      vi.mocked(authApi.logout).mockRejectedValueOnce(err)
      await store.logoutAction()
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
    })

    it('logout 5xx 时保留状态并抛错', async () => {
      const store = useAuthStore()
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)

      const err: any = new Error('server')
      err.response = { status: 500 }
      vi.mocked(authApi.logout).mockRejectedValueOnce(err)
      await expect(store.logoutAction()).rejects.toThrow('server')
      // 状态保留
      expect(store.token).toBe('access-xyz')
    })

    it('无 token 时不调用后端 logout', async () => {
      const store = useAuthStore()
      await store.logoutAction()
      expect(authApi.logout).not.toHaveBeenCalled()
    })
  })

  describe('refreshTokenAction', () => {
    it('无 refresh token 抛错并触发 logout', async () => {
      const store = useAuthStore()
      await expect(store.refreshTokenAction()).rejects.toThrow('没有刷新令牌')
    })

    it('成功刷新写入新 access/refresh', async () => {
      const store = useAuthStore()
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)

      vi.mocked(authApi.refreshToken).mockResolvedValueOnce({
        ...mockLoginResponse,
        accessToken: 'new-access',
        refreshToken: 'new-refresh',
      } as any)
      await store.refreshTokenAction()
      expect(store.token).toBe('new-access')
      expect(tokenUtil.setToken).toHaveBeenCalledWith('new-access')
    })

    it('refresh 失败时清空本地并抛错', async () => {
      const store = useAuthStore()
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)
      // logout 在 catch 内被调用 — mock 一下 (无 token 也走 logoutAction 清空)
      vi.mocked(authApi.refreshToken).mockRejectedValueOnce(new Error('expired'))
      vi.mocked(authApi.logout).mockResolvedValueOnce(undefined)
      await expect(store.refreshTokenAction()).rejects.toThrow('expired')
    })
  })

  describe('hasPermission / hasRole', () => {
    it('hasPermission 返回 false 当未登录', () => {
      const store = useAuthStore()
      expect(store.hasPermission('user:read')).toBe(false)
    })

    it('hasPermission 命中具体 permission', async () => {
      const store = useAuthStore()
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)
      expect(store.hasPermission('user:read')).toBe(true)
      expect(store.hasPermission('user:delete')).toBe(false)
    })

    it('hasPermission 通配符 *', async () => {
      const store = useAuthStore()
      const resp = { ...mockLoginResponse, userInfo: { ...mockUserInfo, permissions: ['*'] } }
      vi.mocked(authApi.login).mockResolvedValueOnce(resp as any)
      await store.loginAction({ username: 'a', password: 'b' } as any)
      expect(store.hasPermission('anything:anywhere')).toBe(true)
    })

    it('hasRole 命中', async () => {
      const store = useAuthStore()
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)
      expect(store.hasRole('ADMIN')).toBe(true)
      expect(store.hasRole('STUDENT')).toBe(false)
    })

    it('hasRole 用户为 null 时返回 false', () => {
      const store = useAuthStore()
      expect(store.hasRole('ADMIN')).toBe(false)
    })
  })

  describe('userName / userRoles 计算属性', () => {
    it('未登录时 userName 为空字符串', () => {
      const store = useAuthStore()
      expect(store.userName).toBe('')
      expect(store.userRoles).toEqual([])
    })

    it('登录后 userName 等于 realName', async () => {
      const store = useAuthStore()
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)
      expect(store.userName).toBe('管理员')
      expect(store.userRoles).toEqual(['ADMIN', 'TEACHER'])
    })
  })

  describe('initAuth', () => {
    it('无 saved token 时不做任何事', async () => {
      vi.mocked(tokenUtil.getToken).mockReturnValueOnce(null)
      const store = useAuthStore()
      await store.initAuth()
      expect(store.token).toBe('')
    })

    it('saved token 已过期 — 不恢复', async () => {
      vi.mocked(tokenUtil.getToken).mockReturnValueOnce('expired')
      vi.mocked(tokenUtil.isTokenExpired).mockReturnValueOnce(true)
      const store = useAuthStore()
      await store.initAuth()
      expect(store.token).toBe('')
    })

    it('saved token 有效 + 有缓存的 user_info — 直接恢复', async () => {
      vi.mocked(tokenUtil.getToken).mockReturnValueOnce('valid')
      vi.mocked(tokenUtil.isTokenExpired).mockReturnValueOnce(false)
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValueOnce('rt')
      vi.mocked(tokenStorage.getUserInfo).mockReturnValueOnce(mockUserInfo)
      const store = useAuthStore()
      await store.initAuth()
      expect(store.token).toBe('valid')
      expect(store.user).toEqual(mockUserInfo)
      expect(store.permissions).toEqual(['user:read', 'user:write'])
    })

    it('saved token 有效但 user_info 缺失 — fetch /me 自愈', async () => {
      vi.mocked(tokenUtil.getToken).mockReturnValueOnce('valid')
      vi.mocked(tokenUtil.isTokenExpired).mockReturnValueOnce(false)
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValueOnce(null)
      vi.mocked(tokenStorage.getUserInfo).mockReturnValueOnce(null)
      vi.mocked(authApi.getCurrentUser).mockResolvedValueOnce(mockUserInfo)
      const store = useAuthStore()
      await store.initAuth()
      expect(authApi.getCurrentUser).toHaveBeenCalled()
      expect(store.user).toEqual(mockUserInfo)
    })

    it('user_info 缺失且 /me 失败 — 清空状态', async () => {
      vi.mocked(tokenUtil.getToken).mockReturnValueOnce('valid')
      vi.mocked(tokenUtil.isTokenExpired).mockReturnValueOnce(false)
      vi.mocked(tokenStorage.getUserInfo).mockReturnValueOnce(null)
      vi.mocked(authApi.getCurrentUser).mockRejectedValueOnce(new Error('boom'))
      const store = useAuthStore()
      await store.initAuth()
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
    })
  })

  describe('isAuthenticated', () => {
    it('无 token 时 false', () => {
      const store = useAuthStore()
      expect(store.isAuthenticated).toBe(false)
    })

    it('有 token 且未过期 true', async () => {
      const store = useAuthStore()
      vi.mocked(tokenUtil.isTokenExpired).mockReturnValue(false)
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)
      expect(store.isAuthenticated).toBe(true)
    })

    it('有 token 但过期返回 false', async () => {
      const store = useAuthStore()
      vi.mocked(authApi.login).mockResolvedValueOnce(mockLoginResponse)
      await store.loginAction({ username: 'a', password: 'b' } as any)
      vi.mocked(tokenUtil.isTokenExpired).mockReturnValueOnce(true)
      expect(store.isAuthenticated).toBe(false)
    })
  })
})
