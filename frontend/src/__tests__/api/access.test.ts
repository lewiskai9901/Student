/**
 * F2: access (权限/角色) API 单测.
 *
 * 验证 URL 拼装、payload、HTTP 方法.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import * as accessApi from '@/api/access'
import { http } from '@/utils/request'

describe('access API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('权限 API', () => {
    it('getPermissions GET /permissions', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await accessApi.getPermissions()
      expect(http.get).toHaveBeenCalledWith('/permissions', { params: undefined })
    })

    it('getPermissions 透传 includeDisabled', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await accessApi.getPermissions({ includeDisabled: true } as any)
      const [, opts] = vi.mocked(http.get).mock.calls[0]
      expect((opts as any).params.includeDisabled).toBe(true)
    })

    it('getPermissionTree GET /permissions/tree', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await accessApi.getPermissionTree()
      expect(http.get).toHaveBeenCalledWith('/permissions/tree')
    })

    it('getPermission GET /permissions/{id}', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({} as any)
      await accessApi.getPermission(42)
      expect(http.get).toHaveBeenCalledWith('/permissions/42')
    })

    it('createPermission POST /permissions', async () => {
      vi.mocked(http.post).mockResolvedValueOnce({} as any)
      await accessApi.createPermission({ permissionCode: 'p:x' } as any)
      expect(http.post).toHaveBeenCalledWith('/permissions', { permissionCode: 'p:x' })
    })

    it('updatePermission PUT /permissions/{id}', async () => {
      vi.mocked(http.put).mockResolvedValueOnce({} as any)
      await accessApi.updatePermission('5', { name: 'X' } as any)
      expect(http.put).toHaveBeenCalledWith('/permissions/5', { name: 'X' })
    })

    it('deletePermission DELETE', async () => {
      vi.mocked(http.delete).mockResolvedValueOnce(undefined)
      await accessApi.deletePermission(7)
      expect(http.delete).toHaveBeenCalledWith('/permissions/7')
    })

    it('enable / disable POST', async () => {
      vi.mocked(http.post).mockResolvedValue(undefined)
      await accessApi.enablePermission(1)
      expect(http.post).toHaveBeenLastCalledWith('/permissions/1/enable')
      await accessApi.disablePermission(2)
      expect(http.post).toHaveBeenLastCalledWith('/permissions/2/disable')
    })
  })

  describe('角色 API', () => {
    it('getRoles 透传所有筛选参数', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await accessApi.getRoles({ roleType: 'SYSTEM', enabled: true, keyword: 'admin' } as any)
      const [, opts] = vi.mocked(http.get).mock.calls[0]
      expect((opts as any).params).toEqual({
        roleType: 'SYSTEM',
        enabled: true,
        keyword: 'admin',
        includeDisabled: undefined,
      })
    })

    it('getRolesPage 切分页', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([
        { id: '1' }, { id: '2' }, { id: '3' }, { id: '4' }, { id: '5' },
      ])
      const page = await accessApi.getRolesPage({ pageNum: 1, pageSize: 2 } as any)
      expect(page.records).toEqual([{ id: '1' }, { id: '2' }])
      expect(page.total).toBe(5)
      expect(page.size).toBe(2)
    })

    it('getRolesPage 第二页正确切片', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([
        { id: '1' }, { id: '2' }, { id: '3' },
      ])
      const page = await accessApi.getRolesPage({ pageNum: 2, pageSize: 2 } as any)
      expect(page.records).toEqual([{ id: '3' }])
    })

    it('getRolePermissionIds 解构 permissionIds', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({ id: '1', permissionIds: ['p1', 'p2'] } as any)
      const ids = await accessApi.getRolePermissionIds('1')
      expect(ids).toEqual(['p1', 'p2'])
    })

    it('getRolePermissionIds permissionIds 缺失时返回 []', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({ id: '1' } as any)
      const ids = await accessApi.getRolePermissionIds('1')
      expect(ids).toEqual([])
    })

    it('createRole POST', async () => {
      vi.mocked(http.post).mockResolvedValueOnce({} as any)
      await accessApi.createRole({ roleCode: 'X' } as any)
      expect(http.post).toHaveBeenCalledWith('/roles', { roleCode: 'X' })
    })

    it('setRolePermissions PUT 携带 permissionIds', async () => {
      vi.mocked(http.put).mockResolvedValueOnce({} as any)
      await accessApi.setRolePermissions('5', [1, 2, 3])
      expect(http.put).toHaveBeenCalledWith('/roles/5/permissions', { permissionIds: [1, 2, 3] })
    })

    it('batchDeleteRoles 并发 DELETE', async () => {
      vi.mocked(http.delete).mockResolvedValue(undefined)
      await accessApi.batchDeleteRoles([1, 2, 3])
      expect(http.delete).toHaveBeenCalledTimes(3)
    })
  })

  describe('用户角色 API', () => {
    it('getUserRoles GET /users/{id}/roles', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await accessApi.getUserRoles(7)
      expect(http.get).toHaveBeenCalledWith('/users/7/roles')
    })

    it('assignRoleToUser POST URL 拼接', async () => {
      vi.mocked(http.post).mockResolvedValueOnce(undefined)
      await accessApi.assignRoleToUser(1, 2)
      expect(http.post).toHaveBeenCalledWith('/users/1/roles/2')
    })

    it('removeUserRoleWithScope 携带 scopeType / scopeId', async () => {
      vi.mocked(http.delete).mockResolvedValueOnce(undefined)
      await accessApi.removeUserRoleWithScope(1, 2, 'ORG_UNIT', 99)
      expect(http.delete).toHaveBeenCalledWith('/users/1/roles/2', {
        params: { scopeType: 'ORG_UNIT', scopeId: 99 },
      })
    })

    it('checkPermission GET 携带 permissionCode 参数', async () => {
      vi.mocked(http.get).mockResolvedValueOnce(true)
      await accessApi.checkPermission('user:read')
      expect(http.get).toHaveBeenCalledWith('/users/current/check', {
        params: { permissionCode: 'user:read' },
      })
    })

    it('checkPermissions POST 批量', async () => {
      vi.mocked(http.post).mockResolvedValueOnce({})
      await accessApi.checkPermissions(['a', 'b'])
      expect(http.post).toHaveBeenCalledWith('/users/current/check-batch', {
        permissionCodes: ['a', 'b'],
      })
    })
  })

  describe('数据权限 API', () => {
    it('getRoleDataPermissions GET', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({} as any)
      await accessApi.getRoleDataPermissions('5')
      expect(http.get).toHaveBeenCalledWith('/roles/5/data-permissions')
    })

    it('saveRoleDataPermissions PUT 携带 config', async () => {
      vi.mocked(http.put).mockResolvedValueOnce(undefined)
      await accessApi.saveRoleDataPermissions('5', { x: 1 } as any)
      expect(http.put).toHaveBeenCalledWith('/roles/5/data-permissions', { x: 1 })
    })

    it('getDataModules 默认 includeDisabled=false', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await accessApi.getDataModules()
      expect(http.get).toHaveBeenCalledWith('/data-modules', { params: { includeDisabled: false } })
    })

    it('getDataModules 显式 true', async () => {
      vi.mocked(http.get).mockResolvedValueOnce([])
      await accessApi.getDataModules(true)
      expect(http.get).toHaveBeenCalledWith('/data-modules', { params: { includeDisabled: true } })
    })

    it('getDataModulesForRole 把 roleId 转字符串', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({} as any)
      await accessApi.getDataModulesForRole({ roleId: 99 })
      const [, opts] = vi.mocked(http.get).mock.calls[0]
      expect((opts as any).params.roleId).toBe('99')
      expect((opts as any).params.includeDisabled).toBe(false)
    })

    it('getDataModulesForRole roleId=undefined 时不传', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({} as any)
      await accessApi.getDataModulesForRole({ includeDisabled: true })
      const [, opts] = vi.mocked(http.get).mock.calls[0]
      expect((opts as any).params.roleId).toBeUndefined()
      expect((opts as any).params.includeDisabled).toBe(true)
    })
  })

  describe('数据权限模拟 API', () => {
    it('simulate POST 携带 userId + modulePermissions', async () => {
      vi.mocked(http.post).mockResolvedValueOnce({} as any)
      const req = {
        userId: 1,
        modulePermissions: [{ moduleCode: 'student', scopeCode: 'ALL' }],
      }
      await accessApi.dataPermissionSimulateApi.simulate(req as any)
      expect(http.post).toHaveBeenCalledWith('/access/data-permissions/simulate', req)
    })
  })

  describe('checkPermissionSync', () => {
    it('GET /system/permission-sync/check', async () => {
      vi.mocked(http.get).mockResolvedValueOnce({} as any)
      await accessApi.checkPermissionSync()
      expect(http.get).toHaveBeenCalledWith('/system/permission-sync/check')
    })
  })
})
