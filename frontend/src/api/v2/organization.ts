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
  ClassStatus
} from '@/types/v2'
import type { PageResponse } from '@/types/v2'

// 后端API路径
const ORG_UNIT_URL = '/v2/org-units'
const CLASS_URL = '/v2/organization/classes'

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
 */
export function getClassesByOrgUnit(orgUnitId: number): Promise<SchoolClass[]> {
  return http.get<SchoolClass[]>(`${ORG_UNIT_URL}/${orgUnitId}/classes`)
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
  getById: getClass,
  getByCode: getClassByCode,
  create: createClass,
  update: updateClass,
  delete: deleteClass,
  activate: activateClass,
  graduate: graduateClass,
  dissolve: dissolveClass,
  assignHeadTeacher,
  assignDeputyHeadTeacher,
  endTeacherAssignment,
  getByOrgUnit: getClassesByOrgUnit,
  getByHeadTeacher: getClassesByHeadTeacher,
  getGraduating: getGraduatingClasses,
  checkCodeExists: checkClassCodeExists
}
