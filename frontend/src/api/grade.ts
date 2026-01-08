/**
 * 年级管理API - V2 DDD架构适配
 * @author system
 * @version 3.0.0
 */

import { http } from '@/utils/request'

// V2 API 基础路径
const BASE_URL = '/v2/grades'

export interface Grade {
  id?: number
  gradeName: string
  gradeCode: string
  enrollmentYear: number
  schoolingYears?: number
  standardClassSize?: number
  sortOrder?: number
  remarks?: string
  status?: string  // V2: PREPARING, ACTIVE, GRADUATED, STOPPED
  directorId?: number
  directorName?: string
  counselorId?: number
  counselorName?: string
  classCount?: number
  studentCount?: number
  createdAt?: string
  updatedAt?: string
  // V1 兼容字段
  gradeDirectorId?: number
  gradeDirectorName?: string
}

export interface GradeStatistics {
  gradeId: number
  gradeName: string
  classCount: number
  studentCount: number
  appealCount: number
  pendingAppealCount: number
  effectiveAppealCount: number
}

export interface GradeQuery {
  pageNum?: number
  pageSize?: number
  enrollmentYear?: number
  status?: string
  keyword?: string
}

export interface GradeCreateRequest {
  gradeName: string
  gradeCode: string
  enrollmentYear: number
  schoolingYears?: number
  standardClassSize?: number
}

export interface GradeUpdateRequest {
  id?: number
  gradeName?: string
  standardClassSize?: number
  sortOrder?: number
  remarks?: string
}

export interface AssignLeadersRequest {
  directorId?: number
  directorName?: string
  counselorId?: number
  counselorName?: string
}

// 获取所有年级
export const listGrades = (params?: GradeQuery) => {
  return http.get<Grade[]>(BASE_URL, { params })
}

// 获取年级分页列表 (V2返回列表,前端分页)
export const getGradePage = (params: GradeQuery & { pageNum: number; pageSize: number }) => {
  return http.get<Grade[]>(BASE_URL).then(grades => {
    const start = (params.pageNum - 1) * params.pageSize
    const end = start + params.pageSize
    return {
      records: grades.slice(start, end),
      total: grades.length,
      current: params.pageNum,
      size: params.pageSize
    }
  })
}

// 获取年级详情
export const getGrade = (id: number) => {
  return http.get<Grade>(`${BASE_URL}/${id}`)
}

// 获取活动状态的年级
export const getActiveGrades = () => {
  return http.get<Grade[]>(`${BASE_URL}/active`)
}

// 按入学年份获取年级
export const getGradeByYear = (enrollmentYear: number) => {
  return http.get<Grade>(`${BASE_URL}/by-year/${enrollmentYear}`)
}

// 按状态获取年级
export const getGradesByStatus = (status: string) => {
  return http.get<Grade[]>(`${BASE_URL}/by-status/${status}`)
}

// 创建年级
export const createGrade = (data: GradeCreateRequest) => {
  return http.post<Grade>(BASE_URL, data)
}

// 更新年级
export const updateGrade = (data: GradeUpdateRequest & { id: number }) => {
  return http.put<Grade>(`${BASE_URL}/${data.id}`, {
    gradeName: data.gradeName,
    standardClassSize: data.standardClassSize,
    sortOrder: data.sortOrder,
    remarks: data.remarks
  })
}

// 分配年级负责人
export const assignGradeLeaders = (id: number, data: AssignLeadersRequest) => {
  return http.put<Grade>(`${BASE_URL}/${id}/leaders`, data)
}

// 激活年级
export const activateGrade = (id: number) => {
  return http.put<Grade>(`${BASE_URL}/${id}/activate`)
}

// 年级毕业
export const graduateGrade = (id: number) => {
  return http.put<Grade>(`${BASE_URL}/${id}/graduate`)
}

// 停止招生
export const stopEnrollment = (id: number) => {
  return http.put<Grade>(`${BASE_URL}/${id}/stop-enrollment`)
}

// 删除年级
export const deleteGrade = (id: number) => {
  return http.delete(`${BASE_URL}/${id}`)
}

// 获取所有年级(不分页,用于下拉选择)
export const getAllGrades = () => {
  return http.get<Grade[]>(BASE_URL)
}

// ==================== V1 兼容方法 (已弃用) ====================

// 同步年级统计数据 (V2暂无此端点)
export const syncGradeStatistics = (id: number) => {
  console.warn('syncGradeStatistics is deprecated in V2 API')
  return Promise.resolve()
}

// 获取年级统计数据 (V2暂无此端点)
export const getGradeStatistics = (id: number) => {
  console.warn('getGradeStatistics is deprecated in V2 API')
  return Promise.resolve({} as GradeStatistics)
}
