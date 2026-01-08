import { http } from '@/utils/request'

// V2 API 基础路径
const BASE_URL = '/v2/org-units'

export interface DepartmentCreateRequest {
  unitCode: string
  unitName: string
  unitType?: string
  parentId?: number
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder?: number
}

export interface DepartmentResponse {
  id: number
  unitCode: string
  unitName: string
  unitType: string
  parentId: number
  leaderId?: number
  deputyLeaderIds?: number[]
  sortOrder: number
  isEnabled: boolean
  createdAt: string
  updatedAt: string
  children?: DepartmentResponse[]
}

// 兼容性字段映射 (V1 -> V2)
// deptCode -> unitCode
// deptName -> unitName
// leaderName -> leaderId (需要额外处理)
// status -> isEnabled

/**
 * 创建部门
 */
export function createDepartment(data: DepartmentCreateRequest) {
  return http.post(BASE_URL, {
    unitCode: data.unitCode,
    unitName: data.unitName,
    unitType: data.unitType || 'DEPARTMENT',
    parentId: data.parentId,
    sortOrder: data.sortOrder
  })
}

/**
 * 更新部门
 */
export function updateDepartment(id: number, data: DepartmentCreateRequest) {
  return http.put(`${BASE_URL}/${id}`, {
    unitName: data.unitName,
    leaderId: data.leaderId,
    deputyLeaderIds: data.deputyLeaderIds,
    sortOrder: data.sortOrder
  })
}

/**
 * 删除部门
 */
export function deleteDepartment(id: number) {
  return http.delete(`${BASE_URL}/${id}`)
}

/**
 * 获取部门详情
 */
export function getDepartmentById(id: number) {
  return http.get<DepartmentResponse>(`${BASE_URL}/${id}`)
}

/**
 * 获取部门树形结构
 */
export function getDepartmentTree() {
  return http.get<DepartmentResponse[]>(`${BASE_URL}/tree`)
}

/**
 * 查询部门列表
 */
export function listDepartments(params?: any) {
  return http.get<DepartmentResponse[]>(`${BASE_URL}/tree`, { params })
}

/**
 * 获取所有启用的部门
 */
export function getAllEnabledDepartments() {
  return http.get<DepartmentResponse[]>(`${BASE_URL}/tree`)
}

/**
 * 根据父部门ID获取子部门
 */
export function getDepartmentsByParentId(parentId: number) {
  return http.get<DepartmentResponse[]>(`${BASE_URL}/${parentId}/children`)
}

/**
 * 检查部门编码是否存在
 * 注意: V2暂无此端点,返回false
 */
export function existsDeptCode(deptCode: string, excludeId?: number) {
  return Promise.resolve(false)
}

/**
 * 更新部门状态 (启用)
 */
export function enableDepartment(id: number) {
  return http.put(`${BASE_URL}/${id}/enable`)
}

/**
 * 更新部门状态 (禁用)
 */
export function disableDepartment(id: number) {
  return http.put(`${BASE_URL}/${id}/disable`)
}

/**
 * 更新部门状态 (兼容V1)
 */
export function updateDepartmentStatus(id: number, status: number) {
  if (status === 1) {
    return enableDepartment(id)
  } else {
    return disableDepartment(id)
  }
}

/**
 * 根据类型获取组织单元
 */
export function getOrgUnitsByType(type: string) {
  return http.get<DepartmentResponse[]>(`${BASE_URL}/by-type/${type}`)
}
