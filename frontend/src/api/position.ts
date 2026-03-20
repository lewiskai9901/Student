/**
 * 岗位管理 API
 */
import { http } from '@/utils/request'
import type {
  Position,
  PositionStaffing,
  UserPosition,
  OrgMember,
  OrgStatistics,
  CreatePositionRequest,
  UpdatePositionRequest,
  AppointRequest,
  EndAppointmentRequest,
  TransferRequest,
} from '@/types/position'

const POSITION_URL = '/positions'
const USER_POSITION_URL = '/user-positions'

// ==================== 岗位 API ====================

export function getPositionsByOrgUnit(orgUnitId: number | string): Promise<Position[]> {
  return http.get<Position[]>(POSITION_URL, { params: { orgUnitId } })
}

export function getPosition(id: number | string): Promise<Position> {
  return http.get<Position>(`${POSITION_URL}/${id}`)
}

export function createPosition(data: CreatePositionRequest): Promise<Position> {
  return http.post<Position>(POSITION_URL, data)
}

export function updatePosition(id: number | string, data: UpdatePositionRequest): Promise<Position> {
  return http.put<Position>(`${POSITION_URL}/${id}`, data)
}

export function deletePosition(id: number | string): Promise<void> {
  return http.delete(`${POSITION_URL}/${id}`)
}

export function enablePosition(id: number | string): Promise<void> {
  return http.put(`${POSITION_URL}/${id}/enable`)
}

export function disablePosition(id: number | string): Promise<void> {
  return http.put(`${POSITION_URL}/${id}/disable`)
}

export function getPositionHolders(id: number | string): Promise<UserPosition[]> {
  return http.get<UserPosition[]>(`${POSITION_URL}/${id}/holders`)
}

export function getPositionStaffing(orgUnitId: number | string): Promise<PositionStaffing[]> {
  return http.get<PositionStaffing[]>(`${POSITION_URL}/staffing`, { params: { orgUnitId } })
}

// ==================== 用户岗位 API ====================

export function appointUser(data: AppointRequest): Promise<UserPosition> {
  return http.post<UserPosition>(`${USER_POSITION_URL}/appoint`, data)
}

export function endAppointment(id: number | string, data: EndAppointmentRequest): Promise<void> {
  return http.put(`${USER_POSITION_URL}/${id}/end`, data)
}

export function transferUser(data: TransferRequest): Promise<void> {
  return http.post(`${USER_POSITION_URL}/transfer`, data)
}

export function getUserPositions(userId: number | string): Promise<UserPosition[]> {
  return http.get<UserPosition[]>(USER_POSITION_URL, { params: { userId } })
}

export function getUserPrimaryPosition(userId: number | string): Promise<UserPosition> {
  return http.get<UserPosition>(`${USER_POSITION_URL}/primary`, { params: { userId } })
}

export function getOrgMembers(orgUnitId: number | string): Promise<OrgMember[]> {
  return http.get<OrgMember[]>(`${USER_POSITION_URL}/org-members`, { params: { orgUnitId } })
}

export function getBelongingMembers(orgUnitId: number | string): Promise<OrgMember[]> {
  return http.get<OrgMember[]>(`${USER_POSITION_URL}/belonging-members`, { params: { orgUnitId } })
}

export function getOrgMembersRecursive(orgUnitId: number | string): Promise<OrgMember[]> {
  return http.get<OrgMember[]>(`${USER_POSITION_URL}/org-members-recursive`, { params: { orgUnitId } })
}

export function getOrgStatistics(orgUnitId: number | string): Promise<OrgStatistics> {
  return http.get<OrgStatistics>(`${USER_POSITION_URL}/org-statistics`, { params: { orgUnitId } })
}

export function addMemberToOrg(orgUnitId: number | string, userId: number | string): Promise<void> {
  return http.post(`${USER_POSITION_URL}/add-member`, { orgUnitId, userId })
}

export function removeMemberFromOrg(orgUnitId: number | string, userId: number | string): Promise<void> {
  return http.post(`${USER_POSITION_URL}/remove-member`, { orgUnitId, userId })
}

// ==================== API 对象封装 ====================

export const positionApi = {
  getByOrgUnit: getPositionsByOrgUnit,
  getById: getPosition,
  create: createPosition,
  update: updatePosition,
  delete: deletePosition,
  enable: enablePosition,
  disable: disablePosition,
  getHolders: getPositionHolders,
  getStaffing: getPositionStaffing,
}

export const userPositionApi = {
  appoint: appointUser,
  endAppointment,
  transfer: transferUser,
  getByUser: getUserPositions,
  getPrimary: getUserPrimaryPosition,
  getOrgMembers,
  getOrgMembersRecursive,
  getBelongingMembers,
  getOrgStatistics,
  addMember: addMemberToOrg,
  removeMember: removeMemberFromOrg,
}

