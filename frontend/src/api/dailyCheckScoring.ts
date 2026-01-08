import { http } from '@/utils/request'

/**
 * 新架构 - 日常检查打分和申诉API
 * 对接后端重构后的API
 * @since 1.0.6
 */

// ==================== 打分相关类型定义 ====================

/**
 * 扣分明细请求
 * 注意：ID字段使用 string | number 类型以避免JavaScript大整数精度丢失问题
 */
export interface ScoringDetailRequest {
  categoryId: string | number
  classId: string | number
  deductionItemId: string | number
  deductionItemName?: string
  deductMode?: number
  linkType: number  // 1=宿舍, 2=教室, 0=通用
  linkId: string | number
  deductScore: number
  personCount?: number
  description?: string
  remark?: string
  photoUrls?: string | null
  dormitoryId?: string | number
  dormitoryNo?: string
  classroomId?: string | number
  classroomNo?: string
  students?: any[]
}

/**
 * 日常检查打分请求
 */
export interface DailyScoringRequest {
  checkId: string | number
  checkerId: string | number
  checkerName: string
  details: ScoringDetailRequest[]
}

/**
 * 扣分明细响应
 */
export interface ScoringDetailResponse {
  id: number
  checkId: number
  categoryId: number
  categoryName?: string
  classId: number
  className?: string
  deductionItemId: number
  deductionItemName?: string
  linkType: number
  linkTypeName?: string
  linkId: number
  linkName?: string
  deductScore: number
  remark?: string
  photoUrls?: string
  appealStatus: number
  createdAt: string
}

/**
 * 日常检查打分响应
 */
export interface DailyScoringResponse {
  checkId: number
  checkName?: string
  checkDate?: string
  checkerId?: number
  checkerName?: string
  totalDeductScore: number
  detailCount: number
  appealStatus: number
  appealCount: number
  details?: ScoringDetailResponse[]
}

// ==================== 申诉相关类型定义 ====================

/**
 * 申诉创建请求
 */
export interface AppealCreateRequest {
  detailId: number
  appealReason: string
  appealUserId: number
  appealUserName: string
  appealPhotoUrls?: string
}

/**
 * 申诉审核请求
 */
export interface AppealReviewRequest {
  appealId: number
  status: number  // 2=通过, 3=驳回
  revisedScore?: number
  reviewOpinion?: string
  reviewerId: number
  reviewerName: string
}

/**
 * 申诉响应
 */
export interface AppealResponse {
  id: number
  checkId: number
  detailId: number
  categoryName?: string
  className?: string
  deductionItemName?: string
  originalScore: number
  appealReason: string
  appealUserId: number
  appealUserName: string
  appealTime: string
  appealPhotoUrls?: string
  status: number  // 0=待处理, 1=处理中, 2=通过, 3=驳回
  statusName?: string
  revisedScore?: number
  reviewOpinion?: string
  reviewerId?: number
  reviewerName?: string
  reviewTime?: string
}

// ==================== 打分API ====================

/**
 * 保存打分数据(增量更新)
 */
export function saveScoring(checkId: number, data: DailyScoringRequest) {
  return http.post<void>(`/quantification/daily-checks/${checkId}/scoring`, data)
}

/**
 * 获取检查的打分数据
 */
export function getScoringByCheckId(checkId: number) {
  return http.get<DailyScoringResponse>(`/quantification/daily-checks/${checkId}/scoring/init`)
}

/**
 * 获取检查某个班级的扣分明细
 */
export function getDetailsByCheckIdAndClassId(checkId: number, classId: number) {
  return http.get<ScoringDetailResponse[]>(`/quantification/daily-checks/${checkId}/classes/${classId}/details`)
}

/**
 * 获取检查某个类别某个班级的扣分明细
 */
export function getDetailsByCheckIdAndCategoryIdAndClassId(
  checkId: number,
  categoryId: number,
  classId: number
) {
  return http.get<ScoringDetailResponse[]>(
    `/quantification/daily-checks/${checkId}/categories/${categoryId}/classes/${classId}/details`
  )
}

/**
 * 删除扣分明细
 */
export function deleteDetail(detailId: number) {
  return http.delete<void>(`/quantification/daily-checks/details/${detailId}`)
}

/**
 * 批量删除扣分明细
 */
export function batchDeleteDetails(detailIds: number[]) {
  return http.delete<void>('/quantification/daily-checks/details', { data: detailIds })
}

// ==================== 申诉API ====================

/**
 * 创建申诉
 */
export function createAppeal(data: AppealCreateRequest) {
  return http.post<number>('/quantification/daily-check-appeals', data)
}

/**
 * 审核申诉
 */
export function reviewAppeal(appealId: number, data: AppealReviewRequest) {
  return http.put<void>(`/quantification/daily-check-appeals/${appealId}/review`, data)
}

/**
 * 根据检查ID查询所有申诉
 */
export function getAppealsByCheckId(checkId: number) {
  return http.get<AppealResponse[]>(`/quantification/daily-check-appeals/check/${checkId}`)
}

/**
 * 根据检查ID和状态查询申诉
 */
export function getAppealsByCheckIdAndStatus(checkId: number, status: number) {
  return http.get<AppealResponse[]>(`/quantification/daily-check-appeals/check/${checkId}/status/${status}`)
}

/**
 * 根据班级ID查询申诉
 */
export function getAppealsByClassId(classId: number) {
  return http.get<AppealResponse[]>(`/quantification/daily-check-appeals/class/${classId}`)
}

/**
 * 根据明细ID查询申诉
 */
export function getAppealByDetailId(detailId: number) {
  return http.get<AppealResponse>(`/quantification/daily-check-appeals/detail/${detailId}`)
}

/**
 * 撤销申诉
 */
export function withdrawAppeal(appealId: number) {
  return http.delete<void>(`/quantification/daily-check-appeals/${appealId}`)
}
