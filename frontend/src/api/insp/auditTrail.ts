/**
 * V7 检查平台 - 审计日志 API
 */
import { http } from '@/utils/request'
import type { AuditTrailEntry } from '@/types/insp/platform'

const BASE = '/v7/insp/audit-trail'

// ==================== 查询 ====================

export function searchAuditTrail(params: {
  userId?: number
  action?: string
  resourceType?: string
  resourceId?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}): Promise<AuditTrailEntry[]> {
  return http.get<AuditTrailEntry[]>(BASE, { params })
}

export function getRecentAuditTrail(limit?: number): Promise<AuditTrailEntry[]> {
  return http.get<AuditTrailEntry[]>(`${BASE}/recent`, { params: { limit } })
}

// ==================== API 对象 ====================

export const auditTrailApi = {
  search: searchAuditTrail,
  getRecent: getRecentAuditTrail,
}
