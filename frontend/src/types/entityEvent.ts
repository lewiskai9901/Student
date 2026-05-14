import type { LongId } from '@/types/common'

export interface EntityEventType {
  id: LongId
  tenantId?: LongId
  categoryCode: string
  categoryName: string
  categoryPolarity?: string // POSITIVE/NEGATIVE/NEUTRAL
  typeCode: string
  typeName: string
  hasScore?: boolean
  hasSeverity?: boolean
  severityLevels?: string | null
  icon?: string | null
  color?: string | null
  applicableSubjects?: string | null
  isSystem?: boolean
  isEnabled?: boolean | number
  sortOrder?: number
}

export interface EntityEvent {
  id: LongId
  subjectType: string
  subjectId: LongId
  subjectName: string | null
  eventCategory: string
  eventType: string
  eventLabel: string | null
  payload: string | null  // JSON
  sourceModule: string | null
  sourceRefType: string | null
  sourceRefId: LongId | null
  tags: string | null  // JSON array
  createdBy: number | null
  createdByName: string | null
  occurredAt: string
}

export interface EntityEventStats {
  subjectType: string
  subjectId: LongId
  totalCount: number
  categoryBreakdown: Record<string, number>
  recentCount: number
}

export interface CreateEntityEventTypeCommand {
  categoryCode: string
  categoryName: string
  /** 分类极性 POSITIVE/NEGATIVE/NEUTRAL */
  categoryPolarity?: string
  typeCode: string
  typeName: string
  /** 创建时是否启用 (兼容旧调用方传 number) */
  isEnabled?: boolean | number
  hasScore?: boolean
  hasSeverity?: boolean
  severityLevels?: string | null
  icon?: string | null
  color?: string | null
  applicableSubjects?: string | null
  sortOrder?: number
}

export interface UpdateEntityEventTypeCommand extends Omit<Partial<CreateEntityEventTypeCommand>, 'isEnabled'> {
  isEnabled?: boolean | number
}
