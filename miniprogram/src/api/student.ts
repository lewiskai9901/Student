/**
 * 学生管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 学生状态
export enum StudentStatus {
  ACTIVE = 1,      // 在籍
  GRADUATED = 2,   // 已毕业
  SUSPENDED = 3,   // 休学
  DROPPED = 4      // 退学
}

// 学生信息
export interface Student {
  id: number | string
  studentNo: string
  userId?: number
  realName: string
  gender?: number
  birthDate?: string
  identityCard?: string
  phone?: string
  email?: string
  avatar?: string
  classId?: number
  className?: string
  gradeId?: number
  gradeName?: string
  majorId?: number
  majorName?: string
  departmentId?: number
  departmentName?: string
  dormitoryId?: number
  dormitoryName?: string
  bedNumber?: string
  enrollmentDate?: string
  graduationDate?: string
  status: number
  statusText?: string
  createdAt?: string
  updatedAt?: string
}

// 查询参数
export interface StudentQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  studentNo?: string
  realName?: string
  classId?: number
  gradeId?: number
  majorId?: number
  departmentId?: number
  status?: number
}

// 创建/更新学生请求
export interface StudentFormData {
  studentNo: string
  realName: string
  gender?: number
  birthDate?: string
  identityCard?: string
  phone?: string
  email?: string
  classId?: number
  dormitoryId?: number
  bedNumber?: string
  enrollmentDate?: string
  status?: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 分页查询学生
 */
export function getStudentPage(params: StudentQueryParams = {}) {
  return get<PageResult<Student>>('/students', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 根据ID获取学生详情
 */
export function getStudentById(id: number | string) {
  return get<Student>(`/students/${id}`)
}

/**
 * 根据学号获取学生
 */
export function getStudentByNo(studentNo: string) {
  return get<Student>(`/students/by-no/${studentNo}`)
}

/**
 * 创建学生
 */
export function createStudent(data: StudentFormData) {
  return post<number>('/students', data)
}

/**
 * 更新学生信息
 */
export function updateStudent(id: number | string, data: StudentFormData) {
  return put<void>(`/students/${id}`, data)
}

/**
 * 删除学生
 */
export function deleteStudent(id: number | string) {
  return del<void>(`/students/${id}`)
}

/**
 * 批量删除学生
 */
export function deleteStudents(ids: (number | string)[]) {
  return del<void>('/students/batch', ids)
}

/**
 * 检查学号是否存在
 */
export function checkStudentNoExists(studentNo: string, excludeId?: number | string) {
  return get<boolean>('/students/exists', { studentNo, excludeId })
}

/**
 * 更新学生状态
 */
export function updateStudentStatus(id: number | string, status: number) {
  return put<void>(`/students/${id}/status`, { status })
}

/**
 * 分配宿舍
 */
export function assignDormitory(id: number | string, dormitoryId: number, bedNumber?: string) {
  return put<void>(`/students/${id}/dormitory`, { dormitoryId, bedNumber })
}

/**
 * 学生转班
 */
export function transferClass(id: number | string, newClassId: number) {
  return put<void>(`/students/${id}/transfer`, { newClassId })
}

/**
 * 重置密码
 */
export function resetPassword(id: number | string, newPassword: string) {
  return put<void>(`/students/${id}/reset-password`, { newPassword })
}

/**
 * 统计班级学生数量
 */
export function countStudentsByClass(classId: number) {
  return get<number>('/students/count/by-class', { classId })
}

/**
 * 统计宿舍学生数量
 */
export function countStudentsByDormitory(dormitoryId: number) {
  return get<number>('/students/count/by-dormitory', { dormitoryId })
}

/**
 * 快速搜索学生(用于选择器)
 */
export function searchStudents(params: { keyword: string; classId?: number; limit?: number }) {
  return get<Student[]>('/students/search', params)
}
