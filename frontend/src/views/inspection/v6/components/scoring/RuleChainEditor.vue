<template>
  <div class="rule-chain-editor">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="4" animated />
    </div>
    <template v-else>
      <div class="rule-list">
        <div
          v-for="rule in allRules"
          :key="rule.code"
          class="rule-item"
          :class="{ active: isRuleEnabled(rule.code) }"
        >
          <div class="rule-switch" :class="{ on: isRuleEnabled(rule.code) }" @click="toggleRule(rule)"></div>
          <div class="rule-content">
            <div class="rule-header">
              <span class="rule-name">{{ rule.name }}</span>
              <el-tag v-if="rule.isSystem" size="small" type="info">内置</el-tag>
            </div>
            <div class="rule-desc">{{ rule.description }}</div>

            <!-- 规则参数 -->
            <div v-if="isRuleEnabled(rule.code) && hasParameters(rule)" class="rule-params">
              <div v-for="(schema, key) in rule.parametersSchema" :key="key" class="rule-param">
                <label>{{ schema.label || key }}:</label>
                <el-input-number
                  v-if="schema.type === 'number'"
                  v-model="ruleParams[rule.code][key]"
                  :min="schema.min"
                  :max="schema.max"
                  :step="schema.step || (schema.type === 'number' && key.includes('multiplier') ? 0.1 : 1)"
                  :precision="schema.precision || (key.includes('multiplier') ? 1 : 0)"
                  size="small"
                  @change="emitChange"
                />
                <el-select
                  v-else-if="schema.type === 'array'"
                  v-model="ruleParams[rule.code][key]"
                  multiple
                  size="small"
                  placeholder="选择..."
                  style="width: 160px"
                  @change="emitChange"
                >
                  <el-option
                    v-for="opt in getSelectOptions(schema)"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <el-input
                  v-else
                  v-model="ruleParams[rule.code][key]"
                  size="small"
                  style="width: 120px"
                  @change="emitChange"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 自定义公式 -->
      <div class="custom-formula-section">
        <div class="section-header" @click="showCustomFormula = !showCustomFormula">
          <span class="section-title">自定义公式（高级）</span>
          <el-icon class="toggle-icon" :class="{ expanded: showCustomFormula }">
            <ArrowDown />
          </el-icon>
        </div>
        <el-collapse-transition>
          <div v-show="showCustomFormula" class="formula-content">
            <FormulaEditor
              v-model="customFormula"
              :functions="formulaFunctions"
              :variables="formulaVariables"
              @change="emitChange"
            />
          </div>
        </el-collapse-transition>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { getAllRules, getAllFormulaResources } from '@/api/scoring'
import type { CalculationRule, FormulaFunction, FormulaVariable } from '@/types/scoring'
import FormulaEditor from './FormulaEditor.vue'

interface RuleConfig {
  code: string
  enabled: boolean
  parameters: Record<string, any>
}

const props = defineProps<{
  modelValue: RuleConfig[]
  formula?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: RuleConfig[]): void
  (e: 'update:formula', value: string): void
  (e: 'change'): void
}>()

const loading = ref(false)
const allRules = ref<CalculationRule[]>([])
const enabledRules = ref<Set<string>>(new Set())
const ruleParams = ref<Record<string, Record<string, any>>>({})
const showCustomFormula = ref(false)
const customFormula = ref('')
const formulaFunctions = ref<FormulaFunction[]>([])
const formulaVariables = ref<FormulaVariable[]>([])

const isRuleEnabled = (code: string) => enabledRules.value.has(code)

const hasParameters = (rule: CalculationRule) => {
  return rule.parametersSchema && Object.keys(rule.parametersSchema).length > 0
}

const getSelectOptions = (schema: any) => {
  // 这里可以根据schema动态获取选项
  return schema.options || []
}

const toggleRule = (rule: CalculationRule) => {
  if (enabledRules.value.has(rule.code)) {
    enabledRules.value.delete(rule.code)
  } else {
    enabledRules.value.add(rule.code)
    // 初始化参数
    if (!ruleParams.value[rule.code]) {
      ruleParams.value[rule.code] = { ...rule.defaultParameters }
    }
  }
  emitChange()
}

const emitChange = () => {
  const configs: RuleConfig[] = []
  for (const code of enabledRules.value) {
    configs.push({
      code,
      enabled: true,
      parameters: ruleParams.value[code] || {}
    })
  }
  emit('update:modelValue', configs)
  emit('update:formula', customFormula.value)
  emit('change')
}

const loadRules = async () => {
  loading.value = true
  try {
    const [rulesRes, resourcesRes] = await Promise.all([
      getAllRules(),
      getAllFormulaResources()
    ])

    allRules.value = rulesRes?.data?.data || rulesRes?.data || rulesRes || []

    const resources = resourcesRes?.data?.data || resourcesRes?.data || resourcesRes || {}
    // 展平函数和变量
    formulaFunctions.value = Object.values(resources.functions || {}).flat() as FormulaFunction[]
    formulaVariables.value = Object.values(resources.variables || {}).flat() as FormulaVariable[]

    // 初始化规则参数
    for (const rule of allRules.value) {
      if (rule.defaultParameters) {
        ruleParams.value[rule.code] = { ...rule.defaultParameters }
      }
    }
  } catch (e) {
    console.error('Failed to load rules:', e)
  } finally {
    loading.value = false
  }
}

// 从props初始化
watch(() => props.modelValue, (val) => {
  if (val && val.length > 0) {
    enabledRules.value = new Set(val.filter(r => r.enabled).map(r => r.code))
    for (const config of val) {
      if (config.parameters) {
        ruleParams.value[config.code] = { ...config.parameters }
      }
    }
  }
}, { immediate: true })

watch(() => props.formula, (val) => {
  if (val) {
    customFormula.value = val
    showCustomFormula.value = true
  }
}, { immediate: true })

onMounted(() => {
  loadRules()
})
</script>

<style scoped>
.rule-chain-editor {
  width: 100%;
}

.loading-state {
  padding: 16px;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.rule-item.active {
  background: #f5f5ff;
  border-color: #d0d0ff;
}

.rule-switch {
  position: relative;
  width: 40px;
  height: 22px;
  background: #d9d9d9;
  border-radius: 11px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-top: 2px;
}

.rule-switch::after {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  background: #fff;
  border-radius: 50%;
  top: 2px;
  left: 2px;
  transition: all 0.2s;
}

.rule-switch.on {
  background: #5b5fc7;
}

.rule-switch.on::after {
  left: 20px;
}

.rule-content {
  flex: 1;
  min-width: 0;
}

.rule-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.rule-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a2e;
}

.rule-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.rule-params {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.rule-param {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rule-param label {
  font-size: 12px;
  color: #666;
}

/* 自定义公式部分 */
.custom-formula-section {
  margin-top: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fafafa;
  cursor: pointer;
}

.section-header:hover {
  background: #f0f0f0;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.toggle-icon {
  transition: transform 0.3s;
  color: #8c8c8c;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

.formula-content {
  padding: 16px;
  background: #fff;
}
</style>
