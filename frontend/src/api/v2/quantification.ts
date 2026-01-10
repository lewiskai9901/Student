import { http } from '@/utils/request'
import type {
  QuantificationType,
  QuantificationRecord,
  QuantificationTypeQueryParams,
  QuantificationRecordQueryParams,
  QuantificationTypeFormData,
  QuantificationRecordFormData
} from '@/types/quantification'

// ========== 量化类型管理 API (1.0版本) ==========

/**
 * 获取量化类型分页列表
 */
export function getQuantificationTypesPage(params: QuantificationTypeQueryParams) {
  return http.get<any>('/quantification/dictionaries/categories', { params }).then((result) => {
    // 映射字段名以兼容前端（后端已返回categoryName/categoryCode格式）
    if (result.records) {
      result.records = result.records.map((type: any) => ({
        ...type,
        categoryName: type.categoryName || type.typeName,
        categoryCode: type.categoryCode || type.typeCode,
        status: type.status !== undefined ? type.status : type.isActive
      }))
    }
    return result
  })
}

/**
 * 获取所有启用的量化类型
 */
export function getAllEnabledTypes() {
  return http.get<QuantificationType[]>('/quantification/dictionaries/categories/enabled').then((types) => {
    // 映射字段名以兼容前端（后端已返回categoryName/categoryCode格式）
    return types.map(type => ({
      ...type,
      // 兼容新旧字段名
      categoryName: type.categoryName || type.typeName,
      categoryCode: type.categoryCode || type.typeCode,
      status: type.status !== undefined ? type.status : type.isActive
    }))
  })
}

/**
 * 获取量化类型详情
 */
export function getQuantificationTypeById(id: string | number) {
  return http.get<QuantificationType>(`/quantification/dictionaries/categories/${id}`).then((type) => {
    // 映射字段名以兼容前端（后端已返回categoryName/categoryCode格式）
    return {
      ...type,
      categoryName: type.categoryName || type.typeName,
      categoryCode: type.categoryCode || type.typeCode,
      status: type.status !== undefined ? type.status : type.isActive
    }
  })
}

/**
 * 创建量化类型
 */
export function createQuantificationType(data: any) {
  // 映射前端字段名到后端字段名
  const backendData = {
    typeName: data.categoryName || data.typeName,
    typeCode: data.categoryCode || data.typeCode,
    checkFrequency: data.checkFrequency || 1,
    timesPerDay: data.timesPerDay,
    deductMode: data.deductMode || 1,
    deductConfig: data.deductConfig,
    isActive: data.status !== undefined ? data.status : (data.isActive !== undefined ? data.isActive : 1),
    allowPhoto: data.allowPhoto !== undefined ? data.allowPhoto : 1,
    allowRemark: data.allowRemark !== undefined ? data.allowRemark : 1
  }
  return http.post<number>('/quantification/dictionaries/categories', backendData)
}

/**
 * 更新量化类型
 */
export function updateQuantificationType(id: string | number, data: any) {
  // 映射前端字段名到后端字段名
  const backendData = {
    id: id,
    typeName: data.categoryName || data.typeName,
    typeCode: data.categoryCode || data.typeCode,
    checkFrequency: data.checkFrequency || 1,
    timesPerDay: data.timesPerDay,
    deductMode: data.deductMode || 1,
    deductConfig: data.deductConfig,
    isActive: data.status !== undefined ? data.status : (data.isActive !== undefined ? data.isActive : 1),
    allowPhoto: data.allowPhoto !== undefined ? data.allowPhoto : 1,
    allowRemark: data.allowRemark !== undefined ? data.allowRemark : 1
  }
  return http.put(`/quantification/dictionaries/categories/${id}`, backendData)
}

/**
 * 删除量化类型
 */
export function deleteQuantificationType(id: string | number) {
  return http.delete(`/quantification/dictionaries/categories/${id}`)
}

/**
 * 批量删除量化类型
 */
export function batchDeleteQuantificationTypes(ids: (string | number)[]) {
  return http.delete('/quantification/dictionaries/categories/batch', { data: ids })
}

/**
 * 更新量化类型状态
 */
export function updateQuantificationTypeStatus(id: string | number, status: number) {
  return http.patch(`/quantification/dictionaries/categories/${id}/status`, null, { params: { status } })
}

/**
 * 检查类型编码是否存在
 */
export function checkTypeCodeExists(typeCode: string, excludeId?: string | number) {
  return http.get<boolean>('/quantification/dictionaries/categories/exists', {
    params: { typeCode, excludeId }
  })
}

// ========== 量化记录管理 API (1.0版本) ==========

/**
 * 获取量化记录分页列表
 */
export function getQuantificationRecordsPage(params: QuantificationRecordQueryParams) {
  return http.get<any>('/quantification-records', { params }).then((result) => {
    // 映射字段名以兼容前端
    if (result.records) {
      result.records = result.records.map((record: any) => ({
        ...record,
        // 保持原有字段，添加兼容字段
        categoryId: record.typeId,
        categoryName: record.typeName,
        categoryCode: record.typeCode
      }))
    }
    return result
  })
}

/**
 * 获取量化记录详情
 */
export function getQuantificationRecordById(id: string | number) {
  return http.get<QuantificationRecord>(`/quantification-records/${id}`)
}

/**
 * 创建量化记录
 */
export function createQuantificationRecord(data: QuantificationRecordFormData) {
  return http.post<number>('/quantification-records', data)
}

/**
 * 更新量化记录
 */
export function updateQuantificationRecord(id: string | number, data: QuantificationRecordFormData) {
  return http.put(`/quantification-records/${id}`, data)
}

/**
 * 删除量化记录
 */
export function deleteQuantificationRecord(id: string | number) {
  return http.delete(`/quantification-records/${id}`)
}

/**
 * 批量删除量化记录
 */
export function batchDeleteQuantificationRecords(ids: (string | number)[]) {
  return http.delete('/quantification-records/batch', { data: ids })
}

/**
 * 提交量化记录(待审核->已确认)
 */
export function submitQuantificationRecord(id: string | number) {
  return http.post(`/quantification-records/${id}/submit`)
}

/**
 * 审核量化记录
 */
export function approveQuantificationRecord(id: string | number, approved: boolean, rejectReason?: string) {
  return http.post(`/quantification-records/${id}/approve`, { approved, rejectReason })
}

// ========== 统计分析 API (1.0版本) ==========

/**
 * 获取班级量化统计
 */
export function getClassQuantificationStats(params: any) {
  return http.get<any>('/quantification-records/stats/by-class', { params })
}

/**
 * 获取类型量化统计
 */
export function getTypeQuantificationStats(params: any) {
  return http.get<any>('/quantification-records/stats/by-type', { params })
}

/**
 * 获取日期量化统计
 */
export function getDateQuantificationStats(params: any) {
  return http.get<any>('/quantification-records/stats/by-date', { params })
}

// ========== 向后兼容的API别名 ==========

// 配置相关
export const getAllCategories = getAllEnabledTypes
export const getCategoryDetail = getQuantificationTypeById
export const createCategory = createQuantificationType
export const updateCategory = updateQuantificationType
export const deleteCategory = deleteQuantificationType

// 记录相关
export const queryDailyRecords = getQuantificationRecordsPage
export const getDailyRecordDetail = getQuantificationRecordById
export const createDailyRecord = createQuantificationRecord
export const deleteDailyRecord = deleteQuantificationRecord

// ========== 量化2.0扣分项API ==========
// 扣分项的完整API请使用 @/api/deductionItems.ts

/**
 * 获取扣分项列表(按类别/类型ID)
 * 调用后端的 /deduction-items/type/{typeId}/enabled 接口
 */
export function getDeductionItemsByCategoryId(categoryId: string | number) {
  return http.get<any[]>(`/deduction-items/type/${categoryId}/enabled`)
}

/**
 * 记录扣分 - 量化2.0版本
 * 调用check-records scoring API
 */
export function recordDeduction(data: any) {
  return http.post('/check-records/scoring', data)
}

/**
 * 完成检查 - 委托给submitQuantificationRecord
 */
export function completeDailyCheck(id: string | number) {
  return submitQuantificationRecord(id)
}

/**
 * 删除扣分明细 - 委托给deleteQuantificationRecord
 */
export function deleteDeduction(id: string | number) {
  return deleteQuantificationRecord(id)
}

// 统计分析兼容API
export const getClassRanking = getClassQuantificationStats
export const getDeductionFrequency = getTypeQuantificationStats
export const getCategoryRatio = getTypeQuantificationStats
export const getDeductionTrend = getDateQuantificationStats

// ==================== 检查模板管理 (兼容) ====================

export function createCheckTemplate(data: any): Promise<any> {
  return http.post('/check-templates', data)
}

export function updateCheckTemplate(id: string | number, data: any): Promise<any> {
  return http.put(`/check-templates/${id}`, data)
}

export function deleteCheckTemplate(id: string | number): Promise<void> {
  return http.delete(`/check-templates/${id}`)
}

export function getCheckTemplates(params?: any): Promise<any> {
  return http.get('/check-templates', { params })
}

export function getCheckTemplate(id: string | number): Promise<any> {
  return http.get(`/check-templates/${id}`)
}

// ==================== 扣分项管理 (兼容) ====================

export function createDeductionItem(data: any): Promise<any> {
  return http.post('/deduction-items', data)
}

export function updateDeductionItem(id: string | number, data: any): Promise<any> {
  return http.put(`/deduction-items/${id}`, data)
}

export function deleteDeductionItem(id: string | number): Promise<void> {
  return http.delete(`/deduction-items/${id}`)
}

export function getDeductionItems(params?: any): Promise<any> {
  return http.get('/deduction-items', { params })
}

export function getDeductionItem(id: string | number): Promise<any> {
  return http.get(`/deduction-items/${id}`)
}

// 兼容别名
export const getCheckTemplatePage = getCheckTemplates
export const getDeductionItemsByTypeId = getDeductionItemsByCategoryId
export const getCheckPlanPage = getCheckPlans

// 计划状态常量
export const PLAN_STATUS = {
  DRAFT: 0,
  PUBLISHED: 1,
  ARCHIVED: 2
}

export const PLAN_STATUS_LABELS: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '已归档'
}

// 检查计划统计
export function getCheckPlanStatistics(planId?: string | number): Promise<any> {
  return http.get('/check-plans/statistics', { params: { planId } })
}

// 检查计划API
export function getCheckPlans(params?: any): Promise<any> {
  return http.get('/check-plans', { params })
}

export function getCheckPlan(id: string | number): Promise<any> {
  return http.get(`/check-plans/${id}`)
}

export function createCheckPlan(data: any): Promise<any> {
  return http.post('/check-plans', data)
}

export function updateCheckPlan(id: string | number, data: any): Promise<any> {
  return http.put(`/check-plans/${id}`, data)
}

export function deleteCheckPlan(id: string | number): Promise<void> {
  return http.delete(`/check-plans/${id}`)
}

export function publishCheckPlan(id: string | number): Promise<void> {
  return http.post(`/check-plans/${id}/publish`)
}

export function archiveCheckPlan(id: string | number): Promise<void> {
  return http.post(`/check-plans/${id}/archive`)
}

export function startPlan(id: string | number): Promise<void> {
  return http.post(`/check-plans/${id}/start`)
}

export function finishPlan(id: string | number): Promise<void> {
  return http.post(`/check-plans/${id}/finish`)
}

export function archivePlan(id: string | number): Promise<void> {
  return http.post(`/check-plans/${id}/archive`)
}

// 检查计划类型
export interface CheckPlanListVO {
  id: number
  planName: string
  planCode: string
  templateId: number
  templateName: string
  startDate: string
  endDate: string
  status: number
  statusLabel: string
  createdBy: number
  creatorName: string
  createdAt: string
  description?: string
}

export interface CheckPlanStatisticsVO {
  total: number
  draft: number
  published: number
  archived: number
  inProgress: number
  completed: number
}

export interface TargetScopeConfig {
  scopeType: string
  departmentIds?: number[]
  gradeIds?: number[]
  classIds?: number[]
}

export interface CheckPlanCreateRequest {
  planName: string
  planCode: string
  templateId: number
  startDate: string
  endDate: string
  description?: string
  targetScopeConfig?: TargetScopeConfig
  weightConfigIds?: number[]
}

/**
 * 获取所有启用的检查模板
 */
export function getAllCheckTemplates(): Promise<any[]> {
  return http.get<any[]>('/check-templates/enabled')
}

/**
 * 获取检查计划目标范围
 */
export function getPlanTargetScope(planId: string | number): Promise<TargetScopeConfig> {
  return http.get<TargetScopeConfig>(`/check-plans/${planId}/target-scope`)
}

/**
 * 获取检查计划目标班级
 */
export function getPlanTargetClasses(planId: string | number): Promise<any[]> {
  return http.get<any[]>(`/check-plans/${planId}/target-classes`)
}

/**
 * 获取检查计划详情
 */
export function getCheckPlanDetail(id: string | number): Promise<any> {
  return http.get(`/check-plans/${id}/detail`)
}

// 模板快照类型
export interface SnapshotDeductionItem {
  id: string | number
  itemName: string
  itemCode: string
  deductMode: number
  fixedScore?: number
  baseScore?: number
  perPersonScore?: number
}

export interface SnapshotCategory {
  categoryId: string | number
  categoryName: string
  categoryCode: string
  isRequired?: number
  deductionItems?: SnapshotDeductionItem[]
}

/**
 * 解析模板快照
 */
export function parseTemplateSnapshot(snapshotJson: string): SnapshotCategory[] {
  try {
    const data = JSON.parse(snapshotJson)
    return data.categories || []
  } catch {
    return []
  }
}

// 检查计划详情类型
export interface CheckPlanVO extends CheckPlanListVO {
  templateSnapshot?: string
  targetScopeConfig?: TargetScopeConfig
  weightConfigIds?: number[]
}

export interface TemplateSnapshot {
  templateId: number
  templateName: string
  categories: SnapshotCategory[]
}

// 检查记录类型
export interface CheckRecord {
  id: number
  planId: number
  checkDate: string
  roundIndex: number
  classId: number
  className: string
  categoryId: number
  categoryName: string
  itemId: number
  itemName: string
  score: number
  personCount?: number
  photoUrls?: string[]
  remark?: string
  status: number
  createdBy: number
  creatorName: string
  createdAt: string
}

/**
 * 获取检查记录列表
 */
export function getCheckRecordsList(params?: any): Promise<any> {
  return http.get('/check-records', { params })
}

/**
 * 删除检查记录
 */
export function deleteCheckRecord(id: string | number): Promise<void> {
  return http.delete(`/check-records/${id}`)
}

/**
 * 发布检查记录
 */
export function publishCheckRecord(id: string | number): Promise<void> {
  return http.post(`/check-records/${id}/publish`)
}

/**
 * 导出检查记录
 */
export function exportCheckRecord(params?: any): Promise<Blob> {
  return http.get('/check-records/export', { params, responseType: 'blob' })
}

/**
 * 获取日常检查详情
 */
export function getDailyCheckById(id: string | number): Promise<any> {
  return http.get(`/daily-checks/${id}`)
}

/**
 * 获取日常检查列表
 */
export function getDailyCheckList(params?: any): Promise<any> {
  return http.get('/daily-checks', { params })
}

/**
 * 创建日常检查
 */
export function createDailyCheck(data: any): Promise<any> {
  return http.post('/daily-checks', data)
}

/**
 * 更新日常检查
 */
export function updateDailyCheck(id: string | number, data: any): Promise<any> {
  return http.put(`/daily-checks/${id}`, data)
}

/**
 * 删除日常检查
 */
export function deleteDailyCheck(id: string | number): Promise<void> {
  return http.delete(`/daily-checks/${id}`)
}

/**
 * 获取日常检查分页列表
 */
export function getDailyCheckPage(params?: any): Promise<any> {
  return http.get('/daily-checks', { params })
}

/**
 * 更新检查状态
 */
export function updateCheckStatus(id: string | number, status: number): Promise<void> {
  return http.patch(`/daily-checks/${id}/status`, null, { params: { status } })
}

// 日常检查类型
export interface CheckTargetItem {
  classId: number
  className: string
  departmentId?: number
  departmentName?: string
  gradeId?: number
  gradeName?: string
}

export interface CheckCategoryItem {
  categoryId: number
  categoryName: string
  roundIndex?: number
  linkType?: number
}

export interface DailyCheckCreateRequest {
  planId: number
  checkDate: string
  targets: CheckTargetItem[]
  categories: CheckCategoryItem[]
  roundNames?: string[]
  totalRounds?: number
}

export interface DailyCheckResponse {
  id: number
  planId: number
  planName: string
  checkDate: string
  status: number
  statusLabel: string
  targets: CheckTargetItem[]
  categories: CheckCategoryItem[]
  roundNames?: string[]
  totalRounds: number
  createdBy: number
  creatorName: string
  createdAt: string
}

// 检查模板类型
export interface CheckTemplateResponse {
  id: number
  templateName: string
  templateCode: string
  status: number
  statusLabel?: string
  categories: SnapshotCategory[]
  createdBy?: number
  creatorName?: string
  createdAt?: string
}

/**
 * 获取检查模板详情
 */
export function getCheckTemplateById(id: string | number): Promise<CheckTemplateResponse> {
  return http.get<CheckTemplateResponse>(`/check-templates/${id}`)
}

/**
 * 将日常检查转换为检查记录
 */
export function convertDailyCheck(dailyCheckId: string | number): Promise<any> {
  return http.post(`/daily-checks/${dailyCheckId}/convert`)
}

// ==================== 权重配置 API ====================

/**
 * 获取权重配置详情
 */
export function getWeightConfigDetail(id: string | number): Promise<any> {
  return http.get(`/weight-configs/${id}`)
}

/**
 * 获取权重配置列表
 */
export function getWeightConfigList(params?: any): Promise<any> {
  return http.get('/weight-configs', { params })
}

/**
 * 获取所有启用的权重配置
 */
export function getAllEnabledWeightConfigs(): Promise<any[]> {
  return http.get<any[]>('/weight-configs/enabled')
}

/**
 * 创建权重配置
 */
export function createWeightConfig(data: any): Promise<any> {
  return http.post('/weight-configs', data)
}

/**
 * 更新权重配置
 */
export function updateWeightConfig(id: string | number, data: any): Promise<any> {
  return http.put(`/weight-configs/${id}`, data)
}

/**
 * 删除权重配置
 */
export function deleteWeightConfig(id: string | number): Promise<void> {
  return http.delete(`/weight-configs/${id}`)
}

/**
 * 获取权重配置树形结构
 */
export function getWeightConfigTree(planId?: string | number): Promise<any> {
  return http.get('/weight-configs/tree', { params: { planId } })
}

// ==================== 检查记录详情 API ====================

/**
 * 获取检查记录详情
 */
export function getCheckRecordDetail(id: string | number): Promise<any> {
  return http.get(`/check-records/${id}/detail`)
}

// 统计类型
export interface DeductionDetail {
  id: number
  itemId: number
  itemName: string
  deductMode: number
  score: number
  personCount?: number
  remark?: string
  photoUrls?: string[]
}

export interface CheckRecordCategoryStatsDTO {
  categoryId: number
  categoryName: string
  totalScore: number
  deductionCount: number
  deductions: DeductionDetail[]
}

export interface ClassStats {
  classId: number
  className: string
  departmentName?: string
  gradeName?: string
  totalScore: number
  deductionScore: number
  categoryStats: CheckRecordCategoryStatsDTO[]
}

export interface CheckRecordDetail {
  id: number
  recordCode: string
  planId: number
  planName: string
  checkDate: string
  status: number
  statusLabel: string
  totalClasses: number
  totalScore: number
  avgScore: number
  classStats: ClassStats[]
  createdBy: number
  creatorName: string
  createdAt: string
}

/**
 * 获取我的班级检查详情
 */
export function getMyClassDetail(recordId: string | number): Promise<ClassStats> {
  return http.get<ClassStats>(`/check-records/${recordId}/my-class`)
}

// ==================== 扣分项 API ====================

export interface DeductionItem {
  id: number
  typeId: number
  itemName: string
  itemCode: string
  deductMode: number
  fixedScore?: number
  baseScore?: number
  perPersonScore?: number
  rangeConfig?: any
  status: number
  sortOrder: number
  description?: string
}

/**
 * 根据类型ID获取启用的扣分项
 */
export function getEnabledDeductionItemsByTypeId(typeId: string | number): Promise<DeductionItem[]> {
  return http.get<DeductionItem[]>(`/deduction-items/type/${typeId}/enabled`)
}

// ==================== 加权计算 API ====================

export interface ClassWeightConfig {
  classId: number
  className: string
  studentCount: number
  weight: number
}

export interface ClassWeightResult {
  classId: number
  className: string
  rawScore: number
  weight: number
  weightedScore: number
}

/**
 * 获取加权配置
 */
export function getWeightConfig(planId: string | number, classIds: number[]): Promise<ClassWeightConfig[]> {
  return http.post<ClassWeightConfig[]>(`/weight-configs/calculate`, { planId, classIds })
}

/**
 * 计算加权分数
 */
export function calculateWeightedScore(scores: { classId: number; rawScore: number }[], weights: ClassWeightConfig[]): ClassWeightResult[] {
  return scores.map(s => {
    const config = weights.find(w => w.classId === s.classId)
    const weight = config?.weight || 1
    return {
      classId: s.classId,
      className: config?.className || '',
      rawScore: s.rawScore,
      weight,
      weightedScore: s.rawScore * weight
    }
  })
}
