<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Code, ChevronRight, AlertCircle, Check, ChevronDown } from 'lucide-vue-next'

// ==================== Types ====================

interface Variable {
  name: string
  description: string
  category?: string
}

interface FormulaTemplate {
  label: string
  description: string
  formula: string
}

// ==================== Props & Emits ====================

const props = defineProps<{
  modelValue: string
  availableVariables: Variable[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// ==================== State ====================

const textareaRef = ref<HTMLTextAreaElement | null>(null)
const advancedMode = ref(false)
const activeCategory = ref<string | null>(null)

// ==================== Formula ====================

const formula = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// ==================== Templates ====================

const TEMPLATES: FormulaTemplate[] = [
  { label: '标准扣分制', description: '基础分 减去 扣分 加上 加分', formula: 'baseScore - deductions + bonuses' },
  { label: '保底分数', description: '扣完保底不低于60分', formula: 'Math.max(baseScore - deductions + bonuses, 60)' },
  { label: '封顶加分', description: '加分后不超过满分的120%', formula: 'Math.min(baseScore - deductions + bonuses, maxScore * 1.2)' },
  { label: '人均分', description: '总分除以人数', formula: '(baseScore - deductions + bonuses) / population' },
  { label: '通过率计分', description: '按通过率折算满分', formula: 'maxScore * passRate' },
  { label: '阶梯扣分', description: '扣分超过50直接归零', formula: 'deductions > 50 ? 0 : baseScore - deductions + bonuses' },
  { label: '加权混合', description: '通过率占70% + 净分占30%', formula: 'maxScore * passRate * 0.7 + netScore * 0.3' },
]

const showTemplates = ref(false)

function applyTemplate(t: FormulaTemplate) {
  formula.value = t.formula
  showTemplates.value = false
}

// ==================== Variable categories ====================

const categories = computed(() => {
  const cats = new Map<string, Variable[]>()
  for (const v of props.availableVariables) {
    const cat = v.category || '其他'
    if (!cats.has(cat)) cats.set(cat, [])
    cats.get(cat)!.push(v)
  }
  return cats
})

// ==================== Operators ====================

const OPERATORS = [
  { label: '+', value: ' + ', title: '加' },
  { label: '−', value: ' - ', title: '减' },
  { label: '×', value: ' * ', title: '乘' },
  { label: '÷', value: ' / ', title: '除' },
  { label: '(', value: '(', title: '左括号' },
  { label: ')', value: ')', title: '右括号' },
]

const FUNCTIONS = [
  { label: '取大', value: 'Math.max(, )', title: '取两者中较大值', cursorOffset: -3 },
  { label: '取小', value: 'Math.min(, )', title: '取两者中较小值', cursorOffset: -3 },
  { label: '四舍五入', value: 'Math.round()', title: '四舍五入', cursorOffset: -1 },
  { label: '绝对值', value: 'Math.abs()', title: '取绝对值', cursorOffset: -1 },
  { label: '若…则…否则', value: ' ? : ', title: '条件表达式: 条件 ? 值A : 值B', cursorOffset: -4 },
]

// ==================== Insert helpers ====================

function insertAt(text: string, cursorOffset?: number) {
  const el = textareaRef.value
  if (!el) {
    formula.value = formula.value + text
    return
  }
  const start = el.selectionStart
  const end = el.selectionEnd
  const before = formula.value.substring(0, start)
  const after = formula.value.substring(end)
  formula.value = before + text + after

  requestAnimationFrame(() => {
    el.focus()
    const newPos = start + text.length + (cursorOffset || 0)
    el.setSelectionRange(newPos, newPos)
  })
}

function insertVariable(varName: string) {
  insertAt(varName)
}

function insertOperator(op: typeof OPERATORS[0]) {
  insertAt(op.value)
}

function insertFunction(fn: typeof FUNCTIONS[0]) {
  insertAt(fn.value, fn.cursorOffset)
}

// ==================== Syntax Validation ====================

const syntaxError = ref<string | null>(null)

watch(formula, (val) => {
  if (!val || val.trim() === '') {
    syntaxError.value = null
    return
  }
  try {
    new Function(...props.availableVariables.map(v => v.name), `return (${val})`)
    syntaxError.value = null
  } catch (e: any) {
    syntaxError.value = e.message || '语法错误'
  }
}, { immediate: true })

const isValid = computed(() => syntaxError.value === null && formula.value.trim() !== '')

// ==================== Chinese preview ====================

const formulaPreview = computed(() => {
  if (!formula.value.trim()) return ''
  let text = formula.value
  // Replace variable names with Chinese
  for (const v of props.availableVariables) {
    text = text.replace(new RegExp(`\\b${v.name}\\b`, 'g'), `【${v.description}】`)
  }
  // Replace functions
  text = text.replace(/Math\.max/g, '取大')
  text = text.replace(/Math\.min/g, '取小')
  text = text.replace(/Math\.round/g, '四舍五入')
  text = text.replace(/Math\.abs/g, '绝对值')
  text = text.replace(/Math\.floor/g, '向下取整')
  text = text.replace(/Math\.ceil/g, '向上取整')
  text = text.replace(/Math\.sqrt/g, '平方根')
  // Replace operators
  text = text.replace(/ \* /g, ' × ')
  text = text.replace(/ \/ /g, ' ÷ ')
  text = text.replace(/ \? /g, ' → ')
  text = text.replace(/ : /g, ' 否则 ')
  return text
})
</script>

<template>
  <div class="fe-root">
    <!-- Header -->
    <div class="fe-header">
      <div class="fe-header-left">
        <Code :size="14" class="fe-header-icon" />
        <span class="fe-title">公式规则</span>
      </div>
      <label class="fe-toggle">
        <input type="checkbox" v-model="advancedMode" />
        <span>代码模式</span>
      </label>
    </div>

    <!-- Template picker -->
    <div class="fe-templates">
      <button class="fe-tmpl-trigger" @click="showTemplates = !showTemplates">
        选择预设模板
        <ChevronDown :size="12" :class="{ 'fe-rot': showTemplates }" />
      </button>
      <div v-if="showTemplates" class="fe-tmpl-list">
        <button
          v-for="t in TEMPLATES"
          :key="t.label"
          class="fe-tmpl-item"
          @click="applyTemplate(t)"
        >
          <span class="fe-tmpl-label">{{ t.label }}</span>
          <span class="fe-tmpl-desc">{{ t.description }}</span>
        </button>
      </div>
    </div>

    <!-- Variable chips -->
    <div class="fe-vars">
      <div class="fe-var-cats">
        <template v-for="[cat, vars] of categories" :key="cat">
          <div class="fe-cat-group">
            <span class="fe-cat-label">{{ cat }}</span>
            <div class="fe-chip-row">
              <button
                v-for="v in vars"
                :key="v.name"
                class="fe-chip"
                :title="v.description"
                @click="insertVariable(v.name)"
              >
                {{ v.description }}
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Operator toolbar -->
    <div class="fe-toolbar">
      <button
        v-for="op in OPERATORS"
        :key="op.label"
        class="fe-op-btn"
        :title="op.title"
        @click="insertOperator(op)"
      >{{ op.label }}</button>
      <span class="fe-toolbar-sep" />
      <button
        v-for="fn in FUNCTIONS"
        :key="fn.label"
        class="fe-fn-btn"
        :title="fn.title"
        @click="insertFunction(fn)"
      >{{ fn.label }}</button>
    </div>

    <!-- Formula input -->
    <div class="fe-input-area" :class="{ 'has-error': syntaxError }">
      <textarea
        ref="textareaRef"
        v-model="formula"
        class="fe-textarea"
        :class="{ 'fe-mono': advancedMode }"
        :rows="3"
        placeholder="点击上方变量和运算符构建公式，或选择预设模板"
        spellcheck="false"
      />
    </div>

    <!-- Validation -->
    <div class="fe-status">
      <template v-if="!formula.trim()">
        <span class="fe-status-hint">请构建公式</span>
      </template>
      <template v-else-if="isValid">
        <Check :size="12" class="fe-icon-ok" />
        <span class="fe-status-ok">公式有效</span>
      </template>
      <template v-else>
        <AlertCircle :size="12" class="fe-icon-err" />
        <span class="fe-status-err">{{ syntaxError }}</span>
      </template>
    </div>

    <!-- Chinese preview -->
    <div v-if="formulaPreview" class="fe-preview">
      <span class="fe-preview-label">公式含义：</span>
      <span class="fe-preview-text">{{ formulaPreview }}</span>
    </div>
  </div>
</template>

<style scoped>
.fe-root { display:flex; flex-direction:column; gap:10px; }

/* Header */
.fe-header { display:flex; align-items:center; justify-content:space-between; }
.fe-header-left { display:flex; align-items:center; gap:6px; }
.fe-header-icon { color:#8c95a3; }
.fe-title { font-size:13px; font-weight:600; color:#1e2a3a; }

.fe-toggle { display:flex; align-items:center; gap:5px; font-size:11px; color:#8c95a3; cursor:pointer; user-select:none; }
.fe-toggle input { accent-color:#1a6dff; }

/* Templates */
.fe-templates { position:relative; }
.fe-tmpl-trigger {
  display:flex; align-items:center; gap:5px;
  padding:6px 12px; font-size:12px; color:#1a6dff; background:#f0f4ff;
  border:1px solid #dce5ff; border-radius:8px; cursor:pointer; transition:background 0.15s;
}
.fe-tmpl-trigger:hover { background:#e0ebff; }
.fe-rot { transform:rotate(180deg); transition:transform 0.15s; }

.fe-tmpl-list {
  position:absolute; top:calc(100% + 4px); left:0; z-index:20;
  min-width:320px; max-height:260px; overflow-y:auto;
  background:#fff; border:1px solid #dce1e8; border-radius:10px;
  box-shadow:0 8px 24px rgba(0,0,0,0.12); padding:4px;
}
.fe-tmpl-item {
  display:flex; flex-direction:column; gap:2px;
  width:100%; text-align:left; padding:8px 12px; border:none; background:none;
  border-radius:6px; cursor:pointer; transition:background 0.1s;
}
.fe-tmpl-item:hover { background:#f0f4ff; }
.fe-tmpl-label { font-size:13px; font-weight:500; color:#1e2a3a; }
.fe-tmpl-desc { font-size:11px; color:#8c95a3; }

/* Variable chips */
.fe-vars { border:1px solid #e8ecf0; border-radius:10px; padding:10px; background:#f8f9fb; }
.fe-var-cats { display:flex; flex-direction:column; gap:8px; }
.fe-cat-group { display:flex; flex-direction:column; gap:4px; }
.fe-cat-label { font-size:10px; font-weight:600; color:#8c95a3; text-transform:uppercase; letter-spacing:0.05em; }
.fe-chip-row { display:flex; flex-wrap:wrap; gap:4px; }
.fe-chip {
  padding:4px 10px; font-size:12px; color:#1a6dff; background:#e8f0fe;
  border:1px solid #d0dfff; border-radius:99px; cursor:pointer; transition:all 0.12s;
  white-space:nowrap;
}
.fe-chip:hover { background:#1a6dff; color:#fff; border-color:#1a6dff; }

/* Operator toolbar */
.fe-toolbar { display:flex; align-items:center; gap:4px; flex-wrap:wrap; }
.fe-op-btn {
  width:32px; height:28px; font-size:14px; font-weight:500;
  border:1px solid #dce1e8; border-radius:6px; background:#fff;
  color:#1e2a3a; cursor:pointer; transition:all 0.12s;
  display:flex; align-items:center; justify-content:center;
}
.fe-op-btn:hover { background:#f0f4ff; border-color:#7aadff; color:#1a6dff; }
.fe-toolbar-sep { width:1px; height:20px; background:#e8ecf0; margin:0 4px; }
.fe-fn-btn {
  padding:4px 10px; font-size:11px; color:#059669; background:#ecfdf5;
  border:1px solid #c8ecc8; border-radius:6px; cursor:pointer; transition:all 0.12s;
}
.fe-fn-btn:hover { background:#059669; color:#fff; border-color:#059669; }

/* Formula input */
.fe-input-area {
  border:1px solid #dce1e8; border-radius:10px; overflow:hidden;
  transition:border-color 0.2s;
}
.fe-input-area:focus-within { border-color:#7aadff; box-shadow:0 0 0 3px rgba(26,109,255,0.08); }
.fe-input-area.has-error { border-color:#f87171; }
.fe-input-area.has-error:focus-within { box-shadow:0 0 0 3px rgba(248,113,113,0.08); }

.fe-textarea {
  width:100%; border:none; outline:none; resize:none;
  padding:10px 14px; font-size:13px; color:#1e2a3a; background:#fff;
  line-height:1.6;
}
.fe-textarea.fe-mono { font-family:monospace; }
.fe-textarea::placeholder { color:#b8c0cc; }

/* Status */
.fe-status { display:flex; align-items:center; gap:5px; }
.fe-status-hint { font-size:11px; color:#b8c0cc; }
.fe-icon-ok { color:#34d399; }
.fe-status-ok { font-size:11px; color:#059669; }
.fe-icon-err { color:#ef4444; }
.fe-status-err { font-size:11px; color:#ef4444; }

/* Preview */
.fe-preview {
  background:#f8faff; border:1px solid #e0eaff; border-left:3px solid #1a6dff;
  border-radius:0 8px 8px 0; padding:8px 12px;
}
.fe-preview-label { font-size:11px; font-weight:600; color:#1a6dff; }
.fe-preview-text { font-size:12px; color:#5a6474; line-height:1.6; }
</style>
