import { http } from '@/utils/request'

/**
 * 通报记录接口
 */
export interface NotificationRecord {
  id: string | number  // 雪花ID，后端序列化为字符串
  planId: number
  templateId: number
  notificationType: string
  dailyCheckIds: string
  checkRecordIds: string
  checkRounds: string
  ratingResultIds: string
  title: string
  contentSnapshot: string
  variableValues: string
  totalCount: number
  totalClasses: number
  totalDeductionCount: number
  fileFormat: string
  fileName: string
  filePath: string
  fileSize: number
  status: number
  publishStatus: number  // 0-草稿, 1-已发布
  errorMessage: string
  generatedAt: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

/**
 * 生成通报草稿请求
 */
export interface GenerateNotificationRequest {
  templateId: number
  dailyCheckIds: number[]
  checkRounds?: number[]
  deductionItemIds?: number[]  // 扣分项筛选，不传则包含全部
  variableValues?: string
}

/**
 * 更新通报内容请求
 */
export interface UpdateNotificationContentRequest {
  title?: string
  contentHtml?: string
}

/**
 * 生成通报
 */
export function generateNotification(planId: string | number, data: GenerateNotificationRequest) {
  return http.post<NotificationRecord>(`/check-plans/${planId}/notifications/generate`, data)
}

/**
 * 获取通报历史
 */
export function getNotificationHistory(planId: string | number) {
  return http.get<NotificationRecord[]>(`/check-plans/${planId}/notifications`)
}

/**
 * 获取通报详情
 */
export function getNotificationById(id: string | number) {
  return http.get<NotificationRecord>(`/notifications/${id}`)
}

/**
 * 重新生成通报
 */
export function regenerateNotification(id: string | number) {
  return http.post<NotificationRecord>(`/notifications/${id}/regenerate`)
}

/**
 * 删除通报
 */
export function deleteNotification(id: string | number) {
  return http.delete<void>(`/notifications/${id}`)
}

/**
 * 更新通报内容
 */
export function updateNotificationContent(id: string | number, data: UpdateNotificationContentRequest) {
  return http.put<NotificationRecord>(`/notifications/${id}/content`, data)
}

/**
 * 发布通报
 */
export function publishNotification(id: string | number) {
  return http.post<NotificationRecord>(`/notifications/${id}/publish`)
}

/**
 * 下载通报文件（按需生成）
 */
export function downloadNotification(id: string | number, format: 'PDF' | 'WORD' = 'PDF') {
  return http.get<Blob>(`/notifications/${id}/download`, {
    params: { format },
    responseType: 'blob'
  })
}

/**
 * 预览通报内容
 */
export function previewNotification(id: string | number) {
  return http.get<{ title: string; html: string }>(`/notifications/${id}/preview`)
}

/**
 * 获取通报状态文本
 */
export function getNotificationStatusText(status: number): string {
  const statusMap: Record<number, string> = {
    0: '编辑中',
    1: '已完成',
    2: '生成失败'
  }
  return statusMap[status] || '未知'
}

/**
 * 获取发布状态文本
 */
export function getPublishStatusText(publishStatus: number): string {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '已发布'
  }
  return statusMap[publishStatus] || '草稿'
}

/**
 * 获取通报类型文本
 */
export function getNotificationTypeText(type: string): string {
  const typeMap: Record<string, string> = {
    'SINGLE_CHECK': '单次检查通报',
    'MULTI_ROUND': '多轮次通报',
    'MULTI_CHECK': '多检查汇总通报',
    'RATING': '评级通报'
  }
  return typeMap[type] || type
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 公开获取通报详情（无需登录）
 */
export function getNotificationPublic(id: string | number) {
  return http.get<NotificationRecord>(`/public/notifications/${id}`)
}
