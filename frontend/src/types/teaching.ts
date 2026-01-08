/**
 * 教学管理模块类型定义
 */

// 教学楼
export interface TeachingBuilding {
  id?: number | string
  buildingName: string
  floorCount: number
  location?: string
  description?: string
  status: number
  deleted?: number
  createdAt?: string
  updatedAt?: string
}

// 教室
export interface Classroom {
  id?: number | string
  buildingId: number | string
  buildingName?: string
  classroomName: string
  classroomCode: string
  floor: number
  roomNumber: string
  capacity: number
  classId?: number | string
  className?: string
  headTeacherId?: number | string
  headTeacherName?: string
  studentCount?: number
  classroomType?: string
  facilities?: string
  status: number
  deleted?: number
  createdAt?: string
  updatedAt?: string
}

// 查询参数
export interface BuildingQueryParams {
  pageNum?: number
  pageSize?: number
  buildingName?: string
  status?: number
}

export interface ClassroomQueryParams {
  pageNum?: number
  pageSize?: number
  buildingId?: number | string
  floor?: number
  classroomType?: string
  status?: number
}
