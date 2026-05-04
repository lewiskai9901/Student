<template>
  <div class="wb">
    <!-- Top Bar -->
    <div class="wb-topbar">
      <div class="wb-topbar-left">
        <h1 class="wb-title">教务工作台</h1>
        <span class="wb-semester-name">{{ currentSemesterName }}</span>
      </div>
      <div class="wb-topbar-right">
        <select v-model="semesterId" class="tm-select" @change="onSemesterChange">
          <option :value="undefined" disabled>选择学期</option>
          <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.semesterName }}</option>
        </select>
      </div>
    </div>

    <!-- Step Navigator -->
    <div class="wb-steps">
      <button
        v-for="(step, idx) in steps"
        :key="idx"
        type="button"
        :class="['wb-step', { active: currentStep === idx, completed: step.done, disabled: !semesterId }]"
        :disabled="!semesterId"
        @click="currentStep = idx"
      >
        <span class="wb-step-num">
          <template v-if="step.done">&#10003;</template>
          <template v-else>{{ idx + 1 }}</template>
        </span>
        <span class="wb-step-label">{{ step.label }}</span>
      </button>
    </div>

    <!-- Body: optional tree + content -->
    <div class="wb-body">
      <!-- Left Tree (only for steps that need org filtering) -->
      <div v-if="showTree" class="wb-tree">
        <ScheduleTree :mode="treeMode" :semester-id="semesterId" @select="onTreeSelect" />
      </div>

      <!-- Content Area -->
      <div class="wb-content">
        <!-- Step 0: 学期准备 -->
        <template v-if="currentStep === 0">
          <StepSemester :semester-id="semesterId" />
        </template>

        <!-- Step 1: 开课计划 -->
        <template v-else-if="currentStep === 1">
          <StepOffering :semester-id="semesterId" :selected-org="selectedOrg" />
        </template>

        <!-- Step 2: 教学任务 -->
        <template v-else-if="currentStep === 2">
          <StepTask :semester-id="semesterId" :selected-org="selectedOrg" />
        </template>

        <!-- Step 3: 排课约束 -->
        <template v-else-if="currentStep === 3">
          <StepConstraint :semester-id="semesterId" />
        </template>

        <!-- Step 4: 智能排课 -->
        <template v-else-if="currentStep === 4">
          <StepSchedule :semester-id="semesterId" :total-weeks="totalWeeks" @scheduled="onScheduled" />
        </template>

        <!-- Step 5: 课表查看 -->
        <template v-else-if="currentStep === 5">
          <StepView :semester-id="semesterId" />
        </template>

        <!-- Step 6: 调课管理 -->
        <template v-else-if="currentStep === 6">
          <StepAdjustment :semester-id="semesterId" />
        </template>
      </div>
    </div>

    <!-- Footer Stats -->
    <div class="wb-footer">
      <span>开课 <b>{{ stats.offerings }}</b> 门</span>
      <span class="wb-footer-sep" />
      <span>任务 <b>{{ stats.tasks }}</b> 条 (已分配 <b>{{ stats.assigned }}</b>)</span>
      <span class="wb-footer-sep" />
      <span>排课 <b>{{ stats.entries }}</b> 条</span>
      <span class="wb-footer-sep" />
      <span>实况 <b>{{ stats.live }}</b> 条</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { semesterApi } from '@/api/calendar'
import { http } from '@/utils/request'

import ScheduleTree from '@/components/teaching/ScheduleTree.vue'
import StepSemester from './workbench/StepSemester.vue'
import StepOffering from './workbench/StepOffering.vue'
import StepTask from './workbench/StepTask.vue'
import StepConstraint from './workbench/StepConstraint.vue'
import StepSchedule from './workbench/StepSchedule.vue'
import StepView from './workbench/StepView.vue'
import StepAdjustment from './workbench/StepAdjustment.vue'

// ==================== State ====================

const semesters = ref<any[]>([])
const semesterId = ref<number | string>()
const currentStep = ref(0)
const selectedOrg = ref<any>(undefined)
const treeMode = ref<'class' | 'teacher' | 'classroom'>('class')
const totalWeeks = ref(22)

const stats = ref({ offerings: 0, tasks: 0, assigned: 0, entries: 0, live: 0, constraints: 0 })

// ==================== Computed ====================

const currentSemesterName = computed(() => {
  const s = semesters.value.find(s => s.id === semesterId.value)
  return s?.semesterName || ''
})

/** Steps that need left tree: 开课(1), 任务(2) */
const showTree = computed(() => [1, 2].includes(currentStep.value))

const steps = computed(() => [
  { label: '学期准备', done: stats.value.tasks > 0 },
  { label: '开课计划', done: stats.value.offerings > 0 },
  { label: '教学任务', done: stats.value.assigned > 0 },
  { label: '排课约束', done: stats.value.constraints > 0 },
  { label: '智能排课', done: stats.value.entries > 0 },
  { label: '课表查看', done: stats.value.entries > 0 },
  { label: '调课管理', done: false },
])

// ==================== Data Loading ====================

async function loadSemesters() {
  try {
    const res = await semesterApi.list()
    semesters.value = Array.isArray(res) ? res : (res as any).data || []
    const current = semesters.value.find((s: any) => s.isCurrent)
    if (current) semesterId.value = current.id
    else if (semesters.value.length > 0) semesterId.value = semesters.value[0].id
  } catch { semesters.value = [] }
}

async function loadStats() {
  if (!semesterId.value) return
  try {
    const [wfRes, rdRes, cstRes] = await Promise.allSettled([
      http.get('/teaching/workflow/stats', { params: { semesterId: semesterId.value } }),
      http.get('/teaching/schedule-readiness', { params: { semesterId: semesterId.value } }),
      http.get('/teaching/constraints', { params: { semesterId: semesterId.value, level: 1 } }),
    ])
    const wf = wfRes.status === 'fulfilled' ? ((wfRes.value as any).data || wfRes.value) : {}
    const rd = rdRes.status === 'fulfilled' ? ((rdRes.value as any).data || rdRes.value) : {}
    const cstData = cstRes.status === 'fulfilled' ? ((cstRes.value as any).data || cstRes.value) : []
    // 教学周数从后端 workflow/stats 获取（已排除考试/假期周）
    if (wf.teachingWeeks) totalWeeks.value = wf.teachingWeeks
    stats.value = {
      offerings: wf.offerings?.total ?? 0,
      tasks: wf.tasks?.total ?? 0,
      assigned: wf.tasks?.teacherAssigned ?? 0,
      entries: rd.plans?.entryCount ?? 0,
      live: rd.instances?.count ?? 0,
      constraints: Array.isArray(cstData) ? cstData.length : 0,
    }
  } catch {
    stats.value = { offerings: 0, tasks: 0, assigned: 0, entries: 0, live: 0, constraints: 0 }
  }
}

// ==================== Event Handlers ====================

function onSemesterChange() {
  selectedOrg.value = undefined
  loadStats()
}

function onTreeSelect(node: any) {
  selectedOrg.value = node?.id ? node : undefined
}

function onScheduled() {
  loadStats()
}

// ==================== Lifecycle ====================

watch(semesterId, () => { if (semesterId.value) loadStats() })

watch(currentStep, () => {
  // Reset tree selection when switching steps
  selectedOrg.value = undefined
  // Refresh stats when switching steps (e.g. after scheduling)
  loadStats()
})

onMounted(async () => {
  await loadSemesters()
})
</script>

<style scoped>
.wb {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fb;
  font-family: 'DM Sans', sans-serif;
}

/* Top Bar */
.wb-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 12px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.wb-topbar-left { display: flex; align-items: baseline; gap: 12px; }
.wb-topbar-right { display: flex; align-items: center; gap: 10px; }
.wb-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}
.wb-semester-name {
  font-size: 13px;
  color: #6b7280;
}

/* Step Navigator */
.wb-steps {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  overflow-x: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.wb-steps::-webkit-scrollbar { display: none; }

.wb-step {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.15s;
  border: none;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  position: relative;
  background: none;
  font-family: inherit;
  outline: none;
}
.wb-step:hover:not(.disabled) { background: #f9fafb; }
.wb-step.active { border-bottom-color: #2563eb; }
.wb-step.disabled { opacity: 0.4; cursor: not-allowed; }

/* Connector line between steps */
.wb-step:not(:last-child)::after {
  content: '';
  position: absolute;
  right: -1px;
  top: 50%;
  width: 10px;
  height: 1px;
  background: #d1d5db;
  pointer-events: none;
}

.wb-step-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 600;
  background: #f3f4f6;
  color: #6b7280;
  flex-shrink: 0;
  transition: all 0.15s;
}
.wb-step.active .wb-step-num { background: #2563eb; color: #fff; }
.wb-step.completed .wb-step-num { background: #10b981; color: #fff; }

.wb-step-label {
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
}
.wb-step.active .wb-step-label { color: #2563eb; font-weight: 600; }
.wb-step.completed .wb-step-label { color: #374151; }

/* Body */
.wb-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0; /* 确保flex子元素能滚动 */
}

.wb-tree {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid #e8eaed;
  background: #fff;
  overflow-y: auto;
  min-height: 0;
}
/* 树滚动条样式 */
.wb-tree::-webkit-scrollbar { width: 4px; }
.wb-tree::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 2px; }
.wb-tree::-webkit-scrollbar-thumb:hover { background: #9ca3af; }

.wb-content {
  flex: 1;
  overflow-y: auto;
  background: #f8f9fb;
}

/* Footer */
.wb-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 24px;
  background: #fff;
  border-top: 1px solid #e8eaed;
  font-size: 12px;
  color: #6b7280;
}
.wb-footer b { font-weight: 600; color: #374151; }
.wb-footer-sep {
  display: block;
  width: 1px;
  height: 12px;
  background: #d1d5db;
}
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
