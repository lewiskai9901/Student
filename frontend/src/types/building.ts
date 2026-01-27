// 楼宇管理类型定义

export interface Building {
  id: number
  buildingNo: string
  buildingName: string
  buildingType: number
  totalFloors: number
  location?: string
  constructionYear?: number
  managerId?: number
  managerName?: string
  description?: string
  status: number
  roomCount?: number
  buildingTypeName?: string
  // 组织单元关联字段
  orgUnitIds?: number[]
  orgUnitNames?: string | string[]  // 后端返回字符串(GROUP_CONCAT),前端需要转换为数组
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  updatedBy?: number
  deleted?: number
}

export interface BuildingQueryParams {
  pageNum: number
  pageSize: number
  buildingNo?: string
  buildingName?: string
  buildingType?: number
  status?: number
}

export interface BuildingFormData {
  buildingNo: string
  buildingName: string
  buildingType: number
  totalFloors: number
  location?: string
  constructionYear?: number
  managerId?: number
  description?: string
  status: number
  // 组织单元关联
  orgUnitIds?: number[]
}

// 楼宇类型选项
export const buildingTypeOptions = [
  { label: '教学楼', value: 1 },
  { label: '宿舍楼', value: 2 },
  { label: '办公楼', value: 3 }
]

// 状态选项
export const buildingStatusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]
