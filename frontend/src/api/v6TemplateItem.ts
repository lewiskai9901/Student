import request from '@/utils/request'

export interface TemplateScoreItem {
  id: number
  categoryId: number
  itemCode: string
  itemName: string
  description?: string
  scoringMode: 'DEDUCTION' | 'ADDITION' | 'FIXED' | 'PER_PERSON' | 'RANGE'
  score: number
  minScore?: number
  maxScore?: number
  perPersonScore?: number
  canLinkIndividual: boolean
  requiresPhoto: boolean
  requiresRemark: boolean
  checkPoints?: string
  sortOrder: number
  isEnabled: boolean
}

export interface TemplateCategory {
  id: number
  templateId: number
  categoryCode: string
  categoryName: string
  description?: string
  icon?: string
  color?: string
  weight: number
  maxScore: number
  sortOrder: number
  isEnabled: boolean
  items?: TemplateScoreItem[]
}

export interface CreateCategoryParams {
  templateId: number
  categoryCode: string
  categoryName: string
  description?: string
  icon?: string
  color?: string
  weight?: number
  sortOrder?: number
}

export interface UpdateCategoryParams {
  categoryName?: string
  description?: string
  icon?: string
  color?: string
  weight?: number
  sortOrder?: number
  isEnabled?: boolean
}

export interface CreateItemParams {
  categoryId: number
  itemCode: string
  itemName: string
  description?: string
  scoringMode?: string
  score?: number
  minScore?: number
  maxScore?: number
  perPersonScore?: number
  canLinkIndividual?: boolean
  requiresPhoto?: boolean
  requiresRemark?: boolean
  sortOrder?: number
}

export interface UpdateItemParams {
  itemName?: string
  description?: string
  scoringMode?: string
  score?: number
  minScore?: number
  maxScore?: number
  perPersonScore?: number
  canLinkIndividual?: boolean
  requiresPhoto?: boolean
  requiresRemark?: boolean
  sortOrder?: number
  isEnabled?: boolean
}

/**
 * 获取模板完整扣分项结构
 */
export function getTemplateItems(templateId: number) {
  return request<TemplateCategory[]>({
    url: `/v6/template-items/templates/${templateId}`,
    method: 'get'
  })
}

/**
 * 获取模板类别列表
 */
export function getCategories(templateId: number) {
  return request<TemplateCategory[]>({
    url: `/v6/template-items/templates/${templateId}/categories`,
    method: 'get'
  })
}

/**
 * 创建类别
 */
export function createCategory(data: CreateCategoryParams) {
  return request<TemplateCategory>({
    url: '/v6/template-items/categories',
    method: 'post',
    data
  })
}

/**
 * 更新类别
 */
export function updateCategory(categoryId: number, data: UpdateCategoryParams) {
  return request({
    url: `/v6/template-items/categories/${categoryId}`,
    method: 'put',
    data
  })
}

/**
 * 删除类别
 */
export function deleteCategory(categoryId: number) {
  return request({
    url: `/v6/template-items/categories/${categoryId}`,
    method: 'delete'
  })
}

/**
 * 获取类别下的扣分项
 */
export function getItemsByCategory(categoryId: number) {
  return request<TemplateScoreItem[]>({
    url: `/v6/template-items/categories/${categoryId}/items`,
    method: 'get'
  })
}

/**
 * 创建扣分项
 */
export function createItem(data: CreateItemParams) {
  return request<TemplateScoreItem>({
    url: '/v6/template-items/items',
    method: 'post',
    data
  })
}

/**
 * 更新扣分项
 */
export function updateItem(itemId: number, data: UpdateItemParams) {
  return request({
    url: `/v6/template-items/items/${itemId}`,
    method: 'put',
    data
  })
}

/**
 * 删除扣分项
 */
export function deleteItem(itemId: number) {
  return request({
    url: `/v6/template-items/items/${itemId}`,
    method: 'delete'
  })
}

export default {
  getTemplateItems,
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  getItemsByCategory,
  createItem,
  updateItem,
  deleteItem
}
