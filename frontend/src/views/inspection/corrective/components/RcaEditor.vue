<script setup lang="ts">
/**
 * RcaEditor - Root Cause Analysis editor with mode toggle
 *
 * Supports two RCA methods: 5-WHY and FISHBONE (Ishikawa).
 * Toggle between methods via tabs. The selected method and its data
 * are stored as { method, data } in the modelValue.
 */
import { computed } from 'vue'
import FiveWhysChain from './FiveWhysChain.vue'
import FishboneDiagram from './FishboneDiagram.vue'

interface RcaData {
  method: string
  data: any
}

const props = withDefaults(
  defineProps<{
    modelValue: RcaData
    readonly?: boolean
  }>(),
  {
    readonly: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: RcaData]
}>()

const currentMethod = computed(() => props.modelValue?.method || '5WHY')

// Typed accessors for each sub-editor's data
const fiveWhysData = computed<string[]>(() => {
  if (currentMethod.value === '5WHY' && Array.isArray(props.modelValue?.data)) {
    return props.modelValue.data
  }
  return ['', '', '', '', '']
})

const fishboneData = computed<Record<string, string[]>>(() => {
  if (currentMethod.value === 'FISHBONE' && props.modelValue?.data) {
    return props.modelValue.data as Record<string, string[]>
  }
  return {}
})

function handleMethodChange(method: string) {
  // Switching method resets data to appropriate default
  let data: any
  if (method === '5WHY') {
    data = fiveWhysData.value
  } else {
    data = fishboneData.value
  }
  emit('update:modelValue', { method, data })
}

function handleFiveWhysUpdate(value: string[]) {
  emit('update:modelValue', { method: '5WHY', data: value })
}

function handleFishboneUpdate(value: Record<string, string[]>) {
  emit('update:modelValue', { method: 'FISHBONE', data: value })
}

// Readonly summary helpers
function hasWhysContent(): boolean {
  return fiveWhysData.value.some((w) => w.trim().length > 0)
}

function hasFishboneContent(): boolean {
  return Object.values(fishboneData.value).some((arr) => arr.some((s) => s.trim().length > 0))
}
</script>

<template>
  <div class="rca-editor">
    <!-- Method tabs -->
    <el-tabs
      :model-value="currentMethod"
      type="border-card"
      @tab-change="(name: string) => handleMethodChange(name)"
    >
      <el-tab-pane label="5-Why 分析法" name="5WHY">
        <template v-if="readonly">
          <!-- Readonly view for 5-WHY -->
          <div v-if="hasWhysContent()" class="readonly-content">
            <div
              v-for="(why, index) in fiveWhysData"
              :key="index"
              class="readonly-why-item"
            >
              <span class="font-medium text-sm text-gray-500">Why {{ index + 1 }}:</span>
              <span class="ml-2">{{ why || '-' }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无5-Why分析数据" :image-size="48" />
        </template>
        <template v-else>
          <div class="method-description text-gray-500 text-xs mb-3">
            通过连续追问5次"为什么"，层层深入找到问题的根本原因。
          </div>
          <FiveWhysChain :model-value="fiveWhysData" @update:model-value="handleFiveWhysUpdate" />
        </template>
      </el-tab-pane>

      <el-tab-pane label="鱼骨图分析法" name="FISHBONE">
        <template v-if="readonly">
          <!-- Readonly view for Fishbone -->
          <div v-if="hasFishboneContent()" class="readonly-content">
            <div
              v-for="(causes, category) in fishboneData"
              :key="category"
              class="readonly-fishbone-item"
            >
              <template v-if="causes.some((c: string) => c.trim())">
                <div class="font-medium text-sm text-gray-600 mb-1">{{ category }}</div>
                <ul class="list-disc list-inside text-sm text-gray-700 mb-2">
                  <li v-for="(cause, i) in causes.filter((c: string) => c.trim())" :key="i">
                    {{ cause }}
                  </li>
                </ul>
              </template>
            </div>
          </div>
          <el-empty v-else description="暂无鱼骨图分析数据" :image-size="48" />
        </template>
        <template v-else>
          <div class="method-description text-gray-500 text-xs mb-3">
            从人员、设备、材料、方法、度量、环境6个维度分析问题原因。
          </div>
          <FishboneDiagram :model-value="fishboneData" @update:model-value="handleFishboneUpdate" />
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.rca-editor {
  padding: 4px 0;
}

.method-description {
  line-height: 1.5;
}

.readonly-content {
  padding: 8px 0;
}

.readonly-why-item {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.readonly-why-item:last-child {
  border-bottom: none;
}

.readonly-fishbone-item {
  margin-bottom: 4px;
}
</style>
