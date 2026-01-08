import { http } from '@/utils/request'
import type {
  BuildingDormitory,
  BuildingDormitoryQueryParams,
  BuildingDormitoryFormData,
  ManagerAssignmentData
} from '@/types/buildingDormitory'

// 宿舍楼管理API

/**
 * 分页查询宿舍楼列表
 */
export function getDormitoryBuildingList(params: BuildingDormitoryQueryParams) {
  return http.get<{
    records: BuildingDormitory[]
    total: number
  }>('/dormitory/buildings', { params })
}

/**
 * 获取宿舍楼详情
 */
export function getDormitoryBuildingDetail(id: number) {
  return http.get<BuildingDormitory>(`/dormitory/buildings/${id}`)
}

/**
 * 根据楼宇ID获取宿舍楼信息
 */
export function getDormitoryBuildingByBuildingId(buildingId: number) {
  return http.get<BuildingDormitory>(`/dormitory/buildings/building/${buildingId}`)
}

/**
 * 更新宿舍楼信息
 */
export function updateDormitoryBuilding(id: number, data: BuildingDormitoryFormData) {
  return http.put<BuildingDormitory>(`/dormitory/buildings/${id}`, data)
}

/**
 * 分配管理员
 */
export function assignManagers(buildingId: number, managerIds: number[]) {
  return http.post<void>(`/dormitory/buildings/${buildingId}/managers`, managerIds)
}

/**
 * 移除管理员
 */
export function removeManager(buildingId: number, userId: number) {
  return http.delete<void>(`/dormitory/buildings/${buildingId}/managers/${userId}`)
}

/**
 * 检查用户是否有宿舍管理权限
 */
export function checkUserPermission(userId: number) {
  return http.get<boolean>(`/dormitory/buildings/check-permission/${userId}`)
}

/**
 * 获取有宿舍管理权限的用户列表
 */
export function getUsersWithDormitoryPermission() {
  return http.get<Array<{
    id: number
    username: string
    realName: string
    departmentName?: string
  }>>('/dormitory/buildings/available-managers')
}
