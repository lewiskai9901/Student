/**
 * 宿舍管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 宿舍楼信息
export interface Building {
  id: number | string
  buildingCode: string
  buildingName: string
  buildingType?: number  // 1:男生宿舍 2:女生宿舍 3:混合
  buildingTypeName?: string
  floorCount?: number
  roomsPerFloor?: number
  totalRooms?: number
  totalBeds?: number
  occupiedBeds?: number
  occupancyRate?: number
  address?: string
  managerId?: number
  managerName?: string
  status: number
  statusText?: string
  createdAt?: string
  updatedAt?: string
}

// 宿舍房间信息
export interface Dormitory {
  id: number | string
  roomNo: string
  buildingId: number
  buildingName?: string
  floor: number
  roomType?: number       // 1:标准间 2:单人间 3:套间
  roomTypeName?: string
  roomUsageType?: number  // 1:学生宿舍 2:教工宿舍 3:客房
  usageTypeName?: string
  bedCapacity: number     // 床位容量
  occupiedBeds?: number   // 已入住床位
  availableBeds?: number  // 可用床位
  occupancyRate?: number  // 入住率
  departmentId?: number
  departmentName?: string
  facilities?: string     // 设施
  status: number
  statusText?: string
  createdAt?: string
  updatedAt?: string
}

// 床位信息
export interface BedInfo {
  bedNo: string
  studentId?: number
  studentNo?: string
  studentName?: string
  status: number  // 0:空闲 1:已占用
  statusText?: string
}

// 查询参数
export interface DormitoryQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  roomNo?: string
  buildingId?: number
  floor?: number
  departmentId?: number
  status?: number
  hasAvailableBeds?: boolean
}

// 创建/更新宿舍请求
export interface DormitoryFormData {
  roomNo: string
  buildingId: number
  floor: number
  roomType?: number
  roomUsageType?: number
  bedCapacity: number
  departmentId?: number
  facilities?: string
  status?: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 入住统计
export interface OccupancyStats {
  totalBuildings: number
  totalRooms: number
  totalBeds: number
  occupiedBeds: number
  availableBeds: number
  occupancyRate: number
  buildingStats?: BuildingStats[]
  departmentStats?: DepartmentStats[]
}

export interface BuildingStats {
  buildingId: number
  buildingName: string
  totalRooms: number
  totalBeds: number
  occupiedBeds: number
  occupancyRate: number
}

export interface DepartmentStats {
  departmentId: number
  departmentName: string
  totalBeds: number
  occupiedBeds: number
  occupancyRate: number
}

/**
 * 获取宿舍楼列表
 */
export function getBuildingList(params?: any) {
  return get<PageResult<Building>>('/dormitory/buildings', {
    pageNum: params?.pageNum || 1,
    pageSize: params?.pageSize || 100,
    ...params
  })
}

/**
 * 获取宿舍楼详情
 */
export function getBuildingDetail(id: number | string) {
  return get<Building>(`/dormitory/buildings/${id}`)
}

/**
 * 获取宿舍列表
 */
export function getDormitoryList(params: DormitoryQueryParams = {}) {
  return get<PageResult<Dormitory>>('/dormitory/rooms', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 获取宿舍详情
 */
export function getDormitoryDetail(id: number | string) {
  return get<Dormitory>(`/dormitory/rooms/${id}`)
}

/**
 * 创建宿舍
 */
export function createDormitory(data: DormitoryFormData) {
  return post<Dormitory>('/dormitory/rooms', data)
}

/**
 * 更新宿舍信息
 */
export function updateDormitory(id: number | string, data: DormitoryFormData) {
  return put<Dormitory>(`/dormitory/rooms/${id}`, data)
}

/**
 * 删除宿舍
 */
export function deleteDormitory(id: number | string, force: boolean = false) {
  return del<void>(`/dormitory/rooms/${id}${force ? '?force=true' : ''}`)
}

/**
 * 批量删除宿舍
 */
export function batchDeleteDormitories(ids: (number | string)[]) {
  return del<void>('/dormitory/rooms/batch', { ids })
}

/**
 * 获取宿舍床位信息
 */
export function getDormitoryBeds(dormitoryId: number | string) {
  return get<BedInfo[]>(`/dormitory/rooms/${dormitoryId}/beds`)
}

/**
 * 分配床位
 */
export function assignBed(dormitoryId: number | string, data: { bedNo: string; studentId: number }) {
  return post<void>(`/dormitory/rooms/${dormitoryId}/assign-bed`, data)
}

/**
 * 释放床位
 */
export function releaseBed(dormitoryId: number | string, bedNo: string) {
  return del<void>(`/dormitory/rooms/${dormitoryId}/beds/${bedNo}`)
}

/**
 * 获取宿舍入住统计
 */
export function getDormitoryOccupancyStats() {
  return get<OccupancyStats>('/dormitory/rooms/occupancy-stats')
}

/**
 * 获取宿舍床位分配情况
 */
export function getBedAllocations(dormitoryId: number | string) {
  return get<any[]>(`/dormitory/rooms/${dormitoryId}/bed-allocations`)
}

/**
 * 分配学生到宿舍
 */
export function assignStudentToDormitory(data: { studentId: number; dormitoryId: number; bedNumber: number }) {
  return post<void>('/dormitory/rooms/assign-student', data)
}

/**
 * 从宿舍移除学生
 */
export function removeStudentFromDormitory(studentId: number) {
  return del<void>(`/dormitory/rooms/remove-student/${studentId}`)
}

/**
 * 交换学生宿舍
 */
export function swapStudentDormitory(data: { studentAId: number; studentBId: number }) {
  return post<void>('/dormitory/rooms/swap-students', data)
}

/**
 * 根据楼宇ID获取宿舍列表
 */
export function getDormitoriesByBuildingId(buildingId: number) {
  return get<Dormitory[]>('/dormitory/rooms/by-building', { buildingId })
}

/**
 * 根据部门ID获取宿舍列表
 */
export function getDormitoriesByDepartment(departmentId: number) {
  return get<Dormitory[]>('/dormitory/rooms/by-department', { departmentId })
}

/**
 * 获取可用房间列表(有空床位)
 */
export function getAvailableRooms(buildingId?: number, floor?: number) {
  return get<Dormitory[]>('/dormitory/rooms/available', { buildingId, floor })
}
