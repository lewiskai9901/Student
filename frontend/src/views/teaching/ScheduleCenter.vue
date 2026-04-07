<template>
  <div class="tm-page">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">排课中心</h1>
        <div class="tm-stats">
          <span v-if="readiness">
            <span class="dot" :class="readiness.plans?.entryCount > 0 ? 'dot-green' : 'dot-gray'" />
            {{ readiness.plans?.entryCount || 0 }} 条排课
          </span>
          <span class="sep" />
          <span>{{ currentSemesterName }}</span>
        </div>
      </div>
      <div style="display: flex; align-items: center; gap: 10px;">
        <select v-model="semesterId" class="tm-select" @change="onSemesterChange">
          <option :value="undefined" disabled>选择学期</option>
          <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.semesterName }}</option>
        </select>
        <button class="tm-btn tm-btn-primary" @click="setupVisible = true">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style="margin-right: 4px;"><path d="M7 1v12M1 7h12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
          排课设置
        </button>
      </div>
    </div>

    <!-- Daily Tabs -->
    <div class="tm-tabs">
      <button :class="['tm-tab', { active: dailyTab === 'timetable' }]" @click="dailyTab = 'timetable'">课表视图</button>
      <button :class="['tm-tab', { active: dailyTab === 'adjustment' }]" @click="dailyTab = 'adjustment'">调课管理</button>
      <button :class="['tm-tab', { active: dailyTab === 'export' }]" @click="dailyTab = 'export'">导出打印</button>
    </div>

    <!-- Daily Content -->
    <div style="flex: 1; overflow-y: auto; padding: 16px 24px;">
      <!-- No schedule hint -->
      <div v-if="readiness && readiness.plans?.entryCount === 0 && dailyTab === 'timetable'" class="empty-hint">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none"><rect x="6" y="10" width="36" height="32" rx="4" stroke="#d1d5db" stroke-width="2"/><path d="M6 18h36M16 6v8M32 6v8" stroke="#d1d5db" stroke-width="2" stroke-linecap="round"/><path d="M18 28h12M18 34h8" stroke="#9ca3af" stroke-width="2" stroke-linecap="round"/></svg>
        </div>
        <p class="empty-title">本学期尚未排课</p>
        <p class="empty-desc">点击右上角「排课设置」开始配置和排课</p>
        <button class="tm-btn tm-btn-primary" @click="setupVisible = true">开始排课</button>
      </div>

      <!-- Timetable -->
      <TimetableViewer v-else-if="dailyTab === 'timetable'" :semester-id="semesterId" :period-config="periodConfig" />

      <!-- Adjustment -->
      <AdjustmentPanel v-else-if="dailyTab === 'adjustment'" :semester-id="semesterId" />

      <!-- Export -->
      <DailyExport v-else-if="dailyTab === 'export'" :semester-id="semesterId" />
    </div>

    <!-- Setup Drawer (全屏) -->
    <Transition name="setup-drawer">
      <div v-if="setupVisible" class="setup-overlay">
        <div class="setup-panel">
          <!-- Setup Header -->
          <div class="setup-header">
            <h2 class="setup-title">排课设置</h2>
            <div class="tm-stats" style="margin-top: 2px;">
              <span>{{ currentSemesterName }}</span>
            </div>
            <button class="tm-drawer-close" style="position: absolute; right: 24px; top: 20px;" @click="setupVisible = false">&times;</button>
          </div>

          <div class="setup-body">
            <!-- Left: Step Nav -->
            <div class="step-nav">
              <div
                v-for="(step, idx) in steps"
                :key="idx"
                :class="['step-item', { active: currentStep === idx, completed: step.completed, locked: step.locked }]"
                @click="!step.locked && (currentStep = idx)"
              >
                <div class="step-number">
                  <template v-if="step.completed">&#10003;</template>
                  <template v-else>{{ idx + 1 }}</template>
                </div>
                <div class="step-info">
                  <div class="step-label">{{ step.label }}</div>
                  <div class="step-desc">{{ step.desc }}</div>
                </div>
              </div>
            </div>

            <!-- Right: Step Content -->
            <div class="step-content">
              <StepPeriodConfig
                v-if="currentStep === 0"
                :semester-id="semesterId"
                :config="periodConfig"
                @saved="onPeriodConfigSaved"
              />
              <StepDataReady
                v-else-if="currentStep === 1"
                :semester-id="semesterId"
                :readiness="readiness"
                @refresh="loadReadiness"
              />
              <StepConstraints
                v-else-if="currentStep === 2"
                :semester-id="semesterId"
              />
              <StepExecute
                v-else-if="currentStep === 3"
                :semester-id="semesterId"
                @scheduled="onScheduled"
              />
              <StepPublish
                v-else-if="currentStep === 4"
                :semester-id="semesterId"
                @published="onPublished"
              />

              <!-- Step Navigation Buttons -->
              <div class="step-footer">
                <button v-if="currentStep > 0" class="tm-btn tm-btn-secondary" @click="currentStep--">上一步</button>
                <span style="flex: 1;" />
                <button v-if="currentStep < 4" class="tm-btn tm-btn-primary" :disabled="steps[currentStep]?.locked" @click="currentStep++">
                  下一步
                </button>
                <button v-if="currentStep === 4" class="tm-btn tm-btn-primary" @click="setupVisible = false">完成</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useSharedDataStore } from '@/stores/sharedData'
import { scheduleConfigApi } from '@/api/teaching'

import TimetableViewer from './schedule/TimetableViewer.vue'
import AdjustmentPanel from './schedule/AdjustmentPanel.vue'
import DailyExport from './schedule/DailyExport.vue'
import StepPeriodConfig from './schedule/setup/StepPeriodConfig.vue'
import StepDataReady from './schedule/setup/StepDataReady.vue'
import StepConstraints from './schedule/setup/StepConstraints.vue'
import StepExecute from './schedule/setup/StepExecute.vue'
import StepPublish from './schedule/setup/StepPublish.vue'

const sharedData = useSharedDataStore()

// State
const semesters = ref<any[]>([])
const semesterId = ref<number | string>()
const dailyTab = ref<'timetable' | 'adjustment' | 'export'>('timetable')
const setupVisible = ref(false)
const currentStep = ref(0)
const periodConfig = ref<any>(null)
const readiness = ref<any>(null)

const currentSemesterName = computed(() => {
  const s = semesters.value.find(s => s.id === semesterId.value)
  return s?.semesterName || '未选择学期'
})

const steps = computed(() => [
  { label: '节次时间', desc: '配置每天几节课', completed: !!periodConfig.value, locked: false },
  { label: '数据准备', desc: '检查教学任务等', completed: readiness.value?.tasks?.status === 'ready', locked: false },
  { label: '约束规则', desc: '可选但建议配置', completed: readiness.value?.constraints?.count > 0, locked: false },
  { label: '执行排课', desc: '智能排课', completed: readiness.value?.plans?.entryCount > 0, locked: !readiness.value?.tasks?.count },
  { label: '检查发布', desc: '确认并发布', completed: false, locked: !readiness.value?.plans?.entryCount },
])

// Data loading
async function loadSemesters() {
  semesters.value = await sharedData.getSemesters()
  const current = await sharedData.getCurrentSemester()
  if (current) semesterId.value = current.id
}

async function loadPeriodConfig() {
  if (!semesterId.value) return
  try {
    const res = await scheduleConfigApi.get(semesterId.value)
    periodConfig.value = (res as any).data || res
  } catch { periodConfig.value = null }
}

async function loadReadiness() {
  if (!semesterId.value) return
  try {
    const res = await scheduleConfigApi.checkReadiness(semesterId.value)
    readiness.value = (res as any).data || res
  } catch { readiness.value = null }
}

function onSemesterChange() {
  loadPeriodConfig()
  loadReadiness()
}

function onPeriodConfigSaved(config: any) {
  periodConfig.value = config
}

function onScheduled() {
  loadReadiness()
}

function onPublished() {
  loadReadiness()
  setupVisible.value = false
}

watch(semesterId, () => {
  if (semesterId.value) { loadPeriodConfig(); loadReadiness() }
})

onMounted(async () => {
  await loadSemesters()
})
</script>

<style scoped>
/* Empty hint */
.empty-hint { text-align: center; padding: 80px 0; }
.empty-icon { margin-bottom: 16px; }
.empty-title { font-size: 16px; font-weight: 600; color: #374151; margin: 0 0 6px; }
.empty-desc { font-size: 13px; color: #9ca3af; margin: 0 0 20px; }

/* Setup Drawer */
.setup-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(2px);
  display: flex; justify-content: center; align-items: center;
}
.setup-panel {
  width: 95vw; max-width: 1200px; height: 90vh;
  background: #fff; border-radius: 16px;
  display: flex; flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}
.setup-header {
  position: relative;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #e8eaed;
}
.setup-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 18px; font-weight: 700; color: #111827; margin: 0;
}
.setup-body {
  flex: 1; display: flex; overflow: hidden;
}

/* Step Nav */
.step-nav {
  width: 220px; flex-shrink: 0;
  border-right: 1px solid #e8eaed;
  padding: 20px 0;
  overflow-y: auto;
}
.step-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 20px; cursor: pointer;
  transition: all 0.15s;
}
.step-item:hover:not(.locked) { background: #f9fafb; }
.step-item.active { background: #eff6ff; border-right: 3px solid #2563eb; }
.step-item.locked { opacity: 0.4; cursor: not-allowed; }

.step-number {
  width: 28px; height: 28px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 12px; font-weight: 600;
  background: #f3f4f6; color: #6b7280;
  transition: all 0.15s;
}
.step-item.active .step-number { background: #2563eb; color: #fff; }
.step-item.completed .step-number { background: #10b981; color: #fff; }

.step-info { min-width: 0; }
.step-label { font-size: 13px; font-weight: 600; color: #111827; }
.step-desc { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.step-item.active .step-label { color: #2563eb; }

/* Step Content */
.step-content {
  flex: 1; display: flex; flex-direction: column;
  overflow-y: auto; padding: 24px;
}
.step-footer {
  display: flex; align-items: center; gap: 10px;
  padding-top: 16px; margin-top: auto;
  border-top: 1px solid #f3f4f6;
}

/* Transitions */
.setup-drawer-enter-active { transition: opacity 0.25s ease; }
.setup-drawer-enter-active .setup-panel { transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease; }
.setup-drawer-leave-active { transition: opacity 0.2s ease; }
.setup-drawer-leave-active .setup-panel { transition: transform 0.2s ease, opacity 0.2s ease; }
.setup-drawer-enter-from { opacity: 0; }
.setup-drawer-enter-from .setup-panel { transform: scale(0.95) translateY(20px); opacity: 0; }
.setup-drawer-leave-to { opacity: 0; }
.setup-drawer-leave-to .setup-panel { transform: scale(0.95) translateY(20px); opacity: 0; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
