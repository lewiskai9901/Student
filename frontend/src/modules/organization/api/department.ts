/**
 * 部门 API (兼容层)
 */
import { http } from '@/shared/utils/request'
import type { OrgUnit, DepartmentResponse, DepartmentCreateRequest } from '../types'

const BASE_URL = '/v2/org-units'

export function createDepartment(data: DepartmentCreateRequest): Promise<OrgUnit> {
  return http.post(BASE_URL, {
    unitCode: data.unitCode,
    unitName: data.unitName,
    unitType: data.unitType || 'DEPARTMENT',
    parentId: data.parentId,
    sortOrder: data.sortOrder
  })
}

export function updateDepartment(id: number, data: DepartmentCreateRequest): Promise<OrgUnit> {
  return http.put(`${BASE_URL}/${id}`, {
    unitName: data.unitName,
    leaderId: data.leaderId,
    deputyLeaderIds: data.deputyLeaderIds,
    sortOrder: data.sortOrder
  })
}

export function deleteDepartment(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

export function getDepartmentById(id: number): Promise<DepartmentResponse> {
  return http.get<DepartmentResponse>(`${BASE_URL}/${id}`)
}

export function getDepartmentTree(): Promise<DepartmentResponse[]> {
  return http.get<DepartmentResponse[]>(`${BASE_URL}/tree`)
}

export function getAllEnabledDepartments(): Promise<DepartmentResponse[]> {
  return http.get<DepartmentResponse[]>(`${BASE_URL}/tree`)
}

export function getDepartmentsByParentId(parentId: number): Promise<DepartmentResponse[]> {
  return http.get<DepartmentResponse[]>(`${BASE_URL}/${parentId}/children`)
}

export function enableDepartment(id: number): Promise<void> {
  return http.put(`${BASE_URL}/${id}/enable`)
}

export function disableDepartment(id: number): Promise<void> {
  return http.put(`${BASE_URL}/${id}/disable`)
}

export function updateDepartmentStatus(id: number, status: number): Promise<void> {
  return status === 1 ? enableDepartment(id) : disableDepartment(id)
}

// 兼容别名
export const getDepartmentList = getDepartmentTree
export const listDepartments = getDepartmentTree
export const existsDeptCode = (_code: string, _excludeId?: number) => Promise.resolve(false)

export const departmentApi = {
  create: createDepartment,
  update: updateDepartment,
  delete: deleteDepartment,
  getById: getDepartmentById,
  getTree: getDepartmentTree,
  getAllEnabled: getAllEnabledDepartments,
  getByParentId: getDepartmentsByParentId,
  enable: enableDepartment,
  disable: disableDepartment,
  updateStatus: updateDepartmentStatus
}
