/**
 * V7 检查平台 - 评分引擎 API
 */
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

const BASE = '/v7/insp/scoring-profiles'

// ==================== 评分配置 ====================

export function getProfiles(): Promise<ScoringProfile[]> {
  return http.get<ScoringProfile[]>(BASE)
}

export function getProfile(id: number): Promise<ScoringProfile> {
  return http.get<ScoringProfile>(`${BASE}/${id}`)
}

export function getProfileByTemplate(templateId: number): Promise<ScoringProfile> {
  return http.get<ScoringProfile>(`${BASE}/by-template/${templateId}`)
}

export function createProfile(data: CreateProfileRequest): Promise<ScoringProfile> {
  return http.post<ScoringProfile>(BASE, data)
}

export function updateProfile(id: number, data: UpdateProfileRequest): Promise<ScoringProfile> {
  return http.put<ScoringProfile>(`${BASE}/${id}`, data)
}

export function updateAdvancedSettings(id: number, data: UpdateAdvancedSettingsRequest): Promise<ScoringProfile> {
  return http.put<ScoringProfile>(`${BASE}/${id}/advanced-settings`, data)
}

export function deleteProfile(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// ==================== 评分维度 ====================

export function getDimensions(profileId: number): Promise<ScoreDimension[]> {
  return http.get<ScoreDimension[]>(`${BASE}/${profileId}/dimensions`)
}

export function createDimension(profileId: number, data: CreateDimensionRequest): Promise<ScoreDimension> {
  return http.post<ScoreDimension>(`${BASE}/${profileId}/dimensions`, data)
}

export function updateDimension(profileId: number, dimensionId: number, data: UpdateDimensionRequest): Promise<ScoreDimension> {
  return http.put<ScoreDimension>(`${BASE}/${profileId}/dimensions/${dimensionId}`, data)
}

export function deleteDimension(profileId: number, dimensionId: number): Promise<void> {
  return http.delete(`${BASE}/${profileId}/dimensions/${dimensionId}`)
}

export function syncDimensionsFromModules(profileId: number): Promise<ScoreDimension[]> {
  return http.post<ScoreDimension[]>(`${BASE}/${profileId}/dimensions/sync-modules`)
}

// ==================== 等级区间 ====================

export function getGradeBands(profileId: number): Promise<GradeBand[]> {
  return http.get<GradeBand[]>(`${BASE}/${profileId}/grade-bands`)
}

export function createGradeBand(profileId: number, data: CreateGradeBandRequest): Promise<GradeBand> {
  return http.post<GradeBand>(`${BASE}/${profileId}/grade-bands`, data)
}

export function updateGradeBand(profileId: number, bandId: number, data: UpdateGradeBandRequest): Promise<GradeBand> {
  return http.put<GradeBand>(`${BASE}/${profileId}/grade-bands/${bandId}`, data)
}

export function deleteGradeBand(profileId: number, bandId: number): Promise<void> {
  return http.delete(`${BASE}/${profileId}/grade-bands/${bandId}`)
}

// ==================== 计算规则 ====================

export function getRules(profileId: number): Promise<CalculationRule[]> {
  return http.get<CalculationRule[]>(`${BASE}/${profileId}/calculation-rules`)
}

export function createRule(profileId: number, data: CreateRuleRequest): Promise<CalculationRule> {
  return http.post<CalculationRule>(`${BASE}/${profileId}/calculation-rules`, data)
}

export function updateRule(profileId: number, ruleId: number, data: UpdateRuleRequest): Promise<CalculationRule> {
  return http.put<CalculationRule>(`${BASE}/${profileId}/calculation-rules/${ruleId}`, data)
}

export function deleteRule(profileId: number, ruleId: number): Promise<void> {
  return http.delete(`${BASE}/${profileId}/calculation-rules/${ruleId}`)
}

// ==================== 版本快照 (1.7) ====================

export function publishVersion(profileId: number, data: PublishVersionRequest): Promise<ScoringProfileVersion> {
  return http.post<ScoringProfileVersion>(`${BASE}/${profileId}/versions`, data)
}

export function getVersions(profileId: number): Promise<ScoringProfileVersion[]> {
  return http.get<ScoringProfileVersion[]>(`${BASE}/${profileId}/versions`)
}

export function getVersion(profileId: number, version: number): Promise<ScoringProfileVersion> {
  return http.get<ScoringProfileVersion>(`${BASE}/${profileId}/versions/${version}`)
}

// ==================== API 对象 ====================

export const scoringProfileApi = {
  getList: getProfiles,
  getById: getProfile,
  getByTemplate: getProfileByTemplate,
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
