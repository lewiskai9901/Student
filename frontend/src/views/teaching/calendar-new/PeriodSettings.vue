<template>
  <div>
    <h3 style="font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 6px;">作息表配置</h3>
    <p style="font-size: 13px; color: #6b7280; margin: 0 0 20px;">配置本学期每天上几节课、每节课的时间。排课中心将引用此配置。</p>

    <!-- If no config exists -->
    <div v-if="!config && !loading" style="border: 1px solid #e5e7eb; border-radius: 10px; background: #f9fafb; padding: 30px; text-align: center; margin-bottom: 16px;">
      <p style="font-size: 14px; color: #374151; margin: 0 0 12px;">本学期暂无作息表</p>
      <div style="display: flex; justify-content: center; gap: 8px;">
        <button class="tm-btn tm-btn-secondary" @click="initFromPrevious">从上学期继承</button>
        <button class="tm-btn tm-btn-primary" @click="createDefault">创建默认(8节制)</button>
      </div>
    </div>

    <!-- Config Editor -->
    <template v-if="config">
      <div style="display: flex; gap: 8px; margin-bottom: 16px;">
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(6)">6节制</button>
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(8)">标准8节制</button>
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(10)">10节制</button>
      </div>

      <div class="tm-field" style="margin-bottom: 16px;">
        <label class="tm-label">排课日</label>
        <div style="display: flex; gap: 8px;">
          <label v-for="d in allDays" :key="d.value" style="display: flex; align-items: center; gap: 4px; font-size: 13px; cursor: pointer;">
            <input type="checkbox" :value="d.value" v-model="scheduleDays" /> {{ d.label }}
          </label>
        </div>
      </div>

      <table class="tm-table" style="margin-bottom: 16px;">
        <colgroup><col style="width: 60px" /><col /><col style="width: 120px" /><col style="width: 120px" /><col style="width: 60px" /></colgroup>
        <thead><tr><th>序号</th><th class="text-left">名称</th><th>开始</th><th>结束</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in periods" :key="i">
            <td class="tm-mono">{{ p.period }}</td>
            <td class="text-left"><input v-model="p.name" class="tm-input" style="padding: 4px 8px;" /></td>
            <td><input v-model="p.startTime" type="time" class="tm-input" style="padding: 4px 8px; text-align: center;" /></td>
            <td><input v-model="p.endTime" type="time" class="tm-input" style="padding: 4px 8px; text-align: center;" /></td>
            <td><button class="tm-action tm-action-danger" @click="periods.splice(i, 1); reindex()">删</button></td>
          </tr>
        </tbody>
      </table>

      <div style="display: flex; gap: 8px;">
        <button class="tm-btn tm-btn-secondary" @click="addPeriod">添加一节</button>
        <span style="flex: 1;" />
        <button class="tm-btn tm-btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存' }}</button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { periodConfigApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()

const allDays = [{ value: 1, label: '周一' },{ value: 2, label: '周二' },{ value: 3, label: '周三' },{ value: 4, label: '周四' },{ value: 5, label: '周五' },{ value: 6, label: '周六' },{ value: 7, label: '周日' }]
const PRESET_8 = [
  { period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },{ period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
  { period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },{ period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
  { period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },{ period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
  { period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },{ period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
]

const loading = ref(false)
const saving = ref(false)
const config = ref<any>(null)
const configId = ref<number>()
const scheduleDays = ref<number[]>([1,2,3,4,5])
const periods = ref<any[]>([])

async function loadConfig() {
  if (!props.semesterId) return
  loading.value = true
  try {
    const res = await periodConfigApi.list(props.semesterId)
    const list = (res as any).data || res || []
    if (list.length > 0) {
      const c = list[0]
      config.value = c
      configId.value = c.id
      scheduleDays.value = typeof c.scheduleDays === 'string' ? JSON.parse(c.scheduleDays) : c.scheduleDays || [1,2,3,4,5]
      periods.value = typeof c.periods === 'string' ? JSON.parse(c.periods) : c.periods || []
    } else { config.value = null }
  } catch { config.value = null } finally { loading.value = false }
}

async function createDefault() {
  if (!props.semesterId) return
  try {
    await periodConfigApi.create({ semesterId: props.semesterId, configName: '默认作息表', periodsPerDay: 8, scheduleDays: [1,2,3,4,5], periods: PRESET_8 })
    ElMessage.success('已创建默认作息表'); loadConfig()
  } catch { ElMessage.error('创建失败') }
}

async function initFromPrevious() {
  if (!props.semesterId) return
  try {
    const res = await periodConfigApi.initFromPrevious(props.semesterId)
    ElMessage.success((res as any).data?.message || '已继承'); loadConfig()
  } catch { ElMessage.error('继承失败，请手动创建') }
}

function applyPreset(n: number) {
  const presets: Record<number, any[]> = { 6: PRESET_8.slice(0, 6), 8: PRESET_8, 10: [...PRESET_8, { period: 9, name: '第九节', startTime: '19:00', endTime: '19:45' }, { period: 10, name: '第十节', startTime: '19:55', endTime: '20:40' }] }
  periods.value = JSON.parse(JSON.stringify(presets[n] || presets[8]))
}

function addPeriod() {
  const last = periods.value[periods.value.length - 1]
  periods.value.push({ period: (last?.period || 0) + 1, name: `第${(last?.period || 0) + 1}节`, startTime: '', endTime: '' })
}

function reindex() { periods.value.forEach((p, i) => p.period = i + 1) }

async function save() {
  if (!configId.value) return
  saving.value = true
  try {
    await periodConfigApi.update(configId.value, { configName: '默认作息表', periodsPerDay: periods.value.length, scheduleDays: scheduleDays.value, periods: periods.value })
    ElMessage.success('已保存')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

watch(() => props.semesterId, () => loadConfig(), { immediate: true })
</script>
