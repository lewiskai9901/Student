/**
 * 检查平台 - 等级方案 API
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type {
  GradeScheme,
  CreateGradeSchemeRequest,
  UpdateGradeSchemeRequest,
  CloneGradeSchemeRequest,
} from '@/types/insp/gradeScheme'

const BASE = '/inspection/grade-schemes'

// ==================== 等级方案 CRUD ====================

export function getGradeSchemes(): Promise<GradeScheme[]> {
  return http.get<GradeScheme[]>(BASE)
}

export function getGradeScheme(id: LongId): Promise<GradeScheme> {
  return http.get<GradeScheme>(`${BASE}/${id}`)
}

export function createGradeScheme(data: CreateGradeSchemeRequest): Promise<GradeScheme> {
  return http.post<GradeScheme>(BASE, data)
}

export function cloneGradeScheme(data: CloneGradeSchemeRequest): Promise<GradeScheme> {
  return http.post<GradeScheme>(`${BASE}/clone`, data)
}

export function updateGradeScheme(id: LongId, data: UpdateGradeSchemeRequest): Promise<GradeScheme> {
  return http.put<GradeScheme>(`${BASE}/${id}`, data)
}

export function deleteGradeScheme(id: LongId): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== API 对象 ====================

export const gradeSchemeApi = {
  getList: getGradeSchemes,
  getById: getGradeScheme,
  create: createGradeScheme,
  clone: cloneGradeScheme,
  update: updateGradeScheme,
  delete: deleteGradeScheme,
}
