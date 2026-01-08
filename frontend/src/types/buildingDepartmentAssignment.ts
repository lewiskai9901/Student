/**
 * 宿舍楼-院系分配类型定义
 */

// 宿舍楼-院系分配实体
export interface BuildingDepartmentAssignment {
  id: number
  buildingId: number
  departmentId: number
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
  departmentName?: string
  floorRangeDesc?: string
}

// 查询参数
export interface BuildingDepartmentAssignmentQueryParams {
  pageNum?: number
  pageSize?: number
  buildingId?: number
  departmentId?: number
  status?: number
}

// 创建请求
export interface BuildingDepartmentAssignmentCreateRequest {
  buildingId: number
  departmentId: number
  floorStart?: number | null
  floorEnd?: number | null
  allocatedRooms?: number | null
  allocatedBeds?: number | null
  priority?: number
  notes?: string
}

// 更新请求
export interface BuildingDepartmentAssignmentUpdateRequest {
  id: number
  floorStart?: number | null
  floorEnd?: number | null
  allocatedRooms?: number | null
  allocatedBeds?: number | null
  priority?: number
  status?: number
  notes?: string
}
