// Academic domain types - majors, courses, curriculum plans

// ==================== Major (from types/major.ts) ====================

export interface Major {
  id: number | string
  majorName: string
  majorCode: string
  orgUnitId: number | string
  orgUnitName?: string
  description?: string
  status: number
  statusName?: string
  // Vocational school fields
  majorCategoryCode?: string
  enrollmentTarget?: string
  educationForm?: string
  leadTeacherId?: number | string
  leadTeacherName?: string
  approvalYear?: number
  majorStatus?: string
  createdAt: string
  updatedAt?: string
}

export interface MajorQueryParams {
  majorName?: string
  majorCode?: string
  orgUnitId?: number | string | null
  status?: number | null
  pageNum?: number
  pageSize?: number
}

export interface MajorFormData {
  majorName: string
  majorCode: string
  orgUnitId: number | string | null
  description?: string
  status: number
  majorCategoryCode?: string
  enrollmentTarget?: string
  educationForm?: string
  leadTeacherId?: number | string
  approvalYear?: number
}

// ==================== MajorDirection (from api/majorDirection.ts) ====================

export interface MajorDirection {
  id?: number
  majorId: number
  directionName: string
  directionCode: string
  level: string           // layer: intermediate/advanced/preparatory-technician/technician
  years: number           // schooling years
  isSegmented?: number    // segmented registration: 0-no, 1-yes
  phase1Level?: string
  phase1Years?: number
  phase2Level?: string
  phase2Years?: number
  remarks?: string
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  updatedBy?: number
  majorName?: string
  // Vocational school fields
  enrollmentTarget?: string
  educationForm?: string
  certificateNames?: string[]
  trainingStandard?: string
  cooperationEnterprise?: string
  maxEnrollment?: number
  // Display helpers
  levelDisplay?: string
  yearsDisplay?: string
}

export interface MajorDirectionQueryParams {
  pageNum?: number
  pageSize?: number
  majorId?: number
  directionName?: string
  level?: string
}

// ==================== Course ====================

export interface Course {
  id: number | string
  courseCode: string
  courseName: string
  courseNameEn?: string
  /** 课程类别: 1-公共基础课 2-专业核心课 3-专业方向课 4-选修课 */
  courseCategory: number
  /** 课程类型: 1-理论 2-实践 3-理论+实践 */
  courseType: number
  /** 课程性质: 1-必修 2-限选 3-任选 */
  courseNature: number
  credits: number
  totalHours: number
  theoryHours: number
  practiceHours: number
  weeklyHours: number
  /** 考核方式: 1-考试 2-考查 */
  examType: number
  orgUnitId?: number | string
  description?: string
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface CourseQueryParams {
  keyword?: string
  courseCategory?: number
  courseType?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

// ==================== CurriculumPlan ====================

export interface CurriculumPlan {
  id: number | string
  planCode: string
  planName: string
  majorId: number | string
  majorName?: string
  gradeYear: number
  totalCredits: number
  requiredCredits?: number
  electiveCredits?: number
  practiceCredits?: number
  trainingObjective?: string
  graduationRequirement?: string
  version: number
  status: number // 0-draft, 1-published, 2-deprecated
  publishedAt?: string
  publishedBy?: number | string
  courses?: PlanCourse[]
  createdAt?: string
  updatedAt?: string
}

export interface PlanCourse {
  id: number | string
  planId: number | string
  courseId: number | string
  courseName?: string
  courseCode?: string
  semesterNumber: number
  /** 课程类别: 1-公共基础 2-专业基础 3-专业核心 4-专业选修 5-实践环节 */
  courseCategory: number
  /** 课程类型: 1-必修 2-限选 3-任选 */
  courseType: number
  credits?: number
  totalHours?: number
  weeklyHours?: number
  theoryHours?: number
  practiceHours?: number
  examType?: number
  sortOrder?: number
  remark?: string
}

export interface CurriculumPlanQueryParams {
  majorId?: number | string
  gradeYear?: number
  status?: number
  pageNum?: number
  pageSize?: number
}
