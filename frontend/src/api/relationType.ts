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
}

const BASE = '/relation-types'

export const relationTypeApi = {
  list(params?: { tier?: string; fromType?: string; toType?: string }): Promise<RelationTypeDef[]> {
    return request.get(BASE, { params })
  },

  listByTier(): Promise<Record<string, RelationTypeDef[]>> {
    return request.get(`${BASE}/tiers`)
  }
}

export default relationTypeApi
