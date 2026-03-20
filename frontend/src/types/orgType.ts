/**
 * 组织类型 类型定义 (统一类型系统 Phase 1)
 * API: /org-types
 */

import type { ConfigurableType, TypeTreeNode, CategoryInfo } from './configurableType'

// ==================== Category ====================

export type OrgCategoryInfo = CategoryInfo

// ==================== 岗位模板 ====================

export interface PositionTemplate {
  positionName: string
  sortOrder: number
}

// ==================== 核心类型 ====================

export interface OrgType extends ConfigurableType {
  // 跨领域关联（OrgType 特有）
  defaultUserTypeCodes: string[] | null
  defaultPlaceTypeCodes: string[] | null
  defaultPositions: PositionTemplate[] | null
}

export type OrgTypeTreeNode = TypeTreeNode<OrgType>

// ==================== Request ====================

export interface CreateOrgTypeRequest {
  typeCode: string
  typeName: string
  category?: string
  parentTypeCode?: string
  icon?: string
  description?: string
  features?: Record<string, boolean>
  metadataSchema?: string
  allowedChildTypeCodes?: string[]
  maxDepth?: number
  defaultUserTypeCodes?: string[]
  defaultPlaceTypeCodes?: string[]
  defaultPositions?: PositionTemplate[]
  sortOrder?: number
}

export interface UpdateOrgTypeRequest {
  typeName?: string
  category?: string
  icon?: string
  description?: string
  features?: Record<string, boolean>
  metadataSchema?: string
  allowedChildTypeCodes?: string[]
  maxDepth?: number
  defaultUserTypeCodes?: string[]
  defaultPlaceTypeCodes?: string[]
  defaultPositions?: PositionTemplate[]
  sortOrder?: number
}

// ==================== Feature Keys ====================

export const ORG_FEATURE_KEYS = {
  DATA_PERMISSION_BOUNDARY: 'dataPermissionBoundary',
  INSPECTION_TARGET: 'inspectionTarget',
  MEMBER_MANAGEMENT: 'memberManagement',
  ATTENDANCE: 'attendance',
  SCHEDULING: 'scheduling'
} as const

export const ORG_FEATURE_LABELS: Record<string, string> = {
  dataPermissionBoundary: '数据权限边界',
  inspectionTarget: '可被检查',
  memberManagement: '成员管理',
  attendance: '考勤',
  scheduling: '排程管理'
}

// ==================== Category 常量 ====================

export const ORG_CATEGORIES = {
  ROOT: 'ROOT',
  BRANCH: 'BRANCH',
  FUNCTIONAL: 'FUNCTIONAL',
  GROUP: 'GROUP',
  CONTAINER: 'CONTAINER'
} as const

export const ORG_CATEGORY_LABELS: Record<string, string> = {
  ROOT: '根组织',
  BRANCH: '分支机构',
  FUNCTIONAL: '职能部门',
  GROUP: '成员组',
  CONTAINER: '容器'
}
