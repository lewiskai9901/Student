import { http } from '@/utils/request'

// ==================== 类型定义 ====================

// 消息类型
export enum MessageType {
  TASK_ASSIGN = 'TASK_ASSIGN',     // 任务分配
  TASK_REMIND = 'TASK_REMIND',     // 任务提醒
  TASK_APPROVE = 'TASK_APPROVE',   // 审批通知
  TASK_REJECT = 'TASK_REJECT',     // 打回通知
  TASK_COMPLETE = 'TASK_COMPLETE', // 完成通知
  SYSTEM = 'SYSTEM'                // 系统通知
}

// 消息DTO
export interface SystemMessageDTO {
  id: number
  messageType: string
  messageTypeText: string
  title: string
  content?: string
  senderId?: number
  senderName?: string
  receiverId: number
  receiverName?: string
  isRead: number
  readTime?: string
  businessType?: string
  businessId?: number
  extraData?: Record<string, any>
  createdAt: string
}

// ==================== API方法 ====================

// 分页查询消息
export function getMessageList(params: {
  pageNum?: number
  pageSize?: number
  messageType?: string
  isRead?: number
}) {
  return http.get<{ records: SystemMessageDTO[]; total: number }>('/task/messages', { params })
}

// 获取未读消息数量
export function getUnreadCount() {
  return http.get<{ count: number }>('/task/messages/unread-count')
}

// 标记消息为已读
export function markAsRead(id: number) {
  return http.post<boolean>(`/task/messages/${id}/read`)
}

// 标记所有消息为已读
export function markAllAsRead() {
  return http.post<number>('/task/messages/read-all')
}

// 删除消息
export function deleteMessage(id: number) {
  return http.delete<boolean>(`/task/messages/${id}`)
}
