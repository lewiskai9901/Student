/**
 * 加分项配置 API
 */
import { http } from '@/utils/request'
import type { BonusItem, CreateBonusItemRequest } from '@/types/inspectionSession'

const BASE_URL = '/inspection/bonus-items'

export function listBonusItems(): Promise<BonusItem[]> {
  return http.get<BonusItem[]>(BASE_URL)
}

export function listBonusItemsByCategory(categoryId: number): Promise<BonusItem[]> {
  return http.get<BonusItem[]>(`${BASE_URL}/category/${categoryId}`)
}

export function getBonusItem(id: number): Promise<BonusItem> {
  return http.get<BonusItem>(`${BASE_URL}/${id}`)
}

export function createBonusItem(data: CreateBonusItemRequest): Promise<BonusItem> {
  return http.post<BonusItem>(BASE_URL, data)
}

export function updateBonusItem(id: number, data: CreateBonusItemRequest): Promise<BonusItem> {
  return http.put<BonusItem>(`${BASE_URL}/${id}`, data)
}

export function deleteBonusItem(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

export const bonusItemApi = {
  list: listBonusItems,
  listByCategory: listBonusItemsByCategory,
  get: getBonusItem,
  create: createBonusItem,
  update: updateBonusItem,
  delete: deleteBonusItem
}
