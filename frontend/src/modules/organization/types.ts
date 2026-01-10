/**
 * 组织管理模块类型定义
 */

// ==================== 组织单元 ====================

export type OrgUnitType = 'SCHOOL' | 'COLLEGE' | 'DEPARTMENT' | 'TEACHING_GROUP'

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

export interface OrgUnitTreeNode extends OrgUnit {
  children?: OrgUnitTreeNode[]
  label?: string
  value?: number
}

export interface CreateOrgUnitRequest {
  unitCode: string
  unitName: string
  unitType: OrgUnitType
  parentId?: number
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder?: number
}

export interface UpdateOrgUnitRequest {
  unitName?: string
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder?: number
}

// ==================== 班级 ====================

export type ClassStatus = 'PREPARING' | 'ACTIVE' | 'GRADUATED' | 'DISSOLVED'
export type TeacherRole = 'HEAD_TEACHER' | 'DEPUTY_HEAD_TEACHER' | 'SUBJECT_TEACHER' | 'COUNSELOR'

export interface TeacherAssignment {
  teacherId: number
  teacherName: string
  role: TeacherRole
  startDate: string
  endDate?: string
  current: boolean
}

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
  headTeacher?: TeacherAssignment
  deputyHeadTeachers?: TeacherAssignment[]
  expectedGraduationYear?: number
  availableSlots?: number
}

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

export interface UpdateClassRequest {
  className?: string
  shortName?: string
  standardSize?: number
  sortOrder?: number
}

export interface AssignHeadTeacherRequest {
  teacherId: number
  teacherName: string
}

export interface ClassQueryParams {
  orgUnitId?: number
  enrollmentYear?: number
  status?: ClassStatus
  majorDirectionId?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
  gradeId?: number
}

export interface ClassDormitoryInfo {
  dormitoryId: number
  dormitoryName: string
  buildingName: string
  roomNo: string
  allocatedBeds: number
  usedBeds: number
}

// ==================== 年级 ====================

export interface Grade {
  id?: number
  gradeName: string
  gradeCode: string
  enrollmentYear: number
  schoolingYears?: number
  standardClassSize?: number
  sortOrder?: number
  remarks?: string
  status?: string
  directorId?: number
  directorName?: string
  counselorId?: number
  counselorName?: string
  classCount?: number
  studentCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface GradeQuery {
  pageNum?: number
  pageSize?: number
  enrollmentYear?: number
  status?: string
  keyword?: string
}

export interface GradeCreateRequest {
  gradeName: string
  gradeCode: string
  enrollmentYear: number
  schoolingYears?: number
  standardClassSize?: number
}

// ==================== 部门 (兼容) ====================

export interface DepartmentResponse {
  id: number
  unitCode: string
  unitName: string
  unitType: string
  parentId: number
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder: number
  isEnabled: boolean
  createdAt: string
  updatedAt: string
  children?: DepartmentResponse[]
}

export interface DepartmentCreateRequest {
  unitCode: string
  unitName: string
  unitType?: string
  parentId?: number
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder?: number
}

// ==================== 常量配置 ====================

export const ClassStatusConfig: Record<ClassStatus, { label: string; type: string; color: string }> = {
  PREPARING: { label: '筹建中', type: 'info', color: '#909399' },
  ACTIVE: { label: '在读中', type: 'success', color: '#67C23A' },
  GRADUATED: { label: '已毕业', type: 'warning', color: '#E6A23C' },
  DISSOLVED: { label: '已撤销', type: 'danger', color: '#F56C6C' }
}

export const TeacherRoleConfig: Record<TeacherRole, { label: string; color: string }> = {
  HEAD_TEACHER: { label: '班主任', color: '#409EFF' },
  DEPUTY_HEAD_TEACHER: { label: '副班主任', color: '#67C23A' },
  SUBJECT_TEACHER: { label: '任课教师', color: '#909399' },
  COUNSELOR: { label: '辅导员', color: '#E6A23C' }
}
