/**
 * 部门管理 API
 */
import { get, post, put, del } from '@/utils/request'

// 部门信息
export interface Department {
  id: number | string
  deptCode: string
  deptName: string
  parentId?: number
  parentName?: string
  deptType?: number      // 1:学校 2:院系 3:专业 4:其他
  deptTypeName?: string
  leaderId?: number
  leaderName?: string
  phone?: string
  email?: string
  sort?: number
  status: number
  statusText?: string
  children?: Department[]
  createdAt?: string
  updatedAt?: string
}

// 查询参数
export interface DepartmentQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  deptName?: string
  deptCode?: string
  parentId?: number
  deptType?: number
  status?: number
}

// 创建/更新部门请求
export interface DepartmentFormData {
  deptCode: string
  deptName: string
  parentId?: number
  deptType?: number
  leaderId?: number
  phone?: string
  email?: string
  sort?: number
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
 * 获取部门列表
 */
export function getDepartmentList(params: DepartmentQueryParams = {}) {
  return get<PageResult<Department>>('/departments', {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 100,
    ...params
  })
}

/**
 * 获取部门详情
 */
export function getDepartmentDetail(id: number | string) {
  return get<Department>(`/departments/${id}`)
}

/**
 * 创建部门
 */
export function createDepartment(data: DepartmentFormData) {
  return post<Department>('/departments', data)
}

/**
 * 更新部门
 */
export function updateDepartment(id: number | string, data: DepartmentFormData) {
  return put<Department>(`/departments/${id}`, data)
}

/**
 * 删除部门
 */
export function deleteDepartment(id: number | string) {
  return del<void>(`/departments/${id}`)
}

/**
 * 获取启用的部门列表(用于下拉选择)
 */
export function getEnabledDepartments() {
  return get<Department[]>('/departments/enabled')
}

/**
 * 获取部门树形结构
 */
export function getDepartmentTree() {
  return get<Department[]>('/departments/tree')
}

/**
 * 获取子部门列表
 */
export function getChildDepartments(parentId: number) {
  return get<Department[]>('/departments/children', { parentId })
}
