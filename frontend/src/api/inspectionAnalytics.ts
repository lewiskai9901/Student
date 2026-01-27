/**
 * 量化检查数据分析 API
 */
import { http } from '@/utils/request'
import type { AnalyticsResponse } from '@/types/analytics'

const ANALYTICS_URL = '/inspection/analytics'

export function getClassRanking(params: {
  startDate: string
  endDate: string
}): Promise<AnalyticsResponse> {
  return http.get<AnalyticsResponse>(`${ANALYTICS_URL}/class-ranking`, { params })
}

export function getViolationDistribution(params: {
  startDate: string
  endDate: string
}): Promise<AnalyticsResponse> {
  return http.get<AnalyticsResponse>(`${ANALYTICS_URL}/violation-distribution`, { params })
}

export function getInspectorWorkload(params: {
  startDate: string
  endDate: string
}): Promise<AnalyticsResponse> {
  return http.get<AnalyticsResponse>(`${ANALYTICS_URL}/inspector-workload`, { params })
}

export function getDepartmentComparison(params: {
  startDate: string
  endDate: string
}): Promise<AnalyticsResponse> {
  return http.get<AnalyticsResponse>(`${ANALYTICS_URL}/department-comparison`, { params })
}

export const inspectionAnalyticsApi = {
  getClassRanking,
  getViolationDistribution,
  getInspectorWorkload,
  getDepartmentComparison
}
