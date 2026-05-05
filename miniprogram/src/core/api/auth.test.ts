import { describe, it, expect, vi, beforeEach } from 'vitest'
import { authApi, toUserInfo, type LoginUserInfo } from './auth'

const mockUni = {
  request: vi.fn(),
  reLaunch: vi.fn(),
  getStorageSync: vi.fn(() => undefined),
  setStorageSync: vi.fn(),
  removeStorageSync: vi.fn(),
  getSystemInfoSync: vi.fn(() => ({ platform: 'devtools', SDKVersion: '3.0.0' }))
}
;(globalThis as any).uni = mockUni

describe('authApi.login', () => {
  beforeEach(() => {
    Object.values(mockUni).forEach((fn: any) => 'mockReset' in fn && fn.mockReset())
  })

  it('unwraps Result envelope and returns LoginResp with nested userInfo', async () => {
    mockUni.request.mockImplementation((o: any) => o.success({
      statusCode: 200,
      data: {
        code: 200, message: 'ok', timestamp: 1,
        data: {
          accessToken: 'a-token',
          refreshToken: 'r-token',
          tokenType: 'Bearer',
          expiresIn: 7200,
          enabledPlugins: ['inspection', 'demo'],
          userInfo: {
            userId: 1, username: 'admin', realName: '管理员',
            roles: ['admin'], permissions: ['inspection:task:list', 'inspection:correction:list'],
            tenantId: 1
          }
        }
      }
    }))
    const r = await authApi.login('admin', 'p')
    expect(r.accessToken).toBe('a-token')
    expect(r.refreshToken).toBe('r-token')
    expect(r.enabledPlugins).toEqual(['inspection', 'demo'])
    expect(r.userInfo.userId).toBe(1)
    expect(r.userInfo.permissions).toContain('inspection:task:list')
    expect(r.userInfo.tenantId).toBe(1)
  })

  it('POSTs /auth/login with username + password and skipAuth true', async () => {
    mockUni.request.mockImplementation((o: any) => o.success({
      statusCode: 200,
      data: { code: 200, message: 'ok', timestamp: 1, data: {
        accessToken: 'a', refreshToken: 'r', tokenType: 'Bearer', expiresIn: 7200,
        enabledPlugins: [], userInfo: { userId: 1, username: 'u', realName: 'U', roles: [], permissions: [] }
      }}
    }))
    await authApi.login('admin', 'pw')
    expect(mockUni.request).toHaveBeenCalledWith(expect.objectContaining({
      url: expect.stringContaining('/auth/login'),
      method: 'POST',
      data: { username: 'admin', password: 'pw' }
    }))
    // skipAuth means no Authorization header is required; verify by checking the header object doesn't contain Authorization
    const call = mockUni.request.mock.calls[0][0]
    expect(call.header?.Authorization).toBeUndefined()
  })
})

describe('toUserInfo', () => {
  it('flattens LoginUserInfo into PluginContext.UserInfo', () => {
    const u: LoginUserInfo = {
      userId: 1, username: 'u', realName: 'U', avatar: 'av',
      roles: ['admin'], permissions: ['p:a'], tenantId: 1
    }
    expect(toUserInfo(u)).toEqual({
      id: 1, username: 'u', name: 'U', avatar: 'av', roles: ['admin']
    })
  })

  it('handles missing avatar gracefully', () => {
    const u: LoginUserInfo = {
      userId: 2, username: 'x', realName: 'X', roles: [], permissions: []
    }
    expect(toUserInfo(u).avatar).toBeUndefined()
  })

  it('defaults roles to empty array if missing', () => {
    const u = { userId: 3, username: 'y', realName: 'Y', permissions: [] } as any
    expect(toUserInfo(u).roles).toEqual([])
  })
})
