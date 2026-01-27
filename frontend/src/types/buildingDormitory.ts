// 宿舍楼管理类型定义

/**
 * 宿舍楼信息
 */
export interface BuildingDormitory {
  id: number | string
  buildingId: number | string
  buildingNo?: string
  buildingName?: string
  buildingLocation?: string
  totalFloors?: number
  dormitoryType?: number | null
  dormitoryTypeName?: string
  totalRooms?: number
  occupiedRooms?: number
  availableRooms?: number
  managementRules?: string
  visitingHours?: string
  facilities?: string
  description?: string
  status?: number
  // 管理员信息
  managerIds?: number[]
  managerNames?: string[]
  managers?: ManagerInfo[]
  // 组织单元信息
  orgUnitIds?: number[]
  orgUnitNames?: string[]
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  updatedBy?: number
}

/**
 * 管理员信息
 */
export interface ManagerInfo {
  id: number
  userId: number
  username: string
  realName: string
  orgUnitName?: string
  assignedAt?: string
}

/**
 * 宿舍楼查询参数
 */
export interface BuildingDormitoryQueryParams {
  pageNum: number
  pageSize: number
  buildingName?: string
  dormitoryType?: number | null
}

/**
 * 宿舍楼表单数据
 */
export interface BuildingDormitoryFormData {
  dormitoryType?: number | null
  managementRules?: string
  visitingHours?: string
  facilities?: string
  description?: string
  managerIds?: number[]
}

/**
 * 管理员分配数据
 */
export interface ManagerAssignmentData {
  buildingId: number
  managerIds: number[]
}

/**
 * 宿舍楼类型选项
 */
export const dormitoryTypeOptions = [
  { label: '男生宿舍楼', value: 1, allowedGenderTypes: [1] },
  { label: '女生宿舍楼', value: 2, allowedGenderTypes: [2] },
  { label: '教职工男生宿舍楼', value: 3, allowedGenderTypes: [1] },
  { label: '教职工女生宿舍楼', value: 4, allowedGenderTypes: [2] },
  { label: '教职工混合宿舍楼', value: 5, allowedGenderTypes: [1, 2, 3] }
]

/**
 * 获取宿舍楼类型名称
 */
export function getDormitoryTypeName(type: number | null | undefined): string {
  if (type === null || type === undefined) return '未设置'
  const option = dormitoryTypeOptions.find(item => item.value === type)
  return option?.label || '未知'
}

/**
 * 获取宿舍楼允许的房间性别类型
 */
export function getAllowedGenderTypes(dormitoryType: number | null | undefined): number[] {
  if (dormitoryType === null || dormitoryType === undefined) return []
  const option = dormitoryTypeOptions.find(item => item.value === dormitoryType)
  return option?.allowedGenderTypes || []
}

/**
 * 房间性别类型选项
 */
export const genderTypeOptions = [
  { label: '男生', value: 1 },
  { label: '女生', value: 2 },
  { label: '混合', value: 3 }
]

/**
 * 获取房间性别类型名称
 */
export function getGenderTypeName(type: number | null | undefined): string {
  if (type === null || type === undefined) return '未设置'
  const option = genderTypeOptions.find(item => item.value === type)
  return option?.label || '未知'
}
