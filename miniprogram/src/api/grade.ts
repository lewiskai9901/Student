/**
 * 年级管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 年级信息
export interface Grade {
  id: number | string
  gradeCode: string
  gradeName: string
  year: number
  status: number
  statusText?: string
  classCount?: number
  studentCount?: number
  createdAt?: string
  updatedAt?: string
}

// 查询参数
export interface GradeQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  gradeName?: string
  year?: number
  status?: number
}

// 创建/更新年级请求
export interface GradeFormData {
  gradeCode: string
  gradeName: string
  year: number
  status?: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 获取年级列表
 */
export function getGradeList(params: GradeQueryParams = {}) {
  return get<PageResult<Grade>>('/grades', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 100,
    ...params
  })
}

/**
 * 获取年级详情
 */
export function getGradeDetail(id: number | string) {
  return get<Grade>(`/grades/${id}`)
}

/**
 * 创建年级
 */
export function createGrade(data: GradeFormData) {
  return post<Grade>('/grades', data)
}

/**
 * 更新年级
 */
export function updateGrade(id: number | string, data: GradeFormData) {
  return put<Grade>(`/grades/${id}`, data)
}

/**
 * 删除年级
 */
export function deleteGrade(id: number | string) {
  return del<void>(`/grades/${id}`)
}

/**
 * 获取所有年级(用于下拉选择)
 */
export function getAllGrades() {
  return get<Grade[]>('/grades/all')
}

/**
 * 获取启用的年级列表
 */
export function getEnabledGrades() {
  return get<Grade[]>('/grades/enabled')
}
