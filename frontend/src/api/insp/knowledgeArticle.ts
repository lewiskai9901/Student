/**
 * V7 检查平台 - 知识库 API
 */
import { http } from '@/utils/request'
import type {
  KnowledgeArticle,
  CreateArticleRequest,
  UpdateArticleRequest,
  CreateFromCaseRequest,
  CorrectiveSubtask,
  CreateSubtaskRequest,
  UpdateSubtaskRequest,
} from '@/types/insp/knowledge'

const KB_BASE = '/v7/insp/knowledge-articles'
const CASE_BASE = '/v7/insp/corrective-cases'

// ==================== Knowledge Articles ====================

export function getArticles(params?: { keyword?: string; category?: string }): Promise<KnowledgeArticle[]> {
  return http.get<KnowledgeArticle[]>(KB_BASE, { params })
}

export function getArticle(id: number): Promise<KnowledgeArticle> {
  return http.get<KnowledgeArticle>(`${KB_BASE}/${id}`)
}

export function createArticle(data: CreateArticleRequest): Promise<KnowledgeArticle> {
  return http.post<KnowledgeArticle>(KB_BASE, data)
}

export function updateArticle(id: number, data: UpdateArticleRequest): Promise<KnowledgeArticle> {
  return http.put<KnowledgeArticle>(`${KB_BASE}/${id}`, data)
}

export function deleteArticle(id: number): Promise<void> {
  return http.delete(`${KB_BASE}/${id}`)
}

export function getPinnedArticles(): Promise<KnowledgeArticle[]> {
  return http.get<KnowledgeArticle[]>(`${KB_BASE}/pinned`)
}

export function markHelpful(id: number): Promise<void> {
  return http.post(`${KB_BASE}/${id}/helpful`)
}

export function pinArticle(id: number): Promise<void> {
  return http.put(`${KB_BASE}/${id}/pin`)
}

export function unpinArticle(id: number): Promise<void> {
  return http.put(`${KB_BASE}/${id}/unpin`)
}

export function createFromCase(caseId: number, data: CreateFromCaseRequest): Promise<KnowledgeArticle> {
  return http.post<KnowledgeArticle>(`${KB_BASE}/from-case/${caseId}`, data)
}

// ==================== Corrective Subtasks ====================

export function getSubtasks(caseId: number): Promise<CorrectiveSubtask[]> {
  return http.get<CorrectiveSubtask[]>(`${CASE_BASE}/${caseId}/subtasks`)
}

export function createSubtask(caseId: number, data: CreateSubtaskRequest): Promise<CorrectiveSubtask> {
  return http.post<CorrectiveSubtask>(`${CASE_BASE}/${caseId}/subtasks`, data)
}

export function updateSubtask(caseId: number, subtaskId: number, data: UpdateSubtaskRequest): Promise<CorrectiveSubtask> {
  return http.put<CorrectiveSubtask>(`${CASE_BASE}/${caseId}/subtasks/${subtaskId}`, data)
}

export function startSubtask(caseId: number, subtaskId: number): Promise<CorrectiveSubtask> {
  return http.put<CorrectiveSubtask>(`${CASE_BASE}/${caseId}/subtasks/${subtaskId}/start`)
}

export function completeSubtask(caseId: number, subtaskId: number): Promise<CorrectiveSubtask> {
  return http.put<CorrectiveSubtask>(`${CASE_BASE}/${caseId}/subtasks/${subtaskId}/complete`)
}

export function blockSubtask(caseId: number, subtaskId: number): Promise<CorrectiveSubtask> {
  return http.put<CorrectiveSubtask>(`${CASE_BASE}/${caseId}/subtasks/${subtaskId}/block`)
}

export function deleteSubtask(caseId: number, subtaskId: number): Promise<void> {
  return http.delete(`${CASE_BASE}/${caseId}/subtasks/${subtaskId}`)
}

export const inspKnowledgeApi = {
  getArticles,
  getArticle,
  createArticle,
  updateArticle,
  deleteArticle,
  getPinnedArticles,
  markHelpful,
  pinArticle,
  unpinArticle,
  createFromCase,
  getSubtasks,
  createSubtask,
  updateSubtask,
  startSubtask,
  completeSubtask,
  blockSubtask,
  deleteSubtask,
}
