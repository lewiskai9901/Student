/**
 * 宿舍管理 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import { http } from '@/utils/request'
import type {
  Dormitory,
  Building,
  BedAllocation,
  CreateDormitoryRequest,
  UpdateDormitoryRequest,
  DormitoryQueryParams,
  BatchCreateDormitoryRequest,
  AssignStudentRequest,
  SwapStudentsRequest,
  BatchUpdateDepartmentRequest,
  BatchUpdateDepartmentByFloorRequest,
  CreateBuildingRequest,
  UpdateBuildingRequest,
  BuildingQueryParams
} from '@/types/v2/dormitory'
import type { PageResponse } from '@/types/v2'

// 后端API路径 - V2 DDD架构
const DORMITORY_URL = '/v2/dormitory/rooms'
const BUILDING_URL = '/teaching/buildings'  // 楼宇暂无V2端点

// ==================== 宿舍 CRUD ====================

/**
 * 分页查询宿舍
 */
export function getDormitories(params?: DormitoryQueryParams): Promise<PageResponse<Dormitory>> {
  return http.get<PageResponse<Dormitory>>(DORMITORY_URL, { params })
}

/**
 * 获取宿舍详情
 */
export function getDormitory(id: number): Promise<Dormitory> {
  return http.get<Dormitory>(`${DORMITORY_URL}/${id}`)
}

/**
 * 创建宿舍
 */
export function createDormitory(data: CreateDormitoryRequest): Promise<number> {
  return http.post<number>(DORMITORY_URL, data)
}

/**
 * 更新宿舍
 */
export function updateDormitory(id: number, data: UpdateDormitoryRequest): Promise<void> {
  return http.put(`${DORMITORY_URL}/${id}`, data)
}

/**
 * 删除宿舍
 */
export function deleteDormitory(id: number, force = false): Promise<void> {
  return http.delete(`${DORMITORY_URL}/${id}`, { params: { force } })
}

/**
 * 批量删除宿舍
 */
export function deleteDormitories(ids: number[]): Promise<void> {
  return http.delete(`${DORMITORY_URL}/batch`, { data: ids })
}

/**
 * 批量创建宿舍
 */
export function batchCreateDormitories(data: BatchCreateDormitoryRequest): Promise<number> {
  return http.post<number>(`${DORMITORY_URL}/batch`, data)
}

// ==================== 宿舍查询 ====================

/**
 * 根据楼宇ID获取宿舍列表
 */
export function getDormitoriesByBuilding(buildingId: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/by-building/${buildingId}`)
}

/**
 * 根据性别类型获取宿舍列表
 */
export function getDormitoriesByGender(genderType: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/by-gender/${genderType}`)
}

/**
 * 根据部门ID获取宿舍列表
 */
export function getDormitoriesByDepartment(departmentId: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/by-department`, { params: { departmentId } })
}

/**
 * 获取有空床位的宿舍
 */
export function getAvailableDormitories(genderType?: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/available`, { params: { genderType } })
}

/**
 * 根据宿管员ID获取宿舍列表
 */
export function getDormitoriesBySupervisor(supervisorId: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/by-supervisor/${supervisorId}`)
}

/**
 * 获取所有正常状态的宿舍
 */
export function getAllNormalDormitories(): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/normal`)
}

/**
 * 检查宿舍编号是否存在
 */
export function existsDormitoryNo(buildingId: number, dormitoryNo: string, excludeId?: number): Promise<boolean> {
  return http.get<boolean>(`${DORMITORY_URL}/exists`, { params: { buildingId, dormitoryNo, excludeId } })
}

// ==================== 宿舍状态操作 ====================

/**
 * 更新宿舍状态
 */
export function updateDormitoryStatus(id: number, status: number): Promise<void> {
  return http.patch(`${DORMITORY_URL}/${id}/status`, null, { params: { status } })
}

/**
 * 更新宿舍已占用床位数
 */
export function updateOccupiedBeds(id: number, occupiedBeds: number): Promise<void> {
  return http.patch(`${DORMITORY_URL}/${id}/occupied-beds`, null, { params: { occupiedBeds } })
}

// ==================== 床位管理 ====================

/**
 * 分配床位
 */
export function assignBed(dormitoryId: number, studentId: number): Promise<void> {
  return http.post(`${DORMITORY_URL}/${dormitoryId}/assign-bed`, null, { params: { studentId } })
}

/**
 * 释放床位
 */
export function releaseBed(dormitoryId: number, studentId: number): Promise<void> {
  return http.post(`${DORMITORY_URL}/${dormitoryId}/release-bed`, null, { params: { studentId } })
}

/**
 * 获取床位分配情况
 */
export function getBedAllocations(dormitoryId: number): Promise<BedAllocation[]> {
  return http.get<BedAllocation[]>(`${DORMITORY_URL}/${dormitoryId}/bed-allocations`)
}

/**
 * 分配学生到宿舍
 */
export function assignStudentToDormitory(data: AssignStudentRequest): Promise<void> {
  return http.post(`${DORMITORY_URL}/assign-student`, data)
}

/**
 * 从宿舍移除学生
 */
export function removeStudentFromDormitory(studentId: number): Promise<void> {
  return http.delete(`${DORMITORY_URL}/remove-student/${studentId}`)
}

/**
 * 交换学生宿舍
 */
export function swapStudentDormitory(data: SwapStudentsRequest): Promise<void> {
  return http.post(`${DORMITORY_URL}/swap-students`, data)
}

// ==================== 批量操作 ====================

/**
 * 批量更新院系分配
 */
export function batchUpdateDepartment(data: BatchUpdateDepartmentRequest): Promise<number> {
  return http.put<number>(`${DORMITORY_URL}/batch-department`, data)
}

/**
 * 按楼层批量更新院系分配
 */
export function batchUpdateDepartmentByFloor(data: BatchUpdateDepartmentByFloorRequest): Promise<number> {
  return http.put<number>(`${DORMITORY_URL}/batch-department-by-floor`, data)
}

// ==================== 楼宇 CRUD (暂用V1端点) ====================

/**
 * 分页查询楼宇
 */
export function getBuildings(params?: BuildingQueryParams): Promise<PageResponse<Building>> {
  return http.get<PageResponse<Building>>(BUILDING_URL, { params })
}

/**
 * 获取楼宇详情
 */
export function getBuilding(id: number): Promise<Building> {
  return http.get<Building>(`${BUILDING_URL}/${id}`)
}

/**
 * 创建楼宇
 */
export function createBuilding(data: CreateBuildingRequest): Promise<Building> {
  return http.post<Building>(BUILDING_URL, data)
}

/**
 * 更新楼宇
 */
export function updateBuilding(id: number, data: UpdateBuildingRequest): Promise<Building> {
  return http.put<Building>(`${BUILDING_URL}/${id}`, data)
}

/**
 * 删除楼宇
 */
export function deleteBuilding(id: number): Promise<void> {
  return http.delete(`${BUILDING_URL}/${id}`)
}

/**
 * 获取所有启用的楼宇
 */
export function getAllEnabledBuildings(buildingType?: number): Promise<Building[]> {
  return http.get<Building[]>(`${BUILDING_URL}/enabled`, { params: { buildingType } })
}

/**
 * 检查楼号是否存在
 */
export function existsBuildingNo(buildingNo: string, excludeId?: number): Promise<boolean> {
  return http.get<boolean>(`${BUILDING_URL}/exists`, { params: { buildingNo, excludeId } })
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 宿舍 API 对象
 */
export const dormitoryApi = {
  getList: getDormitories,
  getById: getDormitory,
  create: createDormitory,
  update: updateDormitory,
  delete: deleteDormitory,
  deleteBatch: deleteDormitories,
  batchCreate: batchCreateDormitories,
  getByBuilding: getDormitoriesByBuilding,
  getByGender: getDormitoriesByGender,
  getByDepartment: getDormitoriesByDepartment,
  getAvailable: getAvailableDormitories,
  getBySupervisor: getDormitoriesBySupervisor,
  getAllNormal: getAllNormalDormitories,
  exists: existsDormitoryNo,
  updateStatus: updateDormitoryStatus,
  updateOccupiedBeds,
  assignBed,
  releaseBed,
  getBedAllocations,
  assignStudent: assignStudentToDormitory,
  removeStudent: removeStudentFromDormitory,
  swapStudents: swapStudentDormitory,
  batchUpdateDepartment,
  batchUpdateDepartmentByFloor
}

/**
 * 楼宇 API 对象
 */
export const buildingApi = {
  getList: getBuildings,
  getById: getBuilding,
  create: createBuilding,
  update: updateBuilding,
  delete: deleteBuilding,
  getAllEnabled: getAllEnabledBuildings,
  exists: existsBuildingNo
}
