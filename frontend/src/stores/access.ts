import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  Permission,
  PermissionType,
  Role,
  RoleType,
  DataScope,
  UserRole
} from '@/types/access'
import {
  permissionApi,
  roleApi,
  userRoleApi
} from '@/api/access'

/**
 * 权限管理 Store (V2)
 * 基于DDD架构的权限管理状态管理
 */
export const useAccessStore = defineStore('access-v2', () => {
  // ==================== 权限状态 ====================
  const permissions = ref<Permission[]>([])
  const permissionTree = ref<Permission[]>([])
  const currentPermission = ref<Permission | null>(null)
  const permissionsLoading = ref(false)

  // ==================== 角色状态 ====================
  const roles = ref<Role[]>([])
  const currentRole = ref<Role | null>(null)
  const rolesLoading = ref(false)
  const rolesTotal = ref(0)

  // ==================== 用户角色状态 ====================
  const userRoles = ref<UserRole[]>([])
  const userRolesLoading = ref(false)

  // ==================== 计算属性 ====================

  // 按类型过滤权限
  const permissionsByType = computed(() => {
    return (type: PermissionType) => permissions.value.filter(p => p.type === type)
  })

  // 菜单权限
  const menuPermissions = computed(() => permissionsByType.value('MENU'))

  // 按钮权限
  const buttonPermissions = computed(() => permissionsByType.value('BUTTON'))

  // API权限
  const apiPermissions = computed(() => permissionsByType.value('API'))

  // 按类型过滤角色
  const rolesByType = computed(() => {
    return (type: RoleType) => roles.value.filter(r => r.roleType === type)
  })

  // 系统角色
  const systemRoles = computed(() =>
    roles.value.filter(r =>
      r.roleType === 'SUPER_ADMIN' ||
      r.roleType === 'SYSTEM_ADMIN' ||
      r.roleType === 'DEPT_ADMIN'
    )
  )

  // 自定义角色
  const customRoles = computed(() =>
    roles.value.filter(r => r.roleType === 'CUSTOM')
  )

  // ==================== 权限操作 ====================

  /**
   * 加载权限列表
   */
  const loadPermissions = async () => {
    permissionsLoading.value = true
    try {
      const data = await permissionApi.getList()
      // request.ts 已解包，data 直接是响应数据
      permissions.value = Array.isArray(data) ? data : []
    } catch (error) {
      console.error('加载权限列表失败:', error)
    } finally {
      permissionsLoading.value = false
    }
  }

  /**
   * 加载权限树
   */
  const loadPermissionTree = async () => {
    permissionsLoading.value = true
    try {
      const data = await permissionApi.getTree()
      // request.ts 已解包，data 直接是响应数据
      permissionTree.value = Array.isArray(data) ? data : []
    } catch (error) {
      console.error('加载权限树失败:', error)
    } finally {
      permissionsLoading.value = false
    }
  }

  /**
   * 获取权限详情
   */
  const getPermission = async (id: number) => {
    try {
      const data = await permissionApi.getById(id)
      currentPermission.value = data || null
      return data
    } catch (error) {
      console.error('获取权限详情失败:', error)
      return null
    }
  }

  /**
   * 创建权限
   */
  const createPermission = async (data: Parameters<typeof permissionApi.create>[0]) => {
    const result = await permissionApi.create(data)
    await loadPermissions()
    await loadPermissionTree()
    return result
  }

  /**
   * 更新权限
   */
  const updatePermission = async (id: number, data: Parameters<typeof permissionApi.update>[1]) => {
    const result = await permissionApi.update(id, data)
    await loadPermissions()
    await loadPermissionTree()
    return result
  }

  /**
   * 删除权限
   */
  const deletePermission = async (id: number) => {
    const result = await permissionApi.delete(id)
    await loadPermissions()
    await loadPermissionTree()
    return result
  }

  /**
   * 启用权限
   */
  const enablePermission = async (id: number) => {
    const result = await permissionApi.enable(id)
    await loadPermissions()
    await loadPermissionTree()
    return result
  }

  /**
   * 禁用权限
   */
  const disablePermission = async (id: number) => {
    const result = await permissionApi.disable(id)
    await loadPermissions()
    await loadPermissionTree()
    return result
  }

  // ==================== 角色操作 ====================

  /**
   * 加载角色列表
   */
  const loadRoles = async (params?: Parameters<typeof roleApi.getList>[0]) => {
    rolesLoading.value = true
    try {
      const data = await roleApi.getList(params)
      // request.ts 已解包，data 直接是响应数据
      // 兼容两种响应格式：直接数组 或 分页对象 {items/records, total}
      if (Array.isArray(data)) {
        roles.value = data
        rolesTotal.value = data.length
      } else if (data) {
        roles.value = data.items || data.records || []
        rolesTotal.value = data.total || 0
      }
    } catch (error) {
      console.error('加载角色列表失败:', error)
    } finally {
      rolesLoading.value = false
    }
  }

  /**
   * 获取角色详情
   */
  const getRole = async (id: number) => {
    try {
      const data = await roleApi.getById(id)
      currentRole.value = data || null
      return data
    } catch (error) {
      console.error('获取角色详情失败:', error)
      return null
    }
  }

  /**
   * 创建角色
   */
  const createRole = async (data: Parameters<typeof roleApi.create>[0]) => {
    const result = await roleApi.create(data)
    await loadRoles()
    return result
  }

  /**
   * 更新角色
   */
  const updateRole = async (id: number, data: Parameters<typeof roleApi.update>[1]) => {
    const result = await roleApi.update(id, data)
    await getRole(id)
    return result
  }

  /**
   * 设置角色权限
   */
  const setRolePermissions = async (roleId: number, permissionIds: number[]) => {
    const result = await roleApi.setPermissions(roleId, { permissionIds })
    await getRole(roleId)
    return result
  }

  /**
   * 获取角色权限
   */
  const getRolePermissions = async (roleId: number) => {
    try {
      const data = await roleApi.getPermissions(roleId)
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('获取角色权限失败:', error)
      return []
    }
  }

  /**
   * 删除角色
   */
  const deleteRole = async (id: number) => {
    const result = await roleApi.delete(id)
    await loadRoles()
    return result
  }

  /**
   * 启用角色
   */
  const enableRole = async (id: number) => {
    const result = await roleApi.enable(id)
    await loadRoles()
    return result
  }

  /**
   * 禁用角色
   */
  const disableRole = async (id: number) => {
    const result = await roleApi.disable(id)
    await loadRoles()
    return result
  }

  // ==================== 用户角色操作 ====================

  /**
   * 获取用户角色
   */
  const getUserRoles = async (userId: number) => {
    userRolesLoading.value = true
    try {
      const data = await userRoleApi.getUserRoles(userId)
      userRoles.value = Array.isArray(data) ? data : []
      return userRoles.value
    } catch (error) {
      console.error('获取用户角色失败:', error)
      return []
    } finally {
      userRolesLoading.value = false
    }
  }

  /**
   * 获取角色的用户
   */
  const getRoleUsers = async (roleId: number) => {
    try {
      const data = await roleApi.getUsers(roleId)
      return Array.isArray(data) ? data : (data?.records || data?.items || [])
    } catch (error) {
      console.error('获取角色用户失败:', error)
      return []
    }
  }

  /**
   * 分配角色（带范围）
   */
  const assignRoleWithScope = async (
    userId: number,
    data: Parameters<typeof userRoleApi.assignRole>[1]
  ) => {
    const result = await userRoleApi.assignRole(userId, data)
    await getUserRoles(userId)
    return result
  }

  /**
   * 设置用户角色
   */
  const setUserRoles = async (
    userId: number,
    roleIds: number[]
  ) => {
    const result = await userRoleApi.setRoles(userId, { roleAssignments: roleIds.map(roleId => ({ roleId })) })
    await getUserRoles(userId)
    return result
  }

  /**
   * 移除用户角色
   */
  const removeUserRole = async (userId: number, roleId: number) => {
    const result = await userRoleApi.removeRole(userId, roleId)
    await getUserRoles(userId)
    return result
  }

  /**
   * 检查当前用户权限
   */
  const checkUserPermission = async (permissionCode: string) => {
    try {
      const data = await userRoleApi.checkPermission(permissionCode)
      return data || false
    } catch (error) {
      console.error('检查权限失败:', error)
      return false
    }
  }

  /**
   * 获取当前用户权限
   */
  const getCurrentPermissions = async () => {
    try {
      const data = await userRoleApi.getCurrentPermissions()
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('获取当前用户权限失败:', error)
      return []
    }
  }

  /**
   * 获取当前用户角色
   */
  const getCurrentRoles = async () => {
    try {
      const data = await userRoleApi.getCurrentRoles()
      return Array.isArray(data) ? data : []
    } catch (error) {
      console.error('获取当前用户角色失败:', error)
      return []
    }
  }

  // ==================== 重置状态 ====================
  const reset = () => {
    permissions.value = []
    permissionTree.value = []
    currentPermission.value = null
    roles.value = []
    currentRole.value = null
    rolesTotal.value = 0
    userRoles.value = []
  }

  return {
    // 权限状态
    permissions,
    permissionTree,
    currentPermission,
    permissionsLoading,

    // 角色状态
    roles,
    currentRole,
    rolesLoading,
    rolesTotal,

    // 用户角色状态
    userRoles,
    userRolesLoading,

    // 计算属性
    permissionsByType,
    menuPermissions,
    buttonPermissions,
    apiPermissions,
    rolesByType,
    systemRoles,
    customRoles,

    // 权限操作
    loadPermissions,
    loadPermissionTree,
    getPermission,
    createPermission,
    updatePermission,
    deletePermission,
    enablePermission,
    disablePermission,

    // 角色操作
    loadRoles,
    getRole,
    createRole,
    updateRole,
    setRolePermissions,
    getRolePermissions,
    deleteRole,
    enableRole,
    disableRole,

    // 用户角色操作
    getUserRoles,
    getRoleUsers,
    assignRoleWithScope,
    setUserRoles,
    removeUserRole,
    checkUserPermission,
    getCurrentPermissions,
    getCurrentRoles,

    // 工具方法
    reset
  }
})
