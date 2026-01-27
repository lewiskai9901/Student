<template>
  <div
    class="group relative overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm transition-all duration-200 hover:-translate-y-1 hover:shadow-lg"
  >
    <!-- 卡片头部 - 渐变背景 -->
    <div
      class="relative px-4 py-3"
      :class="headerGradient"
    >
      <!-- 装饰图案 -->
      <div class="absolute right-0 top-0 h-full w-24 opacity-10">
        <svg viewBox="0 0 100 100" class="h-full w-full">
          <circle cx="80" cy="20" r="40" fill="currentColor" class="text-white" />
          <circle cx="90" cy="60" r="25" fill="currentColor" class="text-white" />
        </svg>
      </div>

      <div class="relative flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-white/20 backdrop-blur-sm">
            <GraduationCap class="h-5 w-5 text-white" />
          </div>
          <div>
            <h3 class="font-semibold text-white">{{ majorName }}</h3>
            <p class="text-sm text-white/80">{{ directions.length }} 个培养方向</p>
          </div>
        </div>

        <!-- 操作菜单 -->
        <div class="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
          <button
            @click="$emit('remove')"
            class="flex h-8 w-8 items-center justify-center rounded-lg bg-white/20 text-white transition-colors hover:bg-white/30"
            title="移除专业"
          >
            <Trash2 class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="p-4">
      <!-- 方向标签列表 -->
      <div class="mb-4 flex flex-wrap gap-2">
        <div
          v-for="direction in directions"
          :key="direction.id"
          class="group/tag relative flex items-center gap-1.5 rounded-lg border border-gray-200 bg-gray-50 px-2.5 py-1.5 text-sm transition-colors hover:border-gray-300 hover:bg-gray-100"
        >
          <Compass class="h-3.5 w-3.5 text-purple-500" />
          <span class="text-gray-700">{{ direction.directionName || getDirectionDisplay(direction) }}</span>

          <!-- 方向信息徽章 -->
          <span class="ml-1 rounded bg-blue-50 px-1 py-0.5 text-xs text-blue-600">
            {{ getLevelDisplay(direction) }}
          </span>
          <span class="rounded bg-amber-50 px-1 py-0.5 text-xs text-amber-600">
            {{ getYearsDisplay(direction) }}
          </span>

          <!-- 移除按钮 -->
          <button
            @click="$emit('remove-direction', direction)"
            class="ml-1 flex h-4 w-4 items-center justify-center rounded text-gray-400 opacity-0 transition-all hover:bg-red-100 hover:text-red-500 group-hover/tag:opacity-100"
            title="移除方向"
          >
            <X class="h-3 w-3" />
          </button>
        </div>

        <!-- 添加方向按钮 -->
        <button
          v-if="canAddMore"
          @click="$emit('add-direction')"
          class="flex items-center gap-1 rounded-lg border-2 border-dashed border-gray-300 px-2.5 py-1.5 text-sm text-gray-400 transition-colors hover:border-emerald-400 hover:bg-emerald-50 hover:text-emerald-600"
        >
          <Plus class="h-3.5 w-3.5" />
          添加方向
        </button>
      </div>

      <!-- 统计信息 -->
      <div class="flex items-center justify-between border-t border-gray-100 pt-3">
        <div class="flex items-center gap-4 text-sm">
          <div class="flex items-center gap-1.5 text-gray-500">
            <Users class="h-4 w-4" />
            <span>{{ totalStudents }} 学生</span>
          </div>
          <div class="flex items-center gap-1.5 text-gray-500">
            <LayoutGrid class="h-4 w-4" />
            <span>{{ totalClasses }} 班级</span>
          </div>
        </div>

        <!-- 状态标签 -->
        <span
          v-if="isConfigured"
          class="flex items-center gap-1 rounded-full bg-emerald-100 px-2 py-0.5 text-xs text-emerald-600"
        >
          <CheckCircle class="h-3 w-3" />
          已配置
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  GraduationCap,
  Trash2,
  Compass,
  X,
  Plus,
  Users,
  LayoutGrid,
  CheckCircle
} from 'lucide-vue-next'

interface Direction {
  id: number | string
  directionName?: string
  level?: string
  years?: number
  isSegmented?: number
  phase1Level?: string
  phase2Level?: string
  phase1Years?: number
  phase2Years?: number
  actualStudentCount?: number
  actualClassCount?: number
}

interface Props {
  majorName: string
  majorId: number | string
  directions: Direction[]
  colorIndex?: number
  canAddMore?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  colorIndex: 0,
  canAddMore: true,
})

defineEmits<{
  remove: []
  'remove-direction': [direction: Direction]
  'add-direction': []
}>()

// 渐变背景色
const gradients = [
  'bg-gradient-to-r from-blue-500 to-blue-600',
  'bg-gradient-to-r from-violet-500 to-purple-600',
  'bg-gradient-to-r from-cyan-500 to-teal-600',
  'bg-gradient-to-r from-orange-500 to-amber-600',
  'bg-gradient-to-r from-emerald-500 to-green-600',
  'bg-gradient-to-r from-pink-500 to-rose-600',
]

const headerGradient = computed(() => {
  return gradients[props.colorIndex % gradients.length]
})

// 获取方向显示文本
const getDirectionDisplay = (direction: Direction) => {
  if (direction.isSegmented === 1) {
    return `${direction.phase1Level}→${direction.phase2Level}`
  }
  return direction.level || '未命名'
}

// 获取层次显示
const getLevelDisplay = (direction: Direction) => {
  if (direction.isSegmented === 1) {
    return `${direction.phase1Level}→${direction.phase2Level}`
  }
  return direction.level || '-'
}

// 获取学制显示
const getYearsDisplay = (direction: Direction) => {
  if (direction.isSegmented === 1) {
    return `${direction.phase1Years}+${direction.phase2Years}年`
  }
  return `${direction.years || 0}年`
}

// 统计数据
const totalStudents = computed(() => {
  return props.directions.reduce((sum, d) => sum + (d.actualStudentCount || 0), 0)
})

const totalClasses = computed(() => {
  return props.directions.reduce((sum, d) => sum + (d.actualClassCount || 0), 0)
})

const isConfigured = computed(() => {
  return props.directions.length > 0
})
</script>
