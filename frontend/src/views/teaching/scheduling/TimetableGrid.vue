<template>
  <div class="timetable-wrap">
    <table class="timetable">
      <thead>
        <tr>
          <th class="tt-period-col">节次</th>
          <th v-for="day in displayWeekdays" :key="day.value" class="tt-day-col">
            {{ day.label }}
            <div v-if="weekDates && weekDates[day.value]" class="tt-day-date">{{ weekDates[day.value] }}</div>
          </th>
        </tr>
      </thead>
      <tbody>
        <template v-for="period in periods" :key="period.period">
          <!-- Section separator: 午休 (after period 4) / 晚间 (after period 8) -->
          <tr v-if="isSectionBreak(period.period)" class="tt-break-row">
            <td :colspan="displayWeekdays.length + 1" class="tt-break-cell">
              {{ getSectionLabel(period.period) }}
            </td>
          </tr>
          <!-- Period row -->
          <tr :class="['tt-row', getPeriodSection(period.period)]">
            <td class="tt-period-cell">
              <div class="tt-period-name">{{ period.name }}</div>
              <div class="tt-period-time">{{ period.startTime }}-{{ period.endTime }}</div>
            </td>
            <td
              v-for="day in displayWeekdays"
              :key="day.value"
              v-show="!isOccupied(day.value, period.period)"
              :rowspan="getRowspan(day.value, period.period)"
              :class="[
                'tt-cell',
                editable ? 'tt-cell-editable' : '',
                getEntriesForStart(day.value, period.period).length > 0 ? 'tt-cell-has-entry' : '',
                hasConflict(day.value, period.period) ? 'tt-cell-conflict' : '',
                isDragOver(day.value, period.period) ? (isDragValid(day.value, period.period) ? 'tt-cell-dragover' : 'tt-cell-drag-invalid') : '',
                isForbiddenSlot(day.value, period.period) ? 'tt-cell-forbidden' : '',
              ]"
              @click="handleCellClick(day.value, period.period)"
              @dragover="onDragOver($event, day.value, period.period)"
              @dragleave="onDragLeave"
              @drop="onDrop($event, day.value, period.period)"
            >
              <div
                v-for="entry in getEntriesForStart(day.value, period.period)"
                :key="entry.id"
                :class="['tt-entry', draggedEntry?.id === entry.id ? 'tt-entry-dragging' : '', entry.scheduleType === 4 ? 'tt-entry-self-study' : '']"
                :style="getEntryStyle(entry)"
                :draggable="editable && entry.scheduleType !== 4"
                @click.stop="emit('entry-click', entry)"
                @dragstart="onDragStart($event, entry)"
                @dragend="onDragEnd"
              >
                <div class="tt-entry-course">
                  <span v-if="(entry as any).isLocked" class="tt-entry-lock" title="已锁定，自动排课不会覆盖">🔒</span>
                  {{ entry.scheduleType === 4 ? '自习' : entry.courseName }}
                </div>
                <!-- 班级视图：显示 教师 + 教室 -->
                <template v-if="viewType === 'class'">
                  <div class="tt-entry-detail">{{ entry.scheduleType === 4 ? '' : (entry.teacherName || '') }}</div>
                  <div class="tt-entry-detail">{{ entry.scheduleType === 4 ? '' : (entry.classroomName || '') }}</div>
                </template>
                <!-- 教师视图：显示 班级 + 教室 -->
                <template v-else-if="viewType === 'teacher'">
                  <div class="tt-entry-detail">{{ entry.scheduleType === 4 ? '' : (entry.className || '') }}</div>
                  <div class="tt-entry-detail">{{ entry.scheduleType === 4 ? '' : (entry.classroomName || '') }}</div>
                </template>
                <!-- 教室视图：显示 班级 + 教师 -->
                <template v-else-if="viewType === 'classroom'">
                  <div class="tt-entry-detail">{{ entry.scheduleType === 4 ? '' : (entry.className || '') }}</div>
                  <div class="tt-entry-detail">{{ entry.scheduleType === 4 ? '' : (entry.teacherName || '') }}</div>
                </template>
                <div v-if="entry.weekStart && entry.weekEnd" class="tt-entry-weeks">
                  {{ entry.weekStart }}-{{ entry.weekEnd }}周{{ entry.weekType === 1 ? '(单)' : entry.weekType === 2 ? '(双)' : '' }}
                </div>
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>

    <div v-if="entries.length === 0" class="tt-empty">暂无课表数据</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ScheduleEntry, PeriodConfig } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

interface Props {
  entries: ScheduleEntry[]
  periods?: PeriodConfig[]
  weekdays?: { value: number; label: string }[]
  weekDates?: Record<number, string>  // dayOfWeek → date string (e.g. "9/1")
  editable?: boolean
  constraintMatrix?: any[][]
  viewType?: 'class' | 'teacher' | 'classroom'  // 根据视图调整显示内容
}

const props = withDefaults(defineProps<Props>(), {
  periods: () => DEFAULT_PERIODS,
  weekdays: undefined,
  weekDates: undefined,
  editable: false,
  constraintMatrix: undefined,
  viewType: 'class',
})

const emit = defineEmits<{
  'entry-click': [entry: ScheduleEntry]
  'cell-click': [day: number, period: number]
  'entry-drop': [entryId: number, newDay: number, newPeriod: number]
}>()

const displayWeekdays = computed(() => props.weekdays ?? WEEKDAYS.slice(0, 5))

// ==================== Color Palette ====================

const colorPalette = [
  { bg: '#EFF6FF', border: '#BFDBFE', text: '#1E40AF' },   // blue
  { bg: '#ECFDF5', border: '#A7F3D0', text: '#065F46' },   // emerald
  { bg: '#FFF7ED', border: '#FED7AA', text: '#9A3412' },   // orange
  { bg: '#F5F3FF', border: '#DDD6FE', text: '#5B21B6' },   // violet
  { bg: '#FFF1F2', border: '#FECDD3', text: '#9F1239' },   // rose
  { bg: '#ECFEFF', border: '#A5F3FC', text: '#155E75' },   // cyan
  { bg: '#EEF2FF', border: '#C7D2FE', text: '#3730A3' },   // indigo
  { bg: '#F0FDF4', border: '#BBF7D0', text: '#14532D' },   // green
]

function getEntryStyle(entry: ScheduleEntry) {
  // Self-study: gray background + dashed border
  if (entry.scheduleType === 4) {
    return {
      background: '#f9fafb',
      borderLeft: '3px dashed #d1d5db',
      color: '#9ca3af',
    }
  }
  const idx = Number(entry.taskId ?? entry.id ?? 0) % colorPalette.length
  const c = colorPalette[idx]
  return {
    background: c.bg,
    borderLeft: `3px solid ${c.border}`,
    color: c.text,
  }
}

// ==================== Section Breaks ====================

// Dynamic section breaks based on total period count
const totalPeriods = computed(() => props.periods.length)
const morningEnd = computed(() => Math.ceil(totalPeriods.value / 2))  // e.g. 4 for 8, 5 for 10
const afternoonEnd = computed(() => totalPeriods.value <= 8 ? totalPeriods.value : Math.ceil(totalPeriods.value * 0.8)) // e.g. 8 for 10

function isSectionBreak(period: number): boolean {
  const pIdx = props.periods.findIndex(p => p.period === period)
  const periodNum = pIdx + 1
  return periodNum === morningEnd.value + 1 || (totalPeriods.value > 8 && periodNum === afternoonEnd.value + 1)
}

function getSectionLabel(period: number): string {
  const pIdx = props.periods.findIndex(p => p.period === period)
  const periodNum = pIdx + 1
  if (periodNum === morningEnd.value + 1) return '午 休'
  if (totalPeriods.value > 8 && periodNum === afternoonEnd.value + 1) return '晚 间'
  return ''
}

function getPeriodSection(period: number): string {
  const pIdx = props.periods.findIndex(p => p.period === period)
  const periodNum = pIdx + 1
  if (periodNum <= morningEnd.value) return 'tt-morning'
  if (periodNum <= afternoonEnd.value) return 'tt-afternoon'
  return 'tt-evening'
}

// ==================== Rowspan Logic ====================

function getEntriesForStart(day: number, period: number): ScheduleEntry[] {
  return props.entries.filter(e => e.dayOfWeek === day && e.periodStart === period)
}

function isOccupied(day: number, period: number): boolean {
  // Check if this cell is occupied by a multi-period entry starting above
  return props.entries.some(
    e => e.dayOfWeek === day && e.periodStart < period && e.periodEnd >= period
  )
}

function getRowspan(day: number, period: number): number {
  const entries = getEntriesForStart(day, period)
  if (entries.length === 0) return 1
  // Use the max span of all entries starting here
  const maxSpan = Math.max(...entries.map(e => {
    let span = e.periodEnd - e.periodStart + 1
    // Don't span across section breaks
    if (e.periodStart <= 4 && e.periodEnd >= 5) span = 4 - e.periodStart + 1
    if (e.periodStart <= 8 && e.periodEnd >= 9) span = 8 - e.periodStart + 1
    return span
  }))
  return maxSpan
}

// ==================== Drag-and-Drop ====================

const draggedEntry = ref<any>(null)
const dragOverCell = ref<{ day: number; period: number } | null>(null)

function onDragStart(e: DragEvent, entry: any) {
  if (!props.editable) return
  draggedEntry.value = entry
  e.dataTransfer!.effectAllowed = 'move'
  e.dataTransfer!.setData('text/plain', String(entry.id))
}

function onDragOver(e: DragEvent, day: number, period: number) {
  if (!props.editable || !draggedEntry.value) return
  e.preventDefault()
  dragOverCell.value = { day, period }
}

function onDragLeave() { dragOverCell.value = null }

function onDrop(e: DragEvent, day: number, period: number) {
  e.preventDefault()
  if (!draggedEntry.value) return
  emit('entry-drop', draggedEntry.value.id, day, period)
  draggedEntry.value = null
  dragOverCell.value = null
}

function onDragEnd() { draggedEntry.value = null; dragOverCell.value = null }

function isDragOver(day: number, period: number): boolean {
  return dragOverCell.value?.day === day && dragOverCell.value?.period === period
}

function isForbiddenSlot(day: number, period: number): boolean {
  if (!props.constraintMatrix) return false
  const dayRow = props.constraintMatrix[day - 1]
  if (!dayRow) return false
  return dayRow[period - 1]?.status === 'forbidden'
}

// Conflict detection: multiple entries for same cell from different tasks
function hasConflict(day: number, period: number): boolean {
  const entries = props.entries.filter(
    e => e.dayOfWeek === day && e.periodStart <= period && e.periodEnd >= period
  )
  return entries.length > 1
}

// Drag validity: check if the target cell is free
function isDragValid(day: number, period: number): boolean {
  if (!draggedEntry.value) return true
  const span = draggedEntry.value.periodEnd - draggedEntry.value.periodStart + 1
  for (let p = period; p < period + span; p++) {
    const existing = props.entries.filter(
      e => e.dayOfWeek === day && e.periodStart <= p && e.periodEnd >= p && e.id !== draggedEntry.value.id
    )
    if (existing.length > 0) return false
  }
  return true
}

function handleCellClick(day: number, period: number) {
  if (props.editable) emit('cell-click', day, period)
}
</script>

<style scoped>
/* ========== Timetable Layout ========== */
.timetable-wrap { overflow-x: auto; }

.timetable {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-family: 'DM Sans', system-ui, sans-serif;
}

/* Header */
.timetable thead th {
  padding: 8px 4px;
  font-size: 12.5px;
  font-weight: 600;
  color: #374151;
  background: #f9fafb;
  border-bottom: 2px solid #e5e7eb;
  text-align: center;
}
.tt-period-col { width: 72px; }
.tt-day-col { min-width: 100px; }
.tt-day-date { font-size: 10px; font-weight: 400; color: #9ca3af; margin-top: 1px; }

/* Period cell */
.tt-period-cell {
  padding: 4px 6px;
  text-align: center;
  background: #fafbfc;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #f3f4f6;
}
.tt-period-name { font-size: 12px; font-weight: 600; color: #374151; }
.tt-period-time { font-size: 10px; color: #9ca3af; margin-top: 1px; }

/* Data cell */
.tt-cell {
  padding: 2px;
  border-right: 1px solid #f3f4f6;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: top;
  min-height: 48px;
  height: 48px;
}
.tt-cell:last-child { border-right: none; }
.tt-cell-editable { cursor: pointer; }
.tt-cell-editable:not(.tt-cell-has-entry):hover {
  background: #eff6ff;
  outline: 2px dashed #3b82f6;
  outline-offset: -2px;
  position: relative;
}
.tt-cell-editable:not(.tt-cell-has-entry):hover::after {
  content: '＋ 手动排课';
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #2563eb;
  font-weight: 500;
  pointer-events: none;
}
.tt-cell-has-entry { padding: 2px; }
.tt-cell-conflict { background: #FEF2F2 !important; outline: 2px solid #EF4444; outline-offset: -2px; }
.tt-cell-dragover { background: #DBEAFE !important; outline: 2px dashed #3B82F6; outline-offset: -2px; }
.tt-cell-drag-invalid { background: #FEE2E2 !important; outline: 2px dashed #EF4444; outline-offset: -2px; cursor: not-allowed; }
.tt-cell-forbidden { background: #f9fafb; opacity: 0.4; }

/* ========== Course Entry Card ========== */
.tt-entry {
  border-radius: 5px;
  padding: 4px 6px;
  margin-bottom: 2px;
  height: calc(100% - 2px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.tt-entry:hover { box-shadow: 0 1px 4px rgba(0,0,0,0.12); }
.tt-entry:last-child { margin-bottom: 0; }
.tt-entry-dragging { opacity: 0.4; transform: scale(0.95); }
.tt-entry-self-study { border-style: dashed !important; opacity: 0.7; }

.tt-entry-lock { font-size: 10px; margin-right: 2px; }
.tt-entry-course {
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tt-entry-detail {
  font-size: 10px;
  line-height: 1.3;
  opacity: 0.8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tt-entry-weeks {
  font-size: 9px;
  opacity: 0.6;
  margin-top: 1px;
}

/* ========== Section Break Rows ========== */
.tt-break-row {}
.tt-break-cell {
  padding: 3px 0;
  text-align: center;
  font-size: 10px;
  font-weight: 500;
  color: #9ca3af;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  letter-spacing: 4px;
}

/* Section background tints */
.tt-morning .tt-period-cell { border-left: 3px solid #3B82F6; }
.tt-afternoon .tt-period-cell { border-left: 3px solid #F59E0B; }
.tt-evening .tt-period-cell { border-left: 3px solid #6366F1; }

/* Empty state */
.tt-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  font-size: 13px;
  color: #9ca3af;
}
</style>
