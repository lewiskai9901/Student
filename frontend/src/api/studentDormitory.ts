/**
 * 学生住宿管理 API
 *
 * 用于管理学生入住、退宿、调换宿舍等操作（Level 3）
 */
import { http } from '@/utils/request'

// API 路径
const BASE_URL = '/student-dormitory'

// ==================== 类型定义 ====================

/**
 * 学生入住请求
 */
export interface StudentCheckInRequest {
  studentId: number
  dormitoryId: number
  bedNumber?: string
  checkInDate?: string
  remark?: string
}

/**
 * 学生退宿请求
 */
export interface StudentCheckOutRequest {
  studentId: number
  checkOutDate?: string
  reason?: string
}

/**
 * 学生调换宿舍请求
 */
export interface StudentChangeDormitoryRequest {
  studentId: number
  newDormitoryId: number
  newBedNumber?: string
  reason?: string
}

/**
 * 学生住宿信息响应
 */
export interface StudentDormitoryResponse {
  id: number
  studentId: number
  studentNo: string
  studentName: string
  dormitoryId: number
  dormitoryNo: string
  bedNumber: string
  buildingId: number
  buildingName: string
  floorNumber: number
  className: string
  checkInDate: string
  checkOutDate?: string
  status: number // 1=在住, 2=已退宿, 3=办理中
  remark?: string
}

/**
 * 学生住宿查询请求
 */
export interface StudentDormitoryQueryRequest {
  pageNum?: number
  pageSize?: number
  studentId?: number
  dormitoryId?: number
  buildingId?: number
  classId?: number
  status?: number
  studentName?: string
  studentNo?: string
}

// ==================== API 函数 ====================

/**
 * 分页查询住宿记录
 */
export function getStudentDormitoryPage(params?: StudentDormitoryQueryRequest) {
  return http.get<{ records: StudentDormitoryResponse[]; total: number }>(`${BASE_URL}/page`, { params })
}

/**
 * 查询学生当前住宿信息
 */
export function getCurrentByStudentId(studentId: number): Promise<StudentDormitoryResponse | null> {
  return http.get<StudentDormitoryResponse | null>(`${BASE_URL}/student/${studentId}/current`)
}

/**
 * 查询宿舍当前入住学生列表
 */
export function getStudentsByDormitoryId(dormitoryId: number): Promise<StudentDormitoryResponse[]> {
  return http.get<StudentDormitoryResponse[]>(`${BASE_URL}/dormitory/${dormitoryId}/students`)
}

/**
 * 查询学生住宿历史
 */
export function getHistoryByStudentId(studentId: number): Promise<StudentDormitoryResponse[]> {
  return http.get<StudentDormitoryResponse[]>(`${BASE_URL}/student/${studentId}/history`)
}

/**
 * 学生入住
 */
export function checkIn(data: StudentCheckInRequest): Promise<number> {
  return http.post<number>(`${BASE_URL}/check-in`, data)
}

/**
 * 学生退宿
 */
export function checkOut(data: StudentCheckOutRequest): Promise<void> {
  return http.post(`${BASE_URL}/check-out`, data)
}

/**
 * 学生调换宿舍
 */
export function changeDormitory(data: StudentChangeDormitoryRequest): Promise<void> {
  return http.post(`${BASE_URL}/change`, data)
}

/**
 * 批量入住
 */
export function batchCheckIn(requests: StudentCheckInRequest[]): Promise<number> {
  return http.post<number>(`${BASE_URL}/batch-check-in`, requests)
}

/**
 * 批量退宿
 */
export function batchCheckOut(studentIds: number[], reason?: string): Promise<number> {
  return http.post<number>(`${BASE_URL}/batch-check-out`, studentIds, { params: { reason } })
}

// ==================== API 对象封装 ====================

/**
 * 学生住宿 API 对象
 */
export const studentDormitoryApi = {
  getPage: getStudentDormitoryPage,
  getCurrentByStudent: getCurrentByStudentId,
  getStudentsByDormitory: getStudentsByDormitoryId,
  getHistoryByStudent: getHistoryByStudentId,
  checkIn,
  checkOut,
  changeDormitory,
  batchCheckIn,
  batchCheckOut
}

export default studentDormitoryApi
