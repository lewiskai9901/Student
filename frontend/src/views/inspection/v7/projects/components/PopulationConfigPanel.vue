<script setup lang="ts">
/**
 * PopulationConfigPanel - Population source configuration
 *
 * Configures how the inspection population (headcount) is determined:
 * AUTO (detect from org structure), MANUAL (input at execution time),
 * or FIXED (set a static value now).
 */
import { computed } from 'vue'

interface PopulationConfig {
  source: string
  fixedValue?: number
}

const props = defineProps<{
  modelValue: PopulationConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PopulationConfig]
}>()

// ---------- Computed ----------

const source = computed({
  get: () => props.modelValue.source ?? 'AUTO',
  set: (val: string) => {
    emit('update:modelValue', { ...props.modelValue, source: val })
  },
})

const fixedValue = computed({
  get: () => props.modelValue.fixedValue ?? 0,
  set: (val: number) => {
    emit('update:modelValue', { ...props.modelValue, fixedValue: val })
  },
})

// ---------- Source Options ----------

const sourceOptions = [
  {
    value: 'AUTO',
    label: '自动检测',
    description: '从组织架构中自动获取对应范围的在册人数',
  },
  {
    value: 'MANUAL',
    label: '手动输入',
    description: '每次执行检查时由检查员手动输入实际人数',
  },
  {
    value: 'FIXED',
    label: '固定值',
    description: '使用预设的固定人数，适用于标准化场景',
  },
]
</script>

<template>
  <div class="population-config-panel space-y-4">
    <div>
      <label class="mb-2 block text-sm font-medium text-gray-700">
        人数来源方式
      </label>
      <el-radio-group v-model="source" class="w-full">
        <div class="space-y-2">
          <div
            v-for="opt in sourceOptions"
            :key="opt.value"
            class="flex items-start gap-2 rounded-md border px-3 py-2.5 transition cursor-pointer"
            :class="source === opt.value
              ? 'border-blue-400 bg-blue-50'
              : 'border-gray-200 hover:border-gray-300'"
            @click="source = opt.value"
          >
            <el-radio :value="opt.value" class="!mt-0.5" />
            <div>
              <span class="text-sm font-medium text-gray-700">{{ opt.label }}</span>
              <p class="text-xs text-gray-400 mt-0.5">{{ opt.description }}</p>
            </div>
          </div>
        </div>
      </el-radio-group>
    </div>

    <!-- FIXED mode: input value -->
    <div v-if="source === 'FIXED'" class="rounded-md border border-gray-200 bg-gray-50 p-3">
      <label class="mb-1 block text-sm text-gray-600">固定人数</label>
      <el-input-number
        v-model="fixedValue"
        :min="0"
        :max="99999"
        :step="1"
        controls-position="right"
      />
      <p class="mt-1 text-xs text-gray-400">
        该值将应用于所有检查目标的人口归一化计算
      </p>
    </div>

    <!-- AUTO mode: info -->
    <div v-if="source === 'AUTO'" class="rounded-md bg-green-50 px-3 py-2">
      <p class="text-xs text-green-600">
        系统将在任务生成时自动从组织架构中查询目标范围内的注册人数。
        若组织数据变更，新任务将自动使用最新人数。
      </p>
    </div>

    <!-- MANUAL mode: info -->
    <div v-if="source === 'MANUAL'" class="rounded-md bg-amber-50 px-3 py-2">
      <p class="text-xs text-amber-600">
        检查员在执行每个检查目标时需要手动填写当前实际人数。
        此模式适合人数经常波动的场景（如实到人数统计）。
      </p>
    </div>
  </div>
</template>
