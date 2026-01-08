/**
 * 公告管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 公告信息
export interface Announcement {
  id: number | string
  title: string
  content: string
  type: number             // 1:系统公告 2:通知 3:活动
  typeName?: string
  priority: number         // 1:普通 2:重要 3:紧急
  priorityName?: string
  publisherId?: number
  publisherName?: string
  publishTime?: string
  expireTime?: string
  isTop?: boolean          // 是否置顶
  viewCount?: number       // 浏览次数
  attachments?: string[]   // 附件
  status: number           // 0:草稿 1:已发布 2:已撤销
  statusText?: string
  createdAt?: string
  updatedAt?: string
}

// 查询参数
export interface AnnouncementQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  title?: string
  type?: number
  priority?: number
  status?: number
  startTime?: string
  endTime?: string
}

// 创建/更新公告请求
export interface AnnouncementFormData {
  title: string
  content: string
  type: number
  priority?: number
  publishTime?: string
  expireTime?: string
  isTop?: boolean
  attachments?: string[]
  status?: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 获取公告列表
 */
export function getAnnouncementList(params: AnnouncementQueryParams = {}) {
  return get<PageResult<Announcement>>('/announcements', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 获取公告详情
 */
export function getAnnouncementDetail(id: number | string) {
  return get<Announcement>(`/announcements/${id}`)
}

/**
 * 创建公告
 */
export function createAnnouncement(data: AnnouncementFormData) {
  return post<Announcement>('/announcements', data)
}

/**
 * 更新公告
 */
export function updateAnnouncement(id: number | string, data: AnnouncementFormData) {
  return put<Announcement>(`/announcements/${id}`, data)
}

/**
 * 删除公告
 */
export function deleteAnnouncement(id: number | string) {
  return del<void>(`/announcements/${id}`)
}

/**
 * 发布公告
 */
export function publishAnnouncement(id: number | string) {
  return post<void>(`/announcements/${id}/publish`)
}

/**
 * 撤销公告
 */
export function revokeAnnouncement(id: number | string) {
  return post<void>(`/announcements/${id}/revoke`)
}

/**
 * 置顶/取消置顶公告
 */
export function toggleTopAnnouncement(id: number | string, isTop: boolean) {
  return put<void>(`/announcements/${id}/top`, { isTop })
}

/**
 * 获取最新公告(首页展示用)
 */
export function getLatestAnnouncements(limit: number = 5) {
  return get<Announcement[]>('/announcements/latest', { limit })
}

/**
 * 获取已发布的公告列表(用户端)
 */
export function getPublishedAnnouncements(params?: { pageNum?: number; pageSize?: number; type?: number }) {
  return get<PageResult<Announcement>>('/announcements/published', {
    pageNum: params?.pageNum || 1,
    pageSize: params?.pageSize || 10,
    ...params
  })
}

/**
 * 增加公告浏览次数
 */
export function incrementViewCount(id: number | string) {
  return post<void>(`/announcements/${id}/view`)
}
