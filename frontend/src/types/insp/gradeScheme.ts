import type { LongId } from '@/types/common'

export interface GradeScheme {
  id: LongId
  tenantId: LongId
  displayName: string
  description: string | null
  schemeType: 'SCORE_RANGE' | 'PERCENT_RANGE'
  isSystem: boolean
  grades: GradeDefinition[]
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface GradeDefinition {
  id?: LongId
  gradeSchemeId?: LongId
  code: string
  name: string
  minValue: number
  maxValue: number
  color: string | null
  icon: string | null
  sortOrder: number
}

export interface CreateGradeSchemeRequest {
  displayName: string
  description?: string
  schemeType: string
  grades: GradeDefinitionInput[]
}

export interface UpdateGradeSchemeRequest {
  displayName: string
  description?: string
  grades: GradeDefinitionInput[]
}

export interface CloneGradeSchemeRequest {
  sourceSchemeId: LongId
  displayName: string
}

export interface GradeDefinitionInput {
  code: string
  name: string
  /** PERCENT_RANGE 模式下后端按 sortOrder 自动分桶, 前端可不传 */
  minValue?: number
  maxValue?: number
  color?: string | null
  icon?: string | null
  sortOrder?: number
  eventTypeCode?: string
}
