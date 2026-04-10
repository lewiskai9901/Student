<template>
  <div>
    <!-- Stats bar -->
    <div class="tm-stats" style="margin-bottom: 16px;">
      <span>教学 <b>{{ grid?.totalTeachingDays || 0 }}</b> 天</span>
      <span class="sep" />
      <span style="color: #dc2626;">假期 <b>{{ grid?.totalHolidayDays || 0 }}</b> 天</span>
      <span class="sep" />
      <span style="color: #ea580c;">补课 <b>{{ grid?.totalMakeupDays || 0 }}</b> 天</span>
      <span class="sep" />
      <span style="color: #7c3aed;">考试 <b>{{ grid?.totalExamDays || 0 }}</b> 天</span>
    </div>

    <!-- Legend -->
    <div style="display: flex; gap: 16px; margin-bottom: 12px; font-size: 12px; color: #6b7280;">
      <span><span class="legend-dot" style="background: #fff; border: 1px solid #d1d5db;"></span> 教学日</span>
      <span><span class="legend-dot" style="background: #f3f4f6;"></span> 休息日</span>
      <span><span class="legend-dot" style="background: #fef2f2;"></span> 假期</span>
      <span><span class="legend-dot" style="background: #fff7ed;"></span> 补课</span>
      <span><span class="legend-dot" style="background: #f5f3ff;"></span> 考试</span>
    </div>

    <!-- Grid table -->
    <div v-if="grid" class="grid-container" @mouseup="onMouseUp" @mouseleave="onMouseUp">
      <table style="width: 100%; border-collapse: collapse; font-size: 12px; user-select: none;">
        <thead>
          <tr style="background: #f9fafb;">
            <th style="padding: 8px 12px; text-align: center; font-weight: 600; color: #374151; width: 50px; border-bottom: 1px solid #e5e7eb;">周次</th>
            <th v-for="d in ['周一','周二','周三','周四','周五','周六','周日']" :key="d"
                style="padding: 8px 4px; text-align: center; font-weight: 500; color: #6b7280; border-bottom: 1px solid #e5e7eb;">{{ d }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="week in grid.weeks" :key="week.weekNumber" style="border-bottom: 1px solid #f3f4f6;">
            <td :style="getWeekCellStyle(week.weekType)" style="padding: 4px 8px; text-align: center; border-right: 1px solid #e5e7eb;">
              <div style="font-weight: 600; color: #374151; font-size: 13px;">{{ week.weekNumber }}</div>
              <span v-if="week.weekType === 'TEACHING'" class="week-chip" style="background: #f3f4f6; color: #6b7280;">教</span>
              <span v-else-if="week.weekType === 'EXAM'" class="week-chip" style="background: #ede9fe; color: #7c3aed;">考</span>
              <span v-else-if="week.weekType === 'VACATION'" class="week-chip" style="background: #fee2e2; color: #dc2626;">假</span>
            </td>
            <td v-for="wd in 7" :key="wd"
                :style="getCellStyle(getDayByWeekday(week, wd))"
                :class="{
                  'day-cell': !!getDayByWeekday(week, wd),
                  'day-selected': isDaySelected(getDayByWeekday(week, wd)),
                }"
                :title="getCellTitle(getDayByWeekday(week, wd))"
                style="padding: 3px 2px; text-align: center; position: relative; cursor: pointer;"
                @mousedown.prevent="onMouseDown(getDayByWeekday(week, wd), $event)"
                @mouseenter="onMouseEnter(getDayByWeekday(week, wd))"
                @click="onCellClick(getDayByWeekday(week, wd), $event)"
            >
              <template v-if="getDayByWeekday(week, wd)">
                <div style="font-size: 12px; font-weight: 500;">{{ formatDay(getDayByWeekday(week, wd)!.date) }}</div>
                <div v-if="getDayByWeekday(week, wd)!.eventName"
                     style="font-size: 9px; margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 85px; margin-left: auto; margin-right: auto; line-height: 1.2;">
                  {{ getDayByWeekday(week, wd)!.eventName }}
                </div>
                <div v-if="getDayByWeekday(week, wd)!.followWeekday" style="font-size: 9px; color: #ea580c; line-height: 1.2;">
                  按{{ weekdayName(getDayByWeekday(week, wd)!.followWeekday!) }}
                </div>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="loading" style="text-align: center; padding: 40px; color: #9ca3af;">加载中...</div>
    <div v-if="!loading && !grid" style="text-align: center; padding: 40px; color: #9ca3af;">请选择学期</div>

    <!-- Popover for adding/viewing event -->
    <Teleport to="body">
      <div v-if="popover.visible" class="popover-overlay" @click.self="closePopover">
        <div class="popover-box" :style="{ top: popover.top + 'px', left: popover.left + 'px' }">
          <!-- Viewing existing event -->
          <template v-if="popover.mode === 'view'">
            <div style="font-weight: 600; font-size: 13px; color: #111827; margin-bottom: 8px;">
              {{ popover.existingEvent?.eventName }}
            </div>
            <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px;">
              {{ popover.dateRange }}
            </div>
            <div style="font-size: 12px; margin-bottom: 12px;">
              <span :class="['tm-chip', getEventChipClass(popover.existingEvent?.dayType)]">{{ getDayTypeName(popover.existingEvent?.dayType) }}</span>
              <span v-if="popover.existingEvent?.followWeekday" style="margin-left: 6px; color: #ea580c;">
                按{{ weekdayName(popover.existingEvent.followWeekday) }}课表
              </span>
            </div>
            <div style="display: flex; gap: 8px;">
              <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 4px 12px;" @click="closePopover">关闭</button>
              <button class="tm-btn" style="font-size: 12px; padding: 4px 12px; color: #dc2626; border-color: #fca5a5;" @click="deleteEvent">删除事件</button>
            </div>
          </template>

          <!-- Creating new event -->
          <template v-else>
            <div style="font-weight: 600; font-size: 13px; color: #111827; margin-bottom: 10px;">
              添加事件 <span style="font-weight: 400; font-size: 12px; color: #6b7280;">{{ popover.dateRange }}</span>
            </div>
            <div style="margin-bottom: 8px;">
              <input v-model="popover.eventName" class="tm-input" style="width: 100%; height: 30px; font-size: 12px;" placeholder="事件名称（如：国庆节）" @keyup.enter="saveEvent" />
            </div>
            <div style="display: flex; gap: 6px; margin-bottom: 8px; flex-wrap: wrap;">
              <button v-for="t in eventTypes" :key="t.value"
                      :class="['mode-btn', { [`active-${t.key}`]: popover.affectType === t.value }]"
                      @click="popover.affectType = t.value">{{ t.label }}</button>
            </div>
            <div v-if="popover.affectType === 3" style="margin-bottom: 8px;">
              <label style="font-size: 11px; color: #6b7280; display: block; margin-bottom: 4px;">按周几课表上课</label>
              <select v-model="popover.followWeekday" class="tm-input" style="width: 100%; height: 30px; font-size: 12px;">
                <option :value="1">周一</option>
                <option :value="2">周二</option>
                <option :value="3">周三</option>
                <option :value="4">周四</option>
                <option :value="5">周五</option>
              </select>
            </div>
            <div style="display: flex; gap: 8px; justify-content: flex-end;">
              <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 4px 12px;" @click="closePopover">取消</button>
              <button class="tm-btn tm-btn-primary" style="font-size: 12px; padding: 4px 12px;" @click="saveEvent">确定</button>
            </div>
          </template>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { semesterApi, academicEventApi } from '@/api/calendar'
import type { CalendarGrid, CalendarDay, CalendarWeek } from '@/types/teaching'

const props = defineProps<{
  semesterId: number | string | undefined
  yearId?: number | string
}>()
const emit = defineEmits<{ loaded: [grid: CalendarGrid] }>()

const grid = ref<CalendarGrid | null>(null)
const loading = ref(false)

// --- Drag selection ---
const isDragging = ref(false)
const dragStart = ref<CalendarDay | null>(null)
const selectedDates = ref<Set<string>>(new Set())

// --- Popover ---
const popover = reactive({
  visible: false,
  top: 0,
  left: 0,
  mode: 'create' as 'create' | 'view',
  dates: [] as string[],
  dateRange: '',
  eventName: '',
  affectType: 1 as number,
  followWeekday: 1,
  existingEvent: null as CalendarDay | null,
})

const eventTypes = [
  { value: 1, key: 'holiday', label: '假期' },
  { value: 3, key: 'makeup', label: '补课' },
  { value: 4, key: 'exam', label: '考试' },
  { value: 10, key: 'activity', label: '活动' },
  { value: 11, key: 'note', label: '其他' },
]

async function loadGrid() {
  if (!props.semesterId) { grid.value = null; return }
  loading.value = true
  try {
    const res = await semesterApi.getCalendarGrid(props.semesterId)
    grid.value = res
    if (res) emit('loaded', res)
  } catch { grid.value = null } finally { loading.value = false }
}

// --- Drag to select ---
function onMouseDown(day: CalendarDay | undefined, e: MouseEvent) {
  if (!day) return
  isDragging.value = true
  dragStart.value = day
  selectedDates.value = new Set([day.date])
}

function onMouseEnter(day: CalendarDay | undefined) {
  if (!isDragging.value || !day || !dragStart.value) return
  // select range between dragStart and current day
  const allDays = getAllDays()
  const startIdx = allDays.findIndex(d => d.date === dragStart.value!.date)
  const endIdx = allDays.findIndex(d => d.date === day.date)
  if (startIdx < 0 || endIdx < 0) return
  const from = Math.min(startIdx, endIdx)
  const to = Math.max(startIdx, endIdx)
  const newSet = new Set<string>()
  for (let i = from; i <= to; i++) newSet.add(allDays[i].date)
  selectedDates.value = newSet
}

function onMouseUp() {
  if (!isDragging.value) return
  isDragging.value = false
  // If multiple dates selected, open popover for range
  if (selectedDates.value.size > 1) {
    const sorted = Array.from(selectedDates.value).sort()
    openCreatePopover(sorted)
  }
}

function onCellClick(day: CalendarDay | undefined, e: MouseEvent) {
  if (!day) return
  // If we were dragging multiple, onMouseUp already handled it
  if (selectedDates.value.size > 1) return

  if (day.eventId) {
    // Click existing event → view popover
    openViewPopover(day, e)
  } else {
    // Click empty day → create popover
    openCreatePopover([day.date], e)
  }
}

function openCreatePopover(dates: string[], e?: MouseEvent) {
  const sorted = dates.sort()
  popover.mode = 'create'
  popover.dates = sorted
  popover.dateRange = sorted.length === 1
    ? formatDateCN(sorted[0])
    : `${formatDateCN(sorted[0])} ~ ${formatDateCN(sorted[sorted.length - 1])}（${sorted.length}天）`
  popover.eventName = ''
  popover.affectType = 1
  popover.followWeekday = 1
  popover.existingEvent = null
  positionPopover(e)
  popover.visible = true
}

function openViewPopover(day: CalendarDay, e: MouseEvent) {
  popover.mode = 'view'
  popover.dates = [day.date]
  popover.dateRange = formatDateCN(day.date)
  popover.existingEvent = day
  positionPopover(e)
  popover.visible = true
}

function positionPopover(e?: MouseEvent) {
  if (e) {
    const x = Math.min(e.clientX, window.innerWidth - 320)
    const y = Math.min(e.clientY + 10, window.innerHeight - 280)
    popover.top = y
    popover.left = x
  } else {
    popover.top = window.innerHeight / 2 - 100
    popover.left = window.innerWidth / 2 - 140
  }
}

function closePopover() {
  popover.visible = false
  selectedDates.value = new Set()
}

async function saveEvent() {
  const name = popover.eventName.trim()
  if (!name) { ElMessage.warning('请输入事件名称'); return }
  if (!props.semesterId) return

  const sorted = popover.dates.sort()
  // Map internal affectType values to backend eventType and real affectType
  const isInfo = popover.affectType >= 10
  const eventTypeMap: Record<number, number> = { 1: 2, 3: 5, 4: 3, 10: 4, 11: 5 }
  const realAffectType = isInfo ? 0 : popover.affectType

  try {
    await academicEventApi.create({
      semesterId: props.semesterId,
      yearId: props.yearId,
      eventName: name,
      eventType: eventTypeMap[popover.affectType] || 5,
      startDate: sorted[0],
      endDate: sorted[sorted.length - 1],
      allDay: true,
      affectType: realAffectType,
      substituteWeekday: popover.affectType === 3 ? popover.followWeekday : undefined,
    } as any)
    ElMessage.success('添加成功')
    closePopover()
    await loadGrid()
  } catch { ElMessage.error('添加失败') }
}

async function deleteEvent() {
  if (!popover.existingEvent?.eventId) return
  try {
    await academicEventApi.delete(popover.existingEvent.eventId)
    ElMessage.success('已删除')
    closePopover()
    await loadGrid()
  } catch { ElMessage.error('删除失败') }
}

// --- Helpers ---
function getAllDays(): CalendarDay[] {
  if (!grid.value) return []
  return grid.value.weeks.flatMap(w => w.days)
}

function isDaySelected(day: CalendarDay | undefined) {
  return day ? selectedDates.value.has(day.date) : false
}

function getDayByWeekday(week: CalendarWeek, wd: number): CalendarDay | undefined {
  return week.days.find(d => d.weekday === wd)
}

function formatDay(dateStr: string) {
  const parts = dateStr.split('-')
  return `${parseInt(parts[1])}/${parseInt(parts[2])}`
}

function formatDateCN(dateStr: string) {
  const parts = dateStr.split('-')
  return `${parts[1]}月${parseInt(parts[2])}日`
}

function weekdayName(wd: number) {
  return ({ 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五' } as Record<number, string>)[wd] || ''
}

function getDayTypeName(t?: string) {
  return ({ TEACHING: '教学日', WEEKEND: '休息日', HOLIDAY: '假期', MAKEUP: '补课', EXAM: '考试' } as any)[t || ''] || ''
}

function getEventChipClass(t?: string) {
  return ({ HOLIDAY: 'tm-chip-red', MAKEUP: 'tm-chip-amber', EXAM: 'tm-chip-purple' } as any)[t || ''] || 'tm-chip-gray'
}

const dayStyles: Record<string, { bg: string; color: string }> = {
  TEACHING: { bg: '#fff', color: '#111827' },
  WEEKEND: { bg: '#f3f4f6', color: '#9ca3af' },
  HOLIDAY: { bg: '#fef2f2', color: '#dc2626' },
  MAKEUP: { bg: '#fff7ed', color: '#ea580c' },
  EXAM: { bg: '#f5f3ff', color: '#7c3aed' },
}

function getCellStyle(day: CalendarDay | undefined) {
  if (!day) return { background: '#fafafa' }
  const s = dayStyles[day.dayType] || dayStyles.TEACHING
  return { background: s.bg, color: s.color }
}

function getWeekCellStyle(weekType?: string) {
  if (weekType === 'EXAM') return { background: '#f5f3ff' }
  if (weekType === 'VACATION') return { background: '#fef2f2' }
  return { background: '#f9fafb' }
}

function getCellTitle(day: CalendarDay | undefined) {
  if (!day) return ''
  const typeName: Record<string, string> = { TEACHING: '教学日', WEEKEND: '休息日', HOLIDAY: '假期', MAKEUP: '补课', EXAM: '考试' }
  let title = `${day.date} ${typeName[day.dayType] || ''}`
  if (day.eventName) title += ` - ${day.eventName}`
  if (day.followWeekday) title += ` (按${weekdayName(day.followWeekday)}课表)`
  return title
}

watch(() => props.semesterId, () => loadGrid(), { immediate: true })
</script>

<style scoped>
.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
  vertical-align: middle;
  margin-right: 4px;
}
.grid-container {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.week-chip {
  display: inline-block;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  margin-top: 2px;
  font-weight: 500;
}
.day-cell {
  cursor: pointer;
  transition: filter 0.1s;
}
.day-cell:hover {
  filter: brightness(0.93);
}
.day-selected {
  outline: 2px solid #3b82f6;
  outline-offset: -2px;
  border-radius: 2px;
}
.mode-btn {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  transition: all 0.15s;
}
.mode-btn:hover { background: #f3f4f6; }
.mode-btn.active-holiday { background: #fef2f2; border-color: #fca5a5; color: #dc2626; }
.mode-btn.active-makeup { background: #fff7ed; border-color: #fdba74; color: #ea580c; }
.mode-btn.active-exam { background: #f5f3ff; border-color: #c4b5fd; color: #7c3aed; }
.mode-btn.active-activity { background: #eff6ff; border-color: #93c5fd; color: #2563eb; }
.mode-btn.active-note { background: #f3f4f6; border-color: #9ca3af; color: #4b5563; }

/* Popover */
.popover-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
}
.popover-box {
  position: fixed;
  width: 280px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
  padding: 16px;
  z-index: 3001;
}
</style>
