/**
 * 检查平台 - 评分引擎 Store
 */
import type { LongId } from '@/types/common'
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
} from '@/api/inspection/scoring'

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

  async function loadProfile(id: LongId) {
    currentProfile.value = await getProfile(id)
    return currentProfile.value
  }

  async function loadProfileBySection(sectionId: LongId) {
    currentProfile.value = await getProfileBySection(sectionId) ?? null
    return currentProfile.value
  }

  /** @deprecated Use loadProfileBySection instead */
  async function loadProfileByTemplate(sectionId: LongId) {
    return loadProfileBySection(sectionId)
  }

  async function createProfile(sectionId: LongId) {
    const profile = await createProfileApi({ sectionId })
    currentProfile.value = profile
    return profile
  }

  async function updateProfile(id: LongId, data: UpdateProfileRequest) {
    const profile = await updateProfileApi(id, data)
    currentProfile.value = profile
    return profile
  }

  async function updateAdvancedSettings(id: LongId, data: UpdateAdvancedSettingsRequest) {
    const profile = await updateAdvancedSettingsApi(id, data)
    currentProfile.value = profile
    return profile
  }

  async function deleteProfile(id: LongId) {
    await deleteProfileApi(id)
    currentProfile.value = null
    dimensions.value = []
    gradeBands.value = []
    rules.value = []
  }

  // ===== Dimension Actions =====

  async function loadDimensions(profileId: LongId) {
    dimensions.value = await getDimensions(profileId)
  }

  async function syncDimensions(profileId: LongId) {
    dimensions.value = await syncDimensionsApi(profileId)
  }

  async function createDimension(profileId: LongId, data: CreateDimensionRequest) {
    const dimension = await createDimensionApi(profileId, data)
    dimensions.value.push(dimension)
    return dimension
  }

  async function updateDimension(profileId: LongId, dimensionId: LongId, data: UpdateDimensionRequest) {
    const dimension = await updateDimensionApi(profileId, dimensionId, data)
    const idx = dimensions.value.findIndex(d => String(d.id) === String(dimensionId))
    if (idx >= 0) dimensions.value[idx] = dimension
    return dimension
  }

  async function deleteDimension(profileId: LongId, dimensionId: LongId) {
    await deleteDimensionApi(profileId, dimensionId)
    dimensions.value = dimensions.value.filter(d => d.id !== dimensionId)
  }

  // ===== GradeBand Actions =====

  async function loadGradeBands(profileId: LongId) {
    gradeBands.value = await getGradeBands(profileId)
  }

  async function createGradeBand(profileId: LongId, data: CreateGradeBandRequest) {
    const band = await createGradeBandApi(profileId, data)
    gradeBands.value.push(band)
    return band
  }

  async function updateGradeBand(profileId: LongId, bandId: LongId, data: UpdateGradeBandRequest) {
    const band = await updateGradeBandApi(profileId, bandId, data)
    const idx = gradeBands.value.findIndex(b => String(b.id) === String(bandId))
    if (idx >= 0) gradeBands.value[idx] = band
    return band
  }

  async function deleteGradeBand(profileId: LongId, bandId: LongId) {
    await deleteGradeBandApi(profileId, bandId)
    gradeBands.value = gradeBands.value.filter(b => String(b.id) !== String(bandId))
  }

  // ===== Rule Actions =====

  async function loadRules(profileId: LongId) {
    rules.value = await getRules(profileId)
  }

  async function createRule(profileId: LongId, data: CreateRuleRequest) {
    const rule = await createRuleApi(profileId, data)
    rules.value.push(rule)
    rules.value.sort((a, b) => a.priority - b.priority)
    return rule
  }

  async function updateRule(profileId: LongId, ruleId: LongId, data: UpdateRuleRequest) {
    const rule = await updateRuleApi(profileId, ruleId, data)
    const idx = rules.value.findIndex(r => String(r.id) === String(ruleId))
    if (idx >= 0) rules.value[idx] = rule
    rules.value.sort((a, b) => a.priority - b.priority)
    return rule
  }

  async function deleteRule(profileId: LongId, ruleId: LongId) {
    await deleteRuleApi(profileId, ruleId)
    rules.value = rules.value.filter(r => String(r.id) !== String(ruleId))
  }

  // ===== Profile Versioning (1.7) =====

  async function loadVersions(profileId: LongId) {
    versions.value = await getVersions(profileId)
  }

  async function publishVersion(profileId: LongId, data: PublishVersionRequest) {
    const version = await publishVersionApi(profileId, data)
    versions.value.unshift(version)
    // Refresh profile to get updated currentVersion
    currentProfile.value = await getProfile(profileId)
    return version
  }

  async function getVersionDetail(profileId: LongId, versionNum: number) {
    return await getVersionApi(profileId, versionNum)
  }

  // ===== Load All Sub-resources =====

  async function loadProfileFull(profileId: LongId) {
    try {
      await Promise.all([
        loadProfile(profileId),
        loadDimensions(profileId),
        loadGradeBands(profileId),
        loadRules(profileId),
        loadVersions(profileId),
      ])
    } catch (e) {
      console.warn('Load full scoring profile failed', e)
      throw e
    }
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
