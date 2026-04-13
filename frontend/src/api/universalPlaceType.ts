/**
 * 通用场所类型 API（统一类型系统 Phase 3）
 * 端点: /place-types
 */
import { http } from '@/utils/request'
import { createTypeApi } from './typeApiFactory'
import type {
  UniversalPlaceType,
  CreatePlaceTypeRequest,
  UpdatePlaceTypeRequest,
  PlaceCategory
} from '@/types/universalPlace'

const base = createTypeApi<UniversalPlaceType, CreatePlaceTypeRequest, UpdatePlaceTypeRequest>('/place-types')

export const universalPlaceTypeApi = {
  ...base,
  // Override categories to use PlaceCategory (has extra fields: allowedChildCategories, leaf, root)
  getCategories(): Promise<PlaceCategory[]> {
    return http.get(`/place-types/categories`)
  },
  // Extra PlaceType-specific endpoint
  getRootTypes(): Promise<UniversalPlaceType[]> {
    return http.get(`/place-types/root`)
  },
}

export default universalPlaceTypeApi
