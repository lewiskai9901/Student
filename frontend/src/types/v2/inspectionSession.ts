/**
 * V4 检查会话类型定义
 */

export type SessionStatus = 'CREATED' | 'IN_PROGRESS' | 'SUBMITTED' | 'PUBLISHED'
export type ClassRecordStatus = 'PENDING' | 'RECORDING' | 'COMPLETED'
export type InputMode = 'SPACE_FIRST' | 'PERSON_FIRST' | 'ORG_FIRST'
export type ScoringMode = 'DEDUCTION_ONLY' | 'BASE_SCORE' | 'DUAL_TRACK'
export type ChecklistResult = 'PASS' | 'FAIL' | 'NA'
export type SpaceType = 'DORMITORY' | 'CLASSROOM' | 'NONE'
export type InputSource = 'CHECKLIST_FAIL' | 'FREE_DEDUCTION' | 'SPACE_RESOLVED' | 'PERSON_RESOLVED'

// ==================== Session ====================

export interface InspectionSession {
  id: number
  sessionCode: string
  templateId: number
  templateVersion: number
  inspectionDate: string
  inspectionPeriod: string
  inputMode: InputMode
  scoringMode: ScoringMode
  baseScore: number
  status: SessionStatus
  inspectorId: number
  inspectorName: string
  submittedAt: string | null
  publishedAt: string | null
  remarks: string | null
  createdAt: string
}

export interface CreateSessionRequest {
  templateId: number
  inspectionDate: string
  inspectionPeriod?: string
  inputMode?: InputMode
  scoringMode?: ScoringMode
  baseScore?: number
}

// ==================== Class Record ====================

export interface ClassInspectionRecord {
  id: number
  sessionId: number
  classId: number
  className: string
  orgUnitId: number | null
  orgUnitName: string | null
  baseScore: number
  totalDeduction: number
  bonusScore: number
  finalScore: number
  status: ClassRecordStatus
  deductionCount: number
  checklistResponseCount: number
  createdAt: string
  deductions: SessionDeductionItem[]
}

export interface SessionDeductionItem {
  id: number
  itemName: string
  categoryName: string | null
  spaceType: SpaceType | null
  spaceName: string | null
  personCount: number
  deductionAmount: number
  inputSource: InputSource | null
  remark: string | null
  evidenceUrls: string[]
}

// ==================== Requests ====================

export interface SpaceDeductionRequest {
  spaceType: SpaceType
  spaceId: number
  spaceName?: string
  deductionItemId?: number
  itemName: string
  categoryName?: string
  deductionAmount: number
  personCount?: number
  studentIds?: number[]
  studentNames?: string[]
  remark?: string
  evidenceUrls?: string[]
}

export interface PersonDeductionRequest {
  studentIds: number[]
  studentNames?: string[]
  deductionItemId?: number
  itemName: string
  categoryName?: string
  deductionAmount: number
  remark?: string
  evidenceUrls?: string[]
}

export interface BatchChecklistRequest {
  classId: number
  items: ChecklistItem[]
}

export interface ChecklistItem {
  checklistItemId: number
  itemName: string
  categoryName?: string
  result: ChecklistResult
  deductionWhenFail?: number
  inspectorNote?: string
}

// ==================== Progress ====================

export interface ChecklistProgress {
  totalClasses: number
  completedClasses: number
  totalChecklistResponses: number
}
