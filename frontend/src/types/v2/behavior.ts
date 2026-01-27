/**
 * 学生行为记录类型定义
 */

export type BehaviorType = 'VIOLATION' | 'COMMENDATION'
export type BehaviorSource = 'INSPECTION' | 'TEACHER_REPORT' | 'SELF_REPORT'
export type BehaviorCategory = 'HYGIENE' | 'DISCIPLINE' | 'SAFETY' | 'ATTENDANCE' | 'ACADEMIC' | 'OTHER'
export type BehaviorStatus = 'RECORDED' | 'NOTIFIED' | 'ACKNOWLEDGED' | 'RESOLVED'
export type AlertType = 'FREQUENCY' | 'SEVERITY' | 'TREND'

export interface BehaviorRecord {
  id: number
  studentId: number
  classId: number
  behaviorType: BehaviorType
  source: BehaviorSource
  sourceId: number | null
  category: BehaviorCategory
  title: string
  detail: string | null
  deductionAmount: number
  status: BehaviorStatus
  recordedBy: number | null
  recordedAt: string
  notifiedAt: string | null
  acknowledgedAt: string | null
  resolvedAt: string | null
  resolutionNote: string | null
}

export interface CreateBehaviorRecordRequest {
  studentId: number
  classId: number
  behaviorType: string
  source: string
  sourceId?: number
  category: string
  title: string
  detail?: string
  deductionAmount?: number
}

export interface BehaviorAlert {
  id: number
  studentId: number
  classId: number
  alertType: AlertType
  alertLevel: string
  title: string
  description: string | null
  triggerData: string | null
  isRead: boolean
  isHandled: boolean
  handledBy: number | null
  handledAt: string | null
  handleNote: string | null
  createdAt: string
}

export interface BehaviorProfile {
  studentId: number
  totalViolations: number
  totalCommendations: number
  recentViolations: number
  riskLevel: string
  trend: string
}

export const BehaviorTypeConfig: Record<BehaviorType, { label: string; type: string }> = {
  VIOLATION: { label: '违规', type: 'danger' },
  COMMENDATION: { label: '表扬', type: 'success' }
}

export const BehaviorStatusConfig: Record<BehaviorStatus, { label: string; type: string }> = {
  RECORDED: { label: '已记录', type: 'info' },
  NOTIFIED: { label: '已通知', type: 'warning' },
  ACKNOWLEDGED: { label: '已确认', type: '' },
  RESOLVED: { label: '已处理', type: 'success' }
}

export const BehaviorCategoryConfig: Record<BehaviorCategory, { label: string }> = {
  HYGIENE: { label: '卫生' },
  DISCIPLINE: { label: '纪律' },
  SAFETY: { label: '安全' },
  ATTENDANCE: { label: '考勤' },
  ACADEMIC: { label: '学业' },
  OTHER: { label: '其他' }
}

export const AlertTypeConfig: Record<AlertType, { label: string; type: string }> = {
  FREQUENCY: { label: '频率预警', type: 'warning' },
  SEVERITY: { label: '严重程度预警', type: 'danger' },
  TREND: { label: '趋势预警', type: 'info' }
}
