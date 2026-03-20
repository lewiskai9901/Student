import { http } from '@/utils/request'
import type { ActivityEvent, ActivityEventQuery, ActivityEventStats } from '@/types/activityEvent'

const BASE = '/activity-events'

/** 分页查询活动事件 */
export function listActivityEvents(params: ActivityEventQuery) {
  return http.get<{ records: ActivityEvent[]; total: number; pageNum: number; pageSize: number }>(
    BASE,
    { params }
  )
}

/** 获取资源活动时间线 */
export function getResourceTimeline(resourceType: string, resourceId: string | number, limit = 50) {
  return http.get<ActivityEvent[]>(`${BASE}/resource/${resourceType}/${resourceId}`, {
    params: { limit }
  })
}

/** 获取用户活动历史 */
export function getUserActivity(userId: number, limit = 50) {
  return http.get<ActivityEvent[]>(`${BASE}/user/${userId}`, {
    params: { limit }
  })
}

/** 获取统计数据 */
export function getActivityStats(startTime?: string, endTime?: string) {
  return http.get<ActivityEventStats>(`${BASE}/stats`, {
    params: { startTime, endTime }
  })
}
