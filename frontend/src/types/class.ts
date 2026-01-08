// 班级相关类型定义

export interface Class {
  id: number
  className: string
  classCode: string
  gradeLevel: number
  gradeId?: number
  departmentId: number
  departmentName: string
  majorId?: number
  majorName?: string
  majorDirectionId?: number
  classSequence?: number
  educationSystem?: string
  skillLevel?: string
  duration?: number
  grade: string
  teacherId?: number
  teacherName?: string
  assistantTeacherId?: number
  assistantTeacherName?: string
  studentCount: number
  maxStudents?: number
  classroomLocation?: string
  enrollmentYear?: number
  graduationYear?: number
  classType?: number
  classTypeName?: string
  status: number
  statusName?: string
  createdAt: string
  updatedAt?: string
}

export interface ClassQueryParams {
  className?: string
  departmentId?: number | null
  majorId?: number | null
  teacherName?: string
  status?: number | null
  pageNum?: number
  pageSize?: number
}

export interface ClassFormData {
  className: string
  classCode: string
  gradeLevel: number | null
  departmentId: number | null
  majorId?: number | null
  teacherId?: number | null
  assistantTeacherId?: number | null
  classroomLocation?: string
  enrollmentYear: number | null
  graduationYear: number | null
  classType: number
  status: number
}

/**
 * 班级宿舍关联信息
 */
export interface ClassDormitoryInfo {
  id: number
  dormitoryId: number
  dormitoryNo: string  // 房间号
  buildingNo?: string  // 楼号
  buildingName?: string  // 楼宇名称
  floorNumber?: number  // 楼层
  bedCapacity?: number  // 床位容量规格
  bedCount?: number  // 实际床位数
  roomUsageType?: number  // 房间用途类型
  occupiedBeds?: number  // 已占用床位
  allocatedBeds?: number  // 分配给班级的床位数
  genderType?: number  // 性别类型
  status?: number
}