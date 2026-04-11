<template>
  <div class="matrix-wrap">
    <!-- Selection bar -->
    <div class="matrix-select-bar">
      <span class="matrix-select-label">选择查看对象：</span>
      <el-select
        v-model="selectedIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        :placeholder="placeholder"
        style="width: 400px;"
        @change="loadAllData"
      >
        <el-option-group v-for="group in groupedOptions" :key="group.label" :label="group.label">
          <el-option v-for="opt in group.options" :key="opt.id" :value="opt.id" :label="opt.name" />
        </el-option-group>
      </el-select>
      <span v-if="selectedIds.length > 0" class="matrix-count">已选 {{ selectedIds.length }} 个</span>
    </div>

    <!-- Matrix table -->
    <div v-if="selectedIds.length > 0 && matrixData.length > 0" class="matrix-table-wrap">
      <table class="matrix-table">
        <thead>
          <tr class="matrix-day-row">
            <th class="matrix-target-col" rowspan="2">{{ modeLabel }}</th>
            <th v-for="day in weekdays" :key="day.value" :colspan="periods.length" class="matrix-day-th">
              {{ day.label }}
              <span v-if="weekDates && weekDates[day.value]" class="matrix-day-date">{{ weekDates[day.value] }}</span>
            </th>
          </tr>
          <tr class="matrix-period-row">
            <template v-for="day in weekdays" :key="'p-'+day.value">
              <th v-for="p in periods" :key="day.value+'-'+p.period" class="matrix-period-th">{{ p.period }}</th>
            </template>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in matrixData" :key="row.id" class="matrix-row">
            <td class="matrix-target-cell">{{ row.name }}</td>
            <template v-for="day in weekdays" :key="'c-'+day.value">
              <td
                v-for="p in periods"
                :key="day.value+'-'+p.period"
                class="matrix-cell"
                :style="getCellStyle(row, day.value, p.period)"
                :title="getCellTitle(row, day.value, p.period)"
              >
                <span v-if="getCellText(row, day.value, p.period)" class="matrix-cell-text">
                  {{ getCellText(row, day.value, p.period) }}
                </span>
              </td>
            </template>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="selectedIds.length === 0" class="matrix-empty">请选择要查看的对象</div>
    <div v-else class="matrix-empty">加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { http as request } from '@/utils/request'
import { scheduleApi } from '@/api/teaching'
import type { PeriodConfig, ScheduleEntry } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

const props = defineProps<{
  mode: 'class' | 'teacher' | 'classroom'
  semesterId?: number | string
  periods?: PeriodConfig[]
  options?: { id: number | string; name: string; group?: string }[]
  weekDates?: Record<number, string>
  // For live timetable: pass instances instead of schedule entries
  dataSource?: 'schedule' | 'instance'
}>()

const weekdays = WEEKDAYS.slice(0, 5)
const periods = computed(() => props.periods || DEFAULT_PERIODS)
const selectedIds = ref<(number | string)[]>([])

const modeLabel = computed(() => ({ class: '班级', teacher: '教师', classroom: '教室' }[props.mode]))
const placeholder = computed(() => `选择${modeLabel.value}（可多选）`)

// Group options
const groupedOptions = computed(() => {
  const opts = props.options || []
  const groups: Record<string, { id: number | string; name: string }[]> = {}
  for (const o of opts) {
    const g = (o as any).group || '全部'
    if (!groups[g]) groups[g] = []
    groups[g].push(o)
  }
  return Object.entries(groups).map(([label, options]) => ({ label, options }))
})

// Color palette for courses
const colorMap = new Map<string, { bg: string; text: string }>()
const colorPalette = [
  { bg: '#DBEAFE', text: '#1E40AF' },
  { bg: '#D1FAE5', text: '#065F46' },
  { bg: '#FEF3C7', text: '#92400E' },
  { bg: '#EDE9FE', text: '#5B21B6' },
  { bg: '#FCE7F3', text: '#9D174D' },
  { bg: '#CFFAFE', text: '#155E75' },
  { bg: '#E0E7FF', text: '#3730A3' },
  { bg: '#FEE2E2', text: '#991B1B' },
]
function getColor(courseName: string) {
  if (!colorMap.has(courseName)) {
    colorMap.set(courseName, colorPalette[colorMap.size % colorPalette.length])
  }
  return colorMap.get(courseName)!
}

// Matrix data: each row = { id, name, entries: ScheduleEntry[] }
const matrixData = ref<{ id: number | string; name: string; entries: any[] }[]>([])

async function loadAllData() {
  if (!props.semesterId || selectedIds.value.length === 0) {
    matrixData.value = []
    return
  }

  const results: { id: number | string; name: string; entries: any[] }[] = []

  for (const targetId of selectedIds.value) {
    const opt = (props.options || []).find(o => String(o.id) === String(targetId))
    const name = opt?.name || String(targetId)

    try {
      let res: any
      if (props.dataSource === 'instance') {
        // Live timetable instances
        const params: any = { semesterId: props.semesterId }
        if (props.mode === 'class') params.orgUnitId = targetId
        else if (props.mode === 'teacher') params.teacherId = targetId
        else params.classroomId = targetId
        res = await request.get('/teaching/instances', { params })
        const instances = (res as any).data || res || []
        // Deduplicate by weekday+slot
        const map = new Map<string, any>()
        for (const inst of instances) {
          if (inst.status === 1) continue
          const key = `${inst.weekday}-${inst.startSlot}`
          if (!map.has(key)) {
            map.set(key, { dayOfWeek: inst.weekday, periodStart: inst.startSlot, periodEnd: inst.endSlot, courseName: inst.courseName, teacherName: inst.teacherName, classroomName: inst.classroomName })
          }
        }
        results.push({ id: targetId, name, entries: Array.from(map.values()) })
      } else {
        // Base schedule entries
        if (props.mode === 'class') res = await scheduleApi.getByClass(targetId, props.semesterId)
        else if (props.mode === 'teacher') res = await scheduleApi.getByTeacher(targetId, props.semesterId)
        else res = await scheduleApi.getByClassroom(targetId, props.semesterId)
        results.push({ id: targetId, name, entries: (res as any).data || res || [] })
      }
    } catch {
      results.push({ id: targetId, name, entries: [] })
    }
  }

  matrixData.value = results
}

function findEntry(row: any, day: number, period: number): any | null {
  return row.entries.find((e: any) =>
    e.dayOfWeek === day && e.periodStart <= period && e.periodEnd >= period
  ) || null
}

function getCellText(row: any, day: number, period: number): string {
  const e = findEntry(row, day, period)
  if (!e) return ''
  // Only show text at periodStart to avoid repeating in merged cells
  if (e.periodStart === period) return e.courseName?.substring(0, 3) || ''
  return ''
}

function getCellTitle(row: any, day: number, period: number): string {
  const e = findEntry(row, day, period)
  if (!e) return ''
  return `${e.courseName || ''}\n${e.teacherName || ''}\n${e.classroomName || ''}`
}

function getCellStyle(row: any, day: number, period: number) {
  const e = findEntry(row, day, period)
  if (!e) return {}
  const c = getColor(e.courseName || '')
  return { background: c.bg, color: c.text }
}

watch(() => props.semesterId, () => { selectedIds.value = []; matrixData.value = [] })
</script>

<style scoped>
.matrix-wrap { overflow: hidden; }
.matrix-select-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  margin-bottom: 12px;
}
.matrix-select-label { font-size: 13px; color: #6b7280; white-space: nowrap; }
.matrix-count { font-size: 12px; color: #2563eb; font-weight: 500; }
.matrix-empty { text-align: center; padding: 40px; color: #9ca3af; font-size: 13px; }

/* Table */
.matrix-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}
.matrix-table {
  border-collapse: collapse;
  font-size: 11px;
  width: max-content;
  min-width: 100%;
}
.matrix-table th, .matrix-table td {
  border: 1px solid #f3f4f6;
  text-align: center;
  white-space: nowrap;
}

/* Header */
.matrix-target-col {
  position: sticky;
  left: 0;
  z-index: 2;
  background: #f9fafb;
  min-width: 100px;
  padding: 6px 8px;
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}
.matrix-day-th {
  background: #f9fafb;
  padding: 5px 4px;
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  border-bottom: none;
}
.matrix-day-date { font-size: 10px; font-weight: 400; color: #9ca3af; margin-left: 2px; }
.matrix-period-th {
  background: #f9fafb;
  padding: 3px 2px;
  font-size: 10px;
  font-weight: 500;
  color: #9ca3af;
  min-width: 28px;
  width: 28px;
}

/* Body */
.matrix-target-cell {
  position: sticky;
  left: 0;
  z-index: 1;
  background: #fff;
  padding: 4px 8px;
  font-size: 11.5px;
  font-weight: 500;
  color: #111827;
  text-align: left;
  border-right: 2px solid #e5e7eb;
}
.matrix-cell {
  padding: 2px 1px;
  min-width: 28px;
  height: 28px;
  transition: background 0.1s;
}
.matrix-cell:hover { opacity: 0.8; }
.matrix-cell-text {
  font-size: 9px;
  font-weight: 600;
  line-height: 1;
  display: block;
  overflow: hidden;
  text-overflow: clip;
}
.matrix-row:hover .matrix-target-cell { background: #f8faff; }
</style>
