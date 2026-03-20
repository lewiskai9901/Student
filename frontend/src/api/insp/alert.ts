/**
 * V7 检查平台 - 预警系统 API
 */
import { http } from '@/utils/request'
import type {
  AlertRule,
  CreateAlertRuleRequest,
  UpdateAlertRuleRequest,
  Alert,
  AlertStatus,
  HeatmapDataPoint,
  SankeyData,
  TimingStats,
} from '@/types/insp/alert'

const RULES_BASE = '/v7/insp/alert-rules'
const ALERTS_BASE = '/v7/insp/alerts'
const ANALYTICS_BASE = '/v7/insp/analytics'

// ==================== Alert Rules ====================

export function getAlertRules(): Promise<AlertRule[]> {
  return http.get<AlertRule[]>(RULES_BASE)
}

export function getAlertRule(id: number): Promise<AlertRule> {
  return http.get<AlertRule>(`${RULES_BASE}/${id}`)
}

export function createAlertRule(data: CreateAlertRuleRequest): Promise<AlertRule> {
  return http.post<AlertRule>(RULES_BASE, data)
}

export function updateAlertRule(id: number, data: UpdateAlertRuleRequest): Promise<AlertRule> {
  return http.put<AlertRule>(`${RULES_BASE}/${id}`, data)
}

export function deleteAlertRule(id: number): Promise<void> {
  return http.delete(`${RULES_BASE}/${id}`)
}

export function toggleAlertRule(id: number, enabled: boolean): Promise<AlertRule> {
  return http.put<AlertRule>(`${RULES_BASE}/${id}`, { isEnabled: enabled })
}

// ==================== Alerts ====================

export function getAlerts(params?: { status?: AlertStatus; limit?: number }): Promise<Alert[]> {
  return http.get<Alert[]>(ALERTS_BASE, { params })
}

export function getRecentAlerts(limit: number = 20): Promise<Alert[]> {
  return http.get<Alert[]>(`${ALERTS_BASE}/recent`, { params: { limit } })
}

export function acknowledgeAlert(id: number): Promise<Alert> {
  return http.put<Alert>(`${ALERTS_BASE}/${id}/acknowledge`)
}

export function resolveAlert(id: number): Promise<Alert> {
  return http.put<Alert>(`${ALERTS_BASE}/${id}/resolve`)
}

export function dismissAlert(id: number): Promise<Alert> {
  return http.put<Alert>(`${ALERTS_BASE}/${id}/dismiss`)
}

// ==================== Analytics Extensions ====================

export function getHeatmapData(params: {
  projectId: number
  dateFrom: string
  dateTo: string
  targetType?: string
  dimensionId?: number
}): Promise<HeatmapDataPoint[]> {
  return http.get<HeatmapDataPoint[]>(`${ANALYTICS_BASE}/heatmap`, { params })
}

export function getIssueFlow(params: {
  projectId: number
  dateFrom: string
  dateTo: string
}): Promise<SankeyData> {
  return http.get<SankeyData>(`${ANALYTICS_BASE}/issue-flow`, { params })
}

export function getTimingStats(params: {
  projectId: number
  dateFrom: string
  dateTo: string
}): Promise<TimingStats[]> {
  return http.get<TimingStats[]>(`${ANALYTICS_BASE}/timing`, { params })
}

export const inspAlertApi = {
  getAlertRules,
  getAlertRule,
  createAlertRule,
  updateAlertRule,
  deleteAlertRule,
  toggleAlertRule,
  getAlerts,
  getRecentAlerts,
  acknowledgeAlert,
  resolveAlert,
  dismissAlert,
  getHeatmapData,
  getIssueFlow,
  getTimingStats,
}
