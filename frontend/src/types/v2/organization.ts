/**
 * 组织管理模块类型定义 - DDD架构适配
 */

// 组织单元类型
export type OrgUnitType = 'SCHOOL' | 'COLLEGE' | 'DEPARTMENT' | 'TEACHING_GROUP'

// 组织单元
export interface OrgUnit {
  id: number
  unitCode: string
  unitName: string
  unitType: OrgUnitType
  parentId: number | null
  treePath: string
  treeLevel: number
  leaderId: number | null
  leaderName?: string
  deputyLeaderIds: number[]
  sortOrder: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

// 组织单元树节点
export interface OrgUnitTreeNode extends OrgUnit {
  children?: OrgUnitTreeNode[]
  label?: string // 用于树形选择器
  value?: number // 用于树形选择器
}

// 创建组织单元请求
export interface CreateOrgUnitRequest {
  unitCode: string
  unitName: string
  unitType: OrgUnitType
  parentId?: number
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder?: number
}

// 更新组织单元请求
export interface UpdateOrgUnitRequest {
  unitName?: string
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder?: number
}

// 班级状态
export type ClassStatus = 'PREPARING' | 'ACTIVE' | 'GRADUATED' | 'DISSOLVED'

// 教师角色
export type TeacherRole = 'HEAD_TEACHER' | 'DEPUTY_HEAD_TEACHER' | 'SUBJECT_TEACHER' | 'COUNSELOR'

// 教师任职记录
export interface TeacherAssignment {
  teacherId: number
  teacherName: string
  role: TeacherRole
  startDate: string
  endDate?: string
  current: boolean
}

// 班级
export interface SchoolClass {
  id: number
  classCode: string
  className: string
  shortName?: string
  orgUnitId: number
  orgUnitName?: string
  enrollmentYear: number
  gradeLevel: number
  majorDirectionId?: number
  majorDirectionName?: string
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
  orgUnitId: number
  enrollmentYear: number
  gradeLevel?: number
  majorDirectionId?: number
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
  teacherId: number
  teacherName: string
}

// 班级查询参数
export interface ClassQueryParams {
  orgUnitId?: number
  enrollmentYear?: number
  status?: ClassStatus
  majorDirectionId?: number
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
