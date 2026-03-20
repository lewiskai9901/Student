/**
 * 组织类型 API (统一类型系统 Phase 1)
 * 端点: /org-types
 */
import { http } from '@/utils/request'
import { createTypeApi } from './typeApiFactory'
import type { OrgType, OrgTypeTreeNode, OrgCategoryInfo, CreateOrgTypeRequest, UpdateOrgTypeRequest } from '@/types/orgType'

const base = createTypeApi<OrgType, CreateOrgTypeRequest, UpdateOrgTypeRequest>('/org-types')

// Extra OrgType-specific endpoints
function getInspectableTypes(): Promise<OrgType[]> {
  return http.get<OrgType[]>('/org-types/inspectable')
}

export const orgTypeApi = {
  ...base,
  getInspectable: getInspectableTypes,
}

// Named exports for backward compatibility
export const getOrgCategories = base.getCategories
export const getAllOrgTypes = base.getAll
export const getEnabledOrgTypes = base.getEnabled
export const getOrgTypeTree = base.getTree as () => Promise<OrgTypeTreeNode[]>
export const getOrgTypeById = base.getById
export const getOrgTypeByCode = base.getByCode
export const createOrgType = base.create
export const updateOrgType = base.update
export const deleteOrgType = base.delete
export const enableOrgType = base.enable
export const disableOrgType = base.disable
export { getInspectableTypes }
