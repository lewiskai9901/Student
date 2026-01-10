/**
 * 宿舍管理模块 API
 */
import { http } from '@/shared/utils/request'
import type { PageResponse } from '@/shared/types'
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
  BuildingQueryParams,
  StudentDormitoryHistory,
  BuildingDepartmentAssignment,
  BuildingDepartmentAssignmentQueryParams
} from './types'

const DORMITORY_URL = '/v2/dormitory/rooms'
const BUILDING_URL = '/teaching/buildings'

// ==================== 宿舍 CRUD ====================

export function getDormitories(params?: DormitoryQueryParams): Promise<PageResponse<Dormitory>> {
  return http.get<PageResponse<Dormitory>>(DORMITORY_URL, { params })
}

export function getDormitory(id: number): Promise<Dormitory> {
  return http.get<Dormitory>(`${DORMITORY_URL}/${id}`)
}

export function createDormitory(data: CreateDormitoryRequest): Promise<number> {
  return http.post<number>(DORMITORY_URL, data)
}

export function updateDormitory(id: number, data: UpdateDormitoryRequest): Promise<void> {
  return http.put(`${DORMITORY_URL}/${id}`, data)
}

export function deleteDormitory(id: number, force = false): Promise<void> {
  return http.delete(`${DORMITORY_URL}/${id}`, { params: { force } })
}

export function deleteDormitories(ids: number[]): Promise<void> {
  return http.delete(`${DORMITORY_URL}/batch`, { data: ids })
}

export function batchCreateDormitories(data: BatchCreateDormitoryRequest): Promise<number> {
  return http.post<number>(`${DORMITORY_URL}/batch`, data)
}

// ==================== 宿舍查询 ====================

export function getDormitoriesByBuilding(buildingId: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/by-building/${buildingId}`)
}

export function getDormitoriesByGender(genderType: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/by-gender/${genderType}`)
}

export function getDormitoriesByDepartment(departmentId: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/by-department`, { params: { departmentId } })
}

export function getAvailableDormitories(genderType?: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/available`, { params: { genderType } })
}

export function getDormitoriesBySupervisor(supervisorId: number): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/by-supervisor/${supervisorId}`)
}

export function getAllNormalDormitories(): Promise<Dormitory[]> {
  return http.get<Dormitory[]>(`${DORMITORY_URL}/normal`)
}

export function existsDormitoryNo(buildingId: number, dormitoryNo: string, excludeId?: number): Promise<boolean> {
  return http.get<boolean>(`${DORMITORY_URL}/exists`, { params: { buildingId, dormitoryNo, excludeId } })
}

// ==================== 宿舍状态操作 ====================

export function updateDormitoryStatus(id: number, status: number): Promise<void> {
  return http.patch(`${DORMITORY_URL}/${id}/status`, null, { params: { status } })
}

export function updateOccupiedBeds(id: number, occupiedBeds: number): Promise<void> {
  return http.patch(`${DORMITORY_URL}/${id}/occupied-beds`, null, { params: { occupiedBeds } })
}

// ==================== 床位管理 ====================

export function assignBed(dormitoryId: number, studentId: number): Promise<void> {
  return http.post(`${DORMITORY_URL}/${dormitoryId}/assign-bed`, null, { params: { studentId } })
}

export function releaseBed(dormitoryId: number, studentId: number): Promise<void> {
  return http.post(`${DORMITORY_URL}/${dormitoryId}/release-bed`, null, { params: { studentId } })
}

export function getBedAllocations(dormitoryId: number): Promise<BedAllocation[]> {
  return http.get<BedAllocation[]>(`${DORMITORY_URL}/${dormitoryId}/bed-allocations`)
}

export function assignStudentToDormitory(data: AssignStudentRequest): Promise<void> {
  return http.post(`${DORMITORY_URL}/assign-student`, data)
}

export function removeStudentFromDormitory(studentId: number): Promise<void> {
  return http.delete(`${DORMITORY_URL}/remove-student/${studentId}`)
}

export function swapStudentDormitory(data: SwapStudentsRequest): Promise<void> {
  return http.post(`${DORMITORY_URL}/swap-students`, data)
}

// ==================== 批量操作 ====================

export function batchUpdateDepartment(data: BatchUpdateDepartmentRequest): Promise<number> {
  return http.put<number>(`${DORMITORY_URL}/batch-department`, data)
}

export function batchUpdateDepartmentByFloor(data: BatchUpdateDepartmentByFloorRequest): Promise<number> {
  return http.put<number>(`${DORMITORY_URL}/batch-department-by-floor`, data)
}

// ==================== 楼宇 CRUD ====================

export function getBuildings(params?: BuildingQueryParams): Promise<PageResponse<Building>> {
  return http.get<PageResponse<Building>>(BUILDING_URL, { params })
}

export function getBuilding(id: number): Promise<Building> {
  return http.get<Building>(`${BUILDING_URL}/${id}`)
}

export function createBuilding(data: CreateBuildingRequest): Promise<Building> {
  return http.post<Building>(BUILDING_URL, data)
}

export function updateBuilding(id: number, data: UpdateBuildingRequest): Promise<Building> {
  return http.put<Building>(`${BUILDING_URL}/${id}`, data)
}

export function deleteBuilding(id: number): Promise<void> {
  return http.delete(`${BUILDING_URL}/${id}`)
}

export function getAllEnabledBuildings(buildingType?: number): Promise<Building[]> {
  return http.get<Building[]>(`${BUILDING_URL}/enabled`, { params: { buildingType } })
}

export function existsBuildingNo(buildingNo: string, excludeId?: number): Promise<boolean> {
  return http.get<boolean>(`${BUILDING_URL}/exists`, { params: { buildingNo, excludeId } })
}

// ==================== 导出与统计 ====================

export function exportDormitories(params?: DormitoryQueryParams): Promise<Blob> {
  return http.get<Blob>('/dormitory/rooms/export', { params, responseType: 'blob' })
}

export function getDormitoryOccupancyStats(): Promise<any> {
  return http.get('/dormitory/rooms/occupancy-stats')
}

export function getDormitoryManagerList(): Promise<any[]> {
  return http.get('/dormitory-managers')
}

// ==================== 学生宿舍历史 ====================

export function getStudentDormitoryHistory(studentId: number): Promise<StudentDormitoryHistory[]> {
  return http.get<StudentDormitoryHistory[]>(`/student-dormitory/history/${studentId}`)
}

// ==================== 楼宇院系分配 ====================

export function getBuildingDepartmentAssignmentList(params?: BuildingDepartmentAssignmentQueryParams): Promise<{ records: BuildingDepartmentAssignment[]; total: number }> {
  return http.get('/dormitory/building-assignments', { params })
}

export function getBuildingDepartmentAssignmentDetail(id: number): Promise<BuildingDepartmentAssignment> {
  return http.get<BuildingDepartmentAssignment>(`/dormitory/building-assignments/${id}`)
}

export function getAssignmentsByBuildingId(buildingId: number): Promise<BuildingDepartmentAssignment[]> {
  return http.get<BuildingDepartmentAssignment[]>(`/dormitory/building-assignments/building/${buildingId}`)
}

export function createBuildingDepartmentAssignment(data: { buildingId: number; departmentId: number; floorStart?: number; floorEnd?: number }): Promise<BuildingDepartmentAssignment> {
  return http.post('/dormitory/building-assignments', data)
}

export function updateBuildingDepartmentAssignment(data: { id: number; floorStart?: number; floorEnd?: number }): Promise<BuildingDepartmentAssignment> {
  return http.put('/dormitory/building-assignments', data)
}

export function deleteBuildingDepartmentAssignment(id: number): Promise<void> {
  return http.delete(`/dormitory/building-assignments/${id}`)
}

export function enableBuildingDepartmentAssignment(id: number): Promise<void> {
  return http.put(`/dormitory/building-assignments/${id}/enable`)
}

export function disableBuildingDepartmentAssignment(id: number): Promise<void> {
  return http.put(`/dormitory/building-assignments/${id}/disable`)
}

// ==================== 宿舍楼宇管理 ====================

export function getDormitoryBuildingList(params?: any): Promise<{ records: Building[]; total: number }> {
  return http.get('/dormitory/buildings', { params })
}

export function getAllEnabledDormitoryBuildings(): Promise<Building[]> {
  return http.get<Building[]>('/dormitory/buildings/enabled')
}

// ==================== 兼容别名 ====================

export const getDormitoryList = getDormitories
export const getDormitoryDetail = getDormitory
export const getBuildingList = getBuildings
export const getBuildingDetail = getBuilding
export const checkBuildingNoExists = existsBuildingNo
export const getDormitoriesByBuildingId = getDormitoriesByBuilding

// ==================== API 对象封装 ====================

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

export const buildingApi = {
  getList: getBuildings,
  getById: getBuilding,
  create: createBuilding,
  update: updateBuilding,
  delete: deleteBuilding,
  getAllEnabled: getAllEnabledBuildings,
  exists: existsBuildingNo
}
