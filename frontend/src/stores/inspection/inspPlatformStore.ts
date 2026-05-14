/**
 * 检查平台 - Platform BC Store
 *
 * 当前激活: Issue Categories / Holiday Calendars / Audit Trail
 * 已下线 (Day 2 #13 死代码清理): NotificationRule / ReportTemplate / Webhook
 *   - API 文件不存在, view 也不消费, 阻塞 prod build, 直接 strip
 *   - 如未来重新引入, 需先建 src/api/inspection/{notificationRule,reportTemplate,webhook}.ts
 */
import type { LongId } from '@/types/common'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  AuditTrailEntry,
  IssueCategory,
  HolidayCalendar,
  CreateHolidayCalendarRequest,
  UpdateHolidayCalendarRequest,
} from '@/types/insp/platform'
import { auditTrailApi } from '@/api/inspection/auditTrail'
import { issueCategoryApi } from '@/api/inspection/issueCategory'
import { holidayCalendarApi } from '@/api/inspection/holidayCalendar'

export const useInspPlatformStore = defineStore('inspPlatform', () => {
  const auditEntries = ref<AuditTrailEntry[]>([])
  const issueCategories = ref<IssueCategory[]>([])
  const holidayCalendars = ref<HolidayCalendar[]>([])
  const currentCalendar = ref<HolidayCalendar | null>(null)
  const loading = ref(false)

  async function fetchAuditEntries(params?: {
    userId?: LongId
    action?: string
    resourceType?: string
    resourceId?: LongId
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

  async function fetchIssueCategories() {
    loading.value = true
    try {
      const flat = await issueCategoryApi.list()
      const map = new Map<LongId, IssueCategory & { children?: IssueCategory[] }>()
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

  async function updateIssueCategory(id: LongId, data: Partial<IssueCategory>) {
    const updated = await issueCategoryApi.update(id, data)
    const idx = issueCategories.value.findIndex(c => c.id === id)
    if (idx !== -1) issueCategories.value[idx] = updated
    return updated
  }

  async function deleteIssueCategory(id: LongId) {
    await issueCategoryApi.delete(id)
    issueCategories.value = issueCategories.value.filter(c => c.id !== id)
  }

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

  async function fetchHolidayCalendar(id: LongId) {
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

  async function updateHolidayCalendar(id: LongId, data: UpdateHolidayCalendarRequest) {
    const updated = await holidayCalendarApi.update(id, data)
    const idx = holidayCalendars.value.findIndex(c => c.id === id)
    if (idx !== -1) holidayCalendars.value[idx] = updated
    if (currentCalendar.value?.id === id) currentCalendar.value = updated
    return updated
  }

  async function deleteHolidayCalendar(id: LongId) {
    await holidayCalendarApi.delete(id)
    holidayCalendars.value = holidayCalendars.value.filter(c => c.id !== id)
    if (currentCalendar.value?.id === id) currentCalendar.value = null
  }

  return {
    auditEntries,
    issueCategories,
    holidayCalendars, currentCalendar,
    loading,
    fetchAuditEntries, fetchRecentAuditEntries,
    fetchIssueCategories, createIssueCategory, updateIssueCategory, deleteIssueCategory,
    fetchHolidayCalendars, fetchHolidayCalendar,
    createHolidayCalendar, updateHolidayCalendar, deleteHolidayCalendar,
  }
})
