import type { LongId } from '@/types/common'

/**
 * 检查平台 - 知识库类型
 */

export interface KnowledgeArticle {
  id: LongId
  tenantId?: LongId
  title: string
  content: string
  category: string | null
  tags: string | null
  relatedItemCodes: string | null // JSON array
  sourceCaseId: LongId | null
  viewCount: number
  helpfulCount: number
  isPinned: boolean
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface CreateArticleRequest {
  title: string
  content: string
  category?: string
  tags?: string
  relatedItemCodes?: string
  sourceCaseId?: LongId
}

export interface UpdateArticleRequest {
  title?: string
  content?: string
  category?: string
  tags?: string
  relatedItemCodes?: string
}

export interface CreateFromCaseRequest {
  title: string
  content: string
  category?: string
  tags?: string
}

// ==================== 纠正子任务 ====================

export type SubtaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'BLOCKED'

export interface CorrectiveSubtask {
  id: LongId
  tenantId?: LongId
  caseId: LongId
  subtaskName: string
  description: string | null
  assigneeId: LongId
  status: SubtaskStatus
  priority: number
  dueDate: string | null
  completedAt: string | null
  sortOrder: number
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface CreateSubtaskRequest {
  subtaskName: string
  description?: string
  assigneeId: LongId
  priority?: number
  dueDate?: string
  sortOrder?: number
}

export interface UpdateSubtaskRequest {
  subtaskName?: string
  description?: string
  assigneeId?: LongId
  priority?: number
  dueDate?: string
  sortOrder?: number
}
