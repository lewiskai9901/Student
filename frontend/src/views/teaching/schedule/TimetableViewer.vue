<template>
  <div class="tv-layout">
    <!-- Left tree (all modes) -->
    <div class="tv-tree">
      <ScheduleTree
        :mode="viewType === 'overview' ? overviewMode : viewType"
        :semester-id="semesterId"
        :multiple="viewType === 'overview'"
        @select="onTreeSelect"
        @multi-select="onMultiSelect"
      />
    </div>

    <!-- Main content -->
    <div class="tv-main">
      <!-- Controls -->
      <div class="tv-controls">
        <div class="tv-controls-row">
          <!-- View type -->
          <div class="tm-radios" style="width: auto;">
            <button :class="['tm-radio', { active: viewType === 'class' }]" @click="setViewType('class')">班级课表</button>
            <button :class="['tm-radio', { active: viewType === 'teacher' }]" @click="setViewType('teacher')">教师课表</button>
            <button :class="['tm-radio', { active: viewType === 'classroom' }]" @click="setViewType('classroom')">教室课表</button>
            <button :class="['tm-radio', { active: viewType === 'overview' }]" @click="setViewType('overview')">总览</button>
          </div>
          <!-- 总览模式下选择对象类型 -->
          <div v-if="viewType === 'overview'" class="tm-radios" style="width: auto;">
            <button :class="['tm-radio', { active: overviewMode === 'class' }]" @click="overviewMode = 'class'">按班级</button>
            <button :class="['tm-radio', { active: overviewMode === 'teacher' }]" @click="overviewMode = 'teacher'">按教师</button>
            <button :class="['tm-radio', { active: overviewMode === 'classroom' }]" @click="overviewMode = 'classroom'">按教室</button>
          </div>

          <!-- Current target display -->
          <span v-if="targetId" class="tv-current-target">{{ currentTargetName }}</span>

          <span class="tv-sep" />

          <!-- Week pager: 只在周次有差异时显示（有单双周 或 不同 start/end_week） -->
          <div v-if="weekPagerNeeded" class="tv-week-pager" :title="weekPagerHint">
            <button class="tv-week-btn" :disabled="!week || week <= 1" @click="week && week--">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6"/></svg>
            </button>
            <span class="tv-week-label" @click="week = undefined">{{ week ? `第${week}周` : '全部周次' }}</span>
            <button class="tv-week-btn" :disabled="week !== undefined && week >= 20" @click="week = (week ?? 0) + 1">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
            </button>
          </div>

          <!-- Odd/even week -->
          <div class="tm-radios" style="width: auto;" :class="{ 'tv-week-disabled': !hasOddEvenWeek }" :title="hasOddEvenWeek ? '' : '当前无单双周课程'">
            <label :class="['tm-radio', { active: weekType === 0 }]" @click="weekType = 0"><input type="radio" />全部</label>
            <label :class="['tm-radio', { active: weekType === 1, 'tm-radio-disabled': !hasOddWeek }]" @click="hasOddWeek && (weekType = 1)"><input type="radio" />仅单周</label>
            <label :class="['tm-radio', { active: weekType === 2, 'tm-radio-disabled': !hasEvenWeek }]" @click="hasEvenWeek && (weekType = 2)"><input type="radio" />仅双周</label>
          </div>
          <!-- 单双周醒目标注 -->
          <span v-if="oddCount > 0" class="tv-week-badge tv-week-badge-odd" :title="`${oddCount}门课程按单周排课`">
            <span class="tv-week-badge-dot" /> 单周 {{ oddCount }}
          </span>
          <span v-if="evenCount > 0" class="tv-week-badge tv-week-badge-even" :title="`${evenCount}门课程按双周排课`">
            <span class="tv-week-badge-dot" /> 双周 {{ evenCount }}
          </span>

          <span v-if="viewType === 'class'" class="tv-sep" />
          <button v-if="viewType === 'class'" :class="['tv-compare-btn', compareMode ? 'tv-compare-active' : '']" @click="toggleCompare">
            {{ compareMode ? '退出对比' : '对比' }}
          </button>
        </div>

        <!-- Compare target selector -->
        <div v-if="compareMode" class="tv-compare-bar">
          <span style="font-size: 12px; color: #6b7280;">对比班级：</span>
          <select v-model="compareTargetId" class="tm-select" style="min-width: 130px;" @change="loadCompareTimetable">
            <option :value="undefined" disabled>选择对比班级</option>
            <template v-for="dept in groupedClasses" :key="dept.name">
              <optgroup :label="dept.name">
                <option v-for="cls in dept.classes" :key="cls.id" :value="cls.id" :disabled="String(cls.id) === String(targetId)">{{ cls.name }}</option>
              </optgroup>
            </template>
          </select>
          <span v-if="compareTargetId" class="tv-current-target" style="font-size: 12px;">{{ compareTargetName }}</span>
        </div>
      </div>

      <!-- Matrix overview mode -->
      <TimetableMatrix
        v-if="viewType === 'overview'"
        :mode="overviewMode"
        :semester-id="semesterId"
        :periods="periods"
        :selected-targets="overviewSelected"
        data-source="schedule"
      />

      <!-- Timetable grids (single or compare) -->
      <div v-else :class="compareMode && compareTargetId ? 'tv-grid-compare' : ''">
        <div class="tv-grid-wrap" :style="compareMode && compareTargetId ? 'flex: 1;' : ''">
          <div v-if="compareMode && targetId" class="tv-grid-label">{{ currentTargetName }}</div>
          <TimetableGrid
            :entries="filteredEntries"
            :periods="periods"
            :editable="!compareMode"
            :view-type="viewType"
            @entry-click="showEntryDetail"
            @entry-drop="onEntryDrop"
            @cell-click="onCellClick"
          />
        </div>
        <div v-if="compareMode && compareTargetId" class="tv-grid-wrap" style="flex: 1;">
          <div class="tv-grid-label">{{ compareTargetName }}</div>
          <TimetableGrid
            :entries="filteredCompareEntries"
            :periods="periods"
            :editable="false"
            :view-type="viewType"
            @entry-click="showEntryDetail"
          />
        </div>
      </div>

      <!-- Bottom stats -->
      <div v-if="targetId" class="tm-stats" style="margin-top: 10px;">
        <span>本周 <b>{{ filteredEntries.length }}</b> 节课</span>
        <span class="sep" />
        <span><b>{{ dayCount }}</b> 天有课</span>
        <span class="sep" />
        <span>周课时 <b>{{ weeklyHours }}</b></span>
      </div>
    </div>

    <!-- Entry detail drawer -->
    <Teleport to="body">
      <Transition name="tm-drawer">
        <div v-if="detailVisible" class="tm-drawer-overlay" @click.self="detailVisible = false">
          <div class="tm-drawer" style="width: 420px;">
            <div class="tm-drawer-header">
              <h3 class="tm-drawer-title">课程详情</h3>
              <button class="tm-drawer-close" @click="detailVisible = false">&times;</button>
            </div>
            <div class="tm-drawer-body">
              <div v-if="selectedEntry" class="tm-section">
                <div class="detail-row"><span class="detail-label">课程:</span><span style="font-weight: 500;">{{ selectedEntry.courseName }}</span></div>
                <div class="detail-row"><span class="detail-label">教师:</span><span>{{ selectedEntry.teacherName || '-' }}</span></div>
                <div class="detail-row"><span class="detail-label">班级:</span><span>{{ selectedEntry.className || '-' }}</span></div>
                <div class="detail-row"><span class="detail-label">教室:</span><span>{{ selectedEntry.classroomName || '-' }}</span></div>
                <div class="detail-row"><span class="detail-label">时间:</span><span>{{ getWeekdayName(selectedEntry.dayOfWeek) }} 第{{ selectedEntry.periodStart }}-{{ selectedEntry.periodEnd }}节</span></div>
                <div class="detail-row"><span class="detail-label">周次范围:</span><span>{{ selectedEntry.weekStart }}-{{ selectedEntry.weekEnd }}周</span></div>
              </div>

              <!-- 锁定状态 -->
              <div v-if="selectedEntry && !compareMode" class="tm-section">
                <h4 class="tm-section-title">排课锁定</h4>
                <div style="display: flex; align-items: center; gap: 10px; padding: 10px; border: 1px solid #e5e7eb; border-radius: 8px; background: #fafafa;">
                  <span style="font-size: 20px;">{{ (selectedEntry as any).isLocked ? '🔒' : '🔓' }}</span>
                  <div style="flex: 1;">
                    <div style="font-size: 13px; font-weight: 500; color: #111827;">
                      {{ (selectedEntry as any).isLocked ? '已锁定' : '未锁定' }}
                    </div>
                    <div style="font-size: 11px; color: #6b7280; margin-top: 2px;">
                      {{ (selectedEntry as any).isLocked ? '自动排课不会覆盖此课' : '自动排课时可被调整' }}
                    </div>
                  </div>
                  <button class="tm-btn tm-btn-secondary" :disabled="lockSaving" @click="toggleLock">
                    {{ lockSaving ? '...' : ((selectedEntry as any).isLocked ? '解锁' : '锁定') }}
                  </button>
                </div>
              </div>

              <!-- 单双周编辑 -->
              <div v-if="selectedEntry && !compareMode" class="tm-section">
                <h4 class="tm-section-title">周次类型</h4>
                <div class="tm-radios" style="width: 100%;">
                  <label :class="['tm-radio', { active: editWeekType === 0 }]" @click="editWeekType = 0"><input type="radio" />每周</label>
                  <label :class="['tm-radio', { active: editWeekType === 1 }]" @click="editWeekType = 1"><input type="radio" />单周</label>
                  <label :class="['tm-radio', { active: editWeekType === 2 }]" @click="editWeekType = 2"><input type="radio" />双周</label>
                </div>
                <p style="font-size: 11px; color: #9ca3af; margin-top: 8px;">
                  单周：仅第1/3/5/7...周上课 | 双周：仅第2/4/6/8...周上课
                </p>
              </div>
            </div>
            <div v-if="selectedEntry && !compareMode" class="tm-drawer-footer">
              <button class="tm-btn tm-btn-secondary" @click="detailVisible = false">取消</button>
              <button class="tm-btn tm-btn-primary" :disabled="editSaving || editWeekType === selectedEntry.weekType" @click="saveWeekType">
                {{ editSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Quick schedule dialog -->
      <Transition name="tm-drawer">
        <div v-if="quickDialogVisible" class="tm-drawer-overlay" @click.self="quickDialogVisible = false">
          <div class="tm-drawer" style="width: 420px;">
            <div class="tm-drawer-header">
              <h3 class="tm-drawer-title">快速排课</h3>
              <button class="tm-drawer-close" @click="quickDialogVisible = false">&times;</button>
            </div>
            <div class="tm-drawer-body">
              <div class="tm-section" style="background: #f9fafb; border-radius: 6px; padding: 10px 12px; margin-bottom: 12px;">
                <span style="font-size: 13px; color: #374151;">
                  {{ getWeekdayName(quickSlot.day) }} 第{{ quickSlot.period }}节
                </span>
              </div>
              <div class="tm-field">
                <label class="tm-label">选择教学任务 <span class="req">*</span></label>
                <el-select v-model="quickTaskId" filterable placeholder="搜索课程" style="width: 100%;">
                  <el-option
                    v-for="t in availableTasks"
                    :key="t.id"
                    :value="t.id"
                    :label="`${t.courseName} - ${t.className || ''}`"
                  />
                </el-select>
              </div>
            </div>
            <div class="tm-drawer-footer">
              <button class="tm-btn tm-btn-secondary" @click="quickDialogVisible = false">取消</button>
              <button class="tm-btn tm-btn-primary" :disabled="!quickTaskId || quickSaving" @click="executeQuickSchedule">
                {{ quickSaving ? '排课中...' : '确定排课' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http as request } from '@/utils/request'
import { universalPlaceApi } from '@/api/universalPlace'
import { scheduleApi, periodConfigApi, teachingTaskApi } from '@/api/teaching'
import { orgUnitApi } from '@/api/organization'
import type { ScheduleEntry, PeriodConfig } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'
import TimetableGrid from '../scheduling/TimetableGrid.vue'
import TimetableMatrix from '../scheduling/TimetableMatrix.vue'
import ScheduleTree from '@/components/teaching/ScheduleTree.vue'

const props = defineProps<{ semesterId: number | string | undefined }>()
const periods = ref<PeriodConfig[]>(DEFAULT_PERIODS)

// State
const viewType = ref<'class' | 'teacher' | 'classroom' | 'overview'>('class')
const overviewMode = ref<'class' | 'teacher' | 'classroom'>('class')
const overviewSelected = ref<{ id: number | string; name: string }[]>([])
const targetId = ref<number | string>()

function setViewType(t: typeof viewType.value) {
  if (viewType.value === t) return
  viewType.value = t
  if (t !== 'overview') {
    targetId.value = undefined
    entries.value = []
    currentTargetNameFromTree.value = ''
    if (compareMode.value) { compareMode.value = false; compareTargetId.value = undefined; compareEntries.value = [] }
  } else {
    overviewSelected.value = []
  }
}

function onMultiSelect(items: { id: number | string; name: string }[]) {
  overviewSelected.value = items
}
const entries = ref<ScheduleEntry[]>([])
const week = ref<number | undefined>()
const weekType = ref(0)

// 统计当前班级/教师/教室的单双周课程数量
const oddCount = computed(() => new Set(entries.value.filter(e => e.weekType === 1).map(e => e.taskId)).size)
const evenCount = computed(() => new Set(entries.value.filter(e => e.weekType === 2).map(e => e.taskId)).size)
const hasOddWeek = computed(() => oddCount.value > 0)
const hasEvenWeek = computed(() => evenCount.value > 0)
const hasOddEvenWeek = computed(() => hasOddWeek.value || hasEvenWeek.value)

// 周次翻页是否需要：存在单双周 或 不同课程起止周不同
const weekPagerNeeded = computed(() => {
  if (hasOddEvenWeek.value) return true
  // 检查 start_week / end_week 是否存在差异
  if (entries.value.length < 2) return false
  const first = entries.value[0]
  return entries.value.some(e => e.weekStart !== first.weekStart || e.weekEnd !== first.weekEnd)
})
const weekPagerHint = computed(() => {
  if (hasOddEvenWeek.value) return '当前有单双周课程，不同周次显示可能不同'
  return '当前有不同周次范围的课程'
})

// 当目标切换或条目更新时，如果当前筛选不可用则重置
watch([hasOddWeek, hasEvenWeek], () => {
  if (weekType.value === 1 && !hasOddWeek.value) weekType.value = 0
  if (weekType.value === 2 && !hasEvenWeek.value) weekType.value = 0
})

const classrooms = ref<{ id: number; name: string }[]>([])
const classList = ref<{ id: number; name: string; dept?: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])

const detailVisible = ref(false)
const selectedEntry = ref<ScheduleEntry | null>(null)

// Matrix mode - removed, replaced by viewType === 'overview'

// Compare mode state
const compareMode = ref(false)
const compareTargetId = ref<number | string>()
const compareEntries = ref<ScheduleEntry[]>([])
const compareTargetName = computed(() => {
  const cls = classList.value.find(c => String(c.id) === String(compareTargetId.value))
  return cls?.name || ''
})
const filteredCompareEntries = computed(() => {
  let result = compareEntries.value
  if (week.value) {
    const w = week.value
    result = result.filter(e => e.weekStart <= w && e.weekEnd >= w)
  }
  if (weekType.value === 1) result = result.filter(e => e.weekType !== 2)
  else if (weekType.value === 2) result = result.filter(e => e.weekType !== 1)
  return result
})
const groupedClasses = computed(() => {
  const groups: Record<string, { id: number; name: string }[]> = {}
  for (const cls of classList.value) {
    const dept = cls.dept || '其他'
    if (!groups[dept]) groups[dept] = []
    groups[dept].push(cls)
  }
  return Object.entries(groups).map(([name, classes]) => ({ name, classes }))
})

function toggleCompare() {
  compareMode.value = !compareMode.value
  if (!compareMode.value) { compareTargetId.value = undefined; compareEntries.value = [] }
}

async function loadCompareTimetable() {
  if (!compareTargetId.value || !props.semesterId) return
  try {
    const res = await scheduleApi.getByClass(compareTargetId.value, props.semesterId)
    compareEntries.value = (res as any).data || res
  } catch { compareEntries.value = [] }
}

// Quick schedule state
const quickDialogVisible = ref(false)
const quickSlot = ref({ day: 1, period: 1 })
const quickTaskId = ref<number | string>()
const quickSaving = ref(false)
const availableTasks = ref<any[]>([])

// Computed
const placeholder = computed(() => {
  const map: Record<string, string> = { class: '选择班级', teacher: '选择教师', classroom: '选择教室', overview: '总览' }
  return map[viewType.value] || '选择'
})
const targetOptions = computed(() => viewType.value === 'teacher' ? teacherList.value : classrooms.value)
// Suppress unused warnings
void placeholder.value; void targetOptions.value
const currentTargetName = computed(() => currentTargetNameFromTree.value || '')

const filteredEntries = computed(() => {
  let result = entries.value
  if (week.value) {
    const w = week.value
    result = result.filter(e => e.weekStart <= w && e.weekEnd >= w)
  }
  if (weekType.value === 1) result = result.filter(e => e.weekType !== 2)
  else if (weekType.value === 2) result = result.filter(e => e.weekType !== 1)
  return result
})

const dayCount = computed(() => new Set(filteredEntries.value.map(e => e.dayOfWeek)).size)
const weeklyHours = computed(() => filteredEntries.value.reduce((sum, e) => sum + (e.periodEnd - e.periodStart + 1), 0))

// Tree selection handler — works for all three modes
const currentTargetNameFromTree = ref('')
function onTreeSelect(node: { type: string; id: number | string; name: string; classIds?: (number | string)[] }) {
  if (!node.id) { targetId.value = undefined; entries.value = []; currentTargetNameFromTree.value = ''; return }
  // For CLASS/TEACHER/CLASSROOM leaf nodes, load timetable
  if (['CLASS', 'TEACHER', 'CLASSROOM'].includes(node.type)) {
    targetId.value = node.id
    currentTargetNameFromTree.value = node.name
    loadTimetable()
  }
}

// Load period config from calendar
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
      periods.value = classPeriods.map((p: any) => ({
        period: p.period,
        name: p.name,
        startTime: p.startTime,
        endTime: p.endTime,
      }))
    }
  } catch { /* use defaults */ }
}

// Data loading
async function loadClassrooms() {
  try {
    const allItems = await universalPlaceApi.getFlatList()
    // Filter: places with capacity (classrooms), exclude buildings/floors
    const filtered = allItems.filter((p: any) => {
      const cap = p.capacity || 0
      const name = p.placeName || p.place_name || ''
      const typeCode = p.typeCode || p.type_code || ''
      // Match by typeCode, or by having reasonable capacity, or by name containing 教室/机房/实验
      return typeCode.includes('CLASSROOM') || typeCode.includes('LAB') || typeCode.includes('TRAINING')
        || (cap > 0 && cap < 1000)
        || /教室|机房|实验|实训/.test(name)
    })
    classrooms.value = filtered.map((p: any) => ({ id: p.id, name: p.placeName || p.place_name || p.name }))
  } catch { /* */ }
}

async function loadClassList() {
  try {
    const tree = await orgUnitApi.getTree()
    const data = Array.isArray(tree) ? tree : (tree as any).data || []
    const result: { id: number; name: string; dept?: string }[] = []
    function walk(nodes: any[], deptName?: string) {
      for (const n of nodes) {
        const t = n.unitType || ''
        if (t === 'CLASS') result.push({ id: n.id, name: n.unitName, dept: deptName })
        if (n.children) walk(n.children, t === 'DEPARTMENT' ? n.unitName : deptName)
      }
    }
    walk(data)
    classList.value = result
  } catch { /* */ }
}

async function loadTeacherList() {
  if (!props.semesterId) return
  try {
    // Load teachers who have been assigned to tasks this semester
    const res = await request.get('/teaching/tasks', { params: { semesterId: props.semesterId, page: 1, size: 500 } })
    const data = (res as any).data || res
    const tasks: any[] = data.records || (Array.isArray(data) ? data : [])
    const teacherMap = new Map<string, string>()
    for (const t of tasks) {
      if (t.teacherName) {
        // Try to get individual teachers from teachers array
        const teachers = t.teachers || []
        for (const te of teachers) {
          const tid = te.teacher_id || te.teacherId
          const name = te.real_name || te.teacherName
          if (tid && name) teacherMap.set(String(tid), name)
        }
      }
    }
    teacherList.value = Array.from(teacherMap.entries()).map(([id, name]) => ({ id: Number(id), name }))
    // Fallback: load from simple user list (non-students)
    if (teacherList.value.length === 0) {
      const res2 = await request.get('/users/simple')
      const users = (res2 as any).data || res2
      const items = Array.isArray(users) ? users : []
      teacherList.value = items.filter((u: any) => u.userType !== 'STUDENT').map((u: any) => ({ id: u.id, name: u.realName || u.username }))
    }
  } catch { /* */ }
}

async function loadTimetable() {
  if (!targetId.value || !props.semesterId) return
  try {
    let res
    if (viewType.value === 'class') res = await scheduleApi.getByClass(targetId.value, props.semesterId)
    else if (viewType.value === 'teacher') res = await scheduleApi.getByTeacher(targetId.value, props.semesterId)
    else res = await scheduleApi.getByClassroom(targetId.value, props.semesterId)
    entries.value = (res as any).data || res
  } catch { /* */ }
}

// Handlers - replaced by setViewType()
function showEntryDetail(entry: ScheduleEntry) {
  selectedEntry.value = entry
  editWeekType.value = entry.weekType ?? 0
  detailVisible.value = true
}
function getWeekdayName(day: number) { return WEEKDAYS.find(w => w.value === day)?.label || '' }

const editWeekType = ref(0)
const editSaving = ref(false)
const lockSaving = ref(false)

async function toggleLock() {
  if (!selectedEntry.value) return
  lockSaving.value = true
  try {
    const res = await request.post(`/teaching/schedules/${selectedEntry.value.id}/toggle-lock`) as any
    const newLocked = (res.data?.isLocked ?? res.isLocked ?? 0)
    // 更新本地数据
    const idx = entries.value.findIndex(e => e.id === selectedEntry.value!.id)
    if (idx >= 0) (entries.value[idx] as any).isLocked = newLocked
    if (selectedEntry.value) (selectedEntry.value as any).isLocked = newLocked
    ElMessage.success(newLocked ? '已锁定' : '已解锁')
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    lockSaving.value = false
  }
}

async function saveWeekType() {
  if (!selectedEntry.value) return
  editSaving.value = true
  try {
    await scheduleApi.updateEntry(0, selectedEntry.value.id, { weekType: editWeekType.value })
    // 更新本地数据
    const idx = entries.value.findIndex(e => e.id === selectedEntry.value!.id)
    if (idx >= 0) entries.value[idx].weekType = editWeekType.value
    if (selectedEntry.value) selectedEntry.value.weekType = editWeekType.value
    ElMessage.success('周次类型已更新')
    detailVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    editSaving.value = false
  }
}

async function onEntryDrop(entryId: number, newDay: number, newPeriod: number) {
  if (!props.semesterId) return
  try {
    const check = await scheduleApi.checkMoveConflict({ entryId, semesterId: props.semesterId, dayOfWeek: newDay, periodStart: newPeriod })
    const checkData = (check as any).data || check
    if (checkData.hasConflict) { ElMessage.warning('无法移动: 存在时间冲突'); return }
    await scheduleApi.moveEntry(entryId, { semesterId: props.semesterId, dayOfWeek: newDay, periodStart: newPeriod })
    ElMessage.success('课程已移动')
    loadTimetable()
  } catch { ElMessage.error('移动失败') }
}

// Quick schedule: click empty cell
async function onCellClick(day: number, period: number) {
  // Check if cell is empty
  const existing = filteredEntries.value.filter(e => e.dayOfWeek === day && e.periodStart <= period && e.periodEnd >= period)
  if (existing.length > 0) return // cell not empty

  quickSlot.value = { day, period }
  quickTaskId.value = undefined
  quickDialogVisible.value = true

  // Load available tasks for this semester
  try {
    const res: any = await teachingTaskApi.list({ semesterId: props.semesterId, page: 1, size: 500 })
    const data = res.data || res
    availableTasks.value = (data.records || data || []).filter((t: any) => t.taskStatus !== 9) // exclude cancelled
  } catch { availableTasks.value = [] }
}

async function executeQuickSchedule() {
  if (!quickTaskId.value || !props.semesterId) return
  quickSaving.value = true
  try {
    // Find the task to get span info
    const task = availableTasks.value.find((t: any) => String(t.id) === String(quickTaskId.value))
    const span = task?.consecutivePeriods || 2
    const periodEnd = quickSlot.value.period + span - 1

    // Get or create a schedule plan
    let scheduleId: any
    const plans: any = await scheduleApi.list({ semesterId: props.semesterId })
    const planData = (plans as any).data || plans
    const planList = Array.isArray(planData) ? planData : planData.records || []
    if (planList.length > 0) {
      scheduleId = planList[0].id
    } else {
      const created: any = await scheduleApi.create({ semesterId: props.semesterId, name: '默认排课方案', status: 0 })
      scheduleId = (created.data || created).id
    }

    await scheduleApi.addEntry(scheduleId, {
      taskId: quickTaskId.value,
      dayOfWeek: quickSlot.value.day,
      periodStart: quickSlot.value.period,
      periodEnd: periodEnd,
      weekStart: task?.startWeek || 1,
      weekEnd: task?.endWeek || 16,
      weekType: 0,
    })
    ElMessage.success('排课成功')
    quickDialogVisible.value = false
    loadTimetable()
  } catch (e: any) {
    ElMessage.error(e?.message || '排课失败')
  } finally {
    quickSaving.value = false
  }
}

watch(() => props.semesterId, (val) => {
  if (val) Promise.all([loadPeriodConfig(), loadClassrooms(), loadClassList(), loadTeacherList()])
  entries.value = []
  targetId.value = undefined
}, { immediate: true })
</script>

<style scoped>
/* Layout: left tree + main content */
.tv-layout { display: flex; gap: 0; height: 100%; }
.tv-tree { width: 220px; flex-shrink: 0; border-right: 1px solid #e5e7eb; overflow-y: auto; background: #fff; border-radius: 10px 0 0 10px; }
.tv-main { flex: 1; min-width: 0; }

.tv-controls {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  padding: 12px 16px;
  margin-bottom: 12px;
}
.tv-controls-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.tv-sep {
  display: inline-block;
  width: 1px;
  height: 18px;
  background: #d1d5db;
}
.tv-current-target {
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  padding: 4px 10px;
  background: #eff6ff;
  border-radius: 5px;
}
.tv-grid-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

/* Week pager */
.tv-week-pager {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  overflow: hidden;
}
.tv-week-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.12s;
}
.tv-week-btn:hover:not(:disabled) { background: #f3f4f6; color: #111827; }
.tv-week-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.tv-week-label {
  padding: 0 10px;
  font-size: 12.5px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  white-space: nowrap;
  min-width: 60px;
  text-align: center;
}
.tv-week-label:hover { color: #2563eb; }

/* Compare mode */
.tv-compare-btn {
  padding: 4px 12px;
  font-size: 12px;
  border: 1px solid #d1d5db;
  border-radius: 5px;
  background: #fff;
  color: #374151;
  cursor: pointer;
  transition: all 0.12s;
}
.tv-compare-btn:hover { border-color: #2563eb; color: #2563eb; }
.tv-compare-active { background: #2563eb !important; color: #fff !important; border-color: #2563eb !important; }

/* 单双周筛选禁用样式 */
.tv-week-disabled { opacity: 0.5; }
.tm-radio-disabled { cursor: not-allowed !important; color: #9ca3af !important; }
.tm-radio-disabled:hover { background: transparent !important; }

/* 单双周醒目标注 */
.tv-week-badge {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 3px 8px 3px 7px; border-radius: 99px;
  font-size: 11.5px; font-weight: 500; letter-spacing: 0.02em;
  animation: tv-badge-pulse 2s ease-in-out infinite;
}
.tv-week-badge-dot {
  display: inline-block; width: 6px; height: 6px; border-radius: 50%;
}
.tv-week-badge-odd {
  background: #fef3c7; color: #d97706; border: 1px solid #fde68a;
}
.tv-week-badge-odd .tv-week-badge-dot { background: #f59e0b; }
.tv-week-badge-even {
  background: #dbeafe; color: #2563eb; border: 1px solid #bfdbfe;
}
.tv-week-badge-even .tv-week-badge-dot { background: #3b82f6; }
@keyframes tv-badge-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(251,191,36,0); }
  50% { box-shadow: 0 0 0 3px rgba(251,191,36,0.15); }
}
.tv-compare-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
}
.tv-grid-compare {
  display: flex;
  gap: 12px;
}
.tv-grid-label {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border-bottom: 1px solid #e5e7eb;
  text-align: center;
}

/* Detail drawer */
.detail-row { display: flex; padding: 6px 0; font-size: 13px; border-bottom: 1px solid #f3f4f6; }
.detail-row:last-child { border-bottom: none; }
.detail-label { width: 60px; color: #6b7280; flex-shrink: 0; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
