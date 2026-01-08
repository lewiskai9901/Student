// 学生相关类型定义

export interface Student {
  id: number
  studentNo: string
  idCardType?: string  // 证件类型
  ethnicity?: string  // 民族
  politicalStatus?: string  // 政治面貌
  userId: number
  username: string
  realName: string
  phone?: string
  gender: number
  genderName: string
  birthDate?: string
  identityCard?: string
  classId: number
  className?: string
  gradeId?: number
  gradeName?: string
  gradeLevel?: number
  majorId?: number
  majorName?: string
  educationLevel?: string  // 专业级别/层次
  studyLength?: string  // 学制
  degreeType?: string  // 学历
  admissionDate: string
  graduationDate?: string
  studentStatus: number
  studentStatusName: string
  guardianName?: string
  guardianPhone?: string
  guardianRelation?: string
  fatherName?: string  // 父亲姓名
  fatherIdCard?: string  // 父亲身份证号
  fatherPhone?: string  // 父亲电话
  motherName?: string  // 母亲姓名
  motherIdCard?: string  // 母亲身份证号
  motherPhone?: string  // 母亲电话
  guardianIdCard?: string  // 监护人身份证号
  emergencyContact?: string
  emergencyPhone?: string
  nativePlace?: string
  homeAddress?: string
  hukouProvince?: string  // 户口所在地-省
  hukouCity?: string  // 户口所在地-市
  hukouDistrict?: string  // 户口所在地-区
  hukouAddress?: string  // 户口详细地址
  hukouType?: string  // 户口性质
  postalCode?: string  // 邮政编码
  isPovertyRegistered?: number  // 是否建档立卡
  financialAidType?: string  // 资助申请类型
  dormitoryId?: number
  buildingNo?: string  // 楼号
  buildingName?: string  // 楼宇名称
  roomNo?: string
  bedNumber?: string
  remark?: string
  healthStatus?: string
  allergies?: string
  specialNotes?: string
  createdAt: string
  updatedAt: string
}

export interface StudentQueryRequest {
  studentNo?: string
  realName?: string
  classId?: number
  gradeId?: number
  gradeLevel?: number
  studentStatus?: number
  gender?: number
  dormitoryId?: number
  hasRoom?: boolean
  admissionDateStart?: string
  admissionDateEnd?: string
  pageNum: number
  pageSize: number
}

export interface StudentCreateRequest {
  studentNo: string
  idCardType?: string  // 证件类型
  ethnicity?: string  // 民族
  politicalStatus?: string  // 政治面貌
  username: string
  password: string
  realName: string
  phone?: string
  gender: number
  birthDate?: string
  identityCard?: string
  classId: number
  gradeId?: number
  majorId?: number
  educationLevel?: string  // 专业级别/层次
  studyLength?: string  // 学制
  degreeType?: string  // 学历
  admissionDate: string
  guardianName?: string
  guardianPhone?: string
  guardianRelation?: string
  fatherName?: string  // 父亲姓名
  fatherIdCard?: string  // 父亲身份证号
  fatherPhone?: string  // 父亲电话
  motherName?: string  // 母亲姓名
  motherIdCard?: string  // 母亲身份证号
  motherPhone?: string  // 母亲电话
  guardianIdCard?: string  // 监护人身份证号
  emergencyContact?: string
  emergencyPhone?: string
  homeAddress?: string
  hukouProvince?: string  // 户口所在地-省
  hukouCity?: string  // 户口所在地-市
  hukouDistrict?: string  // 户口所在地-区
  hukouAddress?: string  // 户口详细地址
  hukouType?: string  // 户口性质
  postalCode?: string  // 邮政编码
  isPovertyRegistered?: number  // 是否建档立卡
  financialAidType?: string  // 资助申请类型
  dormitoryId?: number
  bedNumber?: string
  healthStatus?: string
  allergies?: string
  specialNotes?: string
}

export interface StudentUpdateRequest extends Omit<StudentCreateRequest, 'username' | 'password'> {
  id: number
  graduationDate?: string
  studentStatus: number
}