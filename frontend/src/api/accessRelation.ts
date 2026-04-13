/**
 * 统一访问关系 API
 */
import { http as request } from '@/utils/request'
import type {
  AccessRelation,
  CreateAccessRelationRequest,
  UpdateAccessRelationRequest
} from '@/types/accessRelation'

const BASE_URL = '/access-relations'

export const accessRelationApi = {
  /**
   * 按资源查询关系
   */
  getByResource(resourceType: string, resourceId: number | string): Promise<AccessRelation[]> {
    return request.get(BASE_URL, { params: { resourceType, resourceId } })
  },

  /**
   * 按主体查询关系
   */
  getBySubject(subjectType: string, subjectId: number | string, resourceType?: string): Promise<AccessRelation[]> {
    return request.get(BASE_URL, { params: { subjectType, subjectId, resourceType } })
  },

  /**
   * 检查关系是否存在
   */
  check(params: {
    resourceType: string
    resourceId: number | string
    relation: string
    subjectType: string
    subjectId: number | string
  }): Promise<{ exists: boolean }> {
    return request.get(`${BASE_URL}/check`, { params })
  },

  /**
   * 创建关系
   */
  create(data: CreateAccessRelationRequest): Promise<AccessRelation> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新关系
   */
  update(id: number | string, data: UpdateAccessRelationRequest): Promise<void> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 删除关系
   */
  delete(id: number | string): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  },

  /**
   * 批量创建
   */
  batchCreate(data: CreateAccessRelationRequest[]): Promise<{ created: number }> {
    return request.post(`${BASE_URL}/batch`, data)
  },

  /**
   * 批量删除
   */
  batchDelete(ids: (number | string)[]): Promise<{ deleted: number }> {
    return request.delete(`${BASE_URL}/batch`, { data: { ids } })
  }
}

export default accessRelationApi
