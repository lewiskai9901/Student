<template>
  <div class="live-tt">
    <!-- 时间轴 -->
    <div v-if="weeks.length > 0" class="live-timeline">
      <button class="tt-today-btn" @click="jumpToToday">
        <span class="tt-today-dot" /> 今天 {{ todayLabel }}
      </button>
      <div class="tt-weeks">
        <div
          v-for="w in weeks"
          :key="w.weekNumber"
          :class="['tt-week', weekClass(w)]"
          :title="`第${w.weekNumber}周 ${w.startDate} ~ ${w.endDate} (${weekTypeLabel(w.weekType)})`"
          @click="jumpToWeek(w.weekNumber)"
        >{{ w.weekNumber }}</div>
      </div>
      <div class="tt-legend">
        <span class="tt-legend-item"><span class="tt-legend-dot tt-teach" /> 教学</span>
        <span class="tt-legend-item"><span class="tt-legend-dot tt-exam" /> 考试</span>
        <span class="tt-legend-item"><span class="tt-legend-dot tt-holi" /> 假期</span>
      </div>
    </div>

    <!-- 控制栏 -->
    <div class="live-controls">
      <div class="tm-radios" style="width: auto;">
        <button :class="['tm-radio', { active: viewType === 'class' }]" @click="onTypeChange('class')">班级课表</button>
        <button :class="['tm-radio', { active: viewType === 'teacher' }]" @click="onTypeChange('teacher')">教师课表</button>
        <button :class="['tm-radio', { active: viewType === 'classroom' }]" @click="onTypeChange('classroom')">教室课表</button>
      </div>
      <select v-model="targetId" class="tm-select" @change="loadInstances">
        <option :value="undefined" disabled>{{ targetPlaceholder }}</option>
        <option v-for="o in targetOptions" :key="o.id" :value="o.id">{{ o.name }}</option>
      </select>
      <span class="live-sep" />
      <div class="live-week-pager">
        <button class="live-arrow" :disabled="weekNumber <= 1" @click="weekNumber > 1 && (weekNumber--, loadInstances())">◀</button>
        <span class="live-week-label">
          第 {{ weekNumber }} 周
          <span class="live-week-range">{{ weekRangeLabel }}</span>
        </span>
        <button class="live-arrow" :disabled="weekNumber >= weeks.length" @click="weekNumber < weeks.length && (weekNumber++, loadInstances())">▶</button>
      </div>
      <span v-if="currentWeekTag" class="live-tag" :class="'tag-' + currentWeekTag.type">{{ currentWeekTag.label }}</span>
    </div>

    <!-- 日期+节次网格 -->
    <div v-if="loading" class="live-empty">加载中...</div>
    <div v-else-if="!targetId" class="live-empty">请选择查看对象</div>
    <div v-else class="live-grid-wrap">
      <table class="live-grid">
        <thead>
          <tr>
            <th class="live-period-col">节次</th>
            <th
              v-for="d in weekDates"
              :key="d.dateStr"
              :class="['live-day-col', { 'is-today': d.isToday, 'is-holiday': d.isFullHoliday }]"
            >
              <div class="live-day-wd" :class="{ 'is-weekend': d.weekday > 5 }">
                {{ weekdayName(d.weekday) }}
              </div>
              <div class="live-day-date">{{ d.monthDay }}</div>
              <div v-if="d.event" class="live-day-event" :class="'event-' + d.eventType">
                {{ d.event.eventName }}
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in periods" :key="p.period">
            <td class="live-period-cell">
              <div class="live-period-name">{{ p.name }}</div>
              <div class="live-period-time">{{ p.startTime }}<br>{{ p.endTime }}</div>
            </td>
            <td
              v-for="d in weekDates"
              :key="d.dateStr + '-' + p.period"
              :class="['live-cell', { 'is-today': d.isToday, 'is-holiday': d.isFullHoliday }]"
            >
              <template v-if="d.isFullHoliday">
                <!-- 整天假日：显示假期横幅 -->
              </template>
              <template v-else>
                <div
                  v-for="inst in instancesAt(d.dateStr, p.period)"
                  :key="inst.id"
                  :class="['live-entry', 'status-' + inst.status]"
                  @click="showDetail(inst)"
                >
                  <div class="live-entry-title">
                    <span v-if="inst.status === 1" class="live-status-tag tag-cancel">取消</span>
                    <span v-else-if="inst.status === 2" class="live-status-tag tag-move">已调走</span>
                    <span v-else-if="inst.status === 3" class="live-status-tag tag-makeup">调入</span>
                    <span v-else-if="inst.status === 4" class="live-status-tag tag-sub">代课</span>
                    {{ inst.courseName }}
                  </div>
                  <!-- 调走链接：显示去哪 -->
                  <div v-if="parseLink(inst.movedTo)" class="live-entry-link link-out">
                    → 去 {{ formatLink(parseLink(inst.movedTo)!) }}
                  </div>
                  <!-- 调入链接：显示来自哪 -->
                  <div v-if="parseLink(inst.movedFrom)" class="live-entry-link link-in">
                    ← 来自 {{ formatLink(parseLink(inst.movedFrom)!) }}
                  </div>
                  <!-- 班级视图: 教师 + 教室 -->
                  <template v-if="viewType === 'class' && inst.status !== 2">
                    <div v-if="inst.status === 4 && inst.originalTeacherName" class="live-entry-meta live-entry-sub">
                      {{ inst.originalTeacherName }} → {{ inst.teacherName }}
                    </div>
                    <div v-else class="live-entry-meta">{{ inst.teacherName || '' }}</div>
                    <div class="live-entry-meta">{{ inst.classroomName || '' }}</div>
                  </template>
                  <!-- 教师视图: 班级 + 教室 -->
                  <template v-else-if="viewType === 'teacher' && inst.status !== 2">
                    <div class="live-entry-meta">{{ inst.className || '' }}</div>
                    <div class="live-entry-meta">{{ inst.classroomName || '' }}</div>
                  </template>
                  <!-- 教室视图: 班级 + 教师 -->
                  <template v-else-if="viewType === 'classroom' && inst.status !== 2">
                    <div class="live-entry-meta">{{ inst.className || '' }}</div>
                    <div v-if="inst.status === 4 && inst.originalTeacherName" class="live-entry-meta live-entry-sub">
                      {{ inst.originalTeacherName }} → {{ inst.teacherName }}
                    </div>
                    <div v-else class="live-entry-meta">{{ inst.teacherName || '' }}</div>
                  </template>
                  <div v-if="inst.cancelReason" class="live-entry-reason">💬 {{ inst.cancelReason }}</div>
                </div>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 最近变动 -->
    <div v-if="targetId && recentAdjustments.length > 0" class="live-feed">
      <div class="live-feed-header">
        <h3>📋 最近变动</h3>
        <span>共 {{ recentAdjustments.length }} 条</span>
      </div>
      <div v-for="adj in recentAdjustments" :key="adj.id" class="live-feed-item" :class="'feed-' + adj.adjustmentType">
        <span class="live-feed-type">{{ adjustmentTypeLabel(adj.adjustmentType) }}</span>
        <span class="live-feed-time">{{ formatTime(adj.applyTime) }}</span>
        <span class="live-feed-desc">{{ adj.applyReason || '' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http as request } from '@/utils/request'
import { universalPlaceApi } from '@/api/universalPlace'
import { instanceApi, periodConfigApi } from '@/api/teaching'
import { semesterApi, academicEventApi } from '@/api/calendar'
import type { PeriodConfig } from '@/types/teaching'
import { DEFAULT_PERIODS } from '@/types/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()

const viewType = ref<'class' | 'teacher' | 'classroom'>('class')
const targetId = ref<number | string>()
const instances = ref<any[]>([])
const weeks = ref<any[]>([]) // academic_weeks
const events = ref<any[]>([]) // academic_events
const recentAdjustments = ref<any[]>([])
const loading = ref(false)
const weekNumber = ref(1)
const periods = ref<PeriodConfig[]>(DEFAULT_PERIODS)

const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])
const classroomList = ref<{ id: number; name: string }[]>([])

const targetOptions = computed(() =>
  viewType.value === 'class' ? classList.value
  : viewType.value === 'teacher' ? teacherList.value
  : classroomList.value
)
const targetPlaceholder = computed(() => ({ class: '选择班级', teacher: '选择教师', classroom: '选择教室' }[viewType.value] || '请选择'))

const today = new Date().toISOString().slice(0, 10)
const todayLabel = computed(() => {
  const d = new Date(today)
  return `${d.getMonth() + 1}/${d.getDate()}`
})

// 当前周的日期数组（每天一个对象）
const weekDates = computed(() => {
  const w = weeks.value.find(x => x.weekNumber === weekNumber.value)
  if (!w) return []
  const start = new Date(w.startDate)
  const result: Array<{
    dateStr: string
    weekday: number
    monthDay: string
    isToday: boolean
    isFullHoliday: boolean
    event: any | null
    eventType: string
  }> = []
  // 只显示工作日（周一到周日，或根据配置）
  const showDays = 7
  for (let i = 0; i < showDays; i++) {
    const d = new Date(start.getTime() + i * 86400000)
    const dateStr = d.toISOString().slice(0, 10)
    const weekday = d.getDay() === 0 ? 7 : d.getDay()
    // 判断当天是否有事件（假期/考试/调课）
    const dayEvents = events.value.filter(e => {
      if (!e.startDate) return false
      return e.startDate <= dateStr && (e.endDate || e.startDate) >= dateStr
    })
    const holidayEvent = dayEvents.find(e => String(e.eventType).toUpperCase() === 'HOLIDAY')
    const examEvent = dayEvents.find(e => String(e.eventType).toUpperCase() === 'EXAM')
    const otherEvent = dayEvents[0]
    const mainEvent = holidayEvent || examEvent || otherEvent || null
    let eventType = 'none'
    if (holidayEvent) eventType = 'holiday'
    else if (examEvent) eventType = 'exam'
    else if (mainEvent) eventType = 'other'
    result.push({
      dateStr,
      weekday,
      monthDay: `${d.getMonth() + 1}/${d.getDate()}`,
      isToday: dateStr === today,
      isFullHoliday: !!holidayEvent,
      event: mainEvent,
      eventType,
    })
  }
  return result
})

const weekRangeLabel = computed(() => {
  const dates = weekDates.value
  if (dates.length === 0) return ''
  return `${dates[0].monthDay} ~ ${dates[dates.length - 1].monthDay}`
})

const currentWeekTag = computed(() => {
  const w = weeks.value.find(x => x.weekNumber === weekNumber.value)
  if (!w) return null
  if (w.weekType === 2) return { type: 'exam', label: '🎯 考试周' }
  if (w.weekType === 3) return { type: 'holi', label: '🏖 假期周' }
  return null
})

function weekClass(w: any) {
  const classes: string[] = []
  if (w.weekNumber === weekNumber.value) classes.push('active')
  if (w.weekType === 2) classes.push('week-exam')
  else if (w.weekType === 3) classes.push('week-holi')
  else classes.push('week-teach')
  // 今天所在的周
  const wStart = new Date(w.startDate)
  const wEnd = new Date(w.endDate)
  const t = new Date(today)
  if (t >= wStart && t <= wEnd) classes.push('is-current')
  return classes.join(' ')
}

function weekTypeLabel(t: number) {
  return t === 2 ? '考试周' : t === 3 ? '假期周' : '教学周'
}

function weekdayName(w: number) {
  return ['一', '二', '三', '四', '五', '六', '日'][w - 1] || ''
}

function jumpToToday() {
  // 找今天所在周
  const w = weeks.value.find((x: any) => {
    const s = new Date(x.startDate)
    const e = new Date(x.endDate)
    const t = new Date(today)
    return t >= s && t <= e
  })
  if (w) {
    weekNumber.value = w.weekNumber
    loadInstances()
  }
}

function jumpToWeek(n: number) {
  weekNumber.value = n
  loadInstances()
}

function onTypeChange(t: typeof viewType.value) {
  viewType.value = t
  targetId.value = undefined
  instances.value = []
}

/** 获取某天某节次的实况条目 */
function instancesAt(dateStr: string, period: number): any[] {
  return instances.value.filter(i =>
    i.actualDate === dateStr && i.startSlot <= period && i.endSlot >= period
  )
}

async function loadInstances() {
  if (!props.semesterId || !targetId.value) { instances.value = []; return }
  loading.value = true
  try {
    const params: any = { semesterId: props.semesterId, weekNumber: weekNumber.value }
    if (viewType.value === 'class') params.orgUnitId = targetId.value
    else if (viewType.value === 'teacher') params.teacherId = targetId.value
    else params.classroomId = targetId.value
    const res = await instanceApi.list(params)
    instances.value = (res as any).data || res || []
    await loadRecentAdjustments()
  } catch { instances.value = [] } finally { loading.value = false }
}

async function loadRecentAdjustments() {
  if (!props.semesterId) { recentAdjustments.value = []; return }
  try {
    const res = await request.get('/teaching/adjustments', { params: { semesterId: props.semesterId, size: 10 } })
    const data = (res as any).data || res
    recentAdjustments.value = data?.records || (Array.isArray(data) ? data : [])
  } catch { recentAdjustments.value = [] }
}

function adjustmentTypeLabel(t: number) {
  return ({ 1: '调课', 2: '停课', 3: '补课', 4: '代课' } as any)[t] || '变动'
}

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

function showDetail(inst: any) {
  const parts = [inst.courseName]
  if (inst.teacherName) parts.push('👨‍🏫 ' + inst.teacherName)
  if (inst.classroomName) parts.push('📍 ' + inst.classroomName)
  if (inst.cancelReason) parts.push('💬 ' + inst.cancelReason)
  const to = parseLink(inst.movedTo); if (to) parts.push('→ 去 ' + formatLink(to))
  const from = parseLink(inst.movedFrom); if (from) parts.push('← 来自 ' + formatLink(from))
  ElMessage.info(parts.join(' | '))
}

/** 解析后端返回的 JSON 字符串或对象 */
function parseLink(raw: any): { date: string; slot: number; weekday: number } | null {
  if (!raw) return null
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!obj.date || !obj.slot) return null
    return obj
  } catch { return null }
}

function formatLink(link: { date: string; slot: number; weekday: number }): string {
  const d = new Date(link.date)
  const monthDay = `${d.getMonth() + 1}/${d.getDate()}`
  return `${monthDay}(${weekdayName(link.weekday)}) 第${link.slot}节`
}

// ==================== Lifecycle ====================

async function loadWeeks() {
  if (!props.semesterId) { weeks.value = []; return }
  try {
    const res = await semesterApi.getWeeks(props.semesterId)
    const data = (res as any).data || res
    weeks.value = Array.isArray(data) ? data : []
  } catch { weeks.value = [] }
}

async function loadEvents() {
  if (!props.semesterId) { events.value = []; return }
  try {
    const res = await academicEventApi.list({ semesterId: props.semesterId })
    const data = (res as any).data || res
    events.value = Array.isArray(data) ? data : []
  } catch { events.value = [] }
}

async function loadPeriodConfig() {
  if (!props.semesterId) return
  try {
    const res: any = await periodConfigApi.list(props.semesterId)
    const configs = Array.isArray(res) ? res : (res.data || res)
    if (!Array.isArray(configs) || configs.length === 0) return
    const config = configs.find((c: any) => c.isDefault) || configs[0]
    const rawPeriods = typeof config.periods === 'string' ? JSON.parse(config.periods) : config.periods
    if (!Array.isArray(rawPeriods)) return
    const classPeriods = rawPeriods.filter((p: any) => p.type === 'class' && p.period)
    if (classPeriods.length > 0) {
      periods.value = classPeriods.map((p: any) => ({ period: p.period, name: p.name, startTime: p.startTime, endTime: p.endTime }))
    }
  } catch { /* defaults */ }
}

async function loadOptions() {
  try {
    const r = await request.get('/org-units/tree'); const d = (r as any).data || r
    const classes: { id: number; name: string }[] = []
    function walk(nodes: any[]) { for (const n of nodes) { if (n.unitType === 'CLASS') classes.push({ id: n.id, name: n.unitName }); if (n.children) walk(n.children) } }
    walk(Array.isArray(d) ? d : [])
    classList.value = classes
  } catch { classList.value = [] }
  try {
    const r = await request.get('/teaching/schedule-teachers'); const d = (r as any).data || r
    teacherList.value = (Array.isArray(d) ? d : []).map((t: any) => ({ id: t.id, name: t.realName || t.username }))
  } catch { teacherList.value = [] }
  try {
    const allItems = await universalPlaceApi.getFlatList()
    classroomList.value = allItems.filter((p: any) => (p.capacity || 0) > 0 && (p.capacity || 0) < 1000)
      .map((p: any) => ({ id: p.id, name: p.placeCode || p.placeName || p.name }))
  } catch { classroomList.value = [] }
}

watch(() => props.semesterId, async (v) => {
  if (!v) return
  await Promise.all([loadWeeks(), loadEvents(), loadPeriodConfig(), loadOptions()])
  // 默认跳到今天所在周
  jumpToToday()
}, { immediate: true })
</script>

<style scoped>
.live-tt { padding: 0; }

/* 时间轴 */
.live-timeline {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; margin-bottom: 12px;
  border: 1px solid #e5e7eb; border-radius: 10px; background: #fff;
}
.tt-today-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 12px; border: 1px solid #2563eb; border-radius: 99px;
  background: #eff6ff; color: #1d4ed8;
  font-size: 12px; font-weight: 600; cursor: pointer;
  transition: all 0.15s;
}
.tt-today-btn:hover { background: #dbeafe; }
.tt-today-dot { width: 6px; height: 6px; border-radius: 50%; background: #2563eb; animation: pulse 2s ease-in-out infinite; }
@keyframes pulse { 50% { opacity: 0.4; } }

.tt-weeks {
  flex: 1; display: flex; gap: 2px; flex-wrap: wrap;
}
.tt-week {
  width: 28px; height: 24px;
  display: flex; align-items: center; justify-content: center;
  font-size: 10.5px; font-weight: 500;
  border-radius: 4px; cursor: pointer;
  transition: all 0.15s;
  position: relative;
}
.tt-week:hover { transform: translateY(-1px); box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.tt-week.week-teach { background: #f0fdf4; color: #16a34a; }
.tt-week.week-exam { background: #f5f3ff; color: #7c3aed; }
.tt-week.week-holi { background: #fef2f2; color: #dc2626; }
.tt-week.active { outline: 2px solid #2563eb; outline-offset: 1px; font-weight: 700; }
.tt-week.is-current::after {
  content: ''; position: absolute; bottom: -3px; left: 50%; transform: translateX(-50%);
  width: 4px; height: 4px; border-radius: 50%; background: #2563eb;
}

.tt-legend { display: flex; gap: 10px; font-size: 11px; color: #6b7280; }
.tt-legend-item { display: inline-flex; align-items: center; gap: 4px; }
.tt-legend-dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; }
.tt-legend-dot.tt-teach { background: #f0fdf4; border: 1px solid #bbf7d0; }
.tt-legend-dot.tt-exam { background: #f5f3ff; border: 1px solid #ddd6fe; }
.tt-legend-dot.tt-holi { background: #fef2f2; border: 1px solid #fecaca; }

/* 控制栏 */
.live-controls {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 12px 16px; margin-bottom: 12px;
  border: 1px solid #e5e7eb; border-radius: 10px; background: #fff;
}
.live-sep { display: inline-block; width: 1px; height: 18px; background: #d1d5db; }
.live-week-pager { display: inline-flex; align-items: center; gap: 4px; border: 1px solid #d1d5db; border-radius: 6px; overflow: hidden; }
.live-arrow {
  width: 28px; height: 28px; border: none; background: #fff; color: #6b7280;
  cursor: pointer; font-size: 11px;
}
.live-arrow:hover:not(:disabled) { background: #f3f4f6; color: #111827; }
.live-arrow:disabled { opacity: 0.3; cursor: not-allowed; }
.live-week-label {
  padding: 4px 10px; font-size: 12.5px; font-weight: 600; color: #111827;
  display: inline-flex; align-items: center; gap: 6px;
  border-left: 1px solid #e5e7eb; border-right: 1px solid #e5e7eb;
}
.live-week-range { font-weight: 400; font-size: 11px; color: #9ca3af; }

.live-tag {
  padding: 3px 10px; border-radius: 99px;
  font-size: 11px; font-weight: 500;
}
.live-tag.tag-exam { background: #f5f3ff; color: #7c3aed; border: 1px solid #ddd6fe; }
.live-tag.tag-holi { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }

/* 课表网格 */
.live-empty { text-align: center; padding: 60px 40px; color: #9ca3af; font-size: 13px; }
.live-grid-wrap {
  border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: auto;
}
.live-grid {
  width: 100%; border-collapse: collapse; table-layout: fixed;
  font-family: 'DM Sans', system-ui, sans-serif;
}

/* 表头 */
.live-grid thead th {
  padding: 8px 6px; font-size: 12px; font-weight: 600; color: #374151;
  background: #f9fafb; border-bottom: 2px solid #e5e7eb; text-align: center;
}
.live-period-col { width: 76px; background: #fafbfc !important; }
.live-day-col { min-width: 130px; position: relative; }
.live-day-col.is-today { background: #eff6ff !important; }
.live-day-col.is-holiday { background: #fef2f2 !important; }
.live-day-wd { font-size: 13px; font-weight: 600; color: #374151; }
.live-day-wd.is-weekend { color: #9ca3af; }
.live-day-col.is-today .live-day-wd { color: #2563eb; }
.live-day-date { font-size: 11px; color: #9ca3af; margin-top: 1px; }
.live-day-event {
  margin-top: 3px; padding: 2px 6px; border-radius: 4px;
  font-size: 10px; font-weight: 500;
}
.live-day-event.event-holiday { background: #fee2e2; color: #dc2626; }
.live-day-event.event-exam { background: #ede9fe; color: #7c3aed; }
.live-day-event.event-other { background: #fef3c7; color: #d97706; }

/* 节次列 */
.live-period-cell {
  padding: 6px 4px; text-align: center;
  background: #fafbfc; border-right: 1px solid #e5e7eb; border-bottom: 1px solid #f3f4f6;
}
.live-period-name { font-size: 12px; font-weight: 600; color: #374151; }
.live-period-time { font-size: 10px; color: #9ca3af; margin-top: 2px; line-height: 1.2; }

/* 数据格 */
.live-cell {
  padding: 4px; border-right: 1px solid #f3f4f6; border-bottom: 1px solid #f3f4f6;
  vertical-align: top; min-height: 54px; height: 54px;
  position: relative;
}
.live-cell:last-child { border-right: none; }
.live-cell.is-today { background: rgba(239, 246, 255, 0.4); }
.live-cell.is-holiday {
  background: repeating-linear-gradient(
    -45deg, #fef2f2, #fef2f2 5px, #fee2e2 5px, #fee2e2 10px
  );
}

/* 课程条目 */
.live-entry {
  padding: 4px 6px; margin-bottom: 2px;
  border-radius: 5px;
  background: #dbeafe;
  border-left: 3px solid #3b82f6;
  color: #1e40af;
  cursor: pointer; transition: all 0.15s;
  overflow: hidden;
}
.live-entry:hover { transform: translateY(-1px); box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
.live-entry:last-child { margin-bottom: 0; }

.live-entry.status-0 { /* 正常 - 默认蓝色 */ }
.live-entry.status-1 {
  background: #fee2e2; border-color: #dc2626; color: #991b1b;
  opacity: 0.6; text-decoration: line-through;
}
.live-entry.status-2 {
  background: #f3f4f6; border-color: #9ca3af; color: #6b7280;
  opacity: 0.6;
}
.live-entry.status-3 {
  background: #dcfce7; border-color: #16a34a; color: #14532d;
}
.live-entry.status-4 {
  background: #ffedd5; border-color: #ea580c; color: #7c2d12;
}

.live-entry-title {
  font-size: 11.5px; font-weight: 600; line-height: 1.3;
  display: flex; align-items: center; gap: 4px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.live-entry-meta {
  font-size: 10px; line-height: 1.3;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  opacity: 0.85;
}
.live-entry-reason {
  font-size: 9.5px; color: #6b7280; margin-top: 2px;
  font-style: italic;
}
/* 调课链接：去哪 / 来自哪 */
.live-entry-link {
  font-size: 10px; margin-top: 1px; font-weight: 500;
  padding: 1px 4px; border-radius: 3px;
  display: inline-block;
}
.live-entry-link.link-out {
  background: rgba(107,114,128,0.15); color: #6b7280;
}
.live-entry-link.link-in {
  background: rgba(22,163,74,0.12); color: #16a34a;
}
/* 代课：原教师→代课教师 */
.live-entry-sub {
  color: #ea580c !important; font-weight: 500;
}

/* 状态标签 */
.live-status-tag {
  display: inline-block; padding: 0 4px;
  font-size: 9px; font-weight: 700;
  border-radius: 2px; color: #fff;
  flex-shrink: 0;
}
.tag-cancel { background: #dc2626; }
.tag-move { background: #6b7280; }
.tag-makeup { background: #16a34a; }
.tag-sub { background: #ea580c; }

/* 最近变动 */
.live-feed {
  margin-top: 16px; border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;
}
.live-feed-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; border-bottom: 1px solid #f3f4f6;
  background: #fafafa;
}
.live-feed-header h3 { font-size: 13px; font-weight: 600; color: #111827; margin: 0; }
.live-feed-header span { font-size: 11px; color: #6b7280; }
.live-feed-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 16px; border-bottom: 1px solid #f9fafb;
  font-size: 12px;
}
.live-feed-item:last-child { border-bottom: none; }
.live-feed-type {
  padding: 2px 8px; border-radius: 99px;
  font-size: 11px; font-weight: 500;
  background: #eff6ff; color: #2563eb;
}
.live-feed-item.feed-1 .live-feed-type { background: #fef3c7; color: #d97706; }
.live-feed-item.feed-2 .live-feed-type { background: #fee2e2; color: #dc2626; }
.live-feed-item.feed-3 .live-feed-type { background: #dcfce7; color: #16a34a; }
.live-feed-item.feed-4 .live-feed-type { background: #ffedd5; color: #ea580c; }
.live-feed-time { color: #9ca3af; font-size: 11px; min-width: 90px; }
.live-feed-desc { flex: 1; color: #374151; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
