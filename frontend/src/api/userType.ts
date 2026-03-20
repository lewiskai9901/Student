/**
 * 用户类型 API (统一类型系统 Phase 2)
 * 端点: /user-types
 */
import { createTypeApi } from './typeApiFactory'
import type { UserType, UserTypeTreeNode, UserCategoryInfo, CreateUserTypeRequest, UpdateUserTypeRequest } from '@/types/userType'

const base = createTypeApi<UserType, CreateUserTypeRequest, UpdateUserTypeRequest>('/user-types')

export const userTypeApi = {
  ...base,
}

// Named exports for backward compatibility
export const getUserCategories = base.getCategories
export const getAllUserTypes = base.getAll
export const getEnabledUserTypes = base.getEnabled
export const getUserTypeTree = base.getTree as () => Promise<UserTypeTreeNode[]>
export const getUserTypeById = base.getById
export const getUserTypeByCode = base.getByCode
export const createUserType = base.create
export const updateUserType = base.update
export const deleteUserType = base.delete
export const enableUserType = base.enable
export const disableUserType = base.disable
