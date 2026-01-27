/**
 * 整改工单 API
 */
import { http } from '@/utils/request'
import type { CorrectiveAction, CreateActionRequest, AutoActionRule } from '@/types/corrective'

const BASE_URL = '/corrective-actions'

export function listActions(params?: { status?: string; classId?: number; assigneeId?: number }): Promise<CorrectiveAction[]> {
  return http.get<CorrectiveAction[]>(BASE_URL, { params })
}

export function getAction(id: number): Promise<CorrectiveAction> {
  return http.get<CorrectiveAction>(`${BASE_URL}/${id}`)
}

export function createAction(data: CreateActionRequest): Promise<CorrectiveAction> {
  return http.post<CorrectiveAction>(BASE_URL, data)
}

export function startAction(id: number): Promise<CorrectiveAction> {
  return http.put<CorrectiveAction>(`${BASE_URL}/${id}/start`)
}

export function resolveAction(id: number, data: { resolutionNote: string; attachments?: string[] }): Promise<CorrectiveAction> {
  return http.put<CorrectiveAction>(`${BASE_URL}/${id}/resolve`, data)
}

export function verifyAction(id: number, data: { result: string; comment?: string }): Promise<CorrectiveAction> {
  return http.put<CorrectiveAction>(`${BASE_URL}/${id}/verify`, data)
}

export function escalateAction(id: number): Promise<CorrectiveAction> {
  return http.put<CorrectiveAction>(`${BASE_URL}/${id}/escalate`)
}

export function getActionStatistics(): Promise<Record<string, number>> {
  return http.get<Record<string, number>>(`${BASE_URL}/statistics`)
}

// Rules
export function listRules(): Promise<AutoActionRule[]> {
  return http.get<AutoActionRule[]>(`${BASE_URL}/rules`)
}

export function getRule(id: number): Promise<AutoActionRule> {
  return http.get<AutoActionRule>(`${BASE_URL}/rules/${id}`)
}

export function createRule(data: Partial<AutoActionRule>): Promise<AutoActionRule> {
  return http.post<AutoActionRule>(`${BASE_URL}/rules`, data)
}

export function updateRule(id: number, data: Partial<AutoActionRule>): Promise<AutoActionRule> {
  return http.put<AutoActionRule>(`${BASE_URL}/rules/${id}`, data)
}

export function deleteRule(id: number): Promise<void> {
  return http.delete(`${BASE_URL}/rules/${id}`)
}

export const correctiveActionApi = {
  list: listActions,
  get: getAction,
  create: createAction,
  start: startAction,
  resolve: resolveAction,
  verify: verifyAction,
  escalate: escalateAction,
  statistics: getActionStatistics,
  rules: {
    list: listRules,
    get: getRule,
    create: createRule,
    update: updateRule,
    delete: deleteRule
  }
}
