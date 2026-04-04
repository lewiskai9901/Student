<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Trash2 } from 'lucide-vue-next'
import { createGradeScheme, updateGradeScheme } from '@/api/insp/gradeScheme'
import type { GradeScheme, GradeDefinitionInput } from '@/types/insp/gradeScheme'

const props = defineProps<{
  modelValue: boolean
  scheme: GradeScheme | null
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  saved: [scheme: GradeScheme]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const saving = ref(false)

interface FormGrade {
  code: string
  name: string
  minValue: number
  maxValue: number
  color: string
  icon: string
}

const form = ref({
  displayName: '',
  description: '',
  schemeType: 'PERCENT_RANGE' as 'SCORE_RANGE' | 'PERCENT_RANGE',
  grades: [] as FormGrade[],
})

// Presets
const PRESETS: Record<string, { displayName: string; schemeType: 'SCORE_RANGE' | 'PERCENT_RANGE'; grades: FormGrade[] }> = {
  five: {
    displayName: '标准五级评定',
    schemeType: 'PERCENT_RANGE',
    grades: [
      { code: 'A', name: '优秀', minValue: 90, maxValue: 100, color: '#22C55E', icon: '' },
      { code: 'B', name: '良好', minValue: 80, maxValue: 89.99, color: '#3B82F6', icon: '' },
      { code: 'C', name: '中等', minValue: 70, maxValue: 79.99, color: '#F59E0B', icon: '' },
      { code: 'D', name: '及格', minValue: 60, maxValue: 69.99, color: '#F97316', icon: '' },
      { code: 'F', name: '不及格', minValue: 0, maxValue: 59.99, color: '#EF4444', icon: '' },
    ],
  },
  pass: {
    displayName: '合格评定',
    schemeType: 'PERCENT_RANGE',
    grades: [
      { code: 'PASS', name: '合格', minValue: 60, maxValue: 100, color: '#22C55E', icon: '' },
      { code: 'FAIL', name: '不合格', minValue: 0, maxValue: 59.99, color: '#EF4444', icon: '' },
    ],
  },
  flag: {
    displayName: '流动红旗',
    schemeType: 'PERCENT_RANGE',
    grades: [
      { code: 'RED', name: '红旗', minValue: 90, maxValue: 100, color: '#EF4444', icon: 'flag' },
      { code: 'BLUE', name: '蓝旗', minValue: 70, maxValue: 89.99, color: '#3B82F6', icon: 'flag' },
      { code: 'YELLOW', name: '黄旗', minValue: 0, maxValue: 69.99, color: '#F59E0B', icon: 'flag' },
    ],
  },
  star: {
    displayName: '星级评定',
    schemeType: 'PERCENT_RANGE',
    grades: [
      { code: '5STAR', name: '五星', minValue: 90, maxValue: 100, color: '#FFD700', icon: 'star' },
      { code: '4STAR', name: '四星', minValue: 80, maxValue: 89.99, color: '#FFA500', icon: 'star' },
      { code: '3STAR', name: '三星', minValue: 70, maxValue: 79.99, color: '#C0C0C0', icon: 'star' },
      { code: '2STAR', name: '二星', minValue: 60, maxValue: 69.99, color: '#CD7F32', icon: 'star' },
      { code: '1STAR', name: '一星', minValue: 0, maxValue: 59.99, color: '#808080', icon: 'star' },
    ],
  },
}

// Watch scheme prop to populate form
watch(() => props.modelValue, (visible) => {
  if (!visible) return
  if (props.scheme) {
    form.value = {
      displayName: props.scheme.displayName,
      description: props.scheme.description || '',
      schemeType: props.scheme.schemeType,
      grades: props.scheme.grades.map(g => ({
        code: g.code,
        name: g.name,
        minValue: g.minValue,
        maxValue: g.maxValue,
        color: g.color || '#409EFF',
        icon: g.icon || '',
      })),
    }
  } else {
    form.value = {
      displayName: '',
      description: '',
      schemeType: 'PERCENT_RANGE',
      grades: [],
    }
  }
})

function applyPreset(key: string) {
  const preset = PRESETS[key]
  if (!preset) return
  form.value.displayName = form.value.displayName || preset.displayName
  form.value.schemeType = preset.schemeType
  form.value.grades = preset.grades.map(g => ({ ...g }))
}

function addGrade() {
  const last = form.value.grades[form.value.grades.length - 1]
  form.value.grades.push({
    code: '',
    name: '',
    minValue: last ? last.maxValue + 0.01 : 0,
    maxValue: 100,
    color: '#409EFF',
    icon: '',
  })
}

function removeGrade(index: number) {
  form.value.grades.splice(index, 1)
}

// Validation
const validationWarnings = computed(() => {
  const warnings: string[] = []
  const grades = form.value.grades
  if (grades.length === 0) return warnings

  // Check overlaps
  for (let i = 0; i < grades.length; i++) {
    for (let j = i + 1; j < grades.length; j++) {
      const a = grades[i]
      const b = grades[j]
      if (a.minValue <= b.maxValue && b.minValue <= a.maxValue) {
        warnings.push(`"${a.name || a.code}" 和 "${b.name || b.code}" 的分数范围重叠`)
      }
    }
  }

  // Check gaps
  const sorted = [...grades].sort((a, b) => a.minValue - b.minValue)
  for (let i = 0; i < sorted.length - 1; i++) {
    const gap = sorted[i + 1].minValue - sorted[i].maxValue
    if (gap > 0.02) {
      warnings.push(`${sorted[i].maxValue} ~ ${sorted[i + 1].minValue} 之间存在未覆盖区间`)
    }
  }

  return warnings
})

const hasOverlap = computed(() => {
  const grades = form.value.grades
  for (let i = 0; i < grades.length; i++) {
    for (let j = i + 1; j < grades.length; j++) {
      const a = grades[i]
      const b = grades[j]
      if (a.minValue <= b.maxValue && b.minValue <= a.maxValue) return true
    }
  }
  return false
})

async function handleSave() {
  if (!form.value.displayName.trim()) {
    ElMessage.warning('请输入方案名称')
    return
  }
  if (form.value.grades.length === 0) {
    ElMessage.warning('请至少添加一个等级')
    return
  }
  if (hasOverlap.value) {
    ElMessage.error('存在分数范围重叠，请修正后保存')
    return
  }

  const gradesPayload: GradeDefinitionInput[] = form.value.grades.map((g, i) => ({
    code: g.code,
    name: g.name,
    minValue: g.minValue,
    maxValue: g.maxValue,
    color: g.color || null,
    icon: g.icon || null,
    sortOrder: i,
  }))

  saving.value = true
  try {
    let result: GradeScheme
    if (props.scheme) {
      result = await updateGradeScheme(props.scheme.id, {
        displayName: form.value.displayName,
        description: form.value.description || undefined,
        grades: gradesPayload,
      })
    } else {
      result = await createGradeScheme({
        displayName: form.value.displayName,
        description: form.value.description || undefined,
        schemeType: form.value.schemeType,
        grades: gradesPayload,
      })
    }
    ElMessage.success('保存成功')
    emit('saved', result)
    dialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="等级方案"
    width="640px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <!-- Form fields -->
    <div class="gse-form">
      <div class="gse-row">
        <div class="gse-field" style="flex: 1">
          <label class="gse-label">方案名称</label>
          <el-input v-model="form.displayName" placeholder="如：卫生流动红旗" size="small" />
        </div>
        <div class="gse-field" style="width: 160px">
          <label class="gse-label">映射类型</label>
          <el-select v-model="form.schemeType" size="small" :disabled="!!scheme" style="width: 100%">
            <el-option label="百分比" value="PERCENT_RANGE" />
            <el-option label="绝对分数" value="SCORE_RANGE" />
          </el-select>
        </div>
      </div>
      <div class="gse-field">
        <label class="gse-label">方案描述</label>
        <el-input v-model="form.description" placeholder="可选描述" size="small" />
      </div>
    </div>

    <!-- Quick presets -->
    <div class="gse-presets">
      <span class="gse-presets-label">快速模板</span>
      <el-button v-for="(preset, key) in PRESETS" :key="key" text size="small" @click="applyPreset(key as string)">
        {{ preset.displayName }}
      </el-button>
    </div>

    <!-- Grade table -->
    <el-table :data="form.grades" size="small" stripe class="gse-table" max-height="320">
      <el-table-column label="#" width="40" align="center">
        <template #default="{ $index }">
          <span class="text-xs text-gray-400">{{ $index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="编码" width="90">
        <template #default="{ row }">
          <el-input v-model="row.code" size="small" placeholder="A" />
        </template>
      </el-table-column>
      <el-table-column label="名称" width="100">
        <template #default="{ row }">
          <el-input v-model="row.name" size="small" placeholder="优秀" />
        </template>
      </el-table-column>
      <el-table-column label="下限" width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.minValue" size="small" :precision="2" :step="1" :min="0" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="上限" width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.maxValue" size="small" :precision="2" :step="1" :min="0" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="颜色" width="60" align="center">
        <template #default="{ row }">
          <el-color-picker v-model="row.color" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="" width="44" align="center">
        <template #default="{ $index }">
          <el-button link type="danger" size="small" @click="removeGrade($index)">
            <Trash2 class="w-3.5 h-3.5" />
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-button text type="primary" size="small" class="gse-add-btn" @click="addGrade">+ 添加等级</el-button>

    <!-- Validation warnings -->
    <div v-if="validationWarnings.length > 0" class="gse-warnings">
      <div v-for="(w, i) in validationWarnings" :key="i" class="gse-warning-item">
        {{ w }}
      </div>
    </div>

    <!-- Footer -->
    <template #footer>
      <el-button size="small" @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" size="small" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.gse-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}
.gse-row {
  display: flex;
  gap: 10px;
}
.gse-field {
  display: flex;
  flex-direction: column;
}
.gse-label {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
}
.gse-presets {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
  padding: 6px 10px;
  background: #f9fafb;
  border-radius: 6px;
}
.gse-presets-label {
  font-size: 12px;
  color: #9ca3af;
  margin-right: 4px;
  flex-shrink: 0;
}
.gse-table {
  width: 100%;
}
.gse-add-btn {
  margin-top: 8px;
}
.gse-warnings {
  margin-top: 10px;
  padding: 8px 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 6px;
}
.gse-warning-item {
  font-size: 12px;
  color: #b45309;
  line-height: 1.6;
}
</style>
