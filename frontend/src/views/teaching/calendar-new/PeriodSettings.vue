<template>
  <div>
    <h3 style="font-size: 16px; font-weight: 700; color: #111827; margin: 0 0 6px;">作息表配置</h3>
    <p style="font-size: 13px; color: #6b7280; margin: 0 0 20px;">配置本学期每天的完整作息安排，包括课节和活动（晨读、早操、午休等）。</p>

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
      <div style="display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; align-items: center;">
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(6)">6节制</button>
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(8)">标准8节制</button>
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset(10)">10节制</button>
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 6px 12px;" @click="applyPreset('full')">完整作息</button>
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
        <colgroup>
          <col style="width: 70px" />
          <col />
          <col style="width: 110px" />
          <col style="width: 110px" />
          <col style="width: 60px" />
        </colgroup>
        <thead><tr><th>类型</th><th class="text-left">名称</th><th>开始</th><th>结束</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in periods" :key="i" :style="{ background: p.type === 'activity' ? '#fafbfc' : '' }">
            <td>
              <span v-if="p.type === 'activity'" class="type-chip type-activity">活动</span>
              <span v-else class="type-chip type-class">第{{ p.period }}节</span>
            </td>
            <td class="text-left"><input v-model="p.name" class="tm-input" style="padding: 4px 8px; font-size: 12px;" /></td>
            <td><input v-model="p.startTime" type="time" class="tm-input" style="padding: 4px 8px; text-align: center; font-size: 12px;" /></td>
            <td>
              <input v-if="p.type !== 'activity' || p.endTime" v-model="p.endTime" type="time" class="tm-input" style="padding: 4px 8px; text-align: center; font-size: 12px;" />
              <button v-else class="tm-action" style="font-size: 11px; color: #9ca3af;" @click="p.endTime = p.startTime">+结束</button>
            </td>
            <td><button class="tm-action tm-action-danger" @click="removeItem(i)">删</button></td>
          </tr>
        </tbody>
      </table>

      <div style="display: flex; gap: 8px; flex-wrap: wrap;">
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 5px 12px;" @click="addPeriod">+ 课节</button>
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 5px 12px; border-style: dashed;" @click="addActivity">+ 活动</button>
        <button class="tm-btn tm-btn-secondary" style="font-size: 12px; padding: 5px 12px;" @click="sortByTime">按时间排序</button>
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

const CLASS_PRESETS: Record<string, any[]> = {
  '6': [
    { type: 'class', period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
    { type: 'class', period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { type: 'class', period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
    { type: 'class', period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { type: 'class', period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
    { type: 'class', period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
  ],
  '8': [
    { type: 'class', period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
    { type: 'class', period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { type: 'class', period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
    { type: 'class', period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { type: 'class', period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
    { type: 'class', period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
    { type: 'class', period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },
    { type: 'class', period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
  ],
  '10': [
    { type: 'class', period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
    { type: 'class', period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { type: 'class', period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
    { type: 'class', period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { type: 'class', period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
    { type: 'class', period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
    { type: 'class', period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },
    { type: 'class', period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
    { type: 'class', period: 9, name: '第九节', startTime: '19:00', endTime: '19:45' },
    { type: 'class', period: 10, name: '第十节', startTime: '19:55', endTime: '20:40' },
  ],
  'full': [
    { type: 'activity', name: '晨读', startTime: '06:50', endTime: '07:20' },
    { type: 'activity', name: '早操', startTime: '07:20', endTime: '07:50' },
    { type: 'class', period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
    { type: 'class', period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { type: 'activity', name: '课间操', startTime: '09:40', endTime: '10:00' },
    { type: 'class', period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
    { type: 'class', period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { type: 'activity', name: '午餐', startTime: '11:40', endTime: '12:20' },
    { type: 'activity', name: '午休', startTime: '12:20', endTime: '13:50' },
    { type: 'class', period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
    { type: 'class', period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
    { type: 'class', period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },
    { type: 'class', period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
    { type: 'activity', name: '晚餐', startTime: '17:40', endTime: '18:30' },
    { type: 'activity', name: '晚自习', startTime: '19:00', endTime: '21:00' },
    { type: 'activity', name: '熄灯', startTime: '22:00', endTime: '' },
  ],
}

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
      const raw = typeof c.periods === 'string' ? JSON.parse(c.periods) : c.periods || []
      // Migrate old format: add type='class' if missing
      periods.value = raw.map((p: any) => ({ type: 'class', ...p }))
    } else { config.value = null }
  } catch { config.value = null } finally { loading.value = false }
}

async function createDefault() {
  if (!props.semesterId) return
  try {
    await periodConfigApi.create({
      semesterId: props.semesterId, configName: '默认作息表',
      periodsPerDay: 8, scheduleDays: [1,2,3,4,5],
      periods: CLASS_PRESETS['8'],
    })
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

function applyPreset(key: string | number) {
  const preset = CLASS_PRESETS[String(key)]
  if (preset) periods.value = JSON.parse(JSON.stringify(preset))
}

function addPeriod() {
  const classItems = periods.value.filter(p => p.type !== 'activity')
  const nextNum = classItems.length > 0 ? Math.max(...classItems.map(p => p.period || 0)) + 1 : 1
  periods.value.push({ type: 'class', period: nextNum, name: `第${nextNum}节`, startTime: '', endTime: '' })
}

function addActivity() {
  periods.value.push({ type: 'activity', name: '', startTime: '', endTime: '' })
}

function removeItem(i: number) {
  periods.value.splice(i, 1)
  reindexClasses()
}

function reindexClasses() {
  let classNum = 1
  periods.value.forEach(p => {
    if (p.type !== 'activity') { p.period = classNum++ }
  })
}

function sortByTime() {
  periods.value.sort((a, b) => (a.startTime || '99:99').localeCompare(b.startTime || '99:99'))
  reindexClasses()
}

async function save() {
  if (!configId.value) return
  saving.value = true
  const classCount = periods.value.filter(p => p.type !== 'activity').length
  try {
    await periodConfigApi.update(configId.value, {
      configName: '默认作息表',
      periodsPerDay: classCount,
      scheduleDays: scheduleDays.value,
      periods: periods.value,
    })
    ElMessage.success('已保存')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

watch(() => props.semesterId, () => loadConfig(), { immediate: true })
</script>

<style scoped>
.type-chip {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
.type-class { background: #eff6ff; color: #2563eb; }
.type-activity { background: #f3f4f6; color: #6b7280; }
</style>
