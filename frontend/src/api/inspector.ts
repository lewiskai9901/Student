import { http } from '@/utils/request'

export interface PermissionConfig {
  categoryId: string
  categoryName: string
  classIds?: number[]
}

export interface InspectorCreateRequest {
  userId: number
  permissions: PermissionConfig[]
  remark?: string
}

export interface InspectorUpdateRequest {
  id: number
  status?: number
  permissions: PermissionConfig[]
  remark?: string
}

export interface PermissionDTO {
  id: number
  categoryId: string
  categoryName: string
  classIds?: number[]
  classNames?: string[]
}

export interface InspectorDTO {
  id: number
  planId: number
  userId: number
  userName: string
  username: string
  departmentName: string
  status: number
  remark: string
  permissions: PermissionDTO[]
  createdAt: string
}

export interface CheckTaskAssignment {
  id: number
  dailyCheckId: number
  planId: number
  userId: number
  categoryIds: string
  classIds: string
  status: number // 0待处理 1进行中 2已完成
  notified: number
  notifiedAt: string
  startedAt: string
  completedAt: string
  createdAt: string
  userName: string
  checkName: string
  checkDate: string
  planName: string
}

// 获取计划的打分人员列表
export function getInspectorsByPlanId(planId: string | number) {
  return http.get<InspectorDTO[]>(`/check-plans/${planId}/inspectors`)
}

// 获取打分人员详情
export function getInspectorById(planId: string | number, id: number) {
  return http.get<InspectorDTO>(`/check-plans/${planId}/inspectors/${id}`)
}

// 添加打分人员
export function addInspector(planId: string | number, data: InspectorCreateRequest) {
  return http.post<number>(`/check-plans/${planId}/inspectors`, data)
}

// 更新打分人员
export function updateInspector(planId: string | number, id: number, data: InspectorUpdateRequest) {
  return http.put<void>(`/check-plans/${planId}/inspectors/${id}`, data)
}

// 删除打分人员
export function deleteInspector(planId: string | number, id: number) {
  return http.delete<void>(`/check-plans/${planId}/inspectors/${id}`)
}

// 获取我的检查任务列表
export function getMyTasks(params: {
  status?: number
  planId?: number
  pageNum?: number
  pageSize?: number
}) {
  return http.get<{
    records: CheckTaskAssignment[]
    total: number
    pages: number
    current: number
    size: number
  }>('/my-check-tasks', { params })
}

// 获取任务详情
export function getTaskDetail(taskId: number) {
  return http.get<CheckTaskAssignment>(`/my-check-tasks/${taskId}`)
}

// 开始任务
export function startTask(taskId: number) {
  return http.put<void>(`/my-check-tasks/${taskId}/start`)
}

// 完成任务
export function completeTask(taskId: number) {
  return http.put<void>(`/my-check-tasks/${taskId}/complete`)
}

// 获取待处理任务数量
export function getPendingTaskCount() {
  return http.get<number>('/my-check-tasks/pending-count')
}
