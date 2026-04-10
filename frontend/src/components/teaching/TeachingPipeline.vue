<template>
  <div v-if="stats" class="pipeline">
    <template v-for="(step, idx) in steps" :key="step.key">
      <div class="step" @click="router.push({ path: step.path, query: step.query })">
        <div :class="['circle', circleClass(step.key)]" />
        <span class="step-label">{{ step.label }}</span>
        <span class="step-count">{{ countText(step.key) }}</span>
      </div>
      <div v-if="idx < steps.length - 1" :class="['line', lineClass(idx)]" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/utils/request'

const props = defineProps<{ semesterId: number | string | undefined }>()
const router = useRouter()

interface WorkflowStats {
  offerings: { total: number; confirmed: number }
  assignments: { total: number; confirmed: number }
  tasks: { total: number; teacherAssigned: number; scheduled: number }
  currentStep: string
}

const stats = ref<WorkflowStats | null>(null)

const steps = [
  { key: 'offering', label: '开课计划', path: '/teaching/offerings', query: { tab: 'offerings' } },
  { key: 'fulfillment', label: '任务落实', path: '/teaching/offerings', query: { tab: 'fulfillment' } },
  { key: 'schedule', label: '排课', path: '/teaching/scheduling', query: {} },
]

const isComplete = (key: string): boolean => {
  if (!stats.value) return false
  const s = stats.value
  switch (key) {
    case 'offering': return s.offerings.confirmed >= s.offerings.total && s.offerings.total > 0
    case 'fulfillment': return s.tasks.total > 0 && s.tasks.teacherAssigned >= s.tasks.total
    case 'schedule': return s.tasks.total > 0 && s.tasks.scheduled >= s.tasks.total
    default: return false
  }
}

const currentStepIndex = (): number => {
  for (let i = 0; i < steps.length; i++) {
    if (!isComplete(steps[i].key)) return i
  }
  return steps.length - 1
}

const circleClass = (key: string): string => {
  const idx = steps.findIndex(s => s.key === key)
  const cur = currentStepIndex()
  if (isComplete(key)) return 'circle-done'
  if (idx === cur) return 'circle-current'
  return 'circle-future'
}

const lineClass = (idx: number): string => {
  return isComplete(steps[idx].key) ? 'line-done' : 'line-future'
}

const countText = (key: string): string => {
  if (!stats.value) return ''
  const s = stats.value
  switch (key) {
    case 'offering': return `${s.offerings.confirmed}/${s.offerings.total}`
    case 'fulfillment': {
      if (s.tasks.total === 0) return `${s.assignments.total}个分配`
      return `${s.tasks.teacherAssigned}/${s.tasks.total} 已分配`
    }
    case 'schedule': return `${s.tasks.scheduled}/${s.tasks.total}`
    default: return ''
  }
}

const loadStats = async () => {
  if (!props.semesterId) { stats.value = null; return }
  try {
    const res: any = await http.get('/teaching/workflow/stats', { params: { semesterId: props.semesterId } })
    stats.value = res
  } catch { stats.value = null }
}

watch(() => props.semesterId, loadStats)
onMounted(loadStats)
</script>

<style scoped>
.pipeline { display: flex; align-items: center; padding: 12px 20px; margin-bottom: 4px; }
.step { display: flex; flex-direction: column; align-items: center; cursor: pointer; min-width: 90px; }
.step:hover .circle { transform: scale(1.15); }
.circle { width: 22px; height: 22px; border-radius: 50%; transition: all 0.2s; }
.circle-done { background: #22c55e; }
.circle-current { background: #3b82f6; animation: pulse 1.5s infinite; }
.circle-future { background: #d1d5db; }
.step-label { font-size: 12px; color: #374151; margin-top: 5px; font-weight: 600; }
.step-count { font-size: 11px; color: #9ca3af; margin-top: 1px; }
.line { flex: 1; height: 2px; margin: 0 6px; margin-bottom: 28px; }
.line-done { background: #22c55e; }
.line-future { background: #e5e7eb; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
