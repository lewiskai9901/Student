import request from '@/utils/request'
import type {
  UniversalSpace,
  SpaceTreeNode,
  CreateSpaceRequest,
  UpdateSpaceRequest,
  UniversalSpaceType
} from '@/types/universalSpace'

const BASE_URL = '/v9/spaces'

/**
 * 空间统计数据
 */
export interface SpaceStatistics {
  totalCount: number
  totalCapacity: number
  totalOccupancy: number
  countByType: Record<string, number>
  countByStatus: Record<number, number>
  occupancyRate: number
}

/**
 * 通用空间管理 API
 */
export const universalSpaceApi = {
  /**
   * 获取空间树
   */
  getTree(): Promise<SpaceTreeNode[]> {
    return request.get(`${BASE_URL}/tree`)
  },

  /**
   * 获取指定类型的空间树
   */
  getTreeByType(typeCode: string): Promise<SpaceTreeNode[]> {
    return request.get(`${BASE_URL}/tree/type/${typeCode}`)
  },

  /**
   * 获取空间详情
   */
  getById(id: number): Promise<UniversalSpace> {
    return request.get(`${BASE_URL}/${id}`)
  },

  /**
   * 根据编码获取空间
   */
  getByCode(spaceCode: string): Promise<UniversalSpace> {
    return request.get(`${BASE_URL}/code/${spaceCode}`)
  },

  /**
   * 获取子空间列表
   */
  getChildren(parentId: number): Promise<UniversalSpace[]> {
    return request.get(`${BASE_URL}/${parentId}/children`)
  },

  /**
   * 获取根空间列表
   */
  getRootChildren(): Promise<UniversalSpace[]> {
    return request.get(`${BASE_URL}/roots/children`)
  },

  /**
   * 获取允许创建的子类型（根空间）
   */
  getAllowedChildTypesForRoot(): Promise<UniversalSpaceType[]> {
    return request.get(`${BASE_URL}/allowed-child-types`)
  },

  /**
   * 获取允许创建的子类型
   */
  getAllowedChildTypes(parentId: number): Promise<UniversalSpaceType[]> {
    return request.get(`${BASE_URL}/${parentId}/allowed-child-types`)
  },

  /**
   * 获取统计数据
   */
  getStatistics(): Promise<SpaceStatistics> {
    return request.get(`${BASE_URL}/statistics`)
  },

  /**
   * 创建空间
   */
  create(data: CreateSpaceRequest): Promise<UniversalSpace> {
    return request.post(BASE_URL, data)
  },

  /**
   * 更新空间
   */
  update(id: number, data: UpdateSpaceRequest): Promise<UniversalSpace> {
    return request.put(`${BASE_URL}/${id}`, data)
  },

  /**
   * 更改空间状态
   */
  changeStatus(id: number, status: number): Promise<UniversalSpace> {
    return request.put(`${BASE_URL}/${id}/status`, { status })
  },

  /**
   * 删除空间
   */
  delete(id: number): Promise<void> {
    return request.delete(`${BASE_URL}/${id}`)
  }
}

export default universalSpaceApi
