/**
 * 系部排名 API
 */
import { http } from '@/utils/request'
import type { DepartmentRankingItem } from '@/types/inspectionSession'

const BASE_URL = '/inspection/department-ranking'

export function getDepartmentRanking(
  startDate: string,
  endDate: string,
  classWeight = 1.0
): Promise<DepartmentRankingItem[]> {
  return http.get<DepartmentRankingItem[]>(BASE_URL, {
    params: { startDate, endDate, classWeight }
  })
}

export function getRankingBySession(sessionId: number): Promise<DepartmentRankingItem[]> {
  return http.get<DepartmentRankingItem[]>(`${BASE_URL}/session/${sessionId}`)
}

export const departmentRankingApi = {
  getRanking: getDepartmentRanking,
  getRankingBySession
}
