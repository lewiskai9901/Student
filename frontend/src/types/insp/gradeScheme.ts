export interface GradeScheme {
  id: number
  tenantId: number
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
  id?: number
  gradeSchemeId?: number
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
  sourceSchemeId: number
  displayName: string
}

export interface GradeDefinitionInput {
  code: string
  name: string
  minValue: number
  maxValue: number
  color?: string | null
  icon?: string | null
  sortOrder?: number
}
