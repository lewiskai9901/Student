/**
 * 用户-组织关系 API
 */
import request from '@/utils/request'
import type {
  UserOrgRelation,
  AddUserOrgRelationRequest,
  UpdateUserOrgRelationRequest
} from '@/types/userOrgRelation'

const BASE_URL = '/v6/user-org-relations'

/**
 * 用户组织关系 API
 */
export const userOrgRelationApi = {
  /**
   * 获取用户的所有组织关系
   */
  getUserRelations(userId: number): Promise<UserOrgRelation[]> {
    return request.get(`${BASE_URL}/user/${userId}`)
  },

  /**
   * 获取用户的有效组织关系
   */
  getUserActiveRelations(userId: number): Promise<UserOrgRelation[]> {
    return request.get(`${BASE_URL}/user/${userId}/active`)
  },

  /**
   * 获取用户的主归属
   */
  getUserPrimaryRelation(userId: number): Promise<UserOrgRelation | null> {
    return request.get(`${BASE_URL}/user/${userId}/primary`)
  },

  /**
   * 获取组织的成员关系
   */
  getOrgMembers(orgUnitId: number): Promise<UserOrgRelation[]> {
    return request.get(`${BASE_URL}/org/${orgUnitId}`)
  },

  /**
   * 获取组织的领导
   */
  getOrgLeaders(orgUnitId: number): Promise<UserOrgRelation[]> {
    return request.get(`${BASE_URL}/org/${orgUnitId}/leaders`)
  },

  /**
   * 获取关系详情
   */
  getRelation(id: number): Promise<UserOrgRelation> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 添加用户组织关系
   */
  addRelation(data: AddUserOrgRelationRequest): Promise<UserOrgRelation> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新用户组织关系
   */
  updateRelation(id: number, data: UpdateUserOrgRelationRequest): Promise<UserOrgRelation> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 设置主归属
   */
  setPrimary(userId: number, relationId: number): Promise<UserOrgRelation> {
    return request.put(`${BASE_URL}/user/${userId}/primary/${relationId}`)
  },

  /**
   * 删除用户组织关系
   */
  deleteRelation(id: number): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  }
}

export default userOrgRelationApi
