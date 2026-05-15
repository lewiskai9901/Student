<script setup lang="ts">
/**
 * MyInspectionView — 我的检查任务 (Audit Console redesign)
 * 卷宗式分组列表 · 状态筛选 · 内联快速操作 · 键盘快捷键
 */
import type { LongId } from '@/types/common'
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  getMyTasks, getAvailableTasks,
  claimTask, withdrawTask, startTask,
} from '@/api/inspection/task'
import { getProjects } from '@/api/inspection/project'
import { http } from '@/utils/request'
import type { InspTask, InspProject } from '@/types/insp/project'
import { TaskStatusConfig, type TaskStatus } from '@/types/insp/enums'

const router = useRouter()
const authStore = useAuthStore()

// ── State ──
const loading = ref(false)
const myTaskList = ref<InspTask[]>([])
const availableTaskList = ref<InspTask[]>([])
const projectMap = ref<Map<LongId, InspProject>>(new Map())
const claimingIds = ref<Set<LongId>>(new Set())
const filter = ref<'all' | 'available' | 'mine' | 'inprogress' | 'toreview' | 'overdue' | 'adhoc'>('all')

// V108: 抽查发起对话框
const adhocDialog = ref(false)
const adhocSubmitting = ref(false)
const adhocLoadingProjects = ref(false)
const adhocProjects = ref<{ id: LongId; projectName: string }[]>([])
const adhocForm = reactive({ projectId: null as LongId | null, reason: '' })

async function loadAdhocProjects() {
  adhocLoadingProjects.value = true
  try {
    const list = await http.get<any[]>('/inspection/tasks/ad-hoc/allowed-projects')
    adhocProjects.value = ((list as any) || []).map((p: any) => ({
      id: p.id,
      projectName: p.project_name || p.projectName || ('项目 #' + p.id),
    }))
  } catch { adhocProjects.value = [] }
  finally { adhocLoadingProjects.value = false }
}

async function submitAdhoc() {
  if (!adhocForm.projectId) { ElMessage.warning('请选择项目'); return }
  if (!adhocForm.reason || adhocForm.reason.length < 10) { ElMessage.warning('请填写至少 10 字的发起原因'); return }
  adhocSubmitting.value = true
  try {
    const r = await http.post<any>('/inspection/tasks/ad-hoc', {
      projectId: adhocForm.projectId,
      reason: adhocForm.reason,
    })
    ElMessage.success('抽查任务已发起 #' + (r as any).taskCode)
    adhocDialog.value = false
    adhocForm.projectId = null
    adhocForm.reason = ''
    await loadAll()
    filter.value = 'adhoc'
  } catch (e: any) {
    ElMessage.error('发起失败: ' + (e?.response?.data?.message || e?.message || '未知'))
  } finally {
    adhocSubmitting.value = false
  }
}

// 当前用户角色判定: 我在该任务里是检查员还是审核员?
function roleInTask(t: InspTask): 'inspector' | 'reviewer' | null {
  const me = authStore.user?.userId ?? authStore.user?.id
  if (!me) return null
  if (t.inspectorId === String(me)) return 'inspector'
  if (t.reviewerId === String(me)) return 'reviewer'
  return null
}
function isReviewable(t: InspTask): boolean {
  return roleInTask(t) === 'reviewer' &&
    (t.status === 'SUBMITTED' || t.status === 'UNDER_REVIEW')
}

// ── Today ──
const today = new Date().toISOString().slice(0, 10)

// ── Loaders ──
async function loadAll() {
  loading.value = true
  try {
    const [mine, available, projects] = await Promise.all([
      getMyTasks().catch(() => []),
      getAvailableTasks().catch(() => []),
      getProjects({}).catch(() => []),
    ])
    myTaskList.value = mine || []
    availableTaskList.value = available || []
    const m = new Map<LongId, InspProject>()
    for (const p of projects || []) m.set(p.id, p)
    projectMap.value = m
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ── Combined unique tasks ──
const allTasks = computed(() => {
  const map = new Map<LongId, InspTask>()
  for (const t of myTaskList.value) map.set(t.id, t)
  for (const t of availableTaskList.value) map.set(t.id, t)
  return Array.from(map.values())
})

const counts = computed(() => {
  const all = allTasks.value
  return {
    all: all.length,
    available: availableTaskList.value.length,
    mine: myTaskList.value.length,
    inprogress: myTaskList.value.filter(t => t.status === 'IN_PROGRESS').length,
    toreview: myTaskList.value.filter(isReviewable).length,
    overdue: allTasks.value.filter(isOverdue).length,
    adhoc: allTasks.value.filter((t: any) => t.taskType === 'AD_HOC').length,
  }
})

const filtered = computed(() => {
  let list: InspTask[]
  switch (filter.value) {
    case 'available': list = availableTaskList.value; break
    case 'mine': list = myTaskList.value; break
    case 'inprogress': list = myTaskList.value.filter(t => t.status === 'IN_PROGRESS'); break
    case 'toreview': list = myTaskList.value.filter(isReviewable); break
    case 'overdue': list = allTasks.value.filter(isOverdue); break
    case 'adhoc': list = allTasks.value.filter((t: any) => t.taskType === 'AD_HOC'); break
    default: list = allTasks.value
  }
  return [...list].sort((a, b) => {
    // overdue first
    const aOver = isOverdue(a) ? 0 : 1
    const bOver = isOverdue(b) ? 0 : 1
    if (aOver !== bOver) return aOver - bOver
    // by taskDate ascending
    return (a.taskDate || '').localeCompare(b.taskDate || '')
  })
})

// Group by date bucket
interface DateBucket { key: string; label: string; tasks: InspTask[] }
const grouped = computed((): DateBucket[] => {
  const buckets: Record<string, InspTask[]> = {
    overdue: [], today: [], tomorrow: [], thisWeek: [], later: [],
  }
  const todayD = new Date(today)
  const tomorrow = new Date(todayD); tomorrow.setDate(tomorrow.getDate() + 1)
  const weekEnd = new Date(todayD); weekEnd.setDate(weekEnd.getDate() + 7)

  for (const t of filtered.value) {
    if (isOverdue(t)) { buckets.overdue.push(t); continue }
    if (!t.taskDate) { buckets.later.push(t); continue }
    const d = new Date(t.taskDate)
    if (t.taskDate === today) buckets.today.push(t)
    else if (t.taskDate === tomorrow.toISOString().slice(0, 10)) buckets.tomorrow.push(t)
    else if (d <= weekEnd) buckets.thisWeek.push(t)
    else buckets.later.push(t)
  }

  const out: DateBucket[] = []
  if (buckets.overdue.length) out.push({ key: 'overdue', label: '已逾期', tasks: buckets.overdue })
  if (buckets.today.length) out.push({ key: 'today', label: '今天', tasks: buckets.today })
  if (buckets.tomorrow.length) out.push({ key: 'tomorrow', label: '明天', tasks: buckets.tomorrow })
  if (buckets.thisWeek.length) out.push({ key: 'thisWeek', label: '本周', tasks: buckets.thisWeek })
  if (buckets.later.length) out.push({ key: 'later', label: '稍后', tasks: buckets.later })
  return out
})

// ── S+ 搜索高亮 ──
const searchKeyword = ref('')
function highlightHtml(text: string, kw: string): string {
  if (!kw) return text
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark class="my-mark">$1</mark>')
}

// ── S+ 任务悬浮预览卡 ──
const previewVisible = ref(false)
const previewTarget = ref<InspTask | null>(null)
const previewPos = ref({ top: '0px', left: '0px' })
let previewShowTimer: any = null
let previewHideTimer: any = null
function showTaskPreview(t: InspTask, e: MouseEvent) {
  clearTimeout(previewHideTimer)
  clearTimeout(previewShowTimer)
  const target = e.currentTarget as HTMLElement
  previewShowTimer = setTimeout(() => {
    previewTarget.value = t
    const rect = target.getBoundingClientRect()
    const popoverWidth = 300
    if (rect.right + popoverWidth + 16 < window.innerWidth) {
      previewPos.value = { top: `${rect.top}px`, left: `${rect.right + 8}px` }
    } else {
      previewPos.value = { top: `${rect.bottom + 8}px`, left: `${rect.left}px` }
    }
    previewVisible.value = true
  }, 600)
}
function hideTaskPreview() {
  clearTimeout(previewShowTimer)
  previewHideTimer = setTimeout(() => { previewVisible.value = false }, 100)
}
function keepTaskPreview() { clearTimeout(previewHideTimer) }

// ── S+ 键盘提示条 ──
const showKbdHint = ref(localStorage.getItem('insp_my_kbd_hint_dismissed') !== '1')
function dismissKbdHint() { showKbdHint.value = false; localStorage.setItem('insp_my_kbd_hint_dismissed', '1') }

// 显示用任务列表 (按 keyword 过滤)
const filteredAfterSearch = computed(() => {
  if (!searchKeyword.value.trim()) return filtered.value
  const q = searchKeyword.value.trim().toLowerCase()
  return filtered.value.filter(t =>
    t.taskCode.toLowerCase().includes(q) ||
    (t.inspectorName || '').toLowerCase().includes(q) ||
    projectName(t).toLowerCase().includes(q)
  )
})

// ── Helpers ──
function isOverdue(t: InspTask): boolean {
  // V108: deadlinePolicy=NONE (AD_HOC/SELF_CHECK) 永不逾期
  const policy = (t as any).deadlinePolicy
  if (policy === 'NONE') return false
  if (!t.taskDate) return false
  // 已提交/审核中/已审/已发布/已取消/已过期 都不算"逾期" — 检查员已交差
  if (
    t.status === 'SUBMITTED' || t.status === 'UNDER_REVIEW' || t.status === 'REVIEWED' ||
    t.status === 'PUBLISHED' || t.status === 'CANCELLED' || t.status === 'EXPIRED'
  ) return false
  const eff = (t as any).extendedTo || t.taskDate
  return eff < today
}

function statusVariant(s: TaskStatus): string {
  return ({
    PENDING: 'pending', CLAIMED: 'info', IN_PROGRESS: 'warn',
    SUBMITTED: 'info', UNDER_REVIEW: 'info', REVIEWED: 'pass',
    PUBLISHED: 'pass', CANCELLED: 'fail', EXPIRED: 'fail',
  } as const)[s] || 'pending'
}

function fmtDate(s?: string | null) {
  if (!s) return '—'
  const d = new Date(s)
  return `${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function fmtDateLong(s?: string | null) {
  if (!s) return '—'
  const d = new Date(s)
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}.${d.getDate()} 周${days[d.getDay()]}`
}

function projectName(t: InspTask) {
  return projectMap.value.get(t.projectId)?.projectName || `#${t.projectId}`
}

function progressPct(t: InspTask): number {
  const total = t.totalTargets || 0
  const done = t.completedTargets || 0
  if (!total) return 0
  return Math.round((done / total) * 100)
}

// ── Actions ──
async function handleClaim(t: InspTask) {
  if (claimingIds.value.has(t.id)) return
  claimingIds.value.add(t.id)
  try {
    await claimTask(t.id, { inspectorName: authStore.userName || 'admin' })
    ElMessage.success('领取成功')
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e?.message || '领取失败')
  } finally {
    claimingIds.value.delete(t.id)
  }
}

async function handleWithdraw(t: InspTask) {
  try {
    await ElMessageBox.confirm(`撤回任务「${t.taskCode}」?`, '撤回任务', { type: 'warning' })
    await withdrawTask(t.id)
    ElMessage.success('已撤回')
    loadAll()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  }
}

function goExecute(t: InspTask) {
  // Cockpit 模式默认 (新 redesign), 老 execute 保留: /inspection/tasks/:id/execute
  router.push(`/inspection/tasks/${t.id}/cockpit`)
}

async function handleStart(t: InspTask) {
  try {
    if (t.status === 'CLAIMED') {
      await startTask(t.id)
    }
    goExecute(t)
  } catch (e: any) {
    ElMessage.error(e?.message || '启动失败')
  }
}

// Primary CTA per status — 审核员视角优先
function primaryAction(t: InspTask) {
  // 我作为审核员且任务待审/审核中 > "去审核" (跳到审核台)
  if (isReviewable(t)) {
    return { label: '去审核', accent: true, fn: () => goReview(t) }
  }
  if (t.status === 'PENDING' && !t.inspectorId) {
    return { label: '领取', accent: true, fn: () => handleClaim(t) }
  }
  if (t.status === 'CLAIMED') {
    return { label: '开始', accent: true, fn: () => handleStart(t) }
  }
  if (t.status === 'IN_PROGRESS') {
    return { label: '继续', accent: true, fn: () => goExecute(t) }
  }
  if (t.status === 'SUBMITTED' || t.status === 'UNDER_REVIEW' || t.status === 'REVIEWED') {
    return { label: '查看', accent: false, fn: () => goExecute(t) }
  }
  return { label: '查看', accent: false, fn: () => goExecute(t) }
}

function goReview(t: InspTask) {
  // 跳到审核工作台并预选当前任务
  router.push({ path: '/inspection/tasks/review', query: { taskId: String(t.id) } })
}

// ── Keyboard shortcuts ──
const focusedIdx = ref<number>(-1)
const flatList = computed(() => grouped.value.flatMap(b => b.tasks))

function onKey(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') return
  if (e.metaKey || e.ctrlKey || e.altKey) return

  switch (e.key) {
    case 'j':
    case 'ArrowDown':
      focusedIdx.value = Math.min(focusedIdx.value + 1, flatList.value.length - 1)
      e.preventDefault()
      break
    case 'k':
    case 'ArrowUp':
      focusedIdx.value = Math.max(focusedIdx.value - 1, 0)
      e.preventDefault()
      break
    case 'Enter':
      if (focusedIdx.value >= 0) {
        const t = flatList.value[focusedIdx.value]
        if (t) primaryAction(t).fn()
      }
      break
  }
}

onMounted(() => {
  loadAll()
  loadAdhocProjects()
  window.addEventListener('keydown', onKey)
})
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="insp-shell my-inspection">
    <!-- ── Editorial header ─────────── -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">检查员日程 / Inspector Roster</div>
        <h1 class="insp-display page-title">我的任务</h1>
      </div>
      <div class="head-stats">
        <div class="insp-stat">
          <span class="insp-stat__value">{{ counts.all }}</span>
          <span class="insp-stat__label">总数</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: 'var(--insp-warn)' }">{{ counts.inprogress }}</span>
          <span class="insp-stat__label">进行中</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: counts.overdue ? 'var(--insp-fail)' : 'var(--insp-ink-primary)' }">
            {{ counts.overdue }}
          </span>
          <span class="insp-stat__label">逾期</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: 'var(--insp-info)' }">{{ counts.available }}</span>
          <span class="insp-stat__label">待领取</span>
        </div>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong head-divider" />

    <!-- ── Filter rail + kbd hints ─────────── -->
    <nav class="filter-rail">
      <button class="filter-tab" :class="{ 'is-active': filter === 'all' }" @click="filter = 'all'">
        <span class="filter-tab__label">全部</span>
        <span class="filter-tab__count">{{ counts.all }}</span>
      </button>
      <button class="filter-tab" :class="{ 'is-active': filter === 'available' }" @click="filter = 'available'">
        <span class="filter-tab__label">待领取</span>
        <span class="filter-tab__count">{{ counts.available }}</span>
      </button>
      <button class="filter-tab" :class="{ 'is-active': filter === 'mine' }" @click="filter = 'mine'">
        <span class="filter-tab__label">我的</span>
        <span class="filter-tab__count">{{ counts.mine }}</span>
      </button>
      <button class="filter-tab" :class="{ 'is-active': filter === 'inprogress' }" @click="filter = 'inprogress'">
        <span class="filter-tab__label">进行中</span>
        <span class="filter-tab__count">{{ counts.inprogress }}</span>
      </button>
      <button v-if="counts.toreview > 0 || filter === 'toreview'"
              class="filter-tab filter-tab--review"
              :class="{ 'is-active': filter === 'toreview' }"
              @click="filter = 'toreview'">
        <span class="filter-tab__label">待我审核</span>
        <span class="filter-tab__count" :style="{ color: counts.toreview ? 'var(--insp-info)' : '' }">{{ counts.toreview }}</span>
      </button>
      <button class="filter-tab filter-tab--alert" :class="{ 'is-active': filter === 'overdue' }" @click="filter = 'overdue'">
        <span class="filter-tab__label">逾期</span>
        <span class="filter-tab__count" :style="{ color: counts.overdue ? 'var(--insp-fail)' : '' }">{{ counts.overdue }}</span>
      </button>
      <button v-if="counts.adhoc > 0 || filter === 'adhoc'" class="filter-tab filter-tab--adhoc"
              :class="{ 'is-active': filter === 'adhoc' }" @click="filter = 'adhoc'">
        <span class="filter-tab__label"> 抽查</span>
        <span class="filter-tab__count">{{ counts.adhoc }}</span>
      </button>
      <div class="filter-spacer" />
      <div class="kbd-tray">
        <span class="kbd-pair"><kbd class="insp-kbd">J</kbd><kbd class="insp-kbd">K</kbd> 切换</span>
        <span class="kbd-pair"><kbd class="insp-kbd insp-kbd--inverted">Enter</kbd> 领取/继续</span>
      </div>
      <button class="insp-btn insp-btn--accent" @click="adhocDialog = true"> 发起抽查</button>
      <button class="insp-btn insp-btn--ghost" @click="loadAll">刷新</button>
    </nav>

    <!--  抽查发起对话框 -->
    <el-dialog v-model="adhocDialog" title=" 发起临时抽查" width="500px" append-to-body>
      <el-form :model="adhocForm" label-width="90px">
        <el-form-item label="项目" required>
          <el-select v-model="adhocForm.projectId" placeholder="选择允许抽查的项目" class="w-full"
                     :loading="adhocLoadingProjects">
            <el-option v-for="p in adhocProjects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
          <div v-if="!adhocLoadingProjects && adhocProjects.length===0" class="mt-1 text-xs text-warning">
            暂无项目允许抽查 (项目编辑里勾选 "允许抽查")
          </div>
        </el-form-item>
        <el-form-item label="发起原因" required>
          <el-input v-model="adhocForm.reason" type="textarea" :rows="3"
                    placeholder="说明本次抽查的触发原因 (举报/投诉/突击等), 至少 10 字" maxlength="500" show-word-limit />
        </el-form-item>
        <div class="text-xs text-gray-500 px-2">
          ℹ 抽查任务永不逾期, 不计入计划完成率, 但扣分计入受检主体总分 (×1.2 加权)
        </div>
      </el-form>
      <template #footer>
        <el-button @click="adhocDialog = false">取消</el-button>
        <el-button type="primary" :loading="adhocSubmitting" @click="submitAdhoc"> 立即发起</el-button>
      </template>
    </el-dialog>

    <!-- ── Date-grouped roster ─────────── -->
    <section v-loading="loading" class="roster">
      <div v-for="bucket in grouped" :key="bucket.key" class="bucket">
        <div class="bucket-head">
          <span class="bucket-label" :class="{ 'bucket-label--overdue': bucket.key === 'overdue', 'bucket-label--today': bucket.key === 'today' }">
            {{ bucket.label }}
          </span>
          <span class="bucket-count insp-num">{{ bucket.tasks.length }}</span>
          <hr class="bucket-rule" />
        </div>

        <article
          v-for="(t, i) in bucket.tasks" :key="t.id"
          :data-task-id="t.id"
          class="task-row"
          :class="{
            'is-focused': flatList.indexOf(t) === focusedIdx,
            'is-overdue': isOverdue(t),
          }"
          @mouseenter="(e) => { focusedIdx = flatList.indexOf(t); showTaskPreview(t, e) }"
          @mouseleave="hideTaskPreview"
          @click="primaryAction(t).fn()"
        >
          <span class="row-date insp-num">
            {{ fmtDate(t.taskDate) }}
            <span v-if="(t as any).extendedTo" class="row-date__ext">> {{ fmtDate((t as any).extendedTo) }}</span>
          </span>

          <div class="row-meta">
            <div class="row-code-line">
              <span class="row-code">{{ t.taskCode }}</span>
              <span class="insp-chip" :class="`insp-chip--${statusVariant(t.status)}`">
                {{ TaskStatusConfig[t.status]?.label || t.status }}
              </span>
              <span v-if="roleInTask(t) === 'reviewer'" class="role-chip role-chip--reviewer" title="你作为审核员">
                审核
              </span>
              <span v-if="t.lateSubmission" class="role-chip role-chip--late" :title="`延迟交付 ${t.lateDays} 天`">
                迟{{ t.lateDays }}
              </span>
              <span v-if="(t as any).rejectionCount && (t as any).rejectionCount > 0" class="insp-stamp">
                驳{{ (t as any).rejectionCount }}
              </span>
            </div>
            <div class="row-project">{{ projectName(t) }}</div>
          </div>

          <!-- Progress bar -->
          <div class="row-progress">
            <div class="progress-bar">
              <div class="progress-bar__fill" :style="{ width: progressPct(t) + '%' }" />
            </div>
            <div class="progress-text insp-num">
              {{ t.completedTargets || 0 }}<span class="dim">/{{ t.totalTargets || 0 }}</span>
            </div>
          </div>

          <!-- Actions -->
          <div class="row-actions" @click.stop>
            <button
              v-if="t.status === 'IN_PROGRESS' && t.inspectorId === authStore.user?.userId"
              class="insp-btn insp-btn--sm" @click="handleWithdraw(t)" title="撤回提交"
            >撤回</button>
            <button
              :class="['insp-btn', 'insp-btn--sm', primaryAction(t).accent ? 'insp-btn--accent' : '']"
              :disabled="claimingIds.has(t.id)"
              @click="primaryAction(t).fn()"
            >{{ primaryAction(t).label }}</button>
          </div>
        </article>
      </div>

      <div v-if="!loading && grouped.length === 0" class="empty">
        <div class="insp-stamp">无任务</div>
        <p class="empty-hint">
          {{ filter === 'overdue' ? '没有逾期任务' :
             filter === 'available' ? '暂无可领取任务' :
             filter === 'toreview' ? '暂无待你审核的任务' :
             filter === 'inprogress' ? '没有进行中的任务' : '当前无检查任务' }}
        </p>
      </div>
    </section>

    <!-- 任务悬浮预览卡 (S+) -->
    <Teleport to="body">
      <Transition name="my-popover">
        <div v-if="previewVisible && previewTarget"
             class="my-popover"
             :style="{ top: previewPos.top, left: previewPos.left }"
             @mouseenter="keepTaskPreview"
             @mouseleave="hideTaskPreview">
          <div class="my-popover__head">
            <div class="my-popover__title-line">
              <span class="my-popover__code">{{ previewTarget.taskCode }}</span>
              <span class="insp-chip" :class="`insp-chip--${statusVariant(previewTarget.status)}`">
                {{ TaskStatusConfig[previewTarget.status]?.label || previewTarget.status }}
              </span>
            </div>
            <div class="my-popover__title">{{ projectName(previewTarget) }}</div>
          </div>
          <div class="my-popover__body">
            <div class="my-popover__row">
              <span class="my-popover__label">日期</span>
              <span class="insp-num">{{ fmtDate(previewTarget.taskDate) }}</span>
              <span v-if="(previewTarget as any).extendedTo" class="my-popover__ext">
                延至 <span class="insp-num">{{ fmtDate((previewTarget as any).extendedTo) }}</span>
              </span>
            </div>
            <div v-if="previewTarget.inspectorName" class="my-popover__row">
              <span class="my-popover__label">检查员</span>
              <span>{{ previewTarget.inspectorName }}</span>
            </div>
            <div v-if="previewTarget.reviewerName && roleInTask(previewTarget) === 'reviewer'" class="my-popover__row">
              <span class="my-popover__label">你是审核员</span>
              <span>{{ previewTarget.reviewerName }}</span>
            </div>
            <div class="my-popover__row">
              <span class="my-popover__label">进度</span>
              <span class="my-popover__progress">
                <span class="insp-num">{{ previewTarget.completedTargets || 0 }}</span>
                <span class="dim">/{{ previewTarget.totalTargets || 0 }}</span>
                <span class="my-popover__pct insp-num">{{ progressPct(previewTarget) }}%</span>
              </span>
            </div>
            <div v-if="previewTarget.lateSubmission" class="my-popover__row my-popover__row--late">
              <span class="my-popover__label"> 延迟</span>
              <span class="insp-num">{{ previewTarget.lateDays }}</span> 天交付
            </div>
            <div v-if="(previewTarget as any).rejectionCount && (previewTarget as any).rejectionCount > 0" class="my-popover__row">
              <span class="my-popover__label">已驳回</span>
              <span class="insp-num">{{ (previewTarget as any).rejectionCount }}</span> 次
            </div>
          </div>
          <div class="my-popover__foot">
            点击行 > {{ primaryAction(previewTarget).label }}
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.my-inspection {
  padding: 32px 48px 64px;
  max-width: 1500px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--insp-bg-page);
}

/* ─ Header ─────── */
.page-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: var(--insp-sp-7);
  margin-bottom: var(--insp-sp-4);
}
.page-title { font-size: 44px; margin: 0; font-weight: 500; }
.head-stats {
  display: flex; align-items: center; gap: var(--insp-sp-6);
  padding: 4px 0;
}
.head-rule { width: 1px; height: 32px; background: var(--insp-border-default); }
.head-divider { margin: 0 0 var(--insp-sp-7); }

/* ─ Filter rail ─────── */
.filter-rail {
  display: flex; align-items: center; gap: 0;
  margin-bottom: var(--insp-sp-5);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.filter-tab {
  position: relative;
  display: inline-flex; align-items: baseline; gap: var(--insp-sp-2);
  padding: 10px 18px 12px;
  border: 0; background: transparent; cursor: pointer;
  font-family: inherit; font-size: var(--insp-text-md); font-weight: 500;
  color: var(--insp-ink-tertiary);
  transition: color var(--insp-t-fast);
}
.filter-tab:hover { color: var(--insp-ink-primary); }
.filter-tab.is-active { color: var(--insp-ink-primary); }
.filter-tab.is-active::after {
  content: ''; position: absolute; left: 18px; right: 18px; bottom: -1px;
  height: 2px; background: var(--insp-accent);
}
.filter-tab__count {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs); font-weight: 500;
  color: var(--insp-ink-quaternary);
}
.filter-tab.is-active .filter-tab__count { color: var(--insp-accent); }
.filter-tab--review.is-active .filter-tab__count { color: var(--insp-info); }
.filter-tab--review.is-active::after { background: var(--insp-info) !important; }

.filter-tab--adhoc { color: #9a3412; }
.filter-tab--adhoc.is-active { color: #ea580c; background: linear-gradient(180deg, #fff7ed, #ffedd5); }
.filter-tab--adhoc.is-active::after { background: #ea580c !important; }
.filter-tab--adhoc .filter-tab__count { background: #fed7aa; color: #9a3412; }

.w-full { width: 100%; }
.text-warning { color: #f59e0b; }

/* 角色徽章 (审核员/延迟交付) */
.role-chip {
  display: inline-flex; align-items: center;
  padding: 1px 7px;
  height: 18px;
  border-radius: 9px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
  margin-left: 4px;
  font-family: var(--insp-font-mono);
}
.role-chip--reviewer {
  background: rgba(99, 102, 241, 0.12);
  color: #4338ca;
  border: 1px solid rgba(99, 102, 241, 0.4);
}
.role-chip--late {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.4);
}

.filter-spacer { flex: 1; }
.kbd-tray {
  display: flex; align-items: center; gap: var(--insp-sp-3);
  padding-right: var(--insp-sp-3);
  border-right: 1px solid var(--insp-border-subtle);
  margin-right: var(--insp-sp-3);
}
.kbd-pair {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: var(--insp-text-xs); color: var(--insp-ink-tertiary);
}

/* ─ Roster (date buckets) ─────── */
.roster { display: flex; flex-direction: column; gap: var(--insp-sp-7); }
.bucket { display: flex; flex-direction: column; }

.bucket-head {
  display: flex; align-items: center; gap: var(--insp-sp-3);
  margin-bottom: var(--insp-sp-3);
}
.bucket-label {
  font-family: var(--insp-font-display);
  font-size: 18px; font-weight: 500;
  color: var(--insp-ink-primary);
  letter-spacing: var(--insp-tracking-tight);
}
.bucket-label--overdue { color: var(--insp-fail); }
.bucket-label--today { color: var(--insp-accent); }
.bucket-count {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm); font-weight: 500;
  color: var(--insp-ink-tertiary);
}
.bucket-rule {
  flex: 1; height: 1px; border: 0;
  background: var(--insp-border-default);
  margin: 0;
}

/* ─ Task row ─────── */
.task-row {
  display: grid;
  grid-template-columns: 110px 1fr 220px 200px;
  gap: var(--insp-sp-4);
  align-items: center;
  padding: var(--insp-sp-4) var(--insp-sp-3);
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}
.task-row:hover, .task-row.is-focused {
  background: var(--insp-bg-subtle);
}
.task-row.is-overdue {
  background: linear-gradient(to right, var(--insp-fail-pale), transparent 60%);
}
.task-row.is-overdue:hover, .task-row.is-overdue.is-focused {
  background: linear-gradient(to right, color-mix(in oklab, var(--insp-fail-pale) 80%, white), transparent 60%);
}
.task-row.is-focused {
  box-shadow: inset 3px 0 0 var(--insp-accent);
}

.row-date {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md); font-weight: 600;
  color: var(--insp-ink-primary);
  display: flex; flex-direction: column; gap: 2px;
}
.row-date__ext {
  font-size: var(--insp-text-xs);
  color: var(--insp-warn);
  font-weight: 500;
}

.row-meta { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.row-code-line {
  display: flex; align-items: center; gap: var(--insp-sp-2);
  flex-wrap: wrap;
}
.row-code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md); font-weight: 600;
  color: var(--insp-ink-primary);
}
.row-project {
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ─ Progress ─────── */
.row-progress {
  display: flex; align-items: center; gap: var(--insp-sp-3);
}
.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--insp-bg-sunken);
  border-radius: 0;
  overflow: hidden;
}
.progress-bar__fill {
  height: 100%;
  background: var(--insp-ink-primary);
  transition: width var(--insp-t-medium);
}
.task-row.is-overdue .progress-bar__fill { background: var(--insp-fail); }
.progress-text {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm); font-weight: 600;
  color: var(--insp-ink-primary);
  min-width: 56px;
  text-align: right;
}
.progress-text .dim { color: var(--insp-ink-quaternary); font-weight: 400; }

/* ─ Actions ─────── */
.row-actions {
  display: flex; gap: var(--insp-sp-2);
  justify-content: flex-end;
}

/* ─ Empty ─────── */
.empty { padding: 80px 0; text-align: center; }
.empty-hint {
  margin-top: var(--insp-sp-4);
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-sm);
}

/* ─ Responsive ─────── */
@media (max-width: 1100px) {
  .task-row { grid-template-columns: 90px 1fr 160px 180px; gap: var(--insp-sp-3); }
}
@media (max-width: 800px) {
  .my-inspection { padding: 20px 16px 64px; }
  .page-title { font-size: 32px; }
  .head-stats { gap: var(--insp-sp-4); }
  .kbd-tray { display: none; }
  .task-row { grid-template-columns: 80px 1fr; gap: var(--insp-sp-3); }
  .row-progress, .row-actions { grid-column: 2; }
}

/* ─ S+ 搜索高亮 ─────── */
:deep(.my-mark) {
  background: rgba(245, 200, 70, 0.4);
  color: var(--insp-ink-primary);
  padding: 0 2px;
  border-radius: 2px;
  font-weight: 600;
}

/* ─ S+ 任务悬浮预览卡 ─────── */
.my-popover {
  position: fixed;
  z-index: 9500;
  width: 300px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-strong);
  border-radius: var(--insp-radius-lg);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.my-popover__head {
  padding: 11px 14px;
  background: var(--insp-bg-subtle);
  border-bottom: 1px solid var(--insp-border-subtle);
}
.my-popover__title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.my-popover__code {
  font-family: var(--insp-font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--insp-ink-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.my-popover__title {
  font-size: 12.5px;
  color: var(--insp-ink-secondary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.my-popover__body {
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.my-popover__row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--insp-ink-secondary);
}
.my-popover__label {
  width: 64px;
  color: var(--insp-ink-tertiary);
  font-size: 10.5px;
  letter-spacing: 0.04em;
  flex-shrink: 0;
}
.my-popover__ext {
  margin-left: auto;
  color: var(--insp-info);
  font-size: 10.5px;
}
.my-popover__progress {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.my-popover__pct {
  margin-left: auto;
  color: var(--insp-accent);
  font-weight: 600;
}
.my-popover__row--late {
  background: rgba(245, 158, 11, 0.08);
  margin: 0 -14px;
  padding: 4px 14px;
  color: #b45309;
  font-weight: 500;
}
.my-popover__row--late .my-popover__label { color: #b45309; }
.my-popover__foot {
  padding: 8px 14px;
  background: var(--insp-bg-subtle);
  border-top: 1px solid var(--insp-border-subtle);
  font-size: 10.5px;
  color: var(--insp-ink-tertiary);
  text-align: center;
}
.my-popover-enter-active, .my-popover-leave-active { transition: all 0.15s ease; }
.my-popover-enter-from { opacity: 0; transform: translateX(-4px) scale(0.98); }
.my-popover-leave-to { opacity: 0; }
</style>
