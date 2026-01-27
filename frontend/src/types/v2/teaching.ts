// 教务管理模块类型定义

// ==================== 校历管理 ====================

export interface AcademicYear {
  id: number
  name: string
  startDate: string
  endDate: string
  isCurrent: boolean
  status: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface Semester {
  id: number
  yearId: number
  yearName?: string
  name: string
  termType: number // 1-第一学期, 2-第二学期, 3-短学期
  startDate: string
  endDate: string
  teachingWeeks: number
  isCurrent: boolean
  status: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface TeachingWeek {
  id: number
  semesterId: number
  weekNumber: number
  startDate: string
  endDate: string
  weekType: number // 1-教学周, 2-考试周, 3-假期
  remark?: string
}

export interface AcademicEvent {
  id: number
  yearId?: number
  semesterId?: number
  title: string
  eventType: number // 1-开学, 2-放假, 3-考试, 4-活动, 5-其他
  startDate: string
  endDate?: string
  allDay: boolean
  description?: string
}

// ==================== 课程管理 ====================

export interface Course {
  id: number
  code: string
  name: string
  englishName?: string
  credits: number
  totalHours: number
  theoryHours: number
  practiceHours: number
  courseType: number // 1-必修, 2-选修, 3-通识
  departmentId?: number
  departmentName?: string
  description?: string
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface CourseQueryParams {
  keyword?: string
  courseType?: number
  departmentId?: number
  status?: number
  page?: number
  size?: number
}

// ==================== 培养方案 ====================

export interface CurriculumPlan {
  id: number
  name: string
  majorId: number
  majorName?: string
  gradeYear: number
  version: string
  totalCredits: number
  status: number // 0-草稿, 1-已发布, 2-已废弃
  description?: string
  courses?: PlanCourse[]
  createdAt?: string
  updatedAt?: string
}

export interface PlanCourse {
  id: number
  planId: number
  courseId: number
  courseName?: string
  courseCode?: string
  credits?: number
  semesterNumber: number // 第几学期
  courseCategory: number // 1-公共基础, 2-专业基础, 3-专业核心, 4-专业选修, 5-实践环节
  isRequired: boolean
  weeklyHours?: number
}

export interface CurriculumPlanQueryParams {
  majorId?: number
  gradeYear?: number
  status?: number
  page?: number
  size?: number
}

// ==================== 教学任务 ====================

export interface TeachingTask {
  id: number
  semesterId: number
  semesterName?: string
  courseId: number
  courseName?: string
  courseCode?: string
  classId: number
  className?: string
  studentCount: number
  weeklyHours: number
  startWeek: number
  endWeek: number
  status: number // 0-待分配, 1-已分配, 2-已排课, 3-进行中, 4-已结束
  teachers?: TaskTeacher[]
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface TaskTeacher {
  id: number
  taskId: number
  teacherId: number
  teacherName?: string
  isMain: boolean // 是否主讲教师
}

export interface TeachingTaskQueryParams {
  semesterId?: number
  courseId?: number
  classId?: number
  teacherId?: number
  status?: number
  page?: number
  size?: number
}

// ==================== 排课管理 ====================

export interface CourseSchedule {
  id: number
  semesterId: number
  semesterName?: string
  name: string
  status: number // 0-草稿, 1-已发布, 2-已归档
  generatedAt?: string
  publishedAt?: string
  remark?: string
  entries?: ScheduleEntry[]
  createdAt?: string
  updatedAt?: string
}

export interface ScheduleEntry {
  id: number
  scheduleId: number
  taskId: number
  courseName?: string
  className?: string
  teacherName?: string
  classroomId: number
  classroomName?: string
  dayOfWeek: number // 1-7
  periodStart: number // 开始节次
  periodEnd: number // 结束节次
  weekStart: number
  weekEnd: number
  weekType: number // 0-每周, 1-单周, 2-双周
}

export interface ScheduleConflict {
  type: string // TEACHER, CLASSROOM, CLASS
  message: string
  entries: ScheduleEntry[]
}

export interface AutoScheduleParams {
  scheduleId: number
  maxIterations?: number
  populationSize?: number
  mutationRate?: number
}

export interface AutoScheduleResult {
  success: boolean
  entriesGenerated: number
  conflicts: ScheduleConflict[]
  executionTime: number
}

// ==================== 调课管理 ====================

export interface ScheduleAdjustment {
  id: number
  entryId: number
  originalEntry?: ScheduleEntry
  adjustmentType: number // 1-调课, 2-停课, 3-补课
  newClassroomId?: number
  newClassroomName?: string
  newDayOfWeek?: number
  newPeriodStart?: number
  newPeriodEnd?: number
  newWeek?: number
  reason: string
  status: number // 0-待审批, 1-已批准, 2-已驳回, 3-已执行, 4-已取消
  applicantId: number
  applicantName?: string
  approverId?: number
  approverName?: string
  approvalRemark?: string
  appliedAt: string
  approvedAt?: string
  executedAt?: string
}

export interface AdjustmentQueryParams {
  entryId?: number
  adjustmentType?: number
  status?: number
  applicantId?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

// ==================== 考试管理 ====================

export interface ExamBatch {
  id: number
  semesterId: number
  semesterName?: string
  name: string
  examType: number // 1-期中考试, 2-期末考试, 3-补考, 4-重修考试
  startDate: string
  endDate: string
  status: number // 0-草稿, 1-已发布, 2-进行中, 3-已结束
  remark?: string
  arrangements?: ExamArrangement[]
  createdAt?: string
  updatedAt?: string
}

export interface ExamArrangement {
  id: number
  batchId: number
  courseId: number
  courseName?: string
  courseCode?: string
  classIds: number[]
  classNames?: string[]
  examDate: string
  startTime: string
  endTime: string
  duration: number // 分钟
  examRooms?: ExamRoom[]
  status: number
}

export interface ExamRoom {
  id: number
  arrangementId: number
  classroomId: number
  classroomName?: string
  capacity: number
  actualCount: number
  invigilators?: ExamInvigilator[]
}

export interface ExamInvigilator {
  id: number
  roomId: number
  teacherId: number
  teacherName?: string
  isMain: boolean // 是否主监考
}

export interface ExamBatchQueryParams {
  semesterId?: number
  examType?: number
  status?: number
  page?: number
  size?: number
}

// ==================== 成绩管理 ====================

export interface GradeBatch {
  id: number
  semesterId: number
  semesterName?: string
  courseId: number
  courseName?: string
  courseCode?: string
  classId: number
  className?: string
  batchName: string
  gradeType: number // 1-平时成绩, 2-期中成绩, 3-期末成绩, 4-总评成绩
  status: number // 0-草稿, 1-已提交, 2-已审核, 3-已发布
  inputDeadline?: string
  grades?: StudentGrade[]
  createdBy?: number
  createdByName?: string
  createdAt?: string
  updatedAt?: string
}

export interface StudentGrade {
  id: number
  batchId: number
  studentId: number
  studentName?: string
  studentNo?: string
  semesterId: number
  courseId: number
  classId: number
  batchName?: string
  gradeType: number
  totalScore?: number
  gradeLevel?: string // A/B/C/D/F 或 优/良/中/及格/不及格
  gradePoint?: number // 绩点
  status: number // 0-未录入, 1-已录入, 2-已确认
  items?: GradeItem[]
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface GradeItem {
  id: number
  gradeId: number
  itemName: string // 如：作业1, 实验2, 考勤
  score: number
  weight: number // 权重百分比
}

export interface GradeQueryParams {
  semesterId?: number
  courseId?: number
  classId?: number
  studentId?: number
  gradeType?: number
  status?: number
  page?: number
  size?: number
}

export interface GradeStatistics {
  totalCount: number
  passCount: number
  passRate: number
  excellentCount: number
  excellentRate: number
  averageScore: number
  maxScore: number
  minScore: number
  distribution: GradeDistribution[]
}

export interface GradeDistribution {
  range: string // 如：90-100, 80-89
  count: number
  percentage: number
}

// ==================== 通用类型 ====================

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 课节时间配置
export interface PeriodConfig {
  period: number
  name: string
  startTime: string
  endTime: string
}

// 默认课节配置
export const DEFAULT_PERIODS: PeriodConfig[] = [
  { period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
  { period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
  { period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
  { period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
  { period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
  { period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
  { period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },
  { period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
  { period: 9, name: '第九节', startTime: '19:00', endTime: '19:45' },
  { period: 10, name: '第十节', startTime: '19:55', endTime: '20:40' },
]

// 星期配置
export const WEEKDAYS = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
]
