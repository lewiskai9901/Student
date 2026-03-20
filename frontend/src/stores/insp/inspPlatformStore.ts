/**
 * V7 检查平台 - Platform BC Store
 * Notification Rules, Report Templates, Webhooks, Audit Trail, Issue Categories
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  NotificationRule,
  ReportTemplate,
  WebhookSubscription,
  AuditTrailEntry,
  IssueCategory,
  HolidayCalendar,
  CreateNotificationRuleRequest,
  UpdateNotificationRuleRequest,
  CreateReportTemplateRequest,
  UpdateReportTemplateRequest,
  CreateWebhookRequest,
  UpdateWebhookRequest,
  CreateHolidayCalendarRequest,
  UpdateHolidayCalendarRequest,
} from '@/types/insp/platform'
import { notificationRuleApi } from '@/api/insp/notificationRule'
import { reportTemplateApi } from '@/api/insp/reportTemplate'
import { webhookApi } from '@/api/insp/webhook'
import { auditTrailApi } from '@/api/insp/auditTrail'
import { issueCategoryApi } from '@/api/insp/issueCategory'
import { holidayCalendarApi } from '@/api/insp/holidayCalendar'

export const useInspPlatformStore = defineStore('inspPlatform', () => {
  // ==================== State ====================

  // Notification Rules
  const notificationRules = ref<NotificationRule[]>([])
  const currentRule = ref<NotificationRule | null>(null)

  // Report Templates
  const reportTemplates = ref<ReportTemplate[]>([])
  const currentTemplate = ref<ReportTemplate | null>(null)

  // Webhooks
  const webhooks = ref<WebhookSubscription[]>([])
  const currentWebhook = ref<WebhookSubscription | null>(null)

  // Audit Trail
  const auditEntries = ref<AuditTrailEntry[]>([])

  // Issue Categories
  const issueCategories = ref<IssueCategory[]>([])

  // Holiday Calendars
  const holidayCalendars = ref<HolidayCalendar[]>([])
  const currentCalendar = ref<HolidayCalendar | null>(null)

  const loading = ref(false)

  // ==================== Notification Rules ====================

  async function fetchNotificationRules(projectId?: number) {
    loading.value = true
    try {
      notificationRules.value = await notificationRuleApi.list(projectId)
    } finally {
      loading.value = false
    }
  }

  async function fetchNotificationRule(id: number) {
    loading.value = true
    try {
      currentRule.value = await notificationRuleApi.getById(id)
    } finally {
      loading.value = false
    }
  }

  async function createNotificationRule(data: CreateNotificationRuleRequest) {
    const created = await notificationRuleApi.create(data)
    notificationRules.value.unshift(created)
    return created
  }

  async function updateNotificationRule(id: number, data: UpdateNotificationRuleRequest) {
    const updated = await notificationRuleApi.update(id, data)
    const idx = notificationRules.value.findIndex(r => r.id === id)
    if (idx !== -1) notificationRules.value[idx] = updated
    if (currentRule.value?.id === id) currentRule.value = updated
    return updated
  }

  async function deleteNotificationRule(id: number) {
    await notificationRuleApi.delete(id)
    notificationRules.value = notificationRules.value.filter(r => r.id !== id)
    if (currentRule.value?.id === id) currentRule.value = null
  }

  async function enableNotificationRule(id: number) {
    await notificationRuleApi.enable(id)
    const idx = notificationRules.value.findIndex(r => r.id === id)
    if (idx !== -1) notificationRules.value[idx] = { ...notificationRules.value[idx], isEnabled: true }
  }

  async function disableNotificationRule(id: number) {
    await notificationRuleApi.disable(id)
    const idx = notificationRules.value.findIndex(r => r.id === id)
    if (idx !== -1) notificationRules.value[idx] = { ...notificationRules.value[idx], isEnabled: false }
  }

  // ==================== Report Templates ====================

  async function fetchReportTemplates(reportType?: string) {
    loading.value = true
    try {
      reportTemplates.value = await reportTemplateApi.list(reportType)
    } finally {
      loading.value = false
    }
  }

  async function fetchReportTemplate(id: number) {
    loading.value = true
    try {
      currentTemplate.value = await reportTemplateApi.getById(id)
    } finally {
      loading.value = false
    }
  }

  async function createReportTemplate(data: CreateReportTemplateRequest) {
    const created = await reportTemplateApi.create(data)
    reportTemplates.value.unshift(created)
    return created
  }

  async function updateReportTemplate(id: number, data: UpdateReportTemplateRequest) {
    const updated = await reportTemplateApi.update(id, data)
    const idx = reportTemplates.value.findIndex(t => t.id === id)
    if (idx !== -1) reportTemplates.value[idx] = updated
    if (currentTemplate.value?.id === id) currentTemplate.value = updated
    return updated
  }

  async function deleteReportTemplate(id: number) {
    await reportTemplateApi.delete(id)
    reportTemplates.value = reportTemplates.value.filter(t => t.id !== id)
    if (currentTemplate.value?.id === id) currentTemplate.value = null
  }

  async function generateReport(id: number, params?: Record<string, unknown>) {
    return await reportTemplateApi.generate(id, params || {})
  }

  // ==================== Webhooks ====================

  async function fetchWebhooks(projectId?: number) {
    loading.value = true
    try {
      webhooks.value = await webhookApi.list(projectId)
    } finally {
      loading.value = false
    }
  }

  async function fetchWebhook(id: number) {
    loading.value = true
    try {
      currentWebhook.value = await webhookApi.getById(id)
    } finally {
      loading.value = false
    }
  }

  async function createWebhook(data: CreateWebhookRequest) {
    const created = await webhookApi.create(data)
    webhooks.value.unshift(created)
    return created
  }

  async function updateWebhook(id: number, data: UpdateWebhookRequest) {
    const updated = await webhookApi.update(id, data)
    const idx = webhooks.value.findIndex(w => w.id === id)
    if (idx !== -1) webhooks.value[idx] = updated
    if (currentWebhook.value?.id === id) currentWebhook.value = updated
    return updated
  }

  async function deleteWebhook(id: number) {
    await webhookApi.delete(id)
    webhooks.value = webhooks.value.filter(w => w.id !== id)
    if (currentWebhook.value?.id === id) currentWebhook.value = null
  }

  async function enableWebhook(id: number) {
    await webhookApi.enable(id)
    const idx = webhooks.value.findIndex(w => w.id === id)
    if (idx !== -1) webhooks.value[idx] = { ...webhooks.value[idx], isEnabled: true }
  }

  async function disableWebhook(id: number) {
    await webhookApi.disable(id)
    const idx = webhooks.value.findIndex(w => w.id === id)
    if (idx !== -1) webhooks.value[idx] = { ...webhooks.value[idx], isEnabled: false }
  }

  async function testWebhook(id: number) {
    return await webhookApi.test(id)
  }

  // ==================== Audit Trail ====================

  async function fetchAuditEntries(params?: {
    userId?: number
    action?: string
    resourceType?: string
    resourceId?: number
    startDate?: string
    endDate?: string
  }) {
    loading.value = true
    try {
      auditEntries.value = await auditTrailApi.search(params || {})
    } finally {
      loading.value = false
    }
  }

  async function fetchRecentAuditEntries(limit?: number) {
    loading.value = true
    try {
      auditEntries.value = await auditTrailApi.getRecent(limit)
    } finally {
      loading.value = false
    }
  }

  // ==================== Issue Categories ====================

  async function fetchIssueCategories() {
    loading.value = true
    try {
      const flat = await issueCategoryApi.list()
      // Build tree from flat list
      const map = new Map<number, IssueCategory & { children?: IssueCategory[] }>()
      for (const cat of flat) {
        map.set(cat.id, { ...cat, children: [] })
      }
      const roots: IssueCategory[] = []
      for (const cat of map.values()) {
        if (cat.parentId && map.has(cat.parentId)) {
          map.get(cat.parentId)!.children!.push(cat)
        } else {
          roots.push(cat)
        }
      }
      issueCategories.value = roots
    } finally {
      loading.value = false
    }
  }

  async function createIssueCategory(data: Partial<IssueCategory>) {
    const created = await issueCategoryApi.create(data)
    issueCategories.value.unshift(created)
    return created
  }

  async function updateIssueCategory(id: number, data: Partial<IssueCategory>) {
    const updated = await issueCategoryApi.update(id, data)
    const idx = issueCategories.value.findIndex(c => c.id === id)
    if (idx !== -1) issueCategories.value[idx] = updated
    return updated
  }

  async function deleteIssueCategory(id: number) {
    await issueCategoryApi.delete(id)
    issueCategories.value = issueCategories.value.filter(c => c.id !== id)
  }

  // ==================== Holiday Calendars ====================

  async function fetchHolidayCalendars(year?: number) {
    loading.value = true
    try {
      holidayCalendars.value = year
        ? await holidayCalendarApi.getByYear(year)
        : await holidayCalendarApi.list()
    } finally {
      loading.value = false
    }
  }

  async function fetchHolidayCalendar(id: number) {
    loading.value = true
    try {
      currentCalendar.value = await holidayCalendarApi.getById(id)
    } finally {
      loading.value = false
    }
  }

  async function createHolidayCalendar(data: CreateHolidayCalendarRequest) {
    const created = await holidayCalendarApi.create(data)
    holidayCalendars.value.unshift(created)
    return created
  }

  async function updateHolidayCalendar(id: number, data: UpdateHolidayCalendarRequest) {
    const updated = await holidayCalendarApi.update(id, data)
    const idx = holidayCalendars.value.findIndex(c => c.id === id)
    if (idx !== -1) holidayCalendars.value[idx] = updated
    if (currentCalendar.value?.id === id) currentCalendar.value = updated
    return updated
  }

  async function deleteHolidayCalendar(id: number) {
    await holidayCalendarApi.delete(id)
    holidayCalendars.value = holidayCalendars.value.filter(c => c.id !== id)
    if (currentCalendar.value?.id === id) currentCalendar.value = null
  }

  return {
    // State
    notificationRules, currentRule,
    reportTemplates, currentTemplate,
    webhooks, currentWebhook,
    auditEntries,
    issueCategories,
    loading,
    // Notification Rules
    fetchNotificationRules, fetchNotificationRule,
    createNotificationRule, updateNotificationRule, deleteNotificationRule,
    enableNotificationRule, disableNotificationRule,
    // Report Templates
    fetchReportTemplates, fetchReportTemplate,
    createReportTemplate, updateReportTemplate, deleteReportTemplate,
    generateReport,
    // Webhooks
    fetchWebhooks, fetchWebhook,
    createWebhook, updateWebhook, deleteWebhook,
    enableWebhook, disableWebhook, testWebhook,
    // Audit Trail
    fetchAuditEntries, fetchRecentAuditEntries,
    // Issue Categories
    fetchIssueCategories, createIssueCategory, updateIssueCategory, deleteIssueCategory,
    // Holiday Calendars
    holidayCalendars, currentCalendar,
    fetchHolidayCalendars, fetchHolidayCalendar,
    createHolidayCalendar, updateHolidayCalendar, deleteHolidayCalendar,
  }
})
