<script setup lang="ts">
import { computed } from 'vue'
import { X, Hash, Calendar, Clock, CalendarClock, Type, CheckCircle } from 'lucide-vue-next'
import type { ConditionRule, Operator } from '@/types/insp/conditionLogic'
import { OPERATORS, operatorNeedsValue } from '@/types/insp/conditionLogic'
import type { ScoringMode } from '@/types/insp/enums'
import { ScoringModeConfig, ItemTypeConfig } from '@/types/insp/enums'

interface ItemOption {
  itemCode: string
  itemName: string
  itemType?: string
  isScored?: boolean
  scoringMode?: ScoringMode
}

const props = defineProps<{
  rule: ConditionRule
  items: ItemOption[]
  index: number
}>()

const emit = defineEmits<{
  update: [rule: ConditionRule]
  remove: []
}>()

const needsValue = computed(() => operatorNeedsValue(props.rule.operator as Operator))

const selectedItem = computed(() =>
  props.items.find(i => i.itemCode === props.rule.field),
)

// 字段类型描述
const fieldTypeLabel = computed(() => {
  const item = selectedItem.value
  if (!item) return ''
  if (item.isScored) {
    const mode = item.scoringMode
    return mode ? (ScoringModeConfig[mode]?.label || '评分') : '评分'
  }
  return ItemTypeConfig[item.itemType as keyof typeof ItemTypeConfig]?.label || item.itemType || ''
})

const fieldTypeBadgeClass = computed(() => {
  const item = selectedItem.value
  if (!item) return ''
  return item.isScored ? 'cr-badge-scored' : 'cr-badge-capture'
})

// 值输入类型
// 'no_value' = 仅支持 isEmpty/isNotEmpty，不需要值输入（GPS、照片等）
// 'boolean'  = 勾选/未勾选（CHECKBOX）
type ValueInputKind = 'text' | 'number' | 'date' | 'time' | 'datetime' | 'pass_fail' | 'select' | 'no_value' | 'boolean'
const valueInputKind = computed<ValueInputKind>(() => {
  const item = selectedItem.value
  if (!item) return 'text'
  if (item.isScored) {
    switch (item.scoringMode) {
      case 'PASS_FAIL': return 'pass_fail'
      case 'DEDUCTION':
      case 'ADDITION':
      case 'DIRECT':
      case 'CUMULATIVE':
      case 'RATING_SCALE':
      case 'THRESHOLD':
      case 'FORMULA':
        return 'number'
      case 'LEVEL':
      case 'SCORE_TABLE':
      case 'TIERED_DEDUCTION':
        return 'select'
      case 'WEIGHTED_MULTI':
      case 'RISK_MATRIX':
        return 'number'
      default: return 'text'
    }
  }
  switch (item.itemType) {
    case 'NUMBER':
    case 'SLIDER':
      return 'number'
    case 'DATE':
      return 'date'
    case 'TIME':
      return 'time'
    case 'DATETIME':
      return 'datetime'
    case 'SELECT':
    case 'RADIO':
    case 'MULTI_SELECT':
      return 'select'
    case 'CHECKBOX':
      return 'boolean'
    case 'GPS':
    case 'PHOTO':
    case 'VIDEO':
    case 'SIGNATURE':
    case 'FILE_UPLOAD':
      return 'no_value'
    default:
      return 'text'
  }
})

// 值输入图标
const valueIcon = computed(() => {
  switch (valueInputKind.value) {
    case 'number': return Hash
    case 'date': return Calendar
    case 'time': return Clock
    case 'datetime': return CalendarClock
    case 'pass_fail': return CheckCircle
    default: return Type
  }
})

const passfailOptions = [
  { value: 'PASS', label: '通过' },
  { value: 'FAIL', label: '不通过' },
]

// 按类型过滤操作符
const filteredOperators = computed(() => {
  const kind = valueInputKind.value
  // GPS/照片/签名/文件 等只能判断有无
  if (kind === 'no_value') {
    return OPERATORS.filter(op => ['isEmpty', 'isNotEmpty'].includes(op.value))
  }
  // 布尔/通过不通过 只有等于、不等于、有无
  if (kind === 'pass_fail' || kind === 'boolean') {
    return OPERATORS.filter(op => ['equals', 'notEquals', 'isEmpty', 'isNotEmpty'].includes(op.value))
  }
  // 数字/日期/时间 不支持 contains 和 in
  if (['date', 'time', 'datetime', 'number'].includes(kind)) {
    return OPERATORS.filter(op => op.value !== 'contains' && op.value !== 'in')
  }
  return OPERATORS
})

// 字段下拉分组
const groupedItems = computed(() => {
  const scored = props.items.filter(i => i.isScored)
  const capture = props.items.filter(i => !i.isScored)
  return { scored, capture }
})

// 判断字段是否是 no_value 类型
function isNoValueType(field: string): boolean {
  const item = props.items.find(i => i.itemCode === field)
  if (!item || item.isScored) return false
  return ['GPS', 'PHOTO', 'VIDEO', 'SIGNATURE', 'FILE_UPLOAD'].includes(item.itemType || '')
}

function updateField(field: string) {
  // 切换字段时重置操作符和值
  // no_value 类型默认用 isEmpty
  const defaultOp = isNoValueType(field) ? 'isEmpty' : 'equals'
  emit('update', { ...props.rule, field, operator: defaultOp, value: '' })
}

function updateOperator(operator: string) {
  const updated: ConditionRule = { ...props.rule, operator: operator as Operator }
  if (!operatorNeedsValue(operator as Operator)) {
    delete updated.value
  } else if (!updated.value) {
    updated.value = ''
  }
  emit('update', updated)
}

function updateValue(value: string) {
  emit('update', { ...props.rule, value })
}
</script>

<template>
  <div class="cr-root">
    <!-- 行号 -->
    <span class="cr-index">{{ index + 1 }}</span>

    <!-- 主体 -->
    <div class="cr-body">
      <!-- 第一行：字段选择 + 类型标签 -->
      <div class="cr-row">
        <span class="cr-label">当</span>
        <select
          :value="rule.field"
          class="cr-select cr-select-field"
          @change="updateField(($event.target as HTMLSelectElement).value)"
        >
          <option value="">选择检查项...</option>
          <optgroup v-if="groupedItems.scored.length" label="━ 评分项">
            <option v-for="it in groupedItems.scored" :key="it.itemCode" :value="it.itemCode">
              {{ it.itemName }}
            </option>
          </optgroup>
          <optgroup v-if="groupedItems.capture.length" label="━ 采集项">
            <option v-for="it in groupedItems.capture" :key="it.itemCode" :value="it.itemCode">
              {{ it.itemName }}
            </option>
          </optgroup>
        </select>
        <span v-if="fieldTypeLabel" :class="['cr-type-badge', fieldTypeBadgeClass]">
          {{ fieldTypeLabel }}
        </span>
      </div>

      <!-- 第二行：操作符 + 值 -->
      <div class="cr-row">
        <span class="cr-label-spacer" />
        <select
          :value="rule.operator"
          class="cr-select cr-select-op"
          @change="updateOperator(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="op in filteredOperators" :key="op.value" :value="op.value">
            {{ op.label }}
          </option>
        </select>

        <!-- 值区域 -->
        <template v-if="needsValue">
          <!-- 布尔类型（CHECKBOX） -->
          <select
            v-if="valueInputKind === 'boolean'"
            :value="rule.value || ''"
            class="cr-select cr-select-value"
            @change="updateValue(($event.target as HTMLSelectElement).value)"
          >
            <option value="">选择...</option>
            <option value="true">已勾选</option>
            <option value="false">未勾选</option>
          </select>

          <!-- PASS/FAIL 下拉 -->
          <select
            v-else-if="valueInputKind === 'pass_fail'"
            :value="rule.value || ''"
            class="cr-select cr-select-value"
            @change="updateValue(($event.target as HTMLSelectElement).value)"
          >
            <option value="">选择...</option>
            <option v-for="opt in passfailOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>

          <!-- 日期 -->
          <el-date-picker
            v-else-if="valueInputKind === 'date'"
            :model-value="rule.value || ''"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            size="small"
            class="cr-picker"
            @update:model-value="(v: string) => updateValue(v || '')"
          />

          <!-- 时间 -->
          <el-time-picker
            v-else-if="valueInputKind === 'time'"
            :model-value="rule.value || ''"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择时间"
            size="small"
            class="cr-picker"
            @update:model-value="(v: string) => updateValue(v || '')"
          />

          <!-- 日期时间 -->
          <el-date-picker
            v-else-if="valueInputKind === 'datetime'"
            :model-value="rule.value || ''"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm"
            placeholder="日期时间"
            size="small"
            class="cr-picker cr-picker-wide"
            @update:model-value="(v: string) => updateValue(v || '')"
          />

          <!-- 数字 -->
          <div v-else-if="valueInputKind === 'number'" class="cr-input-wrap">
            <component :is="valueIcon" :size="12" class="cr-input-icon" />
            <input
              :value="rule.value || ''"
              type="number"
              class="cr-input"
              :placeholder="rule.operator === 'between' ? '如 3,7' : '数值'"
              @input="updateValue(($event.target as HTMLInputElement).value)"
            />
          </div>

          <!-- 文本 -->
          <div v-else class="cr-input-wrap">
            <component :is="valueIcon" :size="12" class="cr-input-icon" />
            <input
              :value="rule.value || ''"
              class="cr-input"
              :placeholder="rule.operator === 'between' ? '如 A,B' : rule.operator === 'in' ? '如 A,B,C' : '输入值'"
              @input="updateValue(($event.target as HTMLInputElement).value)"
            />
          </div>
        </template>
        <span v-else class="cr-no-value">
          {{ valueInputKind === 'no_value' ? '此类型仅支持判断有无' : '无需输入值' }}
        </span>
      </div>
    </div>

    <!-- 删除按钮 -->
    <button class="cr-remove" title="删除此条件" @click="emit('remove')">
      <X :size="14" />
    </button>
  </div>
</template>

<style scoped>
.cr-root {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.cr-root:hover {
  border-color: #d1d5db;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

/* 行号 */
.cr-index {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 10px;
  font-weight: 700;
  margin-top: 4px;
}

/* 主体 */
.cr-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.cr-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

/* 标签 */
.cr-label {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 500;
  color: #9ca3af;
  width: 20px;
  text-align: right;
}
.cr-label-spacer {
  flex-shrink: 0;
  width: 20px;
}

/* 选择框通用 */
.cr-select {
  height: 28px;
  padding: 0 8px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  color: #374151;
  outline: none;
  transition: border-color 0.12s;
  cursor: pointer;
}
.cr-select:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 2px rgba(59,130,246,0.08);
}
.cr-select-field {
  flex: 1;
  min-width: 120px;
  max-width: 200px;
  font-weight: 500;
}
.cr-select-op {
  width: 96px;
  flex-shrink: 0;
}
.cr-select-value {
  width: 100px;
  flex-shrink: 0;
}

/* 类型标签 */
.cr-type-badge {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.02em;
}
.cr-badge-scored {
  background: #dbeafe;
  color: #1d4ed8;
}
.cr-badge-capture {
  background: #dcfce7;
  color: #15803d;
}

/* 输入框 */
.cr-input-wrap {
  position: relative;
  flex: 1;
  min-width: 80px;
  max-width: 140px;
}
.cr-input-icon {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}
.cr-input {
  width: 100%;
  height: 28px;
  padding: 0 8px 0 26px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 12px;
  color: #374151;
  outline: none;
  transition: border-color 0.12s;
}
.cr-input:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 2px rgba(59,130,246,0.08);
}

/* Element Plus pickers */
.cr-picker {
  flex-shrink: 0;
}
:deep(.cr-picker) {
  width: 140px !important;
}
:deep(.cr-picker-wide) {
  width: 180px !important;
}

/* 无值提示 */
.cr-no-value {
  font-size: 10px;
  color: #d1d5db;
  font-style: italic;
}

/* 删除按钮 */
.cr-remove {
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
  margin-top: 3px;
}
.cr-remove:hover {
  color: #ef4444;
  background: #fef2f2;
}
</style>
