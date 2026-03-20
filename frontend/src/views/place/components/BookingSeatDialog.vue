<template>
  <el-dialog
    :model-value="visible"
    :title="`排座 · ${booking?.title || '无标题'}`"
    width="920px"
    :close-on-click-modal="false"
    destroy-on-close
    align-center
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="flex gap-4" style="height: 480px;">
      <!-- Left: Attendee list -->
      <div class="w-52 shrink-0 flex flex-col rounded-lg border border-gray-200 bg-gray-50/50">
        <div class="shrink-0 border-b border-gray-200 bg-gray-50 px-3 py-2">
          <span class="text-xs font-semibold text-gray-700">参会人 ({{ attendees.length }})</span>
        </div>
        <div class="shrink-0 border-b border-gray-100 px-2 py-1.5">
          <input
            v-model="attendeeFilter"
            type="text"
            placeholder="搜索..."
            class="w-full rounded border border-gray-200 bg-white px-2 py-1 text-xs text-gray-700 outline-none focus:border-blue-300"
          />
        </div>
        <div class="flex-1 overflow-y-auto">
          <div v-if="filteredAttendees.length === 0" class="px-3 py-4 text-center text-xs text-gray-400">
            暂无参会人
          </div>
          <div
            v-for="att in filteredAttendees"
            :key="att.userId"
            draggable="true"
            class="flex cursor-grab items-center gap-2 border-b border-gray-100 px-3 py-2 text-xs transition-colors select-none"
            :class="{
              'bg-blue-50 ring-1 ring-inset ring-blue-200': isSelected(att),
              'hover:bg-gray-100': !isSelected(att),
              'opacity-50': !!getAssignedPosition(att.userId),
            }"
            @click="selectAttendee(att)"
            @dragstart="onDragStart($event, att)"
            @dragend="onDragEnd"
          >
            <span
              class="h-2 w-2 shrink-0 rounded-full"
              :class="getAssignedPosition(att.userId) ? 'bg-emerald-500' : 'bg-gray-300'"
            />
            <span class="truncate font-medium text-gray-800">{{ att.realName || att.username }}</span>
            <span
              v-if="getAssignedPosition(att.userId)"
              class="ml-auto shrink-0 rounded bg-emerald-50 px-1 py-px text-[10px] text-emerald-600"
            >{{ getAssignedPosition(att.userId) }}</span>
          </div>
        </div>
        <div class="shrink-0 border-t border-gray-200 px-3 py-1.5 text-[10px] text-gray-400">
          <span v-if="selectedAttendee" class="text-blue-500">
            选中: {{ selectedAttendee.realName || selectedAttendee.username }}
          </span>
          <span v-else>选中或拖拽参会人到座位</span>
        </div>
      </div>

      <!-- Right: Floor plan with drop zone -->
      <div
        ref="floorPlanContainerRef"
        class="relative flex-1 overflow-hidden rounded-lg border border-gray-200"
        :class="{ 'ring-2 ring-blue-300 ring-inset': isDragOver }"
        @dragover.prevent="onDragOver"
        @dragleave="onDragLeave"
        @drop.prevent="onDrop"
      >
        <div v-if="placeLayout" class="relative h-full">
          <FloorPlanEditor
            ref="floorPlanRef"
            :place-id="booking?.placeId ?? 0"
            :initial-layout="placeLayout"
            :occupants="occupantsForFloorPlan"
            :show-seat-labels="showSeatLabels"
            mode="view"
            @seat-click="handleSeatClick"
          />
          <!-- Toggle seat labels button -->
          <button
            class="absolute right-2 top-2 z-10 rounded border px-2 py-1 text-[10px] font-medium shadow-sm transition-colors"
            :class="showSeatLabels
              ? 'border-blue-300 bg-blue-50 text-blue-600'
              : 'border-gray-200 bg-white text-gray-500 hover:bg-gray-50'"
            @click="showSeatLabels = !showSeatLabels"
          >{{ showSeatLabels ? '隐藏座位号' : '显示座位号' }}</button>
        </div>
        <div v-else class="flex h-full items-center justify-center text-xs text-gray-400">
          该场所没有平面图布局
        </div>
        <!-- Drag overlay hint -->
        <div
          v-if="isDragOver"
          class="pointer-events-none absolute inset-0 flex items-center justify-center bg-blue-50/40"
        >
          <span class="rounded-lg bg-blue-500 px-3 py-1.5 text-xs font-medium text-white shadow">
            松开分配到最近座位
          </span>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <template #footer>
      <div class="flex items-center justify-between">
        <span class="text-xs text-gray-500">
          已排 {{ assignedCount }}/{{ attendees.length }}
        </span>
        <div class="flex items-center gap-2">
          <el-button size="small" @click="clearAll" :disabled="assignedCount === 0">清除全部</el-button>
          <el-button size="small" @click="$emit('update:visible', false)">取消</el-button>
          <el-button type="primary" size="small" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import FloorPlanEditor from './FloorPlanEditor.vue'
import { universalPlaceApi } from '@/api/universalPlace'
import type {
  PlaceBooking,
  FloorPlanLayout,
  FloorPlanElement,
  PlaceOccupant,
  BookingAttendeeInfo,
  BookingSeatAssignment,
  SaveSeatAssignmentRequest
} from '@/types/universalPlace'

interface Props {
  visible: boolean
  booking: PlaceBooking | null
  placeLayout: FloorPlanLayout | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const floorPlanRef = ref<InstanceType<typeof FloorPlanEditor> | null>(null)
const floorPlanContainerRef = ref<HTMLElement | null>(null)

// State
const assignments = ref<Record<string, { userId: number | string; userName: string }>>({})
const attendees = ref<BookingAttendeeInfo[]>([])
const selectedAttendee = ref<BookingAttendeeInfo | null>(null)
const saving = ref(false)
const attendeeFilter = ref('')
const isDragOver = ref(false)
const showSeatLabels = ref(false)
let dragAttendee: BookingAttendeeInfo | null = null

const assignedCount = computed(() => Object.keys(assignments.value).length)

const filteredAttendees = computed(() => {
  const kw = attendeeFilter.value.trim().toLowerCase()
  if (!kw) return attendees.value
  return attendees.value.filter(a =>
    (a.realName || '').toLowerCase().includes(kw) ||
    (a.username || '').toLowerCase().includes(kw)
  )
})

// Convert assignments → PlaceOccupant[] for FloorPlanEditor
const occupantsForFloorPlan = computed<PlaceOccupant[]>(() =>
  Object.entries(assignments.value).map(([positionNo, user]) => ({
    id: 0,
    placeId: props.booking?.placeId ?? 0,
    occupantType: 'ATTENDEE',
    occupantId: user.userId,
    occupantName: user.userName,
    positionNo,
    checkInTime: '',
    status: 1,
  }))
)

// Get all seat elements from the layout
const seatElements = computed<FloorPlanElement[]>(() => {
  if (!props.placeLayout) return []
  return props.placeLayout.elements.filter(
    el => (el.type === 'seat' || el.type === 'seat-desk') && el.positionNo
  )
})

function getAssignedPosition(userId: number | string): string | null {
  for (const [pos, user] of Object.entries(assignments.value)) {
    if (String(user.userId) === String(userId)) return pos
  }
  return null
}

function isSelected(att: BookingAttendeeInfo): boolean {
  return selectedAttendee.value != null && String(selectedAttendee.value.userId) === String(att.userId)
}

function selectAttendee(att: BookingAttendeeInfo) {
  if (isSelected(att)) {
    selectedAttendee.value = null
  } else {
    selectedAttendee.value = att
  }
}

// ==================== Assign logic ====================

function assignToSeat(att: BookingAttendeeInfo, positionNo: string) {
  const copy = { ...assignments.value }

  // Remove attendee's old position if any
  const existingPos = getAssignedPosition(att.userId)
  if (existingPos) {
    delete copy[existingPos]
  }

  // Remove current occupant of target seat
  // (overwrite is fine — copy[positionNo] gets replaced)

  copy[positionNo] = {
    userId: att.userId,
    userName: att.realName || att.username,
  }
  assignments.value = copy

  // Auto-select next unassigned
  nextTick(() => {
    const next = attendees.value.find(a => !getAssignedPosition(a.userId))
    selectedAttendee.value = next || null
  })
}

function unassignSeat(positionNo: string) {
  const copy = { ...assignments.value }
  delete copy[positionNo]
  assignments.value = copy
}

// ==================== Click handler (from FloorPlanEditor seat-click) ====================

function handleSeatClick(positionNo: string) {
  const existing = assignments.value[positionNo]

  if (existing) {
    unassignSeat(positionNo)
    return
  }

  if (!selectedAttendee.value) {
    ElMessage.warning('请先在左侧选择一位参会人')
    return
  }

  assignToSeat(selectedAttendee.value, positionNo)
}

// ==================== Drag & Drop ====================

function onDragStart(e: DragEvent, att: BookingAttendeeInfo) {
  dragAttendee = att
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(att.userId))
  }
}

function onDragEnd() {
  dragAttendee = null
  isDragOver.value = false
}

function onDragOver(e: DragEvent) {
  if (!dragAttendee) return
  isDragOver.value = true
  if (e.dataTransfer) {
    e.dataTransfer.dropEffect = 'move'
  }
}

function onDragLeave() {
  isDragOver.value = false
}

function onDrop(e: DragEvent) {
  isDragOver.value = false
  if (!dragAttendee || !floorPlanRef.value || !floorPlanContainerRef.value) return

  const att = dragAttendee
  dragAttendee = null

  // Find nearest seat to drop position
  const nearestSeat = findNearestSeat(e.clientX, e.clientY)
  if (!nearestSeat) {
    ElMessage.warning('未找到附近的座位')
    return
  }

  assignToSeat(att, nearestSeat)
}

function findNearestSeat(clientX: number, clientY: number): string | null {
  if (seatElements.value.length === 0) return null

  // Get the canvas container's bounding rect
  const container = floorPlanRef.value?.getCanvasContainer?.()
  if (!container) return null
  const rect = container.getBoundingClientRect()

  // Get stage transform (zoom & pan)
  const transform = floorPlanRef.value?.getStageTransform?.()
  const stageScale = transform?.scale ?? 1
  const stageX = transform?.x ?? 0
  const stageY = transform?.y ?? 0

  // Convert client coords → canvas coords (accounting for zoom/pan)
  const canvasX = (clientX - rect.left - stageX) / stageScale
  const canvasY = (clientY - rect.top - stageY) / stageScale

  // Find nearest seat
  let bestDist = Infinity
  let bestPositionNo: string | null = null

  for (const el of seatElements.value) {
    const cx = el.x + (el.width || 30) / 2
    const cy = el.y + (el.height || 30) / 2
    const dist = Math.hypot(canvasX - cx, canvasY - cy)
    if (dist < bestDist) {
      bestDist = dist
      bestPositionNo = el.positionNo!
    }
  }

  // Limit max distance (don't assign if dropped too far from any seat)
  const maxDist = 80 / stageScale
  if (bestDist > maxDist) return null

  return bestPositionNo
}

// ==================== Actions ====================

function clearAll() {
  assignments.value = {}
  selectedAttendee.value = attendees.value.length > 0 ? attendees.value[0] : null
}

async function handleSave() {
  if (!props.booking) return
  saving.value = true
  try {
    const data: SaveSeatAssignmentRequest[] = Object.entries(assignments.value).map(
      ([positionNo, user]) => ({
        positionNo,
        userId: user.userId,
        userName: user.userName,
      })
    )
    await universalPlaceApi.saveBookingSeating(props.booking.id, data)
    ElMessage.success('排座已保存')
    emit('update:visible', false)
  } catch {
    /* axios interceptor handles */
  } finally {
    saving.value = false
  }
}

// ==================== Load on open ====================

watch(
  () => props.visible,
  async (val) => {
    if (!val || !props.booking) {
      assignments.value = {}
      attendees.value = []
      selectedAttendee.value = null
      attendeeFilter.value = ''
      return
    }

    // Populate attendees
    attendees.value = props.booking.attendees ?? []

    // Load existing seat assignments
    try {
      const existing: BookingSeatAssignment[] = await universalPlaceApi.getBookingSeating(props.booking.id)
      const obj: Record<string, { userId: number | string; userName: string }> = {}
      for (const a of existing) {
        obj[a.positionNo] = { userId: a.userId, userName: a.userName }
      }
      assignments.value = obj
    } catch {
      assignments.value = {}
    }

    // Auto-select first unassigned attendee
    await nextTick()
    const firstUnassigned = attendees.value.find(a => !getAssignedPosition(a.userId))
    selectedAttendee.value = firstUnassigned || (attendees.value.length > 0 ? attendees.value[0] : null)
  },
  { immediate: true }
)
</script>
