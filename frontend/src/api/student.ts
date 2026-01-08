import { http } from '@/utils/request'
import type { Student, StudentQueryRequest, StudentCreateRequest, StudentUpdateRequest } from '@/types/student'
import type { PageResult } from '@/types/common'

// V2 API 基础路径
const BASE_URL = '/v2/students'

// 分页查询学生
export function getStudentPage(params: StudentQueryRequest): Promise<PageResult<Student>> {
  return http.get<PageResult<Student>>(BASE_URL, { params })
}

// 根据ID获取学生详情
export function getStudentById(id: number): Promise<Student> {
  return http.get<Student>(`${BASE_URL}/${id}`)
}

// 根据学号获取学生
export function getStudentByNo(studentNo: string): Promise<Student> {
  return http.get<Student>(`${BASE_URL}/by-no/${studentNo}`)
}

// 创建学生
export function createStudent(data: StudentCreateRequest): Promise<number> {
  return http.post<number>(BASE_URL, data)
}

// 更新学生信息
export function updateStudent(id: number, data: StudentUpdateRequest): Promise<void> {
  return http.put(`${BASE_URL}/${id}`, data)
}

// 删除学生
export function deleteStudent(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

// 批量删除学生
export function deleteStudents(ids: number[]): Promise<void> {
  return http.delete(`${BASE_URL}/batch`, { data: ids })
}

// 检查学号是否存在
export function checkStudentNoExists(studentNo: string, excludeId?: number): Promise<boolean> {
  return http.get<boolean>(`${BASE_URL}/exists`, { params: { studentNo, excludeId } })
}

// 更新学生状态
export function updateStudentStatus(id: number, status: number): Promise<void> {
  return http.patch(`${BASE_URL}/${id}/status`, null, { params: { status } })
}

// 分配宿舍
export function assignDormitory(id: number, dormitoryId: number, bedNumber?: string): Promise<void> {
  return http.patch(`${BASE_URL}/${id}/dormitory`, null, { params: { dormitoryId, bedNumber } })
}

// 学生转班
export function transferClass(id: number, newClassId: number): Promise<void> {
  return http.patch(`${BASE_URL}/${id}/transfer`, null, { params: { newClassId } })
}

// 重置密码
export function resetPassword(id: number, newPassword: string): Promise<void> {
  return http.patch(`${BASE_URL}/${id}/reset-password`, { newPassword })
}

// 统计班级学生数量
export function countStudentsByClass(classId: number): Promise<number> {
  return http.get<number>(`${BASE_URL}/count/by-class`, { params: { classId } })
}

// 统计宿舍学生数量
export function countStudentsByDormitory(dormitoryId: number): Promise<number> {
  return http.get<number>(`${BASE_URL}/count/by-dormitory`, { params: { dormitoryId } })
}

// 导出学生数据
export function exportStudents(params: StudentQueryRequest): Promise<Blob> {
  return http.get<Blob>(`${BASE_URL}/export`, { params, responseType: 'blob' })
}

// 快速搜索学生(用于选择器)
export function searchStudents(params: { keyword: string; classId?: number; limit?: number }): Promise<Student[]> {
  return http.get<Student[]>(`${BASE_URL}/search`, { params })
}

// 下载导入模板
export function downloadImportTemplate(): Promise<Blob> {
  return http.get<Blob>(`${BASE_URL}/template`, { responseType: 'blob' })
}

// 导入学生数据
export function importStudents(file: File): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`${BASE_URL}/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 预览导入数据（解析Excel但不保存）
export function previewImportData(file: File): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`${BASE_URL}/import/preview`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 确认导入数据
export function confirmImport(data: any[]): Promise<any> {
  return http.post(`${BASE_URL}/import/confirm`, data)
}

// 导出导入失败的数据
export function exportFailedData(data: any[]): Promise<Blob> {
  return http.post<Blob>(`${BASE_URL}/import/export-failed`, data, { responseType: 'blob' })
}
