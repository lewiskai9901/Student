<script setup lang="ts">
import { Plus, Layers, Trash2 } from 'lucide-vue-next'
import type { ConditionGroup, ConditionRule, LogicOp } from '@/types/insp/conditionLogic'
import { isConditionGroup, createEmptyRule, createEmptyGroup, MAX_NESTING_DEPTH } from '@/types/insp/conditionLogic'
import ConditionRuleRow from './ConditionRuleRow.vue'

interface ItemOption {
  itemCode: string
  itemName: string
  itemType?: string
  isScored?: boolean
  scoringMode?: import('@/types/insp/enums').ScoringMode
}

const props = defineProps<{
  group: ConditionGroup
  items: ItemOption[]
  depth?: number
}>()

const emit = defineEmits<{
  update: [group: ConditionGroup]
  remove: []
}>()

const currentDepth = props.depth ?? 1

function toggleLogicOp() {
  emit('update', { ...props.group, logicOp: props.group.logicOp === 'and' ? 'or' : 'and' })
}

function updateRule(index: number, rule: ConditionRule | ConditionGroup) {
  const rules = [...props.group.rules]
  rules[index] = rule
  emit('update', { ...props.group, rules })
}

function removeRule(index: number) {
  const rules = props.group.rules.filter((_, i) => i !== index)
  if (rules.length === 0) {
    emit('remove')
  } else {
    emit('update', { ...props.group, rules })
  }
}

function addRule() {
  emit('update', { ...props.group, rules: [...props.group.rules, createEmptyRule()] })
}

function addGroup() {
  const childOp: LogicOp = props.group.logicOp === 'and' ? 'or' : 'and'
  emit('update', { ...props.group, rules: [...props.group.rules, createEmptyGroup(childOp)] })
}

const canNest = currentDepth < MAX_NESTING_DEPTH
const isRoot = currentDepth === 1

// 层级样式
const accentStyles = [
  { border: '#93c5fd', bg: '#eff6ff', badge: '#dbeafe', badgeText: '#1d4ed8' },
  { border: '#fcd34d', bg: '#fffbeb', badge: '#fef3c7', badgeText: '#92400e' },
  { border: '#86efac', bg: '#f0fdf4', badge: '#dcfce7', badgeText: '#166534' },
]
const accent = accentStyles[(currentDepth - 1) % 3]
</script>

<template>
  <div
    class="cg-root"
    :style="{
      borderLeftColor: accent.border,
      background: isRoot ? 'transparent' : accent.bg,
    }"
    :class="{ 'cg-nested': !isRoot }"
  >
    <!-- 规则列表 -->
    <div class="cg-rules">
      <template v-for="(rule, index) in group.rules" :key="index">
        <!-- AND/OR 连接器（在两条规则之间） -->
        <div v-if="index > 0" class="cg-connector">
          <button
            class="cg-logic-btn"
            :style="{ background: accent.badge, color: accent.badgeText }"
            @click="toggleLogicOp"
          >
            {{ group.logicOp === 'and' ? '且 AND' : '或 OR' }}
          </button>
          <div class="cg-connector-line" :style="{ borderColor: accent.border }" />
        </div>

        <!-- 子条件组 -->
        <ConditionGroupNode
          v-if="isConditionGroup(rule)"
          :group="rule"
          :items="items"
          :depth="currentDepth + 1"
          @update="updateRule(index, $event)"
          @remove="removeRule(index)"
        />
        <!-- 单条规则 -->
        <ConditionRuleRow
          v-else
          :rule="rule"
          :items="items"
          :index="index"
          @update="updateRule(index, $event)"
          @remove="removeRule(index)"
        />
      </template>
    </div>

    <!-- 空状态 -->
    <div v-if="group.rules.length === 0" class="cg-empty">
      点击下方按钮添加条件
    </div>

    <!-- 底部操作栏 -->
    <div class="cg-footer">
      <button class="cg-add-btn" @click="addRule">
        <Plus :size="12" /> 条件
      </button>
      <button v-if="canNest" class="cg-add-btn nested" @click="addGroup">
        <Layers :size="12" /> 条件组
      </button>
      <button v-if="!isRoot" class="cg-remove-group" @click="emit('remove')">
        <Trash2 :size="12" /> 删除组
      </button>
    </div>
  </div>
</template>

<style scoped>
.cg-root {
  border-left: 3px solid #93c5fd;
  border-radius: 6px;
}
.cg-nested {
  padding: 8px;
  margin: 4px 0;
}

.cg-rules {
  display: flex;
  flex-direction: column;
}

/* AND/OR 连接器 */
.cg-connector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
}
.cg-logic-btn {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  border: none;
  cursor: pointer;
  transition: all 0.12s;
  opacity: 0.85;
}
.cg-logic-btn:hover {
  opacity: 1;
  transform: scale(1.05);
}
.cg-connector-line {
  flex: 1;
  height: 0;
  border-top: 1px dashed #d1d5db;
}

/* 空状态 */
.cg-empty {
  padding: 12px 8px;
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
}

/* 底部操作 */
.cg-footer {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 0 0;
}
.cg-add-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 500;
  color: #3b82f6;
  background: transparent;
  border: 1px dashed #93c5fd;
  cursor: pointer;
  transition: all 0.12s;
}
.cg-add-btn:hover {
  background: #eff6ff;
  border-style: solid;
}
.cg-add-btn.nested {
  color: #6b7280;
  border-color: #d1d5db;
}
.cg-add-btn.nested:hover {
  background: #f3f4f6;
  color: #374151;
}
.cg-remove-group {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 10px;
  color: #9ca3af;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.12s;
}
.cg-remove-group:hover {
  color: #ef4444;
  background: #fef2f2;
}
</style>
