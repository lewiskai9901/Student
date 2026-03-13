/**
 * V7 检查平台 - 模板引擎类型定义
 */
import type { ItemType, TargetType, TemplateStatus } from './enums'

// ==================== 模板分类 ====================

export interface TemplateCatalog {
  id: number
  parentId: number | null
  catalogCode: string
  catalogName: string
  description: string | null
  icon: string | null
  sortOrder: number
  isEnabled: boolean
  createdAt: string
  updatedAt: string
}

export interface TemplateCatalogTreeNode extends TemplateCatalog {
  children?: TemplateCatalogTreeNode[]
}

export interface CreateCatalogRequest {
  catalogCode: string
  catalogName: string
  parentId?: number | null
  description?: string
  icon?: string
  sortOrder?: number
}

export interface UpdateCatalogRequest {
  catalogName?: string
  description?: string
  parentId?: number | null
  icon?: string
  sortOrder?: number
  isEnabled?: boolean
}

// ==================== 检查模板 ====================

export interface InspTemplate {
  id: number
  tenantId: number
  templateCode: string
  templateName: string
  description: string | null
  catalogId: number | null
  catalogName?: string
  tags: string | null
  targetType: TargetType
  latestVersion: number
  status: TemplateStatus
  isDefault: boolean
  useCount: number
  lastUsedAt: string | null
  createdBy: number | null
  updatedBy: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateTemplateRequest {
  templateName: string
  description?: string
  catalogId?: number | null
  tags?: string
  targetType?: TargetType
}

export interface UpdateTemplateRequest {
  templateName?: string
  description?: string
  catalogId?: number | null
  tags?: string
  targetType?: TargetType
}

export interface TemplateQueryParams {
  page?: number
  size?: number
  status?: TemplateStatus
  catalogId?: number
  keyword?: string
}

// ==================== 模板版本 ====================

export interface TemplateVersion {
  id: number
  templateId: number
  version: number
  structureSnapshot: string
  scoringProfileSnapshot: string | null
  publishedBy: number | null
  createdAt: string
}

// ==================== 模板分区 ====================

export interface TemplateSection {
  id: number
  templateId: number
  sectionCode: string
  sectionName: string
  sortOrder: number
  weight: number
  isRepeatable: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateSectionRequest {
  sectionCode: string
  sectionName: string
  weight?: number
  isRepeatable?: boolean
  sortOrder?: number
}

export interface UpdateSectionRequest {
  sectionName?: string
  weight?: number
  isRepeatable?: boolean
}

// ==================== 模板字段 ====================

export interface TemplateItem {
  id: number
  sectionId: number
  itemCode: string
  itemName: string
  description: string | null
  itemType: ItemType
  config: string | null
  validationRules: string | null
  responseSetId: number | null
  scoringConfig: string | null
  dimensionId: number | null
  helpContent: string | null
  isRequired: boolean
  isScored: boolean
  requireEvidence: boolean
  itemWeight: number
  sortOrder: number
  conditionLogic: string | null
  libraryItemId: number | null
  syncWithLibrary: boolean
  visibilityLogic: string | null
  scoringLogic: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateItemRequest {
  itemCode: string
  itemName: string
  description?: string
  itemType: ItemType
  config?: string
  validationRules?: string
  responseSetId?: number | null
  scoringConfig?: string
  dimensionId?: number | null
  helpContent?: string
  isRequired?: boolean
  isScored?: boolean
  requireEvidence?: boolean
  itemWeight?: number
  conditionLogic?: string
  sortOrder?: number
}

export interface UpdateItemRequest {
  itemName?: string
  description?: string
  itemType?: ItemType
  config?: string
  validationRules?: string
  responseSetId?: number | null
  scoringConfig?: string
  dimensionId?: number | null
  helpContent?: string
  isRequired?: boolean
  isScored?: boolean
  requireEvidence?: boolean
  itemWeight?: number
  conditionLogic?: string
}

// ==================== 选项集 ====================

export interface ResponseSet {
  id: number
  tenantId: number
  setCode: string
  setName: string
  isGlobal: boolean
  isEnabled: boolean
  createdBy: number | null
  createdAt: string
  updatedAt: string
}

export interface ResponseSetOption {
  id: number
  responseSetId: number
  optionValue: string
  optionLabel: string
  optionColor: string | null
  score: number | null
  isFlagged: boolean
  sortOrder: number
}

export interface CreateResponseSetRequest {
  setCode: string
  setName: string
  isGlobal?: boolean
}

export interface UpdateResponseSetRequest {
  setName?: string
  isGlobal?: boolean
  isEnabled?: boolean
}

export interface CreateOptionRequest {
  optionValue: string
  optionLabel: string
  optionColor?: string
  score?: number
  isFlagged?: boolean
  sortOrder?: number
}

export interface UpdateOptionRequest {
  optionLabel?: string
  optionColor?: string
  score?: number
  isFlagged?: boolean
  sortOrder?: number
}

// ==================== 模板模块引用 ====================

export interface TemplateModuleRef {
  id: number
  compositeTemplateId: number
  moduleTemplateId: number
  sortOrder: number
  weight: number
  overrideConfig: string | null
  createdAt: string
}

export interface AddModuleRefRequest {
  moduleTemplateId: number
  sortOrder?: number
  weight?: number
}

export interface UpdateModuleRefRequest {
  weight?: number
  overrideConfig?: string
}

// ==================== 检查项库 ====================

export interface LibraryItem {
  id: number
  itemCode: string
  itemName: string
  description: string | null
  itemType: ItemType
  category: string | null
  tags: string | null
  defaultConfig: string | null
  defaultValidationRules: string | null
  defaultScoringConfig: string | null
  defaultHelpContent: string | null
  usageCount: number
  isStandard: boolean
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface CreateLibraryItemRequest {
  itemCode: string
  itemName: string
  itemType: ItemType
  description?: string
  category?: string
  tags?: string
  defaultConfig?: string
  defaultValidationRules?: string
  defaultScoringConfig?: string
  defaultHelpContent?: string
  isStandard?: boolean
}

export interface UpdateLibraryItemRequest {
  itemName: string
  itemType: ItemType
  description?: string
  category?: string
  tags?: string
  defaultConfig?: string
  defaultValidationRules?: string
  defaultScoringConfig?: string
  defaultHelpContent?: string
  isStandard?: boolean
}

// ==================== 分页结果 ====================

export interface InspPageResult<T> {
  records: T[]
  total: number
}
