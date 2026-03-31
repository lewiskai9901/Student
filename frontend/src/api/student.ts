/**
 * 学生管理 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import { http } from '@/utils/request'
import type {
  Student,
  CreateStudentRequest,
  UpdateStudentRequest,
  StudentQueryParams,
  StudentSearchParams,
  AssignDormitoryRequest,
  ResetPasswordRequest,
  StudentStatusChange
} from '@/types/student'
import type { PageResponse } from '@/types'

// 后端API路径 - V2 DDD架构
const STUDENT_URL = '/students'

// ==================== 学生 CRUD ====================

/**
 * 分页查询学生
 */
export function getStudents(params?: StudentQueryParams): Promise<PageResponse<Student>> {
  return http.get<PageResponse<Student>>(STUDENT_URL, { params })
}

/**
 * 获取学生详情
 */
export function getStudent(id: number | string): Promise<Student> {
  return http.get<Student>(`${STUDENT_URL}/${id}`)
}

/**
 * 根据学号获取学生
 */
export function getStudentByNo(studentNo: string): Promise<Student> {
  return http.get<Student>(`${STUDENT_URL}/by-no/${studentNo}`)
}

/**
 * 创建学生
 */
export function createStudent(data: CreateStudentRequest): Promise<number | string> {
  return http.post<number>(STUDENT_URL, data)
}

/**
 * 更新学生
 */
export function updateStudent(id: number | string, data: UpdateStudentRequest): Promise<void> {
  return http.put(`${STUDENT_URL}/${id}`, data)
}

/**
 * 删除学生
 */
export function deleteStudent(id: number | string): Promise<void> {
  return http.delete(`${STUDENT_URL}/${id}`)
}

/**
 * 批量删除学生
 */
export function deleteStudents(ids: (number | string)[]): Promise<void> {
  return http.delete(`${STUDENT_URL}/batch`, { data: ids })
}

// ==================== 学生搜索与统计 ====================

/**
 * 快速搜索学生
 */
export function searchStudents(params: StudentSearchParams): Promise<Student[]> {
  return http.get<Student[]>(`${STUDENT_URL}/search`, { params })
}

/**
 * 检查学号是否存在
 */
export function existsStudentNo(studentNo: string, excludeId?: number | string): Promise<boolean> {
  return http.get<boolean>(`${STUDENT_URL}/exists`, { params: { studentNo, excludeId } })
}

/**
 * 统计班级学生数量
 */
export function countStudentsByClass(classId: number | string): Promise<number> {
  return http.get<number>(`${STUDENT_URL}/count/by-class`, { params: { classId } })
}

/**
 * 统计宿舍学生数量
 */
export function countStudentsByDormitory(dormitoryId: number | string): Promise<number> {
  return http.get<number>(`${STUDENT_URL}/count/by-dormitory`, { params: { dormitoryId } })
}

/**
 * 根据班级ID获取学生列表
 */
export function getStudentsByClass(classId: number | string): Promise<Student[]> {
  return http.get<Student[]>(`${STUDENT_URL}/by-class/${classId}`)
}

// ==================== 学生状态操作 ====================

/**
 * 更新学生状态
 */
export function updateStudentStatus(id: number | string, status: number): Promise<void> {
  return http.patch(`${STUDENT_URL}/${id}/status`, null, { params: { status } })
}

/**
 * 分配宿舍
 */
export function assignDormitory(id: number | string, data: AssignDormitoryRequest): Promise<void> {
  return http.patch(`${STUDENT_URL}/${id}/dormitory`, null, { params: data })
}

/**
 * 学生转班
 */
export function transferClass(id: number | string, newClassId: number | string): Promise<void> {
  return http.patch(`${STUDENT_URL}/${id}/transfer`, null, { params: { newClassId } })
}

/**
 * 重置密码
 */
export function resetPassword(id: number | string, data: ResetPasswordRequest): Promise<void> {
  return http.patch(`${STUDENT_URL}/${id}/reset-password`, data)
}

// ==================== 学籍异动记录 ====================

/**
 * 获取某学生的异动记录
 */
export function getStudentStatusChanges(studentId: number | string): Promise<StudentStatusChange[]> {
  return http.get<StudentStatusChange[]>(`${STUDENT_URL}/${studentId}/status-changes`)
}

/**
 * 管理员查看所有异动记录（分页）
 */
export function listStatusChanges(params?: {
  page?: number
  size?: number
  changeType?: string
}): Promise<PageResponse<StudentStatusChange>> {
  return http.get<PageResponse<StudentStatusChange>>(`${STUDENT_URL}/status-changes`, { params })
}

// ==================== 导入导出 ====================

/**
 * 导出学生数据
 */
export function exportStudents(params?: StudentQueryParams): Promise<Blob> {
  return http.get<Blob>(`${STUDENT_URL}/export`, { params, responseType: 'blob' })
}

/**
 * 导入学生数据
 */
export function importStudents(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<string>(`${STUDENT_URL}/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 下载导入模板
 */
export function downloadImportTemplate(): Promise<Blob> {
  return http.get<Blob>(`${STUDENT_URL}/template`, { responseType: 'blob' })
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 学生 API 对象
 */
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
  downloadTemplate: downloadImportTemplate,
  getStatusChanges: getStudentStatusChanges,
  listStatusChanges
}
