<template>
  <div>
    <!-- Progress bar -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 20px; margin-bottom: 16px;">
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px;">
        <span style="font-size: 13px; font-weight: 600; color: #374151;">排课进度</span>
        <span style="font-size: 13px; font-weight: 600; color: #2563eb;">{{ overviewProgress }}%</span>
      </div>
      <div style="height: 8px; width: 100%; background: #f3f4f6; border-radius: 99px; overflow: hidden; margin-bottom: 14px;">
        <div style="height: 100%; background: #3b82f6; border-radius: 99px; transition: width 0.5s;" :style="{ width: overviewProgress + '%' }" />
      </div>
      <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;">
        <div class="ov-card"><div class="ov-num">{{ taskStats.total }}</div><div class="ov-label">教学任务</div></div>
        <div class="ov-card ov-green"><div class="ov-num" style="color:#16a34a;">{{ taskStats.scheduled }}</div><div class="ov-label">已排完</div></div>
        <div class="ov-card ov-amber"><div class="ov-num" style="color:#d97706;">{{ taskStats.partial }}</div><div class="ov-label">部分排</div></div>
        <div class="ov-card ov-red"><div class="ov-num" style="color:#dc2626;">{{ taskStats.unscheduled }}</div><div class="ov-label">未排课</div></div>
      </div>
    </div>

    <!-- Action buttons -->
    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px;">
      <button class="tm-btn tm-btn-primary" @click="handleAutoSchedule">智能排课</button>
      <button class="tm-btn tm-btn-secondary" @click="handleFeasibilityCheck">可行性检测</button>
      <button class="tm-btn tm-btn-secondary" @click="handlePublish">发布课表</button>
    </div>

    <!-- Teaching task table -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
      <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 20px;">
        <h2 style="font-size: 13px; font-weight: 600; color: #374151; margin: 0;">教学任务列表</h2>
        <div style="display: flex; align-items: center; gap: 8px;">
          <div class="tm-search" style="flex: 0 0 180px;">
            <svg class="tm-search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none"><circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.5"/><path d="m10 10 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            <input v-model="taskFilter.keyword" class="tm-search-input" placeholder="搜索课程/教师" @keyup.enter="loadTasks" />
          </div>
          <select v-model="taskFilter.status" class="tm-select" @change="loadTasks">
            <option :value="undefined">全部状态</option>
            <option :value="0">待分配</option>
            <option :value="1">已分配</option>
            <option :value="2">已排课</option>
            <option :value="3">进行中</option>
          </select>
        </div>
      </div>
      <table class="tm-table" style="border-radius: 0; border-left: none; border-right: none; border-bottom: none;">
        <colgroup>
          <col style="width: 14%" />
          <col style="width: 18%" />
          <col style="width: 12%" />
          <col style="width: 8%" />
          <col style="width: 10%" />
          <col style="width: 8%" />
          <col style="width: 8%" />
        </colgroup>
        <thead>
          <tr>
            <th class="text-left">教学班</th>
            <th class="text-left">课程</th>
            <th class="text-left">教师</th>
            <th>周课时</th>
            <th>已排/总需</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="taskLoading">
            <td colspan="7" class="tm-empty"><span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...</td>
          </tr>
          <tr v-else-if="tasks.length === 0">
            <td colspan="7" class="tm-empty">暂无教学任务</td>
          </tr>
          <tr v-for="row in tasks" :key="row.id">
            <td class="text-left">{{ row.className }}</td>
            <td class="text-left">{{ row.courseName }}</td>
            <td class="text-left">{{ getMainTeacher(row) || '-' }}</td>
            <td class="tm-mono">{{ row.weeklyHours }}</td>
            <td class="tm-mono">{{ getScheduledHours(row) }}/{{ row.weeklyHours }}</td>
            <td>
              <span :class="['tm-chip', { 0:'tm-chip-gray', 1:'tm-chip-amber', 2:'tm-chip-green', 3:'tm-chip-blue', 4:'tm-chip-red' }[row.status] || 'tm-chip-gray']">
                {{ getTaskStatusName(row.status) }}
              </span>
            </td>
            <td><button class="tm-action" style="color: #2563eb;" @click="showManualEntryDialog(row)">手动排</button></td>
          </tr>
        </tbody>
      </table>
      <div v-if="taskPagination.total > taskPagination.size" class="tm-pagination">
        <span class="tm-page-info">共 {{ taskPagination.total }} 条</span>
        <div class="tm-page-controls">
          <select v-model="taskPagination.size" class="tm-page-select" @change="taskPagination.page = 1; loadTasks()">
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
          <button class="tm-page-btn" :disabled="taskPagination.page <= 1" @click="taskPagination.page--; loadTasks()">‹</button>
          <span class="tm-page-current">{{ taskPagination.page }}</span>
          <button class="tm-page-btn" :disabled="taskPagination.page * taskPagination.size >= taskPagination.total" @click="taskPagination.page++; loadTasks()">›</button>
        </div>
      </div>
    </div>

    <!-- Auto Schedule Drawer -->
    <Transition name="tm-drawer">
      <div v-if="autoScheduleDialogVisible" class="tm-drawer-overlay" @click.self="!scheduling && (autoScheduleDialogVisible = false)">
        <div class="tm-drawer" style="width: 480px;">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">智能排课</h3>
            <button v-if="!scheduling" class="tm-drawer-close" @click="autoScheduleDialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <div style="padding:10px 14px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:7px;font-size:12.5px;color:#1d4ed8;margin-bottom:16px;">
                系统将基于约束规则自动排课。已手动排定的课程不会被调整。
              </div>
              <div class="tm-field">
                <label class="tm-label">排课方案</label>
                <select v-model="autoScheduleParams.scheduleId" class="tm-field-select">
                  <option v-for="s in scheduleList" :key="s.id" :value="s.id">{{ s.name }}</option>
                </select>
              </div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">最大迭代</label>
                  <input v-model.number="autoScheduleParams.maxIterations" type="number" min="100" max="5000" step="100" class="tm-input" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">种群大小</label>
                  <input v-model.number="autoScheduleParams.populationSize" type="number" min="10" max="100" step="10" class="tm-input" />
                </div>
              </div>
              <div class="tm-field">
                <label class="tm-label">变异率: {{ autoScheduleParams.mutationRate }}</label>
                <input v-model.number="autoScheduleParams.mutationRate" type="range" min="0.01" max="0.5" step="0.01" style="width: 100%;" />
              </div>
            </div>
            <div v-if="scheduling" class="tm-section">
              <p style="font-size: 13px; color: #6b7280; margin-bottom: 8px;">正在排课...</p>
              <div style="height: 6px; background: #f3f4f6; border-radius: 99px; overflow: hidden;">
                <div style="height: 100%; background: #3b82f6; border-radius: 99px; transition: width 0.3s;" :style="{ width: autoProgress + '%' }" />
              </div>
            </div>
            <div v-if="autoResult" class="tm-section" :style="{ background: autoResult.success ? '#f0fdf4' : '#fef2f2' }">
              <p style="font-weight: 600; font-size: 13px;" :style="{ color: autoResult.success ? '#16a34a' : '#dc2626' }">
                {{ autoResult.success ? '排课完成' : '排课失败' }}
              </p>
              <p style="font-size: 12px; color: #6b7280; margin-top: 4px;">生成: {{ autoResult.entriesGenerated || 0 }} 条 | 耗时: {{ ((autoResult.executionTime || 0) / 1000).toFixed(1) }}s</p>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" :disabled="scheduling" @click="autoScheduleDialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="scheduling" @click="runAutoSchedule">{{ scheduling ? '排课中...' : '开始排课' }}</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Manual Entry Drawer -->
    <Transition name="tm-drawer">
      <div v-if="entryDialogVisible" class="tm-drawer-overlay" @click.self="entryDialogVisible = false">
        <div class="tm-drawer">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">{{ entryForm.id ? '编辑排课' : '手动排课' }}</h3>
            <button class="tm-drawer-close" @click="entryDialogVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <h4 class="tm-section-title">排课信息</h4>
              <div class="tm-field">
                <label class="tm-label">教学任务</label>
                <input :value="taskOptionsForEntry[0] ? `${taskOptionsForEntry[0].courseName} - ${taskOptionsForEntry[0].className}` : ''" class="tm-input" disabled />
              </div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">星期 <span class="req">*</span></label>
                  <select v-model="entryForm.dayOfWeek" class="tm-field-select">
                    <option :value="undefined" disabled>选择</option>
                    <option v-for="d in weekdays" :key="d.value" :value="d.value">{{ d.label }}</option>
                  </select>
                </div>
                <div class="tm-field">
                  <label class="tm-label">教室 <span class="req">*</span></label>
                  <select v-model="entryForm.classroomId" class="tm-field-select">
                    <option :value="undefined" disabled>选择教室</option>
                    <option v-for="c in classrooms" :key="c.id" :value="c.id">{{ c.name }}</option>
                  </select>
                </div>
              </div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">开始节次 <span class="req">*</span></label>
                  <select v-model="entryForm.periodStart" class="tm-field-select">
                    <option :value="undefined" disabled>选择</option>
                    <option v-for="p in periods" :key="p.period" :value="p.period">{{ p.name }}</option>
                  </select>
                </div>
                <div class="tm-field">
                  <label class="tm-label">结束节次 <span class="req">*</span></label>
                  <select v-model="entryForm.periodEnd" class="tm-field-select">
                    <option :value="undefined" disabled>选择</option>
                    <option v-for="p in periods" :key="p.period" :value="p.period">{{ p.name }}</option>
                  </select>
                </div>
              </div>
              <div class="tm-fields tm-cols-3">
                <div class="tm-field">
                  <label class="tm-label">开始周</label>
                  <input v-model.number="entryForm.weekStart" type="number" min="1" max="20" class="tm-input" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">结束周</label>
                  <input v-model.number="entryForm.weekEnd" type="number" min="1" max="20" class="tm-input" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">周类型</label>
                  <select v-model="entryForm.weekType" class="tm-field-select">
                    <option :value="0">每周</option>
                    <option :value="1">单周</option>
                    <option :value="2">双周</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="entryDialogVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="entrySaving" @click="saveEntry">{{ entrySaving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http as request } from '@/utils/request'
import { scheduleApi, teachingTaskApi, conflictApi } from '@/api/teaching'
import type { CourseSchedule, ScheduleEntry, TeachingTask } from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()

const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS

const tasks = ref<TeachingTask[]>([])
const taskLoading = ref(false)
const taskFilter = reactive({ keyword: '', status: undefined as number | undefined })
const taskPagination = reactive({ page: 1, size: 20, total: 0 })

const scheduleList = ref<CourseSchedule[]>([])
const classrooms = ref<{ id: number; name: string }[]>([])

const taskStats = computed(() => {
  const total = taskPagination.total
  const scheduled = tasks.value.filter(t => t.status >= 2).length
  const partial = tasks.value.filter(t => t.status === 1).length
  const unscheduled = tasks.value.filter(t => t.status === 0).length
  return { total, scheduled, partial, unscheduled }
})

const overviewProgress = computed(() => taskStats.value.total === 0 ? 0 : Math.round((taskStats.value.scheduled / taskStats.value.total) * 100))

const autoScheduleDialogVisible = ref(false)
const scheduling = ref(false)
const autoProgress = ref(0)
const autoResult = ref<any>(null)
const autoScheduleParams = ref({ scheduleId: undefined as number | string | undefined, maxIterations: 1000, populationSize: 100, mutationRate: 0.1 })

const entryDialogVisible = ref(false)
const entrySaving = ref(false)
const entryForm = ref<Partial<ScheduleEntry>>({})
const taskOptionsForEntry = ref<{ id: number | string; courseName: string; className: string }[]>([])

async function loadTasks() {
  if (!props.semesterId) return
  taskLoading.value = true
  try {
    const res = await teachingTaskApi.list({ semesterId: props.semesterId, status: taskFilter.status, page: taskPagination.page, size: taskPagination.size })
    const data = (res as any).data || res
    if (data.records) { tasks.value = data.records; taskPagination.total = data.total || 0 }
    else if (Array.isArray(data)) { tasks.value = data; taskPagination.total = data.length }
  } catch { /* */ } finally { taskLoading.value = false }
}

async function loadScheduleList() {
  if (!props.semesterId) return
  try { const res = await scheduleApi.list({ semesterId: props.semesterId }); scheduleList.value = (res as any).data || res; if (!Array.isArray(scheduleList.value)) scheduleList.value = [] } catch { /* */ }
}

async function loadClassrooms() {
  try {
    const res = await request.get('/places', { params: { roomType: 'CLASSROOM' } })
    const data = (res as any).data || res; const items = Array.isArray(data) ? data : data.records || []
    classrooms.value = items.map((p: any) => ({ id: p.id, name: p.placeName || p.name }))
  } catch { /* */ }
}

function handleAutoSchedule() {
  if (scheduleList.value.length === 0) { ElMessage.warning('当前学期暂无排课方案'); return }
  autoScheduleParams.value = { scheduleId: scheduleList.value[0].id, maxIterations: 500, populationSize: 30, mutationRate: 0.1 }
  autoProgress.value = 0; autoResult.value = null; autoScheduleDialogVisible.value = true
}

async function runAutoSchedule() {
  if (!props.semesterId) { ElMessage.warning('请先选择学期'); return }
  scheduling.value = true; autoProgress.value = 0; autoResult.value = null
  const timer = setInterval(() => { if (autoProgress.value < 90) autoProgress.value += Math.random() * 15 }, 500)
  try {
    const result = await scheduleApi.autoSchedule({ semesterId: props.semesterId, scheduleId: autoScheduleParams.value.scheduleId, maxIterations: autoScheduleParams.value.maxIterations, populationSize: autoScheduleParams.value.populationSize, mutationRate: autoScheduleParams.value.mutationRate })
    autoProgress.value = 100; const data = (result as any).data || result; autoResult.value = data
    if (data.success) { ElMessage.success(`排课完成！共生成 ${data.entriesGenerated} 条记录`); loadTasks(); loadScheduleList() } else { ElMessage.error('排课失败') }
  } catch (e: any) { ElMessage.error('排课失败: ' + (e.message || '请检查约束配置')) } finally { clearInterval(timer); scheduling.value = false }
}

async function handleFeasibilityCheck() {
  if (!props.semesterId) return
  try { await conflictApi.feasibilityCheck(props.semesterId); ElMessage.success('可行性检测完成') } catch { ElMessage.error('可行性检测失败') }
}

async function handlePublish() {
  const draftSchedules = scheduleList.value.filter(s => s.status === 0)
  if (draftSchedules.length === 0) { ElMessage.info('暂无待发布的排课方案'); return }
  await ElMessageBox.confirm(`将发布 ${draftSchedules.length} 个排课方案，确定发布吗？`, '发布确认', { type: 'warning' })
  try { for (const s of draftSchedules) await scheduleApi.publish(s.id); ElMessage.success('发布成功'); loadScheduleList() } catch { ElMessage.error('发布失败') }
}

function getMainTeacher(task: TeachingTask) { return task.teachers?.length ? (task.teachers.find(t => t.isMain)?.teacherName || task.teachers[0]?.teacherName || '') : '' }
function getScheduledHours(task: TeachingTask) { if (task.status >= 2) return task.weeklyHours; if (task.status === 1) return Math.floor(task.weeklyHours / 2); return 0 }
function getTaskStatusName(s: number) { return ({ 0: '待分配', 1: '已分配', 2: '已排课', 3: '进行中', 4: '已结束' } as any)[s] || '未知' }

function showManualEntryDialog(task: TeachingTask) {
  if (scheduleList.value.length === 0) { ElMessage.warning('请先创建排课方案'); return }
  taskOptionsForEntry.value = [{ id: task.id, courseName: task.courseName || '', className: task.className || '' }]
  entryForm.value = { scheduleId: scheduleList.value[0].id, taskId: task.id, weekStart: task.startWeek || 1, weekEnd: task.endWeek || 16, weekType: 0 }
  entryDialogVisible.value = true
}

async function saveEntry() {
  if (!entryForm.value.scheduleId || !entryForm.value.dayOfWeek || !entryForm.value.classroomId || !entryForm.value.periodStart || !entryForm.value.periodEnd) { ElMessage.warning('请填写完整信息'); return }
  entrySaving.value = true
  try {
    if (entryForm.value.id) await scheduleApi.updateEntry(entryForm.value.scheduleId, entryForm.value.id, entryForm.value)
    else await scheduleApi.addEntry(entryForm.value.scheduleId, entryForm.value)
    ElMessage.success('保存成功'); entryDialogVisible.value = false; loadTasks()
  } catch { ElMessage.error('保存失败') } finally { entrySaving.value = false }
}

watch(() => props.semesterId, () => { loadTasks(); loadScheduleList(); loadClassrooms() }, { immediate: true })
defineExpose({ loadTasks })
</script>

<style scoped>
.ov-card { border: 1px solid #e5e7eb; border-radius: 8px; background: #f9fafb; padding: 10px 16px; text-align: center; }
.ov-card.ov-green { border-color: #bbf7d0; background: #f0fdf4; }
.ov-card.ov-amber { border-color: #fde68a; background: #fffbeb; }
.ov-card.ov-red { border-color: #fecaca; background: #fef2f2; }
.ov-num { font-size: 20px; font-weight: 700; color: #111827; }
.ov-label { font-size: 11px; color: #6b7280; margin-top: 2px; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
