import { http } from '@/utils/request'
import type {
  BuildingDepartmentAssignment,
  BuildingDepartmentAssignmentQueryParams,
  BuildingDepartmentAssignmentCreateRequest,
  BuildingDepartmentAssignmentUpdateRequest
} from '@/types/buildingDepartmentAssignment'

// 宿舍楼-院系分配API

/**
 * 分页查询分配列表
 */
export function getBuildingDepartmentAssignmentList(params: BuildingDepartmentAssignmentQueryParams) {
  return http.get<{
    records: BuildingDepartmentAssignment[]
    total: number
  }>('/dormitory/building-assignments', { params })
}

/**
 * 获取分配详情
 */
export function getBuildingDepartmentAssignmentDetail(id: number) {
  return http.get<BuildingDepartmentAssignment>(`/dormitory/building-assignments/${id}`)
}

/**
 * 根据宿舍楼ID查询分配的院系
 */
export function getAssignmentsByBuildingId(buildingId: number) {
  return http.get<BuildingDepartmentAssignment[]>(`/dormitory/building-assignments/building/${buildingId}`)
}

/**
 * 根据院系ID查询分配的宿舍楼
 */
export function getAssignmentsByDepartmentId(departmentId: number) {
  return http.get<BuildingDepartmentAssignment[]>(`/dormitory/building-assignments/department/${departmentId}`)
}

/**
 * 根据宿舍楼ID和楼层查询分配的院系
 */
export function getAssignmentsByBuildingAndFloor(buildingId: number, floor: number) {
  return http.get<BuildingDepartmentAssignment[]>(`/dormitory/building-assignments/building/${buildingId}/floor/${floor}`)
}

/**
 * 创建分配
 */
export function createBuildingDepartmentAssignment(data: BuildingDepartmentAssignmentCreateRequest) {
  return http.post<BuildingDepartmentAssignment>('/dormitory/building-assignments', data)
}

/**
 * 更新分配
 */
export function updateBuildingDepartmentAssignment(data: BuildingDepartmentAssignmentUpdateRequest) {
  return http.put<BuildingDepartmentAssignment>('/dormitory/building-assignments', data)
}

/**
 * 删除分配
 */
export function deleteBuildingDepartmentAssignment(id: number) {
  return http.delete<void>(`/dormitory/building-assignments/${id}`)
}

/**
 * 批量删除分配
 */
export function batchDeleteBuildingDepartmentAssignments(ids: number[]) {
  return http.delete<void>('/dormitory/building-assignments/batch', { data: ids })
}

/**
 * 启用分配
 */
export function enableBuildingDepartmentAssignment(id: number) {
  return http.put<void>(`/dormitory/building-assignments/${id}/enable`)
}

/**
 * 禁用分配
 */
export function disableBuildingDepartmentAssignment(id: number) {
  return http.put<void>(`/dormitory/building-assignments/${id}/disable`)
}

/**
 * 检查楼层冲突
 */
export function checkFloorConflict(params: {
  buildingId: number
  departmentId: number
  floorStart?: number
  floorEnd?: number
  excludeId?: number
}) {
  return http.get<boolean>('/dormitory/building-assignments/check-conflict', { params })
}
