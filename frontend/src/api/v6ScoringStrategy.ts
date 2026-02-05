import request from '@/utils/request'

// 打分策略类型
export type ScoringStrategyType = 'DEDUCTION' | 'ADDITION' | 'BASE_SCORE' | 'RATING' | 'GRADE' | 'PASS_FAIL' | 'CHECKLIST'

// 打分策略
export interface ScoringStrategy {
  id: number
  strategyCode: string
  strategyName: string
  description?: string
  strategyType: ScoringStrategyType
  strategyTypeName: string
  config?: string
  completionRules?: string
  resultFormat?: string
  isSystem: boolean
  isEnabled: boolean
}

// 策略类型信息
export interface StrategyTypeInfo {
  code: ScoringStrategyType
  name: string
  description: string
}

// 创建策略请求
export interface CreateStrategyRequest {
  strategyCode: string
  strategyName: string
  description?: string
  strategyType: ScoringStrategyType
  config?: string
  completionRules?: string
  resultFormat?: string
}

// 创建简单策略请求
export interface CreateSimpleStrategyRequest {
  strategyCode: string
  strategyName: string
  description?: string
}

// 创建基准分策略请求
export interface CreateBaseScoreStrategyRequest {
  strategyCode: string
  strategyName: string
  description?: string
  baseScore: number
  minScore: number
}

// 创建评分制策略请求
export interface CreateRatingStrategyRequest {
  strategyCode: string
  strategyName: string
  description?: string
  minRating: number
  maxRating: number
}

// 创建等级制策略请求
export interface CreateGradeStrategyRequest {
  strategyCode: string
  strategyName: string
  description?: string
  gradesConfig: string
}

// 更新策略请求
export interface UpdateStrategyRequest {
  strategyName: string
  description?: string
  config?: string
  resultFormat?: string
}

// 打分策略 API
export const scoringStrategyApi = {
  // 创建策略
  create(data: CreateStrategyRequest) {
    return request.post<ScoringStrategy>('/v6/scoring-strategies', data)
  },

  // 创建扣分制策略
  createDeduction(data: CreateSimpleStrategyRequest) {
    return request.post<ScoringStrategy>('/v6/scoring-strategies/deduction', data)
  },

  // 创建基准分策略
  createBaseScore(data: CreateBaseScoreStrategyRequest) {
    return request.post<ScoringStrategy>('/v6/scoring-strategies/base-score', data)
  },

  // 创建评分制策略
  createRating(data: CreateRatingStrategyRequest) {
    return request.post<ScoringStrategy>('/v6/scoring-strategies/rating', data)
  },

  // 创建等级制策略
  createGrade(data: CreateGradeStrategyRequest) {
    return request.post<ScoringStrategy>('/v6/scoring-strategies/grade', data)
  },

  // 更新策略
  update(id: number, data: UpdateStrategyRequest) {
    return request.put<ScoringStrategy>(`/v6/scoring-strategies/${id}`, data)
  },

  // 启用策略
  enable(id: number) {
    return request.post(`/v6/scoring-strategies/${id}/enable`)
  },

  // 禁用策略
  disable(id: number) {
    return request.post(`/v6/scoring-strategies/${id}/disable`)
  },

  // 删除策略
  delete(id: number) {
    return request.delete(`/v6/scoring-strategies/${id}`)
  },

  // 根据ID查询
  getById(id: number) {
    return request.get<ScoringStrategy>(`/v6/scoring-strategies/${id}`)
  },

  // 根据策略代码查询
  getByStrategyCode(strategyCode: string) {
    return request.get<ScoringStrategy>(`/v6/scoring-strategies/code/${strategyCode}`)
  },

  // 根据策略类型查询
  getByStrategyType(strategyType: ScoringStrategyType) {
    return request.get<ScoringStrategy[]>(`/v6/scoring-strategies/type/${strategyType}`)
  },

  // 查询所有启用的策略
  getAllEnabled() {
    return request.get<ScoringStrategy[]>('/v6/scoring-strategies')
  },

  // 按类型分组查询
  getGroupedByType() {
    return request.get<Record<string, ScoringStrategy[]>>('/v6/scoring-strategies/grouped')
  },

  // 查询系统内置策略
  getSystemStrategies() {
    return request.get<ScoringStrategy[]>('/v6/scoring-strategies/system')
  },

  // 获取所有策略类型
  getStrategyTypes() {
    return request.get<StrategyTypeInfo[]>('/v6/scoring-strategies/types')
  }
}
