/**
 * V7 检查平台 - 任务 API
 */
import { http } from '@/utils/request'
import type {
  InspTask,
  CreateTaskRequest,
  ClaimTaskRequest,
  ReviewTaskRequest,
  AssignTaskRequest,
} from '@/types/insp/project'

const BASE = '/v7/insp/tasks'

// ==================== 任务 CRUD ====================

export function getTasks(params?: {
  projectId?: number
}): Promise<InspTask[]> {
  return http.get<InspTask[]>(BASE, { params })
}

export function getTask(id: number): Promise<InspTask> {
  return http.get<InspTask>(`${BASE}/${id}`)
}

export function createTask(data: CreateTaskRequest): Promise<InspTask> {
  return http.post<InspTask>(BASE, data)
}

export function getAvailableTasks(): Promise<InspTask[]> {
  return http.get<InspTask[]>(`${BASE}/available`)
}

export function getMyTasks(): Promise<InspTask[]> {
  return http.get<InspTask[]>(`${BASE}/my-tasks`)
}

// ==================== 任务生命周期 ====================

export function claimTask(id: number, data: ClaimTaskRequest): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/claim`, data)
}

export function startTask(id: number): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/start`)
}

export function submitTask(id: number): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/submit`)
}

export function withdrawTask(id: number): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/withdraw`)
}

export function reviewTask(id: number, data: ReviewTaskRequest): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/review`, data)
}

export function publishTask(id: number): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/publish`)
}

export function rejectTask(id: number, comment?: string): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/reject`, { comment: comment || '驳回' })
}

export function cancelTask(id: number): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/cancel`)
}

export function assignTask(id: number, data: AssignTaskRequest): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/assign`, data)
}

// ==================== API 对象 ====================

export const inspTaskApi = {
  getList: getTasks,
  getById: getTask,
  create: createTask,
  getAvailable: getAvailableTasks,
  getMyTasks,
  claim: claimTask,
  start: startTask,
  submit: submitTask,
  withdraw: withdrawTask,
  review: reviewTask,
  reject: rejectTask,
  publish: publishTask,
  cancel: cancelTask,
  assign: assignTask,
}
