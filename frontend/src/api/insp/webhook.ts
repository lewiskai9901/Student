/**
 * V7 检查平台 - Webhook 订阅 API
 */
import { http } from '@/utils/request'
import type { WebhookSubscription, CreateWebhookRequest, UpdateWebhookRequest } from '@/types/insp/platform'

const BASE = '/v7/insp/webhooks'

// ==================== CRUD ====================

export function listWebhooks(projectId?: number): Promise<WebhookSubscription[]> {
  return http.get<WebhookSubscription[]>(BASE, { params: { projectId } })
}

export function getWebhook(id: number): Promise<WebhookSubscription> {
  return http.get<WebhookSubscription>(`${BASE}/${id}`)
}

export function createWebhook(data: CreateWebhookRequest): Promise<WebhookSubscription> {
  return http.post<WebhookSubscription>(BASE, data)
}

export function updateWebhook(id: number, data: UpdateWebhookRequest): Promise<WebhookSubscription> {
  return http.put<WebhookSubscription>(`${BASE}/${id}`, data)
}

export function deleteWebhook(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 操作 ====================

export function enableWebhook(id: number): Promise<void> {
  return http.post(`${BASE}/${id}/enable`)
}

export function disableWebhook(id: number): Promise<void> {
  return http.post(`${BASE}/${id}/disable`)
}

export function testWebhook(id: number): Promise<void> {
  return http.post(`${BASE}/${id}/test`)
}

// ==================== API 对象 ====================

export const webhookApi = {
  list: listWebhooks,
  getById: getWebhook,
  create: createWebhook,
  update: updateWebhook,
  delete: deleteWebhook,
  enable: enableWebhook,
  disable: disableWebhook,
  test: testWebhook,
}
