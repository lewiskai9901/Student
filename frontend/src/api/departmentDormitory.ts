/**
 * 部门宿舍管理 API
 *
 * 用于部门管理员管理本部门宿舍分配给班级的功能（Level 2）
 */
import { http } from '@/utils/request'
import type { Dormitory } from '@/types/dormitory'

// API 路径
const BASE_URL = '/department-dormitory'

// ==================== 类型定义 ====================

/**
 * 班级-宿舍绑定信息
 */
export interface ClassDormitoryBinding {
  id: number
  classId: number
  className: string
  classCode: string
  dormitoryId: number
  dormitoryNo: string
  buildingId: number
  buildingName: string
  bedCapacity: number
  occupiedBeds: number
  floorNumber: number
  studentCount: number
  createdTime: string
}

/**
 * 班级简要信息
 */
export interface ClassInfo {
  id: number
  className: string
  classCode: string
  gradeLevel: number
  studentCount: number
  orgUnitId: number
  orgUnitName?: string
  teacherId?: number
  teacherName?: string
  status: number
}

/**
 * 分配宿舍请求
 */
export interface AssignDormitoryRequest {
  dormitoryId: number
  classId: number
  allocatedBeds?: number
}

/**
 * 批量分配请求
 */
export interface BatchAssignRequest {
  classId: number
  dormitoryIds: number[]
}

/**
 * 部门宿舍统计
 */
export interface DepartmentDormitoryStatistics {
  totalDormitories: number
  totalBeds: number
  occupiedBeds: number
  assignedDormitories: number
  totalClasses: number
}

// ==================== API 函数 ====================

/**
 * 获取当前用户部门下的宿舍列表
 */
export function getMyDepartmentDormitories(): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${BASE_URL}/dormitories`)
}

/**
 * 获取当前用户部门下的班级列表
 */
export function getMyDepartmentClasses(): Promise<ClassInfo[]> {
  return http.get<ClassInfo[]>(`${BASE_URL}/classes`)
}

/**
 * 获取班级-宿舍绑定关系列表
 */
export function getBindings(): Promise<ClassDormitoryBinding[]> {
  return http.get<ClassDormitoryBinding[]>(`${BASE_URL}/bindings`)
}

/**
 * 按班级获取绑定关系
 */
export function getBindingsByClass(classId: number | string): Promise<ClassDormitoryBinding[]> {
  return http.get<ClassDormitoryBinding[]>(`${BASE_URL}/bindings/by-class/${classId}`)
}

/**
 * 将宿舍分配给班级
 */
export function assignDormitoryToClass(data: AssignDormitoryRequest): Promise<void> {
  return http.post(`${BASE_URL}/assign`, data)
}

/**
 * 批量分配宿舍给班级
 */
export function batchAssignDormitoriesToClass(data: BatchAssignRequest): Promise<number> {
  return http.post<number>(`${BASE_URL}/assign/batch`, data)
}

/**
 * 取消宿舍-班级绑定
 */
export function unassignDormitoryFromClass(dormitoryId: number | string, classId: number | string): Promise<void> {
  return http.delete(`${BASE_URL}/unassign`, { params: { dormitoryId, classId } })
}

/**
 * 获取部门宿舍统计信息
 */
export function getDepartmentDormitoryStatistics(): Promise<DepartmentDormitoryStatistics> {
  return http.get<DepartmentDormitoryStatistics>(`${BASE_URL}/statistics`)
}

/**
 * 获取未分配给任何班级的宿舍
 */
export function getUnassignedDormitories(): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${BASE_URL}/dormitories/unassigned`)
}

// ==================== API 对象封装 ====================

/**
 * 部门宿舍管理 API 对象
 */
export const departmentDormitoryApi = {
  getDormitories: getMyDepartmentDormitories,
  getClasses: getMyDepartmentClasses,
  getBindings,
  getBindingsByClass,
  assign: assignDormitoryToClass,
  batchAssign: batchAssignDormitoriesToClass,
  unassign: unassignDormitoryFromClass,
  getStatistics: getDepartmentDormitoryStatistics,
  getUnassignedDormitories
}

export default departmentDormitoryApi
