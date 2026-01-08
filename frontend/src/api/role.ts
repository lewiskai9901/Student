import { http } from '@/utils/request'

// V2 API 基础路径
const BASE_URL = '/v2/roles'

export interface Role {
  id: number
  roleName: string
  roleCode: string
  description?: string
  roleType?: string
  level?: number
  isSystem?: boolean
  isEnabled?: boolean
  dataScope?: number
  permissionIds?: number[]
  sortOrder?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface RoleQueryParams {
  pageNum: number
  pageSize: number
  roleName?: string
  roleCode?: string
  status?: number
  roleType?: string
}

export interface RoleFormData {
  roleName: string
  roleCode: string
  description?: string
  roleType?: string
  dataScope?: number
  sortOrder?: number
  status?: number
}

// 兼容V1字段映射
// V1: roleDesc -> V2: description
// V1: status -> V2: isEnabled

/**
 * 获取角色分页列表
 * 注意: V2返回列表而非分页,需要前端处理
 */
export function getRolePage(params: RoleQueryParams) {
  return http.get<Role[]>(BASE_URL, {
    params: { roleType: params.roleType }
  }).then((data: any) => {
    const roles = Array.isArray(data) ? data : []
    // 前端模拟分页
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return {
      records: roles.slice(start, end),
      total: roles.length,
      current: params.pageNum,
      size: params.pageSize
    }
  })
}

/**
 * 获取所有角色列表
 */
export function getAllRoles() {
  return http.get<Role[]>(BASE_URL)
}

/**
 * 获取角色详情
 */
export function getRoleDetail(id: number) {
  return http.get<Role>(`${BASE_URL}/${id}`)
}

/**
 * 创建角色
 */
export function createRole(data: RoleFormData) {
  return http.post<number>(BASE_URL, {
    roleCode: data.roleCode,
    roleName: data.roleName,
    description: data.description,
    roleType: data.roleType || 'CUSTOM',
    dataScope: data.dataScope
  })
}

/**
 * 更新角色
 */
export function updateRole(id: number, data: RoleFormData) {
  return http.put(`${BASE_URL}/${id}`, {
    roleName: data.roleName,
    description: data.description,
    dataScope: data.dataScope
  })
}

/**
 * 删除角色
 */
export function deleteRole(id: number) {
  return http.delete(`${BASE_URL}/${id}`)
}

/**
 * 批量删除角色
 */
export function batchDeleteRoles(ids: number[]) {
  // V2暂不支持批量删除,逐个删除
  return Promise.all(ids.map(id => deleteRole(id)))
}

/**
 * 检查角色编码是否存在
 * 注意: V2暂无此端点
 */
export function checkRoleCodeExists(roleCode: string, excludeId?: number) {
  return Promise.resolve(false)
}

/**
 * 获取角色的权限ID列表
 */
export function getRolePermissions(id: number) {
  return http.get<Role>(`${BASE_URL}/${id}`).then((role: any) => {
    return role.permissionIds || []
  })
}

/**
 * 为角色分配权限
 */
export function assignRolePermissions(id: number, permissionIds: number[]) {
  return http.put(`${BASE_URL}/${id}/permissions`, { permissionIds })
}

/**
 * 为角色分配用户
 */
export function assignRoleUsers(id: number, userIds: number[]) {
  return http.post(`/roles/${id}/users`, userIds)
}

/**
 * 从角色中移除用户
 */
export function removeRoleUsers(id: number, userIds: number[]) {
  return http.delete(`/roles/${id}/users`, { data: userIds })
}

// ==================== 数据权限相关 ====================

/**
 * 数据权限配置
 */
export interface RoleDataPermission {
  moduleCode: string
  moduleName: string
  dataScope: number
  dataScopeName?: string
  customDeptIds?: string
  customClassIds?: string
}

/**
 * 数据范围选项
 */
export const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '本部门' },
  { value: 3, label: '本年级' },
  { value: 4, label: '本班级' },
  { value: 5, label: '仅本人' }
]

/**
 * 获取角色的数据权限配置
 */
export function getRoleDataPermissions(id: number) {
  return http.get<RoleDataPermission[]>(`/roles/${id}/data-permissions`)
}

/**
 * 保存角色的数据权限配置
 */
export function saveRoleDataPermissions(id: number, permissions: RoleDataPermission[]) {
  return http.post(`/roles/${id}/data-permissions`, permissions)
}
