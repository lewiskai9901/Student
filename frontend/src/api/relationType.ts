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
