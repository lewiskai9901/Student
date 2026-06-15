import type { LongId } from '@/types/common'

/**
 * 权限管理模块类型定义 - V6 动态化重构
 * 删除所有硬编码枚举常量，改为从 API 动态获取
 */

// 权限类型
export type PermissionType = 'MENU' | 'BUTTON' | 'API'

// 权限作用域: PUBLIC=公共, SELF=个人空间, MANAGEMENT=管理后台
export type PermissionScope = 'PUBLIC' | 'SELF' | 'MANAGEMENT'

// 数据范围（5 种核心 + 插件可贡献）
export type DataScope = 'ALL' | 'DEPARTMENT_AND_BELOW' | 'DEPARTMENT' | 'CUSTOM' | 'SELF'

// 权限
export interface Permission {
  id: LongId
  permissionCode: string
  permissionName: string
  type: PermissionType
  scope: PermissionScope
  parentId: LongId | null
  path?: string
  icon?: string
  sortOrder: number
  enabled: boolean
  /** 插件级启用状态 (两状态模型): false = 所属插件被禁 (级联软失效), 前端应灰显 */
  pluginEnabled?: boolean
  isEnabled?: boolean
  origin?: string
  industry?: string
  description?: string
  children?: Permission[]
}

// 创建权限请求
export interface CreatePermissionRequest {
  permissionCode: string
  permissionName: string
  type: PermissionType
  scope?: PermissionScope
  parentId?: LongId | string
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
  id: LongId
  roleCode: string
  roleName: string
  roleType: string
  level: number
  description?: string
  enabled: boolean
  tenantId?: LongId | string
  permissionIds: (string | number)[]
  permissions?: Permission[]
  createdAt: string
  updatedAt: string
}

// 创建角色请求（roleCode 必填：业务 ID 稳定、可读，禁止自动生成）
export interface CreateRoleRequest {
  roleName: string
  roleCode: string
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
  /** 启用/禁用 (折叠进 PUT /roles/{id}, 与后端 UpdateRoleRequest.isEnabled 对应)。 */
  isEnabled?: boolean
}

// 设置权限请求
export interface SetPermissionsRequest {
  permissionIds: (number | string)[]
}

// 作用域类型
export type ScopeTypeValue = 'ALL' | 'ORG_UNIT'

// 用户角色（带作用域）
export interface UserRole {
  id: LongId | string
  userId: LongId | string
  roleId: LongId | string
  roleName: string
  roleCode: string
  scopeType: ScopeTypeValue
  scopeId: LongId | string
  scopeName?: string
  assignedAt: string
  assignedBy?: number | string
  expiresAt?: string
  isActive?: boolean
}

// 分配角色请求（带作用域）
export interface AssignRoleWithScopeRequest {
  scopeType?: ScopeTypeValue
  scopeId?: LongId | string
  expiresAt?: string
  reason?: string
}

// 角色分配项（用于批量设置）
export interface RoleAssignmentItem {
  roleId: LongId | string
  scopeType?: ScopeTypeValue
  scopeId?: LongId | string
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
  parentId?: LongId | string
  keyword?: string
}

// ==================== 动态数据模块配置（从 API 获取） ====================

/**
 * 数据模块 DTO（从 GET /api/data-modules 获取）
 */
export interface DataModuleDTO {
  id: LongId | string
  tenantId: LongId | string
  moduleCode: string
  moduleName: string
  domainCode: string
  domainName: string
  /** 所属行业 CORE / EDU / HEALTH / CARE / CUSTOM (插件架构一级分组) */
  industry?: string
  resourceType?: string
  orgUnitField: string
  creatorField: string
  sortOrder: number
  enabled: boolean
  /** 所属插件是否启用 — false 时前端灰显并提示启用插件 */
  pluginEnabled?: boolean
  /** 本模块支持的 scope 代码数组 — null/undefined 表示用默认全集 */
  allowedScopes?: string[] | null
}

/**
 * 范围项类型 DTO（从 GET /api/data-modules/scope-item-types 获取）
 */
export interface ScopeItemTypeDTO {
  id: LongId | string
  tenantId: LongId | string
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
  /** 来源标识: "CORE" = hardcoded 5 种; "PLUGIN:<domainCode>" = 插件贡献维度 */
  source?: string
}

/**
 * 范围项（自定义范围中的具体项）
 */
export interface ScopeItem {
  itemTypeCode: string
  scopeId: LongId | string
  scopeName: string
  includeChildren: boolean
}

/**
 * 组织锚点 (轴①) — 与后端 OrgAnchor 枚举严格往返
 *  - ALL: 全部组织
 *  - SELF: 仅本人 (不锚定组织)
 *  - PRIMARY_ORG: 用户主属组织 (member 关系所在 org)
 *  - RELATION: 由关系派生的组织集 (如 admin 关系=我管理的组织), anchorParam=relationCode
 *  - CUSTOM_ORG: 管理员显式指定的组织集 (customOrgIds)
 *  - PLUGIN_DIM: 插件动态维度派生, anchorParam=dimCode
 */
export type OrgAnchor = 'ALL' | 'SELF' | 'PRIMARY_ORG' | 'RELATION' | 'CUSTOM_ORG' | 'PLUGIN_DIM'

/**
 * 模块权限配置 — 可组合三轴 (T10 READ 侧)
 *  - 轴① 组织锚点: orgAnchor / anchorParam / includeSubtree / customOrgIds
 *  - 轴② 关系过滤: subjectRelInclude / subjectRelExclude (仅 relationFilterable 资源)
 *  - 轴③ 类型过滤: typeFilter (仅声明了 typeField/typeEntity 的资源)
 * scopeCode 仍保留 (preset 回退/向后兼容); 后端有三轴时优先用三轴。
 */
export interface ModulePermission {
  moduleCode: string
  scopeCode: string
  scopeItems?: ScopeItem[]
  /** 类型过滤(轴③/闸2/2b): 类型码集, 与组织范围 AND 组合; 空/缺省=不限。仅对声明了 typeField 的资源生效 */
  typeFilter?: string[]
  // ── 可组合三轴 (T10): 前端显式配置时下发, 后端优先采用 ──
  /** 轴① 组织锚点 */
  orgAnchor?: OrgAnchor
  /** 轴① 锚点参数: RELATION 时=关系码; PLUGIN_DIM 时=维度码 */
  anchorParam?: string
  /** 轴① 是否含锚定组织的下级 (子树) */
  includeSubtree?: boolean
  /** 轴① CUSTOM_ORG 时的组织单元 id 列表 */
  customOrgIds?: (number | string)[]
  /** 轴② 关系过滤-包含: 仅这些关系的主体 (关系码集) */
  subjectRelInclude?: string[]
  /** 轴② 关系过滤-排除: 排除这些关系的主体 (如"排除管理者") */
  subjectRelExclude?: string[]
}

/**
 * 角色数据权限完整配置
 */
export interface RolePermissionConfig {
  roleId: LongId
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
