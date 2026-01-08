import { http } from '@/utils/request'
import type { Building, BuildingQueryParams, BuildingFormData } from '@/types/building'

// 楼宇管理API

/**
 * 获取楼宇列表(分页)
 */
export function getBuildingList(params: BuildingQueryParams) {
  return http.get<{
    records: Building[]
    total: number
  }>('/teaching/buildings', { params })
}

/**
 * 获取楼宇详情
 */
export function getBuildingDetail(id: number) {
  return http.get<Building>(`/teaching/buildings/${id}`)
}

/**
 * 创建楼宇
 */
export function createBuilding(data: BuildingFormData) {
  return http.post<Building>('/teaching/buildings', data)
}

/**
 * 更新楼宇信息
 */
export function updateBuilding(id: number, data: BuildingFormData) {
  return http.put<Building>(`/teaching/buildings/${id}`, data)
}

/**
 * 删除楼宇
 */
export function deleteBuilding(id: number, force: boolean = false) {
  return http.delete(`/teaching/buildings/${id}`, { params: { force } })
}

/**
 * 批量删除楼宇
 */
export function batchDeleteBuildings(ids: number[]) {
  return http.delete('/teaching/buildings/batch', { data: ids })
}

/**
 * 获取所有启用的楼宇
 */
export function getAllEnabledBuildings(buildingType?: number) {
  return http.get<Building[]>('/teaching/buildings/enabled', {
    params: { buildingType }
  })
}

/**
 * 检查楼号是否存在
 */
export function checkBuildingNoExists(buildingNo: string, excludeId?: number) {
  return http.get<boolean>('/teaching/buildings/exists', {
    params: { buildingNo, excludeId }
  })
}
