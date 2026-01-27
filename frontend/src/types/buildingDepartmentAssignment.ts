/**
 * 宿舍楼-组织单元分配类型定义
 */

// 宿舍楼-组织单元分配实体
export interface BuildingOrgUnitAssignment {
  id: number
  buildingId: number
  orgUnitId: number
  floorStart?: number | null
  floorEnd?: number | null
  allocatedRooms?: number | null
  allocatedBeds?: number | null
  priority: number
  status: number
  notes?: string
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  updatedBy?: number
  // 关联字段
  buildingName?: string
  orgUnitName?: string
  floorRangeDesc?: string
}

// 查询参数
export interface BuildingOrgUnitAssignmentQueryParams {
  pageNum?: number
  pageSize?: number
  buildingId?: number
  orgUnitId?: number
  status?: number
}

// 创建请求
export interface BuildingOrgUnitAssignmentCreateRequest {
  buildingId: number
  orgUnitId: number
  floorStart?: number | null
  floorEnd?: number | null
  allocatedRooms?: number | null
  allocatedBeds?: number | null
  priority?: number
  notes?: string
}

// 更新请求
export interface BuildingOrgUnitAssignmentUpdateRequest {
  id: number
  floorStart?: number | null
  floorEnd?: number | null
  allocatedRooms?: number | null
  allocatedBeds?: number | null
  priority?: number
  status?: number
  notes?: string
}
