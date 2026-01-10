/**
 * 组织单元 API
 */
import { http } from '@/shared/utils/request'
import type { OrgUnit, OrgUnitTreeNode, CreateOrgUnitRequest, UpdateOrgUnitRequest } from '../types'

const BASE_URL = '/v2/org-units'

export function getOrgUnits(): Promise<OrgUnit[]> {
  return http.get<OrgUnit[]>(BASE_URL)
}

export function getOrgUnitTree(): Promise<OrgUnitTreeNode[]> {
  return http.get<OrgUnitTreeNode[]>(`${BASE_URL}/tree`)
}

export function getOrgUnit(id: number): Promise<OrgUnit> {
  return http.get<OrgUnit>(`${BASE_URL}/${id}`)
}

export function createOrgUnit(data: CreateOrgUnitRequest): Promise<OrgUnit> {
  return http.post<OrgUnit>(BASE_URL, data)
}

export function updateOrgUnit(id: number, data: UpdateOrgUnitRequest): Promise<OrgUnit> {
  return http.put<OrgUnit>(`${BASE_URL}/${id}`, data)
}

export function deleteOrgUnit(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

export function enableOrgUnit(id: number): Promise<void> {
  return http.put(`${BASE_URL}/${id}/enable`)
}

export function disableOrgUnit(id: number): Promise<void> {
  return http.put(`${BASE_URL}/${id}/disable`)
}

export function assignLeader(id: number, leaderId: number): Promise<void> {
  return http.put(`${BASE_URL}/${id}/leader`, { leaderId })
}

export function getOrgUnitsByType(type: string): Promise<OrgUnit[]> {
  return http.get<OrgUnit[]>(`${BASE_URL}/by-type/${type}`)
}

export function getOrgUnitChildren(id: number): Promise<OrgUnit[]> {
  return http.get<OrgUnit[]>(`${BASE_URL}/${id}/children`)
}

export const orgUnitApi = {
  getList: getOrgUnits,
  getTree: getOrgUnitTree,
  getById: getOrgUnit,
  getByType: getOrgUnitsByType,
  getChildren: getOrgUnitChildren,
  create: createOrgUnit,
  update: updateOrgUnit,
  delete: deleteOrgUnit,
  enable: enableOrgUnit,
  disable: disableOrgUnit,
  assignLeader
}
