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
