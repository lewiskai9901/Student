/**
 * 排班管理 API
 */
import { http } from '@/utils/request'
import type { SchedulePolicy, ScheduleExecution, CreatePolicyRequest } from '@/types/schedule'

const POLICY_URL = '/schedule/policies'
const EXECUTION_URL = '/schedule/executions'

// ==================== 排班策略 API ====================

export function listPolicies(): Promise<SchedulePolicy[]> {
  return http.get<SchedulePolicy[]>(POLICY_URL)
}

export function getPolicy(id: number): Promise<SchedulePolicy> {
  return http.get<SchedulePolicy>(`${POLICY_URL}/${id}`)
}

export function createPolicy(data: CreatePolicyRequest): Promise<SchedulePolicy> {
  return http.post<SchedulePolicy>(POLICY_URL, data)
}

export function updatePolicy(id: number, data: CreatePolicyRequest): Promise<SchedulePolicy> {
  return http.put<SchedulePolicy>(`${POLICY_URL}/${id}`, data)
}

export function deletePolicy(id: number): Promise<void> {
  return http.delete(`${POLICY_URL}/${id}`)
}

export function enablePolicy(id: number): Promise<void> {
  return http.put(`${POLICY_URL}/${id}/enable`)
}

export function disablePolicy(id: number): Promise<void> {
  return http.put(`${POLICY_URL}/${id}/disable`)
}

// ==================== 排班执行 API ====================

export function listExecutions(params: {
  startDate: string
  endDate: string
}): Promise<ScheduleExecution[]> {
  return http.get<ScheduleExecution[]>(EXECUTION_URL, { params })
}

export function triggerExecution(policyId: number, date: string): Promise<ScheduleExecution> {
  return http.post<ScheduleExecution>(`${EXECUTION_URL}/trigger`, null, {
    params: { policyId, date }
  })
}

// ==================== API 对象封装 ====================

export const scheduleApi = {
  listPolicies,
  getPolicy,
  createPolicy,
  updatePolicy,
  deletePolicy,
  enablePolicy,
  disablePolicy,
  listExecutions,
  triggerExecution
}
