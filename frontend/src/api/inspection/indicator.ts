/**
 * 检查平台 - 评价指标 API
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type {
  Indicator,
  IndicatorScore,
  IndicatorResult,
  ResultStatus,
  CreateLeafIndicatorRequest,
  CreateCompositeIndicatorRequest,
  UpdateIndicatorRequest,
  ManualEvaluateRequest,
} from '@/types/insp/indicator'

const BASE = '/inspection/indicators'
const SCORE_BASE = '/inspection/indicator-scores'
const RESULT_BASE = '/inspection/indicator-results'

// ==================== 指标 CRUD ====================

export function getIndicators(projectId: LongId): Promise<Indicator[]> {
  return http.get<Indicator[]>(BASE, { params: { projectId } })
}

export function getIndicator(id: LongId): Promise<Indicator> {
  return http.get<Indicator>(`${BASE}/${id}`)
}

export function createLeafIndicator(data: CreateLeafIndicatorRequest): Promise<Indicator> {
  return http.post<Indicator>(`${BASE}/leaf`, data)
}

export function createCompositeIndicator(data: CreateCompositeIndicatorRequest): Promise<Indicator> {
  return http.post<Indicator>(`${BASE}/composite`, data)
}

export function updateIndicator(id: LongId, data: UpdateIndicatorRequest): Promise<Indicator> {
  return http.put<Indicator>(`${BASE}/${id}`, data)
}

export function deleteIndicator(id: LongId): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 指标得分 (旧, 兼容) ====================

export function getIndicatorScores(
  indicatorId: LongId,
  periodStart?: string,
  periodEnd?: string,
): Promise<IndicatorScore[]> {
  const params: Record<string, any> = { indicatorId }
  if (periodStart) params.periodStart = periodStart
  if (periodEnd) params.periodEnd = periodEnd
  return http.get<IndicatorScore[]>(SCORE_BASE, { params })
}

export function computeIndicatorScores(
  projectId: LongId,
  periodStart: string,
  periodEnd: string,
): Promise<void> {
  return http.post(`${SCORE_BASE}/compute`, null, {
    params: { projectId, periodStart, periodEnd },
  })
}

// ==================== 评级结果 (Phase 4 评级引擎完美架构) ====================

/** 列表查询 — 按 indicator + 可选 target / periodKey / status 过滤. */
export function listIndicatorResults(params: {
  indicatorId: LongId
  targetId?: LongId
  periodKey?: string
  status?: ResultStatus
}): Promise<IndicatorResult[]> {
  return http.get<IndicatorResult[]>(RESULT_BASE, { params })
}

/** 修订链: 给任一 result id, 返回同 (indicator,target,period) 的整条版本链 (computedAt ASC). */
export function getIndicatorResultHistory(id: LongId): Promise<IndicatorResult[]> {
  return http.get<IndicatorResult[]>(`${RESULT_BASE}/${id}/history`)
}

/** 单条 DRAFT → PUBLISHED. */
export function publishIndicatorResult(id: LongId): Promise<IndicatorResult> {
  return http.post<IndicatorResult>(`${RESULT_BASE}/${id}/publish`)
}

/** 手动评估入口 — MANUAL trigger 指标. */
export function manualEvaluateIndicator(body: ManualEvaluateRequest): Promise<IndicatorResult[]> {
  return http.post<IndicatorResult[]>(`${RESULT_BASE}/manual-evaluate`, body)
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

export const indicatorResultApi = {
  list: listIndicatorResults,
  history: getIndicatorResultHistory,
  publish: publishIndicatorResult,
  manualEvaluate: manualEvaluateIndicator,
}
