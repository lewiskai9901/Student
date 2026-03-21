<template>
  <div class="h-full flex flex-col" style="background:#f4f6f9;">
    <!-- Top bar -->
    <div class="sp-topbar">
      <div class="flex items-center gap-3">
        <button class="sp-ic" @click="goBack">
          <ArrowLeft class="w-4 h-4" />
        </button>
        <h1 class="text-base font-semibold" style="color:#1e2a3a;">汇总规则</h1>
        <span v-if="profile" style="font-size:12px; color:#8c95a3;">分区 #{{ profile.sectionId }}</span>
      </div>
      <div class="flex items-center gap-2">
        <button
          v-if="profile && dirty"
          class="sp-btn-primary"
          @click="saveProfile"
        >
          保存配置
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <div style="color:#8c95a3; font-size:13px;">加载中...</div>
    </div>

    <!-- No profile yet (fallback, normally auto-created) -->
    <div v-else-if="!profile" class="flex-1 flex items-center justify-center">
      <div class="text-center">
        <div style="color:#8c95a3; font-size:13px; margin-bottom:12px;">初始化配置失败</div>
        <button class="sp-btn-primary" @click="initProfile">重试</button>
      </div>
    </div>

    <!-- Main 2-column layout -->
    <div v-else class="flex-1 overflow-hidden flex">
      <!-- LEFT: Scrollable config column -->
      <div class="sp-left">
        <!-- Inline settings -->
        <div class="sp-card">
          <div class="sp-section-head">
            <h3 class="sp-section-title">基础设置</h3>
          </div>
          <div class="grid grid-cols-3 gap-4">
            <div class="sp-fld">
              <label title="分数的绝对上限">最高分</label>
              <input v-model.number="profileForm.maxScore" type="number" @input="dirty = true" />
            </div>
            <div class="sp-fld">
              <label title="分数的绝对下限">最低分</label>
              <input v-model.number="profileForm.minScore" type="number" @input="dirty = true" />
            </div>
            <div class="sp-fld">
              <label title="最终分数保留的小数位数">精度</label>
              <input v-model.number="profileForm.precisionDigits" type="number" min="0" max="4" @input="dirty = true" />
            </div>
          </div>
        </div>

        <!-- Dimensions -->
        <div class="sp-card">
          <DimensionTable
            :dimensions="store.dimensions"
          />
        </div>

        <!-- Grade Bands -->
        <div class="sp-card">
          <GradeBandEditor
            :grade-bands="store.gradeBands"
            @create="handleCreateGradeBand"
            @update="handleUpdateGradeBand"
            @delete="handleDeleteGradeBand"
            @apply-preset="handleApplyPreset"
          />
        </div>

        <!-- Calculation Rules -->
        <div class="sp-card">
          <CalcRuleChain
            :rules="store.rules"
            @create="handleCreateRule"
            @update="handleUpdateRule"
            @delete="handleDeleteRule"
          />
        </div>

        <!-- Advanced Settings (1.9-1.12) -->
        <div class="sp-card">
          <AdvancedScoringSettings
            v-if="profile"
            :profile="profile"
            @save="handleSaveAdvancedSettings"
          />
        </div>
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
import { useInspScoringStore } from '@/stores/insp/inspScoringStore'
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
/* ---- Buttons ---- */
.sp-btn-primary { display:inline-flex; align-items:center; gap:5px; padding:8px 16px; background:#1a6dff; color:#fff; border:none; border-radius:8px; font-size:13px; font-weight:500; cursor:pointer; transition:background 0.15s; white-space:nowrap; }
.sp-btn-primary:hover { background:#1558d6; }
.sp-ic { background:none; border:none; padding:5px; color:#8c95a3; cursor:pointer; border-radius:6px; display:flex; align-items:center; transition:all 0.12s; }
.sp-ic:hover { color:#1a6dff; background:#f0f4ff; }

/* ---- Form fields ---- */
.sp-fld label { display:block; font-size:12px; font-weight:500; color:#5a6474; margin-bottom:5px; }
.sp-fld input, .sp-fld select, .sp-fld textarea { width:100%; border:1px solid #dce1e8; border-radius:8px; padding:8px 12px; font-size:13px; outline:none; transition:border-color 0.2s, box-shadow 0.2s; color:#1e2a3a; background:#fff; }
.sp-fld input::placeholder, .sp-fld textarea::placeholder { color:#b8c0cc; }
.sp-fld input:focus, .sp-fld select:focus, .sp-fld textarea:focus { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }

/* ---- Top bar ---- */
.sp-topbar { background:#fff; border-bottom:1px solid #e8ecf1; padding:12px 20px; display:flex; align-items:center; justify-content:space-between; flex-shrink:0; }

/* ---- 2-column layout ---- */
.sp-left {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.sp-right {
  width: 380px;
  flex-shrink: 0;
  border-left: 1px solid #e8ecf1;
  background: #fff;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* ---- Card ---- */
.sp-card { background:#fff; border-radius:14px; box-shadow:0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.02); padding:20px; }

/* ---- Section head ---- */
.sp-section-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; }
.sp-section-title { font-size:13px; font-weight:600; color:#1e2a3a; margin:0; }

/* ---- Health check ---- */
.sp-health { padding:16px; border-bottom:1px solid #eef0f3; }
.sp-health-title { display:flex; align-items:center; gap:6px; font-size:13px; font-weight:600; color:#1e2a3a; margin-bottom:12px; }
.sp-health-icon { color:#8c95a3; }

.sp-checks { display:flex; flex-direction:column; gap:6px; }
.sp-check { display:flex; align-items:center; gap:8px; font-size:12px; padding:5px 8px; border-radius:8px; }
.sp-check.ok { color:#059669; background:#f0fdf4; }
.sp-check.warn { color:#d97706; background:#fffbeb; }
.sp-check.error { color:#dc2626; background:#fef2f2; }
</style>
