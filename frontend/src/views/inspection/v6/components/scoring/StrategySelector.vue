<template>
  <div class="strategy-selector">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>
    <template v-else>
      <!-- 策略分组 -->
      <div v-for="group in strategyGroups" :key="group.key" class="strategy-group">
        <div class="group-header" @click="toggleGroup(group.key)">
          <span class="group-title">{{ group.label }}</span>
          <span class="group-badge">{{ group.items.length }}个</span>
          <el-icon class="group-arrow" :class="{ expanded: expandedGroups.includes(group.key) }">
            <ArrowDown />
          </el-icon>
        </div>
        <el-collapse-transition>
          <div v-show="expandedGroups.includes(group.key)" class="group-content">
            <div class="strategy-grid">
              <div
                v-for="strategy in group.items"
                :key="strategy.code"
                class="strategy-card"
                :class="{ selected: modelValue === strategy.code, disabled: !strategy.isEnabled }"
                @click="selectStrategy(strategy)"
              >
                <div class="card-header">
                  <span class="card-name">{{ strategy.name }}</span>
                  <el-tag v-if="strategy.isSystem" size="small" type="info">内置</el-tag>
                </div>
                <div class="card-desc">{{ strategy.description }}</div>
                <div class="card-formula">{{ strategy.formulaDescription }}</div>
              </div>
            </div>
          </div>
        </el-collapse-transition>
      </div>

      <!-- 策略参数配置 -->
      <div v-if="selectedStrategy && hasParameters" class="params-section">
        <div class="params-title">策略参数配置</div>
        <div class="params-grid">
          <div v-for="(schema, key) in selectedStrategy.parametersSchema" :key="key" class="param-item">
            <label class="param-label">{{ schema.label || key }}</label>
            <el-input-number
              v-if="schema.type === 'number'"
              v-model="localParams[key]"
              :min="schema.min"
              :max="schema.max"
              :step="schema.step || 1"
              size="small"
              controls-position="right"
              @change="emitParamsChange"
            />
            <el-input
              v-else
              v-model="localParams[key]"
              size="small"
              @change="emitParamsChange"
            />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { getStrategiesGrouped } from '@/api/scoring'
import type { ScoringStrategy } from '@/types/scoring'

const props = defineProps<{
  modelValue: string
  parameters?: Record<string, any>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:parameters', value: Record<string, any>): void
  (e: 'change', strategy: ScoringStrategy): void
}>()

const loading = ref(false)
const strategiesMap = ref<Record<string, ScoringStrategy[]>>({})
const expandedGroups = ref<string[]>(['basic', 'grade'])
const localParams = ref<Record<string, any>>({})

const categoryLabels: Record<string, string> = {
  basic: '基础策略',
  grade: '等级策略',
  advanced: '高级策略',
  time: '时间策略'
}

const strategyGroups = computed(() => {
  return Object.entries(strategiesMap.value).map(([key, items]) => ({
    key,
    label: categoryLabels[key] || key,
    items: items.filter(s => s.isEnabled)
  })).filter(g => g.items.length > 0)
})

const selectedStrategy = computed(() => {
  for (const items of Object.values(strategiesMap.value)) {
    const found = items.find(s => s.code === props.modelValue)
    if (found) return found
  }
  return null
})

const hasParameters = computed(() => {
  return selectedStrategy.value?.parametersSchema &&
         Object.keys(selectedStrategy.value.parametersSchema).length > 0
})

const toggleGroup = (key: string) => {
  const idx = expandedGroups.value.indexOf(key)
  if (idx >= 0) {
    expandedGroups.value.splice(idx, 1)
  } else {
    expandedGroups.value.push(key)
  }
}

const selectStrategy = (strategy: ScoringStrategy) => {
  if (!strategy.isEnabled) return
  emit('update:modelValue', strategy.code)
  emit('change', strategy)

  // 初始化参数
  if (strategy.defaultParameters) {
    localParams.value = { ...strategy.defaultParameters }
    emitParamsChange()
  }
}

const emitParamsChange = () => {
  emit('update:parameters', { ...localParams.value })
}

const loadStrategies = async () => {
  loading.value = true
  try {
    const res = await getStrategiesGrouped()
    strategiesMap.value = res?.data?.data || res?.data || res || {}
  } catch (e) {
    console.error('Failed to load strategies:', e)
  } finally {
    loading.value = false
  }
}

watch(() => props.parameters, (val) => {
  if (val) {
    localParams.value = { ...val }
  }
}, { immediate: true })

onMounted(() => {
  loadStrategies()
})
</script>

<style scoped>
.strategy-selector {
  width: 100%;
}

.loading-state {
  padding: 20px;
}

.strategy-group {
  margin-bottom: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
}

.group-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  cursor: pointer;
  transition: background 0.2s;
}

.group-header:hover {
  background: #f0f0f0;
}

.group-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.group-badge {
  margin-left: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.group-arrow {
  margin-left: auto;
  transition: transform 0.3s;
  color: #8c8c8c;
}

.group-arrow.expanded {
  transform: rotate(180deg);
}

.group-content {
  padding: 16px;
  background: #fff;
}

.strategy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.strategy-card {
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.strategy-card:hover {
  border-color: #b8b8ff;
  background: #fafaff;
}

.strategy-card.selected {
  border-color: #5b5fc7;
  background: #f5f5ff;
}

.strategy-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.card-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.4;
}

.card-formula {
  font-size: 11px;
  color: #8c8c8c;
  padding: 6px 8px;
  background: #f5f5f5;
  border-radius: 4px;
  font-family: 'Monaco', 'Consolas', monospace;
}

.strategy-card.selected .card-formula {
  background: #e8e8ff;
  color: #5b5fc7;
}

/* 参数配置区 */
.params-section {
  margin-top: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
}

.params-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.params-grid {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.param-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.param-label {
  font-size: 12px;
  color: #666;
}
</style>
