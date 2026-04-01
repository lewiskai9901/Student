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

// ==================== Course (from types/teaching.ts) ====================

export interface Course {
  id: number | string
  code: string
  name: string
  englishName?: string
  credits: number
  totalHours: number
  theoryHours: number
  practiceHours: number
  courseType: number // 1-required, 2-elective, 3-general
  departmentId?: number | string
  departmentName?: string
  description?: string
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface CourseQueryParams {
  keyword?: string
  courseType?: number
  departmentId?: number | string
  status?: number
  page?: number
  size?: number
}

// ==================== CurriculumPlan (from types/teaching.ts) ====================

export interface CurriculumPlan {
  id: number | string
  name: string
  majorId: number | string
  majorName?: string
  majorDirectionId?: number | string
  majorDirectionName?: string
  gradeYear: number
  version: string
  totalCredits: number
  status: number // 0-draft, 1-published, 2-deprecated
  description?: string
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
  credits?: number
  semesterNumber: number
  courseCategory: number // 1-public basic, 2-major basic, 3-major core, 4-major elective, 5-practicum
  isRequired: boolean
  weeklyHours?: number
}

export interface CurriculumPlanQueryParams {
  majorId?: number | string
  majorDirectionId?: number | string
  gradeYear?: number
  status?: number
  page?: number
  size?: number
}
