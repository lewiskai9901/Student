/**
 * Teacher profile types
 */
export interface TeacherProfile {
  id: number
  userId: number
  userName?: string
  realName?: string
  phone?: string
  employeeNo?: string
  title?: string
  titleLevel?: string
  orgUnitId?: number
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
  id: number
  teacherProfileId: number
  courseId: number
  courseName?: string
  courseCode?: string
  qualificationLevel: number
  remark?: string
  createdAt?: string
}

export const TEACHER_TITLES = ['教授', '副教授', '讲师', '助教', '实训指导教师']
export const TITLE_LEVELS = ['正高级', '副高级', '中级', '初级']
export const TEACHER_STATUS_MAP: Record<number, string> = {
  1: '在职',
  2: '离职',
  3: '退休',
}
