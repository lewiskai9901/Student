import { http } from '@/utils/request'
import type {
  Major,
  MajorQueryParams,
  MajorFormData,
  MajorDirection,
  MajorDirectionQueryParams,
  Course,
  CourseQueryParams,
  CurriculumPlan,
  PlanCourse,
  CurriculumPlanQueryParams,
} from '@/types/academic'

const BASE = '/academic'

// Re-export types for convenience
export type {
  Major,
  MajorQueryParams,
  MajorFormData,
  MajorDirection,
  MajorDirectionQueryParams,
  Course,
  CourseQueryParams,
  CurriculumPlan,
  PlanCourse,
  CurriculumPlanQueryParams,
}

interface PageResult<T> {
  records: T[]
  total: number
  pages: number
}

// ==================== Major API (was api/major.ts, path: /majors -> /academic/majors) ====================

export const getMajorList = (params: MajorQueryParams) => {
  return http.get<PageResult<Major>>(`${BASE}/majors`, { params })
}

export const getAllEnabledMajors = () => {
  return http.get<Major[]>(`${BASE}/majors/enabled`)
}

export const getMajorsByOrgUnit = (orgUnitId: number | string) => {
  return http.get<Major[]>(`${BASE}/majors/org-unit/${orgUnitId}`)
}

export const getMajorDetail = (id: number | string) => {
  return http.get<Major>(`${BASE}/majors/${id}`)
}

export const addMajor = (data: MajorFormData) => {
  return http.post(`${BASE}/majors`, data)
}

export const updateMajor = (id: number | string, data: MajorFormData) => {
  return http.put(`${BASE}/majors/${id}`, data)
}

export const deleteMajor = (id: number | string) => {
  return http.delete(`${BASE}/majors/${id}`)
}

export const batchDeleteMajors = (ids: (number | string)[]) => {
  return http.delete(`${BASE}/majors/batch`, { data: { ids } })
}

export const exportMajors = (params: MajorQueryParams) => {
  return http.get(`${BASE}/majors/export`, {
    params,
    responseType: 'blob'
  })
}

export const majorApi = {
  list: getMajorList,
  getAllEnabled: getAllEnabledMajors,
  getByOrgUnit: getMajorsByOrgUnit,
  getDetail: getMajorDetail,
  create: addMajor,
  update: updateMajor,
  delete: deleteMajor,
  batchDelete: batchDeleteMajors,
  export: exportMajors,
}

// ==================== MajorDirection API (was api/majorDirection.ts, path: /major-directions -> /academic/major-directions) ====================

export const getMajorDirectionList = (params: MajorDirectionQueryParams) => {
  return http.get<PageResult<MajorDirection>>(`${BASE}/major-directions`, { params })
}

export const getAllDirections = () => {
  return http.get<MajorDirection[]>(`${BASE}/major-directions/all`)
}

export const getDirectionsByMajor = (majorId: number | string) => {
  return http.get<MajorDirection[]>(`${BASE}/major-directions/major/${majorId}`)
}

export const getMajorDirectionDetail = (id: number | string) => {
  return http.get<MajorDirection>(`${BASE}/major-directions/${id}`)
}

export const addMajorDirection = (data: MajorDirection) => {
  return http.post<MajorDirection>(`${BASE}/major-directions`, data)
}

export const updateMajorDirection = (id: number | string, data: MajorDirection) => {
  return http.put(`${BASE}/major-directions/${id}`, data)
}

export const deleteMajorDirection = (id: number | string) => {
  return http.delete(`${BASE}/major-directions/${id}`)
}

export const batchDeleteDirections = (ids: (number | string)[]) => {
  return http.delete(`${BASE}/major-directions/batch`, { data: ids })
}

export const majorDirectionApi = {
  list: getMajorDirectionList,
  getAll: getAllDirections,
  getByMajor: getDirectionsByMajor,
  getDetail: getMajorDirectionDetail,
  create: addMajorDirection,
  update: updateMajorDirection,
  delete: deleteMajorDirection,
  batchDelete: batchDeleteDirections,
}

// ==================== Course API (was teaching.ts courseApi, path: /teaching/courses -> /academic/courses) ====================

export const courseApi = {
  list: (params?: CourseQueryParams) =>
    http.get<PageResult<Course>>(`${BASE}/courses`, { params }),

  listAll: () => http.get<Course[]>(`${BASE}/courses/all`),

  getById: (id: number | string) => http.get<Course>(`${BASE}/courses/${id}`),

  getByCode: (code: string) => http.get<Course>(`${BASE}/courses/code/${code}`),

  create: (data: Partial<Course>) =>
    http.post<Course>(`${BASE}/courses`, data),

  update: (id: number | string, data: Partial<Course>) =>
    http.put<Course>(`${BASE}/courses/${id}`, data),

  delete: (id: number | string) => http.delete(`${BASE}/courses/${id}`),

  updateStatus: (id: number | string, status: number) =>
    http.patch(`${BASE}/courses/${id}/status`, { status }),
}

// ==================== CurriculumPlan API (was teaching.ts curriculumPlanApi, path: /teaching/curriculum-plans -> /academic/curriculum-plans) ====================

export const curriculumPlanApi = {
  list: (params?: CurriculumPlanQueryParams) =>
    http.get<PageResult<CurriculumPlan>>(`${BASE}/curriculum-plans`, { params }),

  getById: (id: number | string) => http.get<CurriculumPlan>(`${BASE}/curriculum-plans/${id}`),

  create: (data: Partial<CurriculumPlan>) =>
    http.post<CurriculumPlan>(`${BASE}/curriculum-plans`, data),

  update: (id: number | string, data: Partial<CurriculumPlan>) =>
    http.put<CurriculumPlan>(`${BASE}/curriculum-plans/${id}`, data),

  delete: (id: number | string) => http.delete(`${BASE}/curriculum-plans/${id}`),

  publish: (id: number | string) =>
    http.post(`${BASE}/curriculum-plans/${id}/publish`),

  deprecate: (id: number | string) =>
    http.post(`${BASE}/curriculum-plans/${id}/deprecate`),

  getCourses: (planId: number | string) =>
    http.get<PlanCourse[]>(`${BASE}/curriculum-plans/${planId}/courses`),

  addCourse: (planId: number | string, data: Partial<PlanCourse>) =>
    http.post<PlanCourse>(`${BASE}/curriculum-plans/${planId}/courses`, data),

  updateCourse: (planId: number | string, courseId: number | string, data: Partial<PlanCourse>) =>
    http.put<PlanCourse>(`${BASE}/curriculum-plans/${planId}/courses/${courseId}`, data),

  removeCourse: (planId: number | string, courseId: number | string) =>
    http.delete(`${BASE}/curriculum-plans/${planId}/courses/${courseId}`),

  copyPlan: (id: number | string, newVersion: string) =>
    http.post<CurriculumPlan>(`${BASE}/curriculum-plans/${id}/copy`, { newVersion }),
}
