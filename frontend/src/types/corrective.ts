/**
 * 整改工单类型定义
 */

export type ActionStatus = 'OPEN' | 'IN_PROGRESS' | 'REVIEW' | 'CLOSED' | 'OVERDUE' | 'ESCALATED'
export type ActionSource = 'INSPECTION' | 'APPEAL' | 'MANUAL'
export type ActionSeverity = 'MINOR' | 'MODERATE' | 'SEVERE' | 'CRITICAL'
export type ActionCategory = 'HYGIENE' | 'DISCIPLINE' | 'SAFETY' | 'OTHER'

export interface CorrectiveAction {
  id: number
  actionCode: string
  title: string
  description: string | null
  source: ActionSource
  sourceId: number | null
  severity: ActionSeverity
  category: ActionCategory
  status: ActionStatus
  classId: number | null
  assigneeId: number | null
  deadline: string | null
  resolutionNote: string | null
  resolutionAttachments: string[]
  resolvedAt: string | null
  verifierId: number | null
  verificationResult: string | null
  verificationComment: string | null
  verifiedAt: string | null
  escalationLevel: number
  createdBy: number | null
  createdAt: string
}

export interface CreateActionRequest {
  title: string
  description?: string
  source: string
  sourceId?: number
  severity: string
  category: string
  classId?: number
  assigneeId?: number
  deadline?: string
}

export interface AutoActionRule {
  id: number
  ruleCode: string
  ruleName: string
  triggerType: string
  triggerCondition: string
  severity: string
  category: string
  deadlineHours: number
  autoAssign: boolean
  enabled: boolean
  createdAt: string
}

export const ActionStatusConfig: Record<ActionStatus, { label: string; type: string }> = {
  OPEN: { label: '待处理', type: 'danger' },
  IN_PROGRESS: { label: '整改中', type: 'warning' },
  REVIEW: { label: '待验证', type: 'info' },
  CLOSED: { label: '已关闭', type: 'success' },
  OVERDUE: { label: '已超期', type: 'danger' },
  ESCALATED: { label: '已升级', type: 'danger' }
}

export const ActionSeverityConfig: Record<ActionSeverity, { label: string; type: string }> = {
  MINOR: { label: '轻微', type: 'info' },
  MODERATE: { label: '一般', type: 'warning' },
  SEVERE: { label: '严重', type: 'danger' },
  CRITICAL: { label: '紧急', type: 'danger' }
}

export const ActionCategoryConfig: Record<ActionCategory, { label: string }> = {
  HYGIENE: { label: '卫生' },
  DISCIPLINE: { label: '纪律' },
  SAFETY: { label: '安全' },
  OTHER: { label: '其他' }
}

export const ActionSourceConfig: Record<ActionSource, { label: string }> = {
  INSPECTION: { label: '检查' },
  APPEAL: { label: '申诉' },
  MANUAL: { label: '手动创建' }
}
