/**
 * V7 检查平台 - 合规映射 API
 */
import { http } from '@/utils/request'
import type {
  ComplianceStandard,
  CreateStandardRequest,
  UpdateStandardRequest,
  ComplianceClause,
  CreateClauseRequest,
  UpdateClauseRequest,
  ItemComplianceMapping,
  CreateMappingRequest,
} from '@/types/insp/compliance'

const BASE = '/v7/insp/compliance-standards'

// ==================== Standards ====================

export function getStandards(): Promise<ComplianceStandard[]> {
  return http.get<ComplianceStandard[]>(BASE)
}

export function getStandard(id: number): Promise<ComplianceStandard> {
  return http.get<ComplianceStandard>(`${BASE}/${id}`)
}

export function createStandard(data: CreateStandardRequest): Promise<ComplianceStandard> {
  return http.post<ComplianceStandard>(BASE, data)
}

export function updateStandard(id: number, data: UpdateStandardRequest): Promise<ComplianceStandard> {
  return http.put<ComplianceStandard>(`${BASE}/${id}`, data)
}

export function deleteStandard(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== Clauses ====================

export function getClauses(standardId: number): Promise<ComplianceClause[]> {
  return http.get<ComplianceClause[]>(`${BASE}/${standardId}/clauses`)
}

export function createClause(standardId: number, data: CreateClauseRequest): Promise<ComplianceClause> {
  return http.post<ComplianceClause>(`${BASE}/${standardId}/clauses`, data)
}

export function updateClause(standardId: number, clauseId: number, data: UpdateClauseRequest): Promise<ComplianceClause> {
  return http.put<ComplianceClause>(`${BASE}/${standardId}/clauses/${clauseId}`, data)
}

export function deleteClause(standardId: number, clauseId: number): Promise<void> {
  return http.delete(`${BASE}/${standardId}/clauses/${clauseId}`)
}

// ==================== Item-Clause Mappings ====================

export function getMappings(itemId: number): Promise<ItemComplianceMapping[]> {
  return http.get<ItemComplianceMapping[]>(`/v7/insp/template-items/${itemId}/compliance-mappings`)
}

export function createMapping(itemId: number, data: CreateMappingRequest): Promise<ItemComplianceMapping> {
  return http.post<ItemComplianceMapping>(`/v7/insp/template-items/${itemId}/compliance-mappings`, data)
}

export function deleteMapping(itemId: number, mappingId: number): Promise<void> {
  return http.delete(`/v7/insp/template-items/${itemId}/compliance-mappings/${mappingId}`)
}

export const inspComplianceApi = {
  getStandards,
  getStandard,
  createStandard,
  updateStandard,
  deleteStandard,
  getClauses,
  createClause,
  updateClause,
  deleteClause,
  getMappings,
  createMapping,
  deleteMapping,
}
