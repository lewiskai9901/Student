// /my/dashboard 首页相关类型定义
// 后端来源: com.school.management.interfaces.rest.my.MyDashboardController 内部 record

export interface DashboardSummary {
  todayLessons: number
  weeklyHoursCurrent: number
  weeklyHoursTotal: number
  substituteRequests: number
}

/** 课次状态：0 正常 / 1 已取消 / 2 已调走 / 3 补课 / 4 代课 */
export type LessonStatus = 0 | 1 | 2 | 3 | 4

export interface TodayLesson {
  instanceId: number
  startSlot: number | null
  endSlot: number | null
  startTime: string | null
  endTime: string | null
  courseName: string | null
  className: string | null
  classroomName: string | null
  status: LessonStatus | null
  canSign: boolean
}

export interface MyClass {
  classId: number
  className: string | null
  studentCount: number
  isHeadTeacher: boolean
  subjects: string[]
}

export interface SubstituteTask {
  taskId: number
  courseName: string | null
  scheduledDate: string | null
  startSlot: number | null
  endSlot: number | null
  requesterName: string | null
  requestedAt: string | null
}
