<template>
  <div>
    <!-- Controls -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 14px 20px; margin-bottom: 16px;">
      <div style="display: flex; flex-wrap: wrap; align-items: center; gap: 10px;">
        <!-- View type radio -->
        <div class="tm-radios" style="width: auto;">
          <label :class="['tm-radio', { active: timetableViewType === 'class' }]" @click="timetableViewType = 'class'; onTimetableTypeChange()"><input type="radio" />班级课表</label>
          <label :class="['tm-radio', { active: timetableViewType === 'teacher' }]" @click="timetableViewType = 'teacher'; onTimetableTypeChange()"><input type="radio" />教师课表</label>
          <label :class="['tm-radio', { active: timetableViewType === 'classroom' }]" @click="timetableViewType = 'classroom'; onTimetableTypeChange()"><input type="radio" />教室课表</label>
        </div>

        <!-- Target selector -->
        <select v-model="timetableTargetId" class="tm-select" @change="loadTimetable">
          <option :value="undefined" disabled>{{ timetableTargetPlaceholder }}</option>
          <option v-for="opt in timetableOptions" :key="opt.id" :value="opt.id">{{ opt.name }}</option>
        </select>

        <i style="display: inline-block; width: 1px; height: 18px; background: #d1d5db;" />

        <!-- Week selector -->
        <select v-model="timetableWeek" class="tm-select">
          <option :value="undefined">全部周次</option>
          <option v-for="w in 20" :key="w" :value="w">第{{ w }}周</option>
        </select>

        <!-- Odd/even week filter -->
        <div class="tm-radios" style="width: auto;">
          <label :class="['tm-radio', { active: timetableWeekType === 0 }]" @click="timetableWeekType = 0"><input type="radio" />全部</label>
          <label :class="['tm-radio', { active: timetableWeekType === 1 }]" @click="timetableWeekType = 1"><input type="radio" />仅单周</label>
          <label :class="['tm-radio', { active: timetableWeekType === 2 }]" @click="timetableWeekType = 2"><input type="radio" />仅双周</label>
        </div>
      </div>
    </div>

    <!-- Timetable grid -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
      <TimetableGrid
        :entries="filteredTimetableEntries"
        :periods="periods"
        :editable="true"
        @entry-click="showEntryDetail"
        @entry-drop="onEntryDrop"
      />
    </div>

    <!-- Bottom stats -->
    <div v-if="timetableTargetId" class="tm-stats" style="margin-top: 10px;">
      <span>本周 <b>{{ timetableWeeklyCount }}</b> 节课</span>
      <span class="sep" />
      <span><b>{{ timetableDayCount }}</b> 天有课</span>
    </div>

    <!-- Entry detail drawer -->
    <Transition name="tm-drawer">
      <div v-if="entryDetailVisible" class="tm-drawer-overlay" @click.self="entryDetailVisible = false">
        <div class="tm-drawer" style="width: 380px;">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">课程详情</h3>
            <button class="tm-drawer-close" @click="entryDetailVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div v-if="selectedEntry" class="tm-section">
              <div class="detail-row"><span class="detail-label">课程:</span><span style="font-weight: 500;">{{ selectedEntry.courseName }}</span></div>
              <div class="detail-row"><span class="detail-label">教师:</span><span>{{ selectedEntry.teacherName || '-' }}</span></div>
              <div class="detail-row"><span class="detail-label">班级:</span><span>{{ selectedEntry.className || '-' }}</span></div>
              <div class="detail-row"><span class="detail-label">教室:</span><span>{{ selectedEntry.classroomName || '-' }}</span></div>
              <div class="detail-row"><span class="detail-label">时间:</span><span>{{ getWeekdayName(selectedEntry.dayOfWeek) }} 第{{ selectedEntry.periodStart }}-{{ selectedEntry.periodEnd }}节</span></div>
              <div class="detail-row"><span class="detail-label">周次:</span><span>{{ formatWeeks(selectedEntry) }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http as request } from '@/utils/request'
import { scheduleApi } from '@/api/teaching'
import type { ScheduleEntry } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'
import TimetableGrid from '../scheduling/TimetableGrid.vue'

const props = defineProps<{ semesterId: number | string | undefined }>()
const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS

const timetableViewType = ref<'class' | 'teacher' | 'classroom'>('class')
const timetableTargetId = ref<number | string>()
const timetableEntries = ref<ScheduleEntry[]>([])
const timetableWeek = ref<number>()
const timetableWeekType = ref<number>(0)

const classrooms = ref<{ id: number; name: string }[]>([])
const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])

const entryDetailVisible = ref(false)
const selectedEntry = ref<ScheduleEntry | null>(null)

const timetableTargetPlaceholder = computed(() => ({ class: '选择班级', teacher: '选择教师', classroom: '选择教室' }[timetableViewType.value]))
const timetableOptions = computed(() => timetableViewType.value === 'class' ? classList.value : timetableViewType.value === 'teacher' ? teacherList.value : classrooms.value)

const filteredTimetableEntries = computed(() => {
  let entries = timetableEntries.value
  if (timetableWeek.value) {
    const w = timetableWeek.value
    entries = entries.filter(e => e.weekStart <= w && e.weekEnd >= w)
    if (timetableWeekType.value === 1) entries = entries.filter(e => e.weekType !== 2)
    else if (timetableWeekType.value === 2) entries = entries.filter(e => e.weekType !== 1)
  } else {
    if (timetableWeekType.value === 1) entries = entries.filter(e => e.weekType !== 2)
    else if (timetableWeekType.value === 2) entries = entries.filter(e => e.weekType !== 1)
  }
  return entries
})

const timetableWeeklyCount = computed(() => filteredTimetableEntries.value.length)
const timetableDayCount = computed(() => new Set(filteredTimetableEntries.value.map(e => e.dayOfWeek)).size)

async function loadClassrooms() {
  try { const res = await request.get('/places', { params: { roomType: 'CLASSROOM' } }); const data = (res as any).data || res; const items = Array.isArray(data) ? data : data.records || []; classrooms.value = items.map((p: any) => ({ id: p.id, name: p.placeName || p.name })) } catch { /* */ }
}
async function loadClassList() {
  try { const res = await request.get('/students/classes'); const data = (res as any).data || res; const items = Array.isArray(data) ? data : data.records || []; classList.value = items.map((c: any) => ({ id: c.id, name: c.className || c.name })) } catch { /* */ }
}
async function loadTeacherList() {
  try { const res = await request.get('/users', { params: { role: 'TEACHER' } }); const data = (res as any).data || res; const items = Array.isArray(data) ? data : data.records || []; teacherList.value = items.map((t: any) => ({ id: t.id, name: t.realName || t.username || t.name })) } catch { /* */ }
}

async function loadTimetable() {
  if (!timetableTargetId.value || !props.semesterId) return
  try {
    let res
    if (timetableViewType.value === 'class') res = await scheduleApi.getByClass(timetableTargetId.value, props.semesterId)
    else if (timetableViewType.value === 'teacher') res = await scheduleApi.getByTeacher(timetableTargetId.value, props.semesterId)
    else res = await scheduleApi.getByClassroom(timetableTargetId.value, props.semesterId)
    timetableEntries.value = (res as any).data || res
  } catch { /* */ }
}

function onTimetableTypeChange() { timetableTargetId.value = undefined; timetableEntries.value = [] }
function showEntryDetail(entry: ScheduleEntry) { selectedEntry.value = entry; entryDetailVisible.value = true }

async function onEntryDrop(entryId: number, newDay: number, newPeriod: number) {
  if (!props.semesterId) return
  try {
    const check = await scheduleApi.checkMoveConflict({ entryId, semesterId: props.semesterId, dayOfWeek: newDay, periodStart: newPeriod })
    const checkData = (check as any).data || check
    if (checkData.hasConflict) { ElMessage.warning('无法移动: 存在时间冲突'); return }
    await scheduleApi.moveEntry(entryId, { semesterId: props.semesterId, dayOfWeek: newDay, periodStart: newPeriod })
    ElMessage.success('课程已移动'); loadTimetable()
  } catch { ElMessage.error('移动失败') }
}

function getWeekdayName(day: number) { return weekdays.find(w => w.value === day)?.label || '' }
function formatWeeks(entry: ScheduleEntry) { return `${entry.weekStart}-${entry.weekEnd}周${entry.weekType === 1 ? '(单)' : entry.weekType === 2 ? '(双)' : ''}` }

watch(() => props.semesterId, (val) => { if (val) Promise.all([loadClassrooms(), loadClassList(), loadTeacherList()]); timetableEntries.value = []; timetableTargetId.value = undefined }, { immediate: true })
</script>

<style scoped>
.detail-row { display: flex; padding: 6px 0; font-size: 13px; border-bottom: 1px solid #f3f4f6; }
.detail-row:last-child { border-bottom: none; }
.detail-label { width: 60px; color: #6b7280; flex-shrink: 0; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
