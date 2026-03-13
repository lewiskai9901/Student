/**
 * V7 检查平台 - 项目 API
 */
import { http } from '@/utils/request'
import type {
  InspProject,
  CreateProjectRequest,
  UpdateProjectRequest,
  PublishProjectRequest,
  ProjectInspector,
  AddInspectorRequest,
  ProjectScore,
} from '@/types/insp/project'
import type { ProjectStatus } from '@/types/insp/enums'

const BASE = '/v7/insp/projects'

// ==================== 项目 CRUD ====================

export function getProjects(params?: {
  status?: ProjectStatus
}): Promise<InspProject[]> {
  return http.get<InspProject[]>(BASE, { params })
}

export function getProject(id: number): Promise<InspProject> {
  return http.get<InspProject>(`${BASE}/${id}`)
}

export function createProject(data: CreateProjectRequest): Promise<InspProject> {
  return http.post<InspProject>(BASE, data)
}

export function updateProject(id: number, data: UpdateProjectRequest): Promise<InspProject> {
  return http.put<InspProject>(`${BASE}/${id}`, data)
}

export function deleteProject(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 项目生命周期 ====================

export function publishProject(id: number, data: PublishProjectRequest): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/publish`, data)
}

export function pauseProject(id: number): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/pause`)
}

export function resumeProject(id: number): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/resume`)
}

export function completeProject(id: number): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/complete`)
}

export function archiveProject(id: number): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/archive`)
}

// ==================== 子项目 + 分数 ====================

export function getChildProjects(projectId: number): Promise<InspProject[]> {
  return http.get<InspProject[]>(`${BASE}/${projectId}/children`)
}

export function getProjectScores(projectId: number): Promise<ProjectScore[]> {
  return http.get<ProjectScore[]>(`${BASE}/${projectId}/scores`)
}

export function getScoreTree(projectId: number): Promise<any> {
  return http.get(`${BASE}/${projectId}/score-tree`)
}

export function previewTargetCount(data: {
  scopeType: string
  scopeConfig: string
  targetType: string
}): Promise<number> {
  return http.post<number>(`${BASE}/target-preview`, data)
}

// ==================== 检查员池 ====================

export function getInspectors(projectId: number): Promise<ProjectInspector[]> {
  return http.get<ProjectInspector[]>(`${BASE}/${projectId}/inspectors`)
}

export function addInspector(projectId: number, data: AddInspectorRequest): Promise<ProjectInspector> {
  return http.post<ProjectInspector>(`${BASE}/${projectId}/inspectors`, data)
}

export function removeInspector(projectId: number, inspectorId: number): Promise<void> {
  return http.delete(`${BASE}/${projectId}/inspectors/${inspectorId}`)
}

// ==================== API 对象 ====================

export const inspProjectApi = {
  getList: getProjects,
  getById: getProject,
  create: createProject,
  update: updateProject,
  delete: deleteProject,
  publish: publishProject,
  pause: pauseProject,
  resume: resumeProject,
  complete: completeProject,
  archive: archiveProject,
  getChildProjects,
  getProjectScores,
  getScoreTree,
  previewTargetCount,
  getInspectors,
  addInspector,
  removeInspector,
}
