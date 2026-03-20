export interface EntityEventType {
  id: number
  tenantId: number
  categoryCode: string
  categoryName: string
  typeCode: string
  typeName: string
  hasScore: boolean
  hasSeverity: boolean
  severityLevels: string | null  // JSON array
  icon: string | null
  color: string | null
  applicableSubjects: string | null  // JSON array
  isSystem: boolean
  isEnabled: boolean
  sortOrder: number
}

export interface EntityEvent {
  id: number
  subjectType: string
  subjectId: number
  subjectName: string | null
  eventCategory: string
  eventType: string
  eventLabel: string | null
  payload: string | null  // JSON
  sourceModule: string | null
  sourceRefType: string | null
  sourceRefId: number | null
  tags: string | null  // JSON array
  createdBy: number | null
  createdByName: string | null
  occurredAt: string
}

export interface EntityEventStats {
  subjectType: string
  subjectId: number
  totalCount: number
  categoryBreakdown: Record<string, number>
  recentCount: number
}

export interface CreateEntityEventTypeCommand {
  categoryCode: string
  categoryName: string
  typeCode: string
  typeName: string
  hasScore: boolean
  hasSeverity: boolean
  severityLevels?: string | null
  icon?: string | null
  color?: string | null
  applicableSubjects?: string | null
  sortOrder?: number
}

export interface UpdateEntityEventTypeCommand extends Partial<CreateEntityEventTypeCommand> {
  isEnabled?: boolean
}
