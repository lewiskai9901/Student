import request from '@/utils/request'
import type {
  UniversalSpaceType,
  SpaceTypeTreeNode,
  CreateSpaceTypeRequest,
  UpdateSpaceTypeRequest
} from '@/types/universalSpace'

const BASE_URL = '/v9/space-types'

/**
 * 通用空间类型 API
 */
export const universalSpaceTypeApi = {
  /**
   * 获取所有空间类型
   */
  getAll(): Promise<UniversalSpaceType[]> {
    return request.get(BASE_URL)
  },

  /**
   * 获取所有启用的空间类型
   */
  getEnabled(): Promise<UniversalSpaceType[]> {
    return request.get(`${BASE_URL}/enabled`)
  },

  /**
   * 获取所有根类型
   */
  getRootTypes(): Promise<UniversalSpaceType[]> {
    return request.get(`${BASE_URL}/root`)
  },

  /**
   * 获取空间类型树
   */
  getTree(): Promise<SpaceTypeTreeNode[]> {
    return request.get(`${BASE_URL}/tree`)
  },

  /**
   * 获取允许的子类型
   */
  getAllowedChildTypes(parentTypeCode: string): Promise<UniversalSpaceType[]> {
    return request.get(`${BASE_URL}/${parentTypeCode}/children`)
  },

  /**
   * 根据ID获取空间类型
   */
  getById(id: number): Promise<UniversalSpaceType> {
    return request.get(`${BASE_URL}/id/${id}`)
  },

  /**
   * 根据编码获取空间类型
   */
  getByCode(typeCode: string): Promise<UniversalSpaceType> {
    return request.get(`${BASE_URL}/code/${typeCode}`)
  },

  /**
   * 创建空间类型
   */
  create(data: CreateSpaceTypeRequest): Promise<UniversalSpaceType> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新空间类型
   */
  update(id: number, data: UpdateSpaceTypeRequest): Promise<UniversalSpaceType> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 删除空间类型
   */
  delete(id: number): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  },

  /**
   * 启用空间类型
   */
  enable(id: number): Promise<UniversalSpaceType> {
    return request.put(`${BASE_URL}/${id}/enable`)
  },

  /**
   * 禁用空间类型
   */
  disable(id: number): Promise<UniversalSpaceType> {
    return request.put(`${BASE_URL}/${id}/disable`)
  }
}

export default universalSpaceTypeApi
