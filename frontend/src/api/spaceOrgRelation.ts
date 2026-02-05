/**
 * 场所-组织关系 API
 */
import request from '@/utils/request'
import type {
  SpaceOrgRelation,
  AddSpaceOrgRelationRequest,
  UpdateSpaceOrgRelationRequest
} from '@/types/spaceOrgRelation'

const BASE_URL = '/v6/space-org-relations'

/**
 * 场所组织关系 API
 */
export const spaceOrgRelationApi = {
  /**
   * 获取场所的所有组织关系
   */
  getSpaceRelations(spaceId: number): Promise<SpaceOrgRelation[]> {
    return request.get(`${BASE_URL}/space/${spaceId}`)
  },

  /**
   * 获取场所的有效组织关系
   */
  getSpaceActiveRelations(spaceId: number): Promise<SpaceOrgRelation[]> {
    return request.get(`${BASE_URL}/space/${spaceId}/active`)
  },

  /**
   * 获取场所的主归属
   */
  getSpacePrimaryRelation(spaceId: number): Promise<SpaceOrgRelation | null> {
    return request.get(`${BASE_URL}/space/${spaceId}/primary`)
  },

  /**
   * 获取组织管理的场所关系
   */
  getOrgSpaces(orgUnitId: number): Promise<SpaceOrgRelation[]> {
    return request.get(`${BASE_URL}/org/${orgUnitId}`)
  },

  /**
   * 获取组织的主管场所
   */
  getOrgPrimarySpaces(orgUnitId: number): Promise<SpaceOrgRelation[]> {
    return request.get(`${BASE_URL}/org/${orgUnitId}/primary`)
  },

  /**
   * 获取组织可检查的场所
   */
  getInspectableSpaces(orgUnitId: number): Promise<SpaceOrgRelation[]> {
    return request.get(`${BASE_URL}/org/${orgUnitId}/inspectable`)
  },

  /**
   * 获取共用场所关系
   */
  getSharedRelations(spaceId: number): Promise<SpaceOrgRelation[]> {
    return request.get(`${BASE_URL}/space/${spaceId}/shared`)
  },

  /**
   * 获取关系详情
   */
  getRelation(id: number): Promise<SpaceOrgRelation> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 添加场所组织关系
   */
  addRelation(data: AddSpaceOrgRelationRequest): Promise<SpaceOrgRelation> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新场所组织关系
   */
  updateRelation(id: number, data: UpdateSpaceOrgRelationRequest): Promise<SpaceOrgRelation> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 设置主归属
   */
  setPrimary(spaceId: number, relationId: number): Promise<SpaceOrgRelation> {
    return request.put(`${BASE_URL}/space/${spaceId}/primary/${relationId}`)
  },

  /**
   * 删除场所组织关系
   */
  deleteRelation(id: number): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  }
}

export default spaceOrgRelationApi
