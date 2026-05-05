import type { TaskStatus, CaseStatus, AppealStatus } from '../api/types'

const TASK_STATUS_LABEL: Record<string, string> = {
  PENDING: '待处理',
  CLAIMED: '已认领',
  IN_PROGRESS: '进行中',
  SUBMITTED: '已提交',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}

const TASK_STATUS_COLOR: Record<string, string> = {
  PENDING: '#5a6a7a',
  CLAIMED: '#3a7bd5',
  IN_PROGRESS: '#3a7bd5',
  SUBMITTED: '#9a5cc6',
  APPROVED: '#15a87e',
  REJECTED: '#e0592a',
  CANCELLED: '#a0aab4'
}

const CASE_STATUS_LABEL: Record<string, string> = {
  PENDING: '待分配',
  ASSIGNED: '已指派',
  IN_PROGRESS: '处理中',
  SUBMITTED: '已提交',
  VERIFIED: '已核实',
  REJECTED: '已驳回',
  CLOSED: '已关闭',
  ESCALATED: '已升级'
}

const CASE_STATUS_COLOR: Record<string, string> = {
  PENDING: '#5a6a7a',
  ASSIGNED: '#3a7bd5',
  IN_PROGRESS: '#3a7bd5',
  SUBMITTED: '#9a5cc6',
  VERIFIED: '#15a87e',
  REJECTED: '#e0592a',
  CLOSED: '#a0aab4',
  ESCALATED: '#d4a030'
}

const APPEAL_STATUS_LABEL: Record<string, string> = {
  PENDING: '审核中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回'
}

const APPEAL_STATUS_COLOR: Record<string, string> = {
  PENDING: '#3a7bd5',
  APPROVED: '#15a87e',
  REJECTED: '#e0592a',
  WITHDRAWN: '#a0aab4'
}

export function taskStatusLabel(s?: TaskStatus): string {
  return (s && TASK_STATUS_LABEL[s]) ?? '-'
}
export function taskStatusColor(s?: TaskStatus): string {
  return (s && TASK_STATUS_COLOR[s]) ?? '#5a6a7a'
}
export function caseStatusLabel(s?: CaseStatus): string {
  return (s && CASE_STATUS_LABEL[s]) ?? '-'
}
export function caseStatusColor(s?: CaseStatus): string {
  return (s && CASE_STATUS_COLOR[s]) ?? '#5a6a7a'
}
export function appealStatusLabel(s?: AppealStatus): string {
  return (s && APPEAL_STATUS_LABEL[s]) ?? '-'
}
export function appealStatusColor(s?: AppealStatus): string {
  return (s && APPEAL_STATUS_COLOR[s]) ?? '#5a6a7a'
}

export function formatDateTime(s?: string): string {
  if (!s) return '-'
  // 支持 ISO 'yyyy-MM-ddTHH:mm:ss[.SSS][Z]' → 'yyyy-MM-dd HH:mm'
  const m = s.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/)
  return m ? `${m[1]} ${m[2]}` : '-'
}
