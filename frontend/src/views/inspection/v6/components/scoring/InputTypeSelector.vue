<template>
  <div class="input-type-selector">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="2" animated />
    </div>
    <template v-else>
      <!-- 基础打分方式 -->
      <div class="type-section">
        <div class="section-title">基础打分方式</div>
        <div class="type-grid">
          <div
            v-for="inputType in basicTypes"
            :key="inputType.code"
            class="type-card"
            :class="{ selected: modelValue === inputType.code, disabled: !inputType.isEnabled }"
            @click="selectType(inputType)"
          >
            <div class="card-icon">
              <el-icon :size="20">
                <component :is="getIcon(inputType.componentType)" />
              </el-icon>
            </div>
            <div class="card-content">
              <span class="card-name">{{ inputType.name }}</span>
              <span class="card-desc">{{ inputType.description }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 扩展打分方式 -->
      <div v-if="extendedTypes.length > 0" class="type-section">
        <div class="section-header" @click="showExtended = !showExtended">
          <span class="section-title">扩展打分方式</span>
          <el-icon class="toggle-icon" :class="{ expanded: showExtended }">
            <ArrowDown />
          </el-icon>
        </div>
        <el-collapse-transition>
          <div v-show="showExtended" class="type-grid">
            <div
              v-for="inputType in extendedTypes"
              :key="inputType.code"
              class="type-card"
              :class="{ selected: modelValue === inputType.code, disabled: !inputType.isEnabled }"
              @click="selectType(inputType)"
            >
              <div class="card-icon">
                <el-icon :size="20">
                  <component :is="getIcon(inputType.componentType)" />
                </el-icon>
              </div>
              <div class="card-content">
                <span class="card-name">{{ inputType.name }}</span>
                <span class="card-desc">{{ inputType.description }}</span>
              </div>
            </div>
          </div>
        </el-collapse-transition>
      </div>

      <!-- 组件配置 -->
      <div v-if="selectedType && hasConfig" class="config-section">
        <div class="config-title">{{ selectedType.name }} - 配置</div>
        <div class="config-form">
          <!-- 数值输入配置 -->
          <template v-if="selectedType.componentType === 'number'">
            <div class="config-row">
              <div class="config-item">
                <label>最小值</label>
                <el-input-number v-model="localConfig.min" size="small" :controls="false" @change="emitConfigChange" />
              </div>
              <div class="config-item">
                <label>最大值</label>
                <el-input-number v-model="localConfig.max" size="small" :controls="false" @change="emitConfigChange" />
              </div>
              <div class="config-item">
                <label>步长</label>
                <el-input-number v-model="localConfig.step" :min="0.1" :step="0.1" size="small" :controls="false" @change="emitConfigChange" />
              </div>
            </div>
          </template>

          <!-- 选项配置 -->
          <template v-else-if="selectedType.componentType === 'select'">
            <div class="options-editor">
              <div v-for="(opt, idx) in localConfig.options" :key="idx" class="option-row">
                <el-input v-model="opt.label" placeholder="选项名称" size="small" style="width: 120px" @change="emitConfigChange" />
                <el-input-number v-model="opt.score" placeholder="分值" size="small" style="width: 100px" :controls="false" @change="emitConfigChange" />
                <el-button v-if="localConfig.options.length > 2" link type="danger" size="small" @click="removeOption(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button link type="primary" size="small" @click="addOption">+ 添加选项</el-button>
            </div>
          </template>

          <!-- 星级配置 -->
          <template v-else-if="selectedType.componentType === 'star'">
            <div class="config-row">
              <div class="config-item">
                <label>最大星数</label>
                <el-input-number v-model="localConfig.max" :min="3" :max="10" size="small" @change="emitConfigChange" />
              </div>
              <div class="config-item">
                <label>允许半星</label>
                <el-switch v-model="localConfig.allowHalf" @change="emitConfigChange" />
              </div>
            </div>
          </template>

          <!-- 等级配置 -->
          <template v-else-if="selectedType.componentType === 'grade'">
            <div class="grades-editor">
              <div v-for="(grade, idx) in localConfig.grades" :key="idx" class="grade-row">
                <el-input v-model="localConfig.grades[idx]" size="small" style="width: 80px" @change="emitConfigChange" />
                <el-color-picker v-model="localConfig.colors[idx]" size="small" @change="emitConfigChange" />
                <el-button v-if="localConfig.grades.length > 2" link type="danger" size="small" @click="removeGrade(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button link type="primary" size="small" @click="addGrade">+ 添加等级</el-button>
            </div>
          </template>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowDown, Delete, Edit, Select, Checked, Operation, Star, Medal, Document } from '@element-plus/icons-vue'
import { getInputTypesGrouped } from '@/api/scoring'
import type { InputType } from '@/types/scoring'

const props = defineProps<{
  modelValue: string
  config?: Record<string, any>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:config', value: Record<string, any>): void
  (e: 'change', inputType: InputType): void
}>()

const loading = ref(false)
const inputTypesMap = ref<Record<string, InputType[]>>({})
const showExtended = ref(false)
const localConfig = ref<Record<string, any>>({})

const iconMap: Record<string, any> = {
  number: Edit,
  select: Select,
  checkbox: Checked,
  slider: Operation,
  star: Star,
  grade: Medal,
  textarea: Document
}

const basicTypes = computed(() => inputTypesMap.value['basic'] || [])
const extendedTypes = computed(() => inputTypesMap.value['extended'] || [])

const selectedType = computed(() => {
  for (const items of Object.values(inputTypesMap.value)) {
    const found = items.find(t => t.code === props.modelValue)
    if (found) return found
  }
  return null
})

const hasConfig = computed(() => {
  return selectedType.value && ['number', 'select', 'star', 'grade'].includes(selectedType.value.componentType)
})

const getIcon = (componentType: string) => iconMap[componentType] || Edit

const selectType = (inputType: InputType) => {
  if (!inputType.isEnabled) return
  emit('update:modelValue', inputType.code)
  emit('change', inputType)

  // 初始化配置
  if (inputType.componentConfig) {
    localConfig.value = { ...inputType.componentConfig }
    // 初始化选项列表
    if (inputType.componentType === 'select' && !localConfig.value.options) {
      localConfig.value.options = [
        { label: '合格', score: 0 },
        { label: '不合格', score: -2 }
      ]
    }
    emitConfigChange()
  }
}

const emitConfigChange = () => {
  emit('update:config', { ...localConfig.value })
}

const addOption = () => {
  if (!localConfig.value.options) localConfig.value.options = []
  localConfig.value.options.push({ label: '', score: 0 })
  emitConfigChange()
}

const removeOption = (idx: number) => {
  localConfig.value.options.splice(idx, 1)
  emitConfigChange()
}

const addGrade = () => {
  if (!localConfig.value.grades) localConfig.value.grades = ['A', 'B', 'C', 'D']
  if (!localConfig.value.colors) localConfig.value.colors = ['#52c41a', '#1890ff', '#faad14', '#ff4d4f']
  localConfig.value.grades.push('')
  localConfig.value.colors.push('#999999')
  emitConfigChange()
}

const removeGrade = (idx: number) => {
  localConfig.value.grades.splice(idx, 1)
  localConfig.value.colors.splice(idx, 1)
  emitConfigChange()
}

const loadInputTypes = async () => {
  loading.value = true
  try {
    const res = await getInputTypesGrouped()
    inputTypesMap.value = res?.data?.data || res?.data || res || {}
  } catch (e) {
    console.error('Failed to load input types:', e)
  } finally {
    loading.value = false
  }
}

watch(() => props.config, (val) => {
  if (val) {
    localConfig.value = { ...val }
  }
}, { immediate: true })

onMounted(() => {
  loadInputTypes()
})
</script>

<style scoped>
.input-type-selector {
  width: 100%;
}

.loading-state {
  padding: 16px;
}

.type-section {
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 0;
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.section-header .section-title {
  margin-bottom: 0;
}

.toggle-icon {
  margin-left: 8px;
  transition: transform 0.3s;
  color: #8c8c8c;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.type-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.type-card:hover {
  border-color: #b8b8ff;
  background: #fafaff;
}

.type-card.selected {
  border-color: #5b5fc7;
  background: #f5f5ff;
}

.type-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.card-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 8px;
  color: #666;
}

.type-card.selected .card-icon {
  background: #e8e8ff;
  color: #5b5fc7;
}

.card-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-name {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a2e;
}

.card-desc {
  font-size: 11px;
  color: #8c8c8c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 配置区域 */
.config-section {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
}

.config-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.config-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-item label {
  font-size: 12px;
  color: #666;
}

/* 选项编辑器 */
.options-editor, .grades-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row, .grade-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
