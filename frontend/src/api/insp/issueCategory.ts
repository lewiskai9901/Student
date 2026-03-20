/**
 * V7 检查平台 - 问题分类 API
 */
import { http } from '@/utils/request'
import type { IssueCategory } from '@/types/insp/platform'

const BASE = '/v7/insp/issue-categories'

// ==================== CRUD ====================

export function listIssueCategories(): Promise<IssueCategory[]> {
  return http.get<IssueCategory[]>(BASE)
}

export function getRootCategories(): Promise<IssueCategory[]> {
  return http.get<IssueCategory[]>(`${BASE}/roots`)
}

export function getChildCategories(parentId: number): Promise<IssueCategory[]> {
  return http.get<IssueCategory[]>(`${BASE}/${parentId}/children`)
}

export function createIssueCategory(data: Partial<IssueCategory>): Promise<IssueCategory> {
  return http.post<IssueCategory>(BASE, data)
}

export function updateIssueCategory(id: number, data: Partial<IssueCategory>): Promise<IssueCategory> {
  return http.put<IssueCategory>(`${BASE}/${id}`, data)
}

export function deleteIssueCategory(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== API 对象 ====================

export const issueCategoryApi = {
  list: listIssueCategories,
  getRoots: getRootCategories,
  getChildren: getChildCategories,
  create: createIssueCategory,
  update: updateIssueCategory,
  delete: deleteIssueCategory,
}
