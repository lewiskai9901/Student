// 教务管理模块类型定义

// ==================== 校历管理 ====================

export interface AcademicYear {
  id: number | string
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
  id: number | string
  yearId: number | string
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
  id: number | string
  semesterId: number | string
  weekNumber: number
  startDate: string
  endDate: string
  weekType: number // 1-教学周, 2-考试周, 3-假期
  remark?: string
}

export interface AcademicEvent {
  id: number | string
  yearId?: number | string
  semesterId?: number | string
  title: string
  eventType: number // 1-开学, 2-放假, 3-考试, 4-活动, 5-其他
  startDate: string
  endDate?: string
  allDay: boolean
  description?: string
}

// ==================== 课程管理 (re-export from academic) ====================

export type {
  Course,
  CourseQueryParams,
  CurriculumPlan,
  PlanCourse,
  CurriculumPlanQueryParams,
} from './academic'

// ==================== 教学任务 ====================

export interface TeachingTask {
  id: number | string
  semesterId: number | string
  semesterName?: string
  courseId: number | string
  courseName?: string
  courseCode?: string
  classId: number | string
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
  id: number | string
  taskId: number | string
  teacherId: number | string
  teacherName?: string
  isMain: boolean // 是否主讲教师
}

export interface TeachingTaskQueryParams {
  semesterId?: number | string
  courseId?: number | string
  classId?: number | string
  teacherId?: number | string
  status?: number
  page?: number
  size?: number
}

// ==================== 排课管理 ====================

export interface CourseSchedule {
  id: number | string
  semesterId: number | string
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
  id: number | string
  scheduleId: number | string
  taskId: number | string
  courseName?: string
  className?: string
  teacherName?: string
  classroomId: number | string
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
  scheduleId: number | string
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
  id: number | string
  entryId: number | string
  originalEntry?: ScheduleEntry
  adjustmentType: number // 1-调课, 2-停课, 3-补课
  newClassroomId?: number | string
  newClassroomName?: string
  newDayOfWeek?: number
  newPeriodStart?: number
  newPeriodEnd?: number
  newWeek?: number
  reason: string
  status: number // 0-待审批, 1-已批准, 2-已驳回, 3-已执行, 4-已取消
  applicantId: number | string
  applicantName?: string
  approverId?: number | string
  approverName?: string
  approvalRemark?: string
  appliedAt: string
  approvedAt?: string
  executedAt?: string
}

export interface AdjustmentQueryParams {
  entryId?: number | string
  adjustmentType?: number
  status?: number
  applicantId?: number | string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

// ==================== 考试管理 ====================

export interface ExamBatch {
  id: number | string
  semesterId: number | string
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
  id: number | string
  batchId: number | string
  courseId: number | string
  courseName?: string
  courseCode?: string
  classIds: (number | string)[]
  classNames?: string[]
  examDate: string
  startTime: string
  endTime: string
  duration: number // 分钟
  examRooms?: ExamRoom[]
  status: number
}

export interface ExamRoom {
  id: number | string
  arrangementId: number | string
  classroomId: number | string
  classroomName?: string
  capacity: number
  actualCount: number
  invigilators?: ExamInvigilator[]
}

export interface ExamInvigilator {
  id: number | string
  roomId: number | string
  teacherId: number | string
  teacherName?: string
  isMain: boolean // 是否主监考
}

export interface ExamBatchQueryParams {
  semesterId?: number | string
  examType?: number
  status?: number
  page?: number
  size?: number
}

// ==================== 成绩管理 ====================

export interface GradeBatch {
  id: number | string
  semesterId: number | string
  semesterName?: string
  courseId: number | string
  courseName?: string
  courseCode?: string
  classId: number | string
  className?: string
  batchName: string
  gradeType: number // 1-平时成绩, 2-期中成绩, 3-期末成绩, 4-总评成绩
  status: number // 0-草稿, 1-已提交, 2-已审核, 3-已发布
  inputDeadline?: string
  grades?: StudentGrade[]
  createdBy?: number | string
  createdByName?: string
  createdAt?: string
  updatedAt?: string
}

export interface StudentGrade {
  id: number | string
  batchId: number | string
  studentId: number | string
  studentName?: string
  studentNo?: string
  semesterId: number | string
  courseId: number | string
  classId: number | string
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
  id: number | string
  gradeId: number | string
  itemName: string // 如：作业1, 实验2, 考勤
  score: number
  weight: number // 权重百分比
}

export interface GradeQueryParams {
  semesterId?: number | string
  courseId?: number | string
  classId?: number | string
  studentId?: number | string
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

// ==================== 教学楼/教室 ====================

export interface TeachingBuilding {
  id: number | string
  buildingNo: string
  buildingName: string
  buildingType: number
  floorCount: number
  address?: string
  status: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface Classroom {
  id: number | string
  buildingId: number | string
  buildingName?: string
  roomNo: string
  roomName?: string
  floorNumber: number
  roomType: number
  capacity: number
  equipment?: string
  status: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface ClassroomQueryParams {
  buildingId?: number | string
  floorNumber?: number
  roomType?: number
  status?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
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

// =====================================================
// 开课管理 (Offering Management)
// =====================================================

export interface SemesterOffering {
  id: number
  semesterId: number
  courseId: number
  courseName?: string
  courseCode?: string
  applicableGrade: string
  weeklyHours: number
  totalWeeks?: number
  startWeek: number
  endWeek?: number
  courseCategory?: number
  courseType?: number
  allowCombined: boolean
  maxCombinedClasses: number
  allowWalking: boolean
  status: number
  remark?: string
}

export interface ClassCourseAssignment {
  id: number
  semesterId: number
  classId: number
  className?: string
  offeringId: number
  courseId: number
  courseName?: string
  weeklyHours: number
  studentCount?: number
  status: number
}

// =====================================================
// 教学班 (Teaching Class)
// =====================================================

export interface TeachingClass {
  id: number
  semesterId: number
  className: string
  classCode?: string
  courseId: number
  courseName?: string
  classType: 1 | 2 | 3
  weeklyHours: number
  studentCount: number
  requiredRoomType?: string
  requiredCapacity?: number
  startWeek: number
  endWeek?: number
  status: number
  remark?: string
  members?: TeachingClassMember[]
}

export interface TeachingClassMember {
  id: number
  teachingClassId: number
  memberType: 1 | 2
  adminClassId?: number
  adminClassName?: string
  studentId?: number
  studentName?: string
}

export const TEACHING_CLASS_TYPES = [
  { value: 1, label: '普通' },
  { value: 2, label: '合堂' },
  { value: 3, label: '走班' },
] as const

// =====================================================
// 排课约束 (Scheduling Constraints)
// =====================================================

export interface SchedulingConstraint {
  id: number
  semesterId: number
  constraintName: string
  constraintLevel: 1 | 2 | 3 | 4
  targetId?: number
  targetName?: string
  constraintType: string
  isHard: boolean
  priority: number
  params: Record<string, any>
  effectiveWeeks?: string
  enabled: boolean
}

export const CONSTRAINT_LEVELS = [
  { value: 1, label: '全局' },
  { value: 2, label: '教师' },
  { value: 3, label: '班级' },
  { value: 4, label: '课程' },
] as const

export const CONSTRAINT_TYPES = [
  { value: 'TIME_FORBIDDEN', label: '时间禁排', isHardDefault: true },
  { value: 'TIME_FIXED', label: '时间固定', isHardDefault: true },
  { value: 'MAX_DAILY', label: '每日上限', isHardDefault: true },
  { value: 'MAX_CONSECUTIVE', label: '最大连排', isHardDefault: true },
  { value: 'ROOM_REQUIRED', label: '教室要求', isHardDefault: true },
  { value: 'TIME_PREFERRED', label: '时间偏好', isHardDefault: false },
  { value: 'TIME_AVOIDED', label: '时间回避', isHardDefault: false },
  { value: 'SPREAD_EVEN', label: '均匀分布', isHardDefault: false },
  { value: 'MORNING_PRIORITY', label: '上午优先', isHardDefault: false },
  { value: 'COMPACT_SCHEDULE', label: '紧凑排课', isHardDefault: false },
  { value: 'MIN_GAP', label: '最小间隔', isHardDefault: false },
  { value: 'ROOM_PREFERRED', label: '教室偏好', isHardDefault: false },
] as const

// =====================================================
// 时间矩阵 & 冲突检测
// =====================================================

export interface TimeSlotStatus {
  day: number
  period: number
  status: 'available' | 'forbidden' | 'preferred' | 'avoided'
  reasons: string[]
}

export type TimeMatrix = TimeSlotStatus[][]

export interface DetectedConflict {
  id: number
  semesterId: number
  detectionBatch?: string
  conflictCategory: 1 | 2 | 3
  conflictType: string
  severity: 1 | 2 | 3
  description: string
  detail?: Record<string, any>
  entryId1?: number
  entryId2?: number
  constraintId?: number
  resolutionStatus: 0 | 1 | 2
  resolutionNote?: string
}

export interface FeasibilityReport {
  blockingIssues: FeasibilityIssue[]
  warnings: FeasibilityIssue[]
  passedChecks: number
}

export interface FeasibilityIssue {
  type: string
  target: string
  description: string
  suggestion: string
}
