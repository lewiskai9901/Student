/**
 * 权限管理模块类型定义 - V6 动态化重构
 * 删除所有硬编码枚举常量，改为从 API 动态获取
 */

// 权限类型
export type PermissionType = 'MENU' | 'BUTTON' | 'API'

// 权限作用域: PUBLIC=公共, SELF=个人空间, MANAGEMENT=管理后台
export type PermissionScope = 'PUBLIC' | 'SELF' | 'MANAGEMENT'

// 权限
export interface Permission {
  id: string | number
  permissionCode: string
  permissionName: string
  type: PermissionType
  scope: PermissionScope
  parentId: string | number | null
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
  scope?: PermissionScope
  parentId?: number | string
  path?: string
  icon?: string
  sortOrder?: number
  description?: string
}

// 更新权限请求
export interface UpdatePermissionRequest {
  permissionName?: string
  scope?: PermissionScope
  path?: string
  icon?: string
  sortOrder?: number
  description?: string
  enabled?: boolean
}

// 角色 - roleType 改为自由字符串
export interface Role {
  id: string | number
  roleCode: string
  roleName: string
  roleType: string
  level: number
  description?: string
  enabled: boolean
  tenantId?: number | string
  permissionIds: (string | number)[]
  permissions?: Permission[]
  createdAt: string
  updatedAt: string
}

// 创建角色请求
export interface CreateRoleRequest {
  roleName: string
  roleCode?: string
  roleType?: string
  level?: number
  description?: string
}

// 更新角色请求
export interface UpdateRoleRequest {
  roleName?: string
  roleType?: string
  level?: number
  description?: string
  enabled?: boolean
}

// 设置权限请求
export interface SetPermissionsRequest {
  permissionIds: (number | string)[]
}

// 作用域类型
export type ScopeTypeValue = 'ALL' | 'ORG_UNIT'

// 用户角色（带作用域）
export interface UserRole {
  id: number | string
  userId: number | string
  roleId: number | string
  roleName: string
  roleCode: string
  scopeType: ScopeTypeValue
  scopeId: number | string
  scopeName?: string
  assignedAt: string
  assignedBy?: number | string
  expiresAt?: string
  isActive?: boolean
}

// 分配角色请求（带作用域）
export interface AssignRoleWithScopeRequest {
  scopeType?: ScopeTypeValue
  scopeId?: number | string
  expiresAt?: string
  reason?: string
}

// 角色分配项（用于批量设置）
export interface RoleAssignmentItem {
  roleId: number | string
  scopeType?: ScopeTypeValue
  scopeId?: number | string
  expiresAt?: string
  reason?: string
}

// 设置用户角色请求
export interface SetUserRolesRequest {
  assignments: RoleAssignmentItem[]
}

// 角色查询参数
export interface RoleQueryParams {
  roleType?: string
  enabled?: boolean
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 权限查询参数
export interface PermissionQueryParams {
  type?: PermissionType
  enabled?: boolean
  parentId?: number | string
  keyword?: string
}

// ==================== 动态数据模块配置（从 API 获取） ====================

/**
 * 数据模块 DTO（从 GET /api/data-modules 获取）
 */
export interface DataModuleDTO {
  id: number | string
  tenantId: number | string
  moduleCode: string
  moduleName: string
  domainCode: string
  domainName: string
  resourceType?: string
  orgUnitField: string
  creatorField: string
  sortOrder: number
  enabled: boolean
}

/**
 * 范围项类型 DTO（从 GET /api/data-modules/scope-item-types 获取）
 */
export interface ScopeItemTypeDTO {
  id: number | string
  tenantId: number | string
  itemTypeCode: string
  itemTypeName: string
  refTable: string
  refIdField: string
  refNameField: string
  refParentField?: string
  supportChildren: boolean
  sortOrder: number
}

/**
 * 数据范围选项
 */
export interface DataScopeOption {
  scopeCode: DataScope
  scopeName: string
  description: string
}

/**
 * 范围项（自定义范围中的具体项）
 */
export interface ScopeItem {
  itemTypeCode: string
  scopeId: number | string
  scopeName: string
  includeChildren: boolean
}

/**
 * 模块权限配置
 */
export interface ModulePermission {
  moduleCode: string
  scopeCode: string
  scopeItems?: ScopeItem[]
}

/**
 * 角色数据权限完整配置
 */
export interface RolePermissionConfig {
  roleId: string | number
  roleName: string
  modulePermissions: ModulePermission[]
}

// 数据范围显示配置（保留，这些是通用概念不是行业硬编码）
export const DataScopeConfig: Record<DataScope, { label: string; description: string }> = {
  ALL: { label: '全部数据', description: '可查看所有数据' },
  DEPARTMENT_AND_BELOW: { label: '组织及以下', description: '可查看本组织及下级组织数据' },
  DEPARTMENT: { label: '本组织', description: '仅可查看本组织数据' },
  CUSTOM: { label: '自定义', description: '根据配置查看指定范围数据' },
  SELF: { label: '仅自己', description: '仅可查看自己的数据' }
}

// 权限类型显示配置
export const PermissionTypeConfig: Record<PermissionType, { label: string; icon: string }> = {
  MENU: { label: '菜单', icon: 'Menu' },
  BUTTON: { label: '按钮', icon: 'Pointer' },
  API: { label: '接口', icon: 'Connection' }
}
