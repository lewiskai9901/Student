/**
 * 学生管理类型定义 - DDD架构适配
 */

// ==================== 枚举类型 ====================

/**
 * 学生状态
 */
export type StudentStatus = 0 | 1 | 2 | 3 | 4

export const StudentStatusMap: Record<StudentStatus, string> = {
  0: '在读',
  1: '休学',
  2: '退学',
  3: '毕业',
  4: '转学'
}

/**
 * 性别
 */
export type Gender = 1 | 2

export const GenderMap: Record<Gender, string> = {
  1: '男',
  2: '女'
}

// ==================== 实体类型 ====================

/**
 * 学生实体 - V2 DDD架构字段名
 */
export interface Student {
  id: number | string
  studentNo: string
  userId?: number | string
  username?: string

  // V2 使用 name 代替 realName
  name: string
  phone?: string
  email?: string
  gender: Gender
  // V2 使用 genderText 代替 genderName
  genderText?: string
  idCard?: string
  idCardType?: string
  ethnicity?: string
  politicalStatus?: string
  birthDate?: string
  nativePlace?: string
  avatarUrl?: string

  // 班级信息 (gradeId/majorId/majorDirectionId are on class, not student)
  orgUnitId?: number | string
  className?: string
  gradeName?: string
  gradeLevel?: number
  majorName?: string

  // 学籍信息
  educationLevel?: string
  studyLength?: string
  degreeType?: string
  // V2 使用 enrollmentDate 代替 admissionDate
  enrollmentDate?: string
  expectedGraduationDate?: string
  // V2 使用 status 代替 studentStatus
  status: StudentStatus
  // V2 使用 statusText 代替 studentStatusName
  statusText?: string

  // 宿舍信息已统一到 place_occupants 管理体系
  // 通过 /v9/places API 查询学生入住信息

  // 家庭信息
  homeAddress?: string
  hukouProvince?: string
  hukouCity?: string
  hukouDistrict?: string
  hukouAddress?: string
  hukouType?: string
  postalCode?: string

  // 紧急联系人 (V2 使用)
  emergencyContact?: string
  emergencyPhone?: string

  // 监护人信息 (兼容扩展字段)
  guardianName?: string
  guardianPhone?: string
  guardianRelation?: string
  guardianIdCard?: string
  fatherName?: string
  fatherIdCard?: string
  fatherPhone?: string
  motherName?: string
  motherIdCard?: string
  motherPhone?: string

  // 其他信息
  isPovertyRegistered?: number
  financialAidType?: string
  healthStatus?: string
  allergies?: string
  specialNotes?: string
  remark?: string

  // 时间戳
  createdAt?: string
  updatedAt?: string
}

// ==================== 请求类型 ====================

/**
 * 创建学生请求 - V2 DDD架构字段名
 */
export interface CreateStudentRequest {
  studentNo?: string
  username?: string
  password?: string
  // V2 使用 name 代替 realName
  name: string
  phone?: string
  email?: string
  gender: Gender
  idCard?: string
  idCardType?: string
  ethnicity?: string
  politicalStatus?: string
  birthDate?: string
  nativePlace?: string

  // 班级信息 (gradeId/majorId/majorDirectionId are on class, not student)
  orgUnitId?: number | string

  // 学籍信息
  educationLevel?: string
  studyLength?: string
  degreeType?: string
  // V2 使用 enrollmentDate 代替 admissionDate
  enrollmentDate?: string
  expectedGraduationDate?: string
  // V2 使用 status 代替 studentStatus
  status?: StudentStatus
  entryLevel?: string
  educationSystem?: string
  graduatedSchool?: string

  // 家庭信息
  homeAddress?: string
  hukouProvince?: string
  hukouCity?: string
  hukouDistrict?: string
  hukouAddress?: string
  hukouType?: string
  postalCode?: string

  // 紧急联系人
  emergencyContact?: string
  emergencyPhone?: string

  // 监护人信息
  guardianName?: string
  guardianPhone?: string
  guardianRelation?: string
  guardianIdCard?: string
  fatherName?: string
  fatherIdCard?: string
  fatherPhone?: string
  motherName?: string
  motherIdCard?: string
  motherPhone?: string

  // 其他信息
  isPovertyRegistered?: number
  financialAidType?: string
  healthStatus?: string
  allergies?: string
  specialNotes?: string
  remark?: string
}

/**
 * 更新学生请求
 */
export interface UpdateStudentRequest extends Partial<CreateStudentRequest> {
  id?: number | string
}

/**
 * 学生查询参数 - V2 DDD架构字段名
 */
export interface StudentQueryParams {
  studentNo?: string
  // V2 使用 name 代替 realName (keyword 用于综合搜索)
  name?: string
  keyword?: string
  orgUnitId?: number | string
  gradeId?: number | string // UI-only: used to filter class dropdown, not a student field
  gradeLevel?: number
  // V2 使用 status 代替 studentStatus
  status?: StudentStatus
  gender?: Gender
  enrollmentDateStart?: string
  enrollmentDateEnd?: string
  pageNum?: number
  pageSize?: number
  orgUnitIds?: (number | string)[]
  orgUnitIds?: (number | string)[]
  selfUserId?: number | string
}

/**
 * 学生搜索参数
 */
export interface StudentSearchParams {
  keyword: string
  orgUnitId?: number | string
  limit?: number
}

/**
 * 重置密码请求
 */
export interface ResetPasswordRequest {
  newPassword: string
}

// ==================== 学籍异动记录 ====================

/**
 * 学籍异动记录
 */
export interface StudentStatusChange {
  id: number
  studentId: number
  studentNo?: string
  studentName?: string
  changeType: string
  fromStatus?: string
  toStatus?: string
  fromClassId?: number
  fromClassName?: string
  toClassId?: number
  toClassName?: string
  reason?: string
  effectiveDate?: string
  operatorId?: number
  operatorName?: string
  remark?: string
  createdAt: string
}

/**
 * 异动类型映射
 */
export const ChangeTypeMap: Record<string, string> = {
  ENROLL: '入学',
  SUSPEND: '休学',
  RESUME: '复学',
  GRADUATE: '毕业',
  WITHDRAW: '退学',
  EXPEL: '开除',
  TRANSFER_CLASS: '转班',
  TRANSFER_MAJOR: '转专业'
}

// ==================== 统计类型 ====================

/**
 * 学生统计
 */
export interface StudentStatistics {
  total: number
  active: number
  suspended: number
  withdrawn: number
  graduated: number
  transferred: number
  byGender: {
    male: number
    female: number
  }
  byGrade: Record<string, number>
  byClass: Record<string, number>
}
