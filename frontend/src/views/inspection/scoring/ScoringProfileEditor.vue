<template>
  <div class="sp-root insp-shell">
    <!-- Top bar -->
    <header class="sp-topbar">
      <div class="sp-topbar__lead">
        <button class="sp-back" @click="goBack" title="返回">
          <ArrowLeft :size="14" />
        </button>
        <div class="sp-head-text">
          <span class="insp-eyebrow">评分方案</span>
          <h1 class="sp-title">汇总规则</h1>
        </div>
        <span v-if="profile" class="insp-chip insp-chip--info">分区 #{{ profile.sectionId }}</span>
      </div>
      <div class="sp-topbar__actions">
        <InspButton v-if="profile && dirty" variant="accent" @click="saveProfile">
          保存配置
        </InspButton>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="sp-state">
      <InspSpinner />
    </div>

    <!-- No profile yet (fallback, normally auto-created) -->
    <div v-else-if="!profile" class="sp-state">
      <InspEmptyState title="初始化配置失败" description="请检查分区配置或重试">
        <template #action>
          <InspButton variant="accent" @click="initProfile">重试</InspButton>
        </template>
      </InspEmptyState>
    </div>

    <!-- Main 2-column layout -->
    <div v-else class="sp-body">
      <!-- LEFT: Scrollable config column -->
      <div class="sp-left">
        <!-- Inline settings -->
        <section class="sp-card">
          <header class="sp-section-head">
            <h3 class="sp-section-title">基础设置</h3>
          </header>
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
        </section>

        <section class="sp-card">
          <DimensionTable :dimensions="store.dimensions" />
        </section>

        <section class="sp-card">
          <GradeBandEditor
            :grade-bands="store.gradeBands"
            @create="handleCreateGradeBand"
            @update="handleUpdateGradeBand"
            @delete="handleDeleteGradeBand"
            @apply-preset="handleApplyPreset"
          />
        </section>

        <section class="sp-card">
          <CalcRuleChain
            :rules="store.rules"
            @create="handleCreateRule"
            @update="handleUpdateRule"
            @delete="handleDeleteRule"
          />
        </section>

        <section class="sp-card">
          <AdvancedScoringSettings
            v-if="profile"
            :profile="profile"
            @save="handleSaveAdvancedSettings"
          />
        </section>
      </div>

      <!-- RIGHT: Sticky sidebar -->
      <div class="sp-right">
        <!-- Health Check -->
        <div class="sp-health">
          <div class="sp-health-title">
            <ShieldCheck :size="14" class="sp-health-icon" />
            <span>配置检查</span>
          </div>
          <div class="sp-checks">
            <div
              v-for="check in healthChecks"
              :key="check.key"
              class="sp-check"
              :class="check.status"
            >
              <component :is="check.status === 'ok' ? CheckCircle2 : check.status === 'warn' ? AlertTriangle : XCircle" :size="14" />
              <span>{{ check.label }}</span>
            </div>
          </div>
        </div>

        <!-- Version History (1.7) -->
        <VersionHistory
          v-if="profile"
          :versions="store.versions"
          :current-version="profile.currentVersion || 0"
          @publish="handlePublishVersion"
        />

        <!-- Score Simulator -->
        <ScoreSimulator
          v-if="profile"
          :profile="profile"
          :dimensions="store.dimensions"
          :grade-bands="store.gradeBands"
          :rules="store.rules"
          :template-id="profile.sectionId"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ShieldCheck, CheckCircle2, AlertTriangle, XCircle } from 'lucide-vue-next'
import { useInspScoringStore } from '@/stores/inspection/inspScoringStore'
import type {
  ScoringProfile,
  CreateGradeBandRequest,
  UpdateGradeBandRequest,
  CreateRuleRequest,
  UpdateRuleRequest,
  UpdateAdvancedSettingsRequest,
} from '@/types/insp/scoring'
import DimensionTable from './components/DimensionTable.vue'
import GradeBandEditor from './components/GradeBandEditor.vue'
import CalcRuleChain from './components/CalcRuleChain.vue'
import ScoreSimulator from './components/ScoreSimulator.vue'
import VersionHistory from './components/VersionHistory.vue'
import AdvancedScoringSettings from './components/AdvancedScoringSettings.vue'
import InspButton from '../shared/InspButton.vue'
import InspSpinner from '../shared/InspSpinner.vue'
import InspEmptyState from '../shared/InspEmptyState.vue'

const route = useRoute()
const router = useRouter()
const store = useInspScoringStore()

const loading = ref(true)
const dirty = ref(false)
const profile = ref<ScoringProfile | null>(null)

const profileForm = reactive({
  maxScore: 100,
  minScore: 0,
  precisionDigits: 2,
})

const templateId = ref<number>(0)

// ==================== Health Checks ====================

interface HealthCheck {
  key: string
  label: string
  status: 'ok' | 'warn' | 'error'
}

const healthChecks = computed<HealthCheck[]>(() => {
  const checks: HealthCheck[] = []

  // 1. Dimensions exist
  const dimCount = store.dimensions.length
  checks.push({
    key: 'dims',
    label: dimCount > 0 ? `${dimCount} 个子项` : '未配置子项权重',
    status: dimCount > 0 ? 'ok' : 'error',
  })

  // 2. Weight sum = 100
  if (dimCount > 0) {
    const totalWeight = store.dimensions.reduce((s, d) => s + d.weight, 0)
    checks.push({
      key: 'weight',
      label: totalWeight === 100 ? '子项权重合计 100%' : `子项权重合计 ${totalWeight}%`,
      status: totalWeight === 100 ? 'ok' : 'error',
    })
  }

  // 3. Grade bands exist
  const bandCount = store.gradeBands.length
  checks.push({
    key: 'grades',
    label: bandCount > 0 ? `${bandCount} 个等级区间` : '未配置等级映射',
    status: bandCount > 0 ? 'ok' : 'warn',
  })

  // 4. Grade bands coverage (check for gaps)
  if (bandCount > 0) {
    const globalBands = store.gradeBands
      .filter(b => !b.dimensionId)
      .sort((a, b) => a.minScore - b.minScore)
    let hasGap = false
    for (let i = 0; i < globalBands.length - 1; i++) {
      if (globalBands[i + 1].minScore - globalBands[i].maxScore > 0.01) {
        hasGap = true
        break
      }
    }
    if (globalBands.length > 0) {
      checks.push({
        key: 'coverage',
        label: hasGap ? '等级区间存在间隙' : '等级区间覆盖完整',
        status: hasGap ? 'warn' : 'ok',
      })
    }
  }

  // 5. Rules
  const ruleCount = store.rules.length
  const enabledRules = store.rules.filter(r => r.isEnabled).length
  checks.push({
    key: 'rules',
    label: ruleCount > 0 ? `${enabledRules}/${ruleCount} 条规则已启用` : '未配置计算规则',
    status: ruleCount > 0 ? 'ok' : 'warn',
  })

  return checks
})

// ==================== Lifecycle ====================

onMounted(async () => {
  const id = route.params.id ? Number(route.params.id) : null
  const tid = route.query.templateId ? Number(route.query.templateId) : null

  try {
    if (id) {
      await store.loadProfileFull(id)
      profile.value = store.currentProfile
    } else if (tid) {
      templateId.value = tid
      let p = await store.loadProfileBySection(tid)
      if (!p) {
        // 自动创建默认配置，无需手动点击
        p = await store.createProfile(tid)
      }
      if (p) {
        profile.value = p
        await Promise.all([
          store.syncDimensions(p.id),
          store.loadGradeBands(p.id),
          store.loadRules(p.id),
        ])
      }
    }
    if (profile.value) {
      syncFormFromProfile(profile.value)
    }
  } finally {
    loading.value = false
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
}

function goBack() {
  router.back()
}

async function initProfile() {
  const tid = templateId.value || (route.query.templateId ? Number(route.query.templateId) : 0)
  if (!tid) return
  const p = await store.createProfile(tid)
  profile.value = p
  syncFormFromProfile(p)
}

async function saveProfile() {
  if (!profile.value) return
  await store.updateProfile(profile.value.id, {
    maxScore: profileForm.maxScore,
    minScore: profileForm.minScore,
    precisionDigits: profileForm.precisionDigits,
  })
  dirty.value = false
}

// GradeBand handlers
async function handleCreateGradeBand(data: CreateGradeBandRequest) {
  if (!profile.value) return
  await store.createGradeBand(profile.value.id, data)
}
async function handleUpdateGradeBand(id: number, data: UpdateGradeBandRequest) {
  if (!profile.value) return
  await store.updateGradeBand(profile.value.id, id, data)
}
async function handleDeleteGradeBand(id: number) {
  if (!profile.value) return
  await store.deleteGradeBand(profile.value.id, id)
}

// Grade band preset handler
async function handleApplyPreset(bands: CreateGradeBandRequest[]) {
  if (!profile.value) return
  const existingIds = store.gradeBands.map(b => b.id)
  for (const id of existingIds) {
    await store.deleteGradeBand(profile.value.id, id)
  }
  for (const band of bands) {
    await store.createGradeBand(profile.value.id, band)
  }
}

// Rule handlers
async function handleCreateRule(data: CreateRuleRequest) {
  if (!profile.value) return
  await store.createRule(profile.value.id, data)
}
async function handleUpdateRule(id: number, data: UpdateRuleRequest) {
  if (!profile.value) return
  await store.updateRule(profile.value.id, id, data)
}
async function handleDeleteRule(id: number) {
  if (!profile.value) return
  await store.deleteRule(profile.value.id, id)
}

// Advanced settings handler (1.9-1.12)
async function handleSaveAdvancedSettings(data: UpdateAdvancedSettingsRequest) {
  if (!profile.value) return
  await store.updateAdvancedSettings(profile.value.id, data)
  profile.value = store.currentProfile
}

// Version handler (1.7)
async function handlePublishVersion(changeSummary: string) {
  if (!profile.value) return
  await store.publishVersion(profile.value.id, { changeSummary })
  profile.value = store.currentProfile
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

/* Card */
.sp-card {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: var(--insp-sp-4);
}

/* Section head */
.sp-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--insp-sp-3);
}
.sp-section-title {
  font-size: var(--insp-text-md);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-primary);
  margin: 0;
}

/* Form fields */
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

/* Health check */
.sp-health {
  padding: var(--insp-sp-3) var(--insp-sp-4);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.sp-health-title {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-1);
  font-size: var(--insp-text-md);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-primary);
  margin-bottom: var(--insp-sp-3);
}
.sp-health-icon { color: var(--insp-ink-tertiary); }

.sp-checks { display: flex; flex-direction: column; gap: var(--insp-sp-1); }
.sp-check {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-2);
  font-size: var(--insp-text-sm);
  padding: var(--insp-sp-1) var(--insp-sp-2);
  border-radius: var(--insp-radius-sm);
}
.sp-check.ok    { color: var(--insp-pass); background: var(--insp-pass-pale); }
.sp-check.warn  { color: var(--insp-warn); background: var(--insp-warn-pale); }
.sp-check.error { color: var(--insp-fail); background: var(--insp-fail-pale); }
</style>
