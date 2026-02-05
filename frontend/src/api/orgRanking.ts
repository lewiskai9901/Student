/**
 * 组织排名 API
 */
import { http } from '@/utils/request'

export interface OrgRankingItem {
  orgUnitId: number
  orgUnitName: string
  averageClassScore: number
  classCount: number
  compositeScore: number
  ranking: number
}

const BASE_URL = '/inspection/org-ranking'

/**
 * 获取日期范围内的组织排名
 */
export function getOrgRanking(
  startDate: string,
  endDate: string,
  classWeight = 1.0
): Promise<OrgRankingItem[]> {
  return http.get<OrgRankingItem[]>(BASE_URL, {
    params: { startDate, endDate, classWeight }
  })
}

/**
 * 获取单个检查场次的组织排名
 */
export function getOrgRankingBySession(sessionId: number): Promise<OrgRankingItem[]> {
  return http.get<OrgRankingItem[]>(`${BASE_URL}/session/${sessionId}`)
}

export const orgRankingApi = {
  getRanking: getOrgRanking,
  getRankingBySession: getOrgRankingBySession
}
