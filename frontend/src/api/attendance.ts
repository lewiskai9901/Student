/**
 * 考勤管理 API
 */
import { http } from '@/utils/request'
import type {
  AttendanceRecord,
  AttendanceStats,
  StudentAttendanceStats,
  ClassAttendanceRow,
  LeaveRequest,
} from '@/types/attendance'

const BASE = '/attendance'

// ==================== 考勤记录 ====================

export function listRecords(params: Record<string, any>): Promise<AttendanceRecord[]> {
  return http.get<AttendanceRecord[]>(`${BASE}/records`, { params })
}

export function getByClass(params: {
  classId: number
  date: string
  courseId?: number
  period?: number
}): Promise<ClassAttendanceRow[]> {
  return http.get<ClassAttendanceRow[]>(`${BASE}/records/by-class`, { params })
}

export function createRecord(data: Record<string, any>): Promise<any> {
  return http.post(`${BASE}/records`, data)
}

export function updateRecord(id: number | string, data: Record<string, any>): Promise<any> {
  return http.put(`${BASE}/records/${id}`, data)
}

export function deleteRecord(id: number | string): Promise<any> {
  return http.delete(`${BASE}/records/${id}`)
}

export function batchRecord(data: Record<string, any>): Promise<any> {
  return http.post(`${BASE}/batch`, data)
}

// ==================== 考勤统计 ====================

export function getStatistics(params: Record<string, any>): Promise<AttendanceStats> {
  return http.get<AttendanceStats>(`${BASE}/statistics`, { params })
}

export function getStudentStats(
  studentId: number | string,
  params?: Record<string, any>
): Promise<StudentAttendanceStats> {
  return http.get<StudentAttendanceStats>(`${BASE}/statistics/student/${studentId}`, { params })
}

// ==================== 请假管理 ====================

export function createLeave(data: Record<string, any>): Promise<any> {
  return http.post(`${BASE}/leave-requests`, data)
}

export function listLeaves(params?: Record<string, any>): Promise<LeaveRequest[]> {
  return http.get<LeaveRequest[]>(`${BASE}/leave-requests`, { params })
}

export function approveLeave(id: number | string, comment?: string): Promise<any> {
  return http.post(`${BASE}/leave-requests/${id}/approve`, comment ? { comment } : {})
}

export function rejectLeave(id: number | string, comment?: string): Promise<any> {
  return http.post(`${BASE}/leave-requests/${id}/reject`, { comment })
}

export function pendingLeaves(): Promise<LeaveRequest[]> {
  return http.get<LeaveRequest[]>(`${BASE}/leave-requests/pending`)
}
