export interface WarningRule {
  id: number
  ruleName: string
  ruleType: string
  warningLevel: 1 | 2 | 3
  conditionParams: Record<string, any>
  applicableGrades?: string
  enabled: boolean
  createdAt?: string
}

export interface AcademicWarning {
  id: number
  studentId: number
  studentNo?: string
  studentName?: string
  classId?: number
  className?: string
  ruleId?: number
  ruleName?: string
  warningType: string
  warningLevel: 1 | 2 | 3
  description: string
  detail?: Record<string, any>
  status: 0 | 1 | 2 | 3
  handlerId?: number
  handleNote?: string
  handledAt?: string
  semesterId?: number
  createdAt: string
}

export interface WarningStatistics {
  totalWarnings: number
  byLevel: { level: number; count: number }[]
  byType: { type: string; count: number }[]
  byStatus: { status: number; count: number }[]
}

export const WARNING_LEVELS = [
  { value: 1, label: '黄色预警', color: '#E6A23C' },
  { value: 2, label: '橙色预警', color: '#F56C6C' },
  { value: 3, label: '红色预警', color: '#C45656' },
] as const

export const WARNING_TYPES = [
  { value: 'GRADE_FAIL', label: '挂科预警' },
  { value: 'ATTENDANCE_LOW', label: '出勤预警' },
  { value: 'CREDIT_SHORT', label: '学分不足' },
] as const

export const WARNING_STATUS = [
  { value: 0, label: '未处理', type: 'danger' },
  { value: 1, label: '已确认', type: 'warning' },
  { value: 2, label: '已干预', type: 'primary' },
  { value: 3, label: '已解除', type: 'success' },
] as const
