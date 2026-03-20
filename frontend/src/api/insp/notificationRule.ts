/**
 * V7 检查平台 - 通知规则 API
 */
import { http } from '@/utils/request'
import type { NotificationRule, CreateNotificationRuleRequest, UpdateNotificationRuleRequest } from '@/types/insp/platform'

const BASE = '/v7/insp/notification-rules'

// ==================== CRUD ====================

export function listNotificationRules(projectId?: number): Promise<NotificationRule[]> {
  return http.get<NotificationRule[]>(BASE, { params: { projectId } })
}

export function getNotificationRule(id: number): Promise<NotificationRule> {
  return http.get<NotificationRule>(`${BASE}/${id}`)
}

export function createNotificationRule(data: CreateNotificationRuleRequest): Promise<NotificationRule> {
  return http.post<NotificationRule>(BASE, data)
}

export function updateNotificationRule(id: number, data: UpdateNotificationRuleRequest): Promise<NotificationRule> {
  return http.put<NotificationRule>(`${BASE}/${id}`, data)
}

export function deleteNotificationRule(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 操作 ====================

export function enableNotificationRule(id: number): Promise<void> {
  return http.post(`${BASE}/${id}/enable`)
}

export function disableNotificationRule(id: number): Promise<void> {
  return http.post(`${BASE}/${id}/disable`)
}

// ==================== API 对象 ====================

export const notificationRuleApi = {
  list: listNotificationRules,
  getById: getNotificationRule,
  create: createNotificationRule,
  update: updateNotificationRule,
  delete: deleteNotificationRule,
  enable: enableNotificationRule,
  disable: disableNotificationRule,
}
