/**
 * 固定资产管理 API
 */
import { http as request } from '@/utils/request'
import type {
  Asset,
  AssetCategory,
  AssetHistory,
  AssetMaintenance,
  AssetStatistics,
  AssetQueryCriteria,
  CreateAssetRequest,
  UpdateAssetRequest,
  TransferAssetRequest,
  ScrapAssetRequest,
  CreateCategoryRequest,
  CreateMaintenanceRequest,
  CompleteMaintenanceRequest,
  BatchCreateAssetRequest,
  BatchCreateResult,
  BatchTransferAssetRequest,
  BatchTransferResult,
  ImportResult
} from '@/types/asset'
import type { PageResult } from '@/types/common'

const BASE_URL = '/assets'
const CATEGORY_URL = '/asset/categories'

// ============ 资产管理 ============

/**
 * 获取资产列表
 */
export function getAssetList(params: AssetQueryCriteria) {
  return request.get<PageResult<Asset>>(BASE_URL, { params })
}

/**
 * 获取资产详情
 */
export function getAsset(id: number | string) {
  return request.get<Asset>(`${BASE_URL}/${id}`)
}

/**
 * 根据位置查询资产
 */
export function getAssetsByLocation(locationType: string, locationId: number | string) {
  return request.get<Asset[]>(`${BASE_URL}/by-location`, {
    params: { locationType, locationId }
  })
}

/**
 * 创建资产
 */
export function createAsset(data: CreateAssetRequest) {
  return request.post<number>(BASE_URL, data)
}

/**
 * 批量入库资产
 */
export function batchCreateAssets(data: BatchCreateAssetRequest) {
  return request.post<BatchCreateResult>(`${BASE_URL}/batch`, data)
}

/**
 * 更新资产
 */
export function updateAsset(id: number | string, data: UpdateAssetRequest) {
  return request.put(`${BASE_URL}/${id}`, data)
}

/**
 * 删除资产
 */
export function deleteAsset(id: number | string) {
  return request.delete(`${BASE_URL}/${id}`)
}

/**
 * 调拨资产
 */
export function transferAsset(id: number | string, data: TransferAssetRequest) {
  return request.post(`${BASE_URL}/${id}/transfer`, data)
}

/**
 * 批量调拨资产
 */
export function batchTransferAssets(data: BatchTransferAssetRequest) {
  return request.post<BatchTransferResult>(`${BASE_URL}/batch-transfer`, data)
}

/**
 * 报废资产
 */
export function scrapAsset(id: number | string, data?: ScrapAssetRequest) {
  return request.post(`${BASE_URL}/${id}/scrap`, data || {})
}

/**
 * 获取资产变更历史
 */
export function getAssetHistory(id: number | string) {
  return request.get<AssetHistory[]>(`${BASE_URL}/${id}/history`)
}

/**
 * 获取资产统计
 */
export function getAssetStatistics() {
  return request.get<AssetStatistics>(`${BASE_URL}/statistics`)
}

// ============ 维修管理 ============

/**
 * 获取资产维修记录
 */
export function getAssetMaintenanceRecords(id: number | string) {
  return request.get<AssetMaintenance[]>(`${BASE_URL}/${id}/maintenance`)
}

/**
 * 创建维修记录
 */
export function createMaintenance(assetId: number | string, data: CreateMaintenanceRequest) {
  return request.post<number>(`${BASE_URL}/${assetId}/maintenance`, data)
}

/**
 * 完成维修
 */
export function completeMaintenance(maintenanceId: number | string, data: CompleteMaintenanceRequest) {
  return request.post(`${BASE_URL}/maintenance/${maintenanceId}/complete`, data)
}

// ============ Excel导入导出 ============

const EXCEL_URL = '/assets/excel'

/**
 * 下载导入模板
 */
export function downloadImportTemplate() {
  return request.get(`${EXCEL_URL}/template`, {
    responseType: 'blob'
  })
}

/**
 * 导入资产
 */
export function importAssets(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ImportResult>(`${EXCEL_URL}/import`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 导出资产列表
 */
export function exportAssets(params?: AssetQueryCriteria) {
  return request.get(`${EXCEL_URL}/export`, {
    params,
    responseType: 'blob'
  })
}

// ============ 分类管理 ============

/**
 * 获取分类树
 */
export function getCategoryTree() {
  return request.get<AssetCategory[]>(`${CATEGORY_URL}/tree`)
}

/**
 * 获取所有分类(平铺)
 */
export function getAllCategories() {
  return request.get<AssetCategory[]>(CATEGORY_URL)
}

/**
 * 获取分类详情
 */
export function getCategory(id: number | string) {
  return request.get<AssetCategory>(`${CATEGORY_URL}/${id}`)
}

/**
 * 创建分类
 */
export function createCategory(data: CreateCategoryRequest) {
  return request.post<number>(CATEGORY_URL, data)
}

/**
 * 更新分类
 */
export function updateCategory(id: number | string, data: CreateCategoryRequest) {
  return request.put(`${CATEGORY_URL}/${id}`, data)
}

/**
 * 删除分类
 */
export function deleteCategory(id: number | string) {
  return request.delete(`${CATEGORY_URL}/${id}`)
}

// 导出默认API对象
export const assetApi = {
  // 资产
  getAssetList,
  getAsset,
  getAssetsByLocation,
  createAsset,
  batchCreateAssets,
  updateAsset,
  deleteAsset,
  transferAsset,
  batchTransferAssets,
  scrapAsset,
  getAssetHistory,
  getAssetStatistics,
  // 维修
  getAssetMaintenanceRecords,
  createMaintenance,
  completeMaintenance,
  // Excel导入导出
  downloadImportTemplate,
  importAssets,
  exportAssets,
  // 分类
  getCategoryTree,
  getAllCategories,
  getCategory,
  createCategory,
  updateCategory,
  deleteCategory
}

export default assetApi
