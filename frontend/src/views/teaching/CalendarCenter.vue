<template>
  <div class="tm-page">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">校历管理</h1>
        <div class="tm-stats">
          <span v-if="currentWeek">
            <span class="dot dot-green" /> 第{{ currentWeek.weekNumber }}教学周
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
          学年设置
        </button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tm-tabs">
      <button :class="['tm-tab', { active: tab === 'overview' }]" @click="tab = 'overview'">学期总览</button>
      <button :class="['tm-tab', { active: tab === 'grid' }]" @click="tab = 'grid'">校历表</button>
      <button :class="['tm-tab', { active: tab === 'events' }]" @click="tab = 'events'">校历事件</button>
      <button :class="['tm-tab', { active: tab === 'periods' }]" @click="tab = 'periods'">作息表</button>
    </div>

    <!-- Content -->
    <div style="flex: 1; overflow-y: auto; padding: 16px 24px;">
      <SemesterOverview v-if="tab === 'overview'" :semester-id="semesterId" :weeks="weeks" :events="events" :current-week="currentWeek" :calendar-grid="calendarGrid" @refresh="loadWeeks(); loadCalendarGrid()" />
      <CalendarGrid v-else-if="tab === 'grid'" :semester-id="semesterId" :year-id="yearId" @loaded="onGridLoaded" />
      <CalendarEvents v-else-if="tab === 'events'" :semester-id="semesterId" :year-id="yearId" :events="events" @refresh="loadEvents" />
      <PeriodSettings v-else-if="tab === 'periods'" :semester-id="semesterId" />
    </div>

    <!-- Year Setup Drawer -->
    <Transition name="setup-drawer">
      <div v-if="setupVisible" class="setup-overlay" @click.self="setupVisible = false">
        <div class="setup-panel" style="width: 640px; max-width: 90vw;">
          <div style="display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid #e8eaed;">
            <h2 style="font-size: 18px; font-weight: 700; color: #111827; margin: 0;">学年与学期设置</h2>
            <button class="tm-drawer-close" @click="setupVisible = false">&times;</button>
          </div>
          <div style="flex: 1; overflow-y: auto; padding: 24px;">
            <YearSetupDrawer @semester-changed="onSetupChanged" />
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { semesterApi, academicEventApi } from '@/api/calendar'
import type { CalendarGrid as CalendarGridType } from '@/types/teaching'

import SemesterOverview from './calendar-new/SemesterOverview.vue'
import CalendarGrid from './calendar-new/CalendarGrid.vue'
import CalendarEvents from './calendar-new/CalendarEvents.vue'
import PeriodSettings from './calendar-new/PeriodSettings.vue'
import YearSetupDrawer from './calendar-new/YearSetupDrawer.vue'

const semesters = ref<any[]>([])
const semesterId = ref<number | string>()
const yearId = ref<number | string>()
const tab = ref<'overview' | 'grid' | 'events' | 'periods'>('overview')
const calendarGrid = ref<CalendarGridType | null>(null)

function onGridLoaded(g: CalendarGridType) {
  calendarGrid.value = g
}
const setupVisible = ref(false)
const weeks = ref<any[]>([])
const events = ref<any[]>([])

const currentSemesterName = computed(() => semesters.value.find(s => s.id === semesterId.value)?.semesterName || '')
const currentWeek = computed(() => weeks.value.find(w => w.isCurrent))

async function loadSemesters() {
  try {
    const res = await semesterApi.list()
    semesters.value = Array.isArray(res) ? res : (res as any).data || []
    const current = semesters.value.find((s: any) => s.isCurrent)
    if (current) { semesterId.value = current.id; yearId.value = current.academicYearId || current.yearId }
    else if (semesters.value.length > 0) { semesterId.value = semesters.value[0].id }
  } catch { semesters.value = [] }
}

async function loadWeeks() {
  if (!semesterId.value) { weeks.value = []; return }
  try {
    const res = await semesterApi.getWeeks(semesterId.value)
    weeks.value = Array.isArray(res) ? res : (res as any).data || []
  } catch { weeks.value = [] }
}

async function loadEvents() {
  if (!semesterId.value) { events.value = []; return }
  try {
    const res = await academicEventApi.list({ semesterId: semesterId.value })
    events.value = Array.isArray(res) ? res : (res as any).data || []
  } catch { events.value = [] }
}

async function loadCalendarGrid() {
  if (!semesterId.value) { calendarGrid.value = null; return }
  try {
    const res = await semesterApi.getCalendarGrid(semesterId.value)
    calendarGrid.value = res
  } catch { calendarGrid.value = null }
}

function onSemesterChange() { loadWeeks(); loadEvents(); loadCalendarGrid() }
function onSetupChanged() { loadSemesters() }

watch(semesterId, () => { if (semesterId.value) { loadWeeks(); loadEvents(); loadCalendarGrid() } })

onMounted(async () => { await loadSemesters() })
</script>

<style scoped>
.setup-overlay { position: fixed; inset: 0; z-index: 2000; background: rgba(0,0,0,0.4); backdrop-filter: blur(2px); display: flex; justify-content: flex-end; }
.setup-panel { height: 100%; background: #fff; display: flex; flex-direction: column; box-shadow: -8px 0 32px rgba(0,0,0,0.12); }
.setup-drawer-enter-active { transition: opacity 0.2s; }
.setup-drawer-enter-active .setup-panel { transition: transform 0.25s cubic-bezier(0.16,1,0.3,1); }
.setup-drawer-leave-active { transition: opacity 0.15s; }
.setup-drawer-leave-active .setup-panel { transition: transform 0.2s; }
.setup-drawer-enter-from { opacity: 0; }
.setup-drawer-enter-from .setup-panel { transform: translateX(100%); }
.setup-drawer-leave-to { opacity: 0; }
.setup-drawer-leave-to .setup-panel { transform: translateX(100%); }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
