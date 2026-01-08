/**
 * 打分功能API封装
 */
import { get, post } from '@/utils/request'
import type {
  ScoringInitResponse,
  SaveScoringRequest,
  CheckPlanItem,
  PageResult
} from '@/types/scoring'

/**
 * 获取可打分的检查计划列表
 */
export function getScoringPlans(params?: {
  pageNum?: number
  pageSize?: number
  checkDate?: string
  status?: number
}) {
  return get<PageResult<CheckPlanItem>>('/quantification/daily-checks', {
    pageNum: params?.pageNum || 1,
    pageSize: params?.pageSize || 20,
    status: params?.status,
    checkDate: params?.checkDate
  })
}

/**
 * 获取打分初始化数据
 * @param checkId 检查ID
 */
export function getScoringInitData(checkId: string | number) {
  return get<ScoringInitResponse>(`/quantification/daily-checks/${checkId}/scoring/init`)
}

/**
 * 保存打分数据
 * @param checkId 检查ID
 * @param data 打分数据
 */
export function saveScoring(checkId: string | number, data: SaveScoringRequest) {
  return post<void>(`/quantification/daily-checks/${checkId}/scoring`, data)
}

/**
 * 获取检查详情
 */
export function getCheckDetail(checkId: string | number) {
  return get<any>(`/quantification/daily-checks/${checkId}`)
}
