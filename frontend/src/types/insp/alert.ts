/**
 * V7 检查平台 - 预警系统类型
 */

// ==================== 预警规则 ====================

export type MetricType = 'SCORE_DROP' | 'CONSECUTIVE_FAIL' | 'HIGH_DEVIATION' | 'LOW_COMPLIANCE' | 'OVERDUE_CORRECTION'
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL'
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED' | 'DISMISSED'

export interface AlertRule {
  id: number
  tenantId?: number
  ruleName: string
  metricType: MetricType
  thresholdConfig: string // JSON
  severity: AlertSeverity
  notificationChannels: string | null // JSON
  isEnabled: boolean
  projectId: number | null
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface CreateAlertRuleRequest {
  ruleName: string
  metricType: MetricType
  thresholdConfig: string
  severity: AlertSeverity
  notificationChannels?: string
  projectId?: number
}

export interface UpdateAlertRuleRequest {
  ruleName?: string
  metricType?: MetricType
  thresholdConfig?: string
  severity?: AlertSeverity
  notificationChannels?: string
  isEnabled?: boolean
  projectId?: number
}

// ==================== 预警记录 ====================

export interface Alert {
  id: number
  tenantId?: number
  alertRuleId: number
  targetId: number | null
  targetType: string | null
  targetName: string | null
  metricValue: number | null
  thresholdValue: number | null
  severity: AlertSeverity
  message: string
  status: AlertStatus
  acknowledgedBy: number | null
  acknowledgedAt: string | null
  resolvedAt: string | null
  triggeredAt: string
}

// ==================== 热力图 ====================

export interface HeatmapDataPoint {
  targetId: number
  targetName: string
  date: string
  score: number
}

// ==================== 桑基图 ====================

export interface SankeyNode {
  name: string
}

export interface SankeyLink {
  source: string
  target: string
  value: number
}

export interface SankeyData {
  nodes: SankeyNode[]
  links: SankeyLink[]
}

// ==================== 计时分析 ====================

export interface TimingStats {
  itemCode: string
  itemName: string
  avgTimeSeconds: number
  minTimeSeconds: number
  maxTimeSeconds: number
  sampleCount: number
}
