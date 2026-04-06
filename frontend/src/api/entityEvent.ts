import { http } from '@/utils/request'
import type {
  EntityEventType,
  EntityEvent,
  EntityEventStats,
  CreateEntityEventTypeCommand,
  UpdateEntityEventTypeCommand,
} from '@/types/entityEvent'

const TYPE_BASE = '/event/types'
const EVENT_BASE = '/event/events'

// ==================== Event Types ====================

/** 获取所有事件类型（后端返回按分类分组，这里展平为列表） */
export async function listEntityEventTypes(): Promise<EntityEventType[]> {
  const data = await http.get<any>(TYPE_BASE)
  // 后端返回 [{categoryCode, categoryName, categoryPolarity, types: [...]}]
  // 展平为 EntityEventType[]
  if (Array.isArray(data)) {
    const flat: EntityEventType[] = []
    for (const group of data) {
      if (group.types && Array.isArray(group.types)) {
        for (const t of group.types) {
          flat.push({
            ...t,
            id: Number(t.id),
            categoryCode: t.categoryCode || group.categoryCode,
            categoryName: t.categoryName || group.categoryName,
            categoryPolarity: t.categoryPolarity || group.categoryPolarity,
          })
        }
      } else if (group.typeCode) {
        // 已经是扁平的
        flat.push(group)
      }
    }
    return flat
  }
  return data as EntityEventType[]
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
