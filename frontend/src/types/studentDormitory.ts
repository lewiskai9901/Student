// 学生住宿记录类型定义

/**
 * 学生住宿记录
 */
export interface StudentDormitory {
  id: number
  studentId: number
  studentNo: string
  studentName: string
  className?: string
  dormitoryId: number
  buildingId?: number
  buildingNo?: string
  buildingName?: string
  dormitoryNo?: string
  floorNumber?: number
  bedNumber?: string
  checkInDate: string
  checkOutDate?: string
  status: number  // 1在住 2已退宿 3调换中
  statusName?: string
  changeReason?: string
  remark?: string
  createdAt: string
  updatedAt: string
}

/**
 * 住宿记录查询请求
 */
export interface StudentDormitoryQueryRequest {
  studentId?: number
  studentNo?: string
  studentName?: string
  dormitoryId?: number
  buildingId?: number
  floorNumber?: number
  status?: number
  checkInDateStart?: string
  checkInDateEnd?: string
  pageNum: number
  pageSize: number
}

/**
 * 学生入住请求
 */
export interface StudentCheckInRequest {
  studentId: number
  dormitoryId: number
  bedNumber?: string
  checkInDate?: string
  remark?: string
}

/**
 * 学生退宿请求
 */
export interface StudentCheckOutRequest {
  studentId: number
  checkOutDate?: string
  reason?: string
}

/**
 * 学生调换宿舍请求
 */
export interface StudentChangeDormitoryRequest {
  studentId: number
  newDormitoryId: number
  newBedNumber?: string
  changeDate?: string
  reason?: string
}

/**
 * 住宿状态选项
 */
export const dormitoryStatusOptions = [
  { label: '在住', value: 1 },
  { label: '已退宿', value: 2 },
  { label: '调换中', value: 3 }
]

/**
 * 获取状态名称
 */
export const getStatusName = (status: number): string => {
  const statusMap: Record<number, string> = {
    1: '在住',
    2: '已退宿',
    3: '调换中'
  }
  return statusMap[status] || '未知'
}

/**
 * 获取状态样式类
 */
export const getStatusClass = (status: number): string => {
  const classMap: Record<number, string> = {
    1: 'bg-green-50 text-green-700',
    2: 'bg-gray-100 text-gray-700',
    3: 'bg-amber-50 text-amber-700'
  }
  return classMap[status] || 'bg-gray-100 text-gray-700'
}
