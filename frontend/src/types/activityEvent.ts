import type { LongId } from '@/types/common'

/** 字段变更 */
export interface FieldChange {
  fieldName: string
  oldValue: string | null
  newValue: string | null
}

/** 活动事件 */
export interface ActivityEvent {
  id: string
  requestId: string | null

  resourceType: string
  resourceId: string
  resourceName: string | null

  action: string
  actionLabel: string | null
  result: 'SUCCESS' | 'FAILURE' | 'PARTIAL'
  errorMessage: string | null

  changedFields: FieldChange[] | null
  beforeSnapshot: string | null
  afterSnapshot: string | null

  userId: LongId | null
  userName: string | null

  sourceIp: string | null
  userAgent: string | null
  apiEndpoint: string | null
  httpMethod: string | null

  reason: string | null
  module: string | null
  tags: Record<string, string> | null

  occurredAt: string
}

/** 查询参数 */
export interface ActivityEventQuery {
  module?: string
  resourceType?: string
  resourceId?: string
  action?: string
  userId?: LongId
  result?: string
  startTime?: string
  endTime?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

/** 统计数据 */
export interface ActivityEventStats {
  total: number
  success: number
  failed: number
  today: number
}
