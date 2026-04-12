<template>
  <div class="matrix-wrap">
    <!-- Hint when empty -->
    <div v-if="displayTargets.length === 0" class="matrix-empty">
      请在左侧树中勾选要查看的{{ modeLabel }}（可勾选部门/年级/楼层批量选择）
    </div>

    <!-- Matrix table -->
    <div v-else-if="matrixData.length > 0" class="matrix-table-wrap">
      <div class="matrix-toolbar">
        <span style="font-size: 12.5px; color: #6b7280;">已选 <b style="color:#2563eb;">{{ displayTargets.length }}</b> 个{{ modeLabel }}</span>
      </div>
      <table class="matrix-table">
        <thead>
          <tr class="matrix-day-row">
            <th class="matrix-target-col" rowspan="2">{{ modeLabel }}</th>
            <th v-for="(day, dIdx) in weekdays" :key="day.value" :colspan="periods.length"
                class="matrix-day-th"
                :class="{ 'matrix-day-th-divider': dIdx < weekdays.length - 1 }">
              {{ day.label }}
              <span v-if="weekDates && weekDates[day.value]" class="matrix-day-date">{{ weekDates[day.value] }}</span>
            </th>
          </tr>
          <tr class="matrix-period-row">
            <template v-for="day in weekdays" :key="'p-'+day.value">
              <th v-for="(p, pIdx) in periods" :key="day.value+'-'+p.period"
                  class="matrix-period-th"
                  :class="{
                    'matrix-cell-day-start': pIdx === 0,
                    'matrix-cell-day-end': pIdx === periods.length - 1,
                  }">{{ p.period }}</th>
            </template>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in matrixData" :key="row.id" class="matrix-row">
            <td class="matrix-target-cell">{{ row.name }}</td>
            <template v-for="day in weekdays" :key="'c-'+day.value">
              <td
                v-for="(p, pIdx) in periods"
                :key="day.value+'-'+p.period"
                class="matrix-cell"
                :class="{
                  'matrix-cell-has': !!findEntry(row, day.value, p.period),
                  'matrix-cell-day-start': pIdx === 0,
                  'matrix-cell-day-end': pIdx === periods.length - 1,
                }"
                :style="getCellStyle(row, day.value, p.period)"
                :title="getCellTitle(row, day.value, p.period)"
              >
                <template v-if="isEntryStart(row, day.value, p.period)">
                  <!-- 单双周同时存在：分两格 -->
                  <div v-if="getSlotEntries(row, day.value, p.period).hasSplit" class="matrix-cell-split">
                    <div
                      v-for="e in getSlotEntries(row, day.value, p.period).list"
                      :key="e.id"
                      class="matrix-cell-content matrix-cell-split-item"
                      :style="getEntryStyle(e)"
                      :title="`${e.weekType === 1 ? '单周' : e.weekType === 2 ? '双周' : '每周'}\n${e.courseName}\n${e.teacherName || ''}\n${e.classroomName || ''}`"
                    >
                      <span v-if="e.weekType === 1" class="matrix-week-tag tag-odd">单</span>
                      <span v-else-if="e.weekType === 2" class="matrix-week-tag tag-even">双</span>
                      <div class="matrix-cell-course">{{ e.courseName }}</div>
                      <div v-if="e.teacherName" class="matrix-cell-teacher">{{ e.teacherName }}</div>
                      <div v-if="e.classroomName" class="matrix-cell-room">{{ e.classroomName }}</div>
                    </div>
                  </div>
                  <!-- 单条目（可能是每周/纯单周/纯双周）-->
                  <div v-else class="matrix-cell-content">
                    <span v-if="getSlotEntries(row, day.value, p.period).list[0]?.weekType === 1" class="matrix-week-tag tag-odd">单</span>
                    <span v-else-if="getSlotEntries(row, day.value, p.period).list[0]?.weekType === 2" class="matrix-week-tag tag-even">双</span>
                    <div class="matrix-cell-course">{{ getSlotEntries(row, day.value, p.period).list[0]?.courseName }}</div>
                    <div v-if="getSlotEntries(row, day.value, p.period).list[0]?.teacherName" class="matrix-cell-teacher">{{ getSlotEntries(row, day.value, p.period).list[0].teacherName }}</div>
                    <div v-if="getSlotEntries(row, day.value, p.period).list[0]?.classroomName" class="matrix-cell-room">{{ getSlotEntries(row, day.value, p.period).list[0].classroomName }}</div>
                  </div>
                </template>
              </td>
            </template>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="matrix-empty">加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { http as request } from '@/utils/request'
import { scheduleApi } from '@/api/teaching'
import type { PeriodConfig } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

const props = defineProps<{
  mode: 'class' | 'teacher' | 'classroom'
  semesterId?: number | string
  periods?: PeriodConfig[]
  options?: { id: number | string; name: string; group?: string }[]
  selectedTargets?: { id: number | string; name: string }[]
  weekDates?: Record<number, string>
  dataSource?: 'schedule' | 'instance'
}>()

const weekdays = WEEKDAYS.slice(0, 5)
const periods = computed(() => props.periods || DEFAULT_PERIODS)

const modeLabel = computed(() => ({ class: '班级', teacher: '教师', classroom: '教室' }[props.mode]))

// 优先使用外部传入的 selectedTargets（来自左树多选），否则用内部状态
const displayTargets = computed(() => props.selectedTargets || [])

// Color palette for courses
const colorMap = new Map<string, { bg: string; text: string; border: string }>()
const colorPalette = [
  { bg: '#EFF6FF', text: '#1E40AF', border: '#BFDBFE' },
  { bg: '#ECFDF5', text: '#065F46', border: '#A7F3D0' },
  { bg: '#FFF7ED', text: '#9A3412', border: '#FED7AA' },
  { bg: '#F5F3FF', text: '#5B21B6', border: '#DDD6FE' },
  { bg: '#FFF1F2', text: '#9F1239', border: '#FECDD3' },
  { bg: '#ECFEFF', text: '#155E75', border: '#A5F3FC' },
  { bg: '#EEF2FF', text: '#3730A3', border: '#C7D2FE' },
  { bg: '#F0FDF4', text: '#14532D', border: '#BBF7D0' },
]
function getColor(courseName: string) {
  if (!colorMap.has(courseName)) {
    colorMap.set(courseName, colorPalette[colorMap.size % colorPalette.length])
  }
  return colorMap.get(courseName)!
}

const matrixData = ref<{ id: number | string; name: string; entries: any[] }[]>([])

async function loadAllData() {
  if (!props.semesterId || displayTargets.value.length === 0) {
    matrixData.value = []
    return
  }

  const results: { id: number | string; name: string; entries: any[] }[] = []

  for (const target of displayTargets.value) {
    const targetId = target.id
    const name = target.name

    try {
      let res: any
      if (props.dataSource === 'instance') {
        const params: any = { semesterId: props.semesterId }
        if (props.mode === 'class') params.orgUnitId = targetId
        else if (props.mode === 'teacher') params.teacherId = targetId
        else params.classroomId = targetId
        res = await request.get('/teaching/instances', { params })
        const instances = (res as any).data || res || []
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

/** 获取同时段的所有条目（可能有单+双周冲突共存）*/
function getSlotEntries(row: any, day: number, period: number): { list: any[]; hasSplit: boolean } {
  const list = row.entries.filter((e: any) =>
    e.dayOfWeek === day && e.periodStart <= period && e.periodEnd >= period
  )
  // 同时存在至少两种不同的 weekType 才分裂显示
  const types = new Set(list.map((e: any) => e.weekType || 0))
  const hasSplit = list.length > 1 && types.size > 1
  return {
    list: list.sort((a: any, b: any) => (a.weekType || 0) - (b.weekType || 0)),
    hasSplit,
  }
}

function isEntryStart(row: any, day: number, period: number): boolean {
  const entries = row.entries.filter((e: any) =>
    e.dayOfWeek === day && e.periodStart === period
  )
  return entries.length > 0
}

function getEntryStyle(entry: any) {
  const c = getColor(entry.courseName || '')
  return {
    background: c.bg,
    color: c.text,
    borderLeft: `3px solid ${c.border}`,
  }
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
  return {
    background: c.bg,
    color: c.text,
    borderLeft: `3px solid ${c.border}`,
  }
}

watch(() => [props.semesterId, props.mode], () => {
  matrixData.value = []
  loadAllData()
})

watch(() => props.selectedTargets, () => {
  loadAllData()
}, { deep: true, immediate: true })
</script>

<style scoped>
.matrix-wrap { overflow: hidden; }
.matrix-toolbar {
  padding: 10px 16px;
  border-bottom: 1px solid #f3f4f6;
  background: #fafafa;
}
.matrix-empty {
  text-align: center;
  padding: 60px 40px;
  color: #9ca3af;
  font-size: 13px;
  border: 1px dashed #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
  margin: 16px;
}

/* Table */
.matrix-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}
.matrix-table {
  border-collapse: collapse;
  font-size: 12px;
  width: max-content;
  min-width: 100%;
}
.matrix-table th, .matrix-table td {
  border: 1px solid #f3f4f6;
  text-align: center;
  vertical-align: middle;
}

/* Header */
.matrix-target-col {
  position: sticky; left: 0; z-index: 2;
  background: #f9fafb;
  min-width: 120px;
  padding: 8px 10px;
  font-size: 12.5px;
  font-weight: 600;
  color: #374151;
}
.matrix-day-th {
  background: #f9fafb;
  padding: 6px 4px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  border-bottom: none;
}
.matrix-day-date { font-size: 10px; font-weight: 400; color: #9ca3af; margin-left: 4px; }
.matrix-period-th {
  background: #f9fafb;
  padding: 4px 2px;
  font-size: 11px;
  font-weight: 500;
  color: #6b7280;
  min-width: 64px;
  width: 64px;
}

/* Body */
.matrix-target-cell {
  position: sticky; left: 0; z-index: 1;
  background: #fff;
  padding: 8px 10px;
  font-size: 12.5px;
  font-weight: 500;
  color: #111827;
  text-align: left;
  border-right: 2px solid #e5e7eb;
  white-space: nowrap;
}
.matrix-cell {
  padding: 0;
  min-width: 64px;
  width: 64px;
  height: 68px;
  transition: background 0.1s;
  position: relative;
}
.matrix-cell-has:hover { opacity: 0.85; cursor: pointer; }

/* 周次之间的分隔线 */
.matrix-cell-day-end {
  border-right: 3px solid #d1d5db !important;
}
.matrix-cell-day-start {
  border-left: 1px solid #e5e7eb;
}
.matrix-day-th-divider {
  border-right: 3px solid #d1d5db !important;
}
/* 整个周次的分隔区域一个淡色背景条 */
.matrix-table tbody .matrix-cell-day-start {
  box-shadow: inset 4px 0 0 -3px #f3f4f6;
}

/* Cell content - 3 lines centered */
.matrix-cell-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 4px 3px;
  gap: 2px;
  text-align: center;
}
.matrix-cell-course {
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  max-width: 100%;
}
.matrix-cell-teacher {
  font-size: 10.5px;
  line-height: 1.2;
  opacity: 0.85;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.matrix-cell-room {
  font-size: 10px;
  line-height: 1.1;
  opacity: 0.7;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.matrix-row:hover .matrix-target-cell { background: #f8faff; }

/* 单双周分裂单元格 */
.matrix-cell-split {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}
.matrix-cell-split-item {
  flex: 1;
  min-height: 0;
  position: relative;
  padding: 3px 2px;
}
.matrix-cell-split-item + .matrix-cell-split-item {
  border-top: 1.5px dashed #d1d5db;
}
.matrix-cell-split .matrix-cell-course { font-size: 11px; line-height: 1.1; }
.matrix-cell-split .matrix-cell-teacher,
.matrix-cell-split .matrix-cell-room { font-size: 9.5px; line-height: 1; }

/* 单双周标签 */
.matrix-week-tag {
  position: absolute;
  top: 2px; right: 2px;
  font-size: 9px;
  font-weight: 700;
  padding: 0 3px;
  border-radius: 2px;
  line-height: 1.2;
  z-index: 1;
}
.tag-odd {
  background: #f59e0b;
  color: #fff;
}
.tag-even {
  background: #3b82f6;
  color: #fff;
}

/* 分裂后单元格要高些 */
.matrix-cell-split {
  height: 100%;
}
</style>
