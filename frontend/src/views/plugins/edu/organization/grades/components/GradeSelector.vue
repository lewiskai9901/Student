<template>
  <div class="w-72 flex flex-col border-r border-gray-200 bg-white">
    <!-- 头部 -->
    <div class="flex items-center justify-between border-b border-gray-200 px-4 py-4">
      <div class="flex items-center gap-2">
        <Layers class="h-5 w-5 text-emerald-600" />
        <span class="font-semibold text-gray-900">年级列表</span>
      </div>
      <button
        @click="$emit('add')"
        class="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 transition-colors hover:bg-emerald-100"
        title="新增年级"
      >
        <Plus class="h-4 w-4" />
      </button>
    </div>

    <!-- 年级列表 -->
    <div class="flex-1 overflow-y-auto p-3">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center justify-center py-8">
        <Loader2 class="h-6 w-6 animate-spin text-gray-400" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="grades.length === 0" class="flex flex-col items-center justify-center py-12 text-gray-400">
        <GraduationCap class="h-12 w-12" />
        <p class="mt-3 text-sm">暂无年级</p>
        <button
          @click="$emit('add')"
          class="mt-3 flex items-center gap-1 rounded-lg bg-emerald-500 px-3 py-1.5 text-sm text-white transition-colors hover:bg-emerald-600"
        >
          <Plus class="h-4 w-4" />
          新增年级
        </button>
      </div>

      <!-- 年级卡片列表 -->
      <div v-else class="space-y-2">
        <div
          v-for="(grade, index) in grades"
          :key="grade.id"
          class="group relative cursor-pointer"
          @click="$emit('select', String(grade.id))"
        >
          <!-- 时间轴连接线 -->
          <div
            v-if="index < grades.length - 1"
            class="absolute left-5 top-full h-2 w-0.5"
            :class="isSelected(grade.id) ? getTheme(index).bg : 'bg-gray-200'"
          ></div>

          <!-- 年级卡片 -->
          <div
            class="relative rounded-xl border-2 p-3 transition-all duration-200"
            :class="[
              isSelected(grade.id)
                ? `${getTheme(index).border} ${getTheme(index).bgLight} shadow-md`
                : 'border-transparent bg-gray-50 hover:bg-gray-100 hover:shadow-sm'
            ]"
          >
            <!-- 选中指示器 -->
            <div
              v-if="isSelected(grade.id)"
              class="absolute -left-0.5 top-1/2 h-8 w-1 -translate-y-1/2 rounded-r-full"
              :class="getTheme(index).bg"
            ></div>

            <!-- 卡片内容 -->
            <div class="flex items-start gap-3">
              <!-- 年级图标 -->
              <div
                class="flex h-10 w-10 items-center justify-center rounded-lg transition-colors"
                :class="isSelected(grade.id) ? getTheme(index).bgLight : 'bg-white'"
              >
                <GraduationCap class="h-5 w-5" :class="getTheme(index).text" />
              </div>

              <!-- 年级信息 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="font-semibold text-gray-900">{{ grade.gradeName }}</span>
                  <span
                    v-if="grade.status === 0"
                    class="rounded bg-gray-200 px-1.5 py-0.5 text-xs text-gray-500"
                  >
                    禁用
                  </span>
                </div>

                <!-- 统计信息 -->
                <div class="mt-1.5 flex items-center gap-3 text-xs text-gray-500">
                  <span class="flex items-center gap-1">
                    <BookOpen class="h-3 w-3" />
                    {{ getStats(grade.id).majorCount }}专业
                  </span>
                  <span class="flex items-center gap-1">
                    <Compass class="h-3 w-3" />
                    {{ getStats(grade.id).directionCount }}方向
                  </span>
                </div>

                <!-- 警告：未配置专业 -->
                <div
                  v-if="getStats(grade.id).majorCount === 0"
                  class="mt-1.5 flex items-center gap-1 text-xs text-amber-600"
                >
                  <AlertCircle class="h-3 w-3" />
                  未配置专业
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                <button
                  @click.stop="$emit('edit', grade)"
                  class="flex h-7 w-7 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-white hover:text-blue-500"
                  title="编辑"
                >
                  <Pencil class="h-3.5 w-3.5" />
                </button>
                <button
                  @click.stop="$emit('delete', grade)"
                  class="flex h-7 w-7 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-white hover:text-red-500"
                  title="删除"
                >
                  <Trash2 class="h-3.5 w-3.5" />
                </button>
              </div>
            </div>

            <!-- 入学年份标签 -->
            <div class="mt-2 flex items-center gap-2">
              <span
                class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs"
                :class="isSelected(grade.id) ? `${getTheme(index).bgLight} ${getTheme(index).text}` : 'bg-gray-100 text-gray-500'"
              >
                <Calendar class="h-3 w-3" />
                {{ grade.enrollmentYear }}级
              </span>
              <span class="text-xs text-gray-400">
                班额{{ grade.standardClassSize || 40 }}人
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部统计 -->
    <div class="border-t border-gray-200 bg-gray-50 px-4 py-3">
      <div class="flex items-center justify-between text-sm">
        <span class="text-gray-500">共 {{ grades.length }} 个年级</span>
        <span class="text-gray-400">
          {{ enabledCount }} 启用 / {{ grades.length - enabledCount }} 禁用
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Layers,
  Plus,
  Loader2,
  GraduationCap,
  BookOpen,
  Compass,
  AlertCircle,
  Pencil,
  Trash2,
  Calendar
} from 'lucide-vue-next'

interface Props {
  grades: any[]
  selectedId: string | null
  loading?: boolean
  gradeStats: Map<string, { majorCount: number; directionCount: number; classCount: number }>
}

const props = defineProps<Props>()

defineEmits<{
  select: [gradeId: string]
  add: []
  edit: [grade: any]
  delete: [grade: any]
}>()

// 主题色配置
const themeColors = [
  { bg: 'bg-blue-500', bgLight: 'bg-blue-50', text: 'text-blue-600', border: 'border-blue-400' },
  { bg: 'bg-violet-500', bgLight: 'bg-violet-50', text: 'text-violet-600', border: 'border-violet-400' },
  { bg: 'bg-cyan-500', bgLight: 'bg-cyan-50', text: 'text-cyan-600', border: 'border-cyan-400' },
  { bg: 'bg-orange-500', bgLight: 'bg-orange-50', text: 'text-orange-600', border: 'border-orange-400' },
  { bg: 'bg-gray-500', bgLight: 'bg-gray-50', text: 'text-gray-600', border: 'border-gray-400' },
]

const getTheme = (index: number) => {
  return themeColors[index % themeColors.length]
}

const isSelected = (gradeId: any) => {
  return String(gradeId) === props.selectedId
}

const getStats = (gradeId: any) => {
  return props.gradeStats.get(String(gradeId)) || { majorCount: 0, directionCount: 0, classCount: 0 }
}

const enabledCount = computed(() => {
  return props.grades.filter(g => g.status === 1).length
})
</script>
