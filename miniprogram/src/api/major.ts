/**
 * 专业管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 专业信息
export interface Major {
  id: number | string
  majorCode: string
  majorName: string
  departmentId: number
  departmentName?: string
  description?: string
  duration?: number       // 学制(年)
  degreeType?: number     // 学位类型
  degreeTypeName?: string
  status: number
  statusText?: string
  classCount?: number
  studentCount?: number
  createdAt?: string
  updatedAt?: string
}

// 查询参数
export interface MajorQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  majorName?: string
  majorCode?: string
  departmentId?: number
  status?: number
}

// 创建/更新专业请求
export interface MajorFormData {
  majorCode: string
  majorName: string
  departmentId: number
  description?: string
  duration?: number
  degreeType?: number
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
 * 获取专业列表
 */
export function getMajorList(params: MajorQueryParams = {}) {
  return get<PageResult<Major>>('/majors', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 100,
    ...params
  })
}

/**
 * 获取专业详情
 */
export function getMajorDetail(id: number | string) {
  return get<Major>(`/majors/${id}`)
}

/**
 * 创建专业
 */
export function createMajor(data: MajorFormData) {
  return post<Major>('/majors', data)
}

/**
 * 更新专业
 */
export function updateMajor(id: number | string, data: MajorFormData) {
  return put<Major>(`/majors/${id}`, data)
}

/**
 * 删除专业
 */
export function deleteMajor(id: number | string) {
  return del<void>(`/majors/${id}`)
}

/**
 * 获取所有专业(用于下拉选择)
 */
export function getAllMajors() {
  return get<Major[]>('/majors/all')
}

/**
 * 根据部门获取专业列表
 */
export function getMajorsByDepartment(departmentId: number) {
  return get<Major[]>('/majors/by-department', { departmentId })
}

/**
 * 获取启用的专业列表
 */
export function getEnabledMajors(departmentId?: number) {
  return get<Major[]>('/majors/enabled', { departmentId })
}
