<template>
  <div>
    <!-- Week Timeline -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 20px; margin-bottom: 16px;">
      <h3 style="font-size: 14px; font-weight: 600; color: #111827; margin: 0 0 12px;">教学周进度</h3>
      <div style="display: flex; gap: 4px; margin-bottom: 8px;">
        <div
          v-for="w in weeks"
          :key="w.weekNumber"
          :title="`第${w.weekNumber}周: ${w.startDate} ~ ${w.endDate}`"
          style="flex: 1; height: 24px; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 10px; cursor: pointer; transition: all 0.15s;"
          :style="{
            background: w.isCurrent ? '#2563eb' : isPastWeek(w) ? '#dbeafe' : '#f3f4f6',
            color: w.isCurrent ? '#fff' : isPastWeek(w) ? '#3b82f6' : '#9ca3af',
            fontWeight: w.isCurrent ? '700' : '400',
          }"
        >{{ w.weekNumber }}</div>
      </div>
      <div class="tm-stats" style="margin-top: 0;">
        <span>共 <b>{{ weeks.length }}</b> 教学周</span>
        <span class="sep" />
        <span>已过 <b>{{ passedWeeks }}</b> 周</span>
        <span class="sep" />
        <span>剩余 <b>{{ weeks.length - passedWeeks }}</b> 周</span>
      </div>
    </div>

    <!-- Stats Cards -->
    <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px;">
      <div class="ov-card">
        <div class="ov-num">{{ weeks.length }}</div>
        <div class="ov-label">教学周</div>
      </div>
      <div class="ov-card">
        <div class="ov-num" style="color: #dc2626;">{{ holidayDays }}</div>
        <div class="ov-label">放假天数</div>
      </div>
      <div class="ov-card">
        <div class="ov-num" style="color: #d97706;">{{ eventCount }}</div>
        <div class="ov-label">校历事件</div>
      </div>
      <div class="ov-card">
        <div class="ov-num" style="color: #2563eb;">{{ makeupDays }}</div>
        <div class="ov-label">补课天数</div>
      </div>
    </div>

    <!-- This Week Events -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 20px;">
      <h3 style="font-size: 14px; font-weight: 600; color: #111827; margin: 0 0 12px;">
        {{ currentWeek ? `第${currentWeek.weekNumber}周 (${currentWeek.startDate} ~ ${currentWeek.endDate})` : '本周' }}
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
}>()

const passedWeeks = computed(() => {
  if (!props.currentWeek) return 0
  return props.currentWeek.weekNumber - 1
})

const holidayDays = computed(() => {
  return props.events.filter(e => e.affectType === 1 || e.eventType === 1).length
})

const eventCount = computed(() => props.events.length)

const makeupDays = computed(() => {
  return props.events.filter(e => e.affectType === 3).length
})

const thisWeekEvents = computed(() => {
  if (!props.currentWeek) return props.events.slice(0, 5)
  const start = props.currentWeek.startDate
  const end = props.currentWeek.endDate
  return props.events.filter(e => e.startDate <= end && (e.endDate || e.startDate) >= start)
})

function isPastWeek(w: any) {
  if (!props.currentWeek) return false
  return w.weekNumber < props.currentWeek.weekNumber
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
</style>
