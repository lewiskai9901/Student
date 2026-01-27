/**
 * 量化检查 API - DDD架构适配
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 */
import { http } from '@/utils/request'
import type {
  InspectionTemplate,
  CreateTemplateRequest,
  AddCategoryRequest,
  InspectionRecord,
  CreateRecordRequest,
  RecordDeductionRequest,
  Appeal,
  CreateAppealRequest,
  ReviewAppealRequest,
  AppealStatistics,
  TemplateStatus,
  RecordStatus,
  AppealStatus
} from '@/types'
import type { PageResponse, PageParams } from '@/types'

// 后端API路径 - 对应后端DDD接口层Controller
const TEMPLATE_URL = '/inspection-templates'
const RECORD_URL = '/inspection-records'
const APPEAL_URL = '/appeals'

// ==================== 检查模板 API ====================

/**
 * 获取模板列表
 */
export function getTemplates(params?: {
  status?: TemplateStatus
  keyword?: string
} & PageParams): Promise<InspectionTemplate[]> {
  return http.get<InspectionTemplate[]>(TEMPLATE_URL, { params })
}

/**
 * 获取模板分页列表（前端分页）
 */
export function getTemplatesPage(params: {
  status?: TemplateStatus
  keyword?: string
} & PageParams & { pageNum: number; pageSize: number }) {
  return getTemplates(params).then(templates => {
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return {
      records: templates.slice(start, end),
      total: templates.length,
      current: params.pageNum,
      size: params.pageSize
    }
  })
}

/**
 * 获取模板详情
 */
export function getTemplate(id: number): Promise<InspectionTemplate> {
  return http.get<InspectionTemplate>(`${TEMPLATE_URL}/${id}`)
}

/**
 * 创建模板
 */
export function createTemplate(data: CreateTemplateRequest): Promise<InspectionTemplate> {
  return http.post<InspectionTemplate>(TEMPLATE_URL, data)
}

/**
 * 更新模板
 */
export function updateTemplate(id: number, data: Partial<CreateTemplateRequest>): Promise<InspectionTemplate> {
  return http.put<InspectionTemplate>(`${TEMPLATE_URL}/${id}`, data)
}

/**
 * 删除模板
 */
export function deleteTemplate(id: number): Promise<void> {
  return http.delete(`${TEMPLATE_URL}/${id}`)
}

/**
 * 发布模板
 */
export function publishTemplate(id: number): Promise<InspectionTemplate> {
  return http.put<InspectionTemplate>(`${TEMPLATE_URL}/${id}/publish`)
}

/**
 * 设置为默认模板
 */
export function setTemplateAsDefault(id: number): Promise<InspectionTemplate> {
  return http.put<InspectionTemplate>(`${TEMPLATE_URL}/${id}/set-default`)
}

/**
 * 归档模板
 */
export function archiveTemplate(id: number): Promise<void> {
  return http.post(`${TEMPLATE_URL}/${id}/archive`)
}

/**
 * 添加检查类别
 */
export function addCategory(templateId: number, data: AddCategoryRequest): Promise<InspectionTemplate> {
  return http.post<InspectionTemplate>(`${TEMPLATE_URL}/${templateId}/categories`, data)
}

/**
 * 更新检查类别
 */
export function updateCategory(templateId: number, categoryId: number, data: Partial<AddCategoryRequest>): Promise<void> {
  return http.put(`${TEMPLATE_URL}/${templateId}/categories/${categoryId}`, data)
}

/**
 * 删除检查类别
 */
export function deleteCategory(templateId: number, categoryId: number): Promise<void> {
  return http.delete(`${TEMPLATE_URL}/${templateId}/categories/${categoryId}`)
}

// ==================== 检查记录 API ====================

/**
 * 获取检查记录列表
 */
export function getRecords(params?: {
  templateId?: number
  status?: RecordStatus
  startDate?: string
  endDate?: string
} & PageParams): Promise<InspectionRecord[]> {
  return http.get<InspectionRecord[]>(RECORD_URL, { params })
}

/**
 * 获取检查记录分页列表（前端分页）
 */
export function getRecordsPage(params: {
  templateId?: number
  status?: RecordStatus
  startDate?: string
  endDate?: string
} & PageParams & { pageNum: number; pageSize: number }) {
  return getRecords(params).then(records => {
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return {
      records: records.slice(start, end),
      total: records.length,
      current: params.pageNum,
      size: params.pageSize
    }
  })
}

/**
 * 获取检查记录详情
 */
export function getRecord(id: number): Promise<InspectionRecord> {
  return http.get<InspectionRecord>(`${RECORD_URL}/${id}`)
}

/**
 * 创建检查记录
 */
export function createRecord(data: CreateRecordRequest): Promise<InspectionRecord> {
  return http.post<InspectionRecord>(RECORD_URL, data)
}

/**
 * 添加班级分数
 */
export function addClassScore(recordId: number, data: { classId: number; className: string; baseScore: number }): Promise<InspectionRecord> {
  return http.post<InspectionRecord>(`${RECORD_URL}/${recordId}/class-scores`, data)
}

/**
 * 记录扣分
 */
export function recordDeduction(recordId: number, data: RecordDeductionRequest): Promise<InspectionRecord> {
  return http.post<InspectionRecord>(`${RECORD_URL}/${recordId}/deductions`, data)
}

/**
 * 移除扣分
 */
export function removeDeduction(recordId: number, classId: number, itemId: number): Promise<void> {
  return http.delete(`${RECORD_URL}/${recordId}/deductions`, { params: { classId, itemId } })
}

/**
 * 提交检查记录
 */
export function submitRecord(id: number): Promise<InspectionRecord> {
  return http.put<InspectionRecord>(`${RECORD_URL}/${id}/submit`)
}

/**
 * 审核检查记录
 */
export function approveRecord(id: number): Promise<InspectionRecord> {
  return http.put<InspectionRecord>(`${RECORD_URL}/${id}/approve`)
}

/**
 * 驳回检查记录
 */
export function rejectRecord(id: number, reason: string): Promise<InspectionRecord> {
  return http.put<InspectionRecord>(`${RECORD_URL}/${id}/reject`, { reason })
}

/**
 * 发布检查记录
 */
export function publishRecord(id: number): Promise<InspectionRecord> {
  return http.put<InspectionRecord>(`${RECORD_URL}/${id}/publish`)
}

/**
 * 删除检查记录
 */
export function deleteRecord(id: number): Promise<void> {
  return http.delete(`${RECORD_URL}/${id}`)
}

// ==================== 申诉 API ====================

/**
 * 获取申诉列表
 */
export function getAppeals(params?: {
  status?: AppealStatus
  classId?: number
  recordId?: number
} & PageParams): Promise<Appeal[]> {
  return http.get<Appeal[]>(APPEAL_URL, { params })
}

/**
 * 获取申诉分页列表（前端分页）
 */
export function getAppealsPage(params: {
  status?: AppealStatus
  classId?: number
  recordId?: number
} & PageParams & { pageNum: number; pageSize: number }) {
  return getAppeals(params).then(appeals => {
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return {
      records: appeals.slice(start, end),
      total: appeals.length,
      current: params.pageNum,
      size: params.pageSize
    }
  })
}

/**
 * 获取我的申诉
 */
export function getMyAppeals(params?: PageParams): Promise<Appeal[]> {
  return http.get<Appeal[]>(`${APPEAL_URL}/my`, { params })
}

/**
 * 获取待审核申诉
 */
export function getPendingAppeals(level: number = 1): Promise<Appeal[]> {
  return http.get<Appeal[]>(`${APPEAL_URL}/pending`, { params: { level } })
}

/**
 * 获取申诉详情
 */
export function getAppeal(id: number): Promise<Appeal> {
  return http.get<Appeal>(`${APPEAL_URL}/${id}`)
}

/**
 * 创建申诉
 */
export function createAppeal(data: CreateAppealRequest): Promise<Appeal> {
  return http.post<Appeal>(APPEAL_URL, data)
}

/**
 * 撤回申诉
 */
export function withdrawAppeal(id: number): Promise<Appeal> {
  return http.put<Appeal>(`${APPEAL_URL}/${id}/withdraw`)
}

/**
 * 开始一级审核
 */
export function startLevel1Review(id: number): Promise<Appeal> {
  return http.put<Appeal>(`${APPEAL_URL}/${id}/start-level1-review`)
}

/**
 * 一级审核通过
 */
export function level1Approve(id: number, comment?: string): Promise<Appeal> {
  return http.put<Appeal>(`${APPEAL_URL}/${id}/level1-approve`, { comment })
}

/**
 * 一级审核驳回
 */
export function level1Reject(id: number, comment: string): Promise<Appeal> {
  return http.put<Appeal>(`${APPEAL_URL}/${id}/level1-reject`, { comment })
}

/**
 * 开始二级审核
 */
export function startLevel2Review(id: number): Promise<Appeal> {
  return http.put<Appeal>(`${APPEAL_URL}/${id}/start-level2-review`)
}

/**
 * 最终审核通过
 */
export function approveAppeal(id: number, data: { comment?: string; approvedDeduction: number }): Promise<Appeal> {
  return http.put<Appeal>(`${APPEAL_URL}/${id}/approve`, data)
}

/**
 * 最终审核驳回
 */
export function rejectAppeal(id: number, comment: string): Promise<Appeal> {
  return http.put<Appeal>(`${APPEAL_URL}/${id}/reject`, { comment })
}

/**
 * 使申诉生效
 */
export function makeAppealEffective(id: number): Promise<Appeal> {
  return http.put<Appeal>(`${APPEAL_URL}/${id}/make-effective`)
}

/**
 * 获取申诉统计
 */
export function getAppealStatistics(): Promise<AppealStatistics> {
  return http.get<AppealStatistics>(`${APPEAL_URL}/statistics`)
}

// ==================== API 对象封装（供 Store 使用） ====================

/**
 * 模板 API 对象
 */
export const templateApi = {
  getList: getTemplates,
  getPage: getTemplatesPage,
  getById: getTemplate,
  create: createTemplate,
  update: updateTemplate,
  delete: deleteTemplate,
  publish: publishTemplate,
  setAsDefault: setTemplateAsDefault,
  archive: archiveTemplate,
  addCategory,
  updateCategory,
  deleteCategory
}

/**
 * 记录 API 对象
 */
export const recordApi = {
  getList: getRecords,
  getPage: getRecordsPage,
  getById: getRecord,
  create: createRecord,
  addClassScore,
  recordDeduction,
  removeDeduction,
  submit: submitRecord,
  approve: approveRecord,
  reject: (id: number, data: { reason: string }) => rejectRecord(id, data.reason),
  publish: publishRecord,
  delete: deleteRecord
}

/**
 * 申诉 API 对象
 */
export const appealApi = {
  getList: getAppeals,
  getPage: getAppealsPage,
  getMyAppeals,
  getPendingReview: getPendingAppeals,
  getById: getAppeal,
  create: createAppeal,
  withdraw: withdrawAppeal,
  startLevel1Review,
  level1Approve,
  level1Reject,
  startLevel2Review,
  approve: approveAppeal,
  reject: rejectAppeal,
  makeEffective: makeAppealEffective,
  getStatistics: getAppealStatistics
}
