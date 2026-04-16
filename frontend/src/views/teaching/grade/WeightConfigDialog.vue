<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="加权总评配置"
    width="480px"
    :close-on-click-modal="false"
  >
    <el-form label-width="90px" size="small" style="padding: 0 8px;">
      <el-form-item label="课程">
        <el-select
          v-model="courseId"
          placeholder="选择课程"
          filterable
          style="width: 100%"
          @change="loadConfig"
        >
          <el-option
            v-for="c in courses"
            :key="c.id"
            :label="c.courseName"
            :value="c.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="平时成绩">
        <el-input-number
          v-model="weights[0].weightPercent"
          :min="0"
          :max="100"
          :step="5"
          controls-position="right"
          style="width: 140px"
        />
        <span style="margin-left: 4px; color: #606266;">%</span>
      </el-form-item>

      <el-form-item label="期中成绩">
        <el-input-number
          v-model="weights[1].weightPercent"
          :min="0"
          :max="100"
          :step="5"
          controls-position="right"
          style="width: 140px"
        />
        <span style="margin-left: 4px; color: #606266;">%</span>
      </el-form-item>

      <el-form-item label="期末成绩">
        <el-input-number
          v-model="weights[2].weightPercent"
          :min="0"
          :max="100"
          :step="5"
          controls-position="right"
          style="width: 140px"
        />
        <span style="margin-left: 4px; color: #606266;">%</span>
      </el-form-item>

      <el-form-item label="合计">
        <span
          :style="{
            color: totalWeight === 100 ? '#67c23a' : '#f56c6c',
            fontWeight: 'bold',
            fontSize: '14px',
          }"
        >
          {{ totalWeight }}%
        </span>
        <span
          v-if="totalWeight !== 100"
          style="color: #f56c6c; margin-left: 8px; font-size: 12px;"
        >
          (必须为100%)
        </span>
      </el-form-item>
    </el-form>

    <template #footer>
      <div style="display: flex; gap: 8px; justify-content: flex-end;">
        <el-button
          size="small"
          type="primary"
          :disabled="totalWeight !== 100 || !courseId"
          :loading="saving"
          @click="saveConfig"
        >
          保存配置
        </el-button>
        <el-button
          size="small"
          type="success"
          :disabled="totalWeight !== 100 || !courseId"
          :loading="calculating"
          @click="doCalculateOverall"
        >
          计算总评
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { gradeApi } from '@/api/teaching'
import { courseApi } from '@/api/academic'
import type { Course } from '@/types/academic'

const props = defineProps<{
  modelValue: boolean
  semesterId?: number | string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  calculated: []
}>()

const courseId = ref<number>()
const courses = ref<Course[]>([])
const saving = ref(false)
const calculating = ref(false)

const weights = ref([
  { componentType: 1, weightPercent: 30 },
  { componentType: 2, weightPercent: 30 },
  { componentType: 3, weightPercent: 40 },
])

const totalWeight = computed(() =>
  weights.value.reduce((sum, w) => sum + (w.weightPercent || 0), 0)
)

// Load courses when dialog opens
watch(() => props.modelValue, async (visible) => {
  if (visible) {
    try {
      courses.value = await courseApi.listAll()
    } catch {
      courses.value = []
    }
  }
})

// Load existing config when course changes
async function loadConfig() {
  if (!props.semesterId || !courseId.value) return
  try {
    const res: any = await gradeApi.getWeightConfigs(
      Number(props.semesterId),
      Number(courseId.value)
    )
    const configs: Array<{ componentType: number; weightPercent: number }> =
      Array.isArray(res) ? res : res.data || []
    if (configs.length > 0) {
      // Map backend configs into local weights array by componentType
      for (const cfg of configs) {
        const idx = weights.value.findIndex(w => w.componentType === cfg.componentType)
        if (idx >= 0) {
          weights.value[idx].weightPercent = cfg.weightPercent
        }
      }
    } else {
      // Reset to defaults
      weights.value[0].weightPercent = 30
      weights.value[1].weightPercent = 30
      weights.value[2].weightPercent = 40
    }
  } catch {
    // Ignore - keep current values
  }
}

async function saveConfig() {
  if (!props.semesterId || !courseId.value) {
    ElMessage.warning('请选择学期和课程')
    return
  }
  saving.value = true
  try {
    await gradeApi.saveWeightConfigs({
      semesterId: Number(props.semesterId),
      courseId: Number(courseId.value),
      configs: weights.value.map(w => ({
        componentType: w.componentType,
        weightPercent: w.weightPercent,
      })),
    })
    ElMessage.success('权重配置已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function doCalculateOverall() {
  if (!props.semesterId || !courseId.value) {
    ElMessage.warning('请选择学期和课程')
    return
  }
  calculating.value = true
  try {
    const res: any = await gradeApi.calculateOverall({
      semesterId: Number(props.semesterId),
      courseId: Number(courseId.value),
    })
    const data = res.data || res
    ElMessage.success(
      `总评计算完成: 计算了 ${data.calculated} 人, 跳过 ${data.skipped} 人`
    )
    emit('calculated')
  } catch {
    ElMessage.error('计算总评失败')
  } finally {
    calculating.value = false
  }
}
</script>
