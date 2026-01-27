/**
 * 学生模块类型定义
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
 * 学生实体
 */
export interface Student {
  id: number
  studentNo: string
  userId?: number
  username?: string
  name: string
  phone?: string
  email?: string
  gender: Gender
  genderText?: string
  idCard?: string
  idCardType?: string
  ethnicity?: string
  politicalStatus?: string
  birthDate?: string
  nativePlace?: string
  avatarUrl?: string

  // 班级信息
  classId?: number
  className?: string
  gradeId?: number
  gradeName?: string
  gradeLevel?: number
  majorId?: number
  majorName?: string
  majorDirectionId?: number

  // 学籍信息
  educationLevel?: string
  studyLength?: string
  degreeType?: string
  enrollmentDate?: string
  expectedGraduationDate?: string
  status: StudentStatus
  statusText?: string

  // 宿舍信息
  dormitoryId?: number
  dormitoryName?: string
  buildingNo?: string
  buildingName?: string
  roomNo?: string
  bedNumber?: number | string

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

  // 时间戳
  createdAt?: string
  updatedAt?: string
}

// ==================== 请求类型 ====================

/**
 * 创建学生请求
 */
export interface CreateStudentRequest {
  studentNo?: string
  username?: string
  password?: string
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
  classId?: number
  gradeId?: number
  majorId?: number
  majorDirectionId?: number
  educationLevel?: string
  studyLength?: string
  degreeType?: string
  enrollmentDate?: string
  expectedGraduationDate?: string
  status?: StudentStatus
  entryLevel?: string
  educationSystem?: string
  graduatedSchool?: string
  dormitoryId?: number
  bedNumber?: number | string
  homeAddress?: string
  hukouProvince?: string
  hukouCity?: string
  hukouDistrict?: string
  hukouAddress?: string
  hukouType?: string
  postalCode?: string
  emergencyContact?: string
  emergencyPhone?: string
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
  id?: number
}

/**
 * 学生查询参数
 */
export interface StudentQueryParams {
  studentNo?: string
  name?: string
  keyword?: string
  classId?: number
  gradeId?: number
  gradeLevel?: number
  status?: StudentStatus
  gender?: Gender
  dormitoryId?: number
  enrollmentDateStart?: string
  enrollmentDateEnd?: string
  pageNum?: number
  pageSize?: number
  classIds?: number[]
  orgUnitIds?: number[]
  selfUserId?: number
}

/**
 * 学生搜索参数
 */
export interface StudentSearchParams {
  keyword: string
  classId?: number
  limit?: number
}

/**
 * 分配宿舍请求
 */
export interface AssignDormitoryRequest {
  dormitoryId: number
  bedNumber?: string
}

/**
 * 重置密码请求
 */
export interface ResetPasswordRequest {
  newPassword: string
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
