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
export type BonusMode = 'FIXED' | 'PROGRESSIVE' | 'IMPROVEMENT'
export type InspectionLevel = 'CLASS' | 'DEPARTMENT' | 'SPECIAL'

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
  inspectionLevel: InspectionLevel
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
  inspectionLevel?: InspectionLevel
}

// ==================== Bonus Item ====================

export interface BonusItem {
  id: number
  categoryId: number
  itemName: string
  bonusMode: BonusMode
  fixedBonus: number | null
  progressiveConfig: string | null
  improvementCoefficient: number | null
  description: string | null
  sortOrder: number
  enabled: boolean
}

export interface CreateBonusItemRequest {
  categoryId: number
  itemName: string
  bonusMode: string
  fixedBonus?: number
  progressiveConfig?: string
  improvementCoefficient?: number
  description?: string
  sortOrder?: number
}

export interface InspectionBonusRecord {
  id: number
  classRecordId: number
  sessionId: number
  classId: number
  bonusItemId: number
  bonusScore: number
  reason: string | null
  recordedBy: number | null
  createdAt: string
}

export interface RecordBonusRequest {
  classId: number
  bonusItemId: number
  bonusScore: number
  reason?: string
}

// ==================== Teacher Dashboard ====================

export interface TeacherOverview {
  classId: number
  weeklyDeduction: number
  weeklyBonus: number
  recordCount: number
}

export interface DeductionDetailRecord {
  id: number
  sessionId: number
  itemName: string
  categoryName: string
  spaceType: string | null
  spaceName: string | null
  personCount: number | null
  deductionAmount: number
  remark: string | null
  createdAt: string
}

export interface TopIssueItem {
  issueName: string
  occurrenceCount: number
  totalDeduction: number
  categoryName: string
}

export interface StudentViolationItem {
  studentId: number
  studentName: string
  violationCount: number
  totalDeduction: number
  violationTypes: string[]
}

export interface ImprovementData {
  currentDeduction: number
  previousDeduction: number
  change: number
  changePercent: number
  improved: boolean
}

// ==================== Department Ranking ====================

export interface DepartmentRankingItem {
  orgUnitId: number
  orgUnitName: string
  averageClassScore: number
  classCount: number
  compositeScore: number
  ranking: number
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
