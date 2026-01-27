<template>
  <div class="rounded-xl border border-gray-200 bg-white">
    <!-- 头部 - 可折叠 -->
    <div
      class="flex cursor-pointer items-center justify-between px-4 py-3 transition-colors hover:bg-gray-50"
      @click="expanded = !expanded"
    >
      <div class="flex items-center gap-2">
        <Package class="h-5 w-5 text-gray-500" />
        <span class="font-medium text-gray-700">可选专业池</span>
        <span class="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
          {{ filteredMajors.length }} 个专业
        </span>
      </div>
      <div class="flex items-center gap-3">
        <!-- 搜索框 -->
        <div class="relative" @click.stop>
          <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索专业..."
            class="h-8 w-48 rounded-lg border border-gray-200 pl-9 pr-3 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
        </div>
        <!-- 展开/收起图标 -->
        <ChevronDown
          class="h-5 w-5 text-gray-400 transition-transform duration-200"
          :class="{ '-rotate-180': expanded }"
        />
      </div>
    </div>

    <!-- 内容区 -->
    <div
      class="overflow-hidden transition-all duration-300"
      :class="expanded ? 'max-h-80' : 'max-h-0'"
    >
      <div class="border-t border-gray-100 p-4">
        <!-- 空状态 -->
        <div v-if="filteredMajors.length === 0" class="flex flex-col items-center justify-center py-8 text-gray-400">
          <Package class="h-10 w-10" />
          <p class="mt-2 text-sm">
            {{ searchKeyword ? '未找到匹配的专业' : '暂无可用专业' }}
          </p>
        </div>

        <!-- 专业网格 -->
        <div v-else class="grid grid-cols-4 gap-3">
          <div
            v-for="major in filteredMajors"
            :key="major.id"
            class="group relative"
            draggable="true"
            @dragstart="handleDragStart($event, major)"
            @dragend="handleDragEnd"
          >
            <div
              class="cursor-grab rounded-lg border border-gray-200 bg-gray-50 p-3 transition-all duration-200 hover:border-emerald-400 hover:bg-emerald-50 hover:shadow-md active:cursor-grabbing"
              :class="{ 'ring-2 ring-emerald-400 ring-offset-2': isDragging && draggedMajor?.id === major.id }"
            >
              <div class="flex items-center gap-2">
                <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-white">
                  <GraduationCap class="h-4 w-4 text-emerald-600" />
                </div>
                <div class="flex-1 min-w-0">
                  <p class="truncate text-sm font-medium text-gray-700">{{ major.majorName }}</p>
                  <p class="text-xs text-gray-400">{{ major.directions?.length || 0 }} 个方向</p>
                </div>
              </div>

              <!-- 快捷添加按钮 -->
              <div class="mt-2 flex items-center justify-between">
                <div class="flex flex-wrap gap-1">
                  <span
                    v-for="(dir, idx) in (major.directions || []).slice(0, 2)"
                    :key="dir.id"
                    class="rounded bg-white px-1.5 py-0.5 text-xs text-gray-500"
                  >
                    {{ dir.level || '方向' + (idx + 1) }}
                  </span>
                  <span v-if="(major.directions?.length || 0) > 2" class="text-xs text-gray-400">
                    +{{ major.directions.length - 2 }}
                  </span>
                </div>
                <button
                  @click.stop="$emit('add', major)"
                  class="flex h-6 w-6 items-center justify-center rounded-full bg-emerald-500 text-white opacity-0 transition-all hover:bg-emerald-600 group-hover:opacity-100"
                  title="添加全部方向"
                >
                  <Plus class="h-3.5 w-3.5" />
                </button>
              </div>
            </div>

            <!-- 拖拽手柄提示 -->
            <div class="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-gray-200 text-gray-500 opacity-0 transition-opacity group-hover:opacity-100">
              <GripVertical class="h-3 w-3" />
            </div>
          </div>
        </div>

        <!-- 提示文字 -->
        <p class="mt-4 text-center text-xs text-gray-400">
          <span class="inline-flex items-center gap-1">
            <GripVertical class="h-3 w-3" />
            拖拽专业到上方区域添加，或点击
            <Plus class="h-3 w-3" />
            按钮快速添加
          </span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Package,
  Search,
  ChevronDown,
  GraduationCap,
  Plus,
  GripVertical
} from 'lucide-vue-next'

interface Major {
  id: number | string
  majorName: string
  directions?: any[]
}

interface Props {
  majors: Major[]
  configuredMajorIds?: (number | string)[]
}

const props = withDefaults(defineProps<Props>(), {
  configuredMajorIds: () => [],
})

const emit = defineEmits<{
  add: [major: Major]
  'drag-start': [major: Major]
  'drag-end': []
}>()

const expanded = ref(true)
const searchKeyword = ref('')
const isDragging = ref(false)
const draggedMajor = ref<Major | null>(null)

// 过滤后的专业列表（排除已配置的）
const filteredMajors = computed(() => {
  let result = props.majors.filter(
    m => !props.configuredMajorIds.includes(m.id) && !props.configuredMajorIds.includes(String(m.id))
  )

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(m => m.majorName.toLowerCase().includes(keyword))
  }

  return result
})

// 拖拽开始
const handleDragStart = (event: DragEvent, major: Major) => {
  isDragging.value = true
  draggedMajor.value = major

  // 设置拖拽数据
  event.dataTransfer!.effectAllowed = 'copy'
  event.dataTransfer!.setData('application/json', JSON.stringify(major))

  // 创建自定义拖拽图像
  const dragImage = document.createElement('div')
  dragImage.className = 'fixed bg-emerald-500 text-white px-3 py-2 rounded-lg shadow-lg text-sm font-medium'
  dragImage.textContent = major.majorName
  dragImage.style.position = 'absolute'
  dragImage.style.top = '-1000px'
  document.body.appendChild(dragImage)
  event.dataTransfer!.setDragImage(dragImage, 0, 0)
  setTimeout(() => document.body.removeChild(dragImage), 0)

  emit('drag-start', major)
}

// 拖拽结束
const handleDragEnd = () => {
  isDragging.value = false
  draggedMajor.value = null
  emit('drag-end')
}
</script>
