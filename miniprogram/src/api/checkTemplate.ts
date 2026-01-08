/**
 * 检查模板管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 检查模板
export interface CheckTemplate {
  id: number | string
  templateCode: string
  templateName: string
  templateType?: number    // 1:日常检查 2:专项检查 3:临时检查
  templateTypeName?: string
  description?: string
  categoryCount?: number   // 类别数量
  itemCount?: number       // 扣分项数量
  totalScore?: number      // 满分
  status: number
  statusText?: string
  categories?: TemplateCategory[]
  createdAt?: string
  updatedAt?: string
}

// 模板类别
export interface TemplateCategory {
  id: number | string
  templateId: number
  categoryCode: string
  categoryName: string
  parentId?: number
  parentName?: string
  sort: number
  weight?: number          // 权重
  maxScore?: number        // 类别最大扣分
  itemCount?: number
  items?: DeductionItem[]
  children?: TemplateCategory[]
}

// 扣分项
export interface DeductionItem {
  id: number | string
  categoryId: number
  categoryName?: string
  itemCode: string
  itemName: string
  description?: string
  deductionMode: number    // 1:固定扣分 2:按人数扣分 3:分数区间
  deductionModeName?: string
  baseScore: number        // 基础扣分
  maxScore?: number        // 最大扣分
  minScore?: number        // 最小扣分
  scoreStep?: number       // 分数步长
  perPersonScore?: number  // 每人扣分
  scoreRanges?: ScoreRange[]
  sort: number
  status: number
  statusText?: string
}

// 分数区间
export interface ScoreRange {
  id: number | string
  itemId: number
  minValue: number
  maxValue: number
  score: number
  description?: string
}

// 查询参数
export interface TemplateQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  templateName?: string
  templateType?: number
  status?: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 获取模板列表
 */
export function getTemplateList(params: TemplateQueryParams = {}) {
  return get<PageResult<CheckTemplate>>('/quantification/templates', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 获取所有模板(用于下拉选择)
 */
export function getAllTemplates() {
  return get<CheckTemplate[]>('/quantification/templates/all')
}

/**
 * 获取启用的模板列表
 */
export function getEnabledTemplates() {
  return get<CheckTemplate[]>('/quantification/templates/enabled')
}

/**
 * 获取模板详情
 */
export function getTemplateById(id: number | string) {
  return get<CheckTemplate>(`/quantification/templates/${id}`)
}

/**
 * 获取模板类别列表
 */
export function getTemplateCategories(templateId: number | string) {
  return get<TemplateCategory[]>(`/quantification/templates/${templateId}/categories`)
}

/**
 * 获取类别的扣分项
 */
export function getCategoryItems(categoryId: number | string) {
  return get<DeductionItem[]>(`/quantification/categories/${categoryId}/items`)
}

/**
 * 获取模板完整结构(含类别和扣分项)
 */
export function getTemplateWithDetails(id: number | string) {
  return get<CheckTemplate>(`/quantification/templates/${id}/details`)
}

/**
 * 创建模板
 */
export function createTemplate(data: Partial<CheckTemplate>) {
  return post<CheckTemplate>('/quantification/templates', data)
}

/**
 * 更新模板
 */
export function updateTemplate(id: number | string, data: Partial<CheckTemplate>) {
  return put<CheckTemplate>(`/quantification/templates/${id}`, data)
}

/**
 * 删除模板
 */
export function deleteTemplate(id: number | string) {
  return del<void>(`/quantification/templates/${id}`)
}

/**
 * 更新模板状态
 */
export function updateTemplateStatus(id: number | string, status: number) {
  return put<void>(`/quantification/templates/${id}/status`, { status })
}

/**
 * 复制模板
 */
export function copyTemplate(id: number | string, newName: string) {
  return post<CheckTemplate>(`/quantification/templates/${id}/copy`, { templateName: newName })
}
