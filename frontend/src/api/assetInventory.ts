import request from '@/utils/request'
import type {
  AssetInventory,
  CreateInventoryRequest,
  UpdateInventoryDetailRequest,
  InventoryStatistics
} from '@/types/asset'
import type { PageResult } from '@/types/common'

const BASE_URL = '/asset-inventories'

/**
 * 创建盘点任务
 */
export function createInventory(data: CreateInventoryRequest): Promise<number> {
  return request.post(BASE_URL, data)
}

/**
 * 分页查询盘点任务
 */
export function getInventories(params: {
  status?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}): Promise<PageResult<AssetInventory>> {
  return request.get(BASE_URL, { params })
}

/**
 * 获取盘点任务详情
 */
export function getInventory(id: number | string): Promise<AssetInventory> {
  return request.get(`${BASE_URL}/${id}`)
}

/**
 * 更新盘点明细
 */
export function updateInventoryDetail(
  inventoryId: number | string,
  detailId: number | string,
  data: UpdateInventoryDetailRequest
): Promise<void> {
  return request.put(`${BASE_URL}/${inventoryId}/details/${detailId}`, data)
}

/**
 * 完成盘点
 */
export function completeInventory(id: number | string): Promise<void> {
  return request.post(`${BASE_URL}/${id}/complete`)
}

/**
 * 取消盘点
 */
export function cancelInventory(id: number | string): Promise<void> {
  return request.post(`${BASE_URL}/${id}/cancel`)
}

/**
 * 获取盘点统计
 */
export function getInventoryStatistics(): Promise<InventoryStatistics> {
  return request.get(`${BASE_URL}/statistics`)
}
