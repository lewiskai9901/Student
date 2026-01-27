<template>
  <div class="feature-list-editor">
    <div class="flex items-center justify-between mb-4">
      <h4 class="text-sm font-medium text-gray-700">特性列表</h4>
      <button
        type="button"
        @click="addFeature"
        :disabled="features.length >= 5"
        class="flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <Plus class="h-3.5 w-3.5" />
        添加
      </button>
    </div>

    <draggable
      v-model="features"
      item-key="id"
      handle=".drag-handle"
      animation="200"
      @change="emitChange"
      class="space-y-3"
    >
      <template #item="{ element, index }">
        <div class="feature-item">
          <div class="drag-handle">
            <GripVertical class="h-4 w-4 text-gray-400" />
          </div>

          <div class="feature-icon-select">
            <select
              :value="element.icon"
              @change="updateFeature(index, 'icon', ($event.target as HTMLSelectElement).value)"
              class="w-full h-9 px-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option v-for="opt in iconOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>

          <div class="feature-text-input">
            <input
              type="text"
              :value="element.text"
              @input="updateFeature(index, 'text', ($event.target as HTMLInputElement).value)"
              placeholder="特性描述"
              class="w-full h-9 px-3 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <button
            type="button"
            @click="removeFeature(index)"
            :disabled="features.length <= 1"
            class="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Trash2 class="h-4 w-4" />
          </button>
        </div>
      </template>
    </draggable>

    <p class="mt-3 text-xs text-gray-400">
      拖拽调整顺序，最多可添加 5 个特性
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { Plus, Trash2, GripVertical } from 'lucide-vue-next'
import type { LoginFeature, FeatureIcon } from '@/types/loginCustomization'
import { featureIconOptions } from '@/types/loginCustomization'

interface FeatureItem extends LoginFeature {
  id: string
}

interface Props {
  modelValue: LoginFeature[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: LoginFeature[]): void
}>()

// 图标选项
const iconOptions = featureIconOptions

// 内部状态，带id用于拖拽
const features = ref<FeatureItem[]>([])

// 生成唯一ID
const generateId = () => Math.random().toString(36).substr(2, 9)

// 初始化
const initFeatures = () => {
  features.value = props.modelValue.map((f) => ({
    ...f,
    id: generateId()
  }))
}

// 监听外部值变化
watch(
  () => props.modelValue,
  () => {
    initFeatures()
  },
  { immediate: true, deep: true }
)

// 发出变更
const emitChange = () => {
  emit(
    'update:modelValue',
    features.value.map(({ icon, text }) => ({ icon, text }))
  )
}

// 添加特性
const addFeature = () => {
  if (features.value.length >= 5) return
  features.value.push({
    id: generateId(),
    icon: 'star' as FeatureIcon,
    text: ''
  })
  emitChange()
}

// 移除特性
const removeFeature = (index: number) => {
  if (features.value.length <= 1) return
  features.value.splice(index, 1)
  emitChange()
}

// 更新特性
const updateFeature = (index: number, field: 'icon' | 'text', value: string) => {
  if (field === 'icon') {
    features.value[index].icon = value as FeatureIcon
  } else {
    features.value[index].text = value
  }
  emitChange()
}
</script>

<style scoped>
.feature-list-editor {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.drag-handle {
  cursor: grab;
  padding: 4px;
}

.drag-handle:active {
  cursor: grabbing;
}

.feature-icon-select {
  width: 100px;
  flex-shrink: 0;
}

.feature-text-input {
  flex: 1;
}
</style>
