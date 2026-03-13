<script setup lang="ts">
/**
 * TimeSlotEditor - Time slot editor
 *
 * Manages a list of time slots (start/end times) for scheduling
 * inspection tasks within a day.
 */
import { computed } from 'vue'
import { Plus, Trash2, Clock } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

interface TimeSlot {
  startTime: string
  endTime: string
  label?: string
}

const props = defineProps<{
  modelValue: TimeSlot[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: TimeSlot[]]
}>()

// ---------- Computed ----------

const slots = computed(() => props.modelValue ?? [])

const hasOverlap = computed(() => {
  const sorted = [...slots.value]
    .filter(s => s.startTime && s.endTime)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
  for (let i = 1; i < sorted.length; i++) {
    if (sorted[i].startTime < sorted[i - 1].endTime) {
      return true
    }
  }
  return false
})

// ---------- Actions ----------

function addSlot() {
  // Default to next logical slot based on existing ones
  let defaultStart = '08:00'
  let defaultEnd = '09:00'
  if (slots.value.length > 0) {
    const last = slots.value[slots.value.length - 1]
    if (last.endTime) {
      defaultStart = last.endTime
      // Add 1 hour
      const [h, m] = last.endTime.split(':').map(Number)
      const endH = Math.min(h + 1, 23)
      defaultEnd = `${String(endH).padStart(2, '0')}:${String(m).padStart(2, '0')}`
    }
  }
  const updated = [
    ...slots.value,
    { startTime: defaultStart, endTime: defaultEnd, label: '' },
  ]
  emit('update:modelValue', updated)
}

function removeSlot(index: number) {
  if (slots.value.length <= 1) {
    ElMessage.warning('至少保留一个时间段')
    return
  }
  const updated = [...slots.value]
  updated.splice(index, 1)
  emit('update:modelValue', updated)
}

function updateSlot(index: number, field: keyof TimeSlot, value: string) {
  const updated = [...slots.value]
  updated[index] = { ...updated[index], [field]: value }

  // Validate: end must be after start
  if (field === 'startTime' && updated[index].endTime && value >= updated[index].endTime) {
    const [h, m] = value.split(':').map(Number)
    const endH = Math.min(h + 1, 23)
    updated[index].endTime = `${String(endH).padStart(2, '0')}:${String(m).padStart(2, '0')}`
  }

  emit('update:modelValue', updated)
}

// ---------- Presets ----------

function applyPreset(preset: 'morning-afternoon' | 'three-slots' | 'full-day') {
  let newSlots: TimeSlot[] = []
  switch (preset) {
    case 'morning-afternoon':
      newSlots = [
        { startTime: '08:00', endTime: '12:00', label: '上午' },
        { startTime: '14:00', endTime: '18:00', label: '下午' },
      ]
      break
    case 'three-slots':
      newSlots = [
        { startTime: '08:00', endTime: '11:30', label: '上午' },
        { startTime: '13:30', endTime: '17:00', label: '下午' },
        { startTime: '19:00', endTime: '21:00', label: '晚间' },
      ]
      break
    case 'full-day':
      newSlots = [
        { startTime: '00:00', endTime: '23:59', label: '全天' },
      ]
      break
  }
  emit('update:modelValue', newSlots)
}
</script>

<template>
  <div class="time-slot-editor">
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-2">
        <Clock class="w-4 h-4 text-gray-500" />
        <span class="text-sm font-medium text-gray-700">时间段配置</span>
        <el-tag size="small" type="info">{{ slots.length }} 个时段</el-tag>
      </div>
      <el-button type="primary" size="small" @click="addSlot">
        <Plus class="w-3.5 h-3.5 mr-1" />添加时段
      </el-button>
    </div>

    <!-- Presets -->
    <div class="flex items-center gap-2 mb-3">
      <span class="text-xs text-gray-400">快捷模板：</span>
      <el-button size="small" @click="applyPreset('morning-afternoon')">
        上午+下午
      </el-button>
      <el-button size="small" @click="applyPreset('three-slots')">
        三时段
      </el-button>
      <el-button size="small" @click="applyPreset('full-day')">
        全天
      </el-button>
    </div>

    <!-- Slot List -->
    <div class="space-y-2">
      <div
        v-for="(slot, index) in slots"
        :key="index"
        class="flex items-center gap-3 rounded-md border border-gray-200 bg-white px-3 py-2"
      >
        <span class="text-xs text-gray-400 w-6 shrink-0">{{ index + 1 }}</span>

        <el-time-picker
          :model-value="slot.startTime"
          format="HH:mm"
          value-format="HH:mm"
          placeholder="开始"
          size="small"
          class="!w-28"
          @update:model-value="(val: string) => updateSlot(index, 'startTime', val)"
        />

        <span class="text-gray-400">-</span>

        <el-time-picker
          :model-value="slot.endTime"
          format="HH:mm"
          value-format="HH:mm"
          placeholder="结束"
          size="small"
          class="!w-28"
          @update:model-value="(val: string) => updateSlot(index, 'endTime', val)"
        />

        <el-input
          :model-value="slot.label ?? ''"
          placeholder="标签（可选）"
          size="small"
          class="!w-32"
          @update:model-value="(val: string) => updateSlot(index, 'label', val)"
        />

        <el-button
          link
          type="danger"
          size="small"
          :disabled="slots.length <= 1"
          @click="removeSlot(index)"
        >
          <Trash2 class="w-3.5 h-3.5" />
        </el-button>
      </div>
    </div>

    <!-- Empty State -->
    <div
      v-if="slots.length === 0"
      class="py-6 text-center text-sm text-gray-400 border border-dashed border-gray-200 rounded-md mt-2"
    >
      暂无时间段，请添加
    </div>

    <!-- Overlap Warning -->
    <div v-if="hasOverlap" class="mt-2 rounded-md bg-red-50 px-3 py-2 text-xs text-red-600">
      存在时间段重叠，请检查并调整
    </div>
  </div>
</template>
