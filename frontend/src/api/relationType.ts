/**
 * 关系类型字典 API (v3)
 */
import { http as request } from '@/utils/request'

export interface RelationTypeDef {
  relationCode: string
  fromType: string
  toType: string
  relationName: string
  isTransitive: number
  category: string        // OWNERSHIP/MEMBERSHIP/ASSOCIATION/DELEGATION/SUBSCRIPTION
  tier: string            // CORE / COMMON_EXT / DOMAIN
  registeredBy: string
  description?: string
  /** 受资源容量限制(如 occupies 受 place.capacity 限制) */
  capacityBound?: number
  /** 每个资源最多 N 个主体(全局默认,null=无限制) */
  maxPerResource?: number | null
  /** 按资源子类型细化的上限,如 { CLASS: 1, GRADE: 3 } (优先于 maxPerResource) */
  maxBySubtype?: Record<string, number> | null
  /**
   * 插件级启用状态 (两状态模型, Phase 2 加).
   * false = 所属插件被禁 > 前端应灰显.
   */
  pluginEnabled?: boolean | number
}

const BASE = '/relation-types'

export const relationTypeApi = {
  /**
   * @param includeDisabled 管理员视角: true 返回所属插件被禁的关系 (pluginEnabled=false)
   */
  list(params?: { tier?: string; fromType?: string; toType?: string; includeDisabled?: boolean }): Promise<RelationTypeDef[]> {
    return request.get(BASE, { params })
  },

  listByTier(params?: { includeDisabled?: boolean }): Promise<Record<string, RelationTypeDef[]>> {
    return request.get(`${BASE}/tiers`, { params })
  }
}

export default relationTypeApi
