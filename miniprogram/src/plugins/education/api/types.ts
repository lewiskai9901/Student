import type { LongId } from '@core/types'

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
  id: LongId
  studentNo: string
  name: string
  gender?: number
  genderText?: string
  phone?: string
  email?: string
  enrollmentDate?: string
  expectedGraduationDate?: string
  orgUnitId?: LongId
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
  id: LongId
  classCode: string
  className: string
  shortName?: string
  orgUnitId?: LongId
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

/** 我管理的班级 (后端 MyClassDTO) */
export interface MyClass {
  id: LongId
  classCode: string
  className: string
  shortName?: string
  currentSize?: number
  standardSize?: number
  status?: string
  enrollmentYear?: number
  majorName?: string
  orgUnitName?: string
  myRole?: string
  weeklyRank?: number
  totalClasses?: number
  weeklyScore?: number
  scoreTrend?: number[]
}

/** 班级概览 (后端 MyClassOverviewDTO) */
export interface MyClassOverview {
  orgUnitId: LongId
  className: string
  studentCount?: number
  maleCount?: number
  femaleCount?: number
  classRank?: number
  totalClasses?: number
  averageScore?: number
  scoreTrend?: number
  pendingAppeals?: number
  scoreTrendList?: { date: string; score: number }[]
  recentRecords?: { id: LongId; checkDate: string; checkType: string; score: number; rank?: number }[]
}

/** 班级学生 (后端 MyClassStudentDTO) */
export interface MyClassStudent {
  id: LongId
  studentNo: string
  name: string
  gender?: string
  phone?: string
  dormitoryName?: string
  bedNo?: string
  status?: string
}

/**
 * 宿舍分布 (后端 DormitoryDistributionDTO)
 * 注意: rooms[].user_student 字段名匹配后端 Java 字段 (DormitoryRoomDTO.user_student),
 * Jackson 默认按字段名序列化 → JSON key 也是 user_student (snake_case)
 */
export interface DormitoryDistribution {
  buildingId: LongId
  buildingName: string
  buildingType?: string
  studentCount?: number
  rooms: {
    dormitoryId: LongId
    roomNo: string
    floor?: number
    studentCount?: number
    // matches backend Java field name (DormitoryRoomDTO.user_student)
    user_student: { id: LongId; name: string; bedNo?: string }[]
  }[]
}
