import type { LongId } from '@/types/common'

/**
 * 统一访问关系类型定义 (Zanzibar Simplified)
 */

/**
 * 访问级别 —— 与后端 AccessLevel 枚举一致 (VARCHAR 存储, 按 name() 序列化)。
 * 后端永远回传字符串 'READ_ONLY' | 'FULL' | 'OWNER', 不是数字。
 */
export type AccessLevel = 'READ_ONLY' | 'FULL' | 'OWNER'

export interface AccessRelation {
  id: LongId | string
  resourceType: string
  resourceId: LongId | string
  relation: string
  subjectType: string
  subjectId: LongId | string
  accessLevel: AccessLevel
  metadata: Record<string, any> | null
  remark: string | null
  createdBy: number | string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface CreateAccessRelationRequest {
  resourceType: string
  resourceId: LongId | string
  relation: string
  subjectType: string
  subjectId: LongId | string
  accessLevel?: AccessLevel
  metadata?: Record<string, any>
  remark?: string
}

export interface UpdateAccessRelationRequest {
  relation?: string
  accessLevel?: AccessLevel
  metadata?: Record<string, any>
  remark?: string
}

/** 访问级别标签 */
export const AccessLevelLabels: Record<AccessLevel, string> = {
  READ_ONLY: '只读',
  FULL: '读写',
  OWNER: '拥有'
}

/**
 * 关系类型标签 (核心通用关系码 → 中文)。
 * 与 CoreManifest 声明的关系码对齐; 行业关系(teaches/mentor_of/family_of 等)
 * 由 relation_types 字典的 relation_name 提供, 不在此核心静态表硬编码。
 */
export const RelationLabels: Record<string, string> = {
  member: '成员',
  admin: '管理员',
  deputy: '副职',
  manages: '管理',
  belongs_to: '归属',
  occupies: '占用',
  delegated_to: '委派',
  watches: '关注',
  viewer: '查看',
  responsible_for: '负责人'
}

/** 主体类型标签 */
export const SubjectTypeLabels: Record<string, string> = {
  org_unit: '组织',
  user: '用户',
  place: '场所'
}

/** 资源类型标签 */
export const ResourceTypeLabels: Record<string, string> = {
  place: '场所',
  org_unit: '组织',
  user: '用户'
}
