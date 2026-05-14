/**
 * Teacher profile API
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type { TeacherProfile, TeacherCourseQualification } from '@/types/teacher'

const BASE_URL = '/teacher-profiles'

export const teacherProfileApi = {
  /** List teacher profiles (paginated) */
  list(params?: {
    pageNum?: number
    pageSize?: number
    orgUnitId?: LongId
    title?: string
    status?: number
    keyword?: string
  }) {
    return http.get<{ records: TeacherProfile[]; total: number }>(BASE_URL, { params })
  },

  /** Get teacher profile by ID */
  getById(id: LongId | string) {
    return http.get<TeacherProfile>(`${BASE_URL}/${id}`)
  },

  /** Get teacher profile by user ID */
  getByUserId(userId: LongId | string) {
    return http.get<TeacherProfile>(`${BASE_URL}/by-user/${userId}`)
  },

  /** Create teacher profile */
  create(data: Partial<TeacherProfile>) {
    return http.post<number>(BASE_URL, data)
  },

  /** Update teacher profile */
  update(id: LongId | string, data: Partial<TeacherProfile>) {
    return http.put(`${BASE_URL}/${id}`, data)
  },

  /** Delete teacher profile */
  delete(id: LongId | string) {
    return http.delete(`${BASE_URL}/${id}`)
  },

  /** Get course qualifications for a teacher */
  getCourses(id: LongId | string) {
    return http.get<TeacherCourseQualification[]>(`${BASE_URL}/${id}/courses`)
  },

  /** Add course qualification */
  addCourse(id: LongId | string, data: { courseId: LongId; qualificationLevel?: number; remark?: string }) {
    return http.post<number>(`${BASE_URL}/${id}/courses`, data)
  },

  /** Remove course qualification */
  removeCourse(id: LongId | string, courseId: LongId | string) {
    return http.delete(`${BASE_URL}/${id}/courses/${courseId}`)
  },

  /** Get teachers qualified for a specific course */
  getAvailableForCourse(courseId: LongId | string) {
    return http.get<TeacherProfile[]>(`${BASE_URL}/available`, { params: { courseId } })
  },
}
