// 宿舍相关类型定义

// 学生简单信息(用于宿舍详情中的学生列表)
export interface StudentSimpleInfo {
  id: number
  studentNo: string
  realName: string
  bedNumber?: string
  className?: string
}

export interface Dormitory {
  id: number
  buildingId?: number
  buildingNo?: string
  buildingName?: string  // 楼宇名称
  dormitoryNo: string  // 房间号
  roomNo?: string // 房间号(前端使用,兼容字段)
  floor?: number // 楼层
  floorNumber?: number // 楼层号
  roomUsageType: number // 房间用途类型: 1学生宿舍 2教职工宿舍 3配电室 4卫生间 5杂物间 6其他
  roomUsageTypeName?: string // 房间用途类型名称
  bedCapacity: number // 床位容量规格: 4/6/8等数字
  roomType?: number // 已废弃
  roomTypeName?: string // 已废弃
  bedCount?: number // 实际床位数
  occupiedBeds?: number // 已占用床位数
  maxOccupancy?: number // 最大容纳人数
  currentOccupancy?: number // 当前入住人数
  availableBeds?: number // 可用床位数
  genderType?: number // 性别类型(从宿舍楼自动继承): 1男 2女 3混合
  genderTypeName?: string
  dormitoryType?: number // 宿舍楼类型
  dormitoryTypeName?: string
  assignedClassIds?: string // 绑定的班级ID列表(逗号分隔)
  assignedClassNames?: string // 绑定的班级名称列表
  supervisorId?: number
  supervisorName?: string
  facilities?: string
  notes?: string
  status: number
  statusName?: string
  students?: StudentSimpleInfo[] // 宿舍内的学生列表
  createdAt?: string
  updatedAt?: string
}

export interface DormitoryQueryParams {
  buildingName?: string | null
  dormitoryNo?: string
  floorNumber?: number | null
  roomType?: number | null
  status?: number | null
  pageNum?: number
  pageSize?: number
}

export interface DormitoryFormData {
  buildingId: number | null
  dormitoryNo: string  // 房间号
  floor?: number
  floorNumber: number
  roomUsageType: number // 房间用途类型: 1学生宿舍 2教职工宿舍 3配电室 4卫生间 5杂物间 6其他
  bedCapacity: number // 床位容量: 4/6/8或自定义数字
  roomType?: number // 已废弃
  totalBeds?: number
  managerId?: number | null
  facilities?: string
  status: number
  notes?: string
}

// 房间用途类型选项
export const roomUsageTypeOptions = [
  { label: '学生宿舍', value: 1 },
  { label: '教职工宿舍', value: 2 },
  { label: '配电室', value: 3 },
  { label: '卫生间', value: 4 },
  { label: '杂物间', value: 5 },
  { label: '其他', value: 6 }
]

// 床位容量选项
export const bedCapacityOptions = [
  { label: '4人间', value: 4 },
  { label: '6人间', value: 6 },
  { label: '8人间', value: 8 },
  { label: '自定义', value: 0 } // 0表示需要手动输入
]

export interface Building {
  id: number
  buildingName: string
  totalFloors: number
  totalRooms: number
  managerId?: number
  managerName?: string
  status: number
  description?: string
  createdAt: string
}

export interface BedInfo {
  id: number
  dormitoryId: number
  bedNo: string
  studentId?: number
  studentName?: string
  status: number
  assignedAt?: string
}