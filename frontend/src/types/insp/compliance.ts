import type { LongId } from '@/types/common'

/**
 * 检查平台 - 合规映射类型
 */

// ==================== 合规标准 ====================

export interface ComplianceStandard {
  id: LongId
  tenantId?: LongId
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
  id: LongId
  tenantId?: LongId
  standardId: LongId
  clauseNumber: string
  clauseTitle: string
  clauseContent: string | null
  parentClauseId: LongId | null
  sortOrder: number
  createdAt: string
  children?: ComplianceClause[]
}

export interface CreateClauseRequest {
  clauseNumber: string
  clauseTitle: string
  clauseContent?: string
  parentClauseId?: LongId
  sortOrder?: number
}

export interface UpdateClauseRequest {
  clauseNumber?: string
  clauseTitle?: string
  clauseContent?: string
  parentClauseId?: LongId
  sortOrder?: number
}

// ==================== 检查项-条款映射 ====================

export type CoverageLevel = 'FULL' | 'PARTIAL' | 'REFERENCE'

export interface ItemComplianceMapping {
  id: LongId
  tenantId?: LongId
  templateItemId: LongId
  clauseId: LongId
  coverageLevel: CoverageLevel
  notes: string | null
  createdAt: string
}

export interface CreateMappingRequest {
  clauseId: LongId
  coverageLevel?: CoverageLevel
  notes?: string
}
