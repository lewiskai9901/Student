/** 后端 PageResult 形状 (com.school.management.common.PageResult) */
export interface PageResult<T> {
  records: T[]
  total: number
  /** 每页数量 */
  size?: number
  /** 当前页 */
  current?: number
  /** 总页数 */
  pages?: number
}

/** 学生 (后端 StudentDTO 子集) */
export interface Student {
  id: number
  studentNo: string
  name: string
  gender?: number
  genderText?: string
  phone?: string
  email?: string
  enrollmentDate?: string
  expectedGraduationDate?: string
  orgUnitId?: number
  className?: string
  status?: number
  statusText?: string
  avatarUrl?: string
  remark?: string
  createdAt?: string
}

/**
 * 班级 (后端 SchoolClassResponse 子集)
 * 注意: 后端 status 是字符串枚举(ACTIVE/INACTIVE/...),没有 statusText 字段;
 * headTeacherName 而非 classTeacherName; currentSize 而非 headcount
 */
export interface SchoolClass {
  id: number
  classCode: string
  className: string
  shortName?: string
  orgUnitId?: number
  orgUnitName?: string
  enrollmentYear?: number
  gradeLevel?: number
  majorName?: string
  majorDirectionName?: string
  schoolingYears?: number
  standardSize?: number
  currentSize?: number
  status?: string
  expectedGraduationYear?: number
  availableSlots?: number
  headTeacherName?: string
  createdAt?: string
}
