/**
 * V7 检查平台 - 选项集 API
 */
import { http } from '@/utils/request'
import type {
  ResponseSet,
  ResponseSetOption,
  CreateResponseSetRequest,
  UpdateResponseSetRequest,
  CreateOptionRequest,
  UpdateOptionRequest,
  InspPageResult,
} from '@/types/insp/template'

const BASE = '/v7/insp/response-sets'

// ==================== 选项集 CRUD ====================

export function getResponseSets(params?: {
  page?: number
  size?: number
  keyword?: string
}): Promise<InspPageResult<ResponseSet>> {
  return http.get<InspPageResult<ResponseSet>>(BASE, { params })
}

export function getResponseSet(id: number): Promise<ResponseSet> {
  return http.get<ResponseSet>(`${BASE}/${id}`)
}

export function createResponseSet(data: CreateResponseSetRequest): Promise<ResponseSet> {
  return http.post<ResponseSet>(BASE, data)
}

export function updateResponseSet(id: number, data: UpdateResponseSetRequest): Promise<ResponseSet> {
  return http.put<ResponseSet>(`${BASE}/${id}`, data)
}

export function deleteResponseSet(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 选项 CRUD ====================

export function getOptions(setId: number): Promise<ResponseSetOption[]> {
  return http.get<ResponseSetOption[]>(`${BASE}/${setId}/options`)
}

export function addOption(setId: number, data: CreateOptionRequest): Promise<ResponseSetOption> {
  return http.post<ResponseSetOption>(`${BASE}/${setId}/options`, data)
}

export function updateOption(setId: number, optionId: number, data: UpdateOptionRequest): Promise<ResponseSetOption> {
  return http.put<ResponseSetOption>(`${BASE}/${setId}/options/${optionId}`, data)
}

export function deleteOption(setId: number, optionId: number): Promise<void> {
  return http.delete(`${BASE}/${setId}/options/${optionId}`)
}

// ==================== API 对象 ====================

export const responseSetApi = {
  getList: getResponseSets,
  getById: getResponseSet,
  create: createResponseSet,
  update: updateResponseSet,
  delete: deleteResponseSet,
  getOptions,
  addOption,
  updateOption,
  deleteOption,
}
