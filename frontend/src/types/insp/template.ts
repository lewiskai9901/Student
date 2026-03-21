/**
 * V62 检查平台 - 统一分区模型类型定义
 *
 * 核心变更：InspTemplate 被根 TemplateSection 替代。
 * TemplateModuleRef 被 refSectionId 替代。
 * targetType 下沉到一级分区。
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

// ==================== 模板分区（统一模型） ====================
//
// 根分区 (parentSectionId=null, templateId=null) 即为"模板"。
// 一级分区携带 targetType。
// refSectionId 替代旧的 refTemplateId / TemplateModuleRef。

export interface TemplateSection {
  id: number
  templateId: number | null
  parentSectionId: number | null
  refSectionId: number | null
  sectionCode: string
  sectionName: string
  targetType: TargetType | null
  targetSourceMode: 'INDEPENDENT' | 'PARENT_ASSOCIATED' | null
  targetTypeFilter: string | null
  description: string | null
  tags: string | null
  catalogId: number | null
  status: TemplateStatus
  latestVersion: number
  sortOrder: number
  weight: number
  isRepeatable: boolean
  scoringConfig: string | null
  inspectionMode: 'SNAPSHOT' | 'CONTINUOUS'
  continuousStart: string | null
  continuousEnd: string | null
  createdAt: string
  updatedAt: string
}

export interface TemplateQueryParams {
  page?: number
  size?: number
  status?: TemplateStatus
  catalogId?: number
  keyword?: string
}

// ==================== 根分区（模板）请求 ====================

export interface CreateRootSectionRequest {
  name: string
  description?: string
  catalogId?: number | null
  tags?: string
}

export interface UpdateRootSectionRequest {
  name?: string
  description?: string
  catalogId?: number | null
  tags?: string
}

// ==================== 子分区请求 ====================

export interface CreateSectionRequest {
  sectionCode: string
  sectionName: string
  parentSectionId?: number | null
  targetType?: string
  weight?: number
  isRepeatable?: boolean
  sortOrder?: number
}

export interface UpdateSectionRequest {
  sectionName?: string
  targetType?: string
  targetSourceMode?: string
  targetTypeFilter?: string
  weight?: number
  isRepeatable?: boolean
}

export interface CreateRefSectionRequest {
  parentSectionId?: number | null
  refSectionId: number
  weight?: number
  sortOrder?: number
}

// ==================== 评分配置 ====================

export interface ScoringConfig {
  aggregation: 'WEIGHTED_AVERAGE' | 'SUM' | 'AVERAGE' | 'MAX' | 'MIN'
  maxScore: number
  minScore: number
  gradeBands: GradeBand[]
}

export interface GradeBand {
  grade: string
  label: string
  minScore: number
  maxScore: number
  color: string
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

// ==================== 检查计划 ====================

export interface InspectionPlan {
  id: number
  projectId: number
  planName: string
  rootSectionId: number | null  // V66: 该计划绑定的模板根分区ID
  sectionIds: string  // JSON
  scheduleMode: 'REGULAR' | 'ON_DEMAND'
  cycleType: string
  frequency: number
  scheduleDays: string | null
  timeSlots: string | null
  skipHolidays: boolean
  isEnabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface CreatePlanRequest {
  projectId: number
  planName: string
  rootSectionId?: number | null  // V66: 计划绑定的模板（可选，null 则从项目继承）
  sectionIds: string
  scheduleMode?: string
  cycleType?: string
  frequency?: number
  scheduleDays?: string
  timeSlots?: string
  skipHolidays?: boolean
}

export interface UpdatePlanRequest {
  planName?: string
  rootSectionId?: number | null  // V66: 可选，变更计划绑定的模板
  sectionIds?: string
  scheduleMode?: string
  cycleType?: string
  frequency?: number
  scheduleDays?: string
  timeSlots?: string
  skipHolidays?: boolean
}

// ==================== 评级维度 ====================

export interface RatingDimension {
  id: number
  projectId: number
  dimensionName: string
  sectionIds: string
  aggregation: string
  gradeBands: string | null
  awardName: string | null
  rankingEnabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface RatingResult {
  id: number
  dimensionId: number
  targetId: number
  targetName: string
  targetType: string
  cycleDate: string
  score: number
  grade: string | null
  rankNo: number
  createdAt: string
}

// ==================== 违纪记录 ====================

export interface ViolationRecord {
  id: number
  submissionId: number
  submissionDetailId: number
  sectionId: number | null
  itemId: number | null
  userId: number
  userName: string
  classInfo: string | null
  occurredAt: string
  severity: 'MINOR' | 'MODERATE' | 'SEVERE'
  description: string | null
  evidenceUrls: string | null
  score: number | null
  createdBy: number | null
  createdAt: string
}

// ==================== 分页结果 ====================

export interface InspPageResult<T> {
  records: T[]
  total: number
}
