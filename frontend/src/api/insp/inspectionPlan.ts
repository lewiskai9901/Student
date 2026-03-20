import { http } from '@/utils/request'
import type { InspectionPlan, CreateInspectionPlanRequest, UpdateInspectionPlanRequest } from '@/types/insp/project'

const BASE = '/v7/insp/inspection-plans'

export function getInspectionPlansByProject(projectId: number): Promise<InspectionPlan[]> {
  return http.get<InspectionPlan[]>(BASE, { params: { projectId } })
}

export function createInspectionPlan(data: CreateInspectionPlanRequest): Promise<InspectionPlan> {
  return http.post<InspectionPlan>(BASE, data)
}

export function updateInspectionPlan(id: number, data: UpdateInspectionPlanRequest): Promise<InspectionPlan> {
  return http.put<InspectionPlan>(`${BASE}/${id}`, data)
}

export function deleteInspectionPlan(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

export function generateTasks(planId: number, startDate: string, endDate: string): Promise<number> {
  return http.post<number>(`${BASE}/${planId}/generate-tasks`, { startDate, endDate })
}
