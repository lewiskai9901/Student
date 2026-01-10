/**
 * 宿舍管理模块类型定义
 */

// ==================== 枚举类型 ====================

export type DormitoryStatus = 0 | 1

export const DormitoryStatusMap: Record<DormitoryStatus, string> = {
  0: '停用',
  1: '正常'
}

export type GenderType = 1 | 2 | 3

export const GenderTypeMap: Record<GenderType, string> = {
  1: '男',
  2: '女',
  3: '混合'
}

export type RoomUsageType = 1 | 2 | 3 | 4 | 5 | 6

export const RoomUsageTypeMap: Record<RoomUsageType, string> = {
  1: '学生宿舍',
  2: '教职工宿舍',
  3: '配电室',
  4: '卫生间',
  5: '杂物间',
  6: '其他'
}

export type BuildingType = 1 | 2 | 3

export const BuildingTypeMap: Record<BuildingType, string> = {
  1: '教学楼',
  2: '宿舍楼',
  3: '办公楼'
}

// ==================== 实体类型 ====================

export interface StudentSimpleInfo {
  id: number
  studentNo: string
  realName: string
  bedNumber?: string
  className?: string
}

export interface Dormitory {
  id: number
  buildingId: number
  buildingName?: string
  buildingNo?: string
  departmentId?: number
  departmentName?: string
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

export interface Building {
  id: number
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

export interface BedAllocation {
  bedNumber: string
  studentId?: number
  studentNo?: string
  studentName?: string
  className?: string
  isOccupied: boolean
}

// ==================== 请求类型 ====================

export interface CreateDormitoryRequest {
  buildingId: number
  departmentId?: number
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

export interface UpdateDormitoryRequest extends Partial<CreateDormitoryRequest> {
  id?: number
}

export interface DormitoryQueryParams {
  dormitoryNo?: string
  buildingId?: number
  buildingName?: string
  floorNumber?: number
  roomType?: number
  genderType?: GenderType
  supervisorId?: number
  status?: DormitoryStatus
  buildingIds?: number[]
  classIds?: number[]
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  pageNum?: number
  pageSize?: number
}

export interface BatchCreateDormitoryRequest {
  buildingId: number
  startFloor: number
  endFloor: number
  roomsPerFloor: number
  roomUsageType: RoomUsageType
  bedCapacity: number
  genderType?: GenderType
  roomNoPrefix?: string
  startRoomNo?: number
}

export interface AssignStudentRequest {
  studentId: number
  dormitoryId: number
  bedNumber?: string
}

export interface SwapStudentsRequest {
  studentAId: number
  studentBId: number
}

export interface BatchUpdateDepartmentRequest {
  dormitoryIds: number[]
  departmentId?: number
}

export interface BatchUpdateDepartmentByFloorRequest {
  buildingId: number
  floor: number
  departmentId?: number
}

export interface CreateBuildingRequest {
  buildingNo: string
  buildingName: string
  buildingType: BuildingType
  floorCount: number
  genderType?: GenderType
  description?: string
  status?: number
}

export interface UpdateBuildingRequest extends Partial<CreateBuildingRequest> {
  id?: number
}

export interface BuildingQueryParams {
  buildingNo?: string
  buildingName?: string
  buildingType?: BuildingType
  status?: number
  pageNum?: number
  pageSize?: number
}

// ==================== 历史与分配 ====================

export interface StudentDormitoryHistory {
  id: number
  studentId: number
  dormitoryId: number
  dormitoryName: string
  buildingName: string
  roomNo: string
  bedNumber: number
  checkInDate: string
  checkOutDate?: string
  status: string
}

export interface BuildingDepartmentAssignment {
  id: number
  buildingId: number
  buildingName: string
  departmentId: number
  departmentName: string
  floorStart?: number
  floorEnd?: number
  roomCount?: number
  status?: number
  createdAt?: string
}

export interface BuildingDepartmentAssignmentQueryParams {
  pageNum?: number
  pageSize?: number
  buildingId?: number
  departmentId?: number
  status?: number
}

// ==================== 统计类型 ====================

export interface DormitoryStatistics {
  totalRooms: number
  totalBeds: number
  occupiedBeds: number
  availableBeds: number
  occupancyRate: number
  byBuilding: Record<string, { total: number; occupied: number }>
  byGender: Record<string, number>
}
