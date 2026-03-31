import { http } from '@/utils/request'
import type { MsgNotification, MsgSubscriptionRule, MsgTemplate } from '@/types/message'

export interface NotificationListParams {
  page?: number
  size?: number
  isRead?: boolean
  msgType?: string
  keyword?: string
}

export interface NotificationPageResult {
  records: MsgNotification[]
  total: number
  current: number
  size: number
}

export interface UnreadCountResult {
  count: number
}

export interface SendManualParams {
  userIds: number[]
  title: string
  content: string
}

// ==================== 用户消息 ====================

export const messageApi = {
  /** 获取消息列表（分页） */
  getNotifications(params?: NotificationListParams): Promise<NotificationPageResult> {
    return http.get('/msg/notifications', { params })
  },

  /** 获取未读消息数量 */
  getUnreadCount(): Promise<UnreadCountResult> {
    return http.get('/msg/notifications/unread-count')
  },

  /** 标记单条消息已读 */
  markRead(id: number): Promise<void> {
    return http.put(`/msg/notifications/${id}/read`)
  },

  /** 标记全部已读 */
  markAllRead(): Promise<void> {
    return http.put('/msg/notifications/read-all')
  },

  /** 删除消息（软删除） */
  delete(id: number): Promise<void> {
    return http.delete(`/msg/notifications/${id}`)
  },
}

// ==================== 管理配置 ====================

export const msgConfigApi = {
  /** 获取订阅规则列表 */
  getRules(): Promise<MsgSubscriptionRule[]> {
    return http.get('/msg/config/rules')
  },

  /** 创建订阅规则 */
  createRule(data: Partial<MsgSubscriptionRule>): Promise<MsgSubscriptionRule> {
    return http.post('/msg/config/rules', data)
  },

  /** 更新订阅规则 */
  updateRule(id: number, data: Partial<MsgSubscriptionRule>): Promise<MsgSubscriptionRule> {
    return http.put(`/msg/config/rules/${id}`, data)
  },

  /** 删除订阅规则 */
  deleteRule(id: number): Promise<void> {
    return http.delete(`/msg/config/rules/${id}`)
  },

  /** 获取消息模板列表 */
  getTemplates(): Promise<MsgTemplate[]> {
    return http.get('/msg/config/templates')
  },

  /** 创建消息模板 */
  createTemplate(data: Partial<MsgTemplate>): Promise<MsgTemplate> {
    return http.post('/msg/config/templates', data)
  },

  /** 更新消息模板 */
  updateTemplate(id: number, data: Partial<MsgTemplate>): Promise<MsgTemplate> {
    return http.put(`/msg/config/templates/${id}`, data)
  },

  /** 删除消息模板 */
  deleteTemplate(id: number): Promise<void> {
    return http.delete(`/msg/config/templates/${id}`)
  },

  /** 发送手动消息 */
  sendManual(params: SendManualParams): Promise<void> {
    return http.post('/msg/config/send-manual', params)
  },
}
