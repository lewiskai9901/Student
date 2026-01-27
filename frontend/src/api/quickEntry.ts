import http from '@/utils/request'

/**
 * 快捷录入 - 扣分项DTO
 */
export interface QuickEntryDeductionItemDTO {
  id: number
  itemName: string
  categoryId: number
  categoryName: string
  baseScore: number
  perPersonScore: number
  allowPhoto: number
  allowRemark: number
  description?: string
}

/**
 * 快捷录入 - 学生DTO
 */
export interface QuickEntryStudentDTO {
  id: number
  userId: number
  studentNo: string
  realName: string
  classId: number
  className: string
  majorName?: string
  departmentName?: string
  avatar?: string
}

/**
 * 快捷录入 - 提交请求
 */
export interface QuickEntrySubmitRequest {
  deductionItemId: number
  studentId: number
  remark?: string
  photoUrls?: string[]
}

/**
 * 快捷录入 - 记录DTO
 */
export interface QuickEntryRecordDTO {
  id: number
  deductionItemId: number
  deductionItemName: string
  categoryName: string
  studentId: number
  studentName: string
  studentNo: string
  classId: number
  className: string
  deductScore: number
  remark?: string
  photoUrls?: string[]
  createdAt: string
  canRevoke: boolean
}

/**
 * 快捷录入 - 检查重复请求
 */
export interface QuickEntryCheckDuplicateRequest {
  deductionItemId: number
  studentId: number
  checkRound?: number
}

/**
 * 快捷录入 - 检查重复响应
 */
export interface QuickEntryCheckDuplicateResponse {
  isDuplicate: boolean
  existingRecordId?: number
  existingRecordTime?: string
  existingRecordOperator?: string
  message?: string
}

/**
 * 获取可用扣分项列表
 */
export function getQuickEntryDeductionItems(checkId: number | string) {
  return http.get<QuickEntryDeductionItemDTO[]>(
    `/quantification/daily-checks/${checkId}/quick-entry/deduction-items`
  )
}

/**
 * 搜索学生
 */
export function searchStudentsForQuickEntry(
  checkId: number | string,
  keyword: string,
  limit: number = 20
) {
  return http.get<QuickEntryStudentDTO[]>(
    `/quantification/daily-checks/${checkId}/quick-entry/search-students`,
    { params: { keyword, limit } }
  )
}

/**
 * 检查重复
 */
export function checkQuickEntryDuplicate(
  checkId: number | string,
  data: QuickEntryCheckDuplicateRequest
) {
  return http.post<QuickEntryCheckDuplicateResponse>(
    `/quantification/daily-checks/${checkId}/quick-entry/check-duplicate`,
    data
  )
}

/**
 * 提交快捷录入
 */
export function submitQuickEntry(
  checkId: number | string,
  data: QuickEntrySubmitRequest
) {
  return http.post<QuickEntryRecordDTO>(
    `/quantification/daily-checks/${checkId}/quick-entry/submit`,
    data
  )
}

/**
 * 撤销录入
 */
export function revokeQuickEntry(checkId: number | string, recordId: number | string) {
  return http.delete<void>(
    `/quantification/daily-checks/${checkId}/quick-entry/records/${recordId}`
  )
}

/**
 * 获取我的录入记录
 */
export function getMyQuickEntryRecords(
  checkId: number | string,
  onlyMine: boolean = true
) {
  return http.get<QuickEntryRecordDTO[]>(
    `/quantification/daily-checks/${checkId}/quick-entry/records`,
    { params: { onlyMine } }
  )
}
