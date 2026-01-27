<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  ArrowLeft, Save, Send, Building2, Users, ClipboardCheck, LayoutGrid,
  Search, Camera, Check, X, AlertTriangle, ChevronRight, ChevronDown,
  MapPin, Trash2, Edit, Plus, ScanLine, Loader2
} from 'lucide-vue-next'
import { ElMessage, ElProgress } from 'element-plus'

// ─── Types ───
type InputMode = 'SPACE_FIRST' | 'PERSON_FIRST' | 'CHECKLIST' | 'ORG_FIRST'
type RoomStatus = 'unchecked' | 'pass' | 'has_issues'
type ChecklistResult = 'PASS' | 'FAIL' | 'NA' | null

interface RoomInfo {
  spaceId: number
  spaceNo: string
  spaceType: string
  classes: { classId: number; className: string; studentCount: number }[]
  status: RoomStatus
  deductionScore: number
  deductionCount: number
}

interface FloorInfo {
  floor: number
  rooms: RoomInfo[]
}

interface BuildingInfo {
  buildingId: number
  buildingName: string
  floors: FloorInfo[]
}

interface DeductionItem {
  id: number
  itemName: string
  deductMode: number
  fixedScore?: number
  baseScore?: number
  perPersonScore?: number
  icon: string
}

interface CategoryInfo {
  categoryId: number
  categoryName: string
  items: DeductionItem[]
}

interface StudentResult {
  studentId: number
  name: string
  studentNo: string
  className: string
  gradeName: string
  orgUnitName: string
}

interface RecordedEntry {
  id: number
  time: string
  target: string
  className: string
  itemName: string
  score: number
}

interface ChecklistItemState {
  itemId: number
  itemName: string
  result: ChecklistResult
  deductScore: number
  remark: string
}

// ─── State ───
const inputMode = ref<InputMode>('SPACE_FIRST')
const sessionStatus = ref(1) // 1=进行中
const saving = ref(false)

// Space mode state
const selectedBuilding = ref(0)
const selectedFloor = ref(3)
const selectedRoom = ref<RoomInfo | null>(null)
const showDeductionPanel = ref(false)
const selectedCategory = ref(0)
const selectedDeductionItem = ref<DeductionItem | null>(null)

// Person mode state
const searchKeyword = ref('')
const searchResults = ref<StudentResult[]>([])
const selectedStudents = ref<StudentResult[]>([])
const personCategoryId = ref<number | null>(null)
const personItemId = ref<number | null>(null)
const searching = ref(false)

// Checklist mode state
const checklistRoomIndex = ref(0)
const checklistItems = ref<ChecklistItemState[]>([])

// ─── Mock Data ───
const buildings: BuildingInfo[] = [
  {
    buildingId: 1, buildingName: '1号宿舍楼',
    floors: [
      {
        floor: 3, rooms: [
          { spaceId: 301, spaceNo: '301', spaceType: 'DORMITORY', classes: [{ classId: 1, className: '计算机2班', studentCount: 6 }], status: 'pass', deductionScore: 0, deductionCount: 0 },
          { spaceId: 302, spaceNo: '302', spaceType: 'DORMITORY', classes: [{ classId: 1, className: '计算机2班', studentCount: 4 }, { classId: 2, className: '软件1班', studentCount: 2 }], status: 'pass', deductionScore: 0, deductionCount: 0 },
          { spaceId: 303, spaceNo: '303', spaceType: 'DORMITORY', classes: [{ classId: 2, className: '软件1班', studentCount: 6 }], status: 'has_issues', deductionScore: 2, deductionCount: 1 },
          { spaceId: 304, spaceNo: '304', spaceType: 'DORMITORY', classes: [{ classId: 3, className: '电商1班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
          { spaceId: 305, spaceNo: '305', spaceType: 'DORMITORY', classes: [{ classId: 3, className: '电商1班', studentCount: 4 }, { classId: 4, className: '会计1班', studentCount: 2 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
          { spaceId: 306, spaceNo: '306', spaceType: 'DORMITORY', classes: [{ classId: 4, className: '会计1班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
          { spaceId: 307, spaceNo: '307', spaceType: 'DORMITORY', classes: [{ classId: 5, className: '物流1班', studentCount: 6 }], status: 'has_issues', deductionScore: 3, deductionCount: 2 },
          { spaceId: 308, spaceNo: '308', spaceType: 'DORMITORY', classes: [{ classId: 5, className: '物流1班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
        ]
      },
      {
        floor: 4, rooms: [
          { spaceId: 401, spaceNo: '401', spaceType: 'DORMITORY', classes: [{ classId: 6, className: '计算机3班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
          { spaceId: 402, spaceNo: '402', spaceType: 'DORMITORY', classes: [{ classId: 6, className: '计算机3班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
          { spaceId: 403, spaceNo: '403', spaceType: 'DORMITORY', classes: [{ classId: 7, className: '软件2班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
          { spaceId: 404, spaceNo: '404', spaceType: 'DORMITORY', classes: [{ classId: 7, className: '软件2班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
        ]
      },
      {
        floor: 5, rooms: [
          { spaceId: 501, spaceNo: '501', spaceType: 'DORMITORY', classes: [{ classId: 8, className: '电商2班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
          { spaceId: 502, spaceNo: '502', spaceType: 'DORMITORY', classes: [{ classId: 8, className: '电商2班', studentCount: 6 }], status: 'unchecked', deductionScore: 0, deductionCount: 0 },
        ]
      }
    ]
  },
  {
    buildingId: 2, buildingName: '2号宿舍楼',
    floors: [
      { floor: 1, rooms: [] },
      { floor: 2, rooms: [] }
    ]
  }
]

const categories: CategoryInfo[] = [
  {
    categoryId: 1, categoryName: '卫生检查', items: [
      { id: 1, itemName: '地面不洁', deductMode: 1, fixedScore: 2, icon: '🧹' },
      { id: 2, itemName: '被褥未叠', deductMode: 2, baseScore: 0, perPersonScore: 0.5, icon: '🛏️' },
      { id: 3, itemName: '物品乱放', deductMode: 1, fixedScore: 1, icon: '📦' },
      { id: 4, itemName: '垃圾未倒', deductMode: 1, fixedScore: 1.5, icon: '🗑️' },
      { id: 5, itemName: '窗户不洁', deductMode: 1, fixedScore: 1, icon: '🪟' },
    ]
  },
  {
    categoryId: 2, categoryName: '纪律检查', items: [
      { id: 6, itemName: '违规电器', deductMode: 1, fixedScore: 5, icon: '⚡' },
      { id: 7, itemName: '晚归', deductMode: 2, baseScore: 2, perPersonScore: 1, icon: '🌙' },
      { id: 8, itemName: '大声喧哗', deductMode: 1, fixedScore: 2, icon: '📢' },
    ]
  }
]

const mockStudents: StudentResult[] = [
  { studentId: 10001, name: '张三', studentNo: '2024010001', className: '计算机2班', gradeName: '2024级', orgUnitName: '信息工程系' },
  { studentId: 10002, name: '张伟', studentNo: '2024020015', className: '电商1班', gradeName: '2024级', orgUnitName: '经济管理系' },
  { studentId: 10003, name: '张丽', studentNo: '2023030008', className: '会计3班', gradeName: '2023级', orgUnitName: '财经系' },
  { studentId: 10004, name: '张磊', studentNo: '2024010022', className: '计算机2班', gradeName: '2024级', orgUnitName: '信息工程系' },
]

// ─── Computed ───
const currentBuilding = computed(() => buildings[selectedBuilding.value])
const currentFloor = computed(() => currentBuilding.value?.floors.find(f => f.floor === selectedFloor.value))
const currentRooms = computed(() => currentFloor.value?.rooms || [])

const progressStats = computed(() => {
  const allRooms = currentBuilding.value?.floors.flatMap(f => f.rooms) || []
  const total = allRooms.length
  const checked = allRooms.filter(r => r.status !== 'unchecked').length
  const issues = allRooms.filter(r => r.status === 'has_issues').length
  const pass = allRooms.filter(r => r.status === 'pass').length
  return { total, checked, issues, pass, percentage: total ? Math.round((checked / total) * 100) : 0 }
})

const checklistProgress = computed(() => {
  const total = checklistItems.value.length
  const done = checklistItems.value.filter(i => i.result !== null).length
  const passed = checklistItems.value.filter(i => i.result === 'PASS').length
  const failed = checklistItems.value.filter(i => i.result === 'FAIL').length
  return {
    total, done, passed, failed,
    percentage: total ? Math.round((done / total) * 100) : 0,
    passRate: (done - failed) > 0 && done > 0 ? Math.round(((passed) / (done - checklistItems.value.filter(i => i.result === 'NA').length)) * 100) : 0
  }
})

const recentEntries = ref<RecordedEntry[]>([
  { id: 1, time: '08:07', target: '303', className: '软件1班', itemName: '地面不洁', score: -2 },
  { id: 2, time: '08:15', target: '307', className: '物流1班', itemName: '被褥未叠', score: -1.5 },
  { id: 3, time: '08:15', target: '307', className: '物流1班', itemName: '垃圾未倒', score: -1.5 },
])

// ─── Methods ───
const modeOptions = [
  { value: 'SPACE_FIRST', label: '物理空间', icon: Building2, desc: '按楼层房间号检查' },
  { value: 'PERSON_FIRST', label: '人员搜索', icon: Users, desc: '按学生姓名/学号' },
  { value: 'CHECKLIST', label: '逐项核验', icon: ClipboardCheck, desc: '逐项确认通过/不通过' },
  { value: 'ORG_FIRST', label: '按班级', icon: LayoutGrid, desc: '传统按班级录入' },
]

function selectRoom(room: RoomInfo) {
  selectedRoom.value = room
  showDeductionPanel.value = true
  selectedCategory.value = 0
  selectedDeductionItem.value = null
}

function markRoomPass() {
  if (selectedRoom.value) {
    selectedRoom.value.status = 'pass'
    showDeductionPanel.value = false
    // Auto-advance to next unchecked room
    const rooms = currentRooms.value
    const idx = rooms.findIndex(r => r.spaceId === selectedRoom.value?.spaceId)
    const next = rooms.slice(idx + 1).find(r => r.status === 'unchecked')
    if (next) selectRoom(next)
    else selectedRoom.value = null
  }
}

function applyDeduction(item: DeductionItem) {
  if (selectedRoom.value) {
    selectedRoom.value.status = 'has_issues'
    selectedRoom.value.deductionScore += (item.fixedScore || 0)
    selectedRoom.value.deductionCount += 1
    const className = selectedRoom.value.classes.map(c => c.className).join(', ')
    recentEntries.value.unshift({
      id: Date.now(),
      time: new Date().toTimeString().slice(0, 5),
      target: selectedRoom.value.spaceNo,
      className,
      itemName: item.itemName,
      score: -(item.fixedScore || 0)
    })
    ElMessage.success(`${selectedRoom.value.spaceNo} - ${item.itemName} 已记录`)
  }
}

function searchStudents() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  searching.value = true
  setTimeout(() => {
    searchResults.value = mockStudents.filter(s =>
      s.name.includes(searchKeyword.value) || s.studentNo.includes(searchKeyword.value)
    )
    searching.value = false
  }, 300)
}

function toggleStudent(student: StudentResult) {
  const idx = selectedStudents.value.findIndex(s => s.studentId === student.studentId)
  if (idx >= 0) selectedStudents.value.splice(idx, 1)
  else selectedStudents.value.push(student)
}

function isStudentSelected(id: number) {
  return selectedStudents.value.some(s => s.studentId === id)
}

function confirmPersonDeduction() {
  if (!selectedStudents.value.length || !personItemId.value) return
  const item = categories.flatMap(c => c.items).find(i => i.id === personItemId.value)
  if (!item) return
  const groups = new Map<string, StudentResult[]>()
  selectedStudents.value.forEach(s => {
    if (!groups.has(s.className)) groups.set(s.className, [])
    groups.get(s.className)!.push(s)
  })
  groups.forEach((students, className) => {
    recentEntries.value.unshift({
      id: Date.now() + Math.random(),
      time: new Date().toTimeString().slice(0, 5),
      target: students.map(s => s.name).join(', '),
      className,
      itemName: item.itemName,
      score: -(item.fixedScore || 0)
    })
  })
  ElMessage.success(`已为 ${selectedStudents.value.length} 名学生记录扣分`)
  selectedStudents.value = []
  searchKeyword.value = ''
  searchResults.value = []
  personItemId.value = null
}

// Checklist mode
function initChecklist() {
  const rooms = currentRooms.value
  if (rooms.length > 0 && checklistRoomIndex.value < rooms.length) {
    checklistItems.value = categories[0].items.map(item => ({
      itemId: item.id,
      itemName: item.itemName,
      result: null,
      deductScore: item.fixedScore || 0,
      remark: ''
    }))
  }
}

function setChecklistResult(itemId: number, result: ChecklistResult) {
  const item = checklistItems.value.find(i => i.itemId === itemId)
  if (item) item.result = result
}

function markAllPass() {
  checklistItems.value.forEach(item => {
    if (item.result === null) item.result = 'PASS'
  })
}

watch(inputMode, (mode) => {
  if (mode === 'CHECKLIST') initChecklist()
})

watch(searchKeyword, () => {
  searchStudents()
})

function handleSave() {
  saving.value = true
  setTimeout(() => {
    saving.value = false
    ElMessage.success('暂存成功')
  }, 800)
}

function getRoomCardClass(room: RoomInfo) {
  const base = 'relative flex flex-col items-center justify-center rounded-xl border-2 p-4 cursor-pointer transition-all duration-200 hover:shadow-md min-h-[100px]'
  const selected = selectedRoom.value?.spaceId === room.spaceId ? ' ring-2 ring-blue-500 ring-offset-2' : ''
  if (room.status === 'pass') return `${base} bg-emerald-50 border-emerald-300 hover:bg-emerald-100${selected}`
  if (room.status === 'has_issues') return `${base} bg-red-50 border-red-300 hover:bg-red-100${selected}`
  return `${base} bg-gray-50 border-gray-200 hover:bg-gray-100 hover:border-gray-300${selected}`
}
</script>

<template>
  <div class="flex h-[calc(100vh-64px)] flex-col overflow-hidden bg-gray-50">
    <!-- Top toolbar -->
    <div class="flex-shrink-0 border-b border-gray-200 bg-white px-6 py-3">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <button class="rounded-lg p-2 text-gray-500 hover:bg-gray-100">
            <ArrowLeft class="h-5 w-5" />
          </button>
          <div>
            <h1 class="text-lg font-semibold text-gray-900">宿舍卫生检查</h1>
            <p class="text-xs text-gray-500">2026年1月27日 | 检查员: 王检查员</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <button
            class="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="handleSave"
          >
            <Save class="h-4 w-4" />
            <span v-if="!saving">暂存</span>
            <Loader2 v-else class="h-4 w-4 animate-spin" />
          </button>
          <button class="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 active:bg-blue-800">
            <Send class="h-4 w-4" />
            提交检查
          </button>
        </div>
      </div>

      <!-- Mode selector & progress -->
      <div class="mt-3 flex items-center justify-between">
        <div class="flex rounded-lg border border-gray-200 bg-gray-100 p-1">
          <button
            v-for="mode in modeOptions"
            :key="mode.value"
            :class="[
              'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium transition-all',
              inputMode === mode.value
                ? 'bg-white text-blue-700 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            ]"
            @click="inputMode = mode.value as InputMode"
          >
            <component :is="mode.icon" class="h-3.5 w-3.5" />
            {{ mode.label }}
          </button>
        </div>
        <div v-if="inputMode === 'SPACE_FIRST'" class="flex items-center gap-4 text-sm text-gray-600">
          <span class="flex items-center gap-1">
            <span class="inline-block h-3 w-3 rounded-sm bg-emerald-400"></span>
            合格 {{ progressStats.pass }}
          </span>
          <span class="flex items-center gap-1">
            <span class="inline-block h-3 w-3 rounded-sm bg-red-400"></span>
            有问题 {{ progressStats.issues }}
          </span>
          <span class="flex items-center gap-1">
            <span class="inline-block h-3 w-3 rounded-sm bg-gray-300"></span>
            未检查 {{ progressStats.total - progressStats.checked }}
          </span>
          <div class="flex items-center gap-2">
            <ElProgress :percentage="progressStats.percentage" :stroke-width="8" class="w-32" :show-text="false" color="#3b82f6" />
            <span class="text-xs font-medium text-blue-600">{{ progressStats.percentage }}%</span>
          </div>
        </div>
        <div v-if="inputMode === 'CHECKLIST'" class="flex items-center gap-4 text-sm text-gray-600">
          <span>完成: {{ checklistProgress.done }}/{{ checklistProgress.total }}</span>
          <span class="text-emerald-600">通过率: {{ checklistProgress.passRate }}%</span>
          <ElProgress :percentage="checklistProgress.percentage" :stroke-width="8" class="w-32" :show-text="false" color="#3b82f6" />
        </div>
      </div>
    </div>

    <!-- Main content area -->
    <div class="flex flex-1 overflow-hidden">
      <!-- ========== SPACE FIRST MODE ========== -->
      <template v-if="inputMode === 'SPACE_FIRST'">
        <!-- Left sidebar: Building & floor navigation -->
        <div class="w-60 flex-shrink-0 overflow-y-auto border-r border-gray-200 bg-white">
          <div class="p-4">
            <h3 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-500">楼栋导航</h3>
            <div v-for="(building, bIdx) in buildings" :key="building.buildingId" class="mb-2">
              <button
                :class="[
                  'w-full rounded-lg px-3 py-2 text-left text-sm font-medium transition-colors',
                  selectedBuilding === bIdx ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'
                ]"
                @click="selectedBuilding = bIdx; selectedFloor = building.floors[0]?.floor || 1"
              >
                <Building2 class="mr-2 inline h-4 w-4" />
                {{ building.buildingName }}
              </button>
              <div v-if="selectedBuilding === bIdx" class="ml-6 mt-1 space-y-0.5">
                <button
                  v-for="floor in building.floors"
                  :key="floor.floor"
                  :class="[
                    'flex w-full items-center justify-between rounded-md px-3 py-1.5 text-xs transition-colors',
                    selectedFloor === floor.floor
                      ? 'bg-blue-100 font-medium text-blue-700'
                      : 'text-gray-600 hover:bg-gray-100'
                  ]"
                  @click="selectedFloor = floor.floor"
                >
                  <span>{{ floor.floor }}层</span>
                  <span class="rounded-full bg-gray-200 px-1.5 py-0.5 text-[10px] font-medium text-gray-600">
                    {{ floor.rooms.length }}间
                  </span>
                </button>
              </div>
            </div>
          </div>
          <div class="border-t border-gray-200 p-4">
            <h3 class="mb-2 text-xs font-semibold uppercase tracking-wider text-gray-500">检查统计</h3>
            <div class="space-y-2 text-sm">
              <div class="flex justify-between">
                <span class="text-gray-500">已检查</span>
                <span class="font-medium text-gray-900">{{ progressStats.checked }}间</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-500">有问题</span>
                <span class="font-medium text-red-600">{{ progressStats.issues }}间</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-500">合格</span>
                <span class="font-medium text-emerald-600">{{ progressStats.pass }}间</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-500">未检查</span>
                <span class="font-medium text-gray-600">{{ progressStats.total - progressStats.checked }}间</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Right main: Room grid -->
        <div class="flex-1 overflow-y-auto p-6">
          <div class="mb-4 flex items-center justify-between">
            <h2 class="text-base font-semibold text-gray-800">
              <MapPin class="mr-1 inline h-4 w-4 text-gray-400" />
              {{ currentBuilding?.buildingName }} - {{ selectedFloor }}层
            </h2>
            <span class="text-xs text-gray-500">点击房间快速录入</span>
          </div>
          <div class="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            <div
              v-for="room in currentRooms"
              :key="room.spaceId"
              :class="getRoomCardClass(room)"
              @click="selectRoom(room)"
            >
              <span class="text-lg font-bold" :class="room.status === 'has_issues' ? 'text-red-700' : room.status === 'pass' ? 'text-emerald-700' : 'text-gray-700'">
                {{ room.spaceNo }}
              </span>
              <Check v-if="room.status === 'pass'" class="absolute right-2 top-2 h-4 w-4 text-emerald-500" />
              <AlertTriangle v-if="room.status === 'has_issues'" class="absolute right-2 top-2 h-4 w-4 text-red-500" />
              <span v-if="room.status === 'has_issues'" class="mt-1 text-xs font-semibold text-red-600">-{{ room.deductionScore }}分</span>
              <div class="mt-1.5 flex flex-wrap justify-center gap-1">
                <span
                  v-for="cls in room.classes"
                  :key="cls.classId"
                  class="rounded bg-gray-200/70 px-1.5 py-0.5 text-[10px] text-gray-600"
                >
                  {{ cls.className }}
                </span>
              </div>
            </div>
          </div>

          <!-- Recent entries -->
          <div v-if="recentEntries.length" class="mt-8">
            <h3 class="mb-3 text-sm font-semibold text-gray-700">本次已录入记录</h3>
            <div class="space-y-2">
              <div
                v-for="entry in recentEntries.slice(0, 8)"
                :key="entry.id"
                class="flex items-center justify-between rounded-lg border border-gray-200 bg-white px-4 py-2.5 text-sm"
              >
                <div class="flex items-center gap-3">
                  <span class="text-xs text-gray-400">{{ entry.time }}</span>
                  <span class="font-medium text-gray-800">{{ entry.target }}</span>
                  <span class="text-gray-500">{{ entry.className }}</span>
                  <span class="text-gray-700">{{ entry.itemName }}</span>
                </div>
                <div class="flex items-center gap-3">
                  <span class="font-semibold text-red-600">{{ entry.score }}分</span>
                  <button class="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500">
                    <Trash2 class="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Deduction panel (slide-in drawer) -->
        <Transition name="slide">
          <div v-if="showDeductionPanel && selectedRoom" class="w-96 flex-shrink-0 overflow-y-auto border-l border-gray-200 bg-white shadow-xl">
            <div class="sticky top-0 z-10 flex items-center justify-between border-b border-gray-200 bg-white px-5 py-3">
              <div>
                <h3 class="text-base font-semibold text-gray-900">{{ selectedRoom.spaceNo }}号宿舍</h3>
                <p class="mt-0.5 text-xs text-gray-500">
                  {{ selectedRoom.classes.map(c => `${c.className}(${c.studentCount}人)`).join(' + ') }}
                </p>
              </div>
              <button class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100" @click="showDeductionPanel = false">
                <X class="h-5 w-5" />
              </button>
            </div>

            <div class="p-5">
              <!-- Quick action: No issues -->
              <button
                class="mb-5 flex w-full items-center justify-center gap-2 rounded-xl border-2 border-dashed border-emerald-300 bg-emerald-50 py-3 text-sm font-medium text-emerald-700 transition-colors hover:bg-emerald-100"
                @click="markRoomPass"
              >
                <Check class="h-5 w-5" />
                无问题，标记为合格
              </button>

              <!-- Category tabs -->
              <div class="mb-4 flex gap-2">
                <button
                  v-for="(cat, idx) in categories"
                  :key="cat.categoryId"
                  :class="[
                    'rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                    selectedCategory === idx
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  ]"
                  @click="selectedCategory = idx"
                >
                  {{ cat.categoryName }}
                </button>
              </div>

              <!-- Deduction items grid -->
              <div class="grid grid-cols-2 gap-2">
                <button
                  v-for="item in categories[selectedCategory]?.items"
                  :key="item.id"
                  class="flex flex-col items-center gap-1 rounded-xl border border-gray-200 bg-gray-50 p-3 text-center transition-all hover:border-red-300 hover:bg-red-50 hover:shadow-sm active:scale-95"
                  @click="applyDeduction(item)"
                >
                  <span class="text-lg">{{ item.icon }}</span>
                  <span class="text-xs font-medium text-gray-800">{{ item.itemName }}</span>
                  <span class="text-xs font-bold text-red-500">
                    -{{ item.fixedScore || item.baseScore }}<span v-if="item.deductMode === 2">/人</span>
                  </span>
                </button>
              </div>

              <!-- Extra actions -->
              <div class="mt-5 space-y-3">
                <div>
                  <label class="mb-1 block text-xs font-medium text-gray-600">备注</label>
                  <textarea
                    class="h-16 w-full resize-none rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
                    placeholder="补充说明（选填）"
                  ></textarea>
                </div>
                <button class="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 py-2.5 text-sm text-gray-600 hover:bg-gray-50">
                  <Camera class="h-4 w-4" />
                  拍照取证
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </template>

      <!-- ========== PERSON FIRST MODE ========== -->
      <template v-if="inputMode === 'PERSON_FIRST'">
        <div class="flex-1 overflow-y-auto p-6">
          <div class="mx-auto max-w-3xl">
            <!-- Search box -->
            <div class="mb-6">
              <div class="relative">
                <Search class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
                <input
                  v-model="searchKeyword"
                  type="text"
                  class="h-12 w-full rounded-xl border border-gray-300 bg-white pl-12 pr-24 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="输入学生姓名或学号搜索..."
                />
                <button class="absolute right-2 top-1/2 flex -translate-y-1/2 items-center gap-1.5 rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-200">
                  <ScanLine class="h-4 w-4" />
                  扫码
                </button>
              </div>
            </div>

            <!-- Search results -->
            <div v-if="searchResults.length" class="mb-6 rounded-xl border border-gray-200 bg-white shadow-sm">
              <div class="border-b border-gray-100 px-4 py-2.5">
                <span class="text-xs font-medium text-gray-500">搜索结果 ({{ searchResults.length }})</span>
              </div>
              <div class="divide-y divide-gray-100">
                <button
                  v-for="student in searchResults"
                  :key="student.studentId"
                  :class="[
                    'flex w-full items-center gap-3 px-4 py-3 text-left transition-colors',
                    isStudentSelected(student.studentId) ? 'bg-blue-50' : 'hover:bg-gray-50'
                  ]"
                  @click="toggleStudent(student)"
                >
                  <div :class="[
                    'flex h-5 w-5 items-center justify-center rounded border-2 transition-colors',
                    isStudentSelected(student.studentId) ? 'border-blue-600 bg-blue-600' : 'border-gray-300'
                  ]">
                    <Check v-if="isStudentSelected(student.studentId)" class="h-3 w-3 text-white" />
                  </div>
                  <div class="flex-1">
                    <span class="font-medium text-gray-900">{{ student.name }}</span>
                    <span class="ml-2 text-xs text-gray-400">{{ student.studentNo }}</span>
                  </div>
                  <span class="text-xs text-gray-500">{{ student.className }}</span>
                  <span class="text-xs text-gray-400">{{ student.orgUnitName }}</span>
                </button>
              </div>
            </div>

            <!-- Loading -->
            <div v-if="searching" class="mb-6 flex items-center justify-center py-8">
              <Loader2 class="h-6 w-6 animate-spin text-blue-500" />
            </div>

            <!-- Selected students -->
            <div v-if="selectedStudents.length" class="mb-6">
              <h3 class="mb-2 text-sm font-semibold text-gray-700">已选学生 ({{ selectedStudents.length }}人)</h3>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="student in selectedStudents"
                  :key="student.studentId"
                  class="inline-flex items-center gap-1.5 rounded-full bg-blue-100 py-1 pl-3 pr-1.5 text-sm text-blue-800"
                >
                  {{ student.name }}
                  <span class="text-xs text-blue-500">{{ student.className }}</span>
                  <button class="ml-0.5 rounded-full p-0.5 hover:bg-blue-200" @click="toggleStudent(student)">
                    <X class="h-3.5 w-3.5" />
                  </button>
                </span>
              </div>
            </div>

            <!-- Deduction selection -->
            <div v-if="selectedStudents.length" class="mb-6 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
              <h3 class="mb-4 text-sm font-semibold text-gray-800">选择扣分项</h3>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="mb-1 block text-xs text-gray-500">检查类别</label>
                  <select
                    v-model="personCategoryId"
                    class="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option :value="null" disabled>请选择类别</option>
                    <option v-for="cat in categories" :key="cat.categoryId" :value="cat.categoryId">
                      {{ cat.categoryName }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-xs text-gray-500">扣分项</label>
                  <select
                    v-model="personItemId"
                    class="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
                  >
                    <option :value="null" disabled>请选择扣分项</option>
                    <template v-for="cat in categories" :key="cat.categoryId">
                      <option
                        v-for="item in cat.items"
                        v-show="!personCategoryId || cat.categoryId === personCategoryId"
                        :key="item.id"
                        :value="item.id"
                      >
                        {{ item.itemName }} (-{{ item.fixedScore || item.baseScore }})
                      </option>
                    </template>
                  </select>
                </div>
              </div>

              <!-- Preview -->
              <div v-if="personItemId" class="mt-4 rounded-lg bg-blue-50 px-4 py-3">
                <p class="text-xs font-medium text-blue-800">
                  系统将自动按班级生成 {{ new Set(selectedStudents.map(s => s.className)).size }} 条扣分记录
                </p>
              </div>

              <button
                class="mt-4 w-full rounded-lg bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
                :disabled="!personItemId"
                @click="confirmPersonDeduction"
              >
                确认录入
              </button>
            </div>

            <!-- Recent log -->
            <div v-if="recentEntries.length" class="rounded-xl border border-gray-200 bg-white shadow-sm">
              <div class="border-b border-gray-100 px-4 py-2.5">
                <span class="text-sm font-semibold text-gray-700">本次录入记录</span>
              </div>
              <div class="divide-y divide-gray-100">
                <div
                  v-for="entry in recentEntries"
                  :key="entry.id"
                  class="flex items-center justify-between px-4 py-3 text-sm"
                >
                  <div class="flex items-center gap-3">
                    <span class="text-xs text-gray-400">{{ entry.time }}</span>
                    <span class="font-medium text-gray-800">{{ entry.target }}</span>
                    <span class="text-gray-500">({{ entry.className }})</span>
                    <span class="text-gray-600">{{ entry.itemName }}</span>
                  </div>
                  <span class="font-semibold text-red-600">{{ entry.score }}分</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- ========== CHECKLIST MODE ========== -->
      <template v-if="inputMode === 'CHECKLIST'">
        <!-- Left sidebar: Room list -->
        <div class="w-60 flex-shrink-0 overflow-y-auto border-r border-gray-200 bg-white">
          <div class="p-4">
            <h3 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-500">房间列表</h3>
            <div class="space-y-1">
              <button
                v-for="(room, idx) in currentRooms"
                :key="room.spaceId"
                :class="[
                  'flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm transition-colors',
                  checklistRoomIndex === idx
                    ? 'bg-blue-50 font-medium text-blue-700'
                    : 'text-gray-700 hover:bg-gray-50'
                ]"
                @click="checklistRoomIndex = idx; initChecklist()"
              >
                <span>{{ room.spaceNo }}</span>
                <span :class="[
                  'rounded-full px-2 py-0.5 text-[10px] font-medium',
                  room.status === 'pass' ? 'bg-emerald-100 text-emerald-600' :
                  room.status === 'has_issues' ? 'bg-red-100 text-red-600' :
                  'bg-gray-100 text-gray-500'
                ]">
                  {{ room.status === 'pass' ? '已完成' : room.status === 'has_issues' ? '有问题' : '待检查' }}
                </span>
              </button>
            </div>
          </div>
        </div>

        <!-- Right: Checklist -->
        <div class="flex-1 overflow-y-auto p-6">
          <div class="mx-auto max-w-2xl">
            <div class="mb-4 flex items-center justify-between">
              <div>
                <h2 class="text-base font-semibold text-gray-800">
                  {{ currentRooms[checklistRoomIndex]?.spaceNo }}号宿舍
                </h2>
                <p class="mt-0.5 text-xs text-gray-500">
                  {{ currentRooms[checklistRoomIndex]?.classes.map(c => c.className).join(' + ') }}
                </p>
              </div>
              <button
                class="rounded-lg border border-emerald-300 bg-emerald-50 px-3 py-1.5 text-xs font-medium text-emerald-700 hover:bg-emerald-100"
                @click="markAllPass"
              >
                全部标记为通过
              </button>
            </div>

            <div class="space-y-3">
              <div
                v-for="item in checklistItems"
                :key="item.itemId"
                class="rounded-xl border bg-white shadow-sm transition-all"
                :class="item.result === 'FAIL' ? 'border-red-200' : item.result === 'PASS' ? 'border-emerald-200' : 'border-gray-200'"
              >
                <div class="flex items-center justify-between px-5 py-4">
                  <span class="text-sm font-medium text-gray-800">{{ item.itemName }}</span>
                  <div class="flex gap-2">
                    <button
                      :class="[
                        'rounded-lg px-4 py-2 text-xs font-semibold transition-all',
                        item.result === 'PASS'
                          ? 'bg-emerald-500 text-white shadow-sm'
                          : 'bg-gray-100 text-gray-600 hover:bg-emerald-50 hover:text-emerald-700'
                      ]"
                      @click="setChecklistResult(item.itemId, 'PASS')"
                    >
                      <Check class="mr-1 inline h-3.5 w-3.5" /> 通过
                    </button>
                    <button
                      :class="[
                        'rounded-lg px-4 py-2 text-xs font-semibold transition-all',
                        item.result === 'FAIL'
                          ? 'bg-red-500 text-white shadow-sm'
                          : 'bg-gray-100 text-gray-600 hover:bg-red-50 hover:text-red-700'
                      ]"
                      @click="setChecklistResult(item.itemId, 'FAIL')"
                    >
                      <X class="mr-1 inline h-3.5 w-3.5" /> 不通过
                    </button>
                    <button
                      :class="[
                        'rounded-lg px-3 py-2 text-xs font-medium transition-all',
                        item.result === 'NA'
                          ? 'bg-gray-500 text-white'
                          : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                      ]"
                      @click="setChecklistResult(item.itemId, 'NA')"
                    >
                      N/A
                    </button>
                  </div>
                </div>
                <!-- Expanded fail details -->
                <Transition name="expand">
                  <div v-if="item.result === 'FAIL'" class="border-t border-red-100 bg-red-50/50 px-5 py-3">
                    <div class="flex items-center gap-4 text-sm">
                      <span class="font-medium text-red-700">扣分: -{{ item.deductScore }}</span>
                      <button class="flex items-center gap-1 rounded-md bg-white px-2.5 py-1 text-xs text-gray-600 shadow-sm hover:bg-gray-50">
                        <Camera class="h-3.5 w-3.5" /> 拍照
                      </button>
                      <input
                        v-model="item.remark"
                        class="flex-1 rounded-md border border-red-200 bg-white px-2.5 py-1 text-xs focus:border-red-400 focus:outline-none"
                        placeholder="备注说明..."
                      />
                    </div>
                  </div>
                </Transition>
              </div>
            </div>

            <!-- Next room button -->
            <div class="mt-6 flex justify-between">
              <button
                v-if="checklistRoomIndex > 0"
                class="flex items-center gap-1.5 rounded-lg border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
                @click="checklistRoomIndex--; initChecklist()"
              >
                <ArrowLeft class="h-4 w-4" /> 上一间
              </button>
              <div v-else></div>
              <button
                v-if="checklistRoomIndex < currentRooms.length - 1"
                class="flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
                @click="checklistRoomIndex++; initChecklist()"
              >
                下一间 <ChevronRight class="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </template>

      <!-- ========== ORG FIRST MODE (simplified) ========== -->
      <template v-if="inputMode === 'ORG_FIRST'">
        <div class="flex-1 overflow-y-auto p-6">
          <div class="mx-auto max-w-2xl">
            <div class="rounded-xl border border-gray-200 bg-white p-6 text-center shadow-sm">
              <LayoutGrid class="mx-auto mb-3 h-12 w-12 text-gray-300" />
              <h3 class="mb-1 text-base font-semibold text-gray-700">传统按班级录入模式</h3>
              <p class="text-sm text-gray-500">选择班级 → 选择检查类别 → 选择扣分项 → 录入分数</p>
              <p class="mt-3 text-xs text-gray-400">此模式保持与现有系统兼容，适合课堂纪律等已知班级的检查场景</p>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
.expand-enter-active,
.expand-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}
.expand-enter-to,
.expand-leave-from {
  max-height: 100px;
  opacity: 1;
}
</style>
