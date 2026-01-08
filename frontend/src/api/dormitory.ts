import { http } from '@/utils/request'
import type { Dormitory, DormitoryQueryParams, DormitoryFormData, Building, BedInfo } from '@/types/dormitory'

// V2 API 基础路径
const BASE_URL = '/v2/dormitory/rooms'

/**
 * 获取宿舍列表
 */
export function getDormitoryList(params: DormitoryQueryParams) {
  return http.get<{
    records: Dormitory[]
    total: number
  }>(BASE_URL, { params })
}

/**
 * 获取宿舍详情
 */
export function getDormitoryDetail(id: number) {
  return http.get<Dormitory>(`${BASE_URL}/${id}`)
}

/**
 * 创建宿舍
 */
export function createDormitory(data: DormitoryFormData) {
  return http.post<Dormitory>(BASE_URL, data)
}

/**
 * 更新宿舍信息
 */
export function updateDormitory(id: number, data: DormitoryFormData) {
  return http.put<Dormitory>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除宿舍
 */
export function deleteDormitory(id: number, force: boolean = false) {
  return http.delete(`${BASE_URL}/${id}`, { params: { force } })
}

/**
 * 批量删除宿舍
 */
export function batchDeleteDormitories(ids: number[]) {
  return http.delete(`${BASE_URL}/batch`, { data: { ids } })
}

/**
 * 获取宿舍楼列表 (保持V1端点,因V2暂无楼宇管理)
 */
export function getBuildingList(params?: any) {
  return http.get<{
    records: Building[]
    total: number
  }>('/dormitory/buildings', { params })
}

/**
 * 获取宿舍床位信息
 */
export function getDormitoryBeds(dormitoryId: number) {
  return http.get<BedInfo[]>(`/dormitory/rooms/${dormitoryId}/beds`)
}

/**
 * 学生入住
 */
export function checkInStudent(dormitoryId: number, data: {
  studentId: number
  studentName?: string
  bedNumber: number
}) {
  return http.post(`${BASE_URL}/${dormitoryId}/check-in`, data)
}

/**
 * 学生退宿
 */
export function checkOutStudent(dormitoryId: number, data: {
  studentId: number
  studentName?: string
  bedNumber: number
}) {
  return http.post(`${BASE_URL}/${dormitoryId}/check-out`, data)
}

/**
 * 分配床位 (兼容V1)
 */
export function assignBed(dormitoryId: number, data: {
  bedNo: string
  studentId: number
}) {
  return checkInStudent(dormitoryId, {
    studentId: data.studentId,
    bedNumber: parseInt(data.bedNo)
  })
}

/**
 * 释放床位 (兼容V1)
 */
export function releaseBed(dormitoryId: number, bedNo: string) {
  return http.delete(`/dormitory/rooms/${dormitoryId}/beds/${bedNo}`)
}

/**
 * 获取宿舍入住统计
 */
export function getDormitoryOccupancyStats() {
  return http.get('/dormitory/rooms/occupancy-stats')
}

/**
 * 导出宿舍数据
 */
export function exportDormitories(params: DormitoryQueryParams) {
  return http.get('/dormitory/rooms/export', {
    params,
    responseType: 'blob'
  })
}

/**
 * 获取宿舍管理员列表
 */
export function getDormitoryManagerList() {
  return http.get('/dormitory-managers')
}

/**
 * 获取宿舍床位分配情况
 */
export function getBedAllocations(dormitoryId: number) {
  return http.get<any[]>(`/dormitory/rooms/${dormitoryId}/bed-allocations`)
}

/**
 * 分配学生到宿舍 (兼容V1)
 */
export function assignStudentToDormitory(data: {
  studentId: number
  dormitoryId: number
  bedNumber: number
}) {
  return checkInStudent(data.dormitoryId, {
    studentId: data.studentId,
    bedNumber: data.bedNumber
  })
}

/**
 * 从宿舍移除学生
 */
export function removeStudentFromDormitory(studentId: number) {
  return http.delete(`/dormitory/rooms/remove-student/${studentId}`)
}

/**
 * 交换学生宿舍
 */
export function swapStudentDormitory(data: {
  studentAId: number
  studentBId: number
}) {
  return http.post('/dormitory/rooms/swap-students', data)
}

/**
 * 批量生成宿舍
 */
export function batchCreateDormitories(data: {
  buildingId: number
  startFloor: number
  endFloor: number
  roomsPerFloor: number
  numberFormat: number
  roomUsageType: number
  bedCapacity: number
  facilities?: string
  status: number
}) {
  return http.post<number>('/dormitory/rooms/batch', data)
}

/**
 * 批量更新宿舍房间的院系分配
 */
export function batchUpdateDepartment(data: {
  dormitoryIds: number[]
  departmentId: number | null
}) {
  return http.put<number>('/dormitory/rooms/batch-department', data)
}

/**
 * 按楼层批量更新院系分配
 */
export function batchUpdateDepartmentByFloor(data: {
  buildingId: number
  floor: number
  departmentId: number | null
}) {
  return http.put<number>('/dormitory/rooms/batch-department-by-floor', data)
}

/**
 * 根据楼宇ID获取宿舍列表
 */
export function getDormitoriesByBuildingId(buildingId: number) {
  return http.get<Dormitory[]>(`${BASE_URL}/by-building/${buildingId}`)
}
