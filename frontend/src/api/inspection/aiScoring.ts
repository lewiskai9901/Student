import { http } from '@/utils/request'

/**
 * AI 辅助打分 API (Track 5).
 * 默认使用启发式后端 (heuristic), 后续切换 Claude 不改前端.
 */

export interface SuggestScoreRequest {
  description: string
  itemTitle?: string
  itemMaxScore?: number
  scoringMode?: 'SCORE' | 'PASS_FAIL' | 'DEDUCTION'
  evidenceUrls?: string[]
}

export interface SuggestScoreResponse {
  suggestedScore: number | null
  suggestedVerdict: 'PASS' | 'FAIL' | null
  categoryTags: string[]
  confidence: number
  reasoning: string
  provider: string
}

export function suggestScore(req: SuggestScoreRequest): Promise<SuggestScoreResponse> {
  return http.post<SuggestScoreResponse>('/inspection/ai/suggest-score', req)
}
