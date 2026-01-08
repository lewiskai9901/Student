import { http } from '@/utils/request'

/**
 * 检查计划相关接口
 */

// 分类/扣分项加权配置
export interface ItemWeightConfig {
  itemId?: string | number            // 扣分项ID(可选)
  categoryId: string | number         // 分类ID
  weightConfigId: string | number     // 加权方案ID
}

// 目标范围配置
export interface TargetScopeConfig {
  departmentIds?: (string | number)[] // 院系ID列表
  gradeIds?: (string | number)[]      // 年级ID列表
  classIds?: (string | number)[]      // 班级ID列表(精确指定)
  excludeClassIds?: (string | number)[] // 排除的班级ID列表
}

// 创建检查计划请求
export interface CheckPlanCreateRequest {
  planName: string                    // 计划名称
  description?: string                // 计划描述
  templateId: string | number         // 模板ID
  startDate: string                   // 开始日期 YYYY-MM-DD
  endDate: string                     // 结束日期 YYYY-MM-DD
  weightConfigId?: string | number    // 加权方案ID
  enableWeight?: number               // 是否启用加权 0否 1是
  customStandardSize?: number         // 自定义标准人数
  itemWeightConfigs?: ItemWeightConfig[]  // 分类级别加权配置
  targetScopeType?: string            // 目标范围类型: all/department/grade/custom
  targetScopeConfig?: TargetScopeConfig // 目标范围配置
}

// 更新检查计划请求
export interface CheckPlanUpdateRequest {
  id: string | number                 // 计划ID
  planName?: string                   // 计划名称
  description?: string                // 计划描述
  startDate?: string                  // 开始日期
  endDate?: string                    // 结束日期
  weightConfigId?: string | number    // 加权方案ID
  enableWeight?: number               // 是否启用加权
  customStandardSize?: number         // 自定义标准人数
  itemWeightConfigs?: ItemWeightConfig[]  // 分类级别加权配置
  targetScopeType?: string            // 目标范围类型: all/department/grade/custom
  targetScopeConfig?: TargetScopeConfig // 目标范围配置
}

// 检查计划查询请求
export interface CheckPlanQueryRequest {
  pageNum?: number
  pageSize?: number
  planName?: string                   // 计划名称(模糊查询)
  templateId?: string | number        // 模板ID
  status?: number                     // 状态 0草稿 1进行中 2已结束 3已归档
  startDateFrom?: string              // 开始日期起始
  startDateTo?: string                // 开始日期结束
}

// 检查计划列表项
export interface CheckPlanListVO {
  id: string | number
  planCode: string                    // 计划编号
  planName: string                    // 计划名称
  description?: string                // 计划描述
  templateName: string                // 模板名称
  startDate: string                   // 开始日期
  endDate: string                     // 结束日期
  status: number                      // 状态 0草稿 1进行中 2已结束 3已归档
  totalChecks: number                 // 检查次数
  totalRecords: number                // 检查记录数
  totalDeductionScore: number         // 总扣分
  enableWeight: number                // 是否启用加权
  creatorName?: string                // 创建人姓名
  createdAt: string                   // 创建时间
}

// 检查计划详情
export interface CheckPlanVO {
  id: string | number
  planCode: string                    // 计划编号
  planName: string                    // 计划名称
  description?: string                // 计划描述
  templateId: string | number         // 模板ID
  templateName: string                // 模板名称
  templateSnapshot: string            // 模板快照JSON
  startDate: string                   // 开始日期
  endDate: string                     // 结束日期
  weightConfigId?: string | number    // 加权方案ID
  enableWeight: number                // 是否启用加权
  customStandardSize?: number         // 自定义标准人数
  itemWeightConfigs?: ItemWeightConfig[]  // 分类级别加权配置
  status: number                      // 状态
  totalChecks: number                 // 检查次数
  totalRecords: number                // 检查记录数
  totalDeductionScore: number         // 总扣分
  ratingConfigId?: string | number    // 评优方案ID(预留)
  statisticsConfig?: string           // 统计配置(预留)
  targetScopeType?: string            // 目标范围类型: all/department/grade/custom
  targetScopeConfig?: string          // 目标范围配置JSON
  createdBy?: string | number         // 创建人ID
  creatorName?: string                // 创建人姓名
  createdAt: string                   // 创建时间
  updatedAt: string                   // 更新时间
}

// 检查计划统计
export interface CheckPlanStatisticsVO {
  totalPlans: number                  // 总计划数
  draftCount: number                  // 草稿数
  inProgressCount: number             // 进行中数
  finishedCount: number               // 已结束数
  archivedCount: number               // 已归档数
  totalChecks: number                 // 总检查次数
  totalRecords: number                // 总检查记录数
  totalDeductionScore: number         // 总扣分
}

// 模板快照中的扣分项
export interface SnapshotDeductionItem {
  itemId: string | number
  itemName: string
  deductMode: number                // 扣分模式: 1固定扣分 2按人数扣分 3区间扣分
  fixedScore?: number | null        // 固定扣分分数(模式1)
  baseScore?: number | null         // 基础扣分分数(模式2)
  perPersonScore?: number | null    // 每人扣分分数(模式2)
  rangeConfig?: string | null       // 区间配置JSON(模式3)
  description?: string | null
  sortOrder?: number
  allowPhoto?: number               // 是否允许上传照片
  allowRemark?: number              // 是否允许添加备注
  allowStudents?: number            // 是否允许添加学生
}

// 模板快照中的检查类别
export interface SnapshotCategory {
  id?: string | number               // 关联记录ID
  categoryId: string | number
  categoryName: string
  categoryCode?: string
  linkType?: number                  // 关联类型 0不关联 1关联宿舍 2关联教室
  sortOrder?: number
  isRequired?: number
  checkRounds?: number               // 旧字段，保留兼容
  participatedRounds?: string        // 参与的轮次，如 "1,3"
  deductionItems?: SnapshotDeductionItem[]
}

// 模板快照结构
export interface TemplateSnapshot {
  templateId: string | number
  templateName: string
  templateCode: string
  snapshotTime: string
  totalRounds?: number               // 总轮次数
  roundNames?: string[]              // 轮次名称数组
  categories: SnapshotCategory[]
}

// 分页响应
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 创建检查计划
 */
export function createCheckPlan(data: CheckPlanCreateRequest) {
  return http.post<CheckPlanVO>('/check-plans', data)
}

/**
 * 更新检查计划
 */
export function updateCheckPlan(data: CheckPlanUpdateRequest) {
  return http.put<CheckPlanVO>('/check-plans', data)
}

/**
 * 删除检查计划(仅草稿状态可删除)
 */
export function deleteCheckPlan(id: string | number) {
  return http.delete<void>(`/check-plans/${id}`)
}

/**
 * 获取计划详情
 */
export function getCheckPlanDetail(id: string | number) {
  return http.get<CheckPlanVO>(`/check-plans/${id}`)
}

/**
 * 分页查询计划列表
 */
export function getCheckPlanPage(params: CheckPlanQueryRequest) {
  return http.get<PageResponse<CheckPlanListVO>>('/check-plans', { params })
}

/**
 * 开始计划(草稿→进行中)
 */
export function startPlan(id: string | number) {
  return http.post<void>(`/check-plans/${id}/start`)
}

/**
 * 结束计划(进行中→已结束)
 */
export function finishPlan(id: string | number) {
  return http.post<void>(`/check-plans/${id}/finish`)
}

/**
 * 归档计划(已结束→已归档)
 */
export function archivePlan(id: string | number) {
  return http.post<void>(`/check-plans/${id}/archive`)
}

/**
 * 获取计划统计数据
 */
export function getCheckPlanStatistics() {
  return http.get<CheckPlanStatisticsVO>('/check-plans/statistics')
}

/**
 * 获取计划的模板快照
 */
export function getTemplateSnapshot(id: string | number) {
  return http.get<string>(`/check-plans/${id}/template-snapshot`)
}

/**
 * 解析模板快照JSON
 */
export function parseTemplateSnapshot(snapshotJson: string): TemplateSnapshot | null {
  try {
    return JSON.parse(snapshotJson) as TemplateSnapshot
  } catch {
    return null
  }
}

/**
 * 获取计划的目标班级列表
 */
export function getPlanTargetClasses(id: string | number) {
  return http.get<any[]>(`/check-plans/${id}/target-classes`)
}

/**
 * 获取计划的目标范围配置
 */
export function getPlanTargetScope(id: string | number) {
  return http.get<{ scopeType: string; scopeConfig: string }>(`/check-plans/${id}/target-scope`)
}

// 计划状态常量
export const PLAN_STATUS = {
  DRAFT: 0,           // 草稿
  IN_PROGRESS: 1,     // 进行中
  FINISHED: 2,        // 已结束
  ARCHIVED: 3         // 已归档
} as const

// 计划状态标签
export const PLAN_STATUS_LABELS: Record<number, string> = {
  [PLAN_STATUS.DRAFT]: '草稿',
  [PLAN_STATUS.IN_PROGRESS]: '进行中',
  [PLAN_STATUS.FINISHED]: '已结束',
  [PLAN_STATUS.ARCHIVED]: '已归档'
}

// 计划状态对应的颜色类型(用于Element Plus)
export const PLAN_STATUS_TYPES: Record<number, 'info' | 'warning' | 'success' | 'danger'> = {
  [PLAN_STATUS.DRAFT]: 'info',
  [PLAN_STATUS.IN_PROGRESS]: 'warning',
  [PLAN_STATUS.FINISHED]: 'success',
  [PLAN_STATUS.ARCHIVED]: 'info'
}
