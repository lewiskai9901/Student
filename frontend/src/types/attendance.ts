import type { LongId } from '@/types/common'

/**
 * 考勤管理类型定义
 */

export interface AttendanceRecord {
  id: LongId
  semesterId: LongId
  courseId?: LongId
  courseName?: string
  orgUnitId: LongId
  className?: string
  studentId: LongId
  studentName?: string
  studentNo?: string
  attendanceDate: string
  period?: number
  attendanceType: 1 | 2
  status: 1 | 2 | 3 | 4 | 5 // 1出勤 2迟到 3早退 4请假 5旷课
  checkInTime?: string
  checkMethod: string
  remark?: string
  recordedBy?: number
}

export interface LeaveRequest {
  id: LongId
  studentId: LongId
  studentName?: string
  studentNo?: string
  orgUnitId?: LongId
  leaveType: 1 | 2 | 3 // 1事假 2病假 3公假
  startDate: string
  endDate: string
  startPeriod?: number
  endPeriod?: number
  reason: string
  attachmentUrls?: string
  approvalStatus: 0 | 1 | 2 // 0待审 1通过 2拒绝
  approverId?: LongId
  approverName?: string
  approvalTime?: string
  approvalComment?: string
  createdAt?: string
}

export interface AttendanceStats {
  total: number
  present: number
  late: number
  earlyLeave: number
  leave: number
  absent: number
  attendanceRate: number
  absentRate: number
}

export interface StudentAttendanceStats extends AttendanceStats {
  studentId: LongId
  recentRecords?: AttendanceRecord[]
}

export interface ClassAttendanceRow {
  studentId: LongId
  studentNo: string
  studentName: string
  status: number | null
  remark: string | null
  recordId: LongId | null
}

export const ATTENDANCE_STATUS = [
  { value: 1, label: '出勤', color: 'success' },
  { value: 2, label: '迟到', color: 'warning' },
  { value: 3, label: '早退', color: 'warning' },
  { value: 4, label: '请假', color: 'info' },
  { value: 5, label: '旷课', color: 'danger' },
] as const

export const LEAVE_TYPE = [
  { value: 1, label: '事假' },
  { value: 2, label: '病假' },
  { value: 3, label: '公假' },
] as const

export const APPROVAL_STATUS = [
  { value: 0, label: '待审批', color: 'warning' },
  { value: 1, label: '已通过', color: 'success' },
  { value: 2, label: '已拒绝', color: 'danger' },
] as const
