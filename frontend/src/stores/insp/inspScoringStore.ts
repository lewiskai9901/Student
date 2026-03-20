/**
 * V7 检查平台 - 评分引擎 Store
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  ScoringProfile,
  ScoreDimension,
  GradeBand,
  CalculationRule,
  ScoringProfileVersion,
  UpdateProfileRequest,
  CreateDimensionRequest,
  UpdateDimensionRequest,
  CreateGradeBandRequest,
  UpdateGradeBandRequest,
  CreateRuleRequest,
  UpdateRuleRequest,
  PublishVersionRequest,
  UpdateAdvancedSettingsRequest,
} from '@/types/insp/scoring'
import {
  getProfiles,
  getProfile,
  getProfileBySection,
  createProfile as createProfileApi,
  updateProfile as updateProfileApi,
  deleteProfile as deleteProfileApi,
  getDimensions,
  createDimension as createDimensionApi,
  updateDimension as updateDimensionApi,
  deleteDimension as deleteDimensionApi,
  syncDimensionsFromModules as syncDimensionsApi,
  getGradeBands,
  createGradeBand as createGradeBandApi,
  updateGradeBand as updateGradeBandApi,
  deleteGradeBand as deleteGradeBandApi,
  getRules,
  createRule as createRuleApi,
  updateRule as updateRuleApi,
  deleteRule as deleteRuleApi,
  updateAdvancedSettings as updateAdvancedSettingsApi,
  publishVersion as publishVersionApi,
  getVersions,
  getVersion as getVersionApi,
} from '@/api/insp/scoring'

export const useInspScoringStore = defineStore('inspScoring', () => {
  // State
  const profiles = ref<ScoringProfile[]>([])
  const currentProfile = ref<ScoringProfile | null>(null)
  const dimensions = ref<ScoreDimension[]>([])
  const gradeBands = ref<GradeBand[]>([])
  const rules = ref<CalculationRule[]>([])
  const versions = ref<ScoringProfileVersion[]>([])

  // ===== Profile Actions =====

  async function loadProfiles() {
    profiles.value = await getProfiles()
  }

  async function loadProfile(id: number) {
    currentProfile.value = await getProfile(id)
    return currentProfile.value
  }

  async function loadProfileBySection(sectionId: number) {
    currentProfile.value = await getProfileBySection(sectionId) ?? null
    return currentProfile.value
  }

  /** @deprecated Use loadProfileBySection instead */
  async function loadProfileByTemplate(sectionId: number) {
    return loadProfileBySection(sectionId)
  }

  async function createProfile(sectionId: number) {
    const profile = await createProfileApi({ sectionId })
    currentProfile.value = profile
    return profile
  }

  async function updateProfile(id: number, data: UpdateProfileRequest) {
    const profile = await updateProfileApi(id, data)
    currentProfile.value = profile
    return profile
  }

  async function updateAdvancedSettings(id: number, data: UpdateAdvancedSettingsRequest) {
    const profile = await updateAdvancedSettingsApi(id, data)
    currentProfile.value = profile
    return profile
  }

  async function deleteProfile(id: number) {
    await deleteProfileApi(id)
    currentProfile.value = null
    dimensions.value = []
    gradeBands.value = []
    rules.value = []
  }

  // ===== Dimension Actions =====

  async function loadDimensions(profileId: number) {
    dimensions.value = await getDimensions(profileId)
  }

  async function syncDimensions(profileId: number) {
    dimensions.value = await syncDimensionsApi(profileId)
  }

  async function createDimension(profileId: number, data: CreateDimensionRequest) {
    const dimension = await createDimensionApi(profileId, data)
    dimensions.value.push(dimension)
    return dimension
  }

  async function updateDimension(profileId: number, dimensionId: number, data: UpdateDimensionRequest) {
    const dimension = await updateDimensionApi(profileId, dimensionId, data)
    const idx = dimensions.value.findIndex(d => String(d.id) === String(dimensionId))
    if (idx >= 0) dimensions.value[idx] = dimension
    return dimension
  }

  async function deleteDimension(profileId: number, dimensionId: number) {
    await deleteDimensionApi(profileId, dimensionId)
    dimensions.value = dimensions.value.filter(d => d.id !== dimensionId)
  }

  // ===== GradeBand Actions =====

  async function loadGradeBands(profileId: number) {
    gradeBands.value = await getGradeBands(profileId)
  }

  async function createGradeBand(profileId: number, data: CreateGradeBandRequest) {
    const band = await createGradeBandApi(profileId, data)
    gradeBands.value.push(band)
    return band
  }

  async function updateGradeBand(profileId: number, bandId: number, data: UpdateGradeBandRequest) {
    const band = await updateGradeBandApi(profileId, bandId, data)
    const idx = gradeBands.value.findIndex(b => String(b.id) === String(bandId))
    if (idx >= 0) gradeBands.value[idx] = band
    return band
  }

  async function deleteGradeBand(profileId: number, bandId: number) {
    await deleteGradeBandApi(profileId, bandId)
    gradeBands.value = gradeBands.value.filter(b => String(b.id) !== String(bandId))
  }

  // ===== Rule Actions =====

  async function loadRules(profileId: number) {
    rules.value = await getRules(profileId)
  }

  async function createRule(profileId: number, data: CreateRuleRequest) {
    const rule = await createRuleApi(profileId, data)
    rules.value.push(rule)
    rules.value.sort((a, b) => a.priority - b.priority)
    return rule
  }

  async function updateRule(profileId: number, ruleId: number, data: UpdateRuleRequest) {
    const rule = await updateRuleApi(profileId, ruleId, data)
    const idx = rules.value.findIndex(r => String(r.id) === String(ruleId))
    if (idx >= 0) rules.value[idx] = rule
    rules.value.sort((a, b) => a.priority - b.priority)
    return rule
  }

  async function deleteRule(profileId: number, ruleId: number) {
    await deleteRuleApi(profileId, ruleId)
    rules.value = rules.value.filter(r => String(r.id) !== String(ruleId))
  }

  // ===== Profile Versioning (1.7) =====

  async function loadVersions(profileId: number) {
    versions.value = await getVersions(profileId)
  }

  async function publishVersion(profileId: number, data: PublishVersionRequest) {
    const version = await publishVersionApi(profileId, data)
    versions.value.unshift(version)
    // Refresh profile to get updated currentVersion
    currentProfile.value = await getProfile(profileId)
    return version
  }

  async function getVersionDetail(profileId: number, versionNum: number) {
    return await getVersionApi(profileId, versionNum)
  }

  // ===== Load All Sub-resources =====

  async function loadProfileFull(profileId: number) {
    await Promise.all([
      loadProfile(profileId),
      loadDimensions(profileId),
      loadGradeBands(profileId),
      loadRules(profileId),
      loadVersions(profileId),
    ])
  }

  return {
    profiles,
    currentProfile,
    dimensions,
    gradeBands,
    rules,
    versions,
    loadProfiles,
    loadProfile,
    loadProfileBySection,
    loadProfileByTemplate,
    loadProfileFull,
    createProfile,
    updateProfile,
    updateAdvancedSettings,
    deleteProfile,
    loadDimensions,
    syncDimensions,
    createDimension,
    updateDimension,
    deleteDimension,
    loadGradeBands,
    createGradeBand,
    updateGradeBand,
    deleteGradeBand,
    loadRules,
    createRule,
    updateRule,
    deleteRule,
    loadVersions,
    publishVersion,
    getVersionDetail,
  }
})
