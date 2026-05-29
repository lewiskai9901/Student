<template>
  <div class="sp-root insp-shell">
    <!-- Top bar -->
    <header class="sp-topbar">
      <div class="sp-topbar__lead">
        <button class="sp-back" @click="goBack" title="返回项目">
          <ArrowLeft :size="14" />
        </button>
        <div class="sp-head-text">
          <span class="insp-eyebrow">评分配置 · 按章节</span>
          <h1 class="sp-title">{{ currentSectionName || '当前章节' }}</h1>
        </div>
      </div>
      <div class="sp-topbar__actions">
        <InspButton v-if="profile && dirty" variant="accent" @click="saveProfile">
          保存配置
        </InspButton>
      </div>
    </header>

    <!-- Section tabs bar (L2 2026-05-26: 多章节项目内快速切换, 单章则隐藏) -->
    <nav v-if="profile && projectSectionTabs.length > 1" class="sp-sections-bar" aria-label="章节切换">
      <button
        v-for="tab in projectSectionTabs"
        :key="tab.profileId"
        class="sp-section-tab"
        :class="{ 'sp-section-tab--active': tab.profileId === profile?.id }"
        :title="tab.sectionName"
        @click="switchToProfile(tab.profileId)"
      >
        <span class="sp-section-tab-dot" aria-hidden></span>
        <span class="sp-section-tab-name">{{ tab.sectionName }}</span>
      </button>
    </nav>

    <!-- Loading -->
    <div v-if="loading" class="sp-state">
      <InspSpinner />
    </div>

    <!-- Load error (区分于"无配置") -->
    <div v-else-if="loadError" class="sp-state">
      <InspEmptyState title="加载评分配置失败" :description="loadError">
        <template #action>
          <InspButton variant="accent" @click="reload">重试</InspButton>
        </template>
      </InspEmptyState>
    </div>

    <!-- No profile yet (fallback, normally auto-created) -->
    <div v-else-if="!profile" class="sp-state">
      <InspEmptyState title="尚未初始化评分配置" description="点击下方按钮创建默认配置">
        <template #action>
          <InspButton variant="accent" @click="initProfile">创建配置</InspButton>
        </template>
      </InspEmptyState>
    </div>

    <!-- Concept diagram (P3a 2026-05-26 + L4 2026-05-26: 项目评级跳转) -->
    <ConceptDiagram v-else-if="profile" :project-id="profile?.projectId" />

    <!-- Main 2-column layout (L3 2026-05-26: 左列改手风琴, 删独立健康检查卡) -->
    <div v-if="profile" class="sp-body">
      <!-- LEFT: Scrollable accordion column -->
      <div class="sp-left">
        <InspAccordion
          id="sp-anchor-raw"
          title="基础设置"
          :summary="basicSummary"
          :status="basicStatus"
          :default-expanded="true"
          storage-key="sp-acc-basic"
        >
          <div class="sp-grid-3">
            <div class="sp-fld">
              <label title="分数的绝对上限">最高分</label>
              <input class="insp-input" v-model.number="profileForm.maxScore" type="number" @input="dirty = true" />
            </div>
            <div class="sp-fld">
              <label title="分数的绝对下限">最低分</label>
              <input class="insp-input" v-model.number="profileForm.minScore" type="number" @input="dirty = true" />
            </div>
            <div class="sp-fld">
              <label title="最终分数保留的小数位数">精度</label>
              <input class="insp-input" v-model.number="profileForm.precisionDigits" type="number" min="0" max="4" @input="dirty = true" />
            </div>
          </div>

          <!-- 1.13 章节级归一化 (规模公平性) -->
          <div class="sp-norm">
            <div class="sp-norm-head">规模归一化</div>
            <div class="sp-grid-3">
              <div class="sp-fld">
                <label title="本章节扣分按哪个维度摊平">归一化维度</label>
                <el-select
                  v-model="profileForm.normalizeBy"
                  size="small"
                  @change="onNormalizeByChange"
                >
                  <el-option
                    v-for="opt in NormalizeByOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </div>
              <div v-if="profileForm.normalizeBy !== 'NONE'" class="sp-fld">
                <label title="归一化的计算方式">归一化方式</label>
                <el-select
                  v-model="profileForm.normalizationMode"
                  size="small"
                  @change="dirty = true"
                >
                  <el-option
                    v-for="opt in NormalizationModeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </div>
              <div v-if="profileForm.normalizeBy !== 'NONE'" class="sp-fld">
                <label title="归一化基准规模, 低于此规模不摊平">基准规模</label>
                <el-input-number
                  v-model="profileForm.baselinePopulation"
                  :min="1"
                  :step="1"
                  size="small"
                  controls-position="right"
                  @change="dirty = true"
                />
              </div>
            </div>

            <div v-if="profileForm.normalizeBy !== 'NONE'" class="sp-grid-3">
              <div class="sp-fld">
                <label title="归一化系数下限, 留空不限">系数下限 (可空)</label>
                <el-input-number
                  v-model="profileForm.normFloor"
                  :min="0"
                  :step="0.1"
                  :controls="false"
                  size="small"
                  placeholder="不限"
                  @change="dirty = true"
                />
              </div>
              <div class="sp-fld">
                <label title="归一化系数上限, 留空不限">系数上限 (可空)</label>
                <el-input-number
                  v-model="profileForm.normCap"
                  :min="0"
                  :step="0.1"
                  :controls="false"
                  size="small"
                  placeholder="不限"
                  @change="dirty = true"
                />
              </div>
            </div>

            <p class="sp-norm-hint">
              人多、场所多或子组织多的单位，扣分会按此基数摊平，保证不同规模单位评分公平。
            </p>
          </div>
        </InspAccordion>

        <InspAccordion
          id="sp-anchor-dims"
          title="评分维度"
          :summary="dimSummary"
          :status="dimStatus"
          :default-expanded="true"
          storage-key="sp-acc-dims"
        >
          <DimensionTable :dimensions="store.dimensions" />
        </InspAccordion>

        <InspAccordion
          id="sp-anchor-rules"
          title="计算规则链"
          :summary="ruleSummary"
          :status="ruleStatus"
          :default-expanded="false"
          storage-key="sp-acc-rules"
        >
          <CalcRuleChain
            :rules="store.rules"
            @create="handleCreateRule"
            @update="handleUpdateRule"
            @delete="handleDeleteRule"
          />
        </InspAccordion>

        <InspAccordion
          id="sp-anchor-adv"
          title="高级算法 (可选)"
          :summary="advSummary"
          :status="advStatus"
          :default-expanded="false"
          storage-key="sp-acc-adv"
        >
          <AdvancedScoringSettings
            v-if="profile"
            :profile="profile"
            @save="handleSaveAdvancedSettings"
          />
        </InspAccordion>
      </div>

      <!-- RIGHT: Sticky sidebar (健康检查已融入手风琴头部状态点 L3) -->
      <div class="sp-right">
        <!-- Version History (1.7) -->
        <VersionHistory
          v-if="profile"
          :versions="store.versions"
          :current-version="profile.currentVersion || 0"
          @publish="handlePublishVersion"
        />

        <!-- Score Simulator (L4 将替换为 RealtimePreview) -->
        <ScoreSimulator
          v-if="profile"
          :profile="profile"
          :dimensions="store.dimensions"
          :rules="store.rules"
          :template-id="profile.sectionId"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from 'lucide-vue-next'
import { useInspScoringStore } from '@/stores/inspection/inspScoringStore'
import type {
  ScoringProfile,
  CreateRuleRequest,
  UpdateRuleRequest,
  UpdateAdvancedSettingsRequest,
  NormalizeBy,
  NormalizationMode,
} from '@/types/insp/scoring'
import { NormalizeByOptions, NormalizationModeOptions } from '@/types/insp/scoring'
import DimensionTable from './components/DimensionTable.vue'
import CalcRuleChain from './components/CalcRuleChain.vue'
import ScoreSimulator from './components/ScoreSimulator.vue'
import VersionHistory from './components/VersionHistory.vue'
import AdvancedScoringSettings from './components/AdvancedScoringSettings.vue'
import ConceptDiagram from './components/ConceptDiagram.vue'
import InspAccordion from '../shared/InspAccordion.vue'
import { getProject } from '@/api/inspection/project'
import { getSections } from '@/api/inspection/template'
import { getProfiles } from '@/api/inspection/scoring'
import InspButton from '../shared/InspButton.vue'
import InspSpinner from '../shared/InspSpinner.vue'
import InspEmptyState from '../shared/InspEmptyState.vue'

const route = useRoute()
const router = useRouter()
const store = useInspScoringStore()

const loading = ref(true)
const dirty = ref(false)
const loadError = ref<string>('')
const profile = ref<ScoringProfile | null>(null)

// L2: 章节切换栏数据 — 本项目所有 (有 profile 的) section
interface SectionTab {
  profileId: LongId
  sectionId: LongId
  sectionName: string
}
const projectSectionTabs = ref<SectionTab[]>([])
const currentSectionName = computed(() => {
  if (!profile.value) return ''
  return projectSectionTabs.value.find(t => t.profileId === profile.value!.id)?.sectionName
    ?? `分区 #${profile.value.sectionId}`
})

function msg(e: unknown): string {
  return (e as { message?: string })?.message || '请稍后重试'
}

const profileForm = reactive({
  maxScore: 100,
  minScore: 0,
  precisionDigits: 2,
  // 1.13 章节级归一化 (规模公平性)
  normalizeBy: 'NONE' as NormalizeBy,
  normalizationMode: 'NONE' as NormalizationMode,
  baselinePopulation: 1,
  normFloor: null as number | null,
  normCap: null as number | null,
})

// 维度切回不归一时, 同步清空方式 (与后端 NONE 语义一致)
function onNormalizeByChange() {
  dirty.value = true
  if (profileForm.normalizeBy === 'NONE') {
    profileForm.normalizationMode = 'NONE'
  } else if (profileForm.normalizationMode === 'NONE') {
    // 选了维度但方式仍为 NONE → 默认人均
    profileForm.normalizationMode = 'PER_CAPITA'
  }
}

const templateId = ref<LongId>('')

// ==================== Accordion Summaries (L3 2026-05-26) ====================
// 头部摘要 + 状态点取代原 sp-health 独立卡片

type SectionStatus = 'ok' | 'warn' | 'error' | 'neutral'

// 1. 基础设置
const basicStatus = computed<SectionStatus>(() => {
  const { maxScore, minScore, precisionDigits } = profileForm
  if (maxScore == null || minScore == null) return 'error'
  if (maxScore <= minScore) return 'error'
  if (precisionDigits == null || precisionDigits < 0 || precisionDigits > 4) return 'error'
  return 'ok'
})
const basicSummary = computed(() => {
  const { maxScore, minScore, precisionDigits } = profileForm
  return `${minScore}–${maxScore} 分 · 精度 ${precisionDigits}`
})

// 2. 评分维度
const dimStatus = computed<SectionStatus>(() => {
  const dims = store.dimensions
  if (dims.length === 0) return 'error'
  const total = dims.reduce((s, d) => s + d.weight, 0)
  return total === 100 ? 'ok' : 'error'
})
const dimSummary = computed(() => {
  const dims = store.dimensions
  if (dims.length === 0) return '未配置 ✗'
  const total = dims.reduce((s, d) => s + d.weight, 0)
  return `${dims.length} 子项 · 权重合 ${total}%${total === 100 ? ' ✓' : ' ✗'}`
})

// 3. 规则链 (可选)
const ruleStatus = computed<SectionStatus>(() => {
  const rules = store.rules
  if (rules.length === 0) return 'neutral'
  const enabled = rules.filter(r => r.isEnabled).length
  return enabled > 0 ? 'ok' : 'warn'
})
const ruleSummary = computed(() => {
  const rules = store.rules
  if (rules.length === 0) return '未配置 (可选)'
  const enabled = rules.filter(r => r.isEnabled).length
  return `${rules.length} 条 · ${enabled} 已启用`
})

// 4. 高级算法 (可选)
const advStatus = computed<SectionStatus>(() => {
  if (!profile.value) return 'neutral'
  const p = profile.value
  const anyOn = p.trendFactorEnabled || p.decayEnabled || p.calibrationEnabled
    || (p.multiRaterMode && p.multiRaterMode !== 'LATEST')
  return anyOn ? 'ok' : 'neutral'
})
const advSummary = computed(() => {
  if (!profile.value) return '全关'
  const p = profile.value
  const features: string[] = []
  if (p.trendFactorEnabled) features.push('趋势')
  if (p.decayEnabled) features.push('衰减')
  if (p.multiRaterMode && p.multiRaterMode !== 'LATEST') features.push('多人评')
  if (p.calibrationEnabled) features.push('校准')
  return features.length === 0 ? '全关' : features.join(' · ')
})

// ==================== Lifecycle ====================

async function loadAll() {
  loading.value = true
  loadError.value = ''
  const id = route.params.id ? route.params.id as string : null
  const tid = route.query.templateId ? String(route.query.templateId) : null
  const pid = route.query.projectId ? String(route.query.projectId) : null

  try {
    if (id) {
      await store.loadProfileFull(id)
      profile.value = store.currentProfile
    } else if (tid) {
      templateId.value = tid
      if (!pid) {
        loadError.value = '缺少 projectId 参数 — 评分方案现为项目-owned, 请从项目内进入'
        return
      }
      let p = await store.loadProfileByProjectAndSection(pid, tid)
      if (!p) {
        // 自动创建默认配置，无需手动点击
        p = await store.createProfile(tid, pid)
      }
      if (p) {
        profile.value = p
        // GradeBand 已迁移到「评级」Tab Indicator + GradeScheme, 此处不再加载
        await Promise.all([
          store.syncDimensions(p.id),
          store.loadRules(p.id),
        ])
      }
    }
    if (profile.value) {
      syncFormFromProfile(profile.value)
    }
  } catch (e) {
    // 加载失败与"未初始化配置"明确区分
    loadError.value = msg(e)
    profile.value = null
  } finally {
    loading.value = false
  }
}

function reload() {
  loadAll()
}

// L2: 加载本项目所有评分章节, 构建 tabs bar 数据
async function loadSectionTabs(projectId: LongId) {
  try {
    const proj = await getProject(projectId)
    if (!proj.rootSectionId) {
      projectSectionTabs.value = []
      return
    }
    const [sections, profiles] = await Promise.all([
      getSections(proj.rootSectionId),
      getProfiles(projectId),
    ])
    const nameMap = new Map<string, string>()
    for (const s of sections) nameMap.set(String(s.id), s.sectionName)
    projectSectionTabs.value = profiles
      .filter(p => p.sectionId != null)
      .map(p => ({
        profileId: p.id,
        sectionId: p.sectionId,
        sectionName: nameMap.get(String(p.sectionId)) ?? `分区 #${p.sectionId}`,
      }))
  } catch (e) {
    console.warn('加载章节切换栏失败', e)
    projectSectionTabs.value = []
  }
}

// 切换到另一个 profile (dirty 走 onBeforeRouteLeave 自动拦截)
function switchToProfile(profileId: LongId) {
  if (!profile.value || profileId === profile.value.id) return
  router.push(`/inspection/scoring/${profileId}`)
}

// 路由切换时重新加载 (Vue Router 在同一组件不同 :id 不会重跑 onMounted)
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) loadAll()
})

// profile 加载完 → 拉本项目的章节列表
watch(() => profile.value?.projectId, (pid) => {
  if (pid) loadSectionTabs(String(pid))
  else projectSectionTabs.value = []
})

onMounted(loadAll)

// 离开页面前确认未保存的基础设置
onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('基础设置有未保存的修改，确定离开吗？', '未保存的修改', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '留下',
    })
    return true
  } catch {
    return false
  }
})

watch(() => store.currentProfile, (p) => {
  profile.value = p
  if (p) syncFormFromProfile(p)
})

function syncFormFromProfile(p: ScoringProfile) {
  profileForm.maxScore = p.maxScore
  profileForm.minScore = p.minScore
  profileForm.precisionDigits = p.precisionDigits
  profileForm.normalizeBy = p.normalizeBy ?? 'NONE'
  profileForm.normalizationMode = p.normalizationMode ?? 'NONE'
  profileForm.baselinePopulation = p.baselinePopulation ?? 1
  profileForm.normFloor = p.normFloor ?? null
  profileForm.normCap = p.normCap ?? null
}

function goBack() {
  router.back()
}

async function initProfile() {
  const tid = templateId.value || (route.query.templateId ? String(route.query.templateId) : '')
  const pid = route.query.projectId ? String(route.query.projectId) : ''
  if (!tid) {
    ElMessage.error('缺少分区信息，无法创建评分配置')
    return
  }
  if (!pid) {
    ElMessage.error('缺少 projectId — 评分方案现为项目-owned, 请从项目内进入')
    return
  }
  try {
    const p = await store.createProfile(tid, pid)
    profile.value = p
    syncFormFromProfile(p)
    loadError.value = ''
    ElMessage.success('评分配置已创建')
  } catch (e) {
    ElMessage.error('创建评分配置失败: ' + msg(e))
  }
}

async function saveProfile() {
  if (!profile.value) return
  try {
    await store.updateProfile(profile.value.id, {
      maxScore: profileForm.maxScore,
      minScore: profileForm.minScore,
      precisionDigits: profileForm.precisionDigits,
      normalizeBy: profileForm.normalizeBy,
      normalizationMode: profileForm.normalizationMode,
      baselinePopulation: profileForm.baselinePopulation,
      normFloor: profileForm.normFloor,
      normCap: profileForm.normCap,
    })
    dirty.value = false
    ElMessage.success('基础设置已保存')
  } catch (e) {
    // 保存失败时保留 dirty, 用户可重试
    ElMessage.error('保存基础设置失败: ' + msg(e))
  }
}

// Rule handlers
async function handleCreateRule(
  data: CreateRuleRequest,
  onDone: (ok: boolean) => void,
) {
  if (!profile.value) { onDone(false); return }
  try {
    await store.createRule(profile.value.id, data)
    ElMessage.success('规则已添加')
    onDone(true)
  } catch (e) {
    ElMessage.error('添加规则失败: ' + msg(e))
    onDone(false)
  }
}
async function handleUpdateRule(
  id: LongId,
  data: UpdateRuleRequest,
  onDone?: (ok: boolean) => void,
) {
  if (!profile.value) { onDone?.(false); return }
  try {
    await store.updateRule(profile.value.id, id, data)
    ElMessage.success('规则已更新')
    onDone?.(true)
  } catch (e) {
    ElMessage.error('更新规则失败: ' + msg(e))
    onDone?.(false)
  }
}
async function handleDeleteRule(id: LongId) {
  if (!profile.value) return
  try {
    await store.deleteRule(profile.value.id, id)
    ElMessage.success('规则已删除')
  } catch (e) {
    ElMessage.error('删除规则失败: ' + msg(e))
  }
}

// Advanced settings handler (1.9-1.12)
async function handleSaveAdvancedSettings(
  data: UpdateAdvancedSettingsRequest,
  onDone: (ok: boolean) => void,
) {
  if (!profile.value) { onDone(false); return }
  try {
    await store.updateAdvancedSettings(profile.value.id, data)
    profile.value = store.currentProfile
    ElMessage.success('高级设置已保存')
    onDone(true)
  } catch (e) {
    ElMessage.error('保存高级设置失败: ' + msg(e))
    onDone(false)
  }
}

// Version handler (1.7)
async function handlePublishVersion(
  changeSummary: string,
  onDone: (ok: boolean) => void,
) {
  if (!profile.value) { onDone(false); return }
  try {
    await store.publishVersion(profile.value.id, { changeSummary })
    profile.value = store.currentProfile
    ElMessage.success('版本已发布')
    onDone(true)
  } catch (e) {
    ElMessage.error('发布版本失败: ' + msg(e))
    onDone(false)
  }
}
</script>

<style scoped>
/* ============================================================
 * ScoringProfileEditor — A 级 token 化
 * ============================================================ */
.sp-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--insp-bg-page);
}

/* Top bar */
.sp-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--insp-sp-3) var(--insp-sp-4);
  background: var(--insp-bg-surface);
  border-bottom: 1px solid var(--insp-border-default);
  flex-shrink: 0;
}
.sp-topbar__lead { display: flex; align-items: center; gap: var(--insp-sp-3); }
.sp-topbar__actions { display: flex; align-items: center; gap: var(--insp-sp-2); }

/* L2: 章节切换栏 (类似 VS Code editor tabs) */
.sp-sections-bar {
  display: flex;
  align-items: stretch;
  gap: 0;
  padding: 0 var(--insp-sp-4);
  background: var(--insp-bg-surface);
  border-bottom: 1px solid var(--insp-border-default);
  overflow-x: auto;
  flex-shrink: 0;
}
.sp-section-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  color: var(--insp-ink-tertiary, #6b7280);
  white-space: nowrap;
  transition: color 0.15s, border-color 0.15s, background 0.15s;
}
.sp-section-tab:hover {
  color: var(--insp-ink-primary, #111827);
  background: var(--insp-bg-subtle, #fafbfc);
}
.sp-section-tab--active {
  color: var(--insp-accent, #2563eb);
  border-bottom-color: var(--insp-accent, #2563eb);
  font-weight: 600;
}
.sp-section-tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.6;
  flex-shrink: 0;
}
.sp-section-tab--active .sp-section-tab-dot { opacity: 1; }

.sp-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--insp-h-md);
  height: var(--insp-h-md);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  background: var(--insp-bg-surface);
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  transition: background var(--insp-t-fast), color var(--insp-t-fast), border-color var(--insp-t-fast);
}
.sp-back:hover {
  background: var(--insp-bg-subtle);
  color: var(--insp-ink-primary);
  border-color: var(--insp-border-strong);
}

.sp-head-text { display: flex; flex-direction: column; gap: 2px; }
.sp-title {
  font-family: var(--insp-font-display);
  font-size: var(--insp-text-h2);
  font-weight: var(--insp-fw-bold);
  letter-spacing: var(--insp-tracking-tight);
  color: var(--insp-ink-primary);
  margin: 0;
  line-height: var(--insp-leading-tight);
}

/* States (loading / empty) */
.sp-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Body 2-column */
.sp-body { flex: 1; display: flex; overflow: hidden; }
.sp-left {
  flex: 1;
  overflow-y: auto;
  padding: var(--insp-sp-4);
  display: flex;
  flex-direction: column;
  gap: var(--insp-sp-3);
  min-width: 0;
}
.sp-right {
  width: 360px;
  flex-shrink: 0;
  border-left: 1px solid var(--insp-border-default);
  background: var(--insp-bg-surface);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}
@media (max-width: 1280px) {
  .sp-right { width: 300px; }
}
@media (max-width: 1024px) {
  .sp-body { flex-direction: column; }
  .sp-right {
    width: auto;
    border-left: none;
    border-top: 1px solid var(--insp-border-default);
  }
}

/* Form fields (基础设置手风琴内部) */
.sp-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--insp-sp-3);
}
.sp-fld { display: flex; flex-direction: column; gap: var(--insp-sp-1); }
.sp-fld label {
  display: block;
  font-size: var(--insp-text-xs);
  font-weight: var(--insp-fw-medium);
  color: var(--insp-ink-tertiary);
}
.sp-fld .insp-input { width: 100%; }
.sp-fld :deep(.el-select),
.sp-fld :deep(.el-input-number) { width: 100%; }

/* 1.13 章节级归一化块 */
.sp-norm {
  margin-top: var(--insp-sp-4);
  padding-top: var(--insp-sp-3);
  border-top: 1px solid var(--insp-border-default);
  display: flex;
  flex-direction: column;
  gap: var(--insp-sp-3);
}
.sp-norm-head {
  font-size: var(--insp-text-xs);
  font-weight: var(--insp-fw-medium);
  color: var(--insp-ink-secondary, var(--insp-ink-primary));
}
.sp-norm-hint {
  margin: 0;
  font-size: var(--insp-text-xs);
  line-height: var(--insp-leading-normal, 1.5);
  color: var(--insp-ink-tertiary);
}

/* L3 2026-05-26: 手风琴 body 内, 隐藏子组件冗余的 title (保留 button) */
.sp-left :deep(.crc-header > .sp-section-title) { display: none; }
.sp-left :deep(.adv-top > .adv-title) { display: none; }
/* DimensionTable 内部小标题 "子项权重" 保留 — 视觉权重低且有 hint 副标题 */
</style>
