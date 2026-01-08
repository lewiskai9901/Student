import http from '@/utils/request'

/**
 * 检查目标项
 */
export interface CheckTargetItem {
  targetType: number  // 目标类型: 1班级 2年级 3院系
  targetId: number    // 目标ID
  targetName: string  // 目标名称
}

/**
 * 检查类别项
 */
export interface CheckCategoryItem {
  categoryId: number   // 类别ID
  categoryName: string // 类别名称
  linkType?: number    // 关联类型: 0无关联 1宿舍 2教室
  isRequired?: number  // 是否必填: 0否 1是
  sortOrder?: number   // 排序号
  checkRounds?: number // 检查轮次，默认1
}

/**
 * 创建日常检查请求
 */
export interface DailyCheckCreateRequest {
  planId?: number | string       // 所属检查计划ID(可选)
  checkDate: string              // 检查日期 YYYY-MM-DD
  checkName: string              // 检查名称
  templateId?: number            // 模板ID(可选)
  checkType?: number             // 检查类型: 1日常检查 2专项检查
  description?: string           // 检查说明
  targets: CheckTargetItem[]     // 检查目标列表
  categories?: CheckCategoryItem[] // 检查类别列表(如果使用模板可不传)
}

/**
 * 检查目标响应
 */
export interface CheckTargetResponse {
  id: number
  targetType: number
  targetId: number
  targetName: string
}

/**
 * 检查类别响应
 */
export interface CheckCategoryResponse {
  id: number
  categoryId: number
  categoryName: string
  categoryCode?: string
  linkType: number
  isRequired: number
  sortOrder: number
  checkRounds?: number // 检查轮次
}

/**
 * 日常检查响应
 */
export interface DailyCheckResponse {
  id: number
  planId?: number           // 所属检查计划ID
  planName?: string         // 所属检查计划名称
  checkDate: string
  checkName: string
  templateId?: number
  templateName?: string
  checkType: number
  status: number  // 0未开始 1进行中 2已完成 3已发布
  description?: string
  createdBy?: number
  createdAt: string
  updatedAt: string
  targets?: CheckTargetResponse[]
  categories?: CheckCategoryResponse[]
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 创建日常检查
 */
export function createDailyCheck(data: DailyCheckCreateRequest) {
  return http.post<number>('/quantification/daily-checks', data)
}

/**
 * 更新日常检查
 */
export function updateDailyCheck(id: number, data: DailyCheckCreateRequest) {
  return http.put<void>(`/quantification/daily-checks/${id}`, data)
}

/**
 * 删除日常检查
 */
export function deleteDailyCheck(id: number) {
  return http.delete<void>(`/quantification/daily-checks/${id}`)
}

/**
 * 获取检查详情
 */
export function getDailyCheckById(id: number) {
  return http.get<DailyCheckResponse>(`/quantification/daily-checks/${id}`)
}

/**
 * 分页查询检查
 */
export function getDailyCheckPage(params: {
  pageNum?: number
  pageSize?: number
  checkDate?: string
  status?: number
  checkName?: string
}) {
  return http.get<PageResponse<DailyCheckResponse>>('/quantification/daily-checks', { params })
}

/**
 * 更新检查状态
 */
export function updateCheckStatus(id: number, status: number) {
  return http.patch<void>(`/quantification/daily-checks/${id}/status`, null, {
    params: { status }
  })
}
