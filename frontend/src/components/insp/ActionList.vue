<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, X, Eye, EyeOff, Asterisk, Lock, Hash, Eraser, ChevronDown } from 'lucide-vue-next'
import type { ConditionAction, ActionType } from '@/types/insp/conditionLogic'
import { ACTION_TYPES } from '@/types/insp/conditionLogic'

const props = defineProps<{
  actions: ConditionAction[]
}>()

const emit = defineEmits<{
  update: [actions: ConditionAction[]]
}>()

// 每种动作的图标和颜色
const actionStyle: Record<string, { icon: typeof Eye; color: string; bg: string; desc: string }> = {
  show: { icon: Eye, color: '#15803d', bg: '#dcfce7', desc: '条件满足时显示此字段' },
  hide: { icon: EyeOff, color: '#9333ea', bg: '#f3e8ff', desc: '条件满足时隐藏此字段' },
  require: { icon: Asterisk, color: '#dc2626', bg: '#fef2f2', desc: '条件满足时设为必填' },
  disable: { icon: Lock, color: '#6b7280', bg: '#f3f4f6', desc: '条件满足时禁用此字段' },
  setScore: { icon: Hash, color: '#1d4ed8', bg: '#dbeafe', desc: '条件满足时自动设置分值' },
  clearValue: { icon: Eraser, color: '#d97706', bg: '#fffbeb', desc: '条件满足时清空已输入的值' },
}

const usedTypes = computed(() => new Set(props.actions.map(a => a.type)))
const canAdd = computed(() => props.actions.length < ACTION_TYPES.length)
const showPicker = ref(false)

const availableActions = computed(() =>
  ACTION_TYPES.filter(a => !usedTypes.value.has(a.value)),
)

function getActionMeta(type: ActionType) {
  return ACTION_TYPES.find(a => a.value === type)
}

function updateActionType(index: number, type: string) {
  const list = [...props.actions]
  const meta = ACTION_TYPES.find(a => a.value === type)
  list[index] = meta?.hasExtra ? { type: type as ActionType, value: 0 } : { type: type as ActionType }
  emit('update', list)
}

function updateActionValue(index: number, value: string) {
  const list = [...props.actions]
  list[index] = { ...list[index], value: Number(value) }
  emit('update', list)
}

function removeAction(index: number) {
  emit('update', props.actions.filter((_, i) => i !== index))
}

function pickAction(type: ActionType) {
  const meta = ACTION_TYPES.find(a => a.value === type)
  const newAction: ConditionAction = meta?.hasExtra
    ? { type, value: 0 }
    : { type }
  emit('update', [...props.actions, newAction])
  showPicker.value = false
}
</script>

<template>
  <div class="al-root">
    <!-- 动作列表 -->
    <div v-for="(action, index) in actions" :key="index" class="al-card">
      <!-- 动作图标 -->
      <span
        class="al-icon"
        :style="{
          background: actionStyle[action.type]?.bg || '#f3f4f6',
          color: actionStyle[action.type]?.color || '#6b7280',
        }"
      >
        <component :is="actionStyle[action.type]?.icon || Eye" :size="13" />
      </span>

      <!-- 动作选择 -->
      <select
        :value="action.type"
        class="al-select"
        @change="updateActionType(index, ($event.target as HTMLSelectElement).value)"
      >
        <option
          v-for="a in ACTION_TYPES"
          :key="a.value"
          :value="a.value"
          :disabled="a.value !== action.type && usedTypes.has(a.value)"
        >
          {{ a.label }}
        </option>
      </select>

      <!-- 额外参数（如分值） -->
      <template v-if="getActionMeta(action.type)?.hasExtra">
        <span class="al-eq">=</span>
        <div class="al-input-wrap">
          <Hash :size="11" class="al-input-icon" />
          <input
            :value="action.value ?? 0"
            type="number"
            class="al-input"
            placeholder="分值"
            @input="updateActionValue(index, ($event.target as HTMLInputElement).value)"
          />
        </div>
      </template>

      <!-- 删除 -->
      <button class="al-remove" title="删除动作" @click="removeAction(index)">
        <X :size="13" />
      </button>
    </div>

    <!-- 空状态 -->
    <div v-if="actions.length === 0" class="al-empty">
      点击下方按钮选择要执行的动作
    </div>

    <!-- 添加按钮 + 选择面板 -->
    <div v-if="canAdd" class="al-add-area">
      <button class="al-add" @click="showPicker = !showPicker">
        <Plus :size="12" /> 添加动作
        <ChevronDown :size="11" :class="['al-chevron', showPicker && 'al-chevron-open']" />
      </button>

      <!-- 动作选择面板 -->
      <div v-if="showPicker" class="al-picker">
        <button
          v-for="a in availableActions"
          :key="a.value"
          class="al-picker-item"
          @click="pickAction(a.value)"
        >
          <span
            class="al-icon"
            :style="{
              background: actionStyle[a.value]?.bg || '#f3f4f6',
              color: actionStyle[a.value]?.color || '#6b7280',
            }"
          >
            <component :is="actionStyle[a.value]?.icon || Eye" :size="13" />
          </span>
          <span class="al-picker-info">
            <span class="al-picker-label">{{ a.label }}</span>
            <span class="al-picker-desc">{{ actionStyle[a.value]?.desc || '' }}</span>
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.al-root {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 动作卡片 */
.al-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.al-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 图标 */
.al-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
}

/* 选择框 */
.al-select {
  flex: 1;
  min-width: 100px;
  max-width: 160px;
  height: 28px;
  padding: 0 8px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  outline: none;
  transition: border-color 0.12s;
  cursor: pointer;
}
.al-select:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.08);
}
.al-select option:disabled {
  color: #d1d5db;
}

/* 等号 */
.al-eq {
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
}

/* 输入框 */
.al-input-wrap {
  position: relative;
  width: 72px;
  flex-shrink: 0;
}
.al-input-icon {
  position: absolute;
  left: 7px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}
.al-input {
  width: 100%;
  height: 28px;
  padding: 0 8px 0 22px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  color: #374151;
  outline: none;
  transition: border-color 0.12s;
}
.al-input:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.08);
}

/* 删除 */
.al-remove {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #d1d5db;
  cursor: pointer;
  transition: all 0.12s;
  margin-left: auto;
}
.al-remove:hover {
  color: #ef4444;
  background: #fef2f2;
}

/* 空状态 */
.al-empty {
  padding: 12px 8px;
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
}

/* 添加区域 */
.al-add-area {
  position: relative;
}
.al-add {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 500;
  color: #15803d;
  background: transparent;
  border: 1px dashed #86efac;
  cursor: pointer;
  transition: all 0.12s;
}
.al-add:hover {
  background: #f0fdf4;
  border-style: solid;
}
.al-chevron {
  transition: transform 0.15s;
  color: #86efac;
}
.al-chevron-open {
  transform: rotate(180deg);
}

/* 动作选择面板 */
.al-picker {
  margin-top: 4px;
  padding: 4px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.al-picker-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background 0.1s;
  text-align: left;
}
.al-picker-item:hover {
  background: #f3f4f6;
}
.al-picker-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.al-picker-label {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
}
.al-picker-desc {
  font-size: 10px;
  color: #9ca3af;
}
</style>
