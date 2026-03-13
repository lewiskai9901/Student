/**
 * V10 空间分类 API
 */
import request from '@/utils/request'
import type {
  SpaceCategoryDTO,
  CreateSpaceCategoryRequest,
  UpdateSpaceCategoryRequest,
  SpaceCategoryLevel
} from '@/types/spaceCategory'

const BASE_URL = '/space-categories'

/**
 * 获取所有空间分类
 */
export function getAllCategories() {
  return request.get<SpaceCategoryDTO[]>(BASE_URL)
}

/**
 * 获取所有启用的空间分类
 */
export function getEnabledCategories() {
  return request.get<SpaceCategoryDTO[]>(`${BASE_URL}/enabled`)
}

/**
 * 按层级获取空间分类
 */
export function getCategoriesByLevel(level: SpaceCategoryLevel) {
  return request.get<SpaceCategoryDTO[]>(`${BASE_URL}/level/${level}`)
}

/**
 * 按层级获取启用的空间分类
 */
export function getEnabledCategoriesByLevel(level: SpaceCategoryLevel) {
  return request.get<SpaceCategoryDTO[]>(`${BASE_URL}/level/${level}/enabled`)
}

/**
 * 获取楼栋分类
 */
export function getBuildingCategories() {
  return request.get<SpaceCategoryDTO[]>(`${BASE_URL}/building`)
}

/**
 * 获取房间分类
 */
export function getRoomCategories() {
  return request.get<SpaceCategoryDTO[]>(`${BASE_URL}/room`)
}

/**
 * 获取空间分类详情
 */
export function getCategoryById(id: number) {
  return request.get<SpaceCategoryDTO>(`${BASE_URL}/${id}`)
}

/**
 * 创建空间分类
 */
export function createCategory(data: CreateSpaceCategoryRequest) {
  return request.post<SpaceCategoryDTO>(BASE_URL, data)
}

/**
 * 更新空间分类
 */
export function updateCategory(id: number, data: UpdateSpaceCategoryRequest) {
  return request.put<SpaceCategoryDTO>(`${BASE_URL}/${id}`, data)
}

/**
 * 启用空间分类
 */
export function enableCategory(id: number) {
  return request.post(`${BASE_URL}/${id}/enable`)
}

/**
 * 停用空间分类
 */
export function disableCategory(id: number) {
  return request.post(`${BASE_URL}/${id}/disable`)
}

/**
 * 删除空间分类
 */
export function deleteCategory(id: number) {
  return request.delete(`${BASE_URL}/${id}`)
}

// 导出所有API
export default {
  getAllCategories,
  getEnabledCategories,
  getCategoriesByLevel,
  getEnabledCategoriesByLevel,
  getBuildingCategories,
  getRoomCategories,
  getCategoryById,
  createCategory,
  updateCategory,
  enableCategory,
  disableCategory,
  deleteCategory
}
