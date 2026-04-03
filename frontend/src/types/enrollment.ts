export interface EnrollmentPlan {
  id: number
  academicYear: number
  majorId: number
  majorName?: string
  majorDirectionId?: number
  majorDirectionName?: string
  orgUnitId?: number
  orgUnitName?: string
  plannedCount: number
  actualCount: number
  registeredCount: number
  enrollmentTarget?: string
  status: number
  remark?: string
  createdAt?: string
}

export interface EnrollmentApplication {
  id: number
  planId: number
  academicYear: number
  applicantName: string
  gender?: number
  idCard?: string
  phone?: string
  guardianName?: string
  guardianPhone?: string
  graduateFrom?: string
  majorId: number
  majorName?: string
  majorDirectionId?: number
  majorDirectionName?: string
  applicationDate?: string
  examScore?: number
  status: number // 0待审核 1已录取 2未录取 3已报到 4已放弃
  reviewComment?: string
  assignedClassId?: number
  assignedClassName?: string
  assignedStudentId?: number
  remark?: string
  createdAt?: string
  registeredAt?: string
}

export const APPLICATION_STATUS = [
  { value: 0, label: '待审核', type: 'warning' },
  { value: 1, label: '已录取', type: 'success' },
  { value: 2, label: '未录取', type: 'danger' },
  { value: 3, label: '已报到', type: 'primary' },
  { value: 4, label: '已放弃', type: 'info' },
] as const

export const PLAN_STATUS = [
  { value: 0, label: '草稿', type: 'info' },
  { value: 1, label: '已发布', type: 'success' },
  { value: 2, label: '招生中', type: 'primary' },
  { value: 3, label: '已结束', type: 'warning' },
] as const
