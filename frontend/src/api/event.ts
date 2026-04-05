import { http } from '@/utils/request'
import type {
  TriggerPoint,
  EventTrigger,
  EventType,
  EntityEvent,
  EventStatItem,
} from '@/types/event'

// ==================== 触发点 API ====================

export const triggerPointApi = {
  /** 获取触发点列表 */
  list(module?: string) {
    return http.get<TriggerPoint[]>('/event/trigger-points', {
      params: module ? { module } : undefined,
    })
  },

  /** 获取触发点详情 */
  getById(id: number) {
    return http.get<TriggerPoint>(`/event/trigger-points/${id}`)
  },

  /** 创建触发点 */
  create(data: Partial<TriggerPoint>) {
    return http.post('/event/trigger-points', data)
  },

  /** 更新触发点 */
  update(id: number, data: Partial<TriggerPoint>) {
    return http.put(`/event/trigger-points/${id}`, data)
  },

  /** 删除触发点 */
  delete(id: number) {
    return http.delete(`/event/trigger-points/${id}`)
  },

  /** 启用触发点 */
  enable(id: number) {
    return http.put(`/event/trigger-points/${id}/enable`)
  },

  /** 禁用触发点 */
  disable(id: number) {
    return http.put(`/event/trigger-points/${id}/disable`)
  },
}

// ==================== 事件触发器 API ====================

export const eventTriggerApi = {
  /** 获取触发器列表 */
  list(params?: { pointCode?: string; eventType?: string }) {
    return http.get<EventTrigger[]>('/event/triggers', { params })
  },

  /** 获取触发器详情 */
  getById(id: number) {
    return http.get<EventTrigger>(`/event/triggers/${id}`)
  },

  /** 创建触发器 */
  create(data: Partial<EventTrigger>) {
    return http.post('/event/triggers', data)
  },

  /** 更新触发器 */
  update(id: number, data: Partial<EventTrigger>) {
    return http.put(`/event/triggers/${id}`, data)
  },

  /** 删除触发器 */
  delete(id: number) {
    return http.delete(`/event/triggers/${id}`)
  },

  /** 启用触发器 */
  enable(id: number) {
    return http.put(`/event/triggers/${id}/enable`)
  },

  /** 禁用触发器 */
  disable(id: number) {
    return http.put(`/event/triggers/${id}/disable`)
  },

  /** 测试触发器匹配 */
  test(pointCode: string, context: Record<string, unknown>) {
    return http.post<Array<Record<string, unknown>>>('/event/triggers/test', {
      pointCode,
      context,
    })
  },
}

// ==================== 事件类型 API ====================

export const eventTypeApi = {
  /** 获取所有事件类型 */
  list(category?: string) {
    return http.get<EventType[]>('/entity-event-types', {
      params: category ? { category } : undefined,
    })
  },

  /** 获取启用的事件类型 */
  listEnabled() {
    return http.get<EventType[]>('/entity-event-types/enabled')
  },

  /** 创建事件类型 */
  create(data: Partial<EventType>) {
    return http.post<EventType>('/entity-event-types', data)
  },

  /** 更新事件类型 */
  update(id: number, data: Partial<EventType>) {
    return http.put<EventType>(`/entity-event-types/${id}`, data)
  },

  /** 删除事件类型 */
  delete(id: number) {
    return http.delete(`/entity-event-types/${id}`)
  },
}

// ==================== 实体事件 API ====================

export const entityEventApi = {
  /** 按主体查询事件时间线 */
  bySubject(type: string, id: number, limit = 50) {
    return http.get<EntityEvent[]>(`/entity-events/subject/${type}/${id}`, {
      params: { limit },
    })
  },

  /** 按关联主体查询事件时间线 */
  byRelated(type: string, id: number, limit = 50) {
    return http.get<EntityEvent[]>(`/entity-events/related/${type}/${id}`, {
      params: { limit },
    })
  },

  /** 获取主体事件统计 */
  statistics(type: string, id: number) {
    return http.get<EventStatItem[]>(`/entity-events/stats/${type}/${id}`)
  },
}
