/**
 * V62 检查平台 - 项目 & 计划 & 评级维度 & 违纪记录 API
 */
import type { LongId } from '@/types/common'
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
import type {
  InspectionPlan,
  CreatePlanRequest,
  UpdatePlanRequest,
  RatingDimension,
  RatingResult,
  ViolationRecord,
} from '@/types/insp/template'
import type { ProjectStatus } from '@/types/insp/enums'

const BASE = '/inspection/projects'

// ==================== 项目 CRUD ====================

export function getProjects(params?: {
  status?: ProjectStatus
}): Promise<InspProject[]> {
  return http.get<InspProject[]>(BASE, { params })
}

/** 列表页聚合视图 — 项目 + 任务统计 + 检查员人数 (单次查询消除 N+1) */
export interface ProjectStatsSummary {
  project: InspProject
  taskTotal: number
  taskDone: number
  taskOverdue: number
  taskPendingReview: number
  inspectorCount: number
}
export function getProjectsWithStats(params?: { status?: ProjectStatus }): Promise<ProjectStatsSummary[]> {
  return http.get<ProjectStatsSummary[]>(`${BASE}/with-stats`, { params })
}

export function getProject(id: LongId): Promise<InspProject> {
  return http.get<InspProject>(`${BASE}/${id}`)
}

export function createProject(data: CreateProjectRequest): Promise<InspProject> {
  return http.post<InspProject>(BASE, data)
}

export function updateProject(id: LongId, data: UpdateProjectRequest): Promise<InspProject> {
  return http.put<InspProject>(`${BASE}/${id}`, data)
}

export function deleteProject(id: LongId): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 项目生命周期 ====================

export function publishProject(id: LongId, data: PublishProjectRequest): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/publish`, data)
}

/** review #1: 升级项目模板版本至模板的最新已发布版本 */
export function upgradeTemplateVersion(id: LongId): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/upgrade-template-version`)
}

/** review #12: 查询模板版本对比状态 */
export interface TemplateVersionStatus {
  drifted: boolean
  currentVersionId: LongId | null
  currentVersionNumber?: number
  latestVersionId?: LongId
  latestVersionNumber?: number
  templateId?: LongId
  rootSectionId?: LongId | null
  multiTemplate?: boolean
}
export function getTemplateVersionStatus(id: LongId): Promise<TemplateVersionStatus> {
  return http.get<TemplateVersionStatus>(`${BASE}/${id}/template-version-status`)
}

export function pauseProject(id: LongId): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/pause`)
}

export function resumeProject(id: LongId): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/resume`)
}

export function completeProject(id: LongId): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/complete`)
}

export function archiveProject(id: LongId): Promise<InspProject> {
  return http.post<InspProject>(`${BASE}/${id}/archive`)
}

// ==================== 项目分数 ====================

export function getProjectScores(projectId: LongId): Promise<ProjectScore[]> {
  return http.get<ProjectScore[]>(`${BASE}/${projectId}/scores`)
}

export function getScoreTree(projectId: LongId): Promise<any> {
  return http.get(`${BASE}/${projectId}/score-tree`)
}

export function previewTargetCount(data: {
  scopeType: string
  scopeConfig: string
  targetType?: string
}): Promise<number> {
  return http.post<number>(`${BASE}/target-preview`, data)
}

// ==================== 检查员池 ====================

export function getInspectors(projectId: LongId): Promise<ProjectInspector[]> {
  return http.get<ProjectInspector[]>(`${BASE}/${projectId}/inspectors`)
}

export function addInspector(projectId: LongId, data: AddInspectorRequest): Promise<ProjectInspector> {
  return http.post<ProjectInspector>(`${BASE}/${projectId}/inspectors`, data)
}

export function removeInspector(projectId: LongId, inspectorId: LongId): Promise<void> {
  return http.delete(`${BASE}/${projectId}/inspectors/${inspectorId}`)
}

// ==================== 运营配置 ====================

export function updateOperationalConfig(id: LongId, data: {
  projectName?: string
  assignmentMode?: string
  reviewRequired?: boolean
  autoPublish?: boolean
}): Promise<InspProject> {
  return http.patch<InspProject>(`${BASE}/${id}/config`, data)
}

// ==================== 检查计划 ====================

const PLAN_BASE = '/inspection/plans'

export function createPlan(data: CreatePlanRequest): Promise<InspectionPlan> {
  return http.post<InspectionPlan>(PLAN_BASE, data)
}

export function listPlans(projectId: LongId): Promise<InspectionPlan[]> {
  return http.get<InspectionPlan[]>(PLAN_BASE, { params: { projectId } })
}

export function getPlan(planId: LongId): Promise<InspectionPlan> {
  return http.get<InspectionPlan>(`${PLAN_BASE}/${planId}`)
}

export function updatePlan(planId: LongId, data: UpdatePlanRequest): Promise<InspectionPlan> {
  return http.put<InspectionPlan>(`${PLAN_BASE}/${planId}`, data)
}

export function deletePlan(planId: LongId): Promise<void> {
  return http.delete(`${PLAN_BASE}/${planId}`)
}

export function enablePlan(planId: LongId): Promise<void> {
  return http.post(`${PLAN_BASE}/${planId}/enable`)
}

export function disablePlan(planId: LongId): Promise<void> {
  return http.post(`${PLAN_BASE}/${planId}/disable`)
}

export function triggerPlan(planId: LongId): Promise<void> {
  return http.post(`${PLAN_BASE}/${planId}/trigger`)
}

// ==================== 评级维度 ====================

const DIMENSION_BASE = '/inspection/rating-dimensions'

export function createDimension(data: Partial<RatingDimension>): Promise<RatingDimension> {
  return http.post<RatingDimension>(DIMENSION_BASE, data)
}

export function listDimensions(projectId: LongId): Promise<RatingDimension[]> {
  return http.get<RatingDimension[]>(DIMENSION_BASE, { params: { projectId } })
}

export function getDimension(dimensionId: LongId): Promise<RatingDimension> {
  return http.get<RatingDimension>(`${DIMENSION_BASE}/${dimensionId}`)
}

export function updateDimension(dimensionId: LongId, data: Partial<RatingDimension>): Promise<RatingDimension> {
  return http.put<RatingDimension>(`${DIMENSION_BASE}/${dimensionId}`, data)
}

export function deleteDimension(dimensionId: LongId): Promise<void> {
  return http.delete(`${DIMENSION_BASE}/${dimensionId}`)
}

export function calculateRatings(dimensionId: LongId, params?: { cycleDate?: string }): Promise<RatingResult[]> {
  return http.post<RatingResult[]>(`${DIMENSION_BASE}/${dimensionId}/calculate`, null, { params })
}

export function getRankings(dimensionId: LongId, params?: { cycleDate?: string }): Promise<RatingResult[]> {
  return http.get<RatingResult[]>(`${DIMENSION_BASE}/${dimensionId}/rankings`, { params })
}

// ==================== 违纪记录 ====================

const VIOLATION_BASE = '/inspection/violation-records'

export function createViolationRecord(data: Partial<ViolationRecord>): Promise<ViolationRecord> {
  return http.post<ViolationRecord>(VIOLATION_BASE, data)
}

export function listViolationsBySubmission(submissionId: LongId): Promise<ViolationRecord[]> {
  return http.get<ViolationRecord[]>(VIOLATION_BASE, { params: { submissionId } })
}

export function listViolationsByUser(userId: LongId, params?: { startDate?: string; endDate?: string }): Promise<ViolationRecord[]> {
  return http.get<ViolationRecord[]>(VIOLATION_BASE, { params: { userId, ...params } })
}

export function getViolationRecord(recordId: LongId): Promise<ViolationRecord> {
  return http.get<ViolationRecord>(`${VIOLATION_BASE}/${recordId}`)
}

export function updateViolationRecord(recordId: LongId, data: Partial<ViolationRecord>): Promise<ViolationRecord> {
  return http.put<ViolationRecord>(`${VIOLATION_BASE}/${recordId}`, data)
}

export function deleteViolationRecord(recordId: LongId): Promise<void> {
  return http.delete(`${VIOLATION_BASE}/${recordId}`)
}

// ==================== API 对象 ====================

export const inspProjectApi = {
  getList: getProjects,
  getListWithStats: getProjectsWithStats,
  getById: getProject,
  create: createProject,
  update: updateProject,
  delete: deleteProject,
  publish: publishProject,
  upgradeTemplateVersion,
  getTemplateVersionStatus,
  pause: pauseProject,
  resume: resumeProject,
  complete: completeProject,
  archive: archiveProject,
  getProjectScores,
  getScoreTree,
  previewTargetCount,
  getInspectors,
  addInspector,
  removeInspector,
  updateOperationalConfig,
}

export const inspPlanApi = {
  create: createPlan,
  list: listPlans,
  getById: getPlan,
  update: updatePlan,
  delete: deletePlan,
  enable: enablePlan,
  disable: disablePlan,
  trigger: triggerPlan,
}

export const inspRatingDimensionApi = {
  create: createDimension,
  list: listDimensions,
  getById: getDimension,
  update: updateDimension,
  delete: deleteDimension,
  calculate: calculateRatings,
  getRankings,
}

export const inspViolationApi = {
  create: createViolationRecord,
  listBySubmission: listViolationsBySubmission,
  listByUser: listViolationsByUser,
  getById: getViolationRecord,
  update: updateViolationRecord,
  delete: deleteViolationRecord,
}
