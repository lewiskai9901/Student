<template>
  <div class="flex gap-4">
    <!-- Left: Calendar view -->
    <div class="min-w-0 flex-1 rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-1">
          <el-button text size="small" @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
          <span class="min-w-[80px] text-center text-sm font-medium text-gray-900">{{ currentMonthText }}</span>
          <el-button text size="small" @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <div class="flex items-center gap-2">
          <el-button text type="primary" size="small" @click="goToday">今天</el-button>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="month">月历</el-radio-button>
            <el-radio-button label="list">周历</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- Month view -->
      <div
        v-if="viewMode === 'month'"
        class="select-none p-3"
        :class="{ 'cursor-crosshair': isDragging }"
        @mouseup="handleDragEnd"
        @mouseleave="handleDragEnd"
      >
        <!-- Weekday headers -->
        <div class="mb-1 grid grid-cols-[28px_repeat(7,1fr)]">
          <span class="text-center text-[10px] text-gray-300">周</span>
          <span v-for="d in ['日','一','二','三','四','五','六']" :key="d" class="text-center text-xs text-gray-400 py-1">{{ d }}</span>
        </div>
        <!-- Weeks -->
        <div class="flex flex-col gap-0.5">
          <div v-for="(week, wi) in calendarWeeks" :key="wi" class="grid grid-cols-[28px_repeat(7,1fr)] gap-0.5">
            <div class="flex items-center justify-center rounded text-[10px] text-gray-300 bg-gray-50">{{ week.weekNumber || '-' }}</div>
            <div
              v-for="(day, di) in week.days"
              :key="di"
              class="relative flex aspect-square cursor-pointer flex-col items-center justify-center rounded text-xs transition-all"
              :class="getDayClasses(day)"
              @mousedown.prevent="startDrag(day)"
              @mouseenter="moveDrag(day)"
              @click.prevent="handleDayClick(day)"
            >
              <span class="text-xs">{{ day.day }}</span>
              <span v-if="day.events.length" class="mt-px text-[9px] leading-none" :class="day.isToday ? 'text-white/80' : 'text-gray-500'">
                {{ day.events[0].title?.substring(0, 4) || getShortEventName(day.events[0].eventType) }}
              </span>
            </div>
          </div>
        </div>
        <div class="mt-2 text-center text-[11px] text-gray-300 transition-opacity" :class="{ 'opacity-0': isDragging }">
          拖拽选择日期范围快速添加事件
        </div>
      </div>

      <!-- Semester overview view -->
      <div
        v-else
        class="select-none p-3"
        :class="{ 'cursor-crosshair': isDragging }"
        @mouseup="handleDragEnd"
        @mouseleave="handleDragEnd"
      >
        <div class="mb-2 text-sm font-medium text-gray-700">{{ currentSemester?.name || '本学期' }} 校历总览</div>
        <div class="overflow-x-auto">
          <table class="w-full border-collapse text-xs">
            <thead>
              <tr class="bg-gray-50">
                <th class="w-10 border border-gray-200 px-1 py-1.5 text-center font-medium text-gray-500">月</th>
                <th class="w-8 border border-gray-200 px-1 py-1.5 text-center font-medium text-gray-500">周</th>
                <th v-for="d in ['日','一','二','三','四','五','六']" :key="d" class="w-9 border border-gray-200 px-1 py-1.5 text-center font-medium text-gray-500">{{ d }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="week in semesterWeeksData" :key="week.weekNumber" :class="{ 'border-b-2 border-gray-300': week.isMonthLastRow }">
                <td v-if="week.monthRowSpan > 0" class="border border-gray-200 bg-gray-50 px-1 py-1 text-center font-medium text-gray-600" :rowspan="week.monthRowSpan">{{ week.monthLabel }}</td>
                <td class="border border-gray-200 px-1 py-1 text-center text-gray-400">{{ week.weekNumber }}</td>
                <td
                  v-for="(day, idx) in week.days"
                  :key="idx"
                  class="border border-gray-200 px-1 py-1 text-center transition-colors"
                  :class="getOverviewDayClasses(day)"
                  @mousedown.prevent="startOverviewDrag(day)"
                  @mouseenter="moveOverviewDrag(day)"
                  @click.prevent="handleOverviewDayClick(day)"
                >
                  <div class="text-xs">{{ day.day || '' }}</div>
                  <div v-if="day.events.length" class="mt-px text-[9px] leading-none text-gray-500">{{ day.events[0].title?.substring(0, 2) }}</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Legend -->
      <div class="flex items-center gap-3 border-t border-gray-100 px-4 py-2">
        <span v-for="item in legendItems" :key="item.type" class="flex items-center gap-1 text-[11px] text-gray-500">
          <i class="inline-block h-2 w-4 rounded-sm" :class="item.color" />
          {{ item.label }}
        </span>
      </div>
    </div>

    <!-- Right: Event list -->
    <div class="flex w-80 shrink-0 flex-col rounded-xl border border-gray-200 bg-white" style="max-height: calc(100vh - 300px)">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <span class="text-sm font-semibold text-gray-900">事件列表</span>
        <div class="flex items-center gap-2">
          <el-radio-group v-model="eventFilter" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="upcoming">近期</el-radio-button>
          </el-radio-group>
          <button
            class="inline-flex items-center gap-1 rounded-lg bg-blue-600 px-2.5 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
            @click="$emit('show-event-dialog')"
          >
            添加
          </button>
        </div>
      </div>
      <div class="flex-1 overflow-y-auto p-2">
        <div
          v-for="event in filteredEvents"
          :key="event.id"
          class="mb-1.5 flex cursor-pointer gap-3 rounded-lg px-3 py-2.5 transition-colors hover:bg-gray-50"
          @click="$emit('show-event-dialog', event)"
        >
          <div
            class="flex h-11 w-11 shrink-0 flex-col items-center justify-center rounded-lg text-white"
            :class="getEventDateBg(event.eventType)"
          >
            <span class="text-base font-bold leading-none">{{ getEventDay(event.startDate) }}</span>
            <span class="text-[10px] leading-none">{{ getEventMonth(event.startDate) }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <div class="truncate text-sm font-medium text-gray-900">{{ event.title }}</div>
            <div class="mt-0.5 flex items-center gap-2">
              <el-tag size="small" :type="getEventTagType(event.eventType)">{{ getEventTypeName(event.eventType) }}</el-tag>
              <span v-if="event.endDate && event.endDate !== event.startDate" class="text-xs text-gray-400">
                {{ formatEventDate(event.startDate) }} ~ {{ formatEventDate(event.endDate) }}
              </span>
              <span v-else class="text-xs text-gray-400">{{ getEventCountdown(event.startDate) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!filteredEvents.length" description="暂无事件" :image-size="50" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import type { Semester, AcademicEvent } from '@/types/teaching'

const props = defineProps<{
  events: AcademicEvent[]
  currentSemester: Semester | null
  currentYearId: number | undefined
}>()

const emit = defineEmits<{
  'show-event-dialog': [event?: AcademicEvent]
  'create-event-from-drag': [data: { startDate: string; endDate: string; isRange: boolean }]
}>()

const calendarDate = ref(new Date())
const viewMode = ref<'month' | 'list'>('month')
const eventFilter = ref('all')
const isDragging = ref(false)
const dragStartDate = ref<Date | null>(null)
const dragEndDate = ref<Date | null>(null)
const dragSelectedDates = ref<Set<string>>(new Set())

const legendItems = [
  { type: 1, label: '开学', color: 'bg-emerald-400' },
  { type: 2, label: '放假', color: 'bg-amber-400' },
  { type: 3, label: '考试', color: 'bg-red-400' },
  { type: 4, label: '活动', color: 'bg-blue-400' },
  { type: 6, label: '补课', color: 'bg-orange-400' },
]

// ========== Date helpers ==========
const formatDateStr = (date: Date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const getShortEventName = (type: number) => {
  const map: Record<number, string> = { 1: '开', 2: '假', 3: '考', 4: '活', 5: '他', 6: '补' }
  return map[type] || ''
}

// ========== Month navigation ==========
const currentMonthText = computed(() => {
  const y = calendarDate.value.getFullYear()
  const m = calendarDate.value.getMonth() + 1
  return `${y}年${m}月`
})

const prevMonth = () => {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() - 1)
  calendarDate.value = d
}
const nextMonth = () => {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() + 1)
  calendarDate.value = d
}
const goToday = () => { calendarDate.value = new Date() }

// ========== Calendar days (month view) ==========
const calendarDays = computed(() => {
  const year = calendarDate.value.getFullYear()
  const month = calendarDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const startDate = new Date(firstDay)
  startDate.setDate(startDate.getDate() - firstDay.getDay())
  const days = []
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  for (let i = 0; i < 42; i++) {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + i)
    const dateStr = formatDateStr(date)
    const dayEvents = props.events.filter(e => {
      const eventStart = e.startDate?.substring(0, 10)
      const eventEnd = e.endDate?.substring(0, 10) || eventStart
      return dateStr >= eventStart && dateStr <= eventEnd
    })
    days.push({
      date, day: date.getDate(),
      isCurrentMonth: date.getMonth() === month,
      isToday: date.getTime() === today.getTime(),
      isWeekend: date.getDay() === 0 || date.getDay() === 6,
      events: dayEvents,
    })
  }
  return days
})

const calendarWeeks = computed(() => {
  const days = calendarDays.value
  const weeks = []
  let semesterStart: Date | null = null
  let semesterEnd: Date | null = null
  let semesterStartWeekSunday: Date | null = null

  if (props.currentSemester?.startDate) {
    semesterStart = new Date(props.currentSemester.startDate)
    semesterStart.setHours(0, 0, 0, 0)
    semesterStartWeekSunday = new Date(semesterStart)
    semesterStartWeekSunday.setDate(semesterStartWeekSunday.getDate() - semesterStartWeekSunday.getDay())
  }
  if (props.currentSemester?.endDate) {
    semesterEnd = new Date(props.currentSemester.endDate)
    semesterEnd.setHours(23, 59, 59, 999)
  }

  for (let i = 0; i < 6; i++) {
    const weekDays = days.slice(i * 7, (i + 1) * 7)
    let weekNumber: number | null = null
    if (semesterStart && semesterEnd && semesterStartWeekSunday && weekDays.length > 0) {
      const thisWeekSunday = weekDays[0].date
      const weekEnd = weekDays[6].date
      if (thisWeekSunday <= semesterEnd && weekEnd >= semesterStart) {
        const weeksDiff = Math.round((thisWeekSunday.getTime() - semesterStartWeekSunday.getTime()) / (7 * 24 * 60 * 60 * 1000))
        weekNumber = weeksDiff + 1
        if (weekNumber < 1) weekNumber = null
      }
    }
    weeks.push({ weekNumber, days: weekDays })
  }
  return weeks
})

// ========== Semester overview (week view) ==========
const semesterWeeksData = computed(() => {
  if (!props.currentSemester) return []
  const startDate = new Date(props.currentSemester.startDate)
  const endDate = new Date(props.currentSemester.endDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const rows: Array<{
    weekNumber: number | string; monthLabel: string; monthRowSpan: number; isMonthLastRow: boolean
    days: Array<{ date: Date | null; day: number | null; isToday: boolean; events: AcademicEvent[] }>
  }> = []

  const semesterWeekStart = new Date(startDate)
  semesterWeekStart.setDate(semesterWeekStart.getDate() - semesterWeekStart.getDay())
  let currentDate = new Date(startDate)

  while (currentDate <= endDate) {
    const rowMonth = currentDate.getMonth()
    const dayOfWeek = currentDate.getDay()
    const daysSinceStart = Math.floor((currentDate.getTime() - semesterWeekStart.getTime()) / 86400000)
    const weekNum = Math.floor(daysSinceStart / 7) + 1
    const days: typeof rows[0]['days'] = []

    for (let d = 0; d < dayOfWeek; d++) {
      days.push({ date: null, day: null, isToday: false, events: [] })
    }

    let rowWeekStart = weekNum, rowWeekEnd = weekNum
    while (days.length < 7 && currentDate <= endDate) {
      const date = new Date(currentDate)
      if (date.getMonth() !== rowMonth) break
      const dateStr = formatDateStr(date)
      const currDays = Math.floor((date.getTime() - semesterWeekStart.getTime()) / 86400000)
      rowWeekEnd = Math.floor(currDays / 7) + 1
      const dayEvents = props.events.filter(e => {
        const es = e.startDate?.substring(0, 10)
        const ee = e.endDate?.substring(0, 10) || es
        return dateStr >= es && dateStr <= ee
      })
      days.push({ date, day: date.getDate(), isToday: date.getTime() === today.getTime(), events: dayEvents })
      currentDate.setDate(currentDate.getDate() + 1)
    }
    while (days.length < 7) days.push({ date: null, day: null, isToday: false, events: [] })

    rows.push({
      weekNumber: rowWeekStart === rowWeekEnd ? rowWeekStart : `${rowWeekStart}-${rowWeekEnd}`,
      monthLabel: (rowMonth + 1) + '月', monthRowSpan: 0, isMonthLastRow: false, days,
    })
  }

  let i = 0
  while (i < rows.length) {
    const month = rows[i].monthLabel
    let span = 1
    while (i + span < rows.length && rows[i + span].monthLabel === month) { rows[i + span].monthRowSpan = 0; span++ }
    rows[i].monthRowSpan = span
    rows[i + span - 1].isMonthLastRow = true
    i += span
  }
  return rows
})

// ========== Day styling ==========
const isDayInDragSelection = (date: Date | null) => date ? dragSelectedDates.value.has(formatDateStr(date)) : false

const getDayClasses = (day: typeof calendarDays.value[0]) => {
  const c: string[] = []
  if (!day.isCurrentMonth) { c.push('text-gray-300'); return c }
  if (isDayInDragSelection(day.date)) { c.push('!bg-blue-400 text-white'); return c }
  if (day.isToday) { c.push('bg-blue-600 text-white'); return c }
  if (day.events.length) {
    const t = day.events[0].eventType
    const map: Record<number, string> = { 1: 'bg-emerald-50 text-emerald-700', 2: 'bg-amber-50 text-amber-700', 3: 'bg-red-50 text-red-700', 4: 'bg-blue-50 text-blue-700', 5: 'bg-gray-100 text-gray-600', 6: 'bg-orange-50 text-orange-700' }
    c.push(map[t] || 'bg-gray-50')
    return c
  }
  if (day.isWeekend) c.push('text-red-400 bg-gray-50')
  else c.push('bg-gray-50 hover:bg-blue-50')
  return c
}

const getOverviewDayClasses = (day: { date: Date | null; isToday: boolean; events: AcademicEvent[] }) => {
  if (!day.date) return 'bg-gray-50/50'
  if (isDayInDragSelection(day.date)) return '!bg-blue-400 text-white'
  if (day.isToday) return 'bg-blue-600 text-white'
  if (day.events.length) {
    const t = day.events[0].eventType
    const map: Record<number, string> = { 1: 'bg-emerald-50', 2: 'bg-amber-50', 3: 'bg-red-50', 4: 'bg-blue-50', 5: 'bg-gray-100', 6: 'bg-orange-50' }
    return `cursor-pointer ${map[t] || ''}`
  }
  return 'cursor-pointer hover:bg-blue-50/50'
}

// ========== Drag selection ==========
const updateDragSelection = () => {
  if (!dragStartDate.value || !dragEndDate.value) { dragSelectedDates.value = new Set(); return }
  const start = dragStartDate.value.getTime() < dragEndDate.value.getTime() ? dragStartDate.value : dragEndDate.value
  const end = dragStartDate.value.getTime() < dragEndDate.value.getTime() ? dragEndDate.value : dragStartDate.value
  const s = new Set<string>()
  const cur = new Date(start)
  while (cur.getTime() <= end.getTime()) { s.add(formatDateStr(cur)); cur.setDate(cur.getDate() + 1) }
  dragSelectedDates.value = s
}

const startDrag = (day: { date: Date }) => {
  if (!day.date) return
  isDragging.value = true
  dragStartDate.value = new Date(day.date)
  dragEndDate.value = new Date(day.date)
  updateDragSelection()
}
const moveDrag = (day: { date: Date }) => {
  if (!isDragging.value || !day.date) return
  dragEndDate.value = new Date(day.date)
  updateDragSelection()
}
const startOverviewDrag = (day: { date: Date | null }) => {
  if (!day.date) return
  isDragging.value = true
  dragStartDate.value = new Date(day.date)
  dragEndDate.value = new Date(day.date)
  updateDragSelection()
}
const moveOverviewDrag = (day: { date: Date | null }) => {
  if (!isDragging.value || !day.date) return
  dragEndDate.value = new Date(day.date)
  updateDragSelection()
}
const handleDragEnd = () => {
  if (!isDragging.value) return
  isDragging.value = false
  if (dragStartDate.value && dragEndDate.value) {
    const start = dragStartDate.value.getTime() < dragEndDate.value.getTime() ? dragStartDate.value : dragEndDate.value
    const end = dragStartDate.value.getTime() < dragEndDate.value.getTime() ? dragEndDate.value : dragStartDate.value
    emit('create-event-from-drag', {
      startDate: formatDateStr(start),
      endDate: formatDateStr(end),
      isRange: start.getTime() !== end.getTime(),
    })
  }
  dragStartDate.value = null
  dragEndDate.value = null
  dragSelectedDates.value = new Set()
}

const handleDayClick = (day: { date: Date; events: AcademicEvent[] }) => {
  if (dragSelectedDates.value.size > 1) return
  if (day.events.length === 1) {
    emit('show-event-dialog', day.events[0])
  } else if (day.events.length === 0 && dragSelectedDates.value.size === 0) {
    emit('create-event-from-drag', { startDate: formatDateStr(day.date), endDate: formatDateStr(day.date), isRange: false })
  }
}

const handleOverviewDayClick = (day: { date: Date | null; events: AcademicEvent[] }) => {
  if (!day.date || isDragging.value) return
  if (day.events.length === 1) emit('show-event-dialog', day.events[0])
}

// ========== Event list helpers ==========
const filteredEvents = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  let list = [...props.events]
  if (eventFilter.value === 'upcoming') list = list.filter(e => new Date(e.startDate) >= today)
  return list.sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())
})

const getEventDay = (dateStr: string) => new Date(dateStr).getDate()
const getEventMonth = (dateStr: string) => (new Date(dateStr).getMonth() + 1) + '月'

const getEventDateBg = (type: number) => {
  const map: Record<number, string> = { 1: 'bg-emerald-500', 2: 'bg-amber-500', 3: 'bg-red-500', 4: 'bg-blue-500', 5: 'bg-gray-400', 6: 'bg-orange-500' }
  return map[type] || 'bg-gray-400'
}

const getEventTagType = (type: number) => {
  const map: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = { 1: 'success', 2: 'warning', 3: 'danger', 4: '', 5: 'info', 6: 'warning' }
  return map[type] || 'info'
}

const getEventTypeName = (type: number) => {
  const map: Record<number, string> = { 1: '开学', 2: '放假', 3: '考试', 4: '活动', 5: '其他', 6: '补课' }
  return map[type] || '其他'
}

const getEventCountdown = (dateStr: string) => {
  const diff = Math.ceil((new Date(dateStr).getTime() - Date.now()) / 86400000)
  if (diff < 0) return '已过'
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  return `${diff}天后`
}

const formatEventDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}
</script>
