import type { LongId } from '@/types/common'

/**
 * Teacher profile types
 */
export interface TeacherProfile {
  id: LongId
  userId: LongId
  userName?: string
  realName?: string
  phone?: string
  employeeNo?: string
  title?: string
  titleLevel?: string
  orgUnitId?: LongId
  orgUnitName?: string
  teachingGroup?: string
  maxWeeklyHours: number
  qualification?: string
  specialties?: string[]
  hireDate?: string
  status: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface TeacherCourseQualification {
  id: LongId
  teacherProfileId: LongId
  courseId: LongId
  courseName?: string
  courseCode?: string
  qualificationLevel: number
  remark?: string
  createdAt?: string
}

export const TEACHER_TITLES = ['教授', '副教授', '讲师', '助教', '实训指导教师']
export const TITLE_LEVELS = ['正高级', '副高级', '中级', '初级']
export const TEACHER_STATUS_MAP: Record<LongId, string> = {
  1: '在职',
  2: '离职',
  3: '退休',
}
