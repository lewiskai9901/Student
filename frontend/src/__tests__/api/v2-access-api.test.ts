import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock the http client before importing API modules
vi.mock('@/utils/request', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn()
  }
}))

import { http } from '@/utils/request'
import {
  getPermissions,
  getPermissionTree,
  getPermission,
  createPermission,
  updatePermission,
  deletePermission,
  enablePermission,
  disablePermission,
  getRoles,
  getAllRoles,
  getRole,
  createRole,
  updateRole,
  deleteRole,
  enableRole,
  disableRole,
  setRolePermissions,
  getRolePermissions,
  getRoleUsers,
  getUserRoles,
  assignRoleToUser,
  setUserRoles,
  removeUserRole,
  getCurrentUserPermissions,
  getCurrentUserRoles,
  checkPermission,
  checkPermissions,
  permissionApi,
  roleApi,
  userRoleApi,
  DATA_SCOPE_OPTIONS
} from '@/api/access'

const mockedHttp = vi.mocked(http)

/**
 * V2 权限管理 API 测试
 */
describe('V2 Access API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Permission API', () => {
    describe('getPermissions', () => {
      it('should call GET /permissions', async () => {
        const mockPermissions = [{ id: 1, permissionCode: 'user:read', permissionName: '查看用户' }]
        mockedHttp.get.mockResolvedValue(mockPermissions)

        const result = await getPermissions()

        expect(mockedHttp.get).toHaveBeenCalledWith('/permissions', { params: undefined })
        expect(result).toEqual(mockPermissions)
      })

      it('should call with query params', async () => {
        const mockPermissions = [{ id: 1, permissionCode: 'user:read' }]
        mockedHttp.get.mockResolvedValue(mockPermissions)

        const params = { enabled: true }
        await getPermissions(params)

        expect(mockedHttp.get).toHaveBeenCalledWith('/permissions', { params })
      })
    })

    describe('getPermissionTree', () => {
      it('should call GET /permissions/tree', async () => {
        const mockTree = [{ id: 1, permissionName: '系统管理', children: [] }]
        mockedHttp.get.mockResolvedValue(mockTree)

        const result = await getPermissionTree()

        expect(mockedHttp.get).toHaveBeenCalledWith('/permissions/tree')
        expect(result).toEqual(mockTree)
      })
    })

    describe('getPermission', () => {
      it('should call GET /permissions/:id', async () => {
        const mockPermission = { id: 1, permissionCode: 'user:read' }
        mockedHttp.get.mockResolvedValue(mockPermission)

        const result = await getPermission(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/permissions/1')
        expect(result).toEqual(mockPermission)
      })
    })

    describe('createPermission', () => {
      it('should call POST /permissions', async () => {
        const mockPermission = { id: 1, permissionCode: 'user:create' }
        mockedHttp.post.mockResolvedValue(mockPermission)

        const data = { permissionCode: 'user:create', permissionName: '创建用户' }
        const result = await createPermission(data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/permissions', data)
        expect(result).toEqual(mockPermission)
      })
    })

    describe('updatePermission', () => {
      it('should call PUT /permissions/:id', async () => {
        const mockPermission = { id: 1, permissionName: '更新后的权限' }
        mockedHttp.put.mockResolvedValue(mockPermission)

        const data = { permissionName: '更新后的权限' }
        const result = await updatePermission(1, data)

        expect(mockedHttp.put).toHaveBeenCalledWith('/permissions/1', data)
        expect(result).toEqual(mockPermission)
      })
    })

    describe('deletePermission', () => {
      it('should call DELETE /permissions/:id', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await deletePermission(1)

        expect(mockedHttp.delete).toHaveBeenCalledWith('/permissions/1')
      })
    })

    describe('enablePermission', () => {
      it('should call POST /permissions/:id/enable', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        await enablePermission(1)

        expect(mockedHttp.post).toHaveBeenCalledWith('/permissions/1/enable')
      })
    })

    describe('disablePermission', () => {
      it('should call POST /permissions/:id/disable', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        await disablePermission(1)

        expect(mockedHttp.post).toHaveBeenCalledWith('/permissions/1/disable')
      })
    })
  })

  describe('Role API', () => {
    describe('getRoles', () => {
      it('should call GET /roles', async () => {
        const mockRoles = [{ id: 1, roleCode: 'admin', roleName: '管理员' }]
        mockedHttp.get.mockResolvedValue(mockRoles)

        const result = await getRoles()

        expect(mockedHttp.get).toHaveBeenCalledWith('/roles', { params: { roleType: undefined } })
        expect(result).toEqual(mockRoles)
      })

      it('should call with roleType param', async () => {
        const mockRoles = [{ id: 1, roleCode: 'teacher' }]
        mockedHttp.get.mockResolvedValue(mockRoles)

        await getRoles({ roleType: 'TEACHER' })

        expect(mockedHttp.get).toHaveBeenCalledWith('/roles', { params: { roleType: 'TEACHER' } })
      })
    })

    describe('getAllRoles', () => {
      it('should call GET /roles without params', async () => {
        const mockRoles = [{ id: 1, roleCode: 'admin' }, { id: 2, roleCode: 'teacher' }]
        mockedHttp.get.mockResolvedValue(mockRoles)

        const result = await getAllRoles()

        expect(mockedHttp.get).toHaveBeenCalledWith('/roles')
        expect(result).toEqual(mockRoles)
      })
    })

    describe('getRole', () => {
      it('should call GET /roles/:id', async () => {
        const mockRole = { id: 1, roleCode: 'admin', roleName: '管理员' }
        mockedHttp.get.mockResolvedValue(mockRole)

        const result = await getRole(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/roles/1')
        expect(result).toEqual(mockRole)
      })
    })

    describe('createRole', () => {
      it('should call POST /roles', async () => {
        const mockRole = { id: 1, roleCode: 'teacher', roleName: '教师' }
        mockedHttp.post.mockResolvedValue(mockRole)

        const data = { roleCode: 'teacher', roleName: '教师' }
        const result = await createRole(data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/roles', data)
        expect(result).toEqual(mockRole)
      })
    })

    describe('updateRole', () => {
      it('should call PUT /roles/:id', async () => {
        const mockRole = { id: 1, roleName: '高级教师' }
        mockedHttp.put.mockResolvedValue(mockRole)

        const data = { roleName: '高级教师' }
        const result = await updateRole(1, data)

        expect(mockedHttp.put).toHaveBeenCalledWith('/roles/1', data)
        expect(result).toEqual(mockRole)
      })
    })

    describe('deleteRole', () => {
      it('should call DELETE /roles/:id', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await deleteRole(1)

        expect(mockedHttp.delete).toHaveBeenCalledWith('/roles/1')
      })
    })

    describe('enableRole', () => {
      it('should call POST /roles/:id/enable', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        await enableRole(1)

        expect(mockedHttp.post).toHaveBeenCalledWith('/roles/1/enable')
      })
    })

    describe('disableRole', () => {
      it('should call POST /roles/:id/disable', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        await disableRole(1)

        expect(mockedHttp.post).toHaveBeenCalledWith('/roles/1/disable')
      })
    })

    describe('setRolePermissions', () => {
      it('should call PUT /roles/:id/permissions', async () => {
        const mockRole = { id: 1, permissionIds: [1, 2, 3] }
        mockedHttp.put.mockResolvedValue(mockRole)

        const result = await setRolePermissions(1, [1, 2, 3])

        expect(mockedHttp.put).toHaveBeenCalledWith('/roles/1/permissions', { permissionIds: [1, 2, 3] })
        expect(result).toEqual(mockRole)
      })
    })

    describe('getRolePermissions', () => {
      it('should call GET /roles/:id/permissions', async () => {
        const mockPermissions = [{ id: 1, permissionCode: 'user:read' }]
        mockedHttp.get.mockResolvedValue(mockPermissions)

        const result = await getRolePermissions(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/roles/1/permissions')
        expect(result).toEqual(mockPermissions)
      })
    })

    describe('getRoleUsers', () => {
      it('should call GET /roles/:id/users', async () => {
        const mockResponse = { records: [{ userId: 1, username: 'admin' }], total: 1 }
        mockedHttp.get.mockResolvedValue(mockResponse)

        const result = await getRoleUsers(1, { pageNum: 1, pageSize: 10 })

        expect(mockedHttp.get).toHaveBeenCalledWith('/roles/1/users', { params: { pageNum: 1, pageSize: 10 } })
        expect(result).toEqual(mockResponse)
      })
    })
  })

  describe('User Role API', () => {
    describe('getUserRoles', () => {
      it('should call GET /users/:id/roles', async () => {
        const mockRoles = [{ roleId: 1, roleName: '管理员' }]
        mockedHttp.get.mockResolvedValue(mockRoles)

        const result = await getUserRoles(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/users/1/roles')
        expect(result).toEqual(mockRoles)
      })
    })

    describe('assignRoleToUser', () => {
      it('should call POST /users/:id/roles', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        const data = { roleId: 1, scopeType: 'DEPARTMENT', scopeId: 100 }
        await assignRoleToUser(1, data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/users/1/roles', data)
      })
    })

    describe('setUserRoles', () => {
      it('should call PUT /users/:id/roles', async () => {
        mockedHttp.put.mockResolvedValue(undefined)

        const data = { roleIds: [1, 2] }
        await setUserRoles(1, data)

        expect(mockedHttp.put).toHaveBeenCalledWith('/users/1/roles', data)
      })
    })

    describe('removeUserRole', () => {
      it('should call DELETE /users/:userId/roles/:roleId', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await removeUserRole(1, 2)

        expect(mockedHttp.delete).toHaveBeenCalledWith('/users/1/roles/2')
      })
    })

    describe('getCurrentUserPermissions', () => {
      it('should call GET /users/current/permissions', async () => {
        const mockPermissions = [{ id: 1, permissionCode: 'user:read' }]
        mockedHttp.get.mockResolvedValue(mockPermissions)

        const result = await getCurrentUserPermissions()

        expect(mockedHttp.get).toHaveBeenCalledWith('/users/current/permissions')
        expect(result).toEqual(mockPermissions)
      })
    })

    describe('getCurrentUserRoles', () => {
      it('should call GET /users/current/roles', async () => {
        const mockRoles = [{ roleId: 1, roleName: '管理员' }]
        mockedHttp.get.mockResolvedValue(mockRoles)

        const result = await getCurrentUserRoles()

        expect(mockedHttp.get).toHaveBeenCalledWith('/users/current/roles')
        expect(result).toEqual(mockRoles)
      })
    })

    describe('checkPermission', () => {
      it('should call GET /users/current/check', async () => {
        mockedHttp.get.mockResolvedValue(true)

        const result = await checkPermission('user:read')

        expect(mockedHttp.get).toHaveBeenCalledWith('/users/current/check', { params: { permissionCode: 'user:read' } })
        expect(result).toBe(true)
      })
    })

    describe('checkPermissions', () => {
      it('should call POST /users/current/check-batch', async () => {
        const mockResult = { 'user:read': true, 'user:write': false }
        mockedHttp.post.mockResolvedValue(mockResult)

        const result = await checkPermissions(['user:read', 'user:write'])

        expect(mockedHttp.post).toHaveBeenCalledWith('/users/current/check-batch', { permissionCodes: ['user:read', 'user:write'] })
        expect(result).toEqual(mockResult)
      })
    })
  })

  describe('API Objects', () => {
    it('permissionApi should have all methods', () => {
      expect(permissionApi.getList).toBe(getPermissions)
      expect(permissionApi.getTree).toBe(getPermissionTree)
      expect(permissionApi.getById).toBe(getPermission)
      expect(permissionApi.create).toBe(createPermission)
      expect(permissionApi.update).toBe(updatePermission)
      expect(permissionApi.delete).toBe(deletePermission)
      expect(permissionApi.enable).toBe(enablePermission)
      expect(permissionApi.disable).toBe(disablePermission)
    })

    it('roleApi should have all methods', () => {
      expect(roleApi.getList).toBe(getRoles)
      expect(roleApi.getAll).toBe(getAllRoles)
      expect(roleApi.getById).toBe(getRole)
      expect(roleApi.create).toBe(createRole)
      expect(roleApi.update).toBe(updateRole)
      expect(roleApi.delete).toBe(deleteRole)
      expect(roleApi.enable).toBe(enableRole)
      expect(roleApi.disable).toBe(disableRole)
      expect(roleApi.setPermissions).toBe(setRolePermissions)
      expect(roleApi.getPermissions).toBe(getRolePermissions)
      expect(roleApi.getUsers).toBe(getRoleUsers)
    })

    it('userRoleApi should have all methods', () => {
      expect(userRoleApi.getUserRoles).toBe(getUserRoles)
      expect(userRoleApi.assignRole).toBe(assignRoleToUser)
      expect(userRoleApi.setRoles).toBe(setUserRoles)
      expect(userRoleApi.removeRole).toBe(removeUserRole)
      expect(userRoleApi.getCurrentPermissions).toBe(getCurrentUserPermissions)
      expect(userRoleApi.getCurrentRoles).toBe(getCurrentUserRoles)
      expect(userRoleApi.checkPermission).toBe(checkPermission)
      expect(userRoleApi.checkPermissions).toBe(checkPermissions)
    })
  })

  describe('Constants', () => {
    it('DATA_SCOPE_OPTIONS should have correct values', () => {
      expect(DATA_SCOPE_OPTIONS).toHaveLength(5)
      expect(DATA_SCOPE_OPTIONS[0]).toEqual({ value: 1, label: '全部数据' })
      expect(DATA_SCOPE_OPTIONS[1]).toEqual({ value: 2, label: '本部门' })
      expect(DATA_SCOPE_OPTIONS[2]).toEqual({ value: 3, label: '本年级' })
      expect(DATA_SCOPE_OPTIONS[3]).toEqual({ value: 4, label: '本班级' })
      expect(DATA_SCOPE_OPTIONS[4]).toEqual({ value: 5, label: '仅本人' })
    })
  })
})
