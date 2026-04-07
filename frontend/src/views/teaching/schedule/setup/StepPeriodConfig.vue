<template>
  <div>
    <h3 class="setup-step-title">节次时间配置</h3>
    <p class="setup-step-desc">配置本学期每天上几节课、每节课的时间。不同学期可以有不同配置。</p>

    <!-- Presets -->
    <div style="display: flex; gap: 8px; margin-bottom: 16px;">
      <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(8)">标准8节制</button>
      <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(10)">10节制(含晚自习)</button>
      <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(6)">6节制</button>
    </div>

    <!-- Schedule Days -->
    <div class="tm-field" style="margin-bottom: 16px;">
      <label class="tm-label">排课日</label>
      <div style="display: flex; gap: 8px;">
        <label v-for="d in allDays" :key="d.value" style="display: flex; align-items: center; gap: 4px; font-size: 13px; cursor: pointer;">
          <input type="checkbox" :value="d.value" v-model="scheduleDays" /> {{ d.label }}
        </label>
      </div>
    </div>

    <!-- Period Table -->
    <table class="tm-table" style="margin-bottom: 16px;">
      <colgroup>
        <col style="width: 60px" />
        <col />
        <col style="width: 120px" />
        <col style="width: 120px" />
        <col style="width: 60px" />
      </colgroup>
      <thead>
        <tr>
          <th>序号</th>
          <th class="text-left">名称</th>
          <th>开始时间</th>
          <th>结束时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(p, idx) in periods" :key="idx">
          <td class="tm-mono">{{ p.period }}</td>
          <td class="text-left">
            <input v-model="p.name" class="tm-input" style="padding: 4px 8px;" />
          </td>
          <td><input v-model="p.startTime" type="time" class="tm-input" style="padding: 4px 8px; text-align: center;" /></td>
          <td><input v-model="p.endTime" type="time" class="tm-input" style="padding: 4px 8px; text-align: center;" /></td>
          <td><button class="tm-action tm-action-danger" @click="removePeriod(idx)">删</button></td>
        </tr>
        <tr v-if="periods.length === 0">
          <td colspan="5" class="tm-empty">暂无节次，请选择预设或手动添加</td>
        </tr>
      </tbody>
    </table>

    <div style="display: flex; gap: 8px;">
      <button class="tm-btn tm-btn-secondary" @click="addPeriod">添加一节</button>
      <span style="flex: 1;" />
      <button class="tm-btn tm-btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存配置' }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { scheduleConfigApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined; config: any }>()
const emit = defineEmits<{ saved: [config: any] }>()

const allDays = [
  { value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' },
  { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' }, { value: 7, label: '周日' },
]

const scheduleDays = ref<number[]>([1, 2, 3, 4, 5])
const periods = ref<{ period: number; name: string; startTime: string; endTime: string }[]>([])
const saving = ref(false)

const PRESETS: Record<number, { period: number; name: string; startTime: string; endTime: string }[]> = {
  6: [
    { period: 1, name: '第一节', startTime: '08:00', endTime: '08:40' },
    { period: 2, name: '第二节', startTime: '08:50', endTime: '09:30' },
    { period: 3, name: '第三节', startTime: '09:50', endTime: '10:30' },
    { period: 4, name: '第四节', startTime: '10:40', endTime: '11:20' },
    { period: 5, name: '第五节', startTime: '14:00', endTime: '14:40' },
    { period: 6, name: '第六节', startTime: '14:50', endTime: '15:30' },
  ],
  8: [
    { period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
    { period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
    { period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
    { period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
    { period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },
    { period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
  ],
  10: [
    { period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
    { period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
    { period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
    { period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
    { period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },
    { period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
    { period: 9, name: '第九节', startTime: '19:00', endTime: '19:45' },
    { period: 10, name: '第十节', startTime: '19:55', endTime: '20:40' },
  ],
}

function applyPreset(n: number) {
  periods.value = JSON.parse(JSON.stringify(PRESETS[n] || PRESETS[8]))
}

function addPeriod() {
  const last = periods.value[periods.value.length - 1]
  periods.value.push({
    period: (last?.period || 0) + 1,
    name: `第${(last?.period || 0) + 1}节`,
    startTime: '',
    endTime: '',
  })
}

function removePeriod(idx: number) {
  periods.value.splice(idx, 1)
  periods.value.forEach((p, i) => { p.period = i + 1 })
}

async function save() {
  if (!props.semesterId) return
  saving.value = true
  try {
    const config = { semesterId: props.semesterId, periodsPerDay: periods.value.length, scheduleDays: scheduleDays.value, periods: periods.value }
    await scheduleConfigApi.save(config)
    ElMessage.success('节次配置已保存')
    emit('saved', config)
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

// Init from props
watch(() => props.config, (cfg) => {
  if (cfg) {
    scheduleDays.value = cfg.scheduleDays || [1, 2, 3, 4, 5]
    periods.value = JSON.parse(JSON.stringify(cfg.periods || PRESETS[8]))
  } else {
    scheduleDays.value = [1, 2, 3, 4, 5]
    periods.value = JSON.parse(JSON.stringify(PRESETS[8]))
  }
}, { immediate: true })
</script>

<style scoped>
.setup-step-title { font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 6px; }
.setup-step-desc { font-size: 13px; color: #6b7280; margin: 0 0 20px; }
</style>
