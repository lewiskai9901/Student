/**
 * 权限管理模块类型定义 - DDD架构适配
 */

// 权限类型
export type PermissionType = 'MENU' | 'BUTTON' | 'API' | 'DATA'

// 权限
export interface Permission {
  id: number
  permissionCode: string
  permissionName: string
  type: PermissionType
  parentId: number | null
  path?: string
  icon?: string
  sortOrder: number
  enabled: boolean
  description?: string
  children?: Permission[]
}

// 创建权限请求
export interface CreatePermissionRequest {
  permissionCode: string
  permissionName: string
  type: PermissionType
  parentId?: number
  path?: string
  icon?: string
  sortOrder?: number
  description?: string
}

// 更新权限请求
export interface UpdatePermissionRequest {
  permissionName?: string
  path?: string
  icon?: string
  sortOrder?: number
  description?: string
  enabled?: boolean
}

// 角色类型
export type RoleType =
  | 'SUPER_ADMIN'
  | 'SYSTEM_ADMIN'
  | 'DEPT_ADMIN'
  | 'GRADE_DIRECTOR'
  | 'CLASS_TEACHER'
  | 'INSPECTOR'
  | 'USER'
  | 'CUSTOM'

// 数据权限范围
export type DataScope = 'ALL' | 'DEPARTMENT_AND_BELOW' | 'DEPARTMENT' | 'CUSTOM' | 'SELF'

// 角色
export interface Role {
  id: number
  roleCode: string
  roleName: string
  roleType: RoleType
  dataScope: DataScope
  level: number
  description?: string
  enabled: boolean
  permissionIds: number[]
  permissions?: Permission[]
  createdAt: string
  updatedAt: string
}

// 创建角色请求
export interface CreateRoleRequest {
  roleCode: string
  roleName: string
  roleType?: RoleType
  dataScope?: DataScope
  level?: number
  description?: string
}

// 更新角色请求
export interface UpdateRoleRequest {
  roleName?: string
  dataScope?: DataScope
  level?: number
  description?: string
  enabled?: boolean
}

// 设置权限请求
export interface SetPermissionsRequest {
  permissionIds: number[]
}

// 用户角色
export interface UserRole {
  id: number
  userId: number
  roleId: number
  roleName: string
  roleCode: string
  orgUnitId?: number
  orgUnitName?: string
  assignedAt: string
  assignedBy?: number
  assignedByName?: string
}

// 分配角色请求（带范围）
export interface AssignRoleWithScopeRequest {
  roleId: number
  orgUnitId?: number
}

// 设置用户角色请求
export interface SetUserRolesRequest {
  roleAssignments: AssignRoleWithScopeRequest[]
}

// 角色查询参数
export interface RoleQueryParams {
  roleType?: RoleType
  enabled?: boolean
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 权限查询参数
export interface PermissionQueryParams {
  type?: PermissionType
  enabled?: boolean
  parentId?: number
  keyword?: string
}

// 角色类型显示配置
export const RoleTypeConfig: Record<RoleType, { label: string; level: number; color: string }> = {
  SUPER_ADMIN: { label: '超级管理员', level: 0, color: '#F56C6C' },
  SYSTEM_ADMIN: { label: '系统管理员', level: 1, color: '#E6A23C' },
  DEPT_ADMIN: { label: '部门管理员', level: 2, color: '#409EFF' },
  GRADE_DIRECTOR: { label: '年级主任', level: 3, color: '#67C23A' },
  CLASS_TEACHER: { label: '班主任', level: 4, color: '#909399' },
  INSPECTOR: { label: '检查员', level: 5, color: '#909399' },
  USER: { label: '普通用户', level: 6, color: '#909399' },
  CUSTOM: { label: '自定义角色', level: 99, color: '#909399' }
}

// 数据范围显示配置
export const DataScopeConfig: Record<DataScope, { label: string; description: string }> = {
  ALL: { label: '全部数据', description: '可查看所有数据' },
  DEPARTMENT_AND_BELOW: { label: '部门及以下', description: '可查看本部门及下级部门数据' },
  DEPARTMENT: { label: '本部门', description: '仅可查看本部门数据' },
  CUSTOM: { label: '自定义', description: '根据配置查看指定范围数据' },
  SELF: { label: '仅自己', description: '仅可查看自己的数据' }
}

// 权限类型显示配置
export const PermissionTypeConfig: Record<PermissionType, { label: string; icon: string }> = {
  MENU: { label: '菜单', icon: 'Menu' },
  BUTTON: { label: '按钮', icon: 'Pointer' },
  API: { label: '接口', icon: 'Connection' },
  DATA: { label: '数据', icon: 'Document' }
}

// ==================== V2 数据权限类型 ====================

/**
 * 数据模块信息
 */
export interface DataModuleInfo {
  code: string
  name: string
  domain: string
}

/**
 * 数据范围选项
 */
export interface DataScopeOption {
  code: string
  name: string
  intCode: string
}

/**
 * 模块权限配置
 */
export interface ModulePermission {
  moduleCode: string
  scopeCode: string
  customOrgUnitIds?: number[]
}

/**
 * 角色数据权限完整配置
 */
export interface RolePermissionConfig {
  roleId: number
  roleName: string
  modulePermissions: ModulePermission[]
}

/**
 * 按领域分组的模块列表
 */
export type GroupedModules = Record<string, DataModuleInfo[]>

/**
 * 领域显示配置
 */
export const DomainConfig: Record<string, { label: string; icon: string }> = {
  organization: { label: '组织管理', icon: 'OfficeBuilding' },
  inspection: { label: '量化检查', icon: 'DocumentChecked' },
  evaluation: { label: '评价管理', icon: 'Medal' },
  task: { label: '任务管理', icon: 'Tickets' }
}
