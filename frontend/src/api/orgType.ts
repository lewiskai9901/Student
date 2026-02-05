/**
 * 组织类型 API (重构后使用 org-unit-types 端点)
 */
import { http } from '@/utils/request'
import type { OrgType, OrgTypeTreeNode, CreateOrgTypeRequest, UpdateOrgTypeRequest } from '@/types/orgType'

const BASE_URL = '/org-unit-types'

/**
 * 获取所有组织类型
 */
export function getAllOrgTypes(): Promise<OrgType[]> {
  return http.get<OrgType[]>(BASE_URL)
}

/**
 * 获取所有启用的组织类型
 */
export function getEnabledOrgTypes(): Promise<OrgType[]> {
  return http.get<OrgType[]>(`${BASE_URL}/enabled`)
}

/**
 * 获取组织类型树
 */
export function getOrgTypeTree(): Promise<OrgTypeTreeNode[]> {
  return http.get<OrgTypeTreeNode[]>(`${BASE_URL}/tree`)
}

/**
 * 获取教学单位类型
 */
export function getAcademicTypes(): Promise<OrgType[]> {
  return http.get<OrgType[]>(`${BASE_URL}/academic`)
}

/**
 * 获取职能部门类型
 */
export function getFunctionalTypes(): Promise<OrgType[]> {
  return http.get<OrgType[]>(`${BASE_URL}/functional`)
}

/**
 * 获取可检查的类型
 */
export function getInspectableTypes(): Promise<OrgType[]> {
  return http.get<OrgType[]>(`${BASE_URL}/inspectable`)
}

/**
 * 获取组织类型详情
 */
export function getOrgTypeById(id: number): Promise<OrgType> {
  return http.get<OrgType>(`${BASE_URL}/${id}`)
}

/**
 * 根据编码获取组织类型
 */
export function getOrgTypeByCode(typeCode: string): Promise<OrgType> {
  return http.get<OrgType>(`${BASE_URL}/code/${typeCode}`)
}

/**
 * 创建组织类型
 */
export function createOrgType(data: CreateOrgTypeRequest): Promise<OrgType> {
  return http.post<OrgType>(BASE_URL, data)
}

/**
 * 更新组织类型
 */
export function updateOrgType(id: number, data: UpdateOrgTypeRequest): Promise<OrgType> {
  return http.put<OrgType>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除组织类型
 */
export function deleteOrgType(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

/**
 * 启用组织类型
 */
export function enableOrgType(id: number): Promise<OrgType> {
  return http.put<OrgType>(`${BASE_URL}/${id}/enable`)
}

/**
 * 禁用组织类型
 */
export function disableOrgType(id: number): Promise<OrgType> {
  return http.put<OrgType>(`${BASE_URL}/${id}/disable`)
}

export const orgTypeApi = {
  getAll: getAllOrgTypes,
  getEnabled: getEnabledOrgTypes,
  getTree: getOrgTypeTree,
  getAcademic: getAcademicTypes,
  getFunctional: getFunctionalTypes,
  getInspectable: getInspectableTypes,
  getById: getOrgTypeById,
  getByCode: getOrgTypeByCode,
  create: createOrgType,
  update: updateOrgType,
  delete: deleteOrgType,
  enable: enableOrgType,
  disable: disableOrgType
}
