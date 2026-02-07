/**
 * 量化检查类型定义 - 桩文件
 *
 * 这些接口尚未完整实现，仅提供最小类型定义以支持 stores/inspection.ts 编译。
 * 完整实现时请替换此文件。
 */

export type TemplateStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export type RecordStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED'

export type AppealStatus =
  | 'PENDING'
  | 'REVIEWING'
  | 'DEPT_REVIEWING'
  | 'FINAL_REVIEWING'
  | 'APPROVED'
  | 'REJECTED'

export interface InspectionCategory {
  id?: number
  name?: string
  templateId?: number
  sortOrder?: number
  items?: DeductionItem[]
}

export interface DeductionItem {
  id?: number
  categoryId?: number
  name?: string
  score?: number
  description?: string
}

export interface InspectionTemplate {
  id?: number
  name?: string
  description?: string
  status?: TemplateStatus
  categories?: InspectionCategory[]
  createdAt?: string
  updatedAt?: string
}

export interface ClassScore {
  id?: number
  recordId?: number
  classId?: number
  className?: string
  score?: number
  deductions?: number
}

export interface InspectionRecord {
  id?: number
  templateId?: number
  templateName?: string
  status?: RecordStatus
  inspectorId?: number
  inspectorName?: string
  inspectionDate?: string
  classScores?: ClassScore[]
  createdAt?: string
  updatedAt?: string
}

export interface Appeal {
  id?: number
  recordId?: number
  classId?: number
  className?: string
  reason?: string
  status?: AppealStatus
  reviewerComment?: string
  createdAt?: string
  updatedAt?: string
}
