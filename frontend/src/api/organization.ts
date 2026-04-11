/**
 * 组织管理 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import { http } from '@/utils/request'
import type {
  OrgUnit,
  OrgUnitTreeNode,
  OrgUnitTypeConfig,
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

/** 一次性加载所有记录的默认分页大小 */
const LOAD_ALL_PAGE_SIZE = 1000

/** 一次性加载所有记录的大分页大小（用于班级等数量较多的场景） */
const LOAD_ALL_PAGE_SIZE_LARGE = 10000

// 后端API路径
const ORG_UNIT_URL = '/org-units'
const ORG_UNIT_TYPE_URL = '/org-types'
const CLASS_URL = '/students/classes'

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
export function getOrgUnit(id: number | string): Promise<OrgUnit> {
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
export function updateOrgUnit(id: number | string, data: UpdateOrgUnitRequest): Promise<OrgUnit> {
  return http.put<OrgUnit>(`${ORG_UNIT_URL}/${id}`, data)
}

/**
 * 删除组织单元
 */
export function deleteOrgUnit(id: number | string): Promise<void> {
  return http.delete(`${ORG_UNIT_URL}/${id}`)
}

/**
 * 冻结组织单元
 */
export function freezeOrgUnit(id: number | string, reason?: string): Promise<OrgUnit> {
  return http.put<OrgUnit>(`${ORG_UNIT_URL}/${id}/freeze`, reason ? { reason } : {})
}

/**
 * 解冻组织单元
 */
export function unfreezeOrgUnit(id: number | string): Promise<OrgUnit> {
  return http.put<OrgUnit>(`${ORG_UNIT_URL}/${id}/unfreeze`)
}

/**
 * 解散组织单元
 */
export function dissolveOrgUnit(id: number | string, reason: string): Promise<OrgUnit> {
  return http.put<OrgUnit>(`${ORG_UNIT_URL}/${id}/dissolve`, { reason })
}

/**
 * 合并组织单元（将 source 合并到 target）
 */
export function mergeOrgUnit(sourceId: number | string, targetId: number | string, reason?: string): Promise<OrgUnit> {
  return http.post<OrgUnit>(`${ORG_UNIT_URL}/${sourceId}/merge-into/${targetId}`, reason ? { reason } : {})
}

/**
 * 拆分组织单元
 */
export function splitOrgUnit(id: number | string, data: { reason: string; splits: Array<{ unitCode: string; unitName: string; childIds?: (number | string)[] }> }): Promise<OrgUnit[]> {
  return http.post<OrgUnit[]>(`${ORG_UNIT_URL}/${id}/split`, data)
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
export function getOrgUnitChildren(id: number | string): Promise<OrgUnit[]> {
  return http.get<OrgUnit[]>(`${ORG_UNIT_URL}/${id}/children`)
}

// 组织类型配置已迁移到 entity_type_configs，使用 entityTypeApi（@/api/entityType.ts）

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
export function getClass(id: number | string): Promise<SchoolClass> {
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
export function updateClass(id: number | string, data: UpdateClassRequest): Promise<SchoolClass> {
  return http.put<SchoolClass>(`${CLASS_URL}/${id}`, data)
}

/**
 * 删除班级
 */
export function deleteClass(id: number | string): Promise<void> {
  return http.delete(`${CLASS_URL}/${id}`)
}

/**
 * 激活班级
 */
export function activateClass(id: number | string): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/activate`)
}

/**
 * 班级毕业
 */
export function graduateClass(id: number | string): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/graduate`)
}

/**
 * 撤销班级
 */
export function dissolveClass(id: number | string): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/dissolve`)
}

/**
 * 分配班主任
 */
export function assignHeadTeacher(id: number | string, data: AssignHeadTeacherRequest): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/head-teacher`, data)
}

/**
 * 分配副班主任
 */
export function assignDeputyHeadTeacher(id: number | string, data: AssignHeadTeacherRequest): Promise<void> {
  return http.post(`${CLASS_URL}/${id}/deputy-head-teacher`, data)
}

/**
 * 结束教师任职
 */
export function endTeacherAssignment(orgUnitId: number | string, teacherId: number | string, role: string): Promise<void> {
  return http.post(`${CLASS_URL}/${orgUnitId}/teachers/${teacherId}/end`, null, { params: { role } })
}

/**
 * 获取组织单元下的班级
 * @param orgUnitId 支持 number 或 string 类型（大数字ID需要使用 string 避免精度丢失）
 */
export function getClassesByOrgUnit(orgUnitId: number | string): Promise<SchoolClass[]> {
  return http.get(`${CLASS_URL}`, {
    params: { orgUnitId, pageNum: 1, pageSize: LOAD_ALL_PAGE_SIZE }
  }).then((res: any) => res.records || [])
}

/**
 * 获取班主任管理的班级
 */
export function getClassesByHeadTeacher(teacherId: number | string): Promise<SchoolClass[]> {
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
export function batchDeleteClasses(ids: (number | string)[]): Promise<number> {
  return http.delete<number>(`${CLASS_URL}/batch`, { data: ids })
}

/**
 * 获取所有班级（不分页）
 */
export function getAllClasses(): Promise<SchoolClass[]> {
  return getClasses({ pageNum: 1, pageSize: LOAD_ALL_PAGE_SIZE_LARGE }).then(res => res.records)
}

/**
 * 获取班级列表（兼容V1接口名）
 */
export function getClassList(params?: { gradeId?: number | string; status?: ClassStatus }): Promise<SchoolClass[]> {
  return getClasses({ ...params, pageNum: 1, pageSize: LOAD_ALL_PAGE_SIZE_LARGE }).then(res => res.records)
}

/**
 * 导出班级列表
 * @description 前端导出，使用现有列表数据
 */
export async function exportClasses(params: ClassQueryParams): Promise<SchoolClass[]> {
  // 获取所有数据用于导出
  const result = await getClasses({ ...params, pageNum: 1, pageSize: LOAD_ALL_PAGE_SIZE_LARGE })
  return result.records
}

/**
 * 根据组织单元ID获取宿舍列表
 * @param orgUnitId 支持 number 或 string 类型（大数字ID需要使用 string 避免精度丢失）
 */
export function getDormitoriesByOrgUnit(orgUnitId: number | string): Promise<any[]> {
  return http.get('/dormitory/rooms', {
    params: { orgUnitId, pageNum: 1, pageSize: LOAD_ALL_PAGE_SIZE }
  }).then((res: any) => res.records || [])
}

/**
 * 获取所有教室列表
 */
export function getClassroomList(): Promise<any[]> {
  return http.get('/teaching/classrooms', {
    params: { pageNum: 1, pageSize: LOAD_ALL_PAGE_SIZE, status: 1 }
  }).then((res: any) => res.records || [])
}

/**
 * 获取所有宿舍列表
 */
export function getDormitoryList(): Promise<any[]> {
  return http.get('/dormitory/rooms', {
    params: { pageNum: 1, pageSize: LOAD_ALL_PAGE_SIZE, status: 1 }
  }).then((res: any) => res.records || [])
}

/**
 * 获取班级学生列表
 */
export function getClassStudents(orgUnitId: number | string): Promise<any[]> {
  return http.get(`/students/by-class/${orgUnitId}`)
}

/**
 * 获取教师列表
 */
export function getTeacherList(): Promise<any[]> {
  return http.get('/users', {
    params: { pageNum: 1, pageSize: LOAD_ALL_PAGE_SIZE }
  }).then((res: any) => res.records || [])
}

/**
 * 获取专业列表
 */
export function getMajorList(orgUnitId?: number | string): Promise<any[]> {
  return http.get('/majors', {
    params: orgUnitId ? { orgUnitId } : undefined
  })
}

/**
 * 设置班主任 (兼容V1)
 */
export function assignTeacher(orgUnitId: number | string, teacherId: number | string | null): Promise<void> {
  return http.post(`${CLASS_URL}/${orgUnitId}/head-teacher`, {
    teacherId
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
  freeze: freezeOrgUnit,
  unfreeze: unfreezeOrgUnit,
  dissolve: dissolveOrgUnit,
  merge: mergeOrgUnit,
  split: splitOrgUnit,
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
  getStudents: getClassStudents
}

// ==================== 部门管理 (兼容V1 department.ts) ====================

export interface DepartmentResponse {
  id: number
  unitCode: string
  unitName: string
  deptName?: string
  unitType: string
  category?: string
  typeName?: string
  typeIcon?: string
  typeColor?: string
  parentId: number | null
  headcount?: number
  sortOrder: number
  status: string
  statusLabel?: string
  mergedIntoId?: number
  dissolvedAt?: string
  dissolvedReason?: string
  createdAt: string
  updatedAt: string
  children?: DepartmentResponse[]
  attributes?: Record<string, any>
}

export interface DepartmentCreateRequest {
  unitCode: string
  unitName: string
  unitType?: string
  parentId?: number
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
export function updateDepartment(id: number | string, data: DepartmentCreateRequest): Promise<OrgUnit> {
  return http.put(`${ORG_UNIT_URL}/${id}`, {
    unitName: data.unitName,
    sortOrder: data.sortOrder,
  })
}

/**
 * 删除部门
 */
export function deleteDepartment(id: number | string): Promise<void> {
  return http.delete(`${ORG_UNIT_URL}/${id}`)
}

/**
 * 获取部门详情
 */
export function getDepartmentById(id: number | string): Promise<DepartmentResponse> {
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
export function getDepartmentsByParentId(parentId: number | string): Promise<DepartmentResponse[]> {
  return http.get<DepartmentResponse[]>(`${ORG_UNIT_URL}/${parentId}/children`)
}

/**
 * 冻结部门
 */
export function freezeDepartment(id: number | string, reason?: string): Promise<OrgUnit> {
  return freezeOrgUnit(id, reason)
}

/**
 * 解冻部门
 */
export function unfreezeDepartment(id: number | string): Promise<OrgUnit> {
  return unfreezeOrgUnit(id)
}

/**
 * 解散部门
 */
export function dissolveDepartment(id: number | string, reason: string): Promise<OrgUnit> {
  return dissolveOrgUnit(id, reason)
}

// ==================== 年级(Cohort)管理 (兼容V1 grade.ts) ====================

const COHORT_URL = '/students/cohorts'

export interface Cohort {
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

export interface CohortQuery {
  pageNum?: number
  pageSize?: number
  enrollmentYear?: number
  status?: string
  keyword?: string
}

export interface CohortCreateRequest {
  gradeName: string
  gradeCode: string
  enrollmentYear: number
  schoolingYears?: number
  standardClassSize?: number
}

export const listCohorts = (params?: CohortQuery): Promise<Cohort[]> => http.get<Cohort[]>(COHORT_URL, { params })

export const getCohortPage = (params: CohortQuery & { pageNum: number; pageSize: number }) => {
  return http.get<Cohort[]>(COHORT_URL, { params }).then(cohorts => {
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return { records: cohorts.slice(start, end), total: cohorts.length, current: params.pageNum, size: params.pageSize }
  })
}

export const getCohort = (id: number | string): Promise<Cohort> => http.get<Cohort>(`${COHORT_URL}/${id}`)
export const getActiveCohorts = (): Promise<Cohort[]> => http.get<Cohort[]>(`${COHORT_URL}/active`)
export const getCohortByYear = (enrollmentYear: number): Promise<Cohort> => http.get<Cohort>(`${COHORT_URL}/by-year/${enrollmentYear}`)
export const getCohortsByStatus = (status: string): Promise<Cohort[]> => http.get<Cohort[]>(`${COHORT_URL}/by-status/${status}`)
export const createCohort = (data: CohortCreateRequest): Promise<Cohort> => http.post<Cohort>(COHORT_URL, data)
export const updateCohort = (data: { id: number | string; gradeName?: string; standardClassSize?: number; sortOrder?: number; remarks?: string }): Promise<Cohort> =>
  http.put<Cohort>(`${COHORT_URL}/${data.id}`, data)
export const activateCohort = (id: number | string): Promise<Cohort> => http.put<Cohort>(`${COHORT_URL}/${id}/activate`)
export const graduateCohort = (id: number | string): Promise<Cohort> => http.put<Cohort>(`${COHORT_URL}/${id}/graduate`)
export const stopEnrollment = (id: number | string): Promise<Cohort> => http.put<Cohort>(`${COHORT_URL}/${id}/stop-enrollment`)
export const deleteCohort = (id: number | string): Promise<void> => http.delete(`${COHORT_URL}/${id}`)
export const getAllCohorts = (): Promise<Cohort[]> => http.get<Cohort[]>(COHORT_URL)
export const assignCohortLeaders = (id: number | string, data: { directorId?: number | string; counselorId?: number | string }): Promise<Cohort> =>
  http.put<Cohort>(`${COHORT_URL}/${id}/leaders`, data)

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

export const getDepartmentList = getOrgUnitTree
export const listDepartments = getDepartmentTree
export const existsDeptCode = (_code: string, _excludeId?: number | string) => Promise.resolve(false)

// ==================== 类型重导出 ====================
export type {
  OrgUnit,
  OrgUnitTreeNode,
  OrgUnitTypeConfig,
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
