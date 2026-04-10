/**
 * 组织成员管理 API（对接 /org-units/{id}/members 端点）
 */
import { http } from '@/utils/request'
import type { OrgMember, OrgStatistics } from '@/types/position'

const BASE = '/org-units'

export function getBelongingMembers(orgUnitId: number | string): Promise<OrgMember[]> {
  return http.get<OrgMember[]>(`${BASE}/${orgUnitId}/members`)
}

export function getMembersRecursive(orgUnitId: number | string): Promise<OrgMember[]> {
  return http.get<OrgMember[]>(`${BASE}/${orgUnitId}/members/recursive`)
}

export function getOrgStatistics(orgUnitId: number | string): Promise<OrgStatistics> {
  return http.get<OrgStatistics>(`${BASE}/${orgUnitId}/statistics`)
}

export function addMember(orgUnitId: number | string, userId: number | string): Promise<void> {
  return http.post(`${BASE}/${orgUnitId}/members/${userId}`)
}

export function removeMember(orgUnitId: number | string, userId: number | string): Promise<void> {
  return http.delete(`${BASE}/${orgUnitId}/members/${userId}`)
}

export const orgMemberApi = {
  getBelongingMembers,
  getOrgMembers: getBelongingMembers,
  getOrgMembersRecursive: getMembersRecursive,
  getOrgStatistics,
  addMember,
  removeMember,
}
