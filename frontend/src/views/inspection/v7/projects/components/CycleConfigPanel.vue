<script setup lang="ts">
/**
 * CycleConfigPanel - Period cycle configuration
 *
 * Configures inspection frequency: DAILY, WEEKLY, MONTHLY, or CUSTOM.
 * Shows conditional fields based on the selected cycle type.
 */
import { computed } from 'vue'
import { CycleTypeConfig } from '@/types/insp/enums'

interface CycleConfig {
  cycleType: string
  startDate?: string
  endDate?: string
  daysOfWeek?: number[]
  dayOfMonth?: number
}

const props = defineProps<{
  modelValue: CycleConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: CycleConfig]
}>()

// ---------- Computed ----------

const cycleType = computed({
  get: () => props.modelValue.cycleType,
  set: (val: string) => update({ cycleType: val }),
})

const startDate = computed({
  get: () => props.modelValue.startDate ?? '',
  set: (val: string) => update({ startDate: val }),
})

const endDate = computed({
  get: () => props.modelValue.endDate ?? '',
  set: (val: string) => update({ endDate: val }),
})

const daysOfWeek = computed({
  get: () => props.modelValue.daysOfWeek ?? [],
  set: (val: number[]) => update({ daysOfWeek: val }),
})

const dayOfMonth = computed({
  get: () => props.modelValue.dayOfMonth ?? 1,
  set: (val: number) => update({ dayOfMonth: val }),
})

const cycleTypeOptions = computed(() =>
  Object.entries(CycleTypeConfig).map(([key, cfg]) => ({
    value: key,
    label: cfg.label,
  }))
)

const isWeekly = computed(() =>
  cycleType.value === 'WEEKLY' || cycleType.value === 'BIWEEKLY'
)

const isMonthly = computed(() =>
  cycleType.value === 'MONTHLY' || cycleType.value === 'QUARTERLY'
)

const isCustom = computed(() => cycleType.value === 'CUSTOM')

// ---------- Day of Week Options ----------

const weekDayOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
]

// Day of month options (1-28 to be safe across months)
const dayOfMonthOptions = Array.from({ length: 28 }, (_, i) => ({
  value: i + 1,
  label: `${i + 1} 日`,
}))

// ---------- Helper ----------

function update(partial: Partial<CycleConfig>) {
  emit('update:modelValue', { ...props.modelValue, ...partial })
}
</script>

<template>
  <div class="cycle-config-panel space-y-4">
    <div>
      <label class="mb-1 block text-sm font-medium text-gray-700">检查周期</label>
      <el-select v-model="cycleType" class="w-full">
        <el-option
          v-for="opt in cycleTypeOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
    </div>

    <!-- Date Range (always shown) -->
    <div class="grid grid-cols-2 gap-3">
      <div>
        <label class="mb-1 block text-sm text-gray-600">开始日期</label>
        <el-date-picker
          v-model="startDate"
          type="date"
          placeholder="选择开始日期"
          value-format="YYYY-MM-DD"
          class="!w-full"
        />
      </div>
      <div>
        <label class="mb-1 block text-sm text-gray-600">结束日期</label>
        <el-date-picker
          v-model="endDate"
          type="date"
          placeholder="选择结束日期（可选）"
          value-format="YYYY-MM-DD"
          class="!w-full"
        />
      </div>
    </div>

    <!-- Weekly/Biweekly: Day of week selection -->
    <div v-if="isWeekly">
      <label class="mb-1 block text-sm text-gray-600">检查日</label>
      <el-checkbox-group v-model="daysOfWeek">
        <el-checkbox
          v-for="day in weekDayOptions"
          :key="day.value"
          :label="day.value"
          :value="day.value"
        >
          {{ day.label }}
        </el-checkbox>
      </el-checkbox-group>
      <p class="mt-1 text-xs text-gray-400">
        选择每周需要执行检查的日期
      </p>
    </div>

    <!-- Monthly/Quarterly: Day of month selection -->
    <div v-if="isMonthly">
      <label class="mb-1 block text-sm text-gray-600">每月检查日</label>
      <el-select v-model="dayOfMonth" class="w-full">
        <el-option
          v-for="opt in dayOfMonthOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <p class="mt-1 text-xs text-gray-400">
        选择每月执行检查的固定日期
      </p>
    </div>

    <!-- Custom: shows both day-of-week and custom interval hints -->
    <div v-if="isCustom" class="rounded-md border border-gray-200 bg-gray-50 p-3">
      <label class="mb-2 block text-sm text-gray-600">自定义检查日</label>
      <el-checkbox-group v-model="daysOfWeek">
        <el-checkbox
          v-for="day in weekDayOptions"
          :key="day.value"
          :label="day.value"
          :value="day.value"
        >
          {{ day.label }}
        </el-checkbox>
      </el-checkbox-group>
      <p class="mt-2 text-xs text-gray-400">
        自定义模式下可自由组合检查日
      </p>
    </div>

    <!-- Summary -->
    <div class="rounded-md bg-blue-50 px-3 py-2">
      <p class="text-xs text-blue-600">
        <template v-if="cycleType === 'DAILY'">每天执行检查</template>
        <template v-else-if="isWeekly && daysOfWeek.length > 0">
          每{{ cycleType === 'BIWEEKLY' ? '两' : '' }}周的
          {{ daysOfWeek.map(d => weekDayOptions.find(w => w.value === d)?.label).filter(Boolean).join('、') }}
          执行检查
        </template>
        <template v-else-if="isMonthly">
          每{{ cycleType === 'QUARTERLY' ? '季度' : '月' }}{{ dayOfMonth }}日执行检查
        </template>
        <template v-else-if="isCustom && daysOfWeek.length > 0">
          自定义周期：{{ daysOfWeek.map(d => weekDayOptions.find(w => w.value === d)?.label).filter(Boolean).join('、') }}
        </template>
        <template v-else>请完成周期配置</template>
      </p>
    </div>
  </div>
</template>
