/**
 * 统一访问关系类型定义 (Zanzibar Simplified)
 */

export interface AccessRelation {
  id: number | string
  resourceType: string
  resourceId: number | string
  relation: string
  subjectType: string
  subjectId: number | string
  includeChildren: boolean
  accessLevel: number
  metadata: Record<string, any> | null
  remark: string | null
  createdBy: number | string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface CreateAccessRelationRequest {
  resourceType: string
  resourceId: number | string
  relation: string
  subjectType: string
  subjectId: number | string
  includeChildren?: boolean
  accessLevel?: number
  metadata?: Record<string, any>
  remark?: string
}

export interface UpdateAccessRelationRequest {
  relation?: string
  accessLevel?: number
  includeChildren?: boolean
  metadata?: Record<string, any>
  remark?: string
}

/** 关系类型标签 */
export const RelationLabels: Record<string, string> = {
  owner: '拥有',
  manager: '管理',
  user: '使用',
  member: '成员',
  viewer: '查看',
  responsible: '负责人',
  occupant: '入住'
}

/** 主体类型标签 */
export const SubjectTypeLabels: Record<string, string> = {
  org_unit: '组织',
  user: '用户'
}

/** 资源类型标签 */
export const ResourceTypeLabels: Record<string, string> = {
  place: '场所',
  org_unit: '组织',
  student: '学生',
  class: '班级'
}
