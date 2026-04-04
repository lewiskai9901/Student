/**
 * V7 检查平台 - 评价指标 API
 */
import { http } from '@/utils/request'
import type {
  Indicator,
  IndicatorScore,
  CreateLeafIndicatorRequest,
  CreateCompositeIndicatorRequest,
  UpdateIndicatorRequest,
} from '@/types/insp/indicator'

const BASE = '/v7/insp/indicators'
const SCORE_BASE = '/v7/insp/indicator-scores'

// ==================== 指标 CRUD ====================

export function getIndicators(projectId: number): Promise<Indicator[]> {
  return http.get<Indicator[]>(BASE, { params: { projectId } })
}

export function getIndicator(id: number): Promise<Indicator> {
  return http.get<Indicator>(`${BASE}/${id}`)
}

export function createLeafIndicator(data: CreateLeafIndicatorRequest): Promise<Indicator> {
  return http.post<Indicator>(`${BASE}/leaf`, data)
}

export function createCompositeIndicator(data: CreateCompositeIndicatorRequest): Promise<Indicator> {
  return http.post<Indicator>(`${BASE}/composite`, data)
}

export function updateIndicator(id: number, data: UpdateIndicatorRequest): Promise<Indicator> {
  return http.put<Indicator>(`${BASE}/${id}`, data)
}

export function deleteIndicator(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 指标得分 ====================

export function getIndicatorScores(
  indicatorId: number,
  periodStart?: string,
  periodEnd?: string,
): Promise<IndicatorScore[]> {
  const params: Record<string, any> = { indicatorId }
  if (periodStart) params.periodStart = periodStart
  if (periodEnd) params.periodEnd = periodEnd
  return http.get<IndicatorScore[]>(SCORE_BASE, { params })
}

export function computeIndicatorScores(
  projectId: number,
  periodStart: string,
  periodEnd: string,
): Promise<void> {
  return http.post(`${SCORE_BASE}/compute`, null, {
    params: { projectId, periodStart, periodEnd },
  })
}

// ==================== API 对象 ====================

export const indicatorApi = {
  getList: getIndicators,
  getById: getIndicator,
  createLeaf: createLeafIndicator,
  createComposite: createCompositeIndicator,
  update: updateIndicator,
  delete: deleteIndicator,
  getScores: getIndicatorScores,
  computeScores: computeIndicatorScores,
}
