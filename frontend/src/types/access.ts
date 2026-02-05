/**
 * 权限管理模块类型定义 - DDD架构适配
 */

// 权限类型
export type PermissionType = 'MENU' | 'BUTTON' | 'API' | 'DATA'

// 权限
// 注意: id 和 parentId 为 string | number 类型，因为 Snowflake ID 超过 JavaScript 的 MAX_SAFE_INTEGER
export interface Permission {
  id: string | number
  permissionCode: string
  permissionName: string
  type: PermissionType
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
// 注意: id 为 string | number 类型，因为 Snowflake ID 超过 JavaScript 的 MAX_SAFE_INTEGER
export interface Role {
  id: string | number
  roleCode: string
  roleName: string
  roleType: RoleType
  dataScope: DataScope
  level: number
  description?: string
  enabled: boolean
  permissionIds: (string | number)[]
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
  DEPT_ADMIN: { label: '组织管理员', level: 2, color: '#409EFF' },
  GRADE_DIRECTOR: { label: '年级主任', level: 3, color: '#67C23A' },
  CLASS_TEACHER: { label: '班主任', level: 4, color: '#909399' },
  INSPECTOR: { label: '检查员', level: 5, color: '#909399' },
  USER: { label: '普通用户', level: 6, color: '#909399' },
  CUSTOM: { label: '自定义角色', level: 99, color: '#909399' }
}

// 数据范围显示配置
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
  API: { label: '接口', icon: 'Connection' },
  DATA: { label: '数据', icon: 'Document' }
}

// ==================== V2 数据权限类型 ====================

/**
 * 数据模块信息
 */
export interface DataModuleInfo {
  moduleCode: string
  moduleName: string
  domainCode: string
  domainName?: string
}

/**
 * 数据范围选项
 */
export interface DataScopeOption {
  scopeCode: string
  scopeName: string
  sortOrder?: number
}

/**
 * 范围项类型（用于CUSTOM范围配置）
 */
export interface ScopeItemType {
  itemTypeCode: string
  itemTypeName: string
  supportChildren: boolean
}

/**
 * 自定义范围项
 */
export interface ScopeItem {
  itemTypeCode: string      // ORG_UNIT, CLASS, GRADE, BUILDING
  scopeId: number           // 对应实体的ID
  scopeName: string         // 对应实体的名称（用于显示）
  includeChildren: boolean  // 是否包含下级（仅组织支持）
}

/**
 * 模块权限配置
 */
export interface ModulePermission {
  moduleCode: string
  scopeCode: string
  /** 自定义范围项列表（当scopeCode为CUSTOM时使用） */
  scopeItems?: ScopeItem[]
  /** 自定义范围（多维度：组织单元、年级、班级） */
  customScope?: CustomScope
  /** @deprecated 使用 customScope.orgUnitIds 替代 */
  customOrgUnitIds?: number[]
}

/**
 * 自定义范围（多维度）
 */
export interface CustomScope {
  /** 选中的组织单元ID列表 */
  orgUnitIds?: number[]
  /** 选中的年级ID列表 */
  gradeIds?: number[]
  /** 选中的班级ID列表 */
  classIds?: number[]
}

/**
 * 角色数据权限完整配置
 */
export interface RolePermissionConfig {
  roleId: string | number
  roleName: string
  modulePermissions: ModulePermission[]
}

/**
 * 领域及其模块
 */
export interface DomainWithModules {
  domainCode: string
  domainName: string
  modules: DataModuleInfo[]
}

/**
 * 按领域分组的模块列表（数组形式）
 */
export type GroupedModules = DomainWithModules[]

/**
 * 领域显示配置
 */
export const DomainConfig: Record<string, { label: string; icon: string }> = {
  organization: { label: '组织管理', icon: 'OfficeBuilding' },
  space: { label: '场地管理', icon: 'Place' },
  inspection: { label: '量化检查', icon: 'DocumentChecked' },
  access: { label: '权限管理', icon: 'Lock' }
}

// ==================== V5 数据权限类型 ====================

/**
 * V5 领域模块分组
 */
export interface DomainModulesV5 {
  domainCode: string
  domainName: string
  modules: { code: string; name: string }[]
}

/**
 * V5 数据范围类型
 */
export interface ScopeTypeV5 {
  code: string
  name: string
  description: string
  level: number
  calcType: 'NONE' | 'USER_ORG' | 'USER_ORG_TREE' | 'CUSTOM_CONFIG' | 'CREATOR'
}

/**
 * V5 范围项类型
 */
export interface ScopeItemTypeV5 {
  code: string
  name: string
  supportChildren: boolean
  referenceTable: string
  applicableModules: string[]
}

/**
 * V5 范围项
 */
export interface ScopeItemV5 {
  scopeId: number
  scopeName: string
  itemTypeCode: string
  includeChildren?: boolean
}

/**
 * V5 角色模块权限配置
 */
export interface RoleModulePermissionV5 {
  moduleCode: string
  moduleName: string
  domainCode: string
  scopeCode: string
  scopeItems: ScopeItemV5[]
}

/**
 * V5 保存权限命令
 */
export interface SavePermissionCommandV5 {
  moduleCode: string
  scopeCode: string
  scopeItems?: ScopeItemV5[]
}

/**
 * V5 数据模块定义
 */
export interface DataModuleV5 {
  code: string
  name: string
  domain: string
  description: string
}

/**
 * V5 合并后的数据范围（多角色融合结果）
 */
export interface MergedDataScopeV5 {
  moduleCode: string
  effectiveScope: DataScope
  scopeItems: ScopeItemV5[]
  isAllAccess: boolean
}

/**
 * V5 数据范围配置显示
 */
export const DataScopeV5Config: Record<DataScope, { label: string; description: string; level: number; color: string }> = {
  ALL: { label: '全部数据', description: '可访问系统中所有数据', level: 100, color: '#F56C6C' },
  DEPARTMENT_AND_BELOW: { label: '本组织及下级', description: '可访问本组织及其所有下级组织的数据', level: 80, color: '#E6A23C' },
  DEPARTMENT: { label: '仅本组织', description: '仅可访问本组织的数据', level: 60, color: '#409EFF' },
  CUSTOM: { label: '自定义范围', description: '根据配置的具体组织/班级等范围访问数据', level: 40, color: '#67C23A' },
  SELF: { label: '仅本人', description: '仅可访问自己创建或负责的数据', level: 20, color: '#909399' }
}

/**
 * V5 范围项类型配置
 */
export const ScopeItemTypeV5Config: Record<string, { label: string; icon: string }> = {
  ORG_UNIT: { label: '组织单元', icon: 'OfficeBuilding' },
  CLASS: { label: '班级', icon: 'Collection' },
  GRADE: { label: '年级', icon: 'Memo' },
  BUILDING: { label: '楼栋', icon: 'House' },
  MAJOR: { label: '专业', icon: 'Reading' }
}
