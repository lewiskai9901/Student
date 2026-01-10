/**
 * 学生模块 API
 */
import { http } from '@/shared/utils/request'
import type {
  Student,
  CreateStudentRequest,
  UpdateStudentRequest,
  StudentQueryParams,
  StudentSearchParams,
  AssignDormitoryRequest,
  ResetPasswordRequest
} from './types'
import type { PageResponse } from '@/shared/types'

const BASE_URL = '/v2/students'

// ==================== 学生 CRUD ====================

/**
 * 分页查询学生
 */
export function getStudents(params?: StudentQueryParams): Promise<PageResponse<Student>> {
  return http.get<PageResponse<Student>>(BASE_URL, { params })
}

/**
 * 获取学生详情
 */
export function getStudent(id: number): Promise<Student> {
  return http.get<Student>(`${BASE_URL}/${id}`)
}

/**
 * 根据学号获取学生
 */
export function getStudentByNo(studentNo: string): Promise<Student> {
  return http.get<Student>(`${BASE_URL}/by-no/${studentNo}`)
}

/**
 * 创建学生
 */
export function createStudent(data: CreateStudentRequest): Promise<number> {
  return http.post<number>(BASE_URL, data)
}

/**
 * 更新学生
 */
export function updateStudent(id: number, data: UpdateStudentRequest): Promise<void> {
  return http.put(`${BASE_URL}/${id}`, data)
}

/**
 * 删除学生
 */
export function deleteStudent(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

/**
 * 批量删除学生
 */
export function deleteStudents(ids: number[]): Promise<void> {
  return http.delete(`${BASE_URL}/batch`, { data: ids })
}

// ==================== 学生搜索与统计 ====================

/**
 * 快速搜索学生
 */
export function searchStudents(params: StudentSearchParams): Promise<Student[]> {
  return http.get<Student[]>(`${BASE_URL}/search`, { params })
}

/**
 * 检查学号是否存在
 */
export function existsStudentNo(studentNo: string, excludeId?: number): Promise<boolean> {
  return http.get<boolean>(`${BASE_URL}/exists`, { params: { studentNo, excludeId } })
}

/**
 * 统计班级学生数量
 */
export function countStudentsByClass(classId: number): Promise<number> {
  return http.get<number>(`${BASE_URL}/count/by-class`, { params: { classId } })
}

/**
 * 统计宿舍学生数量
 */
export function countStudentsByDormitory(dormitoryId: number): Promise<number> {
  return http.get<number>(`${BASE_URL}/count/by-dormitory`, { params: { dormitoryId } })
}

/**
 * 根据班级ID获取学生列表
 */
export function getStudentsByClass(classId: number): Promise<Student[]> {
  return http.get<Student[]>(`${BASE_URL}/by-class/${classId}`)
}

// ==================== 学生状态操作 ====================

/**
 * 更新学生状态
 */
export function updateStudentStatus(id: number, status: number): Promise<void> {
  return http.patch(`${BASE_URL}/${id}/status`, null, { params: { status } })
}

/**
 * 分配宿舍
 */
export function assignDormitory(id: number, data: AssignDormitoryRequest): Promise<void> {
  return http.patch(`${BASE_URL}/${id}/dormitory`, null, { params: data })
}

/**
 * 学生转班
 */
export function transferClass(id: number, newClassId: number): Promise<void> {
  return http.patch(`${BASE_URL}/${id}/transfer`, null, { params: { newClassId } })
}

/**
 * 重置密码
 */
export function resetPassword(id: number, data: ResetPasswordRequest): Promise<void> {
  return http.patch(`${BASE_URL}/${id}/reset-password`, data)
}

// ==================== 导入导出 ====================

/**
 * 导出学生数据
 */
export function exportStudents(params?: StudentQueryParams): Promise<Blob> {
  return http.get<Blob>(`${BASE_URL}/export`, { params, responseType: 'blob' })
}

/**
 * 导入学生数据
 */
export function importStudents(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<string>(`${BASE_URL}/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 下载导入模板
 */
export function downloadImportTemplate(): Promise<Blob> {
  return http.get<Blob>(`${BASE_URL}/template`, { responseType: 'blob' })
}

// ==================== API 对象封装 ====================

export const studentApi = {
  getList: getStudents,
  getById: getStudent,
  getByNo: getStudentByNo,
  getByClass: getStudentsByClass,
  search: searchStudents,
  create: createStudent,
  update: updateStudent,
  delete: deleteStudent,
  deleteBatch: deleteStudents,
  exists: existsStudentNo,
  countByClass: countStudentsByClass,
  countByDormitory: countStudentsByDormitory,
  updateStatus: updateStudentStatus,
  assignDormitory,
  transferClass,
  resetPassword,
  export: exportStudents,
  import: importStudents,
  downloadTemplate: downloadImportTemplate
}
