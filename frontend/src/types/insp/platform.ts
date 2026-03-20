/**
 * V7 检查平台 - Platform BC 类型定义
 */

// ==================== 通知规则 ====================

export interface NotificationRule {
  id: number
  tenantId?: number
  projectId?: number
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
  projectId?: number
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
  id: number
  tenantId?: number
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
  tenantId?: number
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
  id: number
  tenantId?: number
  projectId?: number
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
  projectId?: number
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
  id: number
  tenantId?: number
  userId: number
  userName?: string
  action: string
  resourceType: string
  resourceId?: number
  resourceName?: string
  details?: string // JSON
  ipAddress?: string
  occurredAt: string
}

// ==================== 问题分类 ====================

export interface IssueCategory {
  id: number
  tenantId?: number
  categoryCode: string
  categoryName: string
  parentId?: number
  icon?: string
  sortOrder: number
  isEnabled: boolean
  children?: IssueCategory[]
  createdAt?: string
  updatedAt?: string
}

// ==================== 假期日历 ====================

export interface HolidayCalendar {
  id: number
  tenantId?: number
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
