/**
 * 校历管理 API (共享资源)
 *
 * 从 teaching.ts 提取，学年/学期/教学周/校历事件现在走 /calendar 路径。
 * 这些是跨领域共享的日历资源，不再属于 teaching 专有。
 */
import { http } from '@/utils/request'
import type {
  AcademicYear,
  Semester,
  TeachingWeek,
  AcademicEvent,
} from '@/types/teaching'

const BASE = '/calendar'

// ==================== 学年管理 ====================

export const academicYearApi = {
  list: () => http.get<AcademicYear[]>(`${BASE}/academic-years`),

  getById: (id: number | string) => http.get<AcademicYear>(`${BASE}/academic-years/${id}`),

  getCurrent: () => http.get<AcademicYear>(`${BASE}/academic-years/current`),

  create: (data: Partial<AcademicYear>) =>
    http.post<AcademicYear>(`${BASE}/academic-years`, data),

  update: (id: number | string, data: Partial<AcademicYear>) =>
    http.put<AcademicYear>(`${BASE}/academic-years/${id}`, data),

  delete: (id: number | string) => http.delete(`${BASE}/academic-years/${id}`),

  setCurrent: (id: number | string) =>
    http.post(`${BASE}/academic-years/${id}/set-current`),
}

// ==================== 学期管理 ====================

export const semesterApi = {
  list: (yearId?: number | string) => {
    const params = yearId ? { yearId } : {}
    return http.get<Semester[]>(`${BASE}/semesters`, { params })
  },

  getById: (id: number | string) => http.get<Semester>(`${BASE}/semesters/${id}`),

  getCurrent: () => http.get<Semester>(`${BASE}/semesters/current`),

  create: (data: Partial<Semester>) =>
    http.post<Semester>(`${BASE}/semesters`, data),

  update: (id: number | string, data: Partial<Semester>) =>
    http.put<Semester>(`${BASE}/semesters/${id}`, data),

  delete: (id: number | string) => http.delete(`${BASE}/semesters/${id}`),

  setCurrent: (id: number | string) =>
    http.post(`${BASE}/semesters/${id}/set-current`),

  getWeeks: (semesterId: number | string) =>
    http.get<TeachingWeek[]>(`${BASE}/semesters/${semesterId}/weeks`),

  generateWeeks: (semesterId: number | string) =>
    http.post<TeachingWeek[]>(`${BASE}/semesters/${semesterId}/generate-weeks`),
}

// ==================== 校历事件 ====================

export const academicEventApi = {
  list: (params: { yearId?: number | string; semesterId?: number | string; eventType?: number }) =>
    http.get<AcademicEvent[]>(`${BASE}/events`, { params }),

  getById: (id: number | string) => http.get<AcademicEvent>(`${BASE}/events/${id}`),

  create: (data: Partial<AcademicEvent>) =>
    http.post<AcademicEvent>(`${BASE}/events`, data),

  update: (id: number | string, data: Partial<AcademicEvent>) =>
    http.put<AcademicEvent>(`${BASE}/events/${id}`, data),

  delete: (id: number | string) => http.delete(`${BASE}/events/${id}`),
}
