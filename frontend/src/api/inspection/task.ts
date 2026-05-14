/**
 * 检查平台 - 任务 API
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type {
  InspTask,
  CreateTaskRequest,
  ClaimTaskRequest,
  ReviewTaskRequest,
  AssignTaskRequest,
} from '@/types/insp/project'

const BASE = '/inspection/tasks'

// ==================== 任务 CRUD ====================

export function getTasks(params?: {
  projectId?: LongId
}): Promise<InspTask[]> {
  return http.get<InspTask[]>(BASE, { params })
}

export function getTask(id: LongId): Promise<InspTask> {
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

export function claimTask(id: LongId, data: ClaimTaskRequest): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/claim`, data)
}

export function startTask(id: LongId): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/start`)
}

export function submitTask(id: LongId): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/submit`)
}

export function withdrawTask(id: LongId): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/withdraw`)
}

export function reviewTask(id: LongId, data: ReviewTaskRequest): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/review`, data)
}

export function publishTask(id: LongId): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/publish`)
}

export function rejectTask(id: LongId, comment?: string): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/reject`, { comment: comment || '驳回' })
}

export function cancelTask(id: LongId): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/cancel`)
}

export function assignTask(id: LongId, data: AssignTaskRequest): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/assign`, data)
}

/** P1#5: 项目管理员手动延期任务 */
export function extendTaskDeadline(id: LongId, newDeadline: string): Promise<InspTask> {
  return http.post<InspTask>(`${BASE}/${id}/extend-deadline`, { newDeadline })
}

/** review #D: 检查员离职批量重派 — 返回受影响任务数 */
export function reassignDepartedInspector(
  userId: LongId,
  data: { reason: string; fallbackInspectorId?: LongId; fallbackInspectorName?: string },
): Promise<number> {
  return http.post<number>(`${BASE}/reassign-departed-inspector/${userId}`, data)
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
  extendDeadline: extendTaskDeadline,
  reassignDepartedInspector,
}
