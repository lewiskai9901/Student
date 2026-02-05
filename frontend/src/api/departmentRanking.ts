/**
 * 系部排名 API
 * @deprecated 请使用 orgRanking.ts 中的 orgRankingApi
 */
import { http } from '@/utils/request'
import type { DepartmentRankingItem } from '@/types/inspectionSession'

// 旧 API 路径（将逐步废弃）
const BASE_URL = '/inspection/department-ranking'

/**
 * @deprecated 使用 orgRankingApi.getRanking
 */
export function getDepartmentRanking(
  startDate: string,
  endDate: string,
  classWeight = 1.0
): Promise<DepartmentRankingItem[]> {
  return http.get<DepartmentRankingItem[]>(BASE_URL, {
    params: { startDate, endDate, classWeight }
  })
}

/**
 * @deprecated 使用 orgRankingApi.getRankingBySession
 */
export function getRankingBySession(sessionId: number): Promise<DepartmentRankingItem[]> {
  return http.get<DepartmentRankingItem[]>(`${BASE_URL}/session/${sessionId}`)
}

/**
 * @deprecated 使用 orgRankingApi
 */
export const departmentRankingApi = {
  getRanking: getDepartmentRanking,
  getRankingBySession
}
