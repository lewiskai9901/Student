/**
 * V7 检查平台 - 合规映射类型
 */

// ==================== 合规标准 ====================

export interface ComplianceStandard {
  id: number
  tenantId?: number
  standardCode: string
  standardName: string
  issuingBody: string | null
  effectiveDate: string | null
  standardVersion: string | null
  description: string | null
  createdAt: string
  updatedAt: string | null
}

export interface CreateStandardRequest {
  standardCode: string
  standardName: string
  issuingBody?: string
  effectiveDate?: string
  version?: string
  description?: string
}

export interface UpdateStandardRequest {
  standardName?: string
  issuingBody?: string
  effectiveDate?: string
  version?: string
  description?: string
}

// ==================== 合规条款 ====================

export interface ComplianceClause {
  id: number
  tenantId?: number
  standardId: number
  clauseNumber: string
  clauseTitle: string
  clauseContent: string | null
  parentClauseId: number | null
  sortOrder: number
  createdAt: string
  children?: ComplianceClause[]
}

export interface CreateClauseRequest {
  clauseNumber: string
  clauseTitle: string
  clauseContent?: string
  parentClauseId?: number
  sortOrder?: number
}

export interface UpdateClauseRequest {
  clauseNumber?: string
  clauseTitle?: string
  clauseContent?: string
  parentClauseId?: number
  sortOrder?: number
}

// ==================== 检查项-条款映射 ====================

export type CoverageLevel = 'FULL' | 'PARTIAL' | 'REFERENCE'

export interface ItemComplianceMapping {
  id: number
  tenantId?: number
  templateItemId: number
  clauseId: number
  coverageLevel: CoverageLevel
  notes: string | null
  createdAt: string
}

export interface CreateMappingRequest {
  clauseId: number
  coverageLevel?: CoverageLevel
  notes?: string
}
