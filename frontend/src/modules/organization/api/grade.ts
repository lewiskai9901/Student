/**
 * 年级 API
 */
import { http } from '@/shared/utils/request'
import type { Grade, GradeQuery, GradeCreateRequest } from '../types'

const BASE_URL = '/v2/grades'

export function listGrades(params?: GradeQuery): Promise<Grade[]> {
  return http.get<Grade[]>(BASE_URL, { params })
}

export function getGradePage(params: GradeQuery & { pageNum: number; pageSize: number }) {
  return http.get<Grade[]>(BASE_URL).then(grades => {
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return {
      records: grades.slice(start, end),
      total: grades.length,
      current: params.pageNum,
      size: params.pageSize
    }
  })
}

export function getGrade(id: number): Promise<Grade> {
  return http.get<Grade>(`${BASE_URL}/${id}`)
}

export function getActiveGrades(): Promise<Grade[]> {
  return http.get<Grade[]>(`${BASE_URL}/active`)
}

export function getGradeByYear(enrollmentYear: number): Promise<Grade> {
  return http.get<Grade>(`${BASE_URL}/by-year/${enrollmentYear}`)
}

export function getGradesByStatus(status: string): Promise<Grade[]> {
  return http.get<Grade[]>(`${BASE_URL}/by-status/${status}`)
}

export function createGrade(data: GradeCreateRequest): Promise<Grade> {
  return http.post<Grade>(BASE_URL, data)
}

export function updateGrade(data: {
  id: number
  gradeName?: string
  standardClassSize?: number
  sortOrder?: number
  remarks?: string
}): Promise<Grade> {
  return http.put<Grade>(`${BASE_URL}/${data.id}`, data)
}

export function activateGrade(id: number): Promise<Grade> {
  return http.put<Grade>(`${BASE_URL}/${id}/activate`)
}

export function graduateGrade(id: number): Promise<Grade> {
  return http.put<Grade>(`${BASE_URL}/${id}/graduate`)
}

export function stopEnrollment(id: number): Promise<Grade> {
  return http.put<Grade>(`${BASE_URL}/${id}/stop-enrollment`)
}

export function deleteGrade(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/${id}`)
}

export function getAllGrades(): Promise<Grade[]> {
  return http.get<Grade[]>(BASE_URL)
}

export function assignGradeLeaders(
  id: number,
  data: { directorId?: number; counselorId?: number }
): Promise<Grade> {
  return http.put<Grade>(`${BASE_URL}/${id}/leaders`, data)
}

export const gradeApi = {
  list: listGrades,
  getPage: getGradePage,
  getById: getGrade,
  getActive: getActiveGrades,
  getByYear: getGradeByYear,
  getByStatus: getGradesByStatus,
  getAll: getAllGrades,
  create: createGrade,
  update: updateGrade,
  activate: activateGrade,
  graduate: graduateGrade,
  stopEnrollment,
  delete: deleteGrade,
  assignLeaders: assignGradeLeaders
}
