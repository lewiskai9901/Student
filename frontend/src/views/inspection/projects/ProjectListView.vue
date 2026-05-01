<script setup lang="ts">
/**
 * ProjectListView — 检查项目总台 (3 视图重设计)
 *
 * 不再是单一 Kanban 看板. 三视图智能切换:
 *   ① 列表 (默认) — 信息密度优先, 一屏 8-10 项目, 紧迫度智能排序
 *   ② 看板 — 按"关注度"分组 (需我处理 / 监控中 / 已闭环), 非按机械状态机
 *   ③ 时间轴 — 横向甘特带, 看时间分布
 *
 * 顶部 4 个 KPI: 总数 / 进行中 / 草稿 / 逾期
 * 多筛选条: 状态 / 时段 / 模板 / 我管理的
 * 行内主动作 CTA 按状态自适应
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, AlertTriangle } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'
import { inspProjectApi } from '@/api/inspection/project'
import { getTasks } from '@/api/inspection/task'
import { ProjectStatusConfig, type ProjectStatus } from '@/types/insp/enums'
import type { InspProject, InspTask } from '@/types/insp/project'

const router = useRouter()
const store = useInspExecutionStore()

// ── State ──
const loading = ref(false)
const projects = ref<InspProject[]>([])
const taskListMap = ref<Map<number, InspTask[]>>(new Map())
const inspectorCountMap = ref<Map<number, number>>(new Map())
const today = new Date().toISOString().slice(0, 10)

const view = ref<'list' | 'kanban' | 'timeline'>('list')
const searchQuery = ref('')
const statusFilter = ref<ProjectStatus | 'all'>('all')
const periodFilter = ref<'all' | 'this-week' | 'this-month' | 'overdue'>('all')

// ── Loaders ──
async function loadData() {
  loading.value = true
  try {
    projects.value = await store.loadProjects()
    await Promise.all(projects.value.map(async (p) => {
      try {
        const tasks = await getTasks({ projectId: p.id }).catch(() => [] as InspTask[])
        taskListMap.value.set(p.id, tasks)
        const inspectors = await inspProjectApi.getInspectors(p.id).catch(() => [])
        inspectorCountMap.value.set(p.id, inspectors.length)
      } catch { /* ignore */ }
    }))
  } catch (e: any) {
    ElMessage.error(e.message || '加载项目列表失败')
  } finally {
    loading.value = false
  }
}

// ── Helpers ──
function isOverdue(p: InspProject): boolean {
  if (p.status !== 'PUBLISHED' && p.status !== 'PAUSED') return false
  if (!p.endDate) return false
  return p.endDate < today
}

function isPending(p: InspProject): boolean {
  return p.status === 'DRAFT'
}

function tasksOf(pid: number): InspTask[] {
  return taskListMap.value.get(pid) || []
}

function taskStats(pid: number) {
  const tasks = tasksOf(pid)
  const total = tasks.length
  const done = tasks.filter(t => ['REVIEWED', 'PUBLISHED'].includes(t.status)).length
  const pending = tasks.filter(t => ['PENDING', 'CLAIMED', 'IN_PROGRESS'].includes(t.status)).length
  const overdue = tasks.filter(t => {
    if (!t.taskDate) return false
    if (['PUBLISHED', 'CANCELLED', 'EXPIRED'].includes(t.status)) return false
    const eff = (t as any).extendedTo || t.taskDate
    return eff < today
  }).length
  const pct = total === 0 ? 0 : Math.round((done / total) * 100)
  return { total, done, pending, overdue, pct }
}

function urgencyScore(p: InspProject): number {
  // 紧迫度评分: 越大越紧急, 用于默认排序
  if (isOverdue(p)) return 1000
  const s = taskStats(p.id)
  if (s.overdue > 0) return 800 + s.overdue
  if (p.status === 'DRAFT') return 600
  if (p.status === 'PUBLISHED' && p.endDate) {
    const days = Math.ceil((new Date(p.endDate).getTime() - Date.now()) / 86400000)
    if (days <= 3) return 400 - days
    return 300 - days
  }
  if (p.status === 'PAUSED') return 200
  if (p.status === 'COMPLETED') return 50
  if (p.status === 'ARCHIVED') return 10
  return 100
}

function projectChip(p: InspProject): string {
  if (isOverdue(p)) return 'fail'
  if (taskStats(p.id).overdue > 0) return 'warn'
  return ({
    DRAFT: 'pending', PUBLISHED: 'info', PAUSED: 'warn',
    COMPLETED: 'pass', ARCHIVED: 'pending',
  } as any)[p.status] || 'pending'
}

function fmtDate(s?: string | null) {
  if (!s) return '—'
  return s.replace(/^\d{4}-/, '').replace(/-/g, '.')
}

function fmtRange(p: InspProject): string {
  if (!p.startDate) return '—'
  if (!p.endDate) return `${fmtDate(p.startDate)} 起`
  return `${fmtDate(p.startDate)} → ${fmtDate(p.endDate)}`
}

function daysRemaining(p: InspProject): string | null {
  if (!p.endDate || (p.status !== 'PUBLISHED' && p.status !== 'PAUSED')) return null
  const d = Math.ceil((new Date(p.endDate).getTime() - Date.now()) / 86400000)
  if (d < 0) return `逾期 ${Math.abs(d)} 天`
  if (d === 0) return '今日截止'
  if (d === 1) return '明日截止'
  if (d <= 7) return `剩 ${d} 天`
  return null
}

// ── Filters / sort ──
const filtered = computed(() => {
  const q = searchQuery.value.toLowerCase()
  let list = projects.value.slice()
  if (q) list = list.filter(p => p.projectName.toLowerCase().includes(q))
  if (statusFilter.value !== 'all') list = list.filter(p => p.status === statusFilter.value)
  if (periodFilter.value === 'this-week') {
    const wkEnd = new Date(); wkEnd.setDate(wkEnd.getDate() + 7)
    const wkEndS = wkEnd.toISOString().slice(0, 10)
    list = list.filter(p => p.endDate && p.endDate >= today && p.endDate <= wkEndS)
  } else if (periodFilter.value === 'this-month') {
    const moEnd = new Date(); moEnd.setMonth(moEnd.getMonth() + 1)
    const moEndS = moEnd.toISOString().slice(0, 10)
    list = list.filter(p => p.endDate && p.endDate >= today && p.endDate <= moEndS)
  } else if (periodFilter.value === 'overdue') {
    list = list.filter(p => isOverdue(p) || taskStats(p.id).overdue > 0)
  }
  return list.sort((a, b) => urgencyScore(b) - urgencyScore(a))
})

// ── KPI ──
const kpi = computed(() => ({
  total: projects.value.length,
  active: projects.value.filter(p => p.status === 'PUBLISHED' || p.status === 'PAUSED').length,
  draft: projects.value.filter(p => p.status === 'DRAFT').length,
  overdue: projects.value.filter(p => isOverdue(p) || taskStats(p.id).overdue > 0).length,
}))

// ── Kanban (关注度分组, 非状态机) ──
const buckets = computed(() => {
  const data = filtered.value
  return [
    {
      key: 'attention',
      label: '需要我处理',
      hint: '逾期 · 任务积压 · 草稿待发布',
      tone: 'fail',
      items: data.filter(p => isOverdue(p) || taskStats(p.id).overdue > 0 || p.status === 'DRAFT'),
    },
    {
      key: 'monitor',
      label: '监控中',
      hint: '正常进行 / 暂停',
      tone: 'info',
      items: data.filter(p => (p.status === 'PUBLISHED' || p.status === 'PAUSED')
        && !isOverdue(p) && taskStats(p.id).overdue === 0),
    },
    {
      key: 'done',
      label: '已闭环',
      hint: '完结 / 归档',
      tone: 'pass',
      items: data.filter(p => p.status === 'COMPLETED' || p.status === 'ARCHIVED'),
    },
  ]
})

// ── Timeline ──
const timelineRange = computed(() => {
  if (filtered.value.length === 0) return null
  const dates = filtered.value
    .flatMap(p => [p.startDate, p.endDate])
    .filter(Boolean) as string[]
  if (dates.length === 0) return null
  const min = dates.reduce((a, b) => a < b ? a : b)
  const max = dates.reduce((a, b) => a > b ? a : b)
  return { min, max }
})

function timelineBar(p: InspProject) {
  const range = timelineRange.value
  if (!range || !p.startDate || !p.endDate) return { left: '0%', width: '0%' }
  const minMs = new Date(range.min).getTime()
  const maxMs = new Date(range.max).getTime()
  const span = maxMs - minMs || 1
  const startMs = new Date(p.startDate).getTime()
  const endMs = new Date(p.endDate).getTime()
  return {
    left: `${((startMs - minMs) / span) * 100}%`,
    width: `${((endMs - startMs) / span) * 100}%`,
  }
}

const todayMarker = computed(() => {
  const range = timelineRange.value
  if (!range) return null
  const minMs = new Date(range.min).getTime()
  const maxMs = new Date(range.max).getTime()
  const todayMs = Date.now()
  if (todayMs < minMs || todayMs > maxMs) return null
  return `${((todayMs - minMs) / (maxMs - minMs)) * 100}%`
})

// ── Actions ──
function goCreate() { router.push('/inspection/projects/create') }
function goDetail(p: InspProject) { router.push(`/inspection/projects/${p.id}`) }

async function handleAction(p: InspProject, action: string) {
  try {
    if (action === 'publish') {
      router.push(`/inspection/projects/${p.id}`)
      return
    }
    if (action === 'pause') {
      await store.pauseProject(p.id); ElMessage.success('已暂停')
    } else if (action === 'resume') {
      await store.resumeProject(p.id); ElMessage.success('已恢复')
    } else if (action === 'complete') {
      await ElMessageBox.confirm('确定完结此项目?', '确认', { type: 'warning' })
      await store.completeProject(p.id); ElMessage.success('已完结')
    } else if (action === 'archive') {
      await store.archiveProject(p.id); ElMessage.success('已归档')
    } else if (action === 'delete') {
      await ElMessageBox.confirm(`删除项目「${p.projectName}」?`, '确认删除', { type: 'warning' })
      await store.removeProject(p.id); ElMessage.success('已删除')
    }
    loadData()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

function primaryAction(p: InspProject): { label: string; key: string; emphasized?: boolean } {
  if (p.status === 'DRAFT') return { label: '配置 + 发布', key: 'publish', emphasized: true }
  if (p.status === 'PUBLISHED' && taskStats(p.id).overdue > 0) return { label: '处理逾期', key: 'detail', emphasized: true }
  if (p.status === 'PAUSED') return { label: '恢复', key: 'resume', emphasized: true }
  return { label: '详情', key: 'detail' }
}

onMounted(loadData)
</script>

<template>
  <div class="insp-shell prj-page">
    <!-- ── Header + KPIs ─────────── -->
    <header class="prj-head">
      <div class="prj-head__lead">
        <span class="insp-eyebrow">检查项目 · Inspection Campaigns</span>
        <h1 class="prj-title">检查项目</h1>
      </div>
      <div class="prj-kpi">
        <button class="kpi" :class="{ 'is-active': statusFilter === 'all' }" @click="statusFilter = 'all'">
          <span class="kpi__num insp-num">{{ kpi.total }}</span>
          <span class="kpi__label">总数</span>
        </button>
        <button class="kpi" :class="{ 'is-active': statusFilter === 'PUBLISHED' }" @click="statusFilter = 'PUBLISHED'">
          <span class="kpi__num insp-num" style="color: var(--insp-info)">{{ kpi.active }}</span>
          <span class="kpi__label">进行中</span>
        </button>
        <button class="kpi" :class="{ 'is-active': statusFilter === 'DRAFT' }" @click="statusFilter = 'DRAFT'">
          <span class="kpi__num insp-num">{{ kpi.draft }}</span>
          <span class="kpi__label">草稿</span>
        </button>
        <button class="kpi" :class="{ 'is-active': periodFilter === 'overdue' }" @click="periodFilter = periodFilter === 'overdue' ? 'all' : 'overdue'">
          <span class="kpi__num insp-num" :style="{ color: kpi.overdue ? 'var(--insp-fail)' : 'var(--insp-ink-primary)' }">
            {{ kpi.overdue }}
          </span>
          <span class="kpi__label">告警</span>
        </button>
      </div>
      <button class="prj-cta insp-btn insp-btn--accent" @click="goCreate">
        <Plus :size="13" />新建项目
      </button>
    </header>

    <!-- ── Toolbar (view + filters + search) ─────────── -->
    <div class="prj-toolbar">
      <!-- View toggle -->
      <div class="view-tabs">
        <button class="view-tab" :class="{ 'is-active': view === 'list' }" @click="view = 'list'">
          列表
        </button>
        <button class="view-tab" :class="{ 'is-active': view === 'kanban' }" @click="view = 'kanban'">
          看板
        </button>
        <button class="view-tab" :class="{ 'is-active': view === 'timeline' }" @click="view = 'timeline'">
          时间轴
        </button>
      </div>

      <!-- Filter chips -->
      <div class="filter-chips">
        <button class="chip" :class="{ 'is-active': periodFilter === 'all' }" @click="periodFilter = 'all'">全部时段</button>
        <button class="chip" :class="{ 'is-active': periodFilter === 'this-week' }" @click="periodFilter = 'this-week'">本周到期</button>
        <button class="chip" :class="{ 'is-active': periodFilter === 'this-month' }" @click="periodFilter = 'this-month'">本月</button>
      </div>

      <div class="prj-toolbar__spacer" />

      <input v-model="searchQuery" class="prj-search" placeholder="搜索项目名称…" />
    </div>

    <!-- ============ List View ============ -->
    <section v-if="view === 'list'" class="prj-list" v-loading="loading">
      <div class="row row--head">
        <span class="col col-num">#</span>
        <span class="col col-name">项目</span>
        <span class="col col-status">状态</span>
        <span class="col col-period">期间</span>
        <span class="col col-progress">进度</span>
        <span class="col col-tasks">任务</span>
        <span class="col col-alert">告警</span>
        <span class="col col-actions">操作</span>
      </div>

      <div
        v-for="(p, i) in filtered" :key="p.id"
        class="row row--data"
        :class="{ 'row--overdue': isOverdue(p) || taskStats(p.id).overdue > 0 }"
        @click="goDetail(p)"
      >
        <span class="col col-num insp-num">{{ String(i + 1).padStart(2, '0') }}</span>

        <div class="col col-name">
          <div class="name-line">
            <span class="name-text">{{ p.projectName }}</span>
            <span v-if="isPending(p)" class="dot dot--draft" title="待配置发布" />
          </div>
          <div class="name-meta">
            <span class="insp-num">{{ p.projectCode }}</span>
            <span class="sep">·</span>
            <span><span class="insp-num">{{ inspectorCountMap.get(p.id) || 0 }}</span> 检查员</span>
          </div>
        </div>

        <span class="col col-status">
          <span class="insp-chip" :class="`insp-chip--${projectChip(p)}`">
            {{ ProjectStatusConfig[p.status as ProjectStatus]?.label }}
          </span>
        </span>

        <span class="col col-period">
          <span class="period-line insp-num">{{ fmtRange(p) }}</span>
          <span v-if="daysRemaining(p)" class="period-rel"
                :class="{ 'is-urgent': daysRemaining(p)?.startsWith('逾期') || daysRemaining(p)?.startsWith('今日') }">
            {{ daysRemaining(p) }}
          </span>
        </span>

        <span class="col col-progress">
          <div class="prog-wrap">
            <div class="prog-bar">
              <div class="prog-fill" :style="{ width: taskStats(p.id).pct + '%' }" />
            </div>
            <span class="prog-text insp-num">{{ taskStats(p.id).pct }}%</span>
          </div>
        </span>

        <span class="col col-tasks insp-num">
          {{ taskStats(p.id).done }}<span class="dim">/{{ taskStats(p.id).total }}</span>
        </span>

        <span class="col col-alert">
          <span v-if="taskStats(p.id).overdue > 0" class="alert-tag">
            <AlertTriangle :size="11" />
            <span class="insp-num">{{ taskStats(p.id).overdue }}</span> 逾期
          </span>
          <span v-else-if="p.status === 'DRAFT'" class="alert-tag alert-tag--draft">待发布</span>
          <span v-else class="dim">—</span>
        </span>

        <div class="col col-actions" @click.stop>
          <button
            class="insp-btn insp-btn--sm"
            :class="{ 'insp-btn--accent': primaryAction(p).emphasized }"
            @click="primaryAction(p).key === 'detail' || primaryAction(p).key === 'publish' ? goDetail(p) : handleAction(p, primaryAction(p).key)"
          >{{ primaryAction(p).label }}</button>
          <button v-if="p.status === 'PUBLISHED'" class="insp-btn insp-btn--sm" @click="handleAction(p, 'pause')">暂停</button>
          <button v-if="['PUBLISHED','PAUSED'].includes(p.status)" class="insp-btn insp-btn--sm" @click="handleAction(p, 'complete')">完结</button>
          <button v-if="p.status === 'COMPLETED'" class="insp-btn insp-btn--sm" @click="handleAction(p, 'archive')">归档</button>
          <button v-if="p.status === 'DRAFT' || p.status === 'ARCHIVED'" class="insp-btn insp-btn--sm insp-btn--ghost" @click="handleAction(p, 'delete')" title="删除">×</button>
        </div>
      </div>

      <div v-if="!loading && filtered.length === 0" class="empty">
        <p class="empty-title">未找到匹配的项目</p>
        <p class="empty-sub">尝试调整筛选条件, 或新建一个项目</p>
        <button class="insp-btn insp-btn--accent" @click="goCreate">
          <Plus :size="13" />新建项目
        </button>
      </div>
    </section>

    <!-- ============ Kanban View (按关注度) ============ -->
    <section v-else-if="view === 'kanban'" class="prj-kanban" v-loading="loading">
      <article v-for="b in buckets" :key="b.key" class="bucket" :class="`bucket--${b.tone}`">
        <header class="bucket-head">
          <div class="bucket-head__lead">
            <span class="bucket-name">{{ b.label }}</span>
            <span class="bucket-count insp-num">{{ b.items.length }}</span>
          </div>
          <span class="bucket-hint">{{ b.hint }}</span>
        </header>
        <div class="bucket-body">
          <div v-for="p in b.items" :key="p.id"
               class="kban-card"
               :class="{ 'is-overdue': isOverdue(p) || taskStats(p.id).overdue > 0 }"
               @click="goDetail(p)">
            <div class="kban-line1">
              <span class="kban-name">{{ p.projectName }}</span>
              <span class="insp-chip" :class="`insp-chip--${projectChip(p)}`">
                {{ ProjectStatusConfig[p.status as ProjectStatus]?.label }}
              </span>
            </div>
            <div class="kban-meta insp-num">{{ fmtRange(p) }}</div>
            <div class="kban-progress">
              <div class="prog-bar"><div class="prog-fill" :style="{ width: taskStats(p.id).pct + '%' }" /></div>
              <span class="kban-progress__text insp-num">{{ taskStats(p.id).done }}/{{ taskStats(p.id).total }}</span>
            </div>
            <div v-if="taskStats(p.id).overdue > 0 || daysRemaining(p)" class="kban-alert">
              <span v-if="taskStats(p.id).overdue > 0" class="alert-tag">
                <AlertTriangle :size="10" />
                <span class="insp-num">{{ taskStats(p.id).overdue }}</span> 逾期
              </span>
              <span v-else-if="daysRemaining(p)" class="period-rel"
                    :class="{ 'is-urgent': daysRemaining(p)?.startsWith('逾期') || daysRemaining(p)?.startsWith('今日') }">
                {{ daysRemaining(p) }}
              </span>
            </div>
          </div>
          <div v-if="b.items.length === 0" class="bucket-empty">无项目</div>
        </div>
      </article>
    </section>

    <!-- ============ Timeline View ============ -->
    <section v-else class="prj-timeline" v-loading="loading">
      <div v-if="!timelineRange" class="empty">
        <p class="empty-title">无法生成时间轴</p>
        <p class="empty-sub">项目未设置开始/结束日期</p>
      </div>
      <template v-else>
        <header class="tl-head">
          <span class="tl-axis-label insp-num">{{ timelineRange.min }}</span>
          <div class="tl-axis-spacer" />
          <span class="tl-axis-label insp-num">{{ timelineRange.max }}</span>
        </header>
        <div class="tl-grid">
          <div v-if="todayMarker" class="tl-today" :style="{ left: todayMarker }">
            <span class="tl-today__label insp-num">今天</span>
          </div>
          <div v-for="p in filtered" :key="p.id" class="tl-row" @click="goDetail(p)">
            <span class="tl-name">{{ p.projectName }}</span>
            <div class="tl-track">
              <div
                class="tl-bar"
                :class="`tl-bar--${projectChip(p)}`"
                :style="timelineBar(p)"
              >
                <span class="tl-bar__pct insp-num">{{ taskStats(p.id).pct }}%</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.prj-page { padding: 12px 16px; }

/* ─ Head + KPI ─────── */
.prj-head {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 12px 16px;
  margin-bottom: 10px;
}
.prj-head__lead { display: flex; flex-direction: column; gap: 2px; }
.prj-title {
  font-size: 17px;
  font-weight: 700;
  margin: 2px 0 0;
  color: var(--insp-ink-primary);
}
.prj-kpi {
  display: flex;
  margin-left: auto;
  gap: 6px;
}
.kpi {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  min-width: 64px;
  padding: 4px 14px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--insp-radius-sm);
  cursor: pointer;
  transition: all var(--insp-t-fast);
  font-family: inherit;
}
.kpi:hover { background: var(--insp-bg-subtle); }
.kpi.is-active {
  background: var(--insp-accent-paler);
  border-color: var(--insp-accent-pale);
}
.kpi__num {
  font-family: var(--insp-font-mono);
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  color: var(--insp-ink-primary);
}
.kpi__label {
  font-size: 10px;
  color: var(--insp-ink-tertiary);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  margin-top: 2px;
}
.prj-cta { margin-left: 4px; }

/* ─ Toolbar ─────── */
.prj-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 6px 8px 6px 12px;
  margin-bottom: 10px;
}

.view-tabs {
  display: flex;
  gap: 2px;
  padding: 2px;
  background: var(--insp-bg-subtle);
  border-radius: var(--insp-radius-sm);
}
.view-tab {
  height: 24px;
  padding: 0 12px;
  background: transparent;
  border: 0;
  border-radius: 3px;
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  transition: all var(--insp-t-fast);
}
.view-tab:hover { color: var(--insp-ink-primary); }
.view-tab.is-active {
  background: var(--insp-bg-surface);
  color: var(--insp-ink-primary);
  box-shadow: var(--insp-shadow-xs);
  font-weight: 600;
}

.filter-chips {
  display: flex;
  gap: 4px;
}
.chip {
  height: 24px;
  padding: 0 10px;
  background: transparent;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-pill);
  font-family: inherit;
  font-size: 11px;
  font-weight: 500;
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  transition: all var(--insp-t-fast);
}
.chip:hover { color: var(--insp-ink-primary); border-color: var(--insp-border-strong); }
.chip.is-active {
  background: var(--insp-accent);
  color: white;
  border-color: var(--insp-accent);
}

.prj-toolbar__spacer { flex: 1; }

.prj-search {
  height: 26px;
  padding: 0 10px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-size: 12px;
  font-family: inherit;
  width: 220px;
  background: var(--insp-bg-surface);
}
.prj-search:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}

/* ============ List view ============ */
.prj-list {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  overflow: hidden;
}

.row {
  display: grid;
  grid-template-columns: 36px 2fr 90px 140px 130px 70px 90px 220px;
  gap: 10px;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
  transition: background var(--insp-t-fast);
}
.row--head {
  background: var(--insp-bg-subtle);
  font-size: 10px;
  font-weight: 600;
  color: var(--insp-ink-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 8px 14px;
}
.row--data { cursor: pointer; }
.row--data:hover { background: var(--insp-bg-subtle); }
.row--data.row--overdue {
  background: linear-gradient(to right, var(--insp-fail-pale), transparent 60%);
}
.row--data.row--overdue:hover {
  background: linear-gradient(to right, color-mix(in oklab, var(--insp-fail-pale) 80%, var(--insp-bg-subtle)), var(--insp-bg-subtle) 60%);
}

.col { min-width: 0; }
.col-num {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  color: var(--insp-ink-quaternary);
}

.name-line { display: flex; align-items: center; gap: 6px; }
.name-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot--draft { background: var(--insp-warn); }
.name-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--insp-ink-tertiary);
  margin-top: 2px;
}
.sep { color: var(--insp-ink-quaternary); }

.period-line {
  font-family: var(--insp-font-mono);
  font-size: 12px;
  color: var(--insp-ink-secondary);
  display: block;
}
.period-rel {
  display: block;
  font-size: 10px;
  color: var(--insp-ink-tertiary);
  margin-top: 2px;
}
.period-rel.is-urgent {
  color: var(--insp-fail);
  font-weight: 600;
}

.prog-wrap { display: flex; align-items: center; gap: 6px; }
.prog-bar {
  flex: 1;
  height: 5px;
  background: var(--insp-bg-sunken);
  border-radius: 3px;
  overflow: hidden;
}
.prog-fill {
  height: 100%;
  background: var(--insp-accent);
  transition: width var(--insp-t-medium);
}
.row--overdue .prog-fill { background: var(--insp-fail); }
.prog-text {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  min-width: 32px;
  text-align: right;
}

.col-tasks {
  font-family: var(--insp-font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--insp-ink-primary);
}
.col-tasks .dim { color: var(--insp-ink-quaternary); font-weight: 400; }

.alert-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  font-weight: 600;
  color: var(--insp-fail);
  padding: 1px 6px;
  background: var(--insp-fail-pale);
  border: 1px solid var(--insp-fail-border);
  border-radius: 3px;
  white-space: nowrap;
}
.alert-tag--draft {
  color: var(--insp-warn);
  background: var(--insp-warn-pale);
  border-color: var(--insp-warn-border);
}

.col-actions {
  display: flex;
  gap: 4px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.dim { color: var(--insp-ink-quaternary); }

.empty {
  padding: 60px 20px;
  text-align: center;
}
.empty-title { font-size: 14px; font-weight: 600; color: var(--insp-ink-primary); margin: 0 0 4px; }
.empty-sub { font-size: 12px; color: var(--insp-ink-tertiary); margin: 0 0 14px; }

/* ============ Kanban view ============ */
.prj-kanban {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.bucket {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  display: flex;
  flex-direction: column;
  min-height: 360px;
}
.bucket--fail { border-top: 3px solid var(--insp-fail); }
.bucket--info { border-top: 3px solid var(--insp-info); }
.bucket--pass { border-top: 3px solid var(--insp-pass); }

.bucket-head {
  padding: 10px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.bucket-head__lead {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.bucket-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--insp-ink-primary);
}
.bucket-count {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--insp-accent);
}
.bucket-hint {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}

.bucket-body {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.kban-card {
  padding: 10px 12px;
  background: var(--insp-bg-page);
  border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-sm);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: all var(--insp-t-fast);
}
.kban-card:hover {
  border-color: var(--insp-accent);
  background: var(--insp-bg-surface);
}
.kban-card.is-overdue {
  border-color: var(--insp-fail-border);
  background: var(--insp-fail-pale);
}

.kban-line1 {
  display: flex; align-items: center; gap: 6px;
}
.kban-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kban-meta {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}
.kban-progress {
  display: flex; align-items: center; gap: 6px;
}
.kban-progress .prog-bar { height: 4px; }
.kban-progress__text {
  font-size: 10px;
  font-weight: 600;
  color: var(--insp-ink-secondary);
}
.kban-alert {
  display: flex;
  align-items: center;
}

.bucket-empty {
  padding: 30px 10px;
  text-align: center;
  font-size: 11px;
  color: var(--insp-ink-quaternary);
}

/* ============ Timeline view ============ */
.prj-timeline {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 10px 14px;
}
.tl-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 220px;
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}
.tl-axis-label {
  font-family: var(--insp-font-mono);
}
.tl-axis-spacer { flex: 1; border-bottom: 1px dashed var(--insp-border-default); height: 1px; }

.tl-grid {
  position: relative;
  border-top: 1px solid var(--insp-border-subtle);
}
.tl-today {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--insp-accent);
  z-index: 2;
  pointer-events: none;
  margin-left: 220px;
}
.tl-today__label {
  position: absolute;
  top: -16px;
  left: 4px;
  font-family: var(--insp-font-mono);
  font-size: 10px;
  font-weight: 600;
  color: var(--insp-accent);
}
.tl-row {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}
.tl-row:hover { background: var(--insp-bg-subtle); }
.tl-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--insp-ink-primary);
  padding: 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tl-track {
  position: relative;
  height: 22px;
  background: var(--insp-bg-subtle);
  border-radius: 3px;
}
.tl-bar {
  position: absolute;
  top: 2px; bottom: 2px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  font-family: var(--insp-font-mono);
  font-size: 10px;
  font-weight: 600;
  color: white;
  min-width: 36px;
  overflow: hidden;
  white-space: nowrap;
}
.tl-bar--info { background: var(--insp-info); }
.tl-bar--warn { background: var(--insp-warn); }
.tl-bar--fail { background: var(--insp-fail); }
.tl-bar--pass { background: var(--insp-pass); }
.tl-bar--pending { background: var(--insp-pending); }

@media (max-width: 1100px) {
  .row {
    grid-template-columns: 28px 2fr 70px 110px 100px 50px 70px 160px;
    gap: 6px;
    font-size: 11px;
  }
  .prj-kanban { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .row {
    grid-template-columns: 1fr;
    gap: 4px;
    padding: 10px;
  }
  .row--head { display: none; }
  .col-actions { justify-content: flex-start; }
  .prj-kpi { flex-wrap: wrap; }
}
</style>
