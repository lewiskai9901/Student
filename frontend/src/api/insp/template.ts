/**
 * V62 检查平台 - 模板（根分区）& 分区 & 字段 API
 *
 * 核心变更：
 * - "模板" = 根 TemplateSection（parentSectionId=null）
 * - 分区 API 改为 /v7/insp/sections（不再嵌套在 templateId 下）
 * - TemplateModuleRef 被 refSectionId 替代
 */
import { http } from '@/utils/request'
import type {
  TemplateSection,
  TemplateVersion,
  TemplateItem,
  CreateRootSectionRequest,
  UpdateRootSectionRequest,
  CreateSectionRequest,
  UpdateSectionRequest,
  CreateItemRequest,
  UpdateItemRequest,
  InspPageResult,
} from '@/types/insp/template'
import type { TemplateStatus } from '@/types/insp/enums'

const TEMPLATE_BASE = '/v7/insp/templates'
const SECTION_BASE = '/v7/insp/sections'

// ==================== 根分区（模板）CRUD ====================

export function listRootSections(params?: {
  page?: number
  size?: number
  status?: TemplateStatus
  catalogId?: number
  keyword?: string
}): Promise<InspPageResult<TemplateSection>> {
  return http.get<InspPageResult<TemplateSection>>(TEMPLATE_BASE, { params })
}

export function getRootSection(id: number): Promise<TemplateSection> {
  return http.get<TemplateSection>(`${TEMPLATE_BASE}/${id}`)
}

export function createRootSection(data: CreateRootSectionRequest): Promise<TemplateSection> {
  return http.post<TemplateSection>(TEMPLATE_BASE, data)
}

export function updateRootSection(id: number, data: UpdateRootSectionRequest): Promise<TemplateSection> {
  return http.put<TemplateSection>(`${TEMPLATE_BASE}/${id}`, data)
}

export function deleteRootSection(id: number): Promise<void> {
  return http.delete(`${TEMPLATE_BASE}/${id}`)
}

// ==================== 根分区操作 ====================

export function publishRootSection(id: number): Promise<TemplateVersion> {
  return http.post<TemplateVersion>(`${TEMPLATE_BASE}/${id}/publish`)
}

export function deprecateRootSection(id: number): Promise<void> {
  return http.post(`${TEMPLATE_BASE}/${id}/deprecate`)
}

export function archiveRootSection(id: number): Promise<void> {
  return http.post(`${TEMPLATE_BASE}/${id}/archive`)
}

export function duplicateRootSection(id: number): Promise<TemplateSection> {
  return http.post<TemplateSection>(`${TEMPLATE_BASE}/${id}/duplicate`)
}

export function exportRootSection(id: number): Promise<string> {
  return http.post<string>(`${TEMPLATE_BASE}/${id}/export`)
}

// ==================== 版本 ====================

export function getVersions(rootSectionId: number): Promise<TemplateVersion[]> {
  return http.get<TemplateVersion[]>(`${TEMPLATE_BASE}/${rootSectionId}/versions`)
}

export function getVersion(rootSectionId: number, version: number): Promise<TemplateVersion> {
  return http.get<TemplateVersion>(`${TEMPLATE_BASE}/${rootSectionId}/versions/${version}`)
}

// ==================== 分区 CRUD ====================

export function getSections(rootSectionId: number): Promise<TemplateSection[]> {
  return http.get<TemplateSection[]>(`${SECTION_BASE}/tree/${rootSectionId}`)
}

export function getSection(sectionId: number): Promise<TemplateSection> {
  return http.get<TemplateSection>(`${SECTION_BASE}/${sectionId}`)
}

export function getChildSections(parentSectionId: number): Promise<TemplateSection[]> {
  return http.get<TemplateSection[]>(`${SECTION_BASE}/children/${parentSectionId}`)
}

export function createSection(data: CreateSectionRequest & { rootSectionId: number }): Promise<TemplateSection> {
  return http.post<TemplateSection>(SECTION_BASE, data)
}

export function updateSection(sectionId: number, data: UpdateSectionRequest): Promise<TemplateSection> {
  return http.put<TemplateSection>(`${SECTION_BASE}/${sectionId}`, data)
}

export function deleteSection(sectionId: number): Promise<void> {
  return http.delete(`${SECTION_BASE}/${sectionId}`)
}

export function reorderSections(sectionIds: number[]): Promise<void> {
  return http.put(`${SECTION_BASE}/reorder`, sectionIds)
}

export function updateSectionScoringConfig(sectionId: number, scoringConfig: string): Promise<TemplateSection> {
  return http.put<TemplateSection>(`${SECTION_BASE}/${sectionId}/scoring-config`, { scoringConfig })
}

// ==================== 字段 ====================

export function getItems(sectionId: number): Promise<TemplateItem[]> {
  return http.get<TemplateItem[]>(`${SECTION_BASE}/${sectionId}/items`)
}

export function createItem(sectionId: number, data: CreateItemRequest): Promise<TemplateItem> {
  return http.post<TemplateItem>(`${SECTION_BASE}/${sectionId}/items`, data)
}

export function updateItem(itemId: number, data: UpdateItemRequest): Promise<TemplateItem> {
  return http.put<TemplateItem>(`/v7/insp/items/${itemId}`, data)
}

export function deleteItem(itemId: number): Promise<void> {
  return http.delete(`/v7/insp/items/${itemId}`)
}

export function reorderItems(sectionId: number, itemIds: number[]): Promise<void> {
  return http.put(`${SECTION_BASE}/${sectionId}/items/reorder`, itemIds)
}

// ==================== API 对象 ====================

export const inspTemplateApi = {
  // 根分区（模板）
  getList: listRootSections,
  getById: getRootSection,
  create: createRootSection,
  update: updateRootSection,
  delete: deleteRootSection,
  publish: publishRootSection,
  deprecate: deprecateRootSection,
  archive: archiveRootSection,
  duplicate: duplicateRootSection,
  export: exportRootSection,
  getVersions,
  getVersion,
  // 分区
  getSections,
  getSection,
  getChildSections,
  createSection,
  updateSection,
  deleteSection,
  reorderSections,
  updateSectionScoringConfig,
  // 字段
  getItems,
  createItem,
  updateItem,
  deleteItem,
  reorderItems,
}
