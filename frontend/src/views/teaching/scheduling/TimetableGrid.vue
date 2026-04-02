<template>
  <div class="overflow-x-auto">
    <table class="w-full border-collapse table-fixed">
      <thead>
        <tr>
          <th class="w-20 border-b border-r border-gray-200 bg-gray-50 px-2 py-2.5 text-center text-xs font-semibold text-gray-600">
            节次
          </th>
          <th
            v-for="day in displayWeekdays"
            :key="day.value"
            class="border-b border-r border-gray-200 bg-gray-50 px-2 py-2.5 text-center text-xs font-semibold text-gray-600 last:border-r-0"
          >
            {{ day.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="period in periods" :key="period.period">
          <td class="w-20 border-b border-r border-gray-200 px-2 py-2 text-center">
            <div class="text-xs font-medium text-gray-700">{{ period.name }}</div>
            <div class="text-[10px] text-gray-400">{{ period.startTime }}-{{ period.endTime }}</div>
          </td>
          <td
            v-for="day in displayWeekdays"
            :key="day.value"
            class="border-b border-r border-gray-200 p-1 align-top last:border-r-0"
            :class="[
              editable ? 'cursor-pointer hover:bg-blue-50/50' : '',
              getEntriesForCell(day.value, period.period).length === 0 ? 'min-h-[56px]' : '',
              isDragOver(day.value, period.period) ? 'bg-blue-100 ring-2 ring-inset ring-blue-400' : '',
              isForbiddenSlot(day.value, period.period) ? 'bg-gray-100 opacity-50' : '',
            ]"
            style="min-height: 56px"
            @click="handleCellClick(day.value, period.period)"
            @dragover="onDragOver($event, day.value, period.period)"
            @dragleave="onDragLeave"
            @drop="onDrop($event, day.value, period.period)"
          >
            <div
              v-for="entry in getEntriesForCell(day.value, period.period)"
              :key="entry.id"
              class="mb-1 cursor-pointer rounded-md p-1.5 text-white last:mb-0"
              :class="getEntryColor(entry)"
              :draggable="editable"
              @click.stop="emit('entry-click', entry)"
              @dragstart="onDragStart($event, entry)"
              @dragend="onDragEnd"
            >
              <div class="text-[11px] font-semibold leading-tight">{{ entry.courseName }}</div>
              <div class="text-[10px] leading-tight opacity-90">{{ entry.classroomName }}</div>
              <div class="text-[10px] leading-tight opacity-90">{{ entry.teacherName }}</div>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Empty state -->
    <div
      v-if="entries.length === 0"
      class="flex items-center justify-center py-12 text-sm text-gray-400"
    >
      暂无课表数据
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ScheduleEntry, PeriodConfig } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

interface Props {
  entries: ScheduleEntry[]
  periods?: PeriodConfig[]
  weekdays?: { value: number; label: string }[]
  editable?: boolean
  constraintMatrix?: any[][]
}

const props = withDefaults(defineProps<Props>(), {
  periods: () => DEFAULT_PERIODS,
  weekdays: undefined,
  editable: false,
  constraintMatrix: undefined,
})

const emit = defineEmits<{
  'entry-click': [entry: ScheduleEntry]
  'cell-click': [day: number, period: number]
  'entry-drop': [entryId: number, newDay: number, newPeriod: number]
}>()

const displayWeekdays = computed(() => {
  return props.weekdays ?? WEEKDAYS.slice(0, 5)
})

const colors = [
  'bg-blue-500',
  'bg-emerald-500',
  'bg-amber-500',
  'bg-purple-500',
  'bg-rose-500',
  'bg-cyan-500',
  'bg-indigo-500',
  'bg-teal-500',
]

// ==================== Drag-and-Drop State ====================

const draggedEntry = ref<any>(null)
const dragOverCell = ref<{ day: number; period: number } | null>(null)

function onDragStart(e: DragEvent, entry: any) {
  if (!props.editable) return
  draggedEntry.value = entry
  e.dataTransfer!.effectAllowed = 'move'
  e.dataTransfer!.setData('text/plain', String(entry.id))
}

function onDragOver(e: DragEvent, day: number, period: number) {
  if (!props.editable || !draggedEntry.value) return
  e.preventDefault()
  dragOverCell.value = { day, period }
}

function onDragLeave() {
  dragOverCell.value = null
}

function onDrop(e: DragEvent, day: number, period: number) {
  e.preventDefault()
  if (!draggedEntry.value) return
  emit('entry-drop', draggedEntry.value.id, day, period)
  draggedEntry.value = null
  dragOverCell.value = null
}

function onDragEnd() {
  draggedEntry.value = null
  dragOverCell.value = null
}

function isDragOver(day: number, period: number): boolean {
  return dragOverCell.value?.day === day && dragOverCell.value?.period === period
}

function isForbiddenSlot(day: number, period: number): boolean {
  if (!props.constraintMatrix) return false
  const dayRow = props.constraintMatrix[day - 1]
  if (!dayRow) return false
  return dayRow[period - 1]?.status === 'forbidden'
}

// ==================== Cell Logic ====================

function getEntryColor(entry: ScheduleEntry) {
  const idx = Number(entry.taskId ?? 0) % colors.length
  return colors[idx]
}

function getEntriesForCell(day: number, period: number) {
  return props.entries.filter(
    e => e.dayOfWeek === day && e.periodStart <= period && e.periodEnd >= period,
  )
}

function handleCellClick(day: number, period: number) {
  if (props.editable) {
    emit('cell-click', day, period)
  }
}
</script>
