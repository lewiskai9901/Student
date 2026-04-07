export interface SubmissionObservation {
  id: number
  submissionId: number
  detailId: number
  projectId: number | null
  taskId: number | null
  itemCode: string | null
  itemName: string | null
  itemType: string | null
  sectionName: string | null
  subjectType: string
  subjectId: number
  subjectName: string | null
  classId: number | null
  className: string | null
  score: number
  isNegative: boolean | number
  severity: string | null
  isFlagged: boolean | number
  linkedEventTypeCode: string | null
  responseValue: string | null
  description: string | null
  observedAt: string
  createdAt: string
}

export interface ObservationPageResult {
  records: SubmissionObservation[]
  total: number
  current: number
  size: number
}

export const SEVERITY_CONFIG: Record<string, { label: string; color: string; bg: string }> = {
  LOW: { label: '轻微', color: '#6b7280', bg: '#f3f4f6' },
  MEDIUM: { label: '中等', color: '#d97706', bg: '#fef3c7' },
  HIGH: { label: '严重', color: '#ea580c', bg: '#ffedd5' },
  CRITICAL: { label: '危急', color: '#dc2626', bg: '#fef2f2' },
}
