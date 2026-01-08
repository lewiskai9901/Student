import { http } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  StudentDormitory,
  StudentDormitoryQueryRequest,
  StudentCheckInRequest,
  StudentCheckOutRequest,
  StudentChangeDormitoryRequest
} from '@/types/studentDormitory'

/**
 * 分页查询住宿记录
 */
export const getStudentDormitoryPage = (params: StudentDormitoryQueryRequest) => {
  return http.get<PageResult<StudentDormitory>>('/student-dormitory/page', { params })
}

/**
 * 查询学生当前住宿信息
 */
export const getStudentCurrentDormitory = (studentId: number) => {
  return http.get<StudentDormitory>(`/student-dormitory/student/${studentId}/current`)
}

/**
 * 查询宿舍当前入住学生列表
 */
export const getDormitoryStudents = (dormitoryId: number) => {
  return http.get<StudentDormitory[]>(`/student-dormitory/dormitory/${dormitoryId}/students`)
}

/**
 * 查询学生住宿历史
 */
export const getStudentDormitoryHistory = (studentId: number) => {
  return http.get<StudentDormitory[]>(`/student-dormitory/student/${studentId}/history`)
}

/**
 * 学生入住
 */
export const checkIn = (data: StudentCheckInRequest) => {
  return http.post<number>('/student-dormitory/check-in', data)
}

/**
 * 学生退宿
 */
export const checkOut = (data: StudentCheckOutRequest) => {
  return http.post('/student-dormitory/check-out', data)
}

/**
 * 学生调换宿舍
 */
export const changeDormitory = (data: StudentChangeDormitoryRequest) => {
  return http.post('/student-dormitory/change', data)
}

/**
 * 批量入住
 */
export const batchCheckIn = (data: StudentCheckInRequest[]) => {
  return http.post<number>('/student-dormitory/batch-check-in', data)
}

/**
 * 批量退宿
 */
export const batchCheckOut = (studentIds: number[], reason?: string) => {
  return http.post<number>('/student-dormitory/batch-check-out', studentIds, {
    params: { reason }
  })
}

/**
 * 同步学生表宿舍数据
 */
export const syncFromStudentTable = () => {
  return http.post<number>('/student-dormitory/sync')
}
