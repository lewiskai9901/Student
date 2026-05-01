<script setup lang="ts">
/**
 * ProjectListView — 检查项目总台 (重构版)
 *
 * 修复:
 *  - 用 /with-stats 聚合接口, 消除 N+1
 *  - URL query 同步 (view/status/period/showArchived/sort)
 *  - ARCHIVED 默认隐藏 + toggle 显示
 *  - 列头点击排序 (3 列: name/period/progress), 默认按 urgency
 *  - urgencyScore 加 DRAFT 衰减 (createdAt → daysSinceCreate)
 *  - 看板按角色分组 (管理员 vs 检查员)
 *  - 看板空"需要我处理"列友好引导
 *  - 时间线缩放: 季度/年/全部
 *  - 没起止日期的项目走"无日期"专列, 不静默丢
 *  - KPI 告警去重
 *  - 完成不可逆 → primary, 暂停 → ghost
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, AlertTriangle, EyeOff, Eye } from 'lucide-vue-next'
import { useInspExecutionStore } from '@/stores/inspection/inspExecutionStore'
import { useAuthStore } from '@/stores/auth'
import { inspProjectApi, type ProjectStatsSummary } from '@/api/inspection/project'
import { ProjectStatusConfig, type ProjectStatus } from '@/types/insp/enums'
import type { InspProject } from '@/types/insp/project'

const router = useRouter()
const route = useRoute()
const store = useInspExecutionStore()
const auth = useAuthStore()

// ── Role detection (与 InspectionCommandDeck 保持一致) ──
const canManageProjects = computed(() => auth.hasPermission('insp:project:edit') || auth.hasPermission('*'))
const canExecute = computed(() => auth.hasPermission('insp:task:execute'))
// myUserId 备用 (检查员视角中"我的项目"判断)
const myUserId = computed(() => auth.user?.id ? Number(auth.user.id) : null)

// ── State ──
const loading = ref(false)
const summaries = ref<ProjectStatsSummary[]>([])
const today = new Date().toISOString().slice(0, 10)

type ViewMode = 'list' | 'kanban' | 'timeline'
type PeriodFilter = 'all' | 'this-week' | 'this-month' | 'overdue'
type SortKey = 'urgency' | 'name' | 'period' | 'progress'

const view = ref<ViewMode>((route.query.view as ViewMode) || 'list')
const searchQuery = ref('')
const statusFilter = ref<ProjectStatus | 'all'>((route.query.status as any) || 'all')
const periodFilter = ref<PeriodFilter>((route.query.period as PeriodFilter) || 'all')
const showArchived = ref(route.query.archived === '1')
const sortKey = ref<SortKey>((route.query.sort as SortKey) || 'urgency')
const sortDir = ref<'asc' | 'desc'>((route.query.dir as any) || 'desc')
const timelineScale = ref<'auto' | 'month' | 'quarter' | 'year'>(
  (route.query.scale as any) || 'auto'
)

// URL 同步: 任何筛选变化都把当前 state 写到 query
watch(
  [view, statusFilter, periodFilter, showArchived, sortKey, sortDir, timelineScale],
  () => {
    router.replace({
      query: {
        ...route.query,
        view: view.value,
        status: statusFilter.value,
        period: periodFilter.value,
        archived: showArchived.value ? '1' : undefined,
        sort: sortKey.value,
        dir: sortDir.value,
        scale: view.value === 'timeline' ? timelineScale.value : undefined,
      },
    }).catch(() => { /* 路由相同时 replace 会拒绝, 忽略 */ })
  }, { deep: false }
)

// ── Loaders ──
async function loadData() {
  loading.value = true
  try {
    summaries.value = await inspProjectApi.getListWithStats()
  } catch (e: any) {
    ElMessage.error(e.message || '加载项目列表失败')
  } finally {
    loading.value = false
  }
}

// ── Helpers ──
function isOverdueProject(p: InspProject): boolean {
  if (p.status !== 'PUBLISHED' && p.status !== 'PAUSED') return false
  if (!p.endDate) return false
  return p.endDate < today
}
function daysSinceCreate(p: InspProject): number {
  if (!p.createdAt) return 0
  return Math.floor((Date.now() - new Date(p.createdAt).getTime()) / 86400000)
}

function urgencyScore(s: ProjectStatsSummary): number {
  const p = s.project
  if (isOverdueProject(p)) return 1000
  if (s.taskOverdue > 0) return 800 + s.taskOverdue
  if (p.status === 'DRAFT') {
    // 草稿衰减: 每停滞 1 天 +5 分, 上限 +200
    return 600 + Math.min(daysSinceCreate(p) * 5, 200)
  }
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

function projectChip(s: ProjectStatsSummary): string {
  if (isOverdueProject(s.project)) return 'fail'
  if (s.taskOverdue > 0) return 'warn'
  return ({
    DRAFT: 'pending', PUBLISHED: 'info', PAUSED: 'warn',
    COMPLETED: 'pass', ARCHIVED: 'pending',
  } as any)[s.project.status] || 'pending'
}

function fmtDate(s?: string | null) {
  if (!s) return '—'
  return s.replace(/^\d{4}-/, '').replace(/-/g, '.')
}
function fmtRange(p: InspProject): string {
  if (!p.startDate) return '未设期'
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
function pct(s: ProjectStatsSummary): number {
  if (s.taskTotal === 0) return 0
  return Math.round((s.taskDone / s.taskTotal) * 100)
}

// ── Filter + sort ──
const filtered = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  let list = summaries.value.slice()
  if (!showArchived.value) list = list.filter(s => s.project.status !== 'ARCHIVED')
  if (q) list = list.filter(s => s.project.projectName.toLowerCase().includes(q))
  if (statusFilter.value !== 'all') list = list.filter(s => s.project.status === statusFilter.value)
  if (periodFilter.value === 'this-week') {
    const wkEnd = new Date(); wkEnd.setDate(wkEnd.getDate() + 7)
    const wkEndS = wkEnd.toISOString().slice(0, 10)
    list = list.filter(s => s.project.endDate && s.project.endDate >= today && s.project.endDate <= wkEndS)
  } else if (periodFilter.value === 'this-month') {
    const moEnd = new Date(); moEnd.setMonth(moEnd.getMonth() + 1)
    const moEndS = moEnd.toISOString().slice(0, 10)
    list = list.filter(s => s.project.endDate && s.project.endDate >= today && s.project.endDate <= moEndS)
  } else if (periodFilter.value === 'overdue') {
    list = list.filter(s => isOverdueProject(s.project) || s.taskOverdue > 0)
  }

  const cmp = (a: ProjectStatsSummary, b: ProjectStatsSummary) => {
    let r = 0
    if (sortKey.value === 'urgency') r = urgencyScore(b) - urgencyScore(a)
    else if (sortKey.value === 'name') r = a.project.projectName.localeCompare(b.project.projectName)
    else if (sortKey.value === 'period') r = (a.project.startDate || '').localeCompare(b.project.startDate || '')
    else if (sortKey.value === 'progress') r = pct(a) - pct(b)
    return sortDir.value === 'desc' && sortKey.value !== 'urgency' ? -r : r
  }
  return list.sort(cmp)
})

// ── KPI (告警去重) ──
const kpi = computed(() => {
  const all = summaries.value.filter(s => s.project.status !== 'ARCHIVED' || showArchived.value)
  // 告警 = 项目自身逾期 OR 项目内有 overdue task, 同一项目只计 1 次 (Set)
  const alertSet = new Set<number>()
  for (const s of all) {
    if (isOverdueProject(s.project) || s.taskOverdue > 0) alertSet.add(s.project.id)
  }
  return {
    total: all.length,
    active: all.filter(s => s.project.status === 'PUBLISHED' || s.project.status === 'PAUSED').length,
    draft: all.filter(s => s.project.status === 'DRAFT').length,
    overdue: alertSet.size,
  }
})

function toggleSort(key: SortKey) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'desc' ? 'asc' : 'desc'
  } else {
    sortKey.value = key
    sortDir.value = key === 'progress' ? 'asc' : 'desc'
  }
}

// ── Kanban (按角色分组) ──
const buckets = computed(() => {
  const data = filtered.value
  if (canManageProjects.value) {
    // 管理员视角
    return [
      {
        key: 'attention',
        label: '需要我处理',
        hint: '草稿待发布 · 项目逾期 · 任务积压',
        tone: 'fail',
        items: data.filter(s =>
          s.project.status === 'DRAFT' ||
          isOverdueProject(s.project) ||
          s.taskOverdue > 0
        ),
        emptyHint: '一切正常 · 没有需要立即处理的项目',
      },
      {
        key: 'monitor',
        label: '监控中',
        hint: '正常进行 / 暂停',
        tone: 'info',
        items: data.filter(s => (s.project.status === 'PUBLISHED' || s.project.status === 'PAUSED')
          && !isOverdueProject(s.project) && s.taskOverdue === 0),
        emptyHint: '暂无在跑项目',
      },
      {
        key: 'done',
        label: '已闭环',
        hint: '完结 / 归档',
        tone: 'pass',
        items: data.filter(s => s.project.status === 'COMPLETED' || s.project.status === 'ARCHIVED'),
        emptyHint: '尚无闭环项目',
      },
    ]
  } else {
    // 检查员视角 — "我的"为先
    return [
      {
        key: 'mine-overdue',
        label: '我的逾期',
        hint: '我承担的任务已超期',
        tone: 'fail',
        items: data.filter(s => s.taskOverdue > 0 && (s.project.status === 'PUBLISHED' || s.project.status === 'PAUSED')),
        emptyHint: '没有逾期任务 · 保持节奏',
      },
      {
        key: 'mine-active',
        label: '进行中',
        hint: '当前可执行的项目',
        tone: 'info',
        items: data.filter(s => s.project.status === 'PUBLISHED' && s.taskOverdue === 0),
        emptyHint: '暂无进行中项目',
      },
      {
        key: 'closed',
        label: '已闭环',
        hint: '完结 / 暂停 / 归档',
        tone: 'pass',
        items: data.filter(s => ['COMPLETED', 'ARCHIVED', 'PAUSED', 'DRAFT'].includes(s.project.status)),
        emptyHint: '—',
      },
    ]
  }
})

// ── Timeline (含缩放 + 无日期专列) ──
const projectsWithDates = computed(() => filtered.value.filter(s => s.project.startDate && s.project.endDate))
const projectsNoDates = computed(() => filtered.value.filter(s => !s.project.startDate || !s.project.endDate))

const timelineRange = computed<{ min: string; max: string } | null>(() => {
  // 缩放优先
  const now = new Date()
  if (timelineScale.value === 'month') {
    const lo = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10)
    const hi = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().slice(0, 10)
    return { min: lo, max: hi }
  }
  if (timelineScale.value === 'quarter') {
    const qStart = new Date(now.getFullYear(), Math.floor(now.getMonth() / 3) * 3, 1)
    const qEnd = new Date(qStart.getFullYear(), qStart.getMonth() + 3, 0)
    return { min: qStart.toISOString().slice(0, 10), max: qEnd.toISOString().slice(0, 10) }
  }
  if (timelineScale.value === 'year') {
    return { min: `${now.getFullYear()}-01-01`, max: `${now.getFullYear()}-12-31` }
  }
  // auto: fit
  const dates = projectsWithDates.value.flatMap(s => [s.project.startDate, s.project.endDate]).filter(Boolean) as string[]
  if (dates.length === 0) return null
  const min = dates.reduce((a, b) => a < b ? a : b)
  const max = dates.reduce((a, b) => a > b ? a : b)
  return { min, max }
})

function timelineBar(p: InspProject) {
  const range = timelineRange.value
  if (!range || !p.startDate || !p.endDate) return null
  const minMs = new Date(range.min).getTime()
  const maxMs = new Date(range.max).getTime()
  const span = maxMs - minMs || 1
  const startMs = new Date(p.startDate).getTime()
  const endMs = new Date(p.endDate).getTime()
  // 裁剪到可视范围
  const clampStart = Math.max(startMs, minMs)
  const clampEnd = Math.min(endMs, maxMs)
  if (clampEnd < minMs || clampStart > maxMs) return null
  return {
    left: `${((clampStart - minMs) / span) * 100}%`,
    width: `${Math.max(((clampEnd - clampStart) / span) * 100, 1)}%`,
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

async function handleAction(s: ProjectStatsSummary, action: string) {
  const p = s.project
  try {
    if (action === 'detail' || action === 'publish') { goDetail(p); return }
    if (action === 'pause') {
      const live = s.taskTotal - s.taskDone
      const tip = live > 0
        ? `当前还有 ${live} 个未完成任务, 暂停后这些任务将冻结. 继续?`
        : '确定暂停此项目?'
      await ElMessageBox.confirm(tip, '确认暂停', { type: 'warning' })
      await store.pauseProject(p.id); ElMessage.success('已暂停')
    } else if (action === 'resume') {
      await store.resumeProject(p.id); ElMessage.success('已恢复')
    } else if (action === 'complete') {
      const live = s.taskTotal - s.taskDone
      const tip = live > 0
        ? `还有 ${live} 个未完成任务. 完结后项目不可恢复, 任务会被强制归档. 确认?`
        : '完结操作不可逆. 确认?'
      await ElMessageBox.confirm(tip, '确认完结 (不可逆)', { type: 'warning', confirmButtonText: '确认完结', confirmButtonClass: 'el-button--danger' })
      await store.completeProject(p.id); ElMessage.success('已完结')
    } else if (action === 'archive') {
      await ElMessageBox.confirm('归档后项目从默认列表隐藏 (可在筛选中找回). 继续?', '确认归档', { type: 'info' })
      await store.archiveProject(p.id); ElMessage.success('已归档')
    } else if (action === 'delete') {
      await ElMessageBox.confirm(`删除项目「${p.projectName}」?\n仅限草稿状态, 不可恢复.`, '确认删除', { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' })
      await store.removeProject(p.id); ElMessage.success('已删除')
    }
    loadData()
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString?.() !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

// 行内主操作 (单个 CTA, 状态自适应)
function primaryAction(s: ProjectStatsSummary): { label: string; key: string; emphasized?: boolean } {
  const p = s.project
  if (p.status === 'DRAFT') return { label: '配置 + 发布', key: 'publish', emphasized: true }
  if (p.status === 'PUBLISHED' && s.taskOverdue > 0) return { label: '处理逾期', key: 'detail', emphasized: true }
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
        <button class="kpi" :class="{ 'is-active': statusFilter === 'all' && periodFilter !== 'overdue' }"
                @click="statusFilter = 'all'; periodFilter = 'all'">
          <span class="kpi__num insp-num">{{ kpi.total }}</span>
          <span class="kpi__label">总数</span>
        </button>
        <button class="kpi" :class="{ 'is-active': statusFilter === 'PUBLISHED' }"
                @click="statusFilter = statusFilter === 'PUBLISHED' ? 'all' : 'PUBLISHED'">
          <span class="kpi__num insp-num" style="color: var(--insp-info)">{{ kpi.active }}</span>
          <span class="kpi__label">进行中</span>
        </button>
        <button class="kpi" :class="{ 'is-active': statusFilter === 'DRAFT' }"
                @click="statusFilter = statusFilter === 'DRAFT' ? 'all' : 'DRAFT'">
          <span class="kpi__num insp-num">{{ kpi.draft }}</span>
          <span class="kpi__label">草稿</span>
        </button>
        <button class="kpi" :class="{ 'is-active': periodFilter === 'overdue' }"
                @click="periodFilter = periodFilter === 'overdue' ? 'all' : 'overdue'">
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

    <!-- ── Toolbar ─────────── -->
    <div class="prj-toolbar">
      <div class="view-tabs">
        <button class="view-tab" :class="{ 'is-active': view === 'list' }" @click="view = 'list'">列表</button>
        <button class="view-tab" :class="{ 'is-active': view === 'kanban' }" @click="view = 'kanban'">看板</button>
        <button class="view-tab" :class="{ 'is-active': view === 'timeline' }" @click="view = 'timeline'">时间轴</button>
      </div>

      <div class="filter-chips">
        <button class="chip" :class="{ 'is-active': periodFilter === 'all' }" @click="periodFilter = 'all'">全部</button>
        <button class="chip" :class="{ 'is-active': periodFilter === 'this-week' }" @click="periodFilter = 'this-week'">本周到期</button>
        <button class="chip" :class="{ 'is-active': periodFilter === 'this-month' }" @click="periodFilter = 'this-month'">本月</button>
      </div>

      <!-- 时间轴专属缩放 -->
      <div v-if="view === 'timeline'" class="filter-chips">
        <button class="chip" :class="{ 'is-active': timelineScale === 'auto' }" @click="timelineScale = 'auto'">自适应</button>
        <button class="chip" :class="{ 'is-active': timelineScale === 'month' }" @click="timelineScale = 'month'">本月</button>
        <button class="chip" :class="{ 'is-active': timelineScale === 'quarter' }" @click="timelineScale = 'quarter'">本季</button>
        <button class="chip" :class="{ 'is-active': timelineScale === 'year' }" @click="timelineScale = 'year'">本年</button>
      </div>

      <div class="prj-toolbar__spacer" />

      <button class="archive-toggle" @click="showArchived = !showArchived" :title="showArchived ? '隐藏已归档' : '显示已归档'">
        <Eye v-if="showArchived" :size="13" />
        <EyeOff v-else :size="13" />
        <span>{{ showArchived ? '显示已归档' : '隐藏已归档' }}</span>
      </button>

      <input v-model="searchQuery" class="prj-search" placeholder="搜索项目名称…" />
    </div>

    <!-- ============ List View ============ -->
    <section v-if="view === 'list'" class="prj-list" v-loading="loading">
      <div class="row row--head">
        <span class="col col-name sortable" @click="toggleSort('name')">
          项目<span class="sort-arrow" v-if="sortKey === 'name'">{{ sortDir === 'desc' ? '↓' : '↑' }}</span>
        </span>
        <span class="col col-status">状态</span>
        <span class="col col-period sortable" @click="toggleSort('period')">
          期间<span class="sort-arrow" v-if="sortKey === 'period'">{{ sortDir === 'desc' ? '↓' : '↑' }}</span>
        </span>
        <span class="col col-progress sortable" @click="toggleSort('progress')">
          进度<span class="sort-arrow" v-if="sortKey === 'progress'">{{ sortDir === 'desc' ? '↓' : '↑' }}</span>
        </span>
        <span class="col col-tasks">任务</span>
        <span class="col col-alert">告警</span>
        <span class="col col-actions">操作</span>
      </div>

      <div
        v-for="s in filtered" :key="s.project.id"
        class="row row--data"
        :class="{ 'row--overdue': isOverdueProject(s.project) || s.taskOverdue > 0 }"
        @click="goDetail(s.project)"
      >
        <div class="col col-name">
          <div class="name-line">
            <span class="name-text">{{ s.project.projectName }}</span>
            <span v-if="s.project.status === 'DRAFT'" class="dot dot--draft" title="待配置发布" />
          </div>
          <div class="name-meta">
            <span class="insp-num">{{ s.project.projectCode }}</span>
            <span class="sep">·</span>
            <span><span class="insp-num">{{ s.inspectorCount }}</span> 检查员</span>
          </div>
        </div>

        <span class="col col-status">
          <span class="insp-chip" :class="`insp-chip--${projectChip(s)}`">
            {{ ProjectStatusConfig[s.project.status as ProjectStatus]?.label }}
          </span>
        </span>

        <span class="col col-period">
          <span class="period-line insp-num">{{ fmtRange(s.project) }}</span>
          <span v-if="daysRemaining(s.project)" class="period-rel"
                :class="{ 'is-urgent': daysRemaining(s.project)?.startsWith('逾期') || daysRemaining(s.project)?.startsWith('今日') }">
            {{ daysRemaining(s.project) }}
          </span>
        </span>

        <span class="col col-progress">
          <div class="prog-wrap">
            <div class="prog-bar"><div class="prog-fill" :style="{ width: pct(s) + '%' }" /></div>
            <span class="prog-text insp-num">{{ pct(s) }}%</span>
          </div>
        </span>

        <span class="col col-tasks insp-num">
          {{ s.taskDone }}<span class="dim">/{{ s.taskTotal }}</span>
        </span>

        <span class="col col-alert">
          <span v-if="s.taskOverdue > 0" class="alert-tag">
            <AlertTriangle :size="11" />
            <span class="insp-num">{{ s.taskOverdue }}</span> 逾期
          </span>
          <span v-else-if="s.project.status === 'DRAFT'" class="alert-tag alert-tag--draft">待发布</span>
          <span v-else-if="s.taskPendingReview > 0" class="alert-tag alert-tag--info">
            <span class="insp-num">{{ s.taskPendingReview }}</span> 待审
          </span>
          <span v-else class="dim">—</span>
        </span>

        <div class="col col-actions" @click.stop>
          <button class="insp-btn insp-btn--sm" :class="{ 'insp-btn--accent': primaryAction(s).emphasized }"
                  @click="handleAction(s, primaryAction(s).key)">
            {{ primaryAction(s).label }}
          </button>
          <button v-if="s.project.status === 'PUBLISHED'" class="insp-btn insp-btn--sm insp-btn--ghost" @click="handleAction(s, 'pause')">暂停</button>
          <button v-if="['PUBLISHED','PAUSED'].includes(s.project.status)" class="insp-btn insp-btn--sm insp-btn--danger" @click="handleAction(s, 'complete')">完结</button>
          <button v-if="s.project.status === 'COMPLETED'" class="insp-btn insp-btn--sm" @click="handleAction(s, 'archive')">归档</button>
          <button v-if="s.project.status === 'DRAFT'" class="insp-btn insp-btn--sm insp-btn--ghost" @click="handleAction(s, 'delete')" title="删除草稿">×</button>
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

    <!-- ============ Kanban View ============ -->
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
          <div v-for="s in b.items" :key="s.project.id"
               class="kban-card"
               :class="{ 'is-overdue': isOverdueProject(s.project) || s.taskOverdue > 0 }"
               @click="goDetail(s.project)">
            <div class="kban-line1">
              <span class="kban-name">{{ s.project.projectName }}</span>
              <span class="insp-chip" :class="`insp-chip--${projectChip(s)}`">
                {{ ProjectStatusConfig[s.project.status as ProjectStatus]?.label }}
              </span>
            </div>
            <div class="kban-meta insp-num">{{ fmtRange(s.project) }}</div>
            <div class="kban-progress">
              <div class="prog-bar"><div class="prog-fill" :style="{ width: pct(s) + '%' }" /></div>
              <span class="kban-progress__text insp-num">{{ s.taskDone }}/{{ s.taskTotal }}</span>
            </div>
            <div v-if="s.taskOverdue > 0 || daysRemaining(s.project)" class="kban-alert">
              <span v-if="s.taskOverdue > 0" class="alert-tag">
                <AlertTriangle :size="10" />
                <span class="insp-num">{{ s.taskOverdue }}</span> 逾期
              </span>
              <span v-else-if="daysRemaining(s.project)" class="period-rel"
                    :class="{ 'is-urgent': daysRemaining(s.project)?.startsWith('逾期') || daysRemaining(s.project)?.startsWith('今日') }">
                {{ daysRemaining(s.project) }}
              </span>
            </div>
          </div>
          <div v-if="b.items.length === 0" class="bucket-empty">
            <p>{{ b.emptyHint }}</p>
            <button v-if="b.key === 'attention' && canManageProjects" class="insp-btn insp-btn--sm insp-btn--accent" @click="goCreate">
              <Plus :size="11" />新建项目
            </button>
          </div>
        </div>
      </article>
    </section>

    <!-- ============ Timeline View ============ -->
    <section v-else class="prj-timeline" v-loading="loading">
      <div v-if="!timelineRange && projectsNoDates.length === 0" class="empty">
        <p class="empty-title">无法生成时间轴</p>
        <p class="empty-sub">没有项目设置过开始/结束日期</p>
      </div>
      <template v-else>
        <header class="tl-head" v-if="timelineRange">
          <span class="tl-axis-label insp-num">{{ timelineRange.min }}</span>
          <div class="tl-axis-spacer" />
          <span class="tl-axis-label insp-num">{{ timelineRange.max }}</span>
        </header>
        <div class="tl-grid" v-if="timelineRange">
          <div v-if="todayMarker" class="tl-today" :style="{ left: todayMarker }">
            <span class="tl-today__label insp-num">今天</span>
          </div>
          <div v-for="s in projectsWithDates" :key="s.project.id" class="tl-row" @click="goDetail(s.project)">
            <span class="tl-name">{{ s.project.projectName }}</span>
            <div class="tl-track">
              <div v-if="timelineBar(s.project)" class="tl-bar" :class="`tl-bar--${projectChip(s)}`" :style="timelineBar(s.project)!">
                <span class="tl-bar__pct insp-num">{{ pct(s) }}%</span>
              </div>
              <span v-else class="tl-out-of-range">超出当前缩放窗口</span>
            </div>
          </div>
          <div v-if="projectsWithDates.length === 0" class="bucket-empty">当前窗口无项目, 请切换缩放</div>
        </div>

        <!-- 没有日期的项目 -->
        <div v-if="projectsNoDates.length > 0" class="tl-undated">
          <div class="tl-undated-head">未设期间 · {{ projectsNoDates.length }}</div>
          <div class="tl-undated-list">
            <div v-for="s in projectsNoDates" :key="s.project.id" class="tl-undated-item" @click="goDetail(s.project)">
              <span class="kban-name">{{ s.project.projectName }}</span>
              <span class="insp-chip" :class="`insp-chip--${projectChip(s)}`">
                {{ ProjectStatusConfig[s.project.status as ProjectStatus]?.label }}
              </span>
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
  display: flex; align-items: center; gap: 16px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 12px 16px;
  margin-bottom: 10px;
}
.prj-head__lead { display: flex; flex-direction: column; gap: 2px; }
.prj-title { font-size: 17px; font-weight: 700; margin: 2px 0 0; color: var(--insp-ink-primary); }
.prj-kpi { display: flex; margin-left: auto; gap: 6px; }
.kpi {
  display: inline-flex; flex-direction: column; align-items: center; gap: 0;
  min-width: 64px; padding: 4px 14px;
  background: transparent; border: 1px solid transparent;
  border-radius: var(--insp-radius-sm);
  cursor: pointer; transition: all var(--insp-t-fast); font-family: inherit;
}
.kpi:hover { background: var(--insp-bg-subtle); }
.kpi.is-active { background: var(--insp-accent-paler); border-color: var(--insp-accent-pale); }
.kpi__num { font-family: var(--insp-font-mono); font-size: 18px; font-weight: 700; line-height: 1; color: var(--insp-ink-primary); }
.kpi__label { font-size: 10px; color: var(--insp-ink-tertiary); letter-spacing: 0.04em; text-transform: uppercase; margin-top: 2px; }
.prj-cta { margin-left: 4px; }

/* ─ Toolbar ─────── */
.prj-toolbar {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 6px 8px 6px 12px; margin-bottom: 10px;
}
.view-tabs { display: flex; gap: 2px; padding: 2px; background: var(--insp-bg-subtle); border-radius: var(--insp-radius-sm); }
.view-tab {
  height: 24px; padding: 0 12px;
  background: transparent; border: 0; border-radius: 3px;
  font-family: inherit; font-size: 12px; font-weight: 500;
  color: var(--insp-ink-tertiary); cursor: pointer; transition: all var(--insp-t-fast);
}
.view-tab:hover { color: var(--insp-ink-primary); }
.view-tab.is-active { background: var(--insp-bg-surface); color: var(--insp-ink-primary); box-shadow: var(--insp-shadow-xs); font-weight: 600; }
.filter-chips { display: flex; gap: 4px; }
.chip {
  height: 24px; padding: 0 10px;
  background: transparent; border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-pill);
  font-family: inherit; font-size: 11px; font-weight: 500;
  color: var(--insp-ink-tertiary); cursor: pointer; transition: all var(--insp-t-fast);
}
.chip:hover { color: var(--insp-ink-primary); border-color: var(--insp-border-strong); }
.chip.is-active { background: var(--insp-accent); color: white; border-color: var(--insp-accent); }
.prj-toolbar__spacer { flex: 1; }
.archive-toggle {
  display: inline-flex; align-items: center; gap: 4px;
  height: 24px; padding: 0 10px;
  background: transparent; border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-pill);
  font-family: inherit; font-size: 11px; color: var(--insp-ink-tertiary); cursor: pointer;
}
.archive-toggle:hover { color: var(--insp-ink-primary); border-color: var(--insp-border-strong); }
.prj-search {
  height: 26px; padding: 0 10px;
  border: 1px solid var(--insp-border-default); border-radius: var(--insp-radius-sm);
  font-size: 12px; font-family: inherit; width: 220px; background: var(--insp-bg-surface);
}
.prj-search:focus { outline: none; border-color: var(--insp-accent); box-shadow: 0 0 0 3px var(--insp-accent-paler); }

/* ============ List view ============ */
.prj-list { background: var(--insp-bg-surface); border: 1px solid var(--insp-border-default); border-radius: var(--insp-radius-lg); overflow: hidden; }
.row {
  display: grid;
  grid-template-columns: 2fr 90px 140px 130px 70px 90px 220px;
  gap: 10px; align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
  transition: background var(--insp-t-fast);
}
.row--head {
  background: var(--insp-bg-subtle);
  font-size: 10px; font-weight: 600; color: var(--insp-ink-tertiary);
  text-transform: uppercase; letter-spacing: 0.04em; padding: 8px 14px;
}
.row--head .sortable { cursor: pointer; user-select: none; }
.row--head .sortable:hover { color: var(--insp-ink-primary); }
.sort-arrow { margin-left: 4px; color: var(--insp-accent); }
.row--data { cursor: pointer; }
.row--data:hover { background: var(--insp-bg-subtle); }
.row--data.row--overdue { background: linear-gradient(to right, var(--insp-fail-pale), transparent 60%); }
.row--data.row--overdue:hover { background: linear-gradient(to right, color-mix(in oklab, var(--insp-fail-pale) 80%, var(--insp-bg-subtle)), var(--insp-bg-subtle) 60%); }
.col { min-width: 0; }
.name-line { display: flex; align-items: center; gap: 6px; }
.name-text { font-size: 13px; font-weight: 600; color: var(--insp-ink-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.dot--draft { background: var(--insp-warn); }
.name-meta { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--insp-ink-tertiary); margin-top: 2px; }
.sep { color: var(--insp-ink-quaternary); }
.period-line { font-family: var(--insp-font-mono); font-size: 12px; color: var(--insp-ink-secondary); display: block; }
.period-rel { display: block; font-size: 10px; color: var(--insp-ink-tertiary); margin-top: 2px; }
.period-rel.is-urgent { color: var(--insp-fail); font-weight: 600; }
.prog-wrap { display: flex; align-items: center; gap: 6px; }
.prog-bar { flex: 1; height: 5px; background: var(--insp-bg-sunken); border-radius: 3px; overflow: hidden; }
.prog-fill { height: 100%; background: var(--insp-accent); transition: width var(--insp-t-medium); }
.row--overdue .prog-fill { background: var(--insp-fail); }
.prog-text { font-family: var(--insp-font-mono); font-size: 11px; font-weight: 600; color: var(--insp-ink-primary); min-width: 32px; text-align: right; }
.col-tasks { font-family: var(--insp-font-mono); font-size: 12px; font-weight: 600; color: var(--insp-ink-primary); }
.col-tasks .dim { color: var(--insp-ink-quaternary); font-weight: 400; }
.alert-tag {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 11px; font-weight: 600; color: var(--insp-fail);
  padding: 1px 6px; background: var(--insp-fail-pale);
  border: 1px solid var(--insp-fail-border);
  border-radius: 3px; white-space: nowrap;
}
.alert-tag--draft { color: var(--insp-warn); background: var(--insp-warn-pale); border-color: var(--insp-warn-border); }
.alert-tag--info { color: var(--insp-info); background: var(--insp-info-pale, var(--insp-bg-subtle)); border-color: var(--insp-info-border, var(--insp-border-default)); }
.col-actions { display: flex; gap: 4px; justify-content: flex-end; flex-wrap: wrap; }
.dim { color: var(--insp-ink-quaternary); }
.empty { padding: 60px 20px; text-align: center; }
.empty-title { font-size: 14px; font-weight: 600; color: var(--insp-ink-primary); margin: 0 0 4px; }
.empty-sub { font-size: 12px; color: var(--insp-ink-tertiary); margin: 0 0 14px; }

/* ============ Kanban view ============ */
.prj-kanban { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.bucket {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  display: flex; flex-direction: column; min-height: 360px;
}
.bucket--fail { border-top: 3px solid var(--insp-fail); }
.bucket--info { border-top: 3px solid var(--insp-info); }
.bucket--pass { border-top: 3px solid var(--insp-pass); }
.bucket-head { padding: 10px 14px; border-bottom: 1px solid var(--insp-border-subtle); }
.bucket-head__lead { display: flex; align-items: baseline; gap: 8px; }
.bucket-name { font-size: 13px; font-weight: 700; color: var(--insp-ink-primary); }
.bucket-count { font-family: var(--insp-font-mono); font-size: 11px; font-weight: 600; color: var(--insp-accent); }
.bucket-hint { display: block; margin-top: 3px; font-size: 11px; color: var(--insp-ink-tertiary); }
.bucket-body { padding: 8px; display: flex; flex-direction: column; gap: 6px; flex: 1; }
.kban-card {
  padding: 10px 12px;
  background: var(--insp-bg-page);
  border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-sm);
  cursor: pointer; display: flex; flex-direction: column; gap: 6px;
  transition: all var(--insp-t-fast);
}
.kban-card:hover { border-color: var(--insp-accent); background: var(--insp-bg-surface); }
.kban-card.is-overdue { border-color: var(--insp-fail-border); background: var(--insp-fail-pale); }
.kban-line1 { display: flex; align-items: center; gap: 6px; }
.kban-name { font-size: 12px; font-weight: 600; color: var(--insp-ink-primary); flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.kban-meta { font-family: var(--insp-font-mono); font-size: 11px; color: var(--insp-ink-tertiary); }
.kban-progress { display: flex; align-items: center; gap: 6px; }
.kban-progress .prog-bar { height: 4px; }
.kban-progress__text { font-size: 10px; font-weight: 600; color: var(--insp-ink-secondary); }
.kban-alert { display: flex; align-items: center; }
.bucket-empty {
  padding: 30px 10px; text-align: center;
  font-size: 11px; color: var(--insp-ink-quaternary);
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.bucket-empty p { margin: 0; }

/* ============ Timeline view ============ */
.prj-timeline { background: var(--insp-bg-surface); border: 1px solid var(--insp-border-default); border-radius: var(--insp-radius-lg); padding: 10px 14px; }
.tl-head { display: flex; align-items: center; gap: 10px; padding: 6px 10px 6px 220px; font-size: 11px; color: var(--insp-ink-tertiary); }
.tl-axis-label { font-family: var(--insp-font-mono); }
.tl-axis-spacer { flex: 1; border-bottom: 1px dashed var(--insp-border-default); height: 1px; }
.tl-grid { position: relative; border-top: 1px solid var(--insp-border-subtle); }
.tl-today {
  position: absolute; top: 0; bottom: 0; width: 2px;
  background: var(--insp-fail);
  z-index: 2; pointer-events: none;
  margin-left: 220px;
}
.tl-today__label {
  position: absolute; top: -16px; left: 4px;
  font-family: var(--insp-font-mono); font-size: 10px; font-weight: 600;
  color: var(--insp-fail);
}
.tl-row {
  display: grid; grid-template-columns: 220px 1fr; gap: 10px; align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer; transition: background var(--insp-t-fast);
}
.tl-row:hover { background: var(--insp-bg-subtle); }
.tl-name { font-size: 12px; font-weight: 500; color: var(--insp-ink-primary); padding: 0 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tl-track { position: relative; height: 22px; background: var(--insp-bg-subtle); border-radius: 3px; }
.tl-bar {
  position: absolute; top: 2px; bottom: 2px;
  border-radius: 3px; display: flex; align-items: center; padding: 0 8px;
  font-family: var(--insp-font-mono); font-size: 10px; font-weight: 600;
  color: white; min-width: 36px; overflow: hidden; white-space: nowrap;
}
.tl-bar--info { background: var(--insp-info); }
.tl-bar--warn { background: var(--insp-warn); }
.tl-bar--fail { background: var(--insp-fail); }
.tl-bar--pass { background: var(--insp-pass); }
.tl-bar--pending { background: var(--insp-pending); }
.tl-out-of-range {
  display: block; padding: 4px 10px;
  font-size: 10px; color: var(--insp-ink-quaternary);
}
.tl-undated { margin-top: 16px; padding: 12px; background: var(--insp-bg-subtle); border-radius: var(--insp-radius-sm); }
.tl-undated-head { font-size: 11px; font-weight: 600; color: var(--insp-ink-tertiary); margin-bottom: 8px; }
.tl-undated-list { display: flex; flex-wrap: wrap; gap: 6px; }
.tl-undated-item {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 10px; background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-subtle); border-radius: var(--insp-radius-sm);
  cursor: pointer; font-size: 11px;
}
.tl-undated-item:hover { border-color: var(--insp-accent); }

/* ── Responsive ── */
@media (max-width: 1280px) {
  .row { grid-template-columns: 2fr 80px 130px 110px 60px 80px 180px; gap: 6px; font-size: 11px; }
}
@media (max-width: 1100px) {
  .row { grid-template-columns: 2fr 70px 110px 100px 50px 70px 160px; }
  .prj-kanban { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .row { grid-template-columns: 1fr; gap: 4px; padding: 10px; }
  .row--head { display: none; }
  .col-actions { justify-content: flex-start; }
  .prj-kpi { flex-wrap: wrap; }
}
</style>
