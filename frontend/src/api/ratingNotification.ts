import { http } from '@/utils/request'

/**
 * 评级通报API - 对接后端RatingNotificationController
 */

// ==================== 类型定义 ====================

/**
 * 荣誉通报请求
 */
export interface HonorNotificationRequest {
  checkPlanId: number | string
  notificationType: 'HONOR' | 'FULL' | 'ALERT'
  ruleId?: number | string
  levelIds?: (number | string)[]
  periodStart: string
  periodEnd: string
  minFrequency?: number
  title?: string
  createdBy?: number | string
}

/**
 * 通报生成结果VO
 */
export interface NotificationGenerateResultVO {
  notificationId: number | string
  fileName: string
  filePath: string
  classCount: number
  generatedAt: string
}

/**
 * 证书生成请求
 */
export interface CertificateGenerateRequest {
  badgeRecordId: number | string
  classIds: (number | string)[]
  periodStart: string
  periodEnd: string
  templateType?: string
}

/**
 * 证书生成结果VO
 */
export interface CertificateGenerateResultVO {
  classId: number | string
  className: string
  certificatePath: string
  success: boolean
  message: string
}

/**
 * 海报生成请求
 */
export interface PosterGenerateRequest {
  checkPlanId: number | string
  posterType: 'HONOR' | 'RANKING' | 'COMPARISON'
  ruleId?: number | string
  levelIds?: (number | string)[]
  periodStart: string
  periodEnd: string
  minFrequency?: number
  title?: string
  subtitle?: string
}

/**
 * 海报生成结果VO
 */
export interface PosterGenerateResultVO {
  posterPath: string
  classCount: number
  generatedAt: string
}

/**
 * 通报历史VO
 */
export interface NotificationHistoryVO {
  notificationId: number | string
  notificationType: string
  title: string
  filePath: string
  periodStart: string
  periodEnd: string
  classCount: number
  createdAt: string
}

// ==================== API接口 ====================

/**
 * 生成荣誉通报
 */
export function generateHonorNotification(data: HonorNotificationRequest) {
  return http.post<NotificationGenerateResultVO>('/rating/notification/honor', data)
}

/**
 * 批量生成证书
 */
export function batchGenerateCertificates(data: CertificateGenerateRequest) {
  return http.post<CertificateGenerateResultVO[]>('/rating/notification/certificate/batch', data)
}

/**
 * 生成海报
 */
export function generatePoster(data: PosterGenerateRequest) {
  return http.post<PosterGenerateResultVO>('/rating/notification/poster', data)
}

/**
 * 获取通报历史
 */
export function getNotificationHistory(params: {
  checkPlanId: number | string
  notificationType?: string
}) {
  return http.get<NotificationHistoryVO[]>('/rating/notification/history', { params })
}

/**
 * 发布通报
 */
export function publishNotification(notificationId: number | string) {
  return http.post<void>(`/rating/notification/publish/${notificationId}`)
}

/**
 * 删除通报
 */
export function deleteNotification(notificationId: number | string) {
  return http.delete<void>(`/rating/notification/${notificationId}`)
}

/**
 * 下载通报文件
 */
export function downloadNotification(filePath: string) {
  return http.get<Blob>(filePath, {
    responseType: 'blob'
  })
}

// ==================== 常量 ====================

export const NOTIFICATION_TYPES = {
  HONOR: 'HONOR',
  FULL: 'FULL',
  ALERT: 'ALERT'
} as const

export const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  HONOR: '荣誉榜通报',
  FULL: '完整评级通报',
  ALERT: '预警通报'
}

export const NOTIFICATION_TYPE_DESCRIPTIONS: Record<string, string> = {
  HONOR: '仅列出获奖班级，适用于表彰公告',
  FULL: '包含所有班级排名，适用于全面通报',
  ALERT: '仅列出需要改进的班级，适用于预警提醒'
}

export const POSTER_TYPES = {
  HONOR: 'HONOR',
  RANKING: 'RANKING',
  COMPARISON: 'COMPARISON'
} as const

export const POSTER_TYPE_LABELS: Record<string, string> = {
  HONOR: '荣誉榜海报',
  RANKING: '排名榜海报',
  COMPARISON: '对比榜海报'
}
