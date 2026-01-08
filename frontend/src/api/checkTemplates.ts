import { http } from '@/utils/request'

/**
 * 检查模板相关接口 (V2)
 */

// V2 API 基础路径
const BASE_URL = '/v2/inspection-templates'

// 模板类别项接口
export interface TemplateCategoryItem {
  categoryCode: string
  categoryName: string
  description?: string
  baseScore?: number
  sortOrder: number
}

// 创建模板请求
export interface CheckTemplateCreateRequest {
  templateName: string
  templateCode: string
  description?: string
  scope?: string // V2: 'DEPARTMENT' | 'CAMPUS' | 'CUSTOM'
  applicableOrgUnitId?: number
  isDefault?: number
  status?: number
  categories?: TemplateCategoryItem[]
}

// 更新模板请求
export interface CheckTemplateUpdateRequest extends CheckTemplateCreateRequest {
  id: string | number
}

// 模板类别响应
export interface TemplateCategoryResponse {
  id: string | number
  categoryCode: string
  categoryName: string
  description?: string
  baseScore: number
  sortOrder: number
}

// 检查模板响应
export interface CheckTemplateResponse {
  id: string | number
  templateName: string
  templateCode: string
  description: string
  scope?: string
  applicableOrgUnitId?: number
  isDefault: boolean | number
  currentVersion?: number
  status: string | number
  totalBaseScore?: number
  categoryCount?: number
  createdBy?: string | number
  createdAt: string
  updatedAt: string
  categories?: TemplateCategoryResponse[]
}

/**
 * 创建检查模板
 */
export function createCheckTemplate(data: CheckTemplateCreateRequest) {
  return http.post<CheckTemplateResponse>(BASE_URL, {
    templateCode: data.templateCode,
    templateName: data.templateName,
    description: data.description,
    scope: data.scope || 'DEPARTMENT',
    applicableOrgUnitId: data.applicableOrgUnitId
  })
}

/**
 * 更新检查模板
 * 注意: V2暂无直接更新接口,使用发布代替
 */
export function updateCheckTemplate(id: string | number, data: CheckTemplateUpdateRequest) {
  // V2没有直接更新接口,保持V1兼容
  return http.put(`/quantification/templates/${id}`, data)
}

/**
 * 删除检查模板
 * 注意: V2暂无删除接口
 */
export function deleteCheckTemplate(id: string | number) {
  return http.delete(`/quantification/templates/${id}`)
}

/**
 * 获取模板详情
 */
export function getCheckTemplateById(id: string | number) {
  return http.get<CheckTemplateResponse>(`${BASE_URL}/${id}`)
}

/**
 * 获取所有模板列表 (已发布)
 */
export function getAllCheckTemplates() {
  return http.get<CheckTemplateResponse[]>(BASE_URL)
}

/**
 * 分页查询模板
 * V2返回列表,前端模拟分页
 */
export function getCheckTemplatePage(params: {
  pageNum: number
  pageSize: number
  templateName?: string
  status?: number
}) {
  return http.get<CheckTemplateResponse[]>(BASE_URL).then((data: any) => {
    const templates = Array.isArray(data) ? data : []
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
 * 为模板添加类别
 */
export function addCategoryToTemplate(templateId: string | number, data: TemplateCategoryItem) {
  return http.post<CheckTemplateResponse>(`${BASE_URL}/${templateId}/categories`, data)
}

/**
 * 发布模板
 */
export function publishTemplate(id: string | number) {
  return http.put<CheckTemplateResponse>(`${BASE_URL}/${id}/publish`)
}

/**
 * 设置为默认模板
 */
export function setTemplateAsDefault(id: string | number) {
  return http.put<CheckTemplateResponse>(`${BASE_URL}/${id}/set-default`)
}
