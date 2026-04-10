<template>
  <div>
    <!-- Week Timeline -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 20px; margin-bottom: 16px;">
      <h3 style="font-size: 14px; font-weight: 600; color: #111827; margin: 0 0 12px;">教学周进度</h3>
      <div style="display: flex; gap: 3px; margin-bottom: 8px;">
        <div
          v-for="w in gridWeeks"
          :key="w.weekNumber"
          :title="getWeekTitle(w)"
          class="wk-block"
          :style="getWeekBlockStyle(w)"
        >{{ w.weekNumber }}</div>
      </div>
      <div class="tm-stats" style="margin-top: 0;">
        <span>教学 <b>{{ teachingWeekCount }}</b> 周</span>
        <span v-if="examWeekCount" class="sep" />
        <span v-if="examWeekCount" style="color: #7c3aed;">考试 <b>{{ examWeekCount }}</b> 周</span>
        <span v-if="vacationWeekCount" class="sep" />
        <span v-if="vacationWeekCount" style="color: #dc2626;">假期 <b>{{ vacationWeekCount }}</b> 周</span>
        <span class="sep" />
        <span v-if="semesterStatus === 'ongoing' && currentWeekNum > 0">当前 <b>第{{ currentWeekNum }}周</b></span>
        <span v-else-if="semesterStatus === 'ended'" style="color: #6b7280;">已结束</span>
        <span v-else>未开学</span>
        <span class="sep" />
        <span>已过 <b>{{ passedWeeks }}</b> 周</span>
        <span class="sep" />
        <span>剩余 <b>{{ remainingWeeks }}</b> 周</span>
      </div>
    </div>

    <!-- Stats Cards -->
    <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px;">
      <div class="ov-card">
        <div class="ov-num">{{ teachingWeekCount }}</div>
        <div class="ov-label">教学周</div>
      </div>
      <div class="ov-card">
        <div class="ov-num" style="color: #dc2626;">{{ holidayDays }}</div>
        <div class="ov-label">放假天数</div>
      </div>
      <div class="ov-card">
        <div class="ov-num" style="color: #7c3aed;">{{ calendarGrid?.totalExamDays || 0 }}</div>
        <div class="ov-label">考试天数</div>
      </div>
      <div class="ov-card">
        <div class="ov-num" style="color: #ea580c;">{{ makeupDays }}</div>
        <div class="ov-label">补课天数</div>
      </div>
    </div>

    <!-- This Week Info -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 20px;">
      <h3 style="font-size: 14px; font-weight: 600; color: #111827; margin: 0 0 12px;">
        <template v-if="currentGridWeek">
          第{{ currentGridWeek.weekNumber }}周
          <span style="font-weight: 400; font-size: 12px; color: #6b7280; margin-left: 8px;">{{ currentGridWeek.startDate }} ~ {{ currentGridWeek.endDate }}</span>
          <span v-if="currentGridWeek.weekType === 'EXAM'" class="tm-chip tm-chip-purple" style="margin-left: 8px;">考试周</span>
          <span v-if="currentGridWeek.weekType === 'VACATION'" class="tm-chip tm-chip-red" style="margin-left: 8px;">假期</span>
        </template>
        <template v-else>本周</template>
      </h3>
      <div v-if="thisWeekEvents.length === 0" style="color: #9ca3af; font-size: 13px; padding: 16px 0;">
        本周暂无特殊事件
      </div>
      <div v-for="evt in thisWeekEvents" :key="evt.id" style="display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid #f3f4f6;">
        <span :class="['tm-chip', getEventChip(evt.eventType)]">{{ getEventTypeName(evt.eventType) }}</span>
        <span style="font-size: 13px; font-weight: 500; color: #111827;">{{ evt.eventName }}</span>
        <span style="font-size: 12px; color: #6b7280;">{{ evt.startDate }}{{ evt.endDate && evt.endDate !== evt.startDate ? ' ~ ' + evt.endDate : '' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  semesterId: number | string | undefined
  weeks: any[]
  events: any[]
  currentWeek: any
  calendarGrid: any
}>()

const today = new Date().toISOString().slice(0, 10)

const gridWeeks = computed(() => props.calendarGrid?.weeks || [])

const semesterStatus = computed<'before' | 'ongoing' | 'ended'>(() => {
  if (!gridWeeks.value.length) return 'before'
  const first = gridWeeks.value[0]
  const last = gridWeeks.value[gridWeeks.value.length - 1]
  if (today < first.startDate) return 'before'
  if (today > last.endDate) return 'ended'
  return 'ongoing'
})

const currentWeekNum = computed(() => {
  if (semesterStatus.value !== 'ongoing') return 0
  const w = gridWeeks.value.find((w: any) => today >= w.startDate && today <= w.endDate)
  return w ? w.weekNumber : 0
})

const currentGridWeek = computed(() => {
  return gridWeeks.value.find((w: any) => w.weekNumber === currentWeekNum.value) || null
})

const teachingWeekCount = computed(() =>
  gridWeeks.value.filter((w: any) => w.weekType === 'TEACHING').length || props.weeks.length
)

const examWeekCount = computed(() =>
  gridWeeks.value.filter((w: any) => w.weekType === 'EXAM').length
)

const vacationWeekCount = computed(() =>
  gridWeeks.value.filter((w: any) => w.weekType === 'VACATION').length
)

const passedWeeks = computed(() => {
  const total = gridWeeks.value.length || props.weeks.length
  if (semesterStatus.value === 'ended') return total
  if (semesterStatus.value === 'before') return 0
  return currentWeekNum.value > 0 ? currentWeekNum.value - 1 : 0
})

const remainingWeeks = computed(() => {
  const total = gridWeeks.value.length || props.weeks.length
  if (semesterStatus.value === 'ended') return 0
  if (semesterStatus.value === 'before') return total
  return currentWeekNum.value > 0 ? total - currentWeekNum.value : total
})

const holidayDays = computed(() => {
  if (props.calendarGrid) return props.calendarGrid.totalHolidayDays
  return props.events.filter((e: any) => e.affectType === 1 || e.eventType === 1).length
})

const makeupDays = computed(() => {
  if (props.calendarGrid) return props.calendarGrid.totalMakeupDays
  return props.events.filter((e: any) => e.affectType === 3).length
})

const thisWeekEvents = computed(() => {
  if (!currentGridWeek.value) return props.events.slice(0, 5)
  const start = currentGridWeek.value.startDate
  const end = currentGridWeek.value.endDate
  return props.events.filter((e: any) => e.startDate <= end && (e.endDate || e.startDate) >= start)
})

function getWeekTitle(w: any) {
  const typeLabel: Record<string, string> = { TEACHING: '教学周', EXAM: '考试周', VACATION: '假期' }
  return `第${w.weekNumber}周 (${typeLabel[w.weekType] || ''}): ${w.startDate} ~ ${w.endDate}`
}

function getWeekBlockStyle(w: any) {
  const isCurrent = w.weekNumber === currentWeekNum.value
  const isPast = currentWeekNum.value > 0 && w.weekNumber < currentWeekNum.value

  if (isCurrent) {
    return { background: '#2563eb', color: '#fff', fontWeight: '700', outline: '2px solid #2563eb', outlineOffset: '1px' }
  }

  const typeColors: Record<string, { past: string; pastText: string; future: string; futureText: string }> = {
    TEACHING: { past: '#dbeafe', pastText: '#3b82f6', future: '#f3f4f6', futureText: '#9ca3af' },
    EXAM: { past: '#ede9fe', pastText: '#7c3aed', future: '#f5f3ff', futureText: '#a78bfa' },
    VACATION: { past: '#fee2e2', pastText: '#dc2626', future: '#fef2f2', futureText: '#f87171' },
  }
  const c = typeColors[w.weekType] || typeColors.TEACHING

  if (isPast) return { background: c.past, color: c.pastText, fontWeight: '400' }
  return { background: c.future, color: c.futureText, fontWeight: '400' }
}

function getEventTypeName(type: number) {
  const map: Record<number, string> = { 1: '放假', 2: '考试', 3: '活动', 4: '会议', 5: '其他' }
  return map[type] || '事件'
}

function getEventChip(type: number) {
  const map: Record<number, string> = { 1: 'tm-chip-red', 2: 'tm-chip-purple', 3: 'tm-chip-blue', 4: 'tm-chip-amber', 5: 'tm-chip-gray' }
  return map[type] || 'tm-chip-gray'
}
</script>

<style scoped>
.ov-card { border: 1px solid #e5e7eb; border-radius: 10px; background: #f9fafb; padding: 16px; text-align: center; }
.ov-num { font-size: 28px; font-weight: 700; color: #111827; }
.ov-label { font-size: 12px; color: #6b7280; margin-top: 4px; }
.wk-block {
  flex: 1;
  height: 26px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  cursor: default;
  transition: all 0.15s;
}
</style>
