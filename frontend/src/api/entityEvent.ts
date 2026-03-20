import { http } from '@/utils/request'
import type {
  EntityEventType,
  EntityEvent,
  EntityEventStats,
  CreateEntityEventTypeCommand,
  UpdateEntityEventTypeCommand,
} from '@/types/entityEvent'

const TYPE_BASE = '/entity-event-types'
const EVENT_BASE = '/entity-events'

// ==================== Event Types ====================

/** 获取所有事件类型 */
export function listEntityEventTypes() {
  return http.get<EntityEventType[]>(TYPE_BASE)
}

/** 创建事件类型 */
export function createEntityEventType(cmd: CreateEntityEventTypeCommand) {
  return http.post<EntityEventType>(TYPE_BASE, cmd)
}

/** 更新事件类型 */
export function updateEntityEventType(id: number, cmd: UpdateEntityEventTypeCommand) {
  return http.put<EntityEventType>(`${TYPE_BASE}/${id}`, cmd)
}

/** 删除事件类型 */
export function deleteEntityEventType(id: number) {
  return http.delete<void>(`${TYPE_BASE}/${id}`)
}

// ==================== Events ====================

/** 获取主体事件时间线 */
export function getSubjectTimeline(subjectType: string, subjectId: number, limit = 50) {
  return http.get<EntityEvent[]>(`${EVENT_BASE}/subject/${subjectType}/${subjectId}`, {
    params: { limit }
  })
}

/** 获取主体事件统计 */
export function getSubjectStats(subjectType: string, subjectId: number) {
  return http.get<EntityEventStats>(`${EVENT_BASE}/stats/${subjectType}/${subjectId}`)
}
