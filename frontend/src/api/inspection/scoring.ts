/**
 * 检查平台 - 评分引擎 API
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type {
  ScoringProfile,
  ScoreDimension,
  GradeBand,
  CalculationRule,
  CreateProfileRequest,
  UpdateProfileRequest,
  CreateDimensionRequest,
  UpdateDimensionRequest,
  CreateGradeBandRequest,
  UpdateGradeBandRequest,
  CreateRuleRequest,
  UpdateRuleRequest,
  ScoringProfileVersion,
  PublishVersionRequest,
} from '@/types/insp/scoring'

const BASE = '/inspection/scoring-profiles'

// ==================== 评分配置 ====================

/** 获取项目-owned 的评分方案列表. projectId 必传 — 后端不再支持全局列表. */
export function getProfiles(projectId: LongId): Promise<ScoringProfile[]> {
  return http.get<ScoringProfile[]>(BASE, { params: { projectId } })
}

export function getProfile(id: LongId): Promise<ScoringProfile> {
  return http.get<ScoringProfile>(`${BASE}/${id}`)
}

/**
 * 按 (projectId, sectionId) 查评分方案. ScoringProfile 项目-owned 后, 必须明确项目.
 * 后端不存在时返回 null (不抛错), 调用方需处理 null.
 */
export function getProfileByProjectAndSection(
  projectId: LongId, sectionId: LongId,
): Promise<ScoringProfile | null> {
  return http.get<ScoringProfile | null>(`${BASE}/by-project-section`, {
    params: { projectId, sectionId },
  })
}

export function createProfile(data: CreateProfileRequest): Promise<ScoringProfile> {
  return http.post<ScoringProfile>(BASE, data)
}

export function updateProfile(id: LongId, data: UpdateProfileRequest): Promise<ScoringProfile> {
  return http.put<ScoringProfile>(`${BASE}/${id}`, data)
}

export function deleteProfile(id: LongId): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 评分维度 ====================

export function getDimensions(profileId: LongId): Promise<ScoreDimension[]> {
  return http.get<ScoreDimension[]>(`${BASE}/${profileId}/dimensions`)
}

export function createDimension(profileId: LongId, data: CreateDimensionRequest): Promise<ScoreDimension> {
  return http.post<ScoreDimension>(`${BASE}/${profileId}/dimensions`, data)
}

export function updateDimension(profileId: LongId, dimensionId: LongId, data: UpdateDimensionRequest): Promise<ScoreDimension> {
  return http.put<ScoreDimension>(`${BASE}/${profileId}/dimensions/${dimensionId}`, data)
}

export function deleteDimension(profileId: LongId, dimensionId: LongId): Promise<void> {
  return http.delete(`${BASE}/${profileId}/dimensions/${dimensionId}`)
}

export function syncDimensionsFromModules(profileId: LongId): Promise<ScoreDimension[]> {
  return http.post<ScoreDimension[]>(`${BASE}/${profileId}/dimensions/sync-modules`)
}

// ==================== 等级区间 ====================

export function getGradeBands(profileId: LongId): Promise<GradeBand[]> {
  return http.get<GradeBand[]>(`${BASE}/${profileId}/grade-bands`)
}

export function createGradeBand(profileId: LongId, data: CreateGradeBandRequest): Promise<GradeBand> {
  return http.post<GradeBand>(`${BASE}/${profileId}/grade-bands`, data)
}

export function updateGradeBand(profileId: LongId, bandId: LongId, data: UpdateGradeBandRequest): Promise<GradeBand> {
  return http.put<GradeBand>(`${BASE}/${profileId}/grade-bands/${bandId}`, data)
}

export function deleteGradeBand(profileId: LongId, bandId: LongId): Promise<void> {
  return http.delete(`${BASE}/${profileId}/grade-bands/${bandId}`)
}

// ==================== 计算规则 ====================

export function getRules(profileId: LongId): Promise<CalculationRule[]> {
  return http.get<CalculationRule[]>(`${BASE}/${profileId}/calculation-rules`)
}

export function createRule(profileId: LongId, data: CreateRuleRequest): Promise<CalculationRule> {
  return http.post<CalculationRule>(`${BASE}/${profileId}/calculation-rules`, data)
}

export function updateRule(profileId: LongId, ruleId: LongId, data: UpdateRuleRequest): Promise<CalculationRule> {
  return http.put<CalculationRule>(`${BASE}/${profileId}/calculation-rules/${ruleId}`, data)
}

export function deleteRule(profileId: LongId, ruleId: LongId): Promise<void> {
  return http.delete(`${BASE}/${profileId}/calculation-rules/${ruleId}`)
}

// ==================== 版本快照 (1.7) ====================

export function publishVersion(profileId: LongId, data: PublishVersionRequest): Promise<ScoringProfileVersion> {
  return http.post<ScoringProfileVersion>(`${BASE}/${profileId}/versions`, data)
}

export function getVersions(profileId: LongId): Promise<ScoringProfileVersion[]> {
  return http.get<ScoringProfileVersion[]>(`${BASE}/${profileId}/versions`)
}

export function getVersion(profileId: LongId, version: number): Promise<ScoringProfileVersion> {
  return http.get<ScoringProfileVersion>(`${BASE}/${profileId}/versions/${version}`)
}

// ==================== API 对象 ====================

export const scoringProfileApi = {
  getList: getProfiles,
  getById: getProfile,
  getByProjectAndSection: getProfileByProjectAndSection,
  create: createProfile,
  update: updateProfile,
  delete: deleteProfile,
  getDimensions,
  createDimension,
  updateDimension,
  deleteDimension,
  syncDimensionsFromModules,
  getGradeBands,
  createGradeBand,
  updateGradeBand,
  deleteGradeBand,
  getRules,
  createRule,
  updateRule,
  deleteRule,
  publishVersion,
  getVersions,
  getVersion,
}
