<script setup lang="ts">
import { computed } from 'vue'
import { Plus, Trash2, AlertCircle } from 'lucide-vue-next'

// ==================== Types ====================

type ValidationRuleType = 'REQUIRED' | 'MIN_LENGTH' | 'MAX_LENGTH' | 'REGEX' | 'RANGE' | 'CUSTOM'

interface ValidationRule {
  type: ValidationRuleType
  params: Record<string, any>
  message: string
}

const RULE_TYPE_OPTIONS: { value: ValidationRuleType; label: string; description: string }[] = [
  { value: 'REQUIRED', label: '必填', description: '字段不能为空' },
  { value: 'MIN_LENGTH', label: '最小长度', description: '文本最少字符数' },
  { value: 'MAX_LENGTH', label: '最大长度', description: '文本最多字符数' },
  { value: 'REGEX', label: '正则匹配', description: '自定义正则表达式' },
  { value: 'RANGE', label: '数值范围', description: '数值在指定范围内' },
  { value: 'CUSTOM', label: '自定义', description: '自定义校验逻辑' },
]

// ==================== Props & Emits ====================

const props = defineProps<{
  modelValue: ValidationRule[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: ValidationRule[]]
}>()

// ==================== Computed ====================

const rules = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// ==================== Helpers ====================

const inputCls = 'w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400'
const selectCls = 'w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400 bg-white'

function addRule() {
  const updated = [...rules.value, { type: 'REQUIRED' as ValidationRuleType, params: {}, message: '' }]
  emit('update:modelValue', updated)
}

function removeRule(idx: number) {
  const updated = [...rules.value]
  updated.splice(idx, 1)
  emit('update:modelValue', updated)
}

function updateRule(idx: number, field: keyof ValidationRule, value: any) {
  const updated = rules.value.map((r, i) => {
    if (i !== idx) return r
    const clone = { ...r }
    if (field === 'type') {
      clone.type = value
      clone.params = getDefaultParams(value)
    } else if (field === 'params') {
      clone.params = { ...clone.params, ...value }
    } else {
      ;(clone as any)[field] = value
    }
    return clone
  })
  emit('update:modelValue', updated)
}

function getDefaultParams(type: ValidationRuleType): Record<string, any> {
  switch (type) {
    case 'MIN_LENGTH': return { min: 1 }
    case 'MAX_LENGTH': return { max: 200 }
    case 'REGEX': return { pattern: '', flags: '' }
    case 'RANGE': return { min: 0, max: 100 }
    case 'CUSTOM': return { expression: '' }
    default: return {}
  }
}

function needsParams(type: ValidationRuleType): boolean {
  return type !== 'REQUIRED'
}

function getRuleTypeLabel(type: ValidationRuleType): string {
  return RULE_TYPE_OPTIONS.find(o => o.value === type)?.label || type
}
</script>

<template>
  <div class="space-y-3">
    <div class="flex items-center justify-between">
      <div>
        <h3 class="text-sm font-medium text-gray-700">验证规则</h3>
        <p class="text-xs text-gray-400 mt-0.5">检查员提交数据时自动校验</p>
      </div>
      <button
        class="flex items-center gap-1 text-xs px-3 py-1.5 bg-blue-50 text-blue-600 rounded hover:bg-blue-100 transition"
        @click="addRule"
      >
        <Plus :size="12" /> 添加规则
      </button>
    </div>

    <div v-if="rules.length === 0" class="text-center py-8 text-gray-400 text-sm">
      暂无验证规则，点击上方按钮添加
    </div>

    <div v-else class="space-y-2">
      <div
        v-for="(rule, idx) in rules"
        :key="idx"
        class="rounded-lg border border-gray-200 bg-white p-3 space-y-2"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <AlertCircle :size="14" class="text-gray-400" />
            <span class="text-xs font-medium text-gray-500">
              规则 {{ idx + 1 }} - {{ getRuleTypeLabel(rule.type) }}
            </span>
          </div>
          <button
            class="rounded p-0.5 text-gray-400 hover:text-red-500"
            @click="removeRule(idx)"
          >
            <Trash2 :size="14" />
          </button>
        </div>

        <!-- Type selector -->
        <div>
          <label class="mb-0.5 block text-xs text-gray-500">类型</label>
          <select
            :value="rule.type"
            :class="selectCls"
            @change="updateRule(idx, 'type', ($event.target as HTMLSelectElement).value)"
          >
            <option v-for="opt in RULE_TYPE_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }} - {{ opt.description }}
            </option>
          </select>
        </div>

        <!-- Dynamic params based on type -->
        <div v-if="needsParams(rule.type)" class="bg-gray-50 rounded p-2 space-y-2">
          <!-- MIN_LENGTH -->
          <template v-if="rule.type === 'MIN_LENGTH'">
            <div>
              <label class="mb-0.5 block text-xs text-gray-500">最小字符数</label>
              <input
                type="number"
                :value="rule.params.min ?? 1"
                :class="inputCls"
                class="!w-32"
                :min="0"
                @input="updateRule(idx, 'params', { min: Number(($event.target as HTMLInputElement).value) })"
              />
            </div>
          </template>

          <!-- MAX_LENGTH -->
          <template v-else-if="rule.type === 'MAX_LENGTH'">
            <div>
              <label class="mb-0.5 block text-xs text-gray-500">最大字符数</label>
              <input
                type="number"
                :value="rule.params.max ?? 200"
                :class="inputCls"
                class="!w-32"
                :min="1"
                @input="updateRule(idx, 'params', { max: Number(($event.target as HTMLInputElement).value) })"
              />
            </div>
          </template>

          <!-- REGEX -->
          <template v-else-if="rule.type === 'REGEX'">
            <div>
              <label class="mb-0.5 block text-xs text-gray-500">正则表达式</label>
              <input
                :value="rule.params.pattern ?? ''"
                :class="inputCls"
                class="font-mono"
                placeholder="^[a-zA-Z0-9]+$"
                @input="updateRule(idx, 'params', { pattern: ($event.target as HTMLInputElement).value })"
              />
            </div>
            <div>
              <label class="mb-0.5 block text-xs text-gray-500">标志位</label>
              <input
                :value="rule.params.flags ?? ''"
                :class="inputCls"
                class="!w-24 font-mono"
                placeholder="gi"
                @input="updateRule(idx, 'params', { flags: ($event.target as HTMLInputElement).value })"
              />
            </div>
          </template>

          <!-- RANGE -->
          <template v-else-if="rule.type === 'RANGE'">
            <div class="grid grid-cols-2 gap-2">
              <div>
                <label class="mb-0.5 block text-xs text-gray-500">最小值</label>
                <input
                  type="number"
                  :value="rule.params.min ?? 0"
                  :class="inputCls"
                  @input="updateRule(idx, 'params', { min: Number(($event.target as HTMLInputElement).value) })"
                />
              </div>
              <div>
                <label class="mb-0.5 block text-xs text-gray-500">最大值</label>
                <input
                  type="number"
                  :value="rule.params.max ?? 100"
                  :class="inputCls"
                  @input="updateRule(idx, 'params', { max: Number(($event.target as HTMLInputElement).value) })"
                />
              </div>
            </div>
          </template>

          <!-- CUSTOM -->
          <template v-else-if="rule.type === 'CUSTOM'">
            <div>
              <label class="mb-0.5 block text-xs text-gray-500">校验表达式</label>
              <textarea
                :value="rule.params.expression ?? ''"
                :class="inputCls"
                class="font-mono"
                rows="2"
                placeholder="value.length > 0 && value !== 'N/A'"
                @input="updateRule(idx, 'params', { expression: ($event.target as HTMLTextAreaElement).value })"
              />
            </div>
          </template>
        </div>

        <!-- Error message -->
        <div>
          <label class="mb-0.5 block text-xs text-gray-500">错误提示</label>
          <input
            :value="rule.message"
            :class="inputCls"
            placeholder="校验失败时显示的消息"
            @input="updateRule(idx, 'message', ($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>
    </div>

    <!-- JSON Preview -->
    <div v-if="rules.length > 0" class="rounded border border-dashed border-gray-300 bg-gray-50 p-2">
      <p class="text-xs text-gray-400">预览 JSON:</p>
      <code class="block mt-1 text-xs text-gray-600 break-all">{{ JSON.stringify(rules) }}</code>
    </div>
  </div>
</template>
