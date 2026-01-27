/**
 * 量化检查模块类型定义 - DDD架构适配
 */

// 模板范围
export type TemplateScope = 'SCHOOL' | 'DEPARTMENT' | 'CUSTOM'

// 模板状态
export type TemplateStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

// 扣分模式
export type DeductionMode = 'FIXED' | 'PER_PERSON' | 'SCORE_RANGE'

// 扣分项
export interface DeductionItem {
  id: number
  itemCode: string
  itemName: string
  deductionMode: DeductionMode
  baseScore: number
  maxDeduction?: number
  scoreRanges?: ScoreRange[]
  description?: string
  sortOrder: number
}

// 分数范围
export interface ScoreRange {
  minCount: number
  maxCount: number
  score: number
}

// 检查类别
export interface InspectionCategory {
  id: number
  categoryCode: string
  categoryName: string
  weight: number
  sortOrder: number
  deductionItems: DeductionItem[]
}

// 检查模板
export interface InspectionTemplate {
  id: number
  templateCode: string
  templateName: string
  scope: TemplateScope
  status: TemplateStatus
  baseScore: number
  description?: string
  categories: InspectionCategory[]
  createdAt: string
  updatedAt: string
  publishedAt?: string
}

// 创建模板请求
export interface CreateTemplateRequest {
  templateCode: string
  templateName: string
  scope: TemplateScope
  baseScore?: number
  description?: string
}

// 添加类别请求
export interface AddCategoryRequest {
  categoryCode: string
  categoryName: string
  weight: number
  sortOrder?: number
}

// 记录状态
export type RecordStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'PUBLISHED'

// 班级得分
export interface ClassScore {
  classId: number
  className: string
  baseScore: number
  totalDeduction: number
  finalScore: number
  rank?: number
  deductionDetails: DeductionDetail[]
}

// 扣分明细
export interface DeductionDetail {
  itemId: number
  itemName: string
  count: number
  score: number
  remark?: string
  images?: string[]
}

// 检查记录
export interface InspectionRecord {
  id: number
  recordCode: string
  templateId: number
  templateName: string
  inspectionDate: string
  inspectorId: number
  inspectorName: string
  status: RecordStatus
  classScores: ClassScore[]
  totalClasses: number
  averageScore: number
  createdAt: string
  publishedAt?: string
}

// 创建记录请求
export interface CreateRecordRequest {
  templateId: number
  inspectionDate: string
  classIds: number[]
}

// 记录扣分请求
export interface RecordDeductionRequest {
  classId: number
  itemId: number
  count: number
  remark?: string
  images?: string[]
}

// 申诉状态
export type AppealStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'FIRST_REVIEWING'
  | 'FIRST_APPROVED'
  | 'FIRST_REJECTED'
  | 'FINAL_REVIEWING'
  | 'FINAL_APPROVED'
  | 'FINAL_REJECTED'
  | 'WITHDRAWN'

// 申诉审批记录
export interface AppealApproval {
  id: number
  level: number
  approverId: number
  approverName: string
  approved: boolean
  comment?: string
  approvedAt: string
}

// 申诉
export interface Appeal {
  id: number
  appealCode: string
  recordId: number
  classId: number
  className: string
  itemId: number
  itemName: string
  originalScore: number
  appealReason: string
  evidenceUrls: string[]
  status: AppealStatus
  applicantId: number
  applicantName: string
  approvalRecords: AppealApproval[]
  createdAt: string
  updatedAt: string
}

// 创建申诉请求
export interface CreateAppealRequest {
  recordId: number
  classId: number
  itemId: number
  appealReason: string
  evidenceUrls?: string[]
}

// 审核申诉请求
export interface ReviewAppealRequest {
  approved: boolean
  comment?: string
}

// 申诉统计
export interface AppealStatistics {
  totalCount: number
  pendingCount: number
  approvedCount: number
  rejectedCount: number
  approvalRate: number
}

// 状态显示配置
export const TemplateStatusConfig: Record<TemplateStatus, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  ARCHIVED: { label: '已归档', type: 'warning' }
}

export const RecordStatusConfig: Record<RecordStatus, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'primary' },
  APPROVED: { label: '已审核', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  PUBLISHED: { label: '已发布', type: 'success' }
}

export const AppealStatusConfig: Record<AppealStatus, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '待初审', type: 'warning' },
  FIRST_REVIEWING: { label: '初审中', type: 'primary' },
  FIRST_APPROVED: { label: '初审通过', type: 'success' },
  FIRST_REJECTED: { label: '初审驳回', type: 'danger' },
  FINAL_REVIEWING: { label: '终审中', type: 'primary' },
  FINAL_APPROVED: { label: '终审通过', type: 'success' },
  FINAL_REJECTED: { label: '终审驳回', type: 'danger' },
  WITHDRAWN: { label: '已撤回', type: 'info' }
}

export const DeductionModeConfig: Record<DeductionMode, { label: string; description: string }> = {
  FIXED: { label: '固定扣分', description: '每次扣固定分数' },
  PER_PERSON: { label: '按人扣分', description: '扣分 = 人数 × 基础分' },
  SCORE_RANGE: { label: '分段扣分', description: '根据数量分段扣分' }
}
