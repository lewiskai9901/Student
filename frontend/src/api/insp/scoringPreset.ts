/**
 * V7 检查平台 - 评分预设 API
 */
import { http } from '@/utils/request'

export interface ScoringPreset {
  id: number
  templateId: number
  presetName: string
  presetType: string // FULL_PASS | FULL_FAIL | CUSTOM
  itemValues: string // JSON
  usageCount: number
  createdBy: number | null
  createdAt: string
}

export interface CreatePresetRequest {
  templateId: number
  presetName: string
  presetType: string
  itemValues: string
}

const BASE = '/v7/insp/scoring-presets'

export function getPresets(templateId: number): Promise<ScoringPreset[]> {
  return http.get<ScoringPreset[]>(`/v7/insp/templates/${templateId}/scoring-presets`)
}

export function getPreset(id: number): Promise<ScoringPreset> {
  return http.get<ScoringPreset>(`${BASE}/${id}`)
}

export function createPreset(data: CreatePresetRequest): Promise<ScoringPreset> {
  return http.post<ScoringPreset>(`/v7/insp/templates/${data.templateId}/scoring-presets`, data)
}

export function updatePreset(id: number, data: Partial<CreatePresetRequest>): Promise<ScoringPreset> {
  return http.put<ScoringPreset>(`${BASE}/${id}`, data)
}

export function deletePreset(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

export function applyPreset(id: number): Promise<void> {
  return http.post(`${BASE}/${id}/apply`)
}

export const inspScoringPresetApi = {
  getPresets,
  getPreset,
  createPreset,
  updatePreset,
  deletePreset,
  applyPreset,
}
