/**
 * 组织管理模块类型定义 - DDD架构适配
 */

// 组织单元类型 (now a string typeCode from org_unit_types table)
export type OrgUnitType = string

// 范围类型
export type ScopeType = 'all' | 'custom'

// 系统模块
export interface SystemModule {
  id: string
  moduleCode: string
  moduleName: string
  moduleDesc?: string
  parentCode?: string
  icon?: string
  sortOrder: number
  isEnabled: boolean
  children?: SystemModule[]
}

// 范围类型配置
export const ScopeTypeConfig: Record<ScopeType, { label: string }> = {
  all: { label: '全校范围' },
  custom: { label: '自定义范围' }
}

// 组织类型配置 (from org_unit_types table)
// NOTE: 推荐使用 @/types/orgType.ts 中的 OrgType，此接口保留兼容
export interface OrgUnitTypeConfig {
  id: number | string
  typeCode: string
  typeName: string
  category?: string | null
  parentTypeCode?: string | null
  icon?: string | null
  description?: string | null
  features?: Record<string, boolean> | null
  metadataSchema?: string | null
  allowedChildTypeCodes?: string[] | null
  maxDepth?: number | null
  defaultUserTypeCodes?: string[] | null
  defaultPlaceTypeCodes?: string[] | null
  defaultPositions?: Array<{ positionName: string; sortOrder: number }> | null
  system: boolean
  enabled: boolean
  sortOrder: number
  children?: OrgUnitTypeConfig[]
}

// 组织单元状态
export type OrgUnitStatusType = 'DRAFT' | 'ACTIVE' | 'FROZEN' | 'MERGING' | 'DISSOLVED'

// 组织单元状态显示配置
export const OrgUnitStatusConfig: Record<OrgUnitStatusType, { label: string; type: string; color: string }> = {
  DRAFT: { label: '草稿', type: 'info', color: '#909399' },
  ACTIVE: { label: '正常', type: 'success', color: '#67C23A' },
  FROZEN: { label: '冻结', type: 'warning', color: '#E6A23C' },
  MERGING: { label: '合并中', type: 'warning', color: '#F0A020' },
  DISSOLVED: { label: '已撤销', type: 'danger', color: '#F56C6C' }
}

// 组织单元
export interface OrgUnit {
  id: number | string
  unitCode: string
  unitName: string
  unitType: OrgUnitType        // typeCode string
  typeName?: string            // display name from org_unit_types
  typeIcon?: string            // icon from org_unit_types
  typeColor?: string           // color from org_unit_types
  parentId: number | string | null
  treePath: string
  treeLevel: number
  sortOrder: number
  status: OrgUnitStatusType
  statusLabel?: string
  headcount?: number
  attributes?: Record<string, unknown>
  mergedIntoId?: number | string
  dissolvedAt?: string
  dissolvedReason?: string
  createdAt: string
  updatedAt: string
}

// 组织单元树节点
export interface OrgUnitTreeNode extends OrgUnit {
  children?: OrgUnitTreeNode[]
  label?: string // 用于树形选择器
  value?: number | string // 用于树形选择器
  // Extension fields now stored in attributes JSON (via SPI plugins)
}

// 创建组织单元请求
export interface CreateOrgUnitRequest {
  unitCode: string
  unitName: string
  unitType: string             // typeCode from org_unit_types
  parentId?: number | string
  sortOrder?: number
  selectedPositions?: Array<{ positionName: string; headcount: number }>
}

// 更新组织单元请求
export interface UpdateOrgUnitRequest {
  unitName?: string
  sortOrder?: number
  headcount?: number
  attributes?: Record<string, unknown>
  reason?: string
}

// 班级状态
export type ClassStatus = 'PREPARING' | 'ACTIVE' | 'GRADUATED' | 'DISSOLVED'

// 教师角色
export type TeacherRole = 'HEAD_TEACHER' | 'DEPUTY_HEAD_TEACHER' | 'SUBJECT_TEACHER' | 'COUNSELOR'

// 教师任职记录
export interface TeacherAssignment {
  teacherId: number | string
  teacherName: string
  role: TeacherRole
  startDate: string
  endDate?: string
  current: boolean
}

// 班级
export interface SchoolClass {
  id: number | string
  classCode: string
  className: string
  shortName?: string
  orgUnitId: number | string
  orgUnitName?: string
  enrollmentYear: number
  gradeLevel: number
  gradeId?: number | string  // 年级ID，用于关联年级表
  majorDirectionId?: number | string
  majorName?: string  // 专业名称
  majorDirectionName?: string  // 专业方向名称（学制类型）
  schoolingYears: number
  standardSize: number
  currentSize: number
  status: ClassStatus
  teacherAssignments: TeacherAssignment[]
  sortOrder: number
  createdAt: string
  updatedAt: string
  // 便捷属性
  headTeacher?: TeacherAssignment
  deputyHeadTeachers?: TeacherAssignment[]
  expectedGraduationYear?: number
  availableSlots?: number
}

// 创建班级请求
export interface CreateClassRequest {
  classCode: string
  className: string
  shortName?: string
  orgUnitId: number | string
  enrollmentYear: number
  gradeLevel?: number
  majorDirectionId?: number | string
  schoolingYears?: number
  standardSize?: number
}

// 更新班级请求
export interface UpdateClassRequest {
  className?: string
  shortName?: string
  standardSize?: number
  sortOrder?: number
}

// 分配班主任请求
export interface AssignHeadTeacherRequest {
  teacherId: number | string
  teacherName: string
}

// 班级查询参数
export interface ClassQueryParams {
  orgUnitId?: number | string
  enrollmentYear?: number
  status?: ClassStatus
  majorDirectionId?: number | string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 班级状态显示配置
export const ClassStatusConfig: Record<ClassStatus, { label: string; type: string; color: string }> = {
  PREPARING: { label: '筹建中', type: 'info', color: '#909399' },
  ACTIVE: { label: '在读中', type: 'success', color: '#67C23A' },
  GRADUATED: { label: '已毕业', type: 'warning', color: '#E6A23C' },
  DISSOLVED: { label: '已撤销', type: 'danger', color: '#F56C6C' }
}

// 教师角色显示配置
export const TeacherRoleConfig: Record<TeacherRole, { label: string; color: string }> = {
  HEAD_TEACHER: { label: '班主任', color: '#409EFF' },
  DEPUTY_HEAD_TEACHER: { label: '副班主任', color: '#67C23A' },
  SUBJECT_TEACHER: { label: '任课教师', color: '#909399' },
  COUNSELOR: { label: '辅导员', color: '#E6A23C' }
}
