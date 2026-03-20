<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ToggleLeft, Zap, Eye, ChevronRight } from 'lucide-vue-next'
import type { ConditionLogicV2, ConditionGroup, ConditionAction } from '@/types/insp/conditionLogic'
import { isV2, isV1, createEmptyV2 } from '@/types/insp/conditionLogic'
import { normalizeV1toV2, humanizeCondition } from '@/utils/insp/conditionLogicEngine'
import ConditionGroupNode from './ConditionGroupNode.vue'
import ActionList from './ActionList.vue'

interface ItemOption {
  itemCode: string
  itemName: string
  itemType?: string
  isScored?: boolean
  scoringMode?: import('@/types/insp/enums').ScoringMode
}

const props = defineProps<{
  modelValue: string
  allItems: ItemOption[]
  targetLabel?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const enabled = ref(false)
const conditionData = ref<ConditionLogicV2>(createEmptyV2())

function parseInput(json: string) {
  if (!json) {
    enabled.value = false
    conditionData.value = createEmptyV2()
    return
  }
  try {
    const parsed = JSON.parse(json)
    if (isV2(parsed)) {
      enabled.value = true
      conditionData.value = parsed
    } else if (isV1(parsed)) {
      enabled.value = true
      conditionData.value = normalizeV1toV2(parsed)
    } else {
      enabled.value = false
      conditionData.value = createEmptyV2()
    }
  } catch {
    enabled.value = false
    conditionData.value = createEmptyV2()
  }
}

function serialize(): string {
  if (!enabled.value) return ''
  return JSON.stringify(conditionData.value)
}

watch(() => props.modelValue, (val) => {
  parseInput(val)
}, { immediate: true })

function emitUpdate() {
  emit('update:modelValue', serialize())
}

function onToggleEnabled(val: boolean) {
  enabled.value = val
  if (val && !conditionData.value.conditions.rules.length) {
    conditionData.value = createEmptyV2()
  }
  emitUpdate()
}

function onConditionsUpdate(group: ConditionGroup) {
  conditionData.value = { ...conditionData.value, conditions: group }
  emitUpdate()
}

function onActionsUpdate(actions: ConditionAction[]) {
  conditionData.value = { ...conditionData.value, actions }
  emitUpdate()
}

const fieldLabelResolver = (code: string) => {
  const item = props.allItems.find(i => i.itemCode === code)
  return item ? item.itemName : code
}

const preview = computed(() => {
  if (!enabled.value) return ''
  return humanizeCondition(conditionData.value, fieldLabelResolver)
})

const ruleCount = computed(() => {
  function count(group: ConditionGroup): number {
    let n = 0
    for (const r of group.rules) {
      if ('logicOp' in r) n += count(r as ConditionGroup)
      else n++
    }
    return n
  }
  return count(conditionData.value.conditions)
})

const actionCount = computed(() => conditionData.value.actions.length)
</script>

<template>
  <div class="cb-root">
    <!-- 开关头部 -->
    <button
      class="cb-toggle"
      :class="{ active: enabled }"
      @click="onToggleEnabled(!enabled)"
    >
      <ToggleLeft :size="16" :class="enabled ? 'text-blue-500' : 'text-gray-400'" />
      <span class="cb-toggle-label">条件逻辑</span>
      <span v-if="enabled" class="cb-toggle-count">
        {{ ruleCount }} 条件 · {{ actionCount }} 动作
      </span>
      <span v-else class="cb-toggle-hint">关闭</span>
    </button>

    <template v-if="enabled">
      <!-- IF 条件区 -->
      <div class="cb-section">
        <div class="cb-section-header">
          <span class="cb-keyword if">IF</span>
          <span class="cb-section-label">当满足以下条件时</span>
        </div>
        <div class="cb-section-body">
          <ConditionGroupNode
            :group="conditionData.conditions"
            :items="allItems"
            :depth="1"
            @update="onConditionsUpdate"
            @remove="onConditionsUpdate({ logicOp: 'and', rules: [] })"
          />
        </div>
      </div>

      <!-- 连接箭头 -->
      <div class="cb-connector">
        <ChevronRight :size="14" class="text-gray-300" />
      </div>

      <!-- THEN 动作区 -->
      <div class="cb-section">
        <div class="cb-section-header">
          <span class="cb-keyword then">THEN</span>
          <span class="cb-section-label">执行以下动作</span>
        </div>
        <div class="cb-section-body">
          <ActionList
            :actions="conditionData.actions"
            @update="onActionsUpdate"
          />
        </div>
      </div>

      <!-- 自然语言预览 -->
      <div v-if="preview" class="cb-preview">
        <Eye :size="12" class="shrink-0 text-gray-400" />
        <span>{{ preview }}</span>
      </div>
    </template>

    <!-- 关闭状态说明 -->
    <div v-else class="cb-disabled-hint">
      <Zap :size="14" class="text-gray-300" />
      <span>启用后可根据其他检查项的值控制{{ targetLabel ? ` "${targetLabel}" 的` : '此字段的' }}显示、隐藏、必填等行为</span>
    </div>
  </div>
</template>

<style scoped>
.cb-root {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Toggle */
.cb-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fafbfc;
  cursor: pointer;
  transition: all 0.15s;
}
.cb-toggle:hover {
  border-color: #d1d5db;
  background: #f3f4f6;
}
.cb-toggle.active {
  border-color: #bfdbfe;
  background: #eff6ff;
}
.cb-toggle-label {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}
.cb-toggle-count {
  margin-left: auto;
  font-size: 10px;
  color: #6b7280;
  background: #fff;
  padding: 1px 8px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}
.cb-toggle-hint {
  margin-left: auto;
  font-size: 10px;
  color: #9ca3af;
}

/* Section */
.cb-section {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}
.cb-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f9fafb;
  border-bottom: 1px solid #f3f4f6;
}
.cb-section-label {
  font-size: 11px;
  color: #6b7280;
}
.cb-section-body {
  padding: 10px;
}

/* Keywords */
.cb-keyword {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}
.cb-keyword.if {
  background: #dbeafe;
  color: #1d4ed8;
}
.cb-keyword.then {
  background: #dcfce7;
  color: #15803d;
}

/* Connector */
.cb-connector {
  display: flex;
  justify-content: center;
  padding: 0 0 0 16px;
}

/* Preview */
.cb-preview {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fefce8;
  border: 1px solid #fef08a;
  font-size: 11px;
  line-height: 1.5;
  color: #713f12;
}

/* Disabled hint */
.cb-disabled-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
  line-height: 1.5;
}
</style>
