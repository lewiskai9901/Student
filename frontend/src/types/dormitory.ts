/**
 * 宿舍管理类型定义 - DDD架构适配
 */

// ==================== 枚举类型 ====================

/**
 * 宿舍状态
 */
export type DormitoryStatus = 0 | 1

export const DormitoryStatusMap: Record<DormitoryStatus, string> = {
  0: '停用',
  1: '正常'
}

/**
 * 性别类型
 */
export type GenderType = 1 | 2 | 3

export const GenderTypeMap: Record<GenderType, string> = {
  1: '男',
  2: '女',
  3: '混合'
}

/**
 * 房间用途类型
 */
export type RoomUsageType = 1 | 2 | 3 | 4 | 5 | 6

export const RoomUsageTypeMap: Record<RoomUsageType, string> = {
  1: '学生宿舍',
  2: '教职工宿舍',
  3: '配电室',
  4: '卫生间',
  5: '杂物间',
  6: '其他'
}

/**
 * 楼宇类型
 */
export type BuildingType = 1 | 2 | 3

export const BuildingTypeMap: Record<BuildingType, string> = {
  1: '教学楼',
  2: '宿舍楼',
  3: '办公楼'
}

// ==================== 实体类型 ====================

/**
 * 学生简单信息
 */
export interface StudentSimpleInfo {
  id: number | string
  studentNo: string
  realName: string
  bedNumber?: string
  className?: string
}

/**
 * 宿舍实体
 */
export interface Dormitory {
  id: number | string
  buildingId: number | string
  buildingName?: string
  buildingNo?: string
  orgUnitId?: number | string
  orgUnitName?: string
  dormitoryNo: string
  roomNo?: string
  floorNumber: number
  floor?: number
  roomUsageType: RoomUsageType
  roomUsageTypeName?: string
  bedCapacity: number
  bedCount: number
  occupiedBeds: number
  genderType: GenderType
  genderTypeName?: string
  facilities?: string
  notes?: string
  status: DormitoryStatus
  statusName?: string
  assignedClassIds?: string
  assignedClassNames?: string
  classTeacherNames?: string
  classTeacherPhones?: string
  dormitoryType?: number
  dormitoryTypeName?: string
  maxOccupancy?: number
  maxCapacity?: number
  currentOccupancy?: number
  currentCount?: number
  students?: StudentSimpleInfo[]
  createdAt?: string
  updatedAt?: string
}

/**
 * 楼宇实体
 */
export interface Building {
  id: number | string
  buildingNo: string
  buildingName: string
  buildingType: BuildingType
  buildingTypeName?: string
  floorCount: number
  genderType?: GenderType
  genderTypeName?: string
  description?: string
  status: number
  statusName?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 床位分配信息
 */
export interface BedAllocation {
  bedNumber: string
  studentId?: number | string
  studentNo?: string
  studentName?: string
  className?: string
  isOccupied: boolean
}

// ==================== 请求类型 ====================

/**
 * 创建宿舍请求
 */
export interface CreateDormitoryRequest {
  buildingId: number | string
  orgUnitId?: number | string
  dormitoryNo: string
  floorNumber: number
  roomUsageType: RoomUsageType
  bedCapacity: number
  bedCount?: number
  genderType?: GenderType
  facilities?: string
  notes?: string
  status?: DormitoryStatus
}

/**
 * 更新宿舍请求
 */
export interface UpdateDormitoryRequest extends Partial<CreateDormitoryRequest> {
  id?: number | string
}

/**
 * 宿舍查询参数
 */
export interface DormitoryQueryParams {
  dormitoryNo?: string
  buildingId?: number | string
  buildingName?: string
  floorNumber?: number
  roomType?: number
  genderType?: GenderType
  supervisorId?: number | string
  status?: DormitoryStatus
  buildingIds?: (number | string)[]
  orgUnitIds?: (number | string)[]
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  pageNum?: number
  pageSize?: number
}

/**
 * 批量创建宿舍请求
 */
export interface BatchCreateDormitoryRequest {
  buildingId: number | string
  startFloor: number
  endFloor: number
  roomsPerFloor: number
  roomUsageType: RoomUsageType
  bedCapacity: number
  genderType?: GenderType
  roomNoPrefix?: string
  startRoomNo?: number
}

/**
 * 分配学生到宿舍请求
 */
export interface AssignStudentRequest {
  studentId: number | string
  dormitoryId: number | string
  bedNumber?: string
}

/**
 * 交换学生宿舍请求
 */
export interface SwapStudentsRequest {
  studentAId: number | string
  studentBId: number | string
}

/**
 * 批量更新组织单元分配请求
 * 注意: dormitoryIds 使用 string[] 以避免大整数(snowflake ID)精度丢失
 */
export interface BatchUpdateOrgUnitRequest {
  dormitoryIds: (number | string)[]
  orgUnitId?: number | string | null
}

/**
 * 按楼层批量更新组织单元请求
 */
export interface BatchUpdateOrgUnitByFloorRequest {
  buildingId: number | string
  floor: number
  orgUnitId?: number | string | null
}

// 别名（兼容API命名）
export type BatchUpdateDepartmentRequest = BatchUpdateOrgUnitRequest
export type BatchUpdateDepartmentByFloorRequest = BatchUpdateOrgUnitByFloorRequest

/**
 * 创建楼宇请求
 */
export interface CreateBuildingRequest {
  buildingNo: string
  buildingName: string
  buildingType: BuildingType
  floorCount: number
  genderType?: GenderType
  description?: string
  status?: number
}

/**
 * 更新楼宇请求
 */
export interface UpdateBuildingRequest extends Partial<CreateBuildingRequest> {
  id?: number | string
}

/**
 * 楼宇查询参数
 */
export interface BuildingQueryParams {
  buildingNo?: string
  buildingName?: string
  buildingType?: BuildingType
  status?: number
  pageNum?: number
  pageSize?: number
}

// ==================== 统计类型 ====================

/**
 * 宿舍统计
 */
export interface DormitoryStatistics {
  totalRooms: number
  totalBeds: number
  occupiedBeds: number
  availableBeds: number
  occupancyRate: number
  byBuilding: Record<string, {
    total: number
    occupied: number
  }>
  byGender: Record<string, number>
}
