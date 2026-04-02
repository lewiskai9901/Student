<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useInspExecutionStore } from '@/stores/insp/inspExecutionStore'
import { useAuthStore } from '@/stores/auth'
import { TaskStatusConfig, type TaskStatus } from '@/types/insp/enums'
import type { InspTask, InspProject } from '@/types/insp/project'

const route = useRoute()
const router = useRouter()
const store = useInspExecutionStore()
const authStore = useAuthStore()

const loading = ref(false)
const tasks = ref<InspTask[]>([])
const projects = ref<Map<number, InspProject>>(new Map())
const activeView = ref<'all' | 'my' | 'available'>('all')
const selectedDate = ref<string | null>(null) // null=全部

const projectId = computed(() =>
  route.query.projectId ? Number(route.query.projectId) : undefined
)

// ——— 日期列表（从任务中提取所有唯一日期） ———
const dateList = computed(() => {
  const dates = new Set<string>()
  tasks.value.forEach(t => dates.add(t.taskDate))
  return [...dates].sort()
})

// ——— 按日期过滤 ———
const filteredTasks = computed(() => {
  if (!selectedDate.value) return tasks.value
  return tasks.value.filter(t => t.taskDate === selectedDate.value)
})

// ——— 按项目分组 ———
interface ProjectGroup {
  project: InspProject | null
  projectId: number
  tasks: InspTask[]
}

const groupedTasks = computed<ProjectGroup[]>(() => {
  const groups = new Map<number, InspTask[]>()
  for (const t of filteredTasks.value) {
    if (!groups.has(t.projectId)) groups.set(t.projectId, [])
    groups.get(t.projectId)!.push(t)
  }
  const result: ProjectGroup[] = []
  for (const [pid, list] of groups) {
    list.sort((a, b) => b.taskDate.localeCompare(a.taskDate))
    result.push({ project: projects.value.get(pid) || null, projectId: pid, tasks: list })
  }
  return result
})

// ——— 统计 ———
const totalCount = computed(() => filteredTasks.value.length)
const pendingCount = computed(() => filteredTasks.value.filter(t => t.status === 'PENDING').length)
const activeCount = computed(() => filteredTasks.value.filter(t => ['CLAIMED', 'IN_PROGRESS'].includes(t.status)).length)
const doneCount = computed(() => filteredTasks.value.filter(t => ['SUBMITTED', 'UNDER_REVIEW', 'REVIEWED', 'PUBLISHED'].includes(t.status)).length)

// ——— 加载 ———
async function loadData() {
  loading.value = true
  try {
    if (activeView.value === 'my') tasks.value = await store.loadMyTasks()
    else if (activeView.value === 'available') tasks.value = await store.loadAvailableTasks()
    else tasks.value = await store.loadTasks(projectId.value)

    const pids = [...new Set(tasks.value.map(t => t.projectId))]
    for (const pid of pids) {
      if (!projects.value.has(pid)) {
        try {
          const p = await store.loadProject(pid)
          if (p) projects.value.set(pid, p)
        } catch (e) { console.warn(`Load project ${pid} failed`, e) }
      }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function switchView(v: 'all' | 'my' | 'available') {
  activeView.value = v
  selectedDate.value = null
  loadData()
}

function selectDate(d: string | null) {
  selectedDate.value = d
}

function taskProgress(t: InspTask) {
  if (t.totalTargets <= 0) return 0
  return Math.round(((t.completedTargets + t.skippedTargets) / t.totalTargets) * 100)
}

function formatDateShort(d: string) {
  const parts = d.split('-')
  return `${parts[1]}/${parts[2]}`
}

function formatWeekday(d: string) {
  const day = new Date(d).getDay()
  return ['日', '一', '二', '三', '四', '五', '六'][day]
}

function isToday(d: string) {
  return d === new Date().toISOString().slice(0, 10)
}

function statusClass(s: string): string {
  if (s === 'PENDING') return 'st--pending'
  if (['CLAIMED', 'IN_PROGRESS'].includes(s)) return 'st--active'
  if (s === 'SUBMITTED') return 'st--submitted'
  if (['UNDER_REVIEW', 'REVIEWED'].includes(s)) return 'st--review'
  if (s === 'PUBLISHED') return 'st--published'
  if (s === 'CANCELLED') return 'st--cancelled'
  return ''
}

function modeLabel(t: InspTask) {
  return t.collaborationMode === 'COLLABORATIVE' ? '协作' : '单评'
}

function modeClass(t: InspTask) {
  return t.collaborationMode === 'COLLABORATIVE' ? 'mode--collab' : 'mode--single'
}

function goExecute(t: InspTask) { router.push(`/inspection/v7/tasks/${t.id}/execute`) }
function goProject(pid: number) { router.push(`/inspection/v7/projects/${pid}`) }

async function handleClaim(t: InspTask) {
  try {
    await store.claimTask(t.id, { inspectorName: authStore.userName || '未知用户' })
    ElMessage.success('领取成功')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '领取失败') }
}

async function handleCancel(t: InspTask) {
  try {
    await ElMessageBox.confirm('确定取消？', '确认', { type: 'warning' })
    await store.cancelTask(t.id)
    ElMessage.success('已取消')
    loadData()
  } catch (e: any) { if (e !== 'cancel') console.warn('Operation failed', e) }
}

async function handlePublish(t: InspTask) {
  try {
    await store.publishTask(t.id)
    ElMessage.success('已发布')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '发布失败') }
}

onMounted(loadData)
</script>

<template>
  <div class="tl">
    <!-- 顶部 -->
    <div class="tl-header">
      <h2 class="tl-title">检查任务</h2>
      <div class="tl-tabs">
        <button :class="['tl-tab', activeView === 'all' && 'active']" @click="switchView('all')">全部</button>
        <button :class="['tl-tab', activeView === 'my' && 'active']" @click="switchView('my')">我的</button>
        <button :class="['tl-tab', activeView === 'available' && 'active']" @click="switchView('available')">可领取</button>
      </div>
    </div>

    <!-- 日期时间轴 -->
    <div class="tl-timeline" v-if="dateList.length > 0">
      <button
        :class="['tl-date-pill', !selectedDate && 'active']"
        @click="selectDate(null)"
      >全部</button>
      <button
        v-for="d in dateList" :key="d"
        :class="['tl-date-pill', selectedDate === d && 'active', isToday(d) && 'today']"
        @click="selectDate(d)"
      >
        <span class="date-main">{{ formatDateShort(d) }}</span>
        <span class="date-week">周{{ formatWeekday(d) }}</span>
      </button>
    </div>

    <!-- 统计 -->
    <div class="tl-stats">
      <span class="tl-stat">共 <b>{{ totalCount }}</b> 个任务</span>
      <span class="tl-stat-sep">|</span>
      <span class="tl-stat">待领取 <b class="c-amber">{{ pendingCount }}</b></span>
      <span class="tl-stat-sep">|</span>
      <span class="tl-stat">进行中 <b class="c-blue">{{ activeCount }}</b></span>
      <span class="tl-stat-sep">|</span>
      <span class="tl-stat">已完成 <b class="c-green">{{ doneCount }}</b></span>
    </div>

    <!-- 加载 -->
    <div v-if="loading" class="tl-loading">加载中...</div>

    <!-- 任务分组 -->
    <div v-else class="tl-groups">
      <div v-for="group in groupedTasks" :key="group.projectId" class="tl-group">
        <!-- 项目组头 -->
        <div class="tl-group-head">
          <span class="tl-group-name">{{ group.project?.projectName || `项目 #${group.projectId}` }}</span>
          <span class="tl-group-count">{{ group.tasks.length }} 个任务</span>
          <button class="tl-group-link" @click="goProject(group.projectId)">查看项目</button>
        </div>

        <!-- 任务卡片网格 -->
        <div class="tl-cards">
          <div v-for="t in group.tasks" :key="t.id" class="tl-card" @click="goExecute(t)">
            <!-- 卡片头：编号 + 状态 -->
            <div class="card-top">
              <span class="card-code">{{ t.taskCode }}</span>
              <span :class="['card-status', statusClass(t.status)]">
                {{ TaskStatusConfig[t.status as TaskStatus]?.label || t.status }}
              </span>
            </div>

            <!-- 日期 + 模式 -->
            <div class="card-meta">
              <span class="card-date">{{ t.taskDate }}</span>
              <span :class="['card-mode', modeClass(t)]">{{ modeLabel(t) }}</span>
            </div>

            <!-- 进度条 -->
            <div class="card-progress">
              <div class="card-bar">
                <div class="card-bar-fill" :style="{ width: taskProgress(t) + '%' }" />
              </div>
              <span class="card-pct">{{ t.completedTargets + t.skippedTargets }}/{{ t.totalTargets }}</span>
            </div>

            <!-- 检查员信息 -->
            <div class="card-inspector" v-if="t.inspectorName">
              <span class="card-avatar">{{ t.inspectorName.charAt(0) }}</span>
              <span class="card-name">{{ t.inspectorName }}</span>
            </div>
            <div class="card-inspector card-inspector--empty" v-else>
              <span class="card-avatar card-avatar--empty">?</span>
              <span class="card-name">未分配</span>
            </div>

            <!-- 操作 -->
            <div class="card-actions" @click.stop>
              <button v-if="t.status === 'PENDING'" class="card-act act--claim" @click="handleClaim(t)">领取</button>
              <button v-if="['CLAIMED','IN_PROGRESS'].includes(t.status)" class="card-act act--exec" @click="goExecute(t)">执行</button>
              <button v-if="t.status === 'REVIEWED'" class="card-act act--publish" @click="handlePublish(t)">发布</button>
              <button v-if="['SUBMITTED','UNDER_REVIEW','REVIEWED','PUBLISHED'].includes(t.status)" class="card-act act--view" @click="goExecute(t)">查看</button>
              <button v-if="['PENDING','CLAIMED'].includes(t.status)" class="card-act act--cancel" @click="handleCancel(t)">取消</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="groupedTasks.length === 0" class="tl-empty">
        {{ activeView === 'my' ? '你还没有领取任何任务' : activeView === 'available' ? '暂无可领取的任务' : '暂无检查任务' }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.tl { padding: 20px 24px; }

/* 顶部 */
.tl-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.tl-title { font-size: 16px; font-weight: 600; color: #1e2a3a; margin: 0; }
.tl-tabs { display: flex; gap: 2px; background: #f0f1f3; border-radius: 6px; padding: 2px; }
.tl-tab {
  padding: 5px 14px; border-radius: 5px; border: none; background: transparent;
  font-size: 12px; color: #6b7280; cursor: pointer; transition: all 0.15s;
}
.tl-tab.active { background: #fff; color: #1a6dff; font-weight: 500; box-shadow: 0 1px 2px rgba(0,0,0,0.06); }

/* 日期时间轴 */
.tl-timeline {
  display: flex; gap: 6px; overflow-x: auto; padding: 8px 0 12px;
  scrollbar-width: thin; scrollbar-color: #d1d5db transparent;
}
.tl-timeline::-webkit-scrollbar { height: 4px; }
.tl-timeline::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 2px; }
.tl-date-pill {
  display: flex; flex-direction: column; align-items: center; gap: 1px;
  padding: 6px 12px; border-radius: 8px; border: 1px solid #e5e7eb; background: #fff;
  cursor: pointer; white-space: nowrap; transition: all 0.15s; flex-shrink: 0;
}
.tl-date-pill:hover { border-color: #93c5fd; background: #f0f7ff; }
.tl-date-pill.active { border-color: #1a6dff; background: #1a6dff; color: #fff; }
.tl-date-pill.active .date-week { color: rgba(255,255,255,0.8); }
.tl-date-pill.today { border-color: #f59e0b; }
.tl-date-pill.today:not(.active) .date-main { color: #f59e0b; font-weight: 600; }
.date-main { font-size: 13px; font-weight: 500; color: #374151; }
.date-week { font-size: 10px; color: #9ca3af; }

/* 统计 */
.tl-stats { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #6b7280; margin-bottom: 12px; }
.tl-stat b { font-weight: 600; color: #374151; }
.tl-stat-sep { color: #d1d5db; }
.c-amber { color: #d97706; }
.c-blue { color: #1a6dff; }
.c-green { color: #059669; }

/* 加载/空 */
.tl-loading { text-align: center; padding: 40px; color: #9ca3af; font-size: 13px; }
.tl-empty { text-align: center; padding: 48px 20px; color: #9ca3af; font-size: 13px; }

/* 分组 */
.tl-groups { display: flex; flex-direction: column; gap: 16px; }
.tl-group {}
.tl-group-head {
  display: flex; align-items: center; gap: 8px;
  padding: 0 4px 8px; border-bottom: 1px solid #f0f1f3;
}
.tl-group-name { font-size: 14px; font-weight: 600; color: #1e2a3a; flex: 1; }
.tl-group-count { font-size: 11px; color: #9ca3af; }
.tl-group-link {
  font-size: 11px; color: #1a6dff; background: none; border: none; cursor: pointer;
  padding: 2px 6px; border-radius: 4px;
}
.tl-group-link:hover { background: #eff4ff; }

/* 卡片网格 */
.tl-cards {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 10px; padding-top: 10px;
}

/* 单张卡片 */
.tl-card {
  display: flex; flex-direction: column; gap: 8px;
  padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 8px;
  background: #fff; cursor: pointer; transition: all 0.15s;
}
.tl-card:hover { border-color: #93c5fd; box-shadow: 0 2px 8px rgba(26,109,255,0.08); }

/* 卡片顶部 */
.card-top { display: flex; align-items: center; justify-content: space-between; }
.card-code { font-size: 12px; font-weight: 500; color: #374151; font-family: 'SF Mono','Consolas',monospace; }
.card-status {
  font-size: 10px; padding: 1px 6px; border-radius: 3px; font-weight: 500;
}
.st--pending { color: #d97706; background: #fffbeb; }
.st--active { color: #1a6dff; background: #eff4ff; }
.st--submitted { color: #7c3aed; background: #f5f3ff; }
.st--review { color: #0891b2; background: #ecfeff; }
.st--published { color: #059669; background: #ecfdf5; }
.st--cancelled { color: #9ca3af; background: #f3f4f6; }

/* 日期+模式 */
.card-meta { display: flex; align-items: center; gap: 8px; }
.card-date { font-size: 12px; color: #6b7280; }
.card-mode {
  font-size: 10px; padding: 1px 5px; border-radius: 3px; font-weight: 500;
}
.mode--single { color: #6b7280; background: #f3f4f6; }
.mode--collab { color: #7c3aed; background: #f5f3ff; }

/* 进度 */
.card-progress { display: flex; align-items: center; gap: 8px; }
.card-bar { flex: 1; height: 4px; background: #f0f1f3; border-radius: 2px; overflow: hidden; }
.card-bar-fill { height: 100%; background: #1a6dff; border-radius: 2px; transition: width 0.3s; }
.card-pct { font-size: 11px; color: #9ca3af; min-width: 40px; text-align: right; }

/* 检查员 */
.card-inspector { display: flex; align-items: center; gap: 6px; }
.card-avatar {
  width: 22px; height: 22px; border-radius: 50%; background: #1a6dff; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 600; flex-shrink: 0;
}
.card-avatar--empty { background: #d1d5db; color: #fff; }
.card-name { font-size: 12px; color: #374151; }
.card-inspector--empty .card-name { color: #9ca3af; }

/* 操作 */
.card-actions { display: flex; gap: 4px; padding-top: 4px; border-top: 1px solid #f5f6f7; }
.card-act {
  font-size: 11px; padding: 3px 10px; border-radius: 4px; border: 1px solid transparent;
  cursor: pointer; transition: all 0.12s; background: none; flex: 1; text-align: center;
}
.act--claim { color: #d97706; border-color: #fcd34d; }
.act--claim:hover { background: #fffbeb; }
.act--exec { color: #fff; background: #1a6dff; border-color: #1a6dff; }
.act--exec:hover { background: #1559d4; }
.act--publish { color: #059669; border-color: #6ee7b7; }
.act--publish:hover { background: #ecfdf5; }
.act--view { color: #6b7280; border-color: #d1d5db; }
.act--view:hover { background: #f9fafb; }
.act--cancel { color: #ef4444; border-color: #fca5a5; }
.act--cancel:hover { background: #fef2f2; }
</style>
