/**
 * 申诉管理API (V2)
 * @author system
 */

import http from '@/utils/request'

// V2 API 基础路径
const BASE_URL = '/v2/appeals'

export interface CheckItemAppeal {
  id?: number
  appealCode: string
  inspectionRecordId: number
  deductionDetailId: number
  classId: number
  reason: string
  attachments?: string
  originalDeduction: number
  requestedDeduction?: number
  approvedDeduction?: number
  status: string // V2使用字符串状态
  applicantId: number
  appliedAt?: string
  level1ReviewerId?: number
  level1ReviewedAt?: string
  level1Comment?: string
  level2ReviewerId?: number
  level2ReviewedAt?: string
  level2Comment?: string
  effectiveAt?: string
  allowedTransitions?: string[]
  createdAt?: string
  updatedAt?: string
  // 扩展字段
  className?: string
  gradeName?: string
  applicantName?: string
  currentApproverName?: string
  checkName?: string
  itemName?: string
}

export interface AppealListVO {
  id: number
  appealCode: string
  className: string
  gradeName: string
  applicantName: string
  reason: string
  status: string
  originalDeduction: number
  requestedDeduction?: number
  approvedDeduction?: number
  appliedAt: string
}

export interface AppealStatistics {
  countByStatus: Record<string, number>
}

export interface AppealCreateRequest {
  inspectionRecordId: number
  deductionDetailId: number
  classId: number
  reason: string
  attachments?: string
  originalDeduction: number
  requestedDeduction?: number
}

export interface AppealReviewRequest {
  comment: string
  approvedDeduction?: number
}

export interface AppealQuery {
  pageNum: number
  pageSize: number
  gradeId?: number
  classId?: number
  appellantId?: number
  status?: string
  startTime?: string
  endTime?: string
  keyword?: string
}

// 创建申诉
export const createAppeal = (data: AppealCreateRequest) => {
  return http.post<CheckItemAppeal>(BASE_URL, data)
}

// 查询申诉详情
export const getAppeal = (id: number) => {
  return http.get<CheckItemAppeal>(`${BASE_URL}/${id}`)
}

// 查询我的申诉
export const getMyAppeals = () => {
  return http.get<CheckItemAppeal[]>(`${BASE_URL}/my`)
}

// 查询待审核申诉
export const getPendingAppeals = (level: number = 1) => {
  return http.get<CheckItemAppeal[]>(`${BASE_URL}/pending`, {
    params: { level }
  })
}

// 开始一级审核
export const startLevel1Review = (id: number) => {
  return http.put<CheckItemAppeal>(`${BASE_URL}/${id}/start-level1-review`)
}

// 一级审核通过
export const level1Approve = (id: number, data: AppealReviewRequest) => {
  return http.put<CheckItemAppeal>(`${BASE_URL}/${id}/level1-approve`, data)
}

// 一级审核驳回
export const level1Reject = (id: number, data: AppealReviewRequest) => {
  return http.put<CheckItemAppeal>(`${BASE_URL}/${id}/level1-reject`, data)
}

// 开始二级审核
export const startLevel2Review = (id: number) => {
  return http.put<CheckItemAppeal>(`${BASE_URL}/${id}/start-level2-review`)
}

// 最终审核通过
export const approveAppeal = (id: number, data: AppealReviewRequest) => {
  return http.put<CheckItemAppeal>(`${BASE_URL}/${id}/approve`, data)
}

// 最终审核驳回
export const rejectAppeal = (id: number, data: AppealReviewRequest) => {
  return http.put<CheckItemAppeal>(`${BASE_URL}/${id}/reject`, data)
}

// 撤销申诉
export const withdrawAppeal = (id: number) => {
  return http.put<CheckItemAppeal>(`${BASE_URL}/${id}/withdraw`)
}

// 申诉生效
export const makeAppealEffective = (id: number) => {
  return http.put<CheckItemAppeal>(`${BASE_URL}/${id}/make-effective`)
}

// 获取申诉统计
export const getAppealStatistics = () => {
  return http.get<AppealStatistics>(`${BASE_URL}/statistics`)
}

// ==================== 兼容V1的API ====================

// 查询申诉列表 (兼容V1)
export const listAppeals = (params: AppealQuery) => {
  // V2没有分页列表端点,使用my端点代替
  return getMyAppeals().then((data: any) => {
    const appeals = Array.isArray(data) ? data : []
    return {
      records: appeals,
      total: appeals.length
    }
  })
}

// 审核申诉 (兼容V1)
export const reviewAppeal = (data: { appealId: number; approvalStatus: number; approvalOpinion: string; adjustedScore?: number }) => {
  const reviewData: AppealReviewRequest = {
    comment: data.approvalOpinion,
    approvedDeduction: data.adjustedScore
  }

  if (data.approvalStatus === 2) {
    // 通过
    return approveAppeal(data.appealId, reviewData)
  } else {
    // 驳回
    return rejectAppeal(data.appealId, reviewData)
  }
}

// 查询公示中的申诉 (兼容V1)
export const getPublicityAppeals = () => {
  return Promise.resolve([])
}

// 检查是否可以申诉 (兼容V1)
export const checkCanAppeal = (itemId: number) => {
  return Promise.resolve({ canAppeal: true, message: '' })
}

// 查询班级申诉历史 (兼容V1)
export const getClassAppealHistory = (classId: number, limit = 10) => {
  return Promise.resolve([])
}

// 申诉摘要统计接口
export interface AppealSummary {
  myAppeals: number
  pendingReview: number
  inPublicity: number
  totalAppeals: number
}

// 获取申诉摘要统计
export const getAppealSummary = async (): Promise<AppealSummary> => {
  try {
    const [myAppeals, stats] = await Promise.all([
      getMyAppeals(),
      getAppealStatistics().catch(() => null)
    ])

    const myCount = Array.isArray(myAppeals) ? myAppeals.length : 0
    const statusCounts = stats?.countByStatus || {}

    return {
      myAppeals: myCount,
      pendingReview: (statusCounts['PENDING_LEVEL1_REVIEW'] || 0) + (statusCounts['PENDING_LEVEL2_REVIEW'] || 0),
      inPublicity: statusCounts['APPROVED'] || 0,
      totalAppeals: Object.values(statusCounts).reduce((a: number, b: any) => a + (Number(b) || 0), 0)
    }
  } catch (error) {
    console.error('获取申诉摘要统计失败:', error)
    return {
      myAppeals: 0,
      pendingReview: 0,
      inPublicity: 0,
      totalAppeals: 0
    }
  }
}
