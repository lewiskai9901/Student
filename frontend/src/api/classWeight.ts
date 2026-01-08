/**
 * 班级加权配置API
 * @author system
 * @version 3.0.0
 */

import http from '@/utils/request'

export interface ClassWeightConfig {
  id?: number
  semesterId: number
  configName: string
  configDescription?: string
  enableWeight: number // 0=关闭,1=开启
  weightMode: string // STANDARD/PER_CAPITA/SEGMENT/NONE
  minWeight?: number
  maxWeight?: number
  segmentRules?: string // JSON格式的分段规则
  effectiveDate: string
  expiryDate?: string
  applyToAllClasses: number
  specificClassIds?: string
  createdAt?: string
  updatedAt?: string
}

export interface ClassSizeStandard {
  id?: number
  semesterId: number
  gradeLevel: number
  standardName: string
  standardSize: number
  locked: number // 0=否,1=是
  lockedDate?: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface ClassWeightResult {
  classId: number
  originalScore: number
  weightedScore: number
  weightFactor: number
  actualSize: number
  standardSize: number
  weightMode: string
  enableWeight: boolean
  calculationDetails: string
}

export interface WeightConfigQuery {
  pageNum: number
  pageSize: number
  semesterId?: number
  enableWeight?: number
  weightMode?: string
}

export interface StandardSizeQuery {
  pageNum: number
  pageSize: number
  semesterId?: number
  gradeLevel?: number
  locked?: number
}

// 查询加权配置列表
export const listWeightConfigs = (params: WeightConfigQuery) => {
  return http.get<any>('/quantification/weight-config/configs', { params })
}

// 查询加权配置详情
export const getWeightConfig = (id: number) => {
  return http.get<ClassWeightConfig>(`/quantification/weight-config/configs/${id}`)
}

// 创建加权配置
export const createWeightConfig = (data: ClassWeightConfig) => {
  return http.post<ClassWeightConfig>('/quantification/weight-config/configs', data)
}

// 更新加权配置
export const updateWeightConfig = (id: number, data: Partial<ClassWeightConfig>) => {
  return http.put<ClassWeightConfig>(`/quantification/weight-config/configs/${id}`, data)
}

// 删除加权配置
export const deleteWeightConfig = (id: number) => {
  return http.delete(`/quantification/weight-config/configs/${id}`)
}

// 设置默认加权配置
export const setDefaultConfig = (id: number) => {
  return http.put(`/quantification/weight-config/configs/${id}/default`)
}

// 计算加权分数(预览)
export const calculateWeightedScore = (classId: number, originalScore: number, checkDate: string) => {
  return http.post<ClassWeightResult>('/quantification/weight-config/calculate', {
    classId,
    originalScore,
    checkDate
  })
}

// 批量重新计算加权
export const recalculateWeights = (recordId: number) => {
  return http.post(`/quantification/weight-config/recalculate/${recordId}`)
}
