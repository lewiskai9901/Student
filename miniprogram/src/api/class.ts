/**
 * 班级管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 班级信息
export interface Class {
  id: number | string
  classCode: string
  className: string
  gradeId?: number
  gradeName?: string
  majorId?: number
  majorName?: string
  departmentId?: number
  departmentName?: string
  headTeacherId?: number
  headTeacherName?: string
  classroomId?: number
  classroomName?: string
  studentCount?: number
  maleCount?: number
  femaleCount?: number
  maxCapacity?: number
  enrollmentYear?: number
  status: number
  statusText?: string
  createdAt?: string
  updatedAt?: string
}

// 查询参数
export interface ClassQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  className?: string
  gradeId?: number
  majorId?: number
  departmentId?: number
  headTeacherId?: number
  status?: number
}

// 创建/更新班级请求
export interface ClassFormData {
  classCode: string
  className: string
  gradeId?: number
  majorId?: number
  departmentId?: number
  headTeacherId?: number
  maxCapacity?: number
  enrollmentYear?: number
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
 * 获取班级列表
 */
export function getClassList(params: ClassQueryParams = {}) {
  return get<PageResult<Class>>('/classes', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 获取班级详情
 */
export function getClassDetail(id: number | string) {
  return get<Class>(`/classes/${id}`)
}

/**
 * 创建班级
 */
export function createClass(data: ClassFormData) {
  return post<Class>('/classes', data)
}

/**
 * 更新班级信息
 */
export function updateClass(id: number | string, data: ClassFormData) {
  return put<Class>(`/classes/${id}`, data)
}

/**
 * 删除班级
 */
export function deleteClass(id: number | string) {
  return del<void>(`/classes/${id}`)
}

/**
 * 批量删除班级
 */
export function batchDeleteClasses(ids: (number | string)[]) {
  return del<void>('/classes/batch', { ids })
}

/**
 * 获取班级学生列表
 */
export function getClassStudents(classId: number | string) {
  return get<any[]>(`/classes/${classId}/students`)
}

/**
 * 设置班主任
 */
export function assignTeacher(classId: number | string, teacherId: number | null) {
  return post<void>(`/classes/${classId}/assign-teacher`, { teacherId })
}

/**
 * 为班级分配教室
 */
export function assignClassroom(classId: number | string, classroomId: number) {
  return post<void>(`/classes/${classId}/assign-classroom`, { classroomId })
}

/**
 * 取消班级教室分配
 */
export function removeClassroom(classId: number | string) {
  return del<void>(`/classes/${classId}/classroom`)
}

/**
 * 获取班级的教室信息
 */
export function getClassClassroom(classId: number | string) {
  return get<any>(`/classes/${classId}/classroom`)
}

/**
 * 为班级添加宿舍
 */
export function addDormitory(classId: number | string, dormitoryId: number, allocatedBeds: number) {
  return post<void>(`/classes/${classId}/dormitories`, { dormitoryId, allocatedBeds })
}

/**
 * 移除班级宿舍
 */
export function removeDormitory(classId: number | string, dormitoryId: number) {
  return del<void>(`/classes/${classId}/dormitories/${dormitoryId}`)
}

/**
 * 获取班级的宿舍列表
 */
export function getClassDormitories(classId: number | string) {
  return get<any[]>(`/classes/${classId}/dormitories`)
}

/**
 * 获取所有班级(不分页,用于下拉选择)
 */
export function getAllClasses() {
  return get<Class[]>('/classes/all')
}

/**
 * 按部门获取班级
 */
export function getClassesByDepartment(departmentId: number) {
  return get<Class[]>('/classes/by-department', { departmentId })
}

/**
 * 按年级获取班级
 */
export function getClassesByGrade(gradeId: number) {
  return get<Class[]>('/classes/by-grade', { gradeId })
}
