/**
 * V7 检查平台 - 知识库类型
 */

export interface KnowledgeArticle {
  id: number
  tenantId?: number
  title: string
  content: string
  category: string | null
  tags: string | null
  relatedItemCodes: string | null // JSON array
  sourceCaseId: number | null
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
  sourceCaseId?: number
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
  id: number
  tenantId?: number
  caseId: number
  subtaskName: string
  description: string | null
  assigneeId: number
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
  assigneeId: number
  priority?: number
  dueDate?: string
  sortOrder?: number
}

export interface UpdateSubtaskRequest {
  subtaskName?: string
  description?: string
  assigneeId?: number
  priority?: number
  dueDate?: string
  sortOrder?: number
}
