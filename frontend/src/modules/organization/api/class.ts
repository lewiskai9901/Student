/**
 * 班级 API
 */
import { http } from '@/shared/utils/request'
import type { PageResponse } from '@/shared/types'
import type {
  SchoolClass,
  CreateClassRequest,
  UpdateClassRequest,
  AssignHeadTeacherRequest,
  ClassQueryParams,
  ClassDormitoryInfo,
  ClassStatus
} from '../types'

const BASE_URL = '/v2/organization/classes'
const ORG_UNIT_URL = '/v2/org-units'

// ==================== CRUD ====================

export function getClasses(params: ClassQueryParams): Promise<PageResponse<SchoolClass>> {
  return http.get<PageResponse<SchoolClass>>(BASE_URL, { params })
}

export function getClass(id: number): Promise<SchoolClass> {
  return http.get<SchoolClass>(`${BASE_URL}/${id}`)
}

export function getClassByCode(classCode: string): Promise<SchoolClass> {
  return http.get<SchoolClass>(`${BASE_URL}/code/${classCode}`)
}

export function createClass(data: CreateClassRequest): Promise<SchoolClass> {
  return http.post<SchoolClass>(BASE_URL, data)
}

export function updateClass(id: number, data: UpdateClassRequest): Promise<SchoolClass> {
  return http.put<SchoolClass>(`${BASE_URL}/${id}`, data)
}

export function deleteClass(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

export function batchDeleteClasses(ids: number[]): Promise<number> {
  return http.delete<number>(`${BASE_URL}/batch`, { data: ids })
}

// ==================== 状态操作 ====================

export function activateClass(id: number): Promise<void> {
  return http.post(`${BASE_URL}/${id}/activate`)
}

export function graduateClass(id: number): Promise<void> {
  return http.post(`${BASE_URL}/${id}/graduate`)
}

export function dissolveClass(id: number): Promise<void> {
  return http.post(`${BASE_URL}/${id}/dissolve`)
}

// ==================== 教师分配 ====================

export function assignHeadTeacher(id: number, data: AssignHeadTeacherRequest): Promise<void> {
  return http.post(`${BASE_URL}/${id}/head-teacher`, data)
}

export function assignDeputyHeadTeacher(id: number, data: AssignHeadTeacherRequest): Promise<void> {
  return http.post(`${BASE_URL}/${id}/deputy-head-teacher`, data)
}

export function endTeacherAssignment(classId: number, teacherId: number, role: string): Promise<void> {
  return http.post(`${BASE_URL}/${classId}/teachers/${teacherId}/end`, null, { params: { role } })
}

export function assignTeacher(classId: number, teacherId: number | null): Promise<void> {
  return http.post(`${BASE_URL}/${classId}/head-teacher`, { teacherId, teacherName: '' })
}

// ==================== 查询 ====================

export function getClassesByOrgUnit(orgUnitId: number): Promise<SchoolClass[]> {
  return http.get<SchoolClass[]>(`${ORG_UNIT_URL}/${orgUnitId}/classes`)
}

export function getClassesByHeadTeacher(teacherId: number): Promise<SchoolClass[]> {
  return http.get<SchoolClass[]>(`${BASE_URL}/head-teacher/${teacherId}`)
}

export function getGraduatingClasses(year: number): Promise<SchoolClass[]> {
  return http.get<SchoolClass[]>(`${BASE_URL}/graduating`, { params: { year } })
}

export function checkClassCodeExists(classCode: string): Promise<boolean> {
  return http.get<boolean>(`${BASE_URL}/check-code`, { params: { classCode } })
}

export function getAllClasses(): Promise<SchoolClass[]> {
  return getClasses({ pageNum: 1, pageSize: 10000 }).then(res => res.records)
}

export function getClassList(params?: { gradeId?: number; status?: ClassStatus }): Promise<SchoolClass[]> {
  return getClasses({ ...params, pageNum: 1, pageSize: 10000 }).then(res => res.records)
}

export async function exportClasses(params: ClassQueryParams): Promise<SchoolClass[]> {
  const result = await getClasses({ ...params, pageNum: 1, pageSize: 10000 })
  return result.records
}

// ==================== 教室/宿舍 ====================

export function getClassDetail(id: number): Promise<SchoolClass> {
  return http.get<SchoolClass>(`${BASE_URL}/${id}`)
}

export function assignClassroom(classId: number, classroomId: number): Promise<void> {
  return http.post(`/classes/${classId}/assign-classroom`, null, { params: { classroomId } })
}

export function removeClassroom(classId: number): Promise<void> {
  return http.delete(`/classes/${classId}/classroom`)
}

export function getClassClassroom(classId: number): Promise<any> {
  return http.get(`/classes/${classId}/classroom`)
}

export function addClassDormitory(classId: number, dormitoryId: number, allocatedBeds: number): Promise<void> {
  return http.post(`/classes/${classId}/dormitories`, null, { params: { dormitoryId, allocatedBeds } })
}

export function removeClassDormitory(classId: number, dormitoryId: number): Promise<void> {
  return http.delete(`/classes/${classId}/dormitories/${dormitoryId}`)
}

export function getClassDormitories(classId: number): Promise<ClassDormitoryInfo[]> {
  return http.get<ClassDormitoryInfo[]>(`/classes/${classId}/dormitories`)
}

export function getClassStudents(classId: number): Promise<any[]> {
  return http.get(`/v2/students/by-class/${classId}`)
}

// ==================== API 对象 ====================

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
  getDetail: getClassDetail,
  assignClassroom,
  removeClassroom,
  getClassroom: getClassClassroom,
  addDormitory: addClassDormitory,
  removeDormitory: removeClassDormitory,
  getDormitories: getClassDormitories,
  getStudents: getClassStudents
}
