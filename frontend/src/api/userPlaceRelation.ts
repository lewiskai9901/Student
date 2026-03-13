/**
 * 用户-场所关系 API
 */
import request from '@/utils/request'
import type {
  UserSpaceRelation,
  AddUserSpaceRelationRequest,
  UpdateUserSpaceRelationRequest
} from '@/types/userSpaceRelation'

const BASE_URL = '/v6/user-space-relations'

/**
 * 用户场所关系 API
 */
export const userSpaceRelationApi = {
  /**
   * 获取用户的所有场所关系
   */
  getUserRelations(userId: number): Promise<UserSpaceRelation[]> {
    return request.get(`${BASE_URL}/user/${userId}`)
  },

  /**
   * 获取用户的有效场所关系
   */
  getUserActiveRelations(userId: number): Promise<UserSpaceRelation[]> {
    return request.get(`${BASE_URL}/user/${userId}/active`)
  },

  /**
   * 获取用户的主要场所
   */
  getUserPrimaryRelation(userId: number): Promise<UserSpaceRelation | null> {
    return request.get(`${BASE_URL}/user/${userId}/primary`)
  },

  /**
   * 获取用户未缴费的场所
   */
  getUnpaidRelations(userId: number): Promise<UserSpaceRelation[]> {
    return request.get(`${BASE_URL}/user/${userId}/unpaid`)
  },

  /**
   * 获取场所的用户关系
   */
  getSpaceUsers(spaceId: number): Promise<UserSpaceRelation[]> {
    return request.get(`${BASE_URL}/space/${spaceId}`)
  },

  /**
   * 获取场所的分配用户
   */
  getSpaceAssignedUsers(spaceId: number): Promise<UserSpaceRelation[]> {
    return request.get(`${BASE_URL}/space/${spaceId}/assigned`)
  },

  /**
   * 获取场所已分配位置数
   */
  getAssignedPositionCount(spaceId: number): Promise<number> {
    return request.get(`${BASE_URL}/space/${spaceId}/assigned-count`)
  },

  /**
   * 获取关系详情
   */
  getRelation(id: number): Promise<UserSpaceRelation> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 添加用户场所关系
   */
  addRelation(data: AddUserSpaceRelationRequest): Promise<UserSpaceRelation> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新用户场所关系
   */
  updateRelation(id: number, data: UpdateUserSpaceRelationRequest): Promise<UserSpaceRelation> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 设置主要场所
   */
  setPrimary(userId: number, relationId: number): Promise<UserSpaceRelation> {
    return request.put(`${BASE_URL}/user/${userId}/primary/${relationId}`)
  },

  /**
   * 标记已缴费
   */
  markAsPaid(id: number): Promise<UserSpaceRelation> {
    return request.put(`${BASE_URL}/${id}/paid`)
  },

  /**
   * 删除用户场所关系
   */
  deleteRelation(id: number): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  }
}

export default userSpaceRelationApi
