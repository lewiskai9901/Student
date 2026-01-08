/**
 * 检查计划API
 */
import { get, post, put, del } from '@/utils/request'

// 检查计划状态枚举
export enum PlanStatus {
  DRAFT = 0,      // 草稿
  IN_PROGRESS = 1, // 进行中
  FINISHED = 2,   // 已结束
  ARCHIVED = 3    // 已归档
}

// 检查计划接口类型
export interface CheckPlan {
  id: string | number
  planCode: string
  planName: string
  description?: string
  templateId: number
  templateName: string
  startDate: string
  endDate: string
  status: PlanStatus
  enableWeight: number
  weightConfigId?: number
  totalChecks: number
  totalRecords: number
  totalDeductionScore: number
  creatorId?: number
  creatorName?: string
  createdAt: string
  updatedAt?: string
}

// 计划统计
export interface PlanStatistics {
  totalPlans: number
  draftCount: number
  inProgressCount: number
  finishedCount: number
  archivedCount: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 查询参数
export interface PlanQueryParams {
  planName?: string
  status?: number
  startDateFrom?: string
  startDateTo?: string
  pageNum?: number
  pageSize?: number
}

/**
 * 获取检查计划列表
 */
export function getCheckPlanList(params: PlanQueryParams = {}) {
  return get<PageResult<CheckPlan>>('/check-plans', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    ...params
  })
}

/**
 * 获取检查计划详情
 */
export function getCheckPlanDetail(id: string | number) {
  return get<CheckPlan>(`/check-plans/${id}`)
}

/**
 * 获取检查计划统计
 */
export function getCheckPlanStatistics() {
  return get<PlanStatistics>('/check-plans/statistics')
}

/**
 * 获取模板快照
 */
export function getTemplateSnapshot(planId: string | number) {
  return get<any>(`/check-plans/${planId}/template-snapshot`)
}

/**
 * 获取进行中的计划列表
 */
export function getActivePlans() {
  return get<PageResult<CheckPlan>>('/check-plans', {
    status: PlanStatus.IN_PROGRESS,
    pageNum: 1,
    pageSize: 100
  })
}

// 创建计划请求参数
export interface CreatePlanRequest {
  planCode?: string
  planName: string
  description?: string
  templateId: number
  startDate: string
  endDate: string
  enableWeight?: number
  weightConfigId?: number
  enableRating?: number
  ratingConfigId?: number
}

/**
 * 创建检查计划
 */
export function createCheckPlan(data: CreatePlanRequest) {
  return post<CheckPlan>('/check-plans', data)
}

/**
 * 更新检查计划
 */
export function updateCheckPlan(id: string | number, data: Partial<CreatePlanRequest>) {
  return put<CheckPlan>(`/check-plans/${id}`, data)
}

/**
 * 删除检查计划
 */
export function deleteCheckPlan(id: string | number) {
  return del<void>(`/check-plans/${id}`)
}

/**
 * 更新计划状态
 */
export function updateCheckPlanStatus(id: string | number, status: PlanStatus) {
  return put<void>(`/check-plans/${id}/status`, { status })
}
