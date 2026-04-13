<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChevronLeft, ChevronRight, Play, Eye, Target } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { useAuthStore } from '@/stores/auth'
import { getMyTasks, getAvailableTasks, withdrawTask } from '@/api/insp/task'
import { getProjects } from '@/api/insp/project'
import type { InspTask, InspProject } from '@/types/insp/project'
import { TaskStatusConfig, type TaskStatus } from '@/types/insp/enums'

const router = useRouter()
const store = useInspExecutionStore()
const authStore = useAuthStore()

const loading = ref(false)
const myTaskList = ref<InspTask[]>([])
const availableTaskList = ref<InspTask[]>([])
const projectMap = ref<Map<number, InspProject>>(new Map())
const claimingIds = ref<Set<number>>(new Set())

const today = new Date().toISOString().slice(0, 10)
const weekOffset = ref(0)

// ── Week range ──
const weekRange = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + weekOffset.value * 7)
  const day = d.getDay()
  const monday = new Date(d)
  monday.setDate(d.getDate() - (day === 0 ? 6 : day - 1))
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  let prefix = ''
  if (weekOffset.value === 0) prefix = '本周'
  else if (weekOffset.value === -1) prefix = '上周'
  else if (weekOffset.value === 1) prefix = '下周'
  else prefix = `${monday.getMonth() + 1}月`
  return {
    start: monday.toISOString().slice(0, 10),
    end: sunday.toISOString().slice(0, 10),
    label: `${prefix} ${monday.getMonth() + 1}/${monday.getDate()} - ${sunday.getMonth() + 1}/${sunday.getDate()}`,
  }
})

// ── Week days for mini timelines ──
const weekDays = computed(() => {
  const result: { date: string; label: string; dayName: string; isToday: boolean; isPast: boolean }[] = []
  const start = new Date(weekRange.value.start)
  const dayNames = ['日', '一', '二', '三', '四', '五', '六']
  for (let i = 0; i < 7; i++) {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    const ds = d.toISOString().slice(0, 10)
    result.push({
      date: ds,
      label: `${d.getDate()}`,
      dayName: dayNames[d.getDay()],
      isToday: ds === today,
      isPast: ds < today,
    })
  }
  return result
})

// ── All tasks deduplicated ──
const allTasks = computed(() => {
  const map = new Map<number, InspTask>()
  for (const t of myTaskList.value) map.set(t.id, t)
  for (const t of availableTaskList.value) map.set(t.id, t)
  return Array.from(map.values())
})

// ── Group tasks by project ──
interface ProjectGroup {
  project: InspProject | null
  projectId: number
  projectName: string
  tasks: InspTask[]
  todayTasks: InspTask[]
  weekTaskDates: Set<string>  // dates that have tasks this week
  stats: { total: number; done: number; active: number; pending: number }
}

const projectGroups = computed((): ProjectGroup[] => {
  const grouped = new Map<number, InspTask[]>()
  for (const t of allTasks.value) {
    if (!grouped.has(t.projectId)) grouped.set(t.projectId, [])
    grouped.get(t.projectId)!.push(t)
  }

  const groups: ProjectGroup[] = []
  for (const [projectId, tasks] of grouped) {
    const project = projectMap.value.get(projectId) || null
    const todayTasks = tasks.filter(t => t.taskDate === today)
    const weekTaskDates = new Set(
      tasks.filter(t => t.taskDate >= weekRange.value.start && t.taskDate <= weekRange.value.end)
        .map(t => t.taskDate)
    )
    const done = tasks.filter(t => (['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'] as string[]).includes(t.status)).length
    const active = tasks.filter(t => (['IN_PROGRESS', 'CLAIMED'] as string[]).includes(t.status)).length
    const pending = tasks.filter(t => t.status === 'PENDING').length

    groups.push({
      project,
      projectId,
      projectName: project?.projectName || `项目 #${projectId}`,
      tasks: tasks.sort((a, b) => a.taskDate.localeCompare(b.taskDate)),
      todayTasks,
      weekTaskDates,
      stats: { total: tasks.length, done, active, pending },
    })
  }

  // Sort: has today tasks first, then by active count, then by name
  groups.sort((a, b) => {
    if (a.todayTasks.length > 0 && b.todayTasks.length === 0) return -1
    if (a.todayTasks.length === 0 && b.todayTasks.length > 0) return 1
    if (a.stats.active > 0 && b.stats.active === 0) return -1
    if (a.stats.active === 0 && b.stats.active > 0) return 1
    return 0
  })

  return groups
})

// ── Global stats ──
const globalStats = computed(() => {
  const todayCount = allTasks.value.filter(t => t.taskDate === today).length
  const weekTasks = allTasks.value.filter(t => t.taskDate >= weekRange.value.start && t.taskDate <= weekRange.value.end)
  const weekDone = weekTasks.filter(t => (['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'] as string[]).includes(t.status)).length
  const unclaimed = availableTaskList.value.filter(t => !t.inspectorId && t.status === 'PENDING').length
  return {
    todayCount,
    weekDone,
    weekTotal: weekTasks.length,
    unclaimed,
    donePercent: weekTasks.length > 0 ? Math.round((weekDone / weekTasks.length) * 100) : 0,
    projectCount: projectGroups.value.length,
  }
})

const unclaimedTasks = computed(() =>
  availableTaskList.value.filter(t => !t.inspectorId && t.status === 'PENDING')
)

// ── Get task for a specific date in a project group ──
function getTaskForDate(group: ProjectGroup, date: string): InspTask | null {
  return group.tasks.find(t => t.taskDate === date) || null
}

function getDateDotClass(group: ProjectGroup, day: { date: string; isToday: boolean; isPast: boolean }): string {
  const task = getTaskForDate(group, day.date)
  if (!task) return 'dot-empty'
  if (isTaskDone(task)) return 'dot-done'
  if (isTaskActive(task)) return 'dot-active'
  if (isTaskPending(task)) return 'dot-pending'
  return 'dot-empty'
}

// ── Load data ──
async function loadData() {
  loading.value = true
  try {
    const [myTasks, available, projects] = await Promise.all([
      getMyTasks().catch(() => [] as InspTask[]),
      getAvailableTasks().catch(() => [] as InspTask[]),
      getProjects().catch(() => [] as InspProject[]),
    ])
    myTaskList.value = myTasks
    availableTaskList.value = available
    const map = new Map<number, InspProject>()
    for (const p of projects) map.set(p.id, p)
    projectMap.value = map
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ── Actions ──
function goToTask(task: InspTask) {
  router.push(`/inspection/v7/tasks/${task.id}/execute`)
}

async function handleClaimTask(task: InspTask) {
  if (claimingIds.value.has(task.id)) return
  claimingIds.value.add(task.id)
  try {
    await store.claimTask(task.id, { inspectorName: authStore.userName || '检查员' })
    ElMessage.success('领取成功')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '领取失败')
  } finally {
    claimingIds.value.delete(task.id)
  }
}

async function handleWithdrawTask(task: InspTask) {
  try {
    await ElMessageBox.confirm('撤回后可重新修改打分，确认撤回？', '撤回确认', { type: 'info' })
    await withdrawTask(task.id)
    ElMessage.success('已撤回，可重新修改')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') ElMessage.error(e.message || '撤回失败')
  }
}

// ── Helpers ──
function isTaskDone(task: InspTask): boolean {
  return (['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'] as string[]).includes(task.status)
}
function isTaskActive(task: InspTask): boolean {
  return (['IN_PROGRESS', 'CLAIMED'] as string[]).includes(task.status)
}
function isTaskPending(task: InspTask): boolean {
  return task.status === 'PENDING'
}
function getStatusLabel(status: string): string {
  return TaskStatusConfig[status as TaskStatus]?.label || status
}
function getProgressPercent(task: InspTask): number {
  return task.totalTargets > 0
    ? Math.round(((task.completedTargets + (task.skippedTargets || 0)) / task.totalTargets) * 100)
    : 0
}

onMounted(() => loadData())
</script>

<template>
  <div class="my-insp-page">
    <!-- Top Bar -->
    <header class="topbar">
      <div class="topbar-title">我的任务</div>
      <div class="topbar-right">
        <div class="week-nav">
          <button @click="weekOffset--"><ChevronLeft class="w-3.5 h-3.5" /></button>
          <span class="week-label">{{ weekRange.label }}</span>
          <button @click="weekOffset++"><ChevronRight class="w-3.5 h-3.5" /></button>
        </div>
      </div>
    </header>

    <div class="page" v-loading="loading">
      <!-- Stats Bar -->
      <div class="stats-bar">
        <div class="stat-item">
          <span class="stat-num">{{ globalStats.projectCount }}</span>
          <span class="stat-label">个项目</span>
        </div>
        <span class="stat-sep" />
        <div class="stat-item">
          <span class="stat-num blue">{{ globalStats.todayCount }}</span>
          <span class="stat-label">今日任务</span>
        </div>
        <span class="stat-sep" />
        <div class="stat-item">
          <span class="stat-num green">{{ globalStats.weekDone }}/{{ globalStats.weekTotal }}</span>
          <span class="stat-label">本周进度</span>
        </div>
        <span class="stat-sep" />
        <div class="stat-item" v-if="globalStats.unclaimed > 0">
          <span class="stat-num orange">{{ globalStats.unclaimed }}</span>
          <span class="stat-label">待领取</span>
        </div>
      </div>

      <!-- Claim Banner -->
      <div v-if="unclaimedTasks.length > 0" class="claim-banner">
        <div class="claim-banner-left">
          <span class="claim-icon">!</span>
          <span>你有 <b>{{ unclaimedTasks.length }}</b> 个任务可领取</span>
        </div>
        <div class="claim-banner-tasks">
          <div v-for="task in unclaimedTasks.slice(0, 5)" :key="task.id" class="claim-item">
            <span class="claim-name">{{ projectMap.get(task.projectId)?.projectName || '项目' }}</span>
            <span class="claim-date">{{ task.taskDate.slice(5) }}</span>
            <button class="claim-btn-sm" :disabled="claimingIds.has(task.id)" @click="handleClaimTask(task)">
              {{ claimingIds.has(task.id) ? '...' : '领取' }}
            </button>
          </div>
          <span v-if="unclaimedTasks.length > 5" class="claim-more">还有 {{ unclaimedTasks.length - 5 }} 个...</span>
        </div>
      </div>

      <!-- Project Groups -->
      <div class="project-grid">
      <div v-for="group in projectGroups" :key="group.projectId" class="project-group">
        <!-- Project Header -->
        <div class="project-header">
          <div class="project-title-row">
            <span class="project-name">{{ group.projectName }}</span>
            <div class="project-badges">
              <span v-if="group.stats.active > 0" class="badge blue">{{ group.stats.active }}进行中</span>
              <span v-if="group.stats.done > 0" class="badge green">{{ group.stats.done }}已完成</span>
              <span v-if="group.stats.pending > 0" class="badge orange">{{ group.stats.pending }}待领取</span>
            </div>
          </div>

          <!-- Mini Week Timeline (compact single row) -->
          <div class="mini-timeline">
            <div
              v-for="day in weekDays" :key="day.date"
              class="tl-day" :class="{ 'tl-today': day.isToday }"
              :title="`${day.dayName} ${day.date}`"
            >
              <span class="tl-dot" :class="getDateDotClass(group, day)" />
              <span class="tl-label">{{ day.dayName }}</span>
            </div>
          </div>
        </div>

        <!-- Task Cards inside this project -->
        <div class="project-tasks">
          <div
            v-for="task in group.tasks" :key="task.id"
            class="task-row"
            :class="{
              'row-active': isTaskActive(task),
              'row-done': isTaskDone(task),
              'row-pending': isTaskPending(task),
              'row-today': task.taskDate === today,
            }"
          >
            <div class="task-date">
              <span class="date-day">{{ task.taskDate.slice(8) }}</span>
              <span class="date-month">{{ task.taskDate.slice(5, 7) }}月</span>
            </div>

            <div class="task-body">
              <div class="task-top">
                <span class="task-status" :class="{
                  'ts-active': isTaskActive(task),
                  'ts-done': isTaskDone(task),
                  'ts-pending': isTaskPending(task),
                }">{{ getStatusLabel(task.status) }}</span>
                <span class="task-targets">{{ task.completedTargets + (task.skippedTargets || 0) }}/{{ task.totalTargets }}目标</span>
                <span v-if="task.timeSlotStart" class="task-time">{{ task.timeSlotStart }}-{{ task.timeSlotEnd }}</span>
              </div>

              <!-- Progress bar -->
              <div v-if="!isTaskPending(task) && task.totalTargets > 0" class="task-progress">
                <div class="prog-track">
                  <div class="prog-fill" :class="{ green: isTaskDone(task), blue: isTaskActive(task) }"
                    :style="{ width: getProgressPercent(task) + '%' }" />
                </div>
              </div>
            </div>

            <div class="task-action">
              <button v-if="isTaskActive(task)" class="act-btn blue" @click="goToTask(task)">
                <Play class="w-3 h-3" /> 打分
              </button>
              <button v-else-if="isTaskPending(task) && !task.inspectorId" class="act-btn orange"
                :disabled="claimingIds.has(task.id)" @click="handleClaimTask(task)">
                领取
              </button>
              <button v-else-if="task.status === 'SUBMITTED'" class="act-btn outline"
                @click.stop="handleWithdrawTask(task)">
                撤回
              </button>
              <button v-else-if="isTaskDone(task)" class="act-btn outline" @click="goToTask(task)">
                <Eye class="w-3 h-3" /> 查看
              </button>
            </div>
          </div>
        </div>
      </div>

      </div><!-- /project-grid -->

      <!-- Empty -->
      <div v-if="!loading && allTasks.length === 0" class="empty-state">
        <Target class="w-10 h-10 text-gray-300" />
        <p class="mt-3" style="color: #9ca3af; font-size: 13px">暂无检查任务</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-insp-page { background: #f7f8fa; min-height: 100vh; }

/* ── Top Bar ── */
.topbar {
  position: sticky; top: 0; z-index: 100; height: 48px;
  background: #fff; border-bottom: 1px solid #e5e7eb;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
}
.topbar-title { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.topbar-right { display: flex; align-items: center; gap: 16px; }
.week-nav { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #6b7280; }
.week-nav button {
  width: 28px; height: 28px; border: 1px solid #e5e7eb; border-radius: 6px;
  background: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: #6b7280; transition: all 0.15s;
}
.week-nav button:hover { background: #f3f4f6; color: #1a1a2e; }
.week-label { font-weight: 500; color: #1a1a2e; user-select: none; }

/* ── Page ── */
.page { max-width: 1100px; margin: 0 auto; padding: 16px 20px 60px; }

/* ── Stats Bar ── */
.stats-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px; margin-bottom: 16px;
  background: #fff; border-radius: 8px; border: 1px solid #e5e7eb;
}
.stat-item { display: flex; align-items: baseline; gap: 4px; }
.stat-num { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.stat-num.blue { color: #1a6dff; }
.stat-num.green { color: #10b981; }
.stat-num.orange { color: #f59e0b; }
.stat-label { font-size: 12px; color: #9ca3af; }
.stat-sep { width: 1px; height: 16px; background: #e5e7eb; }

/* ── Claim Banner ── */
.claim-banner {
  background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px;
  padding: 10px 14px; margin-bottom: 14px;
}
.claim-banner-left {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #92400e; margin-bottom: 8px;
}
.claim-banner-left b { color: #d97706; }
.claim-icon {
  width: 18px; height: 18px; border-radius: 50%; background: #f59e0b; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; flex-shrink: 0;
}
.claim-banner-tasks { display: flex; flex-wrap: wrap; gap: 6px; }
.claim-item {
  display: flex; align-items: center; gap: 6px;
  background: #fff; border: 1px solid #fde68a; border-radius: 6px;
  padding: 4px 8px; font-size: 12px;
}
.claim-name { font-weight: 600; color: #1a1a2e; }
.claim-date { color: #9ca3af; }
.claim-btn-sm {
  font-size: 11px; font-weight: 600; padding: 2px 10px; border-radius: 4px;
  border: 1px solid #f59e0b; background: #fff; color: #f59e0b;
  cursor: pointer; transition: all 0.15s;
}
.claim-btn-sm:hover { background: #fffbeb; }
.claim-btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.claim-more { font-size: 11px; color: #9ca3af; align-self: center; }

/* ── Project Grid ── */
.project-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

/* ── Project Group ── */
.project-group {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 10px;
  overflow: hidden;
}
.project-header {
  padding: 10px 14px 8px;
  border-bottom: 1px solid #f3f4f6;
}
.project-title-row {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 6px;
}
.project-name { font-size: 13px; font-weight: 700; color: #1a1a2e; }
.project-badges { display: flex; gap: 6px; }
.badge {
  font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600;
}
.badge.blue { background: #e8f0fe; color: #1a6dff; }
.badge.green { background: #ecfdf5; color: #10b981; }
.badge.orange { background: #fffbeb; color: #f59e0b; }

/* ── Mini Timeline (compact) ── */
.mini-timeline {
  display: flex; align-items: center; gap: 4px;
}
.tl-day {
  display: flex; align-items: center; gap: 3px;
  padding: 2px 4px; border-radius: 4px;
}
.tl-day.tl-today { background: #f0f4ff; }
.tl-label { font-size: 10px; color: #bbb; }
.tl-today .tl-label { color: #1a6dff; font-weight: 600; }

.tl-dot {
  width: 6px; height: 6px; border-radius: 50%;
  border: 1.5px solid #e5e7eb; background: #fff;
}
.tl-dot.dot-done { background: #10b981; border-color: #10b981; }
.tl-dot.dot-active { background: #1a6dff; border-color: #1a6dff; }
.tl-dot.dot-pending { background: #fff; border-color: #f59e0b; }
.tl-dot.dot-empty { background: #fff; border-color: #e5e7eb; }

/* ── Task Rows ── */
.project-tasks { padding: 0; }

.task-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.15s;
}
.task-row:last-child { border-bottom: none; }
.task-row:hover { background: #fafbfc; }
.task-row.row-today { background: #fafcff; }

.task-date {
  width: 30px; flex-shrink: 0;
  display: flex; flex-direction: column; align-items: center;
}
.date-day { font-size: 14px; font-weight: 700; color: #1a1a2e; line-height: 1.1; }
.date-month { font-size: 9px; color: #9ca3af; }
.row-today .date-day { color: #1a6dff; }
.row-done .date-day { color: #10b981; }
.row-pending .date-day { color: #f59e0b; }

.task-body { flex: 1; min-width: 0; }
.task-top {
  display: flex; align-items: center; gap: 8px;
  font-size: 12.5px;
}
.task-status {
  font-weight: 600; font-size: 12px;
  padding: 1px 8px; border-radius: 8px;
}
.ts-active { background: #e8f0fe; color: #1a6dff; }
.ts-done { background: #ecfdf5; color: #10b981; }
.ts-pending { background: #fffbeb; color: #f59e0b; }
.task-targets { color: #6b7280; }
.task-time { color: #9ca3af; }

.task-progress { margin-top: 5px; }
.prog-track { height: 3px; background: #f3f4f6; border-radius: 2px; overflow: hidden; }
.prog-fill { height: 100%; border-radius: 2px; transition: width 0.3s; }
.prog-fill.blue { background: #1a6dff; }
.prog-fill.green { background: #10b981; }

.task-action { flex-shrink: 0; }
.act-btn {
  font-size: 12px; font-weight: 600; padding: 5px 14px; border-radius: 6px;
  border: 1px solid; cursor: pointer; transition: all 0.15s;
  display: flex; align-items: center; gap: 3px; white-space: nowrap;
}
.act-btn.blue { background: #1a6dff; color: #fff; border-color: #1a6dff; }
.act-btn.blue:hover { background: #155cd4; }
.act-btn.orange { background: #fff; color: #f59e0b; border-color: #f59e0b; }
.act-btn.orange:hover { background: #fffbeb; }
.act-btn.outline { background: #fff; color: #6b7280; border-color: #e5e7eb; }
.act-btn.outline:hover { background: #f3f4f6; }
.act-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ── Empty ── */
.empty-state {
  text-align: center; padding: 60px 20px;
  display: flex; flex-direction: column; align-items: center;
}

/* ── Responsive ── */
@media (max-width: 860px) {
  .project-grid { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .topbar { padding: 0 12px; }
  .page { padding: 12px 12px 40px; }
  .stats-bar { gap: 8px; padding: 8px 12px; }
  .project-header { padding: 8px 10px 6px; }
  .task-row { padding: 6px 10px; gap: 6px; }
  .task-date { width: 26px; }
  .date-day { font-size: 12px; }
}
</style>
