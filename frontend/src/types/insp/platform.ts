import type { LongId } from '@/types/common'

/**
 * 检查平台 - Platform BC 类型定义
 */

// ==================== 通知规则 ====================

export interface NotificationRule {
  id: LongId
  tenantId?: LongId
  projectId?: LongId
  ruleName: string
  eventType: string
  condition?: string // JSON condition expression
  channels: string // JSON array: IN_APP, EMAIL, SMS, WECHAT
  recipientType: string // ROLE, USER, DYNAMIC
  recipientConfig?: string // JSON
  isEnabled: boolean
  priority: number
  createdAt?: string
  updatedAt?: string
}

export interface CreateNotificationRuleRequest {
  projectId?: LongId
  ruleName: string
  eventType: string
  channels: string
  recipientType: string
  recipientConfig?: string
  condition?: string
  priority?: number
}

export interface UpdateNotificationRuleRequest {
  ruleName?: string
  eventType?: string
  channels?: string
  recipientType?: string
  recipientConfig?: string
  condition?: string
  priority?: number
}

// ==================== 报表模板 ====================

export interface ReportTemplate {
  id: LongId
  tenantId?: LongId
  templateName: string
  templateCode: string
  reportType: string // DAILY_SUMMARY, PERIOD_REPORT, CORRECTIVE_REPORT, INSPECTOR_REPORT, CUSTOM
  formatConfig?: string // JSON
  headerConfig?: string // JSON
  isDefault: boolean
  isEnabled: boolean
  createdBy?: number
  createdAt?: string
  updatedAt?: string
}

export interface CreateReportTemplateRequest {
  tenantId?: LongId
  templateName: string
  templateCode: string
  reportType: string
  formatConfig?: string
  headerConfig?: string
  isDefault?: boolean
  createdBy?: number
}

export interface UpdateReportTemplateRequest {
  templateName?: string
  reportType?: string
  formatConfig?: string
  headerConfig?: string
}

// ==================== Webhook 订阅 ====================

export interface WebhookSubscription {
  id: LongId
  tenantId?: LongId
  projectId?: LongId
  subscriptionName: string
  targetUrl: string
  secret?: string
  eventTypes: string // JSON array
  isEnabled: boolean
  retryCount: number
  lastTriggeredAt?: string
  lastStatus?: string // SUCCESS, FAILED
  createdAt?: string
  updatedAt?: string
}

export interface CreateWebhookRequest {
  projectId?: LongId
  subscriptionName: string
  targetUrl: string
  secret?: string
  eventTypes: string
  retryCount?: number
}

export interface UpdateWebhookRequest {
  subscriptionName?: string
  targetUrl?: string
  secret?: string
  eventTypes?: string
  retryCount?: number
}

// ==================== 审计日志 ====================

export interface AuditTrailEntry {
  id: LongId
  tenantId?: LongId
  userId: LongId
  userName?: string
  action: string
  resourceType: string
  resourceId?: LongId
  resourceName?: string
  details?: string // JSON
  ipAddress?: string
  occurredAt: string
}

// ==================== 问题分类 ====================

export interface IssueCategory {
  id: LongId
  tenantId?: LongId
  categoryCode: string
  categoryName: string
  parentId?: LongId
  icon?: string
  sortOrder: number
  isEnabled: boolean
  children?: IssueCategory[]
  createdAt?: string
  updatedAt?: string
}

// ==================== 假期日历 ====================

export interface HolidayCalendar {
  id: LongId
  tenantId?: LongId
  calendarName: string
  year: number
  holidays: string // JSON array of dates
  workdays: string // JSON array of make-up workdays
  isDefault: boolean
  createdAt?: string
  updatedAt?: string
}

export interface CreateHolidayCalendarRequest {
  calendarName: string
  year: number
  holidays?: string
  workdays?: string
  isDefault?: boolean
}

export interface UpdateHolidayCalendarRequest {
  calendarName?: string
  holidays?: string
  workdays?: string
  isDefault?: boolean
}
