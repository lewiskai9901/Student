/**
 * 用户类型 类型定义 (统一类型系统 Phase 2)
 * API: /user-types
 */

import type { ConfigurableType, TypeTreeNode, CategoryInfo } from './configurableType'

// ==================== Category ====================

export type UserCategoryInfo = CategoryInfo

// ==================== 核心类型 ====================

export interface UserType extends ConfigurableType {
  // 跨领域关联（UserType 特有）
  defaultRoleCodes: string[] | null
  defaultOrgTypeCodes: string[] | null
  defaultPlaceTypeCodes: string[] | null
}

export type UserTypeTreeNode = TypeTreeNode<UserType>

// ==================== Request ====================

export interface CreateUserTypeRequest {
  typeName: string
  category?: string
  parentTypeCode?: string
  icon?: string
  description?: string
  features?: Record<string, boolean>
  metadataSchema?: string
  allowedChildTypeCodes?: string[]
  maxDepth?: number
  defaultRoleCodes?: string[]
  defaultOrgTypeCodes?: string[]
  defaultPlaceTypeCodes?: string[]
  sortOrder?: number
}

export interface UpdateUserTypeRequest {
  typeName?: string
  category?: string
  icon?: string
  description?: string
  features?: Record<string, boolean>
  metadataSchema?: string
  allowedChildTypeCodes?: string[]
  maxDepth?: number
  defaultRoleCodes?: string[]
  defaultOrgTypeCodes?: string[]
  defaultPlaceTypeCodes?: string[]
  sortOrder?: number
}

// ==================== Feature Keys ====================

export const USER_FEATURE_KEYS = {
  REQUIRES_ORG: 'requiresOrg',
  REQUIRES_PLACE: 'requiresPlace'
} as const

export const USER_FEATURE_LABELS: Record<string, string> = {
  requiresOrg: '需要归属组织',
  requiresPlace: '需要关联场所'
}

// ==================== Category 常量 ====================

export const USER_CATEGORIES = {
  ADMIN: 'ADMIN',
  STAFF: 'STAFF',
  MEMBER: 'MEMBER',
  EXTERNAL: 'EXTERNAL'
} as const

export const USER_CATEGORY_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  STAFF: '职工',
  MEMBER: '成员',
  EXTERNAL: '外部人员'
}
