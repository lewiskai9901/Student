<template>
  <div class="formula-editor">
    <div class="editor-container">
      <!-- 编辑区 -->
      <div class="editor-area">
        <div class="editor-header">
          <span class="editor-title">公式编辑器</span>
          <div class="editor-actions">
            <el-button size="small" @click="validateFormula">
              <el-icon><Check /></el-icon>
              验证
            </el-button>
            <el-button size="small" @click="showPreview = !showPreview">
              <el-icon><View /></el-icon>
              预览
            </el-button>
          </div>
        </div>
        <div class="code-editor-wrapper">
          <textarea
            ref="codeInput"
            v-model="localFormula"
            class="code-editor"
            :placeholder="placeholder"
            spellcheck="false"
            @input="handleInput"
            @keydown="handleKeydown"
          ></textarea>
          <div v-if="validationResult" class="validation-status" :class="validationResult.valid ? 'success' : 'error'">
            <el-icon v-if="validationResult.valid"><CircleCheck /></el-icon>
            <el-icon v-else><CircleClose /></el-icon>
            <span>{{ validationResult.valid ? '公式有效' : validationResult.errorMessage }}</span>
          </div>
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="editor-sidebar">
        <!-- 内置函数 -->
        <div class="sidebar-section">
          <div class="sidebar-title">内置函数</div>
          <div class="sidebar-list">
            <div
              v-for="fn in groupedFunctions"
              :key="fn.name"
              class="sidebar-item"
              @click="insertFunction(fn)"
              :title="fn.description"
            >
              <code>{{ fn.name }}()</code>
              <span class="item-desc">{{ fn.description }}</span>
            </div>
          </div>
        </div>

        <!-- 可用变量 -->
        <div class="sidebar-section">
          <div class="sidebar-title">可用变量</div>
          <div class="sidebar-list">
            <div
              v-for="v in groupedVariables"
              :key="v.name"
              class="sidebar-item"
              @click="insertVariable(v)"
              :title="v.description"
            >
              <code>ctx.{{ v.name }}</code>
              <span class="item-type">({{ v.valueType }})</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 预览区 -->
    <el-collapse-transition>
      <div v-show="showPreview" class="preview-section">
        <div class="preview-title">公式预览</div>
        <div class="preview-inputs">
          <div v-for="v in previewVariables" :key="v.name" class="preview-input">
            <label>{{ v.name }}</label>
            <el-input-number
              v-if="v.valueType === 'number'"
              v-model="previewData[v.name]"
              size="small"
              :controls="false"
              style="width: 100%"
              @change="runPreview"
            />
            <el-input
              v-else
              v-model="previewData[v.name]"
              size="small"
              @change="runPreview"
            />
          </div>
        </div>
        <div class="preview-result">
          <div class="result-label">计算结果</div>
          <div class="result-value" :class="{ error: previewError }">
            {{ previewError || previewResult }}
          </div>
        </div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { Check, View, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { validateFormula as apiValidateFormula } from '@/api/scoring'
import type { FormulaFunction, FormulaVariable, FormulaValidationResult } from '@/types/scoring'

const props = defineProps<{
  modelValue: string
  functions?: FormulaFunction[]
  variables?: FormulaVariable[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change'): void
}>()

const codeInput = ref<HTMLTextAreaElement | null>(null)
const localFormula = ref('')
const showPreview = ref(false)
const validationResult = ref<FormulaValidationResult | null>(null)
const previewData = ref<Record<string, any>>({})
const previewResult = ref<string>('')
const previewError = ref<string>('')

const placeholder = `// 输入计分公式
// 示例:
const baseScore = ctx.baseScore || 100;
const deductions = sum(ctx.deductions);
let score = baseScore - deductions;
score = clamp(score, 0, ctx.maxScore);
return Math.round(score * 10) / 10;`

// 分组显示的函数和变量
const groupedFunctions = computed(() => {
  const fns = props.functions || []
  // 只显示常用的
  return fns.slice(0, 10)
})

const groupedVariables = computed(() => {
  const vars = props.variables || []
  // 只显示常用的
  return vars.slice(0, 15)
})

// 预览用的变量
const previewVariables = computed(() => {
  // 从公式中提取使用的变量
  const usedVars: FormulaVariable[] = []
  const formula = localFormula.value || ''
  for (const v of props.variables || []) {
    if (formula.includes(`ctx.${v.name}`)) {
      usedVars.push(v)
    }
  }
  // 默认添加常用变量
  if (usedVars.length === 0) {
    return (props.variables || []).filter(v =>
      ['baseScore', 'deductions', 'additions', 'maxScore', 'minScore'].includes(v.name)
    )
  }
  return usedVars
})

const handleInput = () => {
  emit('update:modelValue', localFormula.value)
  emit('change')
  validationResult.value = null
}

const handleKeydown = (e: KeyboardEvent) => {
  // Tab键插入两个空格
  if (e.key === 'Tab') {
    e.preventDefault()
    const textarea = codeInput.value
    if (textarea) {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      localFormula.value = localFormula.value.substring(0, start) + '  ' + localFormula.value.substring(end)
      nextTick(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 2
      })
    }
  }
}

const insertFunction = (fn: FormulaFunction) => {
  const textarea = codeInput.value
  if (textarea) {
    const start = textarea.selectionStart
    const text = `${fn.name}()`
    localFormula.value = localFormula.value.substring(0, start) + text + localFormula.value.substring(start)
    handleInput()
    nextTick(() => {
      textarea.focus()
      textarea.selectionStart = textarea.selectionEnd = start + fn.name.length + 1
    })
  }
}

const insertVariable = (v: FormulaVariable) => {
  const textarea = codeInput.value
  if (textarea) {
    const start = textarea.selectionStart
    const text = `ctx.${v.name}`
    localFormula.value = localFormula.value.substring(0, start) + text + localFormula.value.substring(start)
    handleInput()
    nextTick(() => {
      textarea.focus()
      textarea.selectionStart = textarea.selectionEnd = start + text.length
    })
  }
}

const validateFormula = async () => {
  if (!localFormula.value.trim()) {
    validationResult.value = { formula: '', valid: false, errorMessage: '请输入公式' }
    return
  }

  try {
    const res = await apiValidateFormula(localFormula.value)
    validationResult.value = res?.data?.data || res?.data || res
  } catch (e: any) {
    validationResult.value = {
      formula: localFormula.value,
      valid: false,
      errorMessage: e.message || '验证失败'
    }
  }
}

const runPreview = () => {
  if (!localFormula.value.trim()) {
    previewResult.value = ''
    previewError.value = ''
    return
  }

  try {
    // 简单的本地预览（实际应调用后端API）
    // 这里仅做UI展示
    const ctx = { ...previewData.value }

    // 模拟一些内置函数
    const sum = (arr: number[]) => Array.isArray(arr) ? arr.reduce((a, b) => a + b, 0) : 0
    const average = (arr: number[]) => arr.length ? sum(arr) / arr.length : 0
    const clamp = (v: number, min: number, max: number) => Math.min(Math.max(v, min), max)

    // 构建执行环境
    const evalContext = { ctx, sum, average, clamp, Math }

    // 尝试执行（简化版，实际应调用后端）
    try {
      // 安全警告：生产环境不应使用eval，这里仅作演示
      const fn = new Function('ctx', 'sum', 'average', 'clamp', 'Math', `
        ${localFormula.value}
      `)
      const result = fn(ctx, sum, average, clamp, Math)
      previewResult.value = String(result)
      previewError.value = ''
    } catch (e: any) {
      previewError.value = e.message
      previewResult.value = ''
    }
  } catch (e: any) {
    previewError.value = e.message
    previewResult.value = ''
  }
}

// 初始化预览数据
watch(previewVariables, (vars) => {
  for (const v of vars) {
    if (!(v.name in previewData.value)) {
      previewData.value[v.name] = v.valueType === 'number' ? 0 :
                                   v.valueType === 'array' ? [] :
                                   v.valueType === 'boolean' ? false : ''
    }
  }
  // 设置默认值
  if (!('baseScore' in previewData.value)) previewData.value.baseScore = 100
  if (!('maxScore' in previewData.value)) previewData.value.maxScore = 120
  if (!('minScore' in previewData.value)) previewData.value.minScore = 0
  if (!('deductions' in previewData.value)) previewData.value.deductions = [2, 3, 1]
  if (!('additions' in previewData.value)) previewData.value.additions = [5]
}, { immediate: true })

watch(() => props.modelValue, (val) => {
  if (val !== localFormula.value) {
    localFormula.value = val
  }
}, { immediate: true })
</script>

<style scoped>
.formula-editor {
  width: 100%;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
}

.editor-container {
  display: flex;
}

.editor-area {
  flex: 1;
  min-width: 0;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}

.editor-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.code-editor-wrapper {
  position: relative;
}

.code-editor {
  width: 100%;
  min-height: 150px;
  padding: 12px;
  border: none;
  font-family: 'Monaco', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  resize: vertical;
  background: #1e1e2e;
  color: #a6e3a1;
  outline: none;
}

.code-editor::placeholder {
  color: #6c6c8a;
}

.validation-status {
  position: absolute;
  bottom: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.validation-status.success {
  background: rgba(82, 196, 26, 0.1);
  color: #52c41a;
}

.validation-status.error {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

/* 侧边栏 */
.editor-sidebar {
  width: 200px;
  border-left: 1px solid #e8e8e8;
  max-height: 300px;
  overflow-y: auto;
  background: #fafafa;
}

.sidebar-section {
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-section:last-child {
  border-bottom: none;
}

.sidebar-title {
  font-size: 11px;
  font-weight: 600;
  color: #8c8c8c;
  margin-bottom: 8px;
  text-transform: uppercase;
}

.sidebar-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-item {
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-item:hover {
  background: #e8e8ff;
  color: #5b5fc7;
}

.sidebar-item code {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 11px;
  color: #5b5fc7;
}

.sidebar-item .item-desc {
  font-size: 10px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-item .item-type {
  font-size: 10px;
  color: #999;
}

/* 预览区 */
.preview-section {
  padding: 16px;
  background: #f5f5f5;
  border-top: 1px solid #e8e8e8;
}

.preview-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.preview-inputs {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.preview-input {
  flex: 1;
  min-width: 80px;
  max-width: 120px;
}

.preview-input label {
  font-size: 11px;
  color: #666;
  display: block;
  margin-bottom: 4px;
}

.preview-result {
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.result-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.result-value {
  font-size: 24px;
  font-weight: 600;
  color: #5b5fc7;
}

.result-value.error {
  font-size: 14px;
  color: #ff4d4f;
}
</style>
