<template>
  <div>
    <!-- Controls -->
    <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 16px;">
      <div class="tm-radios" style="width: auto;">
        <label :class="['tm-radio', { active: groupBy === 'teacher' }]" @click="groupBy = 'teacher'; load()"><input type="radio" />按教师</label>
        <label :class="['tm-radio', { active: groupBy === 'class' }]" @click="groupBy = 'class'; load()"><input type="radio" />按班级</label>
        <label :class="['tm-radio', { active: groupBy === 'course' }]" @click="groupBy = 'course'; load()"><input type="radio" />按课程</label>
        <label :class="['tm-radio', { active: groupBy === 'classroom' }]" @click="groupBy = 'classroom'; load()"><input type="radio" />按场所</label>
      </div>
      <i style="display: inline-block; width: 1px; height: 18px; background: #d1d5db;" />
      <div class="tm-radios" style="width: auto;">
        <label :class="['tm-radio', { active: period === 'all' }]" @click="period = 'all'; load()"><input type="radio" />全学期</label>
        <label :class="['tm-radio', { active: period === 'month' }]" @click="period = 'month'; load()"><input type="radio" />按月</label>
        <label :class="['tm-radio', { active: period === 'week' }]" @click="period = 'week'; load()"><input type="radio" />按周</label>
      </div>
      <select v-if="period === 'month'" v-model="monthVal" class="tm-select" @change="load">
        <option v-for="m in 12" :key="m" :value="m">{{ m }}月</option>
      </select>
      <select v-if="period === 'week'" v-model="weekVal" class="tm-select" @change="load">
        <option v-for="w in 20" :key="w" :value="w">第{{ w }}周</option>
      </select>
    </div>

    <!-- Summary -->
    <div v-if="summary" style="display: flex; gap: 12px; margin-bottom: 16px;">
      <div class="stat-card"><div class="stat-num">{{ summary.count }}</div><div class="stat-label">{{ groupLabel }}数</div></div>
      <div class="stat-card"><div class="stat-num">{{ summary.totalActualHours }}</div><div class="stat-label">总实际课时</div></div>
    </div>

    <!-- Table -->
    <div v-if="loading" style="text-align: center; padding: 40px; color: #9ca3af;">加载中...</div>
    <table v-else-if="items.length > 0" class="tm-table">
      <colgroup>
        <col /><col style="width: 80px" /><col style="width: 80px" /><col style="width: 80px" /><col style="width: 80px" /><col style="width: 80px" /><col style="width: 80px" />
      </colgroup>
      <thead>
        <tr>
          <th class="text-left">{{ groupLabel }}</th>
          <th>总课时</th>
          <th>实际</th>
          <th>正常</th>
          <th>取消</th>
          <th>补课</th>
          <th>代课</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.groupId">
          <td class="text-left" style="font-weight: 500;">{{ item.name }}</td>
          <td class="tm-mono">{{ item.totalHours }}</td>
          <td class="tm-mono" style="font-weight: 600; color: #111827;">{{ item.actualHours }}</td>
          <td class="tm-mono">{{ item.normalHours }}</td>
          <td class="tm-mono" style="color: #dc2626;">{{ item.cancelledHours }}</td>
          <td class="tm-mono" style="color: #2563eb;">{{ item.substituteHours }}</td>
          <td class="tm-mono" style="color: #d97706;">{{ item.proxyHours }}</td>
        </tr>
      </tbody>
    </table>
    <div v-else style="text-align: center; padding: 40px; color: #9ca3af;">暂无课时统计数据（请先生成实况课表）</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { hoursApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()

const groupBy = ref<'teacher' | 'class' | 'course' | 'classroom'>('teacher')
const period = ref<'all' | 'month' | 'week'>('all')
const monthVal = ref(new Date().getMonth() + 1)
const weekVal = ref(1)
const items = ref<any[]>([])
const summary = ref<any>(null)
const loading = ref(false)

const groupLabel = computed(() => ({ teacher: '教师', class: '班级', course: '课程', classroom: '场所' }[groupBy.value]))

async function load() {
  if (!props.semesterId) return
  loading.value = true
  try {
    const params: any = { semesterId: props.semesterId, groupBy: groupBy.value }
    if (period.value === 'month') { params.period = 'month'; params.month = monthVal.value }
    if (period.value === 'week') { params.period = 'week'; params.weekNumber = weekVal.value }
    const res = await hoursApi.getStatistics(params)
    const data = (res as any).data || res
    items.value = data.items || []
    summary.value = data.summary || null
  } catch { items.value = []; summary.value = null } finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.stat-card { border: 1px solid #e5e7eb; border-radius: 10px; background: #f9fafb; padding: 14px 20px; text-align: center; min-width: 120px; }
.stat-num { font-size: 24px; font-weight: 700; color: #111827; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 2px; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
