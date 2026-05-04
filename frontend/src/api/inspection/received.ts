/**
 * 受检主体面 API — 班主任 / 场所负责人 / 受检单位的"被检查"视角.
 */
import { http } from '@/utils/request'

export interface ReceivedSummary {
  orgUnitCount: number
  orgUnitIds: number[]
  totalInspections: number
  avgScore: number | null
  avgPct: number | null
  openCorrectives: number
  overdueCorrectives: number
}

export interface ReceivedInspection {
  submissionId: number
  taskId: number
  taskCode: string
  projectId: number
  projectName: string
  subjectId: number
  subjectName: string
  inspectedAt: string
  score: number
  maxScore: number
  submissionStatus: string
  issueCount: number
}

export interface ReceivedTrendPoint {
  isoWeek: number
  weekStart: string
  submissionCount: number
  avgScore: number | null
  avgPct: number | null
  totalIssues: number
}

export interface ReceivedRecurringItem {
  itemCode: string
  itemName: string
  sectionName: string | null
  recurCount: number
  lastSeenAt: string
}

export function getMySummary(days = 30): Promise<ReceivedSummary> {
  return http.get<ReceivedSummary>('/inspection/received/summary', { params: { days } })
}

export function getMyInspections(params?: {
  projectId?: number
  days?: number
}): Promise<ReceivedInspection[]> {
  return http.get<ReceivedInspection[]>('/inspection/received/inspections', {
    params: params ?? { days: 30 },
  })
}

export function getMyTrends(weeks = 4): Promise<ReceivedTrendPoint[]> {
  return http.get<ReceivedTrendPoint[]>('/inspection/received/trends', { params: { weeks } })
}

export function getMyRecurring(limit = 10): Promise<ReceivedRecurringItem[]> {
  return http.get<ReceivedRecurringItem[]>('/inspection/received/recurring', { params: { limit } })
}
