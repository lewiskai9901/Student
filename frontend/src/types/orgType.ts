/**
 * 组织类型 类型定义 (重构后匹配 org-unit-types API)
 */

export interface OrgType {
  id: number
  typeCode: string
  typeName: string
  parentTypeCode: string | null
  levelOrder: number
  icon: string | null
  color: string | null
  description: string | null
  isAcademic: boolean         // 是否教学单位 (vs 职能部门)
  canBeInspected: boolean     // 是否可被检查
  canHaveChildren: boolean    // 是否可有子级
  maxDepth: number | null     // 最大子级深度
  isSystem: boolean
  isEnabled: boolean
  sortOrder: number
}

export interface OrgTypeTreeNode extends OrgType {
  children: OrgTypeTreeNode[]
}

export interface CreateOrgTypeRequest {
  typeCode: string
  typeName: string
  parentTypeCode?: string
  levelOrder?: number
  icon?: string
  color?: string
  description?: string
  isAcademic?: boolean
  canBeInspected?: boolean
  canHaveChildren?: boolean
  maxDepth?: number
  sortOrder?: number
}

export interface UpdateOrgTypeRequest {
  typeName?: string
  icon?: string
  color?: string
  description?: string
  isAcademic?: boolean
  canBeInspected?: boolean
  canHaveChildren?: boolean
  maxDepth?: number
  sortOrder?: number
}

// 组织类型特性配置
export interface OrgTypeFeatures {
  isAcademic: boolean
  canBeInspected: boolean
  canHaveChildren: boolean
}

// 预置类型编码
export const PRESET_ORG_TYPES = {
  SCHOOL: 'SCHOOL',
  COLLEGE: 'COLLEGE',
  DEPARTMENT: 'DEPARTMENT',
  TEACHING_GROUP: 'TEACHING_GROUP',
  STUDENT_AFFAIRS: 'STUDENT_AFFAIRS',
  ACADEMIC_AFFAIRS: 'ACADEMIC_AFFAIRS',
  LOGISTICS: 'LOGISTICS',
  FINANCE: 'FINANCE',
  GENERAL_OFFICE: 'GENERAL_OFFICE',
  HR: 'HR'
} as const

// 类型图标映射
export const ORG_TYPE_ICONS: Record<string, string> = {
  SCHOOL: 'School',
  COLLEGE: 'Building',
  DEPARTMENT: 'Briefcase',
  TEACHING_GROUP: 'Users',
  STUDENT_AFFAIRS: 'UserCheck',
  ACADEMIC_AFFAIRS: 'BookOpen',
  LOGISTICS: 'Truck',
  FINANCE: 'DollarSign',
  GENERAL_OFFICE: 'FileText',
  HR: 'Users'
}

// 类型颜色映射
export const ORG_TYPE_COLORS: Record<string, string> = {
  SCHOOL: '#1890ff',
  COLLEGE: '#52c41a',
  DEPARTMENT: '#722ed1',
  TEACHING_GROUP: '#13c2c2',
  STUDENT_AFFAIRS: '#fa8c16',
  ACADEMIC_AFFAIRS: '#eb2f96',
  LOGISTICS: '#8c8c8c',
  FINANCE: '#faad14',
  GENERAL_OFFICE: '#595959',
  HR: '#2f54eb'
}
