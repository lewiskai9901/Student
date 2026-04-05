<template>
  <div>
    <!-- Controls -->
    <div class="mb-4 rounded-xl border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-center gap-3">
        <!-- View type radio -->
        <el-radio-group v-model="timetableViewType" size="small" @change="onTimetableTypeChange">
          <el-radio-button value="class">班级课表</el-radio-button>
          <el-radio-button value="teacher">教师课表</el-radio-button>
          <el-radio-button value="classroom">教室课表</el-radio-button>
        </el-radio-group>

        <!-- Target selector -->
        <el-select
          v-model="timetableTargetId"
          :placeholder="timetableTargetPlaceholder"
          class="w-48"
          size="small"
          filterable
          clearable
          @change="loadTimetable"
        >
          <el-option
            v-for="opt in timetableOptions"
            :key="opt.id"
            :value="opt.id"
            :label="opt.name"
          />
        </el-select>

        <div class="h-5 w-px bg-gray-200" />

        <!-- Week selector -->
        <el-select v-model="timetableWeek" placeholder="周次" class="w-28" size="small" clearable>
          <el-option v-for="w in 20" :key="w" :value="w" :label="'第' + w + '周'" />
        </el-select>

        <!-- Odd/even week filter -->
        <el-radio-group v-model="timetableWeekType" size="small">
          <el-radio-button :value="0">全部</el-radio-button>
          <el-radio-button :value="1">仅单周</el-radio-button>
          <el-radio-button :value="2">仅双周</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Timetable grid -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <TimetableGrid
        :entries="filteredTimetableEntries"
        :periods="periods"
        :editable="true"
        @entry-click="showEntryDetail"
        @entry-drop="onEntryDrop"
      />
    </div>

    <!-- Bottom stats -->
    <div v-if="timetableTargetId" class="mt-3 flex items-center gap-4 text-xs text-gray-500">
      <span>本周 <span class="font-semibold text-gray-700">{{ timetableWeeklyCount }}</span> 节课</span>
      <div class="h-3 w-px bg-gray-200" />
      <span><span class="font-semibold text-gray-700">{{ timetableDayCount }}</span> 天有课</span>
    </div>

    <!-- Entry detail dialog -->
    <el-dialog v-model="entryDetailVisible" title="课程详情" width="400px">
      <div v-if="selectedEntry" class="space-y-2 text-sm">
        <div class="flex"><span class="w-20 text-gray-500">课程:</span><span class="font-medium">{{ selectedEntry.courseName }}</span></div>
        <div class="flex"><span class="w-20 text-gray-500">教师:</span><span>{{ selectedEntry.teacherName || '-' }}</span></div>
        <div class="flex"><span class="w-20 text-gray-500">班级:</span><span>{{ selectedEntry.className || '-' }}</span></div>
        <div class="flex"><span class="w-20 text-gray-500">教室:</span><span>{{ selectedEntry.classroomName || '-' }}</span></div>
        <div class="flex"><span class="w-20 text-gray-500">时间:</span><span>{{ getWeekdayName(selectedEntry.dayOfWeek) }} 第{{ selectedEntry.periodStart }}-{{ selectedEntry.periodEnd }}节</span></div>
        <div class="flex"><span class="w-20 text-gray-500">周次:</span><span>{{ formatWeeks(selectedEntry) }}</span></div>
      </div>
    </el-dialog>
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

// ==================== Props ====================

const props = defineProps<{
  semesterId: number | string | undefined
}>()

// ==================== Constants ====================

const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS

// ==================== State ====================

const timetableViewType = ref<'class' | 'teacher' | 'classroom'>('class')
const timetableTargetId = ref<number | string>()
const timetableEntries = ref<ScheduleEntry[]>([])
const timetableWeek = ref<number>()
const timetableWeekType = ref<number>(0)

// Options data
const classrooms = ref<{ id: number; name: string }[]>([])
const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])

// Entry detail
const entryDetailVisible = ref(false)
const selectedEntry = ref<ScheduleEntry | null>(null)

// ==================== Computed ====================

const timetableTargetPlaceholder = computed(() => {
  const m: Record<string, string> = { class: '选择班级', teacher: '选择教师', classroom: '选择教室' }
  return m[timetableViewType.value]
})

const timetableOptions = computed(() => {
  if (timetableViewType.value === 'class') return classList.value
  if (timetableViewType.value === 'teacher') return teacherList.value
  return classrooms.value
})

const filteredTimetableEntries = computed(() => {
  let entries = timetableEntries.value
  // Filter by week
  if (timetableWeek.value) {
    const w = timetableWeek.value
    entries = entries.filter(e => e.weekStart <= w && e.weekEnd >= w)
    // Filter by odd/even
    if (timetableWeekType.value === 1) {
      entries = entries.filter(e => e.weekType !== 2) // not even-only
    } else if (timetableWeekType.value === 2) {
      entries = entries.filter(e => e.weekType !== 1) // not odd-only
    }
  } else if (timetableWeekType.value === 1) {
    entries = entries.filter(e => e.weekType !== 2)
  } else if (timetableWeekType.value === 2) {
    entries = entries.filter(e => e.weekType !== 1)
  }
  return entries
})

const timetableWeeklyCount = computed(() => filteredTimetableEntries.value.length)

const timetableDayCount = computed(() => {
  const days = new Set(filteredTimetableEntries.value.map(e => e.dayOfWeek))
  return days.size
})

// ==================== Data Loading ====================

async function loadClassrooms() {
  try {
    const res = await request.get('/v9/places', { params: { typeCode: 'CLASSROOM' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classrooms.value = items.map((p: any) => ({ id: p.id, name: p.placeName || p.name }))
  } catch (e) {
    console.error('Failed to load classrooms:', e)
  }
}

async function loadClassList() {
  try {
    const res = await request.get('/organization/classes/list')
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classList.value = items.map((c: any) => ({ id: c.id, name: c.className || c.name }))
  } catch (e) {
    console.error('Failed to load class list:', e)
  }
}

async function loadTeacherList() {
  try {
    const res = await request.get('/users', { params: { role: 'TEACHER' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    teacherList.value = items.map((t: any) => ({ id: t.id, name: t.realName || t.username || t.name }))
  } catch (e) {
    console.error('Failed to load teacher list:', e)
  }
}

async function loadTimetable() {
  if (!timetableTargetId.value || !props.semesterId) return
  try {
    let res
    if (timetableViewType.value === 'class') {
      res = await scheduleApi.getByClass(timetableTargetId.value, props.semesterId)
    } else if (timetableViewType.value === 'teacher') {
      res = await scheduleApi.getByTeacher(timetableTargetId.value, props.semesterId)
    } else {
      res = await scheduleApi.getByClassroom(timetableTargetId.value, props.semesterId)
    }
    timetableEntries.value = (res as any).data || res
  } catch (e) {
    console.error('Failed to load timetable:', e)
  }
}

async function loadOptions() {
  await Promise.all([loadClassrooms(), loadClassList(), loadTeacherList()])
}

// ==================== Actions ====================

function onTimetableTypeChange() {
  timetableTargetId.value = undefined
  timetableEntries.value = []
}

function showEntryDetail(entry: ScheduleEntry) {
  selectedEntry.value = entry
  entryDetailVisible.value = true
}

async function onEntryDrop(entryId: number, newDay: number, newPeriod: number) {
  if (!props.semesterId) return
  try {
    const check = await scheduleApi.checkMoveConflict({
      entryId,
      semesterId: Number(props.semesterId),
      dayOfWeek: newDay,
      periodStart: newPeriod,
    })
    const checkData = (check as any).data || check
    if (checkData.hasConflict) {
      ElMessage.warning('无法移动: 存在时间冲突')
      return
    }
    await scheduleApi.moveEntry(entryId, {
      semesterId: Number(props.semesterId),
      dayOfWeek: newDay,
      periodStart: newPeriod,
    })
    ElMessage.success('课程已移动')
    loadTimetable()
  } catch (e: any) {
    ElMessage.error('移动失败')
  }
}

// ==================== Helpers ====================

function getWeekdayName(day: number) {
  return weekdays.find(w => w.value === day)?.label || ''
}

function formatWeeks(entry: ScheduleEntry) {
  const weekTypeText = entry.weekType === 1 ? '(单)' : entry.weekType === 2 ? '(双)' : ''
  return `${entry.weekStart}-${entry.weekEnd}周${weekTypeText}`
}

// ==================== Watchers ====================

watch(() => props.semesterId, (val) => {
  if (val) {
    loadOptions()
  }
  timetableEntries.value = []
  timetableTargetId.value = undefined
}, { immediate: true })
</script>
