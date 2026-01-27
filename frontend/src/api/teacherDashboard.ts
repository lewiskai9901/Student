/**
 * 班主任工作台 API
 */
import { http } from '@/utils/request'
import type {
  TeacherOverview,
  DeductionDetailRecord,
  TopIssueItem,
  StudentViolationItem,
  ImprovementData
} from '@/types/inspectionSession'

const BASE_URL = '/teacher-dashboard'

export function getOverview(classId: number, weekStart: string, weekEnd: string): Promise<TeacherOverview> {
  return http.get<TeacherOverview>(`${BASE_URL}/overview`, {
    params: { classId, weekStart, weekEnd }
  })
}

export function getDeductions(classId: number, startDate: string, endDate: string): Promise<DeductionDetailRecord[]> {
  return http.get<DeductionDetailRecord[]>(`${BASE_URL}/deductions`, {
    params: { classId, startDate, endDate }
  })
}

export function getTopIssues(classId: number, startDate: string, endDate: string, topN = 5): Promise<TopIssueItem[]> {
  return http.get<TopIssueItem[]>(`${BASE_URL}/top-issues`, {
    params: { classId, startDate, endDate, topN }
  })
}

export function getStudentViolations(classId: number, startDate: string, endDate: string): Promise<StudentViolationItem[]> {
  return http.get<StudentViolationItem[]>(`${BASE_URL}/students/violations`, {
    params: { classId, startDate, endDate }
  })
}

export function getImprovement(
  classId: number,
  currentStart: string,
  currentEnd: string,
  previousStart: string,
  previousEnd: string
): Promise<ImprovementData> {
  return http.get<ImprovementData>(`${BASE_URL}/improvement`, {
    params: { classId, currentStart, currentEnd, previousStart, previousEnd }
  })
}

export const teacherDashboardApi = {
  getOverview,
  getDeductions,
  getTopIssues,
  getStudentViolations,
  getImprovement
}
