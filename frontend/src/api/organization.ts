/**
 * 组织管理 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import { http } from '@/utils/request'
import type {
  OrgUnit,
  OrgUnitTreeNode,
  CreateOrgUnitRequest,
  UpdateOrgUnitRequest,
  SchoolClass,
  CreateClassRequest,
  UpdateClassRequest,
  AssignHeadTeacherRequest,
  ClassQueryParams,
  ClassStatus,
  SystemModule,
  PageResponse
} from '@/types'

// 后端API路径
const ORG_UNIT_URL = '/org-units'
const CLASS_URL = '/organization/classes'

// ==================== 组织单元 API ====================

/**
 * 获取组织单元列表
 */
export function getOrgUnits(): Promise<OrgUnit[]> {
  return http.get<OrgUnit[]>(ORG_UNIT_URL)
}

/**
 * 获取组织单元树
 */
export function getOrgUnitTree(): Promise<OrgUnitTreeNode[]> {
  return http.get<OrgUnitTreeNode[]>(`${ORG_UNIT_URL}/tree`)
}

/**
 * 获取组织单元详情
 */
export function getOrgUnit(id: number): Promise<OrgUnit> {
  return http.get<OrgUnit>(`${ORG_UNIT_URL}/${id}`)
}

/**
 * 创建组织单元
 */
export function createOrgUnit(data: CreateOrgUnitRequest): Promise<OrgUnit> {
  return http.post<OrgUnit>(ORG_UNIT_URL, data)
}

/**
 * 更新组织单元
 */
export function updateOrgUnit(id: number, data: UpdateOrgUnitRequest): Promise<OrgUnit> {
  return http.put<OrgUnit>(`${ORG_UNIT_URL}/${id}`, data)
}

/**
 * 删除组织单元
 */
export function deleteOrgUnit(id: number): Promise<void> {
  return http.delete(`${ORG_UNIT_URL}/${id}`)
}

/**
 * 启用组织单元
 */
export function enableOrgUnit(id: number): Promise<void> {
  return http.put(`${ORG_UNIT_URL}/${id}/enable`)
}

/**
 * 禁用组织单元
 */
export function disableOrgUnit(id: number): Promise<void> {
  return http.put(`${ORG_UNIT_URL}/${id}/disable`)
}

/**
 * 分配负责人
 */
export function assignLeader(id: number, leaderId: number): Promise<void> {
  return http.put(`${ORG_UNIT_URL}/${id}/leader`, { leaderId })
}

/**
 * 按类型获取组织单元
 */
export function getOrgUnitsByType(type: string): Promise<OrgUnit[]> {
  return http.get<OrgUnit[]>(`${ORG_UNIT_URL}/by-type/${type}`)
}

/**
 * 获取子组织单元
 */
export function getOrgUnitChildren(id: number): Promise<OrgUnit[]> {
  return http.get<OrgUnit[]>(`${ORG_UNIT_URL}/${id}/children`)
}

// ==================== 班级 API ====================

/**
 * 获取班级列表（分页）
 */
export function getClasses(params: ClassQueryParams): Promise<PageResponse<SchoolClass>> {
  return http.get<PageResponse<SchoolClass>>(CLASS_URL, { params })
}

/**
 * 获取班级详情
 */
export function getClass(id: number): Promise<SchoolClass> {
  return http.get<SchoolClass>(`${CLASS_URL}/${id}`)
}

/**
 * 根据编码获取班级
 */
export function getClassByCode(classCode: string): Promise<SchoolClass> {
  return http.get<SchoolClass>(`${CLASS_URL}/code/${classCode}`)
}

/**
 * 创建班级
 */
export function createClass(data: CreateClassRequest): Promise<SchoolClass> {
  return http.post<SchoolClass>(CLASS_URL, data)
}

/**
 * 更新班级
 */
export function updateClass(id: number, data: UpdateClassRequest): Promise<SchoolClass> {
  return http.put<SchoolClass>(`${CLASS_URL}/${id}`, data)
}

/**
 * 删除班级
 */
export function deleteClass(id: number): Promise<void> {
  return http.delete(`${CLASS_URL}/${id}`)
}

/**
 * 激活班级
 */
export function activateClass(id: number): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/activate`)
}

/**
 * 班级毕业
 */
export function graduateClass(id: number): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/graduate`)
}

/**
 * 撤销班级
 */
export function dissolveClass(id: number): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/dissolve`)
}

/**
 * 分配班主任
 */
export function assignHeadTeacher(id: number, data: AssignHeadTeacherRequest): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/head-teacher`, data)
}

/**
 * 分配副班主任
 */
export function assignDeputyHeadTeacher(id: number, data: AssignHeadTeacherRequest): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/deputy-head-teacher`, data)
}

/**
 * 结束教师任职
 */
export function endTeacherAssignment(classId: number, teacherId: number, role: string): Promise<void> {
  return http.post(`${CLASS_URL}/${classId}/teachers/${teacherId}/end`, null, { params: { role } })
}

/**
 * 获取组织单元下的班级
 * @param orgUnitId 支持 number 或 string 类型（大数字ID需要使用 string 避免精度丢失）
 */
export function getClassesByOrgUnit(orgUnitId: number | string): Promise<SchoolClass[]> {
  return http.get(`${CLASS_URL}`, {
    params: { orgUnitId, pageNum: 1, pageSize: 1000 }
  }).then((res: any) => res.records || [])
}

/**
 * 获取班主任管理的班级
 */
export function getClassesByHeadTeacher(teacherId: number): Promise<SchoolClass[]> {
  return http.get<SchoolClass[]>(`${CLASS_URL}/head-teacher/${teacherId}`)
}

/**
 * 获取即将毕业的班级
 */
export function getGraduatingClasses(year: number): Promise<SchoolClass[]> {
  return http.get<SchoolClass[]>(`${CLASS_URL}/graduating`, { params: { year } })
}

/**
 * 检查班级编码是否存在
 */
export function checkClassCodeExists(classCode: string): Promise<boolean> {
  return http.get<boolean>(`${CLASS_URL}/check-code`, { params: { classCode } })
}

/**
 * 批量删除班级
 */
export function batchDeleteClasses(ids: number[]): Promise<number> {
  return http.delete<number>(`${CLASS_URL}/batch`, { data: ids })
}

/**
 * 获取所有班级（不分页）
 */
export function getAllClasses(): Promise<SchoolClass[]> {
  return getClasses({ pageNum: 1, pageSize: 10000 }).then(res => res.records)
}

/**
 * 获取班级列表（兼容V1接口名）
 */
export function getClassList(params?: { gradeId?: number; status?: ClassStatus }): Promise<SchoolClass[]> {
  return getClasses({ ...params, pageNum: 1, pageSize: 10000 }).then(res => res.records)
}

/**
 * 导出班级列表
 * @description 前端导出，使用现有列表数据
 */
export async function exportClasses(params: ClassQueryParams): Promise<SchoolClass[]> {
  // 获取所有数据用于导出
  const result = await getClasses({ ...params, pageNum: 1, pageSize: 10000 })
  return result.records
}

// ==================== 班级跨域操作 (教室/宿舍) ====================

/**
 * 班级宿舍信息
 */
export interface ClassDormitoryInfo {
  dormitoryId: number
  dormitoryName: string
  buildingName: string
  roomNo: string
  allocatedBeds: number
  usedBeds: number
}

/**
 * 获取班级详情
 */
export function getClassDetail(id: number): Promise<SchoolClass> {
  return http.get<SchoolClass>(`${CLASS_URL}/${id}`)
}

/**
 * 为班级分配教室
 */
export function assignClassroom(classId: number, classroomId: number): Promise<void> {
  return http.post(`/classes/${classId}/assign-classroom`, null, {
    params: { classroomId }
  })
}

/**
 * 取消班级教室分配
 */
export function removeClassroom(classId: number): Promise<void> {
  return http.delete(`/classes/${classId}/classroom`)
}

/**
 * 获取班级的教室信息
 */
export function getClassClassroom(classId: number): Promise<any> {
  return http.get(`/classes/${classId}/classroom`)
}

/**
 * 为班级添加宿舍
 * @param classId 支持 number 或 string 类型（大数字ID需要使用 string 避免精度丢失）
 */
export function addClassDormitory(classId: number | string, dormitoryId: number | string, allocatedBeds: number): Promise<void> {
  return http.post(`/classes/${classId}/dormitories`, null, {
    params: { dormitoryId, allocatedBeds }
  })
}

/**
 * 移除班级宿舍
 * @param classId 支持 number 或 string 类型（大数字ID需要使用 string 避免精度丢失）
 */
export function removeClassDormitory(classId: number | string, dormitoryId: number | string): Promise<void> {
  return http.delete(`/classes/${classId}/dormitories/${dormitoryId}`)
}

/**
 * 获取班级的宿舍列表
 */
export function getClassDormitories(classId: number | string): Promise<ClassDormitoryInfo[]> {
  return http.get<ClassDormitoryInfo[]>(`/classes/${classId}/dormitories`)
}

/**
 * 根据组织单元ID获取宿舍列表
 * @param orgUnitId 支持 number 或 string 类型（大数字ID需要使用 string 避免精度丢失）
 */
export function getDormitoriesByOrgUnit(orgUnitId: number | string): Promise<any[]> {
  return http.get('/dormitory/rooms', {
    params: { orgUnitId, pageNum: 1, pageSize: 1000 }
  }).then((res: any) => res.records || [])
}

/**
 * 获取所有教室列表
 */
export function getClassroomList(): Promise<any[]> {
  return http.get('/teaching/classrooms', {
    params: { pageNum: 1, pageSize: 1000, status: 1 }
  }).then((res: any) => res.records || [])
}

/**
 * 获取所有宿舍列表
 */
export function getDormitoryList(): Promise<any[]> {
  return http.get('/dormitory/rooms', {
    params: { pageNum: 1, pageSize: 1000, status: 1 }
  }).then((res: any) => res.records || [])
}

/**
 * 获取班级学生列表
 */
export function getClassStudents(classId: number): Promise<any[]> {
  return http.get(`/students/by-class/${classId}`)
}

/**
 * 获取教师列表
 */
export function getTeacherList(): Promise<any[]> {
  return http.get('/users', {
    params: { pageNum: 1, pageSize: 1000 }
  }).then((res: any) => res.records || [])
}

/**
 * 获取专业列表
 */
export function getMajorList(orgUnitId?: number): Promise<any[]> {
  return http.get('/majors', {
    params: orgUnitId ? { orgUnitId } : undefined
  })
}

/**
 * 设置班主任 (兼容V1)
 */
export function assignTeacher(classId: number, teacherId: number | null): Promise<void> {
  return http.post(`${CLASS_URL}/${classId}/head-teacher`, {
    teacherId,
    teacherName: ''
  })
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 组织单元 API 对象
 */
export const orgUnitApi = {
  getList: getOrgUnits,
  getTree: getOrgUnitTree,
  getById: getOrgUnit,
  getByType: getOrgUnitsByType,
  getChildren: getOrgUnitChildren,
  create: createOrgUnit,
  update: updateOrgUnit,
  delete: deleteOrgUnit,
  enable: enableOrgUnit,
  disable: disableOrgUnit,
  assignLeader
}

/**
 * 班级 API 对象
 */
export const schoolClassApi = {
  getList: getClasses,
  getAll: getAllClasses,
  getClassList,
  getById: getClass,
  getByCode: getClassByCode,
  create: createClass,
  update: updateClass,
  delete: deleteClass,
  batchDelete: batchDeleteClasses,
  export: exportClasses,
  activate: activateClass,
  graduate: graduateClass,
  dissolve: dissolveClass,
  assignHeadTeacher,
  assignDeputyHeadTeacher,
  endTeacherAssignment,
  getByOrgUnit: getClassesByOrgUnit,
  getByHeadTeacher: getClassesByHeadTeacher,
  getGraduating: getGraduatingClasses,
  checkCodeExists: checkClassCodeExists,
  // 跨域操作
  getDetail: getClassDetail,
  assignClassroom,
  removeClassroom,
  getClassroom: getClassClassroom,
  addDormitory: addClassDormitory,
  removeDormitory: removeClassDormitory,
  getDormitories: getClassDormitories,
  getStudents: getClassStudents
}

// ==================== 部门管理 (兼容V1 department.ts) ====================

export interface DepartmentResponse {
  id: number
  unitCode: string
  unitName: string
  deptName?: string  // 别名，兼容旧代码
  unitType: string
  unitCategory?: string  // ACADEMIC | FUNCTIONAL | ADMINISTRATIVE
  parentId: number | null
  leaderId?: number
  leaderName?: string
  deputyLeaderIds?: number[]
  phone?: string
  email?: string
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
  unitCategory?: string  // ACADEMIC | FUNCTIONAL | ADMINISTRATIVE
  parentId?: number
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder?: number
}

/**
 * 创建部门
 */
export function createDepartment(data: DepartmentCreateRequest): Promise<OrgUnit> {
  return http.post(ORG_UNIT_URL, {
    unitCode: data.unitCode,
    unitName: data.unitName,
    unitType: data.unitType || 'DEPARTMENT',
    parentId: data.parentId,
    sortOrder: data.sortOrder
  })
}

/**
 * 更新部门
 */
export function updateDepartment(id: number, data: DepartmentCreateRequest): Promise<OrgUnit> {
  return http.put(`${ORG_UNIT_URL}/${id}`, {
    unitName: data.unitName,
    leaderId: data.leaderId,
    deputyLeaderIds: data.deputyLeaderIds,
    sortOrder: data.sortOrder
  })
}

/**
 * 删除部门
 */
export function deleteDepartment(id: number): Promise<void> {
  return http.delete(`${ORG_UNIT_URL}/${id}`)
}

/**
 * 获取部门详情
 */
export function getDepartmentById(id: number): Promise<DepartmentResponse> {
  return http.get<DepartmentResponse>(`${ORG_UNIT_URL}/${id}`)
}

/**
 * 获取部门树形结构
 */
export function getDepartmentTree(): Promise<DepartmentResponse[]> {
  return http.get<DepartmentResponse[]>(`${ORG_UNIT_URL}/tree`)
}

/**
 * 获取所有启用的部门
 */
export function getAllEnabledDepartments(): Promise<DepartmentResponse[]> {
  return http.get<DepartmentResponse[]>(`${ORG_UNIT_URL}/tree`)
}

/**
 * 根据父部门ID获取子部门
 */
export function getDepartmentsByParentId(parentId: number): Promise<DepartmentResponse[]> {
  return http.get<DepartmentResponse[]>(`${ORG_UNIT_URL}/${parentId}/children`)
}

/**
 * 启用部门
 */
export function enableDepartment(id: number): Promise<void> {
  return http.put(`${ORG_UNIT_URL}/${id}/enable`)
}

/**
 * 禁用部门
 */
export function disableDepartment(id: number): Promise<void> {
  return http.put(`${ORG_UNIT_URL}/${id}/disable`)
}

/**
 * 更新部门状态 (兼容V1)
 */
export function updateDepartmentStatus(id: number, status: number): Promise<void> {
  return status === 1 ? enableDepartment(id) : disableDepartment(id)
}

// ==================== 年级管理 (兼容V1 grade.ts) ====================

const GRADE_URL = '/grades'

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

export const listGrades = (params?: GradeQuery): Promise<Grade[]> => http.get<Grade[]>(GRADE_URL, { params })

export const getGradePage = (params: GradeQuery & { pageNum: number; pageSize: number }) => {
  return http.get<Grade[]>(GRADE_URL).then(grades => {
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return { records: grades.slice(start, end), total: grades.length, current: params.pageNum, size: params.pageSize }
  })
}

export const getGrade = (id: number): Promise<Grade> => http.get<Grade>(`${GRADE_URL}/${id}`)
export const getActiveGrades = (): Promise<Grade[]> => http.get<Grade[]>(`${GRADE_URL}/active`)
export const getGradeByYear = (enrollmentYear: number): Promise<Grade> => http.get<Grade>(`${GRADE_URL}/by-year/${enrollmentYear}`)
export const getGradesByStatus = (status: string): Promise<Grade[]> => http.get<Grade[]>(`${GRADE_URL}/by-status/${status}`)
export const createGrade = (data: GradeCreateRequest): Promise<Grade> => http.post<Grade>(GRADE_URL, data)
export const updateGrade = (data: { id: number; gradeName?: string; standardClassSize?: number; sortOrder?: number; remarks?: string }): Promise<Grade> =>
  http.put<Grade>(`${GRADE_URL}/${data.id}`, data)
export const activateGrade = (id: number): Promise<Grade> => http.put<Grade>(`${GRADE_URL}/${id}/activate`)
export const graduateGrade = (id: number): Promise<Grade> => http.put<Grade>(`${GRADE_URL}/${id}/graduate`)
export const stopEnrollment = (id: number): Promise<Grade> => http.put<Grade>(`${GRADE_URL}/${id}/stop-enrollment`)
export const deleteGrade = (id: number): Promise<void> => http.delete(`${GRADE_URL}/${id}`)
export const getAllGrades = (): Promise<Grade[]> => http.get<Grade[]>(GRADE_URL)
export const assignGradeLeaders = (id: number, data: { directorId?: number; counselorId?: number }): Promise<Grade> =>
  http.put<Grade>(`${GRADE_URL}/${id}/leaders`, data)

// ==================== 系统模块 API ====================

const SYSTEM_MODULE_URL = '/system-modules'

/**
 * 获取系统模块树
 */
export function getSystemModuleTree(): Promise<SystemModule[]> {
  return http.get<SystemModule[]>(`${SYSTEM_MODULE_URL}/tree`)
}

/**
 * 获取所有系统模块（平铺）
 */
export function getAllSystemModules(): Promise<SystemModule[]> {
  return http.get<SystemModule[]>(SYSTEM_MODULE_URL)
}

/**
 * 获取顶级系统模块
 */
export function getTopLevelModules(): Promise<SystemModule[]> {
  return http.get<SystemModule[]>(`${SYSTEM_MODULE_URL}/top-level`)
}

/**
 * 获取子模块
 */
export function getChildModules(parentCode: string): Promise<SystemModule[]> {
  return http.get<SystemModule[]>(`${SYSTEM_MODULE_URL}/${parentCode}/children`)
}

/**
 * 系统模块 API 对象
 */
export const systemModuleApi = {
  getTree: getSystemModuleTree,
  getAll: getAllSystemModules,
  getTopLevel: getTopLevelModules,
  getChildren: getChildModules
}

// ==================== V1 兼容别名 ====================
// 保持向后兼容，逐步迁移后删除

export const addDormitory = addClassDormitory
export const removeDormitory = removeClassDormitory
export const getDepartmentList = getOrgUnitTree
export const listDepartments = getDepartmentTree
export const existsDeptCode = (_code: string, _excludeId?: number) => Promise.resolve(false)

// ==================== 类型重导出 ====================
export type {
  OrgUnit,
  OrgUnitTreeNode,
  CreateOrgUnitRequest,
  UpdateOrgUnitRequest,
  SchoolClass,
  CreateClassRequest,
  UpdateClassRequest,
  AssignHeadTeacherRequest,
  ClassQueryParams,
  ClassStatus,
  SystemModule,
  PageResponse
} from '@/types'
