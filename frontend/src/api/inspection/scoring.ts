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
  UpdateAdvancedSettingsRequest,
} from '@/types/insp/scoring'

const BASE = '/inspection/scoring-profiles'

// ==================== 评分配置 ====================

export function getProfiles(): Promise<ScoringProfile[]> {
  return http.get<ScoringProfile[]>(BASE)
}

export function getProfile(id: LongId): Promise<ScoringProfile> {
  return http.get<ScoringProfile>(`${BASE}/${id}`)
}

export function getProfileBySection(sectionId: LongId): Promise<ScoringProfile> {
  return http.get<ScoringProfile>(`${BASE}/by-section/${sectionId}`)
}

/** @deprecated Use getProfileBySection instead */
export function getProfileByTemplate(templateId: LongId): Promise<ScoringProfile> {
  return http.get<ScoringProfile>(`${BASE}/by-section/${templateId}`)
}

export function createProfile(data: CreateProfileRequest): Promise<ScoringProfile> {
  return http.post<ScoringProfile>(BASE, data)
}

export function updateProfile(id: LongId, data: UpdateProfileRequest): Promise<ScoringProfile> {
  return http.put<ScoringProfile>(`${BASE}/${id}`, data)
}

export function updateAdvancedSettings(id: LongId, data: UpdateAdvancedSettingsRequest): Promise<ScoringProfile> {
  return http.put<ScoringProfile>(`${BASE}/${id}/advanced-settings`, data)
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
  getBySection: getProfileBySection,
  getByTemplate: getProfileByTemplate, // @deprecated
  create: createProfile,
  update: updateProfile,
  updateAdvanced: updateAdvancedSettings,
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
