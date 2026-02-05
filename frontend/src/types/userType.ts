/**
 * V6 用户类型 类型定义
 */

export interface UserType {
  id: number
  typeCode: string
  typeName: string
  parentTypeCode: string | null
  levelOrder: number
  icon: string | null
  color: string | null
  description: string | null
  canLogin: boolean
  canBeInspector: boolean
  canBeInspected: boolean
  canManageOrg: boolean
  canViewReports: boolean
  requiresClass: boolean
  requiresDormitory: boolean
  defaultRoleCodes: string | null
  isSystem: boolean
  isEnabled: boolean
  sortOrder: number
}

export interface UserTypeTreeNode extends UserType {
  children: UserTypeTreeNode[]
}

export interface CreateUserTypeRequest {
  typeCode: string
  typeName: string
  parentTypeCode?: string
  levelOrder?: number
  icon?: string
  color?: string
  description?: string
  canLogin?: boolean
  canBeInspector?: boolean
  canBeInspected?: boolean
  canManageOrg?: boolean
  canViewReports?: boolean
  requiresClass?: boolean
  requiresDormitory?: boolean
  defaultRoleCodes?: string
  sortOrder?: number
}

export interface UpdateUserTypeRequest {
  typeName?: string
  icon?: string
  color?: string
  description?: string
  canLogin?: boolean
  canBeInspector?: boolean
  canBeInspected?: boolean
  canManageOrg?: boolean
  canViewReports?: boolean
  requiresClass?: boolean
  requiresDormitory?: boolean
  defaultRoleCodes?: string
  sortOrder?: number
}

// 用户类型特性配置
export interface UserTypeFeatures {
  canLogin: boolean
  canBeInspector: boolean
  canBeInspected: boolean
  canManageOrg: boolean
  canViewReports: boolean
  requiresClass: boolean
  requiresDormitory: boolean
}

// 预置类型编码
export const PRESET_USER_TYPES = {
  // 顶级类型
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  STUDENT: 'STUDENT',
  EXTERNAL: 'EXTERNAL',
  // 管理员子类型
  SUPER_ADMIN: 'SUPER_ADMIN',
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  ORG_ADMIN: 'ORG_ADMIN',
  // 教职工子类型
  TEACHING_STAFF: 'TEACHING_STAFF',
  CLASS_TEACHER: 'CLASS_TEACHER',
  COUNSELOR: 'COUNSELOR',
  INSPECTOR: 'INSPECTOR',
  DORM_MANAGER: 'DORM_MANAGER',
  ADMIN_STAFF: 'ADMIN_STAFF',
  // 学生子类型
  UNDERGRADUATE: 'UNDERGRADUATE',
  GRADUATE: 'GRADUATE',
  EXCHANGE: 'EXCHANGE',
  // 外部人员子类型
  VISITOR: 'VISITOR',
  CONTRACTOR: 'CONTRACTOR'
} as const

// 类型图标映射
export const USER_TYPE_ICONS: Record<string, string> = {
  ADMIN: 'Setting',
  TEACHER: 'User',
  STUDENT: 'UserFilled',
  EXTERNAL: 'Avatar',
  SUPER_ADMIN: 'Setting',
  SYSTEM_ADMIN: 'Tools',
  ORG_ADMIN: 'OfficeBuilding',
  TEACHING_STAFF: 'Reading',
  CLASS_TEACHER: 'Stamp',
  COUNSELOR: 'Service',
  INSPECTOR: 'View',
  DORM_MANAGER: 'House',
  ADMIN_STAFF: 'Folder',
  UNDERGRADUATE: 'Notebook',
  GRADUATE: 'Document',
  EXCHANGE: 'Promotion',
  VISITOR: 'Visitor',
  CONTRACTOR: 'Suitcase'
}

// 类型颜色映射
export const USER_TYPE_COLORS: Record<string, string> = {
  ADMIN: '#f5222d',
  TEACHER: '#1890ff',
  STUDENT: '#52c41a',
  EXTERNAL: '#8c8c8c',
  SUPER_ADMIN: '#f5222d',
  SYSTEM_ADMIN: '#fa541c',
  ORG_ADMIN: '#fa8c16',
  TEACHING_STAFF: '#1890ff',
  CLASS_TEACHER: '#13c2c2',
  COUNSELOR: '#722ed1',
  INSPECTOR: '#eb2f96',
  DORM_MANAGER: '#faad14',
  ADMIN_STAFF: '#2f54eb',
  UNDERGRADUATE: '#52c41a',
  GRADUATE: '#52c41a',
  EXCHANGE: '#52c41a',
  VISITOR: '#8c8c8c',
  CONTRACTOR: '#8c8c8c'
}
