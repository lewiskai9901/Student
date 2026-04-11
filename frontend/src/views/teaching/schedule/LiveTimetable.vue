<template>
  <div>
    <!-- Controls -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 14px 20px; margin-bottom: 16px;">
      <div style="display: flex; flex-wrap: wrap; align-items: center; gap: 10px;">
        <div class="tm-radios" style="width: auto;">
          <button :class="['tm-radio', { active: viewType === 'class' }]" @click="viewType = 'class'; targetId = undefined; instances = []">班级</button>
          <button :class="['tm-radio', { active: viewType === 'teacher' }]" @click="viewType = 'teacher'; targetId = undefined; instances = []">教师</button>
          <button :class="['tm-radio', { active: viewType === 'classroom' }]" @click="viewType = 'classroom'; targetId = undefined; instances = []">场所</button>
        </div>
        <select v-model="targetId" class="tm-select" @change="loadInstances">
          <option :value="undefined" disabled>{{ viewType === 'class' ? '选择班级' : viewType === 'teacher' ? '选择教师' : '选择教室' }}</option>
          <option v-for="o in targetOptions" :key="o.id" :value="o.id">{{ o.name }}</option>
        </select>
        <span style="display: inline-block; width: 1px; height: 18px; background: #d1d5db;" />

        <!-- Week pager -->
        <div class="tv-week-pager">
          <button class="tv-week-btn" :disabled="!weekNumber || weekNumber <= 1" @click="weekNumber && weekNumber--; loadInstances()">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6"/></svg>
          </button>
          <span class="tv-week-label" @click="weekNumber = undefined; loadInstances()">{{ weekNumber ? `第${weekNumber}周` : '全部周次' }}</span>
          <button class="tv-week-btn" :disabled="weekNumber !== undefined && weekNumber >= 20" @click="weekNumber = (weekNumber ?? 0) + 1; loadInstances()">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
        </div>

        <span style="display: inline-block; width: 1px; height: 18px; background: #d1d5db;" />

        <!-- View toggle: grid / list -->
        <div class="view-toggle">
          <button :class="['vt-btn', displayMode === 'grid' && 'vt-active']" @click="displayMode = 'grid'" title="课表视图">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
          </button>
          <button :class="['vt-btn', displayMode === 'list' && 'vt-active']" @click="displayMode = 'list'" title="列表视图">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Legend -->
    <div style="display: flex; gap: 16px; margin-bottom: 12px; font-size: 12px; color: #6b7280;">
      <span><span class="legend-dot" style="background:#dcfce7;border-color:#bbf7d0;" />正常</span>
      <span><span class="legend-dot" style="background:#fecaca;border-color:#fca5a5;" />已取消</span>
      <span><span class="legend-dot" style="background:#bbf7d0;border-color:#86efac;" />补课</span>
      <span><span class="legend-dot" style="background:#fed7aa;border-color:#fdba74;" />代课</span>
      <span><span class="legend-dot" style="background:#e5e7eb;border-color:#d1d5db;" />已调走</span>
    </div>

    <!-- Loading / Empty -->
    <div v-if="loading" style="text-align: center; padding: 40px; color: #9ca3af;">加载中...</div>
    <div v-else-if="instances.length === 0" style="text-align: center; padding: 40px; color: #9ca3af;">
      {{ targetId ? '暂无实况数据' : '请选择查看对象' }}
    </div>

    <!-- Grid View -->
    <template v-else-if="displayMode === 'grid'">
      <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
        <TimetableGrid
          :entries="gridEntries"
          :periods="periods"
          :editable="false"
          @entry-click="showDetail"
        />
      </div>
      <div class="tm-stats" style="margin-top: 10px;">
        <span>共 <b>{{ instances.length }}</b> 条</span>
        <span class="sep" />
        <span>正常 <b>{{ instances.filter(i => i.status === 0).length }}</b></span>
        <span class="sep" />
        <span style="color:#dc2626;">取消 <b>{{ instances.filter(i => i.status === 1).length }}</b></span>
      </div>
    </template>

    <!-- List View -->
    <template v-else>
      <table class="tm-table">
        <thead>
          <tr>
            <th style="width:90px">日期</th>
            <th style="width:50px">周次</th>
            <th class="text-left">课程</th>
            <th style="width:100px">班级</th>
            <th style="width:70px">节次</th>
            <th style="width:80px">教室</th>
            <th style="width:70px">教师</th>
            <th style="width:60px">状态</th>
            <th class="text-left">备注</th>
            <th style="width:90px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="inst in instances" :key="inst.id" :style="{ background: statusBg(inst.status) }">
            <td style="font-size: 12px;">{{ inst.actualDate }}</td>
            <td class="tm-mono">{{ inst.weekNumber }}</td>
            <td class="text-left" style="font-weight: 500;">{{ inst.courseName }}</td>
            <td style="font-size: 12px;">{{ inst.className }}</td>
            <td class="tm-mono">{{ inst.startSlot }}-{{ inst.endSlot }}节</td>
            <td style="font-size: 12px;">{{ inst.classroomName || '-' }}</td>
            <td>{{ inst.teacherName || '-' }}</td>
            <td><span :class="['tm-chip', statusChip(inst.status)]">{{ statusName(inst.status) }}</span></td>
            <td class="text-left" style="font-size: 12px; color: #6b7280;">{{ inst.cancelReason || '' }}</td>
            <td>
              <template v-if="inst.status === 0">
                <button class="tm-action tm-action-danger" style="font-size:11px" @click="handleCancel(inst)">取消</button>
              </template>
              <template v-else-if="inst.status === 1">
                <button class="tm-action" style="font-size:11px;color:#2563eb" @click="handleRestore(inst)">恢复</button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http as request } from '@/utils/request'
import { instanceApi, periodConfigApi } from '@/api/teaching'
import type { PeriodConfig, ScheduleEntry } from '@/types/teaching'
import { DEFAULT_PERIODS } from '@/types/teaching'
import TimetableGrid from '../scheduling/TimetableGrid.vue'

const props = defineProps<{ semesterId: number | string | undefined }>()

const viewType = ref<'class' | 'teacher' | 'classroom'>('class')
const targetId = ref<number | string>()
const weekNumber = ref<number | undefined>()
const displayMode = ref<'grid' | 'list'>('grid')
const instances = ref<any[]>([])
const loading = ref(false)
const periods = ref<PeriodConfig[]>(DEFAULT_PERIODS)

const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])
const classroomList = ref<{ id: number; name: string }[]>([])

const targetOptions = computed(() => viewType.value === 'class' ? classList.value : viewType.value === 'teacher' ? teacherList.value : classroomList.value)

// Map instances to TimetableGrid entries format
const gridEntries = computed<ScheduleEntry[]>(() => {
  // Deduplicate: group by weekday+startSlot+endSlot (collapse same course across weeks)
  const map = new Map<string, any>()
  for (const inst of instances.value) {
    if (inst.status === 1) continue // skip cancelled
    const key = `${inst.weekday}-${inst.startSlot}-${inst.endSlot}-${inst.courseName}`
    if (!map.has(key)) {
      map.set(key, {
        id: inst.id,
        taskId: inst.entryId || inst.id,
        dayOfWeek: inst.weekday,
        periodStart: inst.startSlot,
        periodEnd: inst.endSlot,
        courseName: inst.courseName,
        teacherName: inst.teacherName || '',
        classroomName: inst.classroomName || '',
        weekStart: inst.weekNumber,
        weekEnd: inst.weekNumber,
        weekType: 0,
      })
    } else {
      // Extend week range
      const existing = map.get(key)!
      existing.weekEnd = Math.max(existing.weekEnd, inst.weekNumber)
      existing.weekStart = Math.min(existing.weekStart, inst.weekNumber)
    }
  }
  return Array.from(map.values())
})

async function loadInstances() {
  if (!props.semesterId || !targetId.value) { instances.value = []; return }
  loading.value = true
  try {
    const params: any = { semesterId: props.semesterId }
    if (weekNumber.value) params.weekNumber = weekNumber.value
    if (viewType.value === 'class') params.orgUnitId = targetId.value
    else if (viewType.value === 'teacher') params.teacherId = targetId.value
    else params.classroomId = targetId.value
    const res = await instanceApi.list(params)
    instances.value = (res as any).data || res || []
  } catch { instances.value = [] } finally { loading.value = false }
}

function showDetail(entry: any) {
  ElMessage.info(`${entry.courseName} | ${entry.teacherName} | ${entry.classroomName}`)
}

function statusName(s: number) { return ({ 0: '正常', 1: '取消', 2: '调走', 3: '补课', 4: '代课' } as any)[s] || '?' }
function statusChip(s: number) { return ({ 0: 'tm-chip-green', 1: 'tm-chip-red', 2: 'tm-chip-gray', 3: 'tm-chip-blue', 4: 'tm-chip-amber' } as any)[s] || 'tm-chip-gray' }
function statusBg(s: number) { return ({ 1: '#fef2f2', 2: '#f9fafb', 3: '#f0fdf4', 4: '#fffbeb' } as any)[s] || '' }

async function handleCancel(inst: any) {
  const res = await ElMessageBox.prompt('请填写取消原因', '取消课程', { inputPlaceholder: '如: 教师请假' }).catch(() => null)
  if (!res?.value) return
  try { await instanceApi.cancel(inst.id, res.value); ElMessage.success('已取消'); loadInstances() } catch { ElMessage.error('操作失败') }
}

async function handleRestore(inst: any) {
  await ElMessageBox.confirm('确定恢复此课程为正常状态？', '确认')
  try { await instanceApi.restore(inst.id); ElMessage.success('已恢复'); loadInstances() } catch { ElMessage.error('操作失败') }
}

async function loadPeriodConfig() {
  if (!props.semesterId) return
  try {
    const res: any = await periodConfigApi.list(props.semesterId)
    const configs = Array.isArray(res) ? res : (res.data || res)
    if (!Array.isArray(configs) || configs.length === 0) return
    const config = configs.find((c: any) => c.isDefault) || configs[0]
    const rawPeriods = typeof config.periods === 'string' ? JSON.parse(config.periods) : config.periods
    if (!Array.isArray(rawPeriods)) return
    const classPeriods = rawPeriods.filter((p: any) => p.type === 'class' && p.period)
    if (classPeriods.length > 0) {
      periods.value = classPeriods.map((p: any) => ({ period: p.period, name: p.name, startTime: p.startTime, endTime: p.endTime }))
    }
  } catch { /* defaults */ }
}

async function loadOptions() {
  try {
    const r = await request.get('/org-units/tree'); const d = (r as any).data || r
    const classes: { id: number; name: string }[] = []
    function walk(nodes: any[]) { for (const n of nodes) { if (n.unitType === 'CLASS') classes.push({ id: n.id, name: n.unitName }); if (n.children) walk(n.children) } }
    walk(Array.isArray(d) ? d : [])
    classList.value = classes
  } catch { classList.value = [] }

  try {
    const r = await request.get('/teaching/schedule-teachers'); const d = (r as any).data || r
    teacherList.value = (Array.isArray(d) ? d : []).map((t: any) => ({ id: t.id, name: t.realName || t.username }))
  } catch { teacherList.value = [] }

  try {
    const r = await request.get('/places', { params: { pageSize: 500 } }); const d = (r as any).data || r
    const items = Array.isArray(d) ? d : d.list || d.records || []
    classroomList.value = items.filter((p: any) => (p.capacity || 0) > 0 && (p.capacity || 0) < 1000)
      .map((p: any) => ({ id: p.id, name: p.placeCode || p.placeName || p.name }))
  } catch { classroomList.value = [] }
}

onMounted(() => { loadPeriodConfig(); loadOptions() })
</script>

<style scoped>
.legend-dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; border: 1px solid; margin-right: 4px; vertical-align: middle; }
.tv-week-pager { display: inline-flex; align-items: center; gap: 2px; border: 1px solid #d1d5db; border-radius: 6px; overflow: hidden; }
.tv-week-btn { display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; border: none; background: #fff; color: #6b7280; cursor: pointer; }
.tv-week-btn:hover:not(:disabled) { background: #f3f4f6; color: #111827; }
.tv-week-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.tv-week-label { padding: 0 10px; font-size: 12.5px; font-weight: 500; color: #374151; cursor: pointer; white-space: nowrap; min-width: 60px; text-align: center; }
.tv-week-label:hover { color: #2563eb; }
.view-toggle { display: inline-flex; border: 1px solid #d1d5db; border-radius: 5px; overflow: hidden; }
.vt-btn { display: flex; align-items: center; justify-content: center; width: 30px; height: 26px; background: #fff; border: none; color: #9ca3af; cursor: pointer; }
.vt-btn:not(:last-child) { border-right: 1px solid #d1d5db; }
.vt-btn:hover { background: #f3f4f6; color: #374151; }
.vt-active { background: #2563eb !important; color: #fff !important; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
