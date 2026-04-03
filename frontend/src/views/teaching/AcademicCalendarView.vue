<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- ========== Level 1: Year list ========== -->
    <template v-if="!selectedYear">
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
        <div>
          <h1 class="text-lg font-semibold text-gray-900">校历管理</h1>
          <p class="mt-0.5 text-sm text-gray-500">管理学年、学期、校历事件</p>
        </div>
        <button
          class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
          @click="showYearDialog()"
        >
          新建学年
        </button>
      </div>

      <!-- Stats bar -->
      <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
        <span class="text-sm text-gray-500">学年总数 <span class="font-semibold text-gray-900">{{ academicYears.length }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">当前学年 <span class="font-semibold text-gray-900">{{ academicYears.find(y => y.isCurrent)?.name || '未设置' }}</span></span>
      </div>

      <!-- Year grid -->
      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <div
            v-for="year in academicYears"
            :key="year.id"
            class="group relative cursor-pointer rounded-xl border bg-white p-5 transition-all hover:shadow-md"
            :class="year.isCurrent ? 'border-blue-200 ring-1 ring-blue-100' : 'border-gray-200'"
            @click="enterYear(year)"
          >
            <span
              v-if="year.isCurrent"
              class="absolute right-3 top-3 rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-600"
            >当前学年</span>
            <div class="mb-3 text-2xl">📅</div>
            <div class="text-base font-semibold text-gray-900">{{ year.name }}</div>
            <div class="mt-1 text-xs text-gray-400">{{ year.startDate }} ~ {{ year.endDate }}</div>
            <div class="mt-3 flex items-center gap-4 text-sm">
              <span class="text-gray-500"><span class="font-semibold text-gray-900">{{ getYearSemesterCount(year.id) }}</span> 学期</span>
              <span class="text-gray-500"><span class="font-semibold text-gray-900">{{ getYearEventCount(year.id) }}</span> 事件</span>
            </div>
            <div class="mt-3 flex items-center gap-1 text-xs text-blue-600 opacity-0 transition-opacity group-hover:opacity-100">
              <span>点击进入</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <!-- Empty state -->
        <div v-if="!academicYears.length" class="flex flex-col items-center justify-center py-20">
          <div class="text-4xl">📆</div>
          <div class="mt-3 text-sm text-gray-400">暂无学年数据</div>
          <button
            class="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            @click="showYearDialog()"
          >创建第一个学年</button>
        </div>
      </div>
    </template>

    <!-- ========== Level 2: Year detail (tabs) ========== -->
    <template v-else>
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
        <div class="flex items-center gap-3">
          <button class="flex items-center gap-1 text-sm text-gray-500 hover:text-blue-600" @click="selectedYear = null">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </button>
          <div class="h-4 w-px bg-gray-200" />
          <h1 class="text-lg font-semibold text-gray-900">{{ selectedYear.name }}</h1>
          <el-tag v-if="selectedYear.isCurrent" type="success" size="small">当前</el-tag>
        </div>
        <div class="flex items-center gap-2">
          <el-button size="small" @click="showYearDialog(selectedYear)">编辑学年</el-button>
          <el-dropdown trigger="click">
            <el-button type="primary" size="small">
              快捷操作 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showSemesterDialog()">新建学期</el-dropdown-item>
                <el-dropdown-item @click="showEventDialog()">添加事件</el-dropdown-item>
                <el-dropdown-item @click="showPeriodConfigDialog()">配置作息时间</el-dropdown-item>
                <el-dropdown-item @click="showGradeActivityDialog()">添加年级活动</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- Stats bar -->
      <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
        <span class="text-sm text-gray-500">当前学期 <span class="font-semibold text-gray-900">{{ currentSemester?.name || '未设置' }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">当前周次 <span class="font-semibold text-gray-900">{{ currentWeekNumber ? `第${currentWeekNumber}周` : '-' }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">剩余天数 <span class="font-semibold text-gray-900">{{ daysRemaining }}天</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="flex items-center gap-2 text-sm text-gray-500">
          学期进度
          <el-progress :percentage="semesterProgress" :stroke-width="6" :show-text="false" style="width: 60px" />
          <span class="font-semibold text-gray-900">{{ semesterProgress }}%</span>
        </span>
      </div>

      <!-- Tab bar -->
      <div class="flex items-center gap-1 border-b border-gray-200 bg-white px-6">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="relative px-4 py-2.5 text-sm font-medium transition-colors"
          :class="activeTab === tab.key ? 'text-blue-600' : 'text-gray-500 hover:text-gray-700'"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <div v-if="activeTab === tab.key" class="absolute inset-x-0 bottom-0 h-0.5 bg-blue-600" />
        </button>
      </div>

      <!-- Tab content -->
      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <CalendarSemesterTab
          v-if="activeTab === 'semester'"
          :semesters="semesters"
          :selected-semester="selectedSemester"
          :teaching-weeks="teachingWeeks"
          @select="selectSemester"
          @show-dialog="showSemesterDialog"
          @generate-weeks="generateWeeks"
          @set-current="setCurrentSemester"
          @edit-week="editWeek"
        />
        <CalendarEventsTab
          v-if="activeTab === 'events'"
          :events="events"
          :current-semester="currentSemester"
          :current-year-id="currentYearId"
          @show-event-dialog="showEventDialog"
          @create-event-from-drag="handleDragCreatedEvent"
        />
        <CalendarPeriodsTab
          v-if="activeTab === 'periods'"
          :semesters="semesters"
          :period-configs="periodConfigs"
          :period-config-semester="periodConfigSemester"
          @update:period-config-semester="periodConfigSemester = $event"
          @add="showPeriodConfigDialog()"
          @edit="showPeriodConfigDialog"
          @delete="deletePeriodConfig"
        />
        <CalendarActivitiesTab
          v-if="activeTab === 'gradeActivities'"
          :grades="grades"
          :grade-activities="gradeActivities"
          @add="showGradeActivityDialog"
          @edit="editGradeActivity"
          @delete="deleteGradeActivity"
        />
      </div>
    </template>

    <!-- ========== Dialogs ========== -->

    <!-- Year dialog -->
    <el-dialog v-model="yearDialogVisible" :title="yearForm.id ? '编辑学年' : '新建学年'" width="500px" :close-on-click-modal="false">
      <el-form ref="yearFormRef" :model="yearForm" :rules="yearRules" label-position="top">
        <el-form-item label="学年名称" prop="name">
          <el-input v-model="yearForm.name" placeholder="请输入学年名称，如：2025-2026学年" />
        </el-form-item>
        <div class="flex items-center gap-3">
          <el-form-item label="开始日期" prop="startDate" class="flex-1">
            <el-date-picker v-model="yearForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开始日期" style="width: 100%" />
          </el-form-item>
          <span class="mt-4 text-gray-400">至</span>
          <el-form-item label="结束日期" prop="endDate" class="flex-1">
            <el-date-picker v-model="yearForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择结束日期" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="flex items-center gap-1.5 text-xs text-gray-400">
          <el-icon><InfoFilled /></el-icon>
          <span>学年通常从9月开始，到次年8月结束</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="yearDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveYear">{{ yearForm.id ? '保存修改' : '创建学年' }}</el-button>
      </template>
    </el-dialog>

    <!-- Semester dialog -->
    <el-dialog v-model="semesterDialogVisible" :title="semesterForm.id ? '编辑学期' : '新建学期'" width="520px" :close-on-click-modal="false">
      <el-form ref="semesterFormRef" :model="semesterForm" :rules="semesterRules" label-position="top">
        <el-form-item label="学期名称" prop="name">
          <el-input v-model="semesterForm.name" placeholder="请输入学期名称" />
        </el-form-item>
        <el-form-item label="学期类型" prop="termType">
          <div class="flex gap-3">
            <div
              v-for="term in [{value: 1, label: '第一学期', icon: '①'}, {value: 2, label: '第二学期', icon: '②'}, {value: 3, label: '短学期', icon: '☀'}]"
              :key="term.value"
              class="flex cursor-pointer items-center gap-2 rounded-lg border px-4 py-2.5 transition-all"
              :class="semesterForm.termType === term.value ? 'border-blue-300 bg-blue-50 text-blue-700' : 'border-gray-200 text-gray-600 hover:border-gray-300'"
              @click="semesterForm.termType = term.value"
            >
              <span class="text-lg">{{ term.icon }}</span>
              <span class="text-sm font-medium">{{ term.label }}</span>
            </div>
          </div>
        </el-form-item>
        <div class="mb-2 rounded-lg bg-blue-50 px-3 py-2 text-xs text-blue-600">
          当前学年时间范围：{{ selectedYear?.startDate }} ~ {{ selectedYear?.endDate }}，学期日期须在此范围内
        </div>
        <div class="flex items-center gap-3">
          <el-form-item label="开始日期" prop="startDate" class="flex-1">
            <el-date-picker
              v-model="semesterForm.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择开始日期"
              style="width: 100%"
              :disabled-date="(d: Date) => disableSemesterDate(d)"
            />
          </el-form-item>
          <span class="mt-4 text-gray-400">至</span>
          <el-form-item label="结束日期" prop="endDate" class="flex-1">
            <el-date-picker
              v-model="semesterForm.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择结束日期"
              style="width: 100%"
              :disabled-date="(d: Date) => disableSemesterDate(d)"
            />
          </el-form-item>
        </div>
        <el-form-item label="教学周数" prop="teachingWeeks">
          <div class="flex items-center gap-2">
            <el-input-number v-model="semesterForm.teachingWeeks" :min="1" :max="30" controls-position="right" />
            <span class="text-sm text-gray-400">周（一般为16-20周）</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="semesterDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSemester">{{ semesterForm.id ? '保存修改' : '创建学期' }}</el-button>
      </template>
    </el-dialog>

    <!-- Weeks dialog -->
    <el-dialog v-model="weeksDialogVisible" title="教学周历" width="650px">
      <div class="mb-3 flex items-center justify-between">
        <span class="font-medium text-gray-900">{{ currentSemesterForWeeks?.name }}</span>
        <el-button type="primary" size="small" @click="generateWeeks">自动生成</el-button>
      </div>
      <el-table :data="teachingWeeks" border size="small" max-height="400">
        <el-table-column prop="weekNumber" label="周次" width="70" align="center">
          <template #default="{ row }">第{{ row.weekNumber }}周</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始" width="100" align="center" />
        <el-table-column prop="endDate" label="结束" width="100" align="center" />
        <el-table-column prop="weekType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getWeekTypeTag(row.weekType) as any">{{ getWeekTypeName(row.weekType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-dialog>

    <!-- Event dialog -->
    <el-dialog v-model="eventDialogVisible" :title="eventForm.id ? '编辑事件' : '添加事件'" width="450px">
      <el-form ref="eventFormRef" :model="eventForm" :rules="eventRules" label-width="80px">
        <el-form-item label="事件标题" prop="title">
          <el-input v-model="eventForm.title" />
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="eventForm.eventType" style="width: 100%">
            <el-option :value="1" label="开学" />
            <el-option :value="2" label="放假" />
            <el-option :value="3" label="考试" />
            <el-option :value="4" label="活动" />
            <el-option :value="5" label="其他" />
            <el-option :value="6" label="补课" />
          </el-select>
        </el-form-item>
        <el-form-item label="持续事件">
          <el-switch v-model="eventForm.isRange" />
          <span class="ml-2 text-xs text-gray-400">开启后可设置结束日期</span>
        </el-form-item>
        <el-form-item v-if="!eventForm.isRange" label="日期" prop="startDate">
          <el-date-picker v-model="eventForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="eventForm.isRange" label="开始日期" prop="startDate">
          <el-date-picker v-model="eventForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="eventForm.isRange" label="结束日期" prop="endDate">
          <el-date-picker v-model="eventForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="eventForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-between">
          <el-button v-if="eventForm.id" type="danger" text @click="deleteEvent">删除</el-button>
          <div class="flex gap-2">
            <el-button @click="eventDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveEvent">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- Period config dialog -->
    <el-dialog v-model="periodConfigDialogVisible" :title="periodConfigForm.id ? '编辑节次' : '添加节次'" width="420px" :close-on-click-modal="false">
      <el-form :model="periodConfigForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="节次">
              <el-input-number v-model="periodConfigForm.period" :min="1" :max="15" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="名称">
              <el-input v-model="periodConfigForm.name" placeholder="如：第一节、早读" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-time-picker v-model="periodConfigForm.startTime" format="HH:mm" value-format="HH:mm" placeholder="选择开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-time-picker v-model="periodConfigForm.endTime" format="HH:mm" value-format="HH:mm" placeholder="选择结束时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <div v-if="periodConfigForm.startTime && periodConfigForm.endTime" class="flex items-center gap-1.5 rounded-lg bg-blue-50 px-3 py-2 text-sm text-blue-700">
          <el-icon><Clock /></el-icon>
          <span>{{ periodConfigForm.name }}: {{ periodConfigForm.startTime }} - {{ periodConfigForm.endTime }}</span>
          <span class="text-blue-500">({{ calculateDuration(periodConfigForm.startTime, periodConfigForm.endTime) }}分钟)</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="periodConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePeriodConfig">{{ periodConfigForm.id ? '保存修改' : '添加' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, ArrowRight, InfoFilled, Clock } from '@element-plus/icons-vue'
import { academicYearApi, semesterApi, academicEventApi } from '@/api/calendar'
import type { AcademicYear, Semester, TeachingWeek, AcademicEvent } from '@/types/teaching'
import CalendarSemesterTab from './calendar/CalendarSemesterTab.vue'
import CalendarEventsTab from './calendar/CalendarEventsTab.vue'
import CalendarPeriodsTab from './calendar/CalendarPeriodsTab.vue'
import CalendarActivitiesTab from './calendar/CalendarActivitiesTab.vue'

// ==================== State ====================
const academicYears = ref<AcademicYear[]>([])
const currentYearId = ref<number>()
const semesters = ref<Semester[]>([])
const events = ref<AcademicEvent[]>([])
const selectedYear = ref<AcademicYear | null>(null)
const yearSemesterCounts = ref<Map<number, number>>(new Map())
const yearEventCounts = ref<Map<number, number>>(new Map())
const teachingWeeks = ref<TeachingWeek[]>([])
const saving = ref(false)
const selectedSemester = ref<Semester | null>(null)
const currentSemesterForWeeks = ref<Semester>()

const activeTab = ref('semester')
const tabs = [
  { key: 'semester', label: '学期管理' },
  { key: 'events', label: '校历事件' },
  { key: 'periods', label: '作息时间' },
  { key: 'gradeActivities', label: '年级活动' },
]

// Period config
const periodConfigSemester = ref(0)
const periodConfigs = ref<any[]>([])

// Grade activities
const grades = ref<any[]>([])
const gradeActivities = ref<any[]>([])

// Dialog visibility
const yearDialogVisible = ref(false)
const semesterDialogVisible = ref(false)
const weeksDialogVisible = ref(false)
const eventDialogVisible = ref(false)
const periodConfigDialogVisible = ref(false)

// Form refs
const yearFormRef = ref<FormInstance>()
const semesterFormRef = ref<FormInstance>()
const eventFormRef = ref<FormInstance>()

// Form data
const yearForm = ref<Partial<AcademicYear>>({})
const semesterForm = ref<Partial<Semester>>({})
interface EventFormData extends Partial<AcademicEvent> { isRange?: boolean }
const eventForm = ref<EventFormData>({})
const periodConfigForm = ref({
  id: null as number | null, period: 1, name: '', startTime: '', endTime: '', semesterId: 0
})

// Validation rules
const yearRules: FormRules = {
  name: [{ required: true, message: '请输入学年名称', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
}
const semesterRules: FormRules = {
  name: [{ required: true, message: '请输入学期名称', trigger: 'blur' }],
  termType: [{ required: true, message: '请选择学期类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  teachingWeeks: [{ required: true, message: '请输入教学周数', trigger: 'blur' }],
}
const eventRules: FormRules = {
  title: [{ required: true, message: '请输入事件标题', trigger: 'blur' }],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

// ==================== Computed ====================
const currentSemester = computed(() => semesters.value.find(s => s.isCurrent) || null)

const currentWeekNumber = computed(() => {
  if (!currentSemester.value) return null
  const start = new Date(currentSemester.value.startDate)
  const today = new Date()
  const diff = Math.floor((today.getTime() - start.getTime()) / (7 * 24 * 60 * 60 * 1000))
  return diff >= 0 && diff < (currentSemester.value.teachingWeeks || 18) ? diff + 1 : null
})

const daysRemaining = computed(() => {
  if (!currentSemester.value) return 0
  const end = new Date(currentSemester.value.endDate)
  const diff = Math.ceil((end.getTime() - Date.now()) / 86400000)
  return diff > 0 ? diff : 0
})

const semesterProgress = computed(() => {
  if (!currentSemester.value) return 0
  const start = new Date(currentSemester.value.startDate).getTime()
  const end = new Date(currentSemester.value.endDate).getTime()
  const now = Date.now()
  return Math.max(0, Math.min(100, Math.round(((now - start) / (end - start)) * 100)))
})

// ==================== Data loading ====================
const loadAcademicYears = async () => {
  try {
    const res: any = await academicYearApi.list()
    const data = Array.isArray(res) ? res : (res.data || [])
    academicYears.value = data.map((y: any) => ({ ...y, name: y.yearName || y.name }))
    loadYearStats()
  } catch (error) {
    console.error('Failed to load academic years:', error)
  }
}

const loadYearStats = async () => {
  for (const year of academicYears.value) {
    try {
      const semRes: any = await semesterApi.list(year.id)
      const semData = semRes.data || semRes
      yearSemesterCounts.value.set(Number(year.id), Array.isArray(semData) ? semData.length : 0)
      const eventRes = await academicEventApi.list({ yearId: year.id })
      let eventData: any[] = []
      if (Array.isArray(eventRes)) eventData = eventRes
      else if (eventRes && typeof eventRes === 'object') eventData = (eventRes as any).data || (eventRes as any).records || []
      yearEventCounts.value.set(Number(year.id), eventData.length)
    } catch (error) {
      console.error(`Failed to load stats for year ${year.id}:`, error)
    }
  }
}

const getYearSemesterCount = (yearId: number | string) => yearSemesterCounts.value.get(Number(yearId)) || 0
const getYearEventCount = (yearId: number | string) => yearEventCounts.value.get(Number(yearId)) || 0

const enterYear = (year: AcademicYear) => {
  selectedYear.value = year
  currentYearId.value = Number(year.id)
  loadSemesters()
  loadEvents()
}

const loadSemesters = async () => {
  if (!currentYearId.value) return
  try {
    const res: any = await semesterApi.list(currentYearId.value)
    const data = res.data || res
    semesters.value = data.map((s: any) => ({
      ...s,
      name: s.semesterName || s.name,
      termType: s.semesterType || s.termType,
      teachingWeeks: s.weekCount || s.teachingWeeks,
    }))
  } catch (error) {
    console.error('Failed to load semesters:', error)
  }
}

const loadEvents = async () => {
  if (!currentYearId.value) return
  try {
    const res = await academicEventApi.list({ yearId: currentYearId.value })
    let data: any[] = []
    if (Array.isArray(res)) data = res
    else if (res && typeof res === 'object') data = (res as any).data || (res as any).records || []
    if (!data || data.length === 0) { events.value = []; return }
    events.value = data.map((e: any) => ({
      id: e.id || 0,
      yearId: e.yearId,
      semesterId: e.semesterId,
      title: String(e.eventName || e.title || '未命名事件'),
      eventType: Number(e.eventType) || 5,
      startDate: String(e.startDate || ''),
      endDate: e.endDate ? String(e.endDate) : undefined,
      allDay: true,
      description: e.description ? String(e.description) : undefined,
    }))
  } catch (error) {
    console.error('Failed to load events:', error)
  }
}

// ==================== Semester tab handlers ====================
const selectSemester = (sem: Semester) => {
  selectedSemester.value = sem
  loadTeachingWeeks(sem.id)
}

const loadTeachingWeeks = async (semesterId: number | string) => {
  try {
    const res = await semesterApi.getWeeks(semesterId)
    const data = Array.isArray(res) ? res : (res as any)?.data || []
    teachingWeeks.value = data.map((w: any) => ({
      id: w.id,
      semesterId: w.semesterId,
      weekNumber: w.weekNumber,
      startDate: w.startDate,
      endDate: w.endDate,
      weekType: w.weekType || w.status || 1,
      remark: w.weekName || w.remark || '',
    }))
  } catch (error) {
    console.error('Failed to load teaching weeks:', error)
    teachingWeeks.value = []
  }
}

const disableSemesterDate = (date: Date) => {
  if (!selectedYear.value) return false
  const start = new Date(selectedYear.value.startDate + 'T00:00:00')
  const end = new Date(selectedYear.value.endDate + 'T23:59:59')
  return date < start || date > end
}

const generateWeeks = () => {
  if (!selectedSemester.value) return
  ElMessageBox.confirm('是否自动生成教学周次？这将覆盖已有的周次数据。', '确认').then(async () => {
    try {
      await semesterApi.generateWeeks(selectedSemester.value!.id)
      ElMessage.success('教学周次已生成')
      loadTeachingWeeks(selectedSemester.value!.id)
    } catch (error) {
      ElMessage.error('生成失败')
    }
  }).catch(() => {})
}

const editWeek = (_week: TeachingWeek) => {
  ElMessage.info('编辑周次功能开发中')
}

const getWeekTypeName = (type: number) => {
  const map: Record<number, string> = { 1: '教学周', 2: '考试周', 3: '假期', 4: '机动周' }
  return map[type] || '未知'
}
const getWeekTypeTag = (type: number): string => {
  const map: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'info', 4: '' }
  return map[type] || ''
}

// ==================== Dialog handlers ====================
const showYearDialog = (year?: AcademicYear) => {
  yearForm.value = year ? { ...year } : {}
  yearDialogVisible.value = true
}

const saveYear = async () => {
  try { await yearFormRef.value?.validate() } catch { ElMessage.warning('请填写完整的学年信息'); return }
  saving.value = true
  try {
    const payload = {
      ...yearForm.value,
      yearName: yearForm.value.name,
      yearCode: yearForm.value.startDate && yearForm.value.endDate
        ? yearForm.value.startDate.substring(0, 4) + '-' + yearForm.value.endDate.substring(0, 4)
        : undefined,
    }
    if (yearForm.value.id) await academicYearApi.update(yearForm.value.id, payload)
    else await academicYearApi.create(payload)
    ElMessage.success('保存成功')
    yearDialogVisible.value = false
    loadAcademicYears()
  } catch (error) { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const showSemesterDialog = (semester?: Semester) => {
  semesterForm.value = semester ? { ...semester } : { yearId: currentYearId.value, teachingWeeks: 18 }
  semesterDialogVisible.value = true
}

const saveSemester = async () => {
  try { await semesterFormRef.value?.validate() } catch { ElMessage.warning('请填写完整的学期信息'); return }
  saving.value = true
  try {
    const payload = { ...semesterForm.value, semesterName: semesterForm.value.name, semesterType: semesterForm.value.termType, weekCount: semesterForm.value.teachingWeeks }
    if (semesterForm.value.id) await semesterApi.update(semesterForm.value.id, payload)
    else await semesterApi.create(payload)
    ElMessage.success('保存成功')
    semesterDialogVisible.value = false
    loadSemesters()
  } catch (error) { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const setCurrentSemester = async (id: number | string) => {
  try {
    await semesterApi.setCurrent(id)
    ElMessage.success('设置成功')
    loadSemesters()
  } catch (error) { ElMessage.error('设置失败') }
}

const showEventDialog = (event?: AcademicEvent) => {
  if (event) {
    eventForm.value = { ...event, isRange: !!(event.endDate && event.endDate !== event.startDate) }
  } else {
    eventForm.value = { yearId: currentYearId.value, allDay: true, eventType: 4, isRange: false }
  }
  eventDialogVisible.value = true
}

const handleDragCreatedEvent = (data: { startDate: string; endDate: string; isRange: boolean }) => {
  eventForm.value = {
    yearId: currentYearId.value,
    startDate: data.startDate,
    endDate: data.endDate,
    allDay: true,
    eventType: 4,
    isRange: data.isRange,
  }
  eventDialogVisible.value = true
}

const saveEvent = async () => {
  try { await eventFormRef.value?.validate() } catch { ElMessage.warning('请填写完整的事件信息'); return }
  saving.value = true
  try {
    const payload = {
      ...eventForm.value,
      eventName: eventForm.value.title,
      endDate: eventForm.value.isRange ? eventForm.value.endDate : eventForm.value.startDate,
    }
    delete (payload as any).isRange
    if (eventForm.value.id) await academicEventApi.update(eventForm.value.id, payload)
    else await academicEventApi.create(payload)
    ElMessage.success('保存成功')
    eventDialogVisible.value = false
    loadEvents()
  } catch (error) { console.error('Save event error:', error); ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const deleteEvent = async () => {
  if (!eventForm.value.id) return
  await ElMessageBox.confirm('确定删除该事件吗？', '提示', { type: 'warning' })
  try {
    await academicEventApi.delete(eventForm.value.id)
    ElMessage.success('删除成功')
    eventDialogVisible.value = false
    loadEvents()
  } catch (error) { ElMessage.error('删除失败') }
}

// ==================== Period config ====================
const loadPeriodConfigs = () => {
  periodConfigs.value = [
    { period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
    { period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
    { period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
    { period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
    { period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },
    { period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
    { period: 9, name: '第九节', startTime: '19:00', endTime: '19:45' },
    { period: 10, name: '第十节', startTime: '19:55', endTime: '20:40' },
  ]
}

const showPeriodConfigDialog = (row?: any) => {
  if (row) {
    periodConfigForm.value = { ...row, semesterId: periodConfigSemester.value }
  } else {
    const maxPeriod = periodConfigs.value.length > 0 ? Math.max(...periodConfigs.value.map(p => p.period)) : 0
    periodConfigForm.value = { id: null, period: maxPeriod + 1, name: `第${maxPeriod + 1}节`, startTime: '', endTime: '', semesterId: periodConfigSemester.value }
  }
  periodConfigDialogVisible.value = true
}

const savePeriodConfig = () => {
  const form = periodConfigForm.value
  if (!form.startTime || !form.endTime) { ElMessage.warning('请填写开始时间和结束时间'); return }
  if (form.id) {
    const index = periodConfigs.value.findIndex(p => p.id === form.id)
    if (index !== -1) periodConfigs.value[index] = { ...form }
  } else {
    periodConfigs.value.push({ ...form, id: Date.now() })
    periodConfigs.value.sort((a, b) => a.period - b.period)
  }
  periodConfigDialogVisible.value = false
  ElMessage.success(form.id ? '修改成功' : '添加成功')
}

const deletePeriodConfig = (row: any) => {
  ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '确认删除', { type: 'warning' }).then(() => {
    periodConfigs.value = periodConfigs.value.filter(p => p.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const calculateDuration = (startTime: string, endTime: string) => {
  if (!startTime || !endTime) return 0
  const [sh, sm] = startTime.split(':').map(Number)
  const [eh, em] = endTime.split(':').map(Number)
  return (eh * 60 + em) - (sh * 60 + sm)
}

// ==================== Grade activities ====================
const loadGrades = () => {
  grades.value = [
    { id: 1, name: '2024级' }, { id: 2, name: '2023级' },
    { id: 3, name: '2022级' }, { id: 4, name: '2021级' },
  ]
}

const loadGradeActivities = () => {
  gradeActivities.value = [
    { id: 1, gradeId: 1, gradeName: '2024级', activityName: '新生军训', activityType: 1, startDate: '2024-09-01', endDate: '2024-09-14', startWeek: 1, endWeek: 2, affectTeaching: true },
    { id: 2, gradeId: 1, gradeName: '2024级', activityName: '入学教育', activityType: 2, startDate: '2024-09-15', endDate: '2024-09-15', startWeek: 3, endWeek: 3, affectTeaching: false },
    { id: 3, gradeId: 3, gradeName: '2022级', activityName: '毕业实习', activityType: 5, startDate: '2025-03-01', endDate: '2025-05-31', startWeek: 1, endWeek: 12, affectTeaching: true },
    { id: 4, gradeId: 3, gradeName: '2022级', activityName: '毕业答辩', activityType: 7, startDate: '2025-06-01', endDate: '2025-06-15', startWeek: 15, endWeek: 16, affectTeaching: true },
  ]
}

const showGradeActivityDialog = () => { ElMessage.info('添加年级活动对话框开发中') }
const editGradeActivity = (_row: any) => { ElMessage.info('编辑年级活动开发中') }
const deleteGradeActivity = (row: any) => {
  ElMessageBox.confirm('确定要删除该年级活动吗？', '确认').then(() => {
    gradeActivities.value = gradeActivities.value.filter(a => a.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// ==================== Init ====================
onMounted(() => {
  loadAcademicYears()
  loadPeriodConfigs()
  loadGrades()
  loadGradeActivities()
})
</script>
