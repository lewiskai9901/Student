<script setup lang="ts">
/**
 * FishboneDiagram - Fishbone (Ishikawa) diagram with 6M categories
 *
 * Provides a list-based layout for the 6M root cause analysis categories.
 * Each category has an editable list of causes with add/remove controls.
 */
import { computed } from 'vue'
import { Plus, X } from 'lucide-vue-next'

const props = defineProps<{
  modelValue: Record<string, string[]>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, string[]>]
}>()

interface CategoryInfo {
  key: string
  label: string
  description: string
  color: string
}

const categories: CategoryInfo[] = [
  { key: 'man', label: 'Man (人员)', description: '操作人员相关因素', color: '#409EFF' },
  { key: 'machine', label: 'Machine (设备)', description: '设备、工具相关因素', color: '#67C23A' },
  { key: 'material', label: 'Material (材料)', description: '材料、物料相关因素', color: '#E6A23C' },
  { key: 'method', label: 'Method (方法)', description: '流程、操作方法相关因素', color: '#F56C6C' },
  { key: 'measurement', label: 'Measurement (度量)', description: '检测、度量标准相关因素', color: '#909399' },
  { key: 'motherNature', label: 'Mother Nature (环境)', description: '环境、外部条件相关因素', color: '#B37FEB' },
]

// Ensure all 6M keys exist
const data = computed(() => {
  const current = props.modelValue || {}
  const result: Record<string, string[]> = {}
  for (const cat of categories) {
    result[cat.key] = current[cat.key] ? [...current[cat.key]] : []
  }
  return result
})

function emitUpdate(updated: Record<string, string[]>) {
  emit('update:modelValue', { ...updated })
}

function addCause(categoryKey: string) {
  const updated = { ...data.value }
  updated[categoryKey] = [...(updated[categoryKey] || []), '']
  emitUpdate(updated)
}

function updateCause(categoryKey: string, index: number, value: string) {
  const updated = { ...data.value }
  const list = [...(updated[categoryKey] || [])]
  list[index] = value
  updated[categoryKey] = list
  emitUpdate(updated)
}

function removeCause(categoryKey: string, index: number) {
  const updated = { ...data.value }
  const list = [...(updated[categoryKey] || [])]
  list.splice(index, 1)
  updated[categoryKey] = list
  emitUpdate(updated)
}

function totalCauses(): number {
  return Object.values(data.value).reduce((sum, arr) => sum + arr.filter(Boolean).length, 0)
}
</script>

<template>
  <div class="fishbone-diagram">
    <!-- Summary -->
    <div class="fishbone-summary text-sm text-gray-500 mb-3">
      已录入 {{ totalCauses() }} 条原因分析
    </div>

    <!-- Categories grid -->
    <div class="fishbone-grid">
      <div
        v-for="cat in categories"
        :key="cat.key"
        class="fishbone-category"
      >
        <!-- Category header -->
        <div class="category-header" :style="{ borderLeftColor: cat.color }">
          <div class="category-title" :style="{ color: cat.color }">
            {{ cat.label }}
          </div>
          <div class="category-desc text-gray-400 text-xs">{{ cat.description }}</div>
        </div>

        <!-- Cause list -->
        <div class="cause-list">
          <div
            v-for="(cause, index) in data[cat.key]"
            :key="index"
            class="cause-item"
          >
            <el-input
              :model-value="cause"
              size="small"
              placeholder="输入原因..."
              @update:model-value="(val: string) => updateCause(cat.key, index, val)"
            />
            <el-button
              size="small"
              type="danger"
              text
              @click="removeCause(cat.key, index)"
            >
              <X class="w-3.5 h-3.5" />
            </el-button>
          </div>

          <!-- Add button -->
          <el-button
            size="small"
            text
            type="primary"
            class="add-cause-btn"
            @click="addCause(cat.key)"
          >
            <Plus class="w-3.5 h-3.5 mr-1" />
            添加原因
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fishbone-diagram {
  padding: 8px 0;
}

.fishbone-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.fishbone-category {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.category-header {
  border-left: 3px solid;
  padding-left: 8px;
  margin-bottom: 10px;
}

.category-title {
  font-size: 13px;
  font-weight: 600;
}

.cause-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cause-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cause-item .el-input {
  flex: 1;
}

.add-cause-btn {
  align-self: flex-start;
  margin-top: 2px;
}

@media (max-width: 768px) {
  .fishbone-grid {
    grid-template-columns: 1fr;
  }
}
</style>
