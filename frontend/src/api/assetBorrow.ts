/**
 * 资产借用管理 API
 */
import request from '@/utils/request'
import type {
  AssetBorrow,
  CreateBorrowRequest,
  ReturnBorrowRequest,
  BorrowStatistics
} from '@/types/asset'
import type { PageResult } from '@/types/common'

const BASE_URL = '/asset-borrows'

/**
 * 创建借用/领用记录
 */
export function createBorrow(data: CreateBorrowRequest) {
  return request.post<number>(BASE_URL, data)
}

/**
 * 归还资产
 */
export function returnBorrow(id: number, data: ReturnBorrowRequest) {
  return request.post(`${BASE_URL}/${id}/return`, data)
}

/**
 * 取消借用
 */
export function cancelBorrow(id: number) {
  return request.post(`${BASE_URL}/${id}/cancel`)
}

/**
 * 获取借用详情
 */
export function getBorrow(id: number) {
  return request.get<AssetBorrow>(`${BASE_URL}/${id}`)
}

/**
 * 分页查询借用记录
 */
export function listBorrows(params: {
  borrowType?: number
  status?: number
  borrowerId?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<PageResult<AssetBorrow>>(BASE_URL, { params })
}

/**
 * 获取我的借用记录
 */
export function getMyBorrows() {
  return request.get<AssetBorrow[]>(`${BASE_URL}/my`)
}

/**
 * 获取资产的借用历史
 */
export function getAssetBorrowHistory(assetId: number) {
  return request.get<AssetBorrow[]>(`${BASE_URL}/asset/${assetId}`)
}

/**
 * 获取已逾期的借用记录
 */
export function getOverdueBorrows() {
  return request.get<AssetBorrow[]>(`${BASE_URL}/overdue`)
}

/**
 * 获取借用统计
 */
export function getBorrowStatistics() {
  return request.get<BorrowStatistics>(`${BASE_URL}/statistics`)
}

// 导出默认API对象
export const assetBorrowApi = {
  createBorrow,
  returnBorrow,
  cancelBorrow,
  getBorrow,
  listBorrows,
  getMyBorrows,
  getAssetBorrowHistory,
  getOverdueBorrows,
  getBorrowStatistics
}

export default assetBorrowApi
