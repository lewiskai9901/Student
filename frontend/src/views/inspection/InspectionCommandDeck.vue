<script setup lang="ts">
/**
 * InspectionCommandDeck — 检查平台总台 (角色 Inbox 重设计)
 *
 * 真·UX 重设计的核心: 不再是「项目仪表盘」, 而是「关于我」的统一收件箱.
 * 任何角色登录后能立刻看到"我现在该做什么"且无需翻多个 list 页.
 *
 * Inbox 类型 (按角色权限自动过滤):
 *  ① 检查员 — 待我执行的检查任务
 *  ② 审核员 — 待我审的任务 + 待我审的申诉
 *  ③ 责任人 — 待我整改 (我是 assignee)
 *  ④ 当事人 — 关于我/我组织的检查记录 (近 7 天)
 *  ⑤ 申诉人 — 我提交的申诉状态
 *  ⑥ 管理员 — 平台健康度 (积压 / 逾期 / 待重派)
 *
 * 每个 Inbox 项可单击直达对应实体, 旁边标注下一步动作 (CTA).
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import {
  AlertTriangle, Clock, CheckCircle2, Inbox,
  Gavel, Wrench, FileWarning, Sparkles, Settings2,
} from 'lucide-vue-next'
import { getMyTasks, getTasks } from '@/api/inspection/task'
import { getPendingAppeals, getMyAppeals } from '@/api/inspection/appeal'
import { getCases as getCorrectiveCases } from '@/api/inspection/correctiveCase'
import { getProjects } from '@/api/inspection/project'
import type { InspTask, InspProject } from '@/types/insp/project'
import type { InspAppeal } from '@/types/insp/appeal'

const router = useRouter()
const authStore = useAuthStore()

// ── State ──
const loading = ref(true)
const myTasks = ref<InspTask[]>([])
const reviewQueue = ref<InspTask[]>([])
const myCases = ref<any[]>([])
const myAppeals = ref<InspAppeal[]>([])
const pendingAppeals = ref<InspAppeal[]>([])
const aboutMeTasks = ref<InspTask[]>([])  // tasks where I'm the subject
const allProjects = ref<any[]>([])

// ── Identity ──
const userName = computed(() => authStore.user?.realName || authStore.user?.username || '用户')
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// ── Permissions / role gates ──
const canExecute = computed(() => authStore.hasPermission('insp:task:execute'))
const canReviewTasks = computed(() => authStore.hasPermission('insp:task:review'))
const canReviewAppeals = computed(() => authStore.hasPermission('inspection:appeal:review'))
const canCreateAppeal = computed(() => authStore.hasPermission('inspection:appeal:create'))
const canManageProject = computed(() => authStore.hasPermission('insp:project:manage') || authStore.hasPermission('insp:project:edit'))
const isPlatformAdmin = computed(() => authStore.hasPermission('insp:corrective:manage'))

// ── Derived inbox metrics ──
const myTaskTodos = computed(() =>
  myTasks.value.filter(t => ['PENDING', 'CLAIMED', 'IN_PROGRESS'].includes(t.status))
)

const myCaseTodos = computed(() =>
  myCases.value.filter(c =>
    ['ASSIGNED', 'IN_PROGRESS', 'REJECTED'].includes(c.status) &&
    c.assigneeId === Number(authStore.user?.userId)
  )
)

const myAppealsActive = computed(() =>
  myAppeals.value.filter(a => a.status === 'PENDING')
)

const myAppealsRecentClosed = computed(() =>
  myAppeals.value
    .filter(a => a.status === 'APPROVED' || a.status === 'REJECTED')
    .slice(0, 3)
)

// 平台健康度 (管理员视角)
const platformHealth = computed(() => {
  const overdueCases = myCases.value.filter(c => isOverdue(c.deadline) &&
    !['CLOSED', 'VERIFIED'].includes(c.status))
  const escalatedCases = myCases.value.filter(c => c.escalationLevel && c.escalationLevel > 0)
  const draftProjects = allProjects.value.filter(p => p.status === 'DRAFT')
  return {
    overdueCases: overdueCases.length,
    escalatedCases: escalatedCases.length,
    draftProjects: draftProjects.length,
    pendingReviews: reviewQueue.value.length,
    pendingAppeals: pendingAppeals.value.length,
  }
})

const totalActionable = computed(() => {
  let n = myTaskTodos.value.length + myCaseTodos.value.length
  if (canReviewTasks.value) n += reviewQueue.value.length
  if (canReviewAppeals.value) n += pendingAppeals.value.length
  return n
})

// ── Loaders ──
async function loadAll() {
  loading.value = true
  const userId = Number(authStore.user?.userId)
  try {
    const promises: Promise<any>[] = []
    promises.push(getMyTasks().then(d => (myTasks.value = d || [])).catch(() => {}))
    promises.push(getCorrectiveCases({}).then(d => (myCases.value = (d || []))).catch(() => {}))
    if (canCreateAppeal.value) {
      promises.push(getMyAppeals().then(d => (myAppeals.value = d || [])).catch(() => {}))
    }
    if (canReviewTasks.value) {
      promises.push(getTasks().then(d => (reviewQueue.value = (d || []).filter((t: InspTask) =>
        t.status === 'SUBMITTED' || t.status === 'UNDER_REVIEW'))).catch(() => {}))
    }
    if (canReviewAppeals.value) {
      promises.push(getPendingAppeals().then(d => (pendingAppeals.value = d || [])).catch(() => {}))
    }
    promises.push(getProjects({}).then(d => (allProjects.value = d || [])).catch(() => {}))
    await Promise.all(promises)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ── Helpers ──
// V108: 接受 string deadline 或 task object (有 deadlinePolicy 字段)
function isOverdue(deadlineOrTask?: any) {
  if (!deadlineOrTask) return false
  // 如果是 object 且 deadlinePolicy=NONE → 永不逾期
  if (typeof deadlineOrTask === 'object' && deadlineOrTask?.deadlinePolicy === 'NONE') return false
  const deadline = typeof deadlineOrTask === 'string'
    ? deadlineOrTask
    : (deadlineOrTask.taskDate || deadlineOrTask.deadline)
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

function fmtDate(s?: string | null) {
  if (!s) return '—'
  const d = new Date(s)
  return `${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function deadlineLabel(deadline?: string | null): string {
  if (!deadline) return ''
  const d = Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000)
  if (d < 0) return `已逾期 ${Math.abs(d)} 天`
  if (d === 0) return '今日截止'
  if (d === 1) return '明日截止'
  if (d <= 3) return `剩 ${d} 天`
  return `${fmtDate(deadline)} 截止`
}

function statusVariant(s: string): string {
  return ({
    PENDING: 'pending', CLAIMED: 'info', IN_PROGRESS: 'warn',
    SUBMITTED: 'info', ASSIGNED: 'info', REJECTED: 'fail',
    APPROVED: 'pass', VERIFIED: 'pass', CLOSED: 'pass',
    ESCALATED: 'fail',
  } as any)[s] || 'pending'
}

function actionLabel(t: InspTask): string {
  if (t.status === 'PENDING' && !t.inspectorId) return '领取'
  if (t.status === 'CLAIMED') return '开始'
  if (t.status === 'IN_PROGRESS') return '继续打分'
  return '查看'
}

// ── CTAs ──
function goTaskExec(t: InspTask) {
  router.push(`/inspection/tasks/${t.id}/cockpit`)
}
function goCase(c: any) {
  router.push(`/inspection/corrective/${c.id}`)
}
function goAppeal(a: InspAppeal) {
  router.push(`/inspection/appeals/my`)
}
function goReviewTasks() {
  router.push('/inspection/tasks/review')
}
function goReviewAppeals() {
  router.push('/inspection/appeals/review')
}
function goProjects() {
  router.push('/inspection/projects')
}
function goReassign() {
  router.push('/inspection/admin/reassign-departed')
}
function goAboutMe() {
  router.push('/inspection/about-me')
}

onMounted(() => loadAll())
</script>

<template>
  <div class="insp-shell deck">
    <!-- ── Header strip ─────────── -->
    <header class="deck-head">
      <div class="deck-greet">
        <span class="insp-eyebrow">Inspection · Hub</span>
        <h1 class="deck-greet__name">{{ greeting }}, {{ userName }}</h1>
        <p class="deck-greet__sub">
          <template v-if="loading">载入中…</template>
          <template v-else-if="totalActionable === 0">
            <span class="deck-greet__zero">✓ 当前无待办</span>
          </template>
          <template v-else>
            待你处理 <strong class="insp-num">{{ totalActionable }}</strong> 项
          </template>
        </p>
      </div>
      <div class="deck-quick">
        <button v-if="canManageProject" class="quick-btn" @click="goProjects">
          <span class="quick-btn__label">项目管理</span>
        </button>
        <button v-if="canManageProject" class="quick-btn" @click="router.push('/inspection/templates')">
          <span class="quick-btn__label">模板与题库</span>
        </button>
        <button class="quick-btn" @click="router.push('/inspection/analytics')">
          <span class="quick-btn__label">分析报表</span>
        </button>
        <button v-if="isPlatformAdmin" class="quick-btn quick-btn--alert" @click="goReassign">
          <span class="quick-btn__label">离职重派</span>
        </button>
      </div>
    </header>

    <!-- ── Main: 6-bin inbox ─────────── -->
    <div class="deck-grid" v-loading="loading">

      <!-- Bin 1 · 我的检查任务 -->
      <section class="bin" v-if="canExecute">
        <header class="bin-head">
          <div class="bin-head__lead">
            <Gavel :size="14" class="bin-icon" />
            <span class="bin-title">我的检查任务</span>
          </div>
          <span class="bin-count" :class="{ 'is-empty': myTaskTodos.length === 0 }">
            {{ myTaskTodos.length }}
          </span>
        </header>
        <ul class="bin-list" v-if="myTaskTodos.length">
          <li v-for="t in myTaskTodos.slice(0, 5)" :key="t.id" class="bin-row" @click="goTaskExec(t)">
            <div class="bin-row__main">
              <span class="bin-row__title">{{ t.taskCode }}</span>
              <span class="insp-chip" :class="`insp-chip--${statusVariant(t.status)}`">
                {{ ({ PENDING:'待领取', CLAIMED:'已领取', IN_PROGRESS:'进行中' } as any)[t.status] }}
              </span>
            </div>
            <div class="bin-row__meta">
              <span class="bin-row__progress insp-num">{{ t.completedTargets || 0 }}/{{ t.totalTargets || 0 }}</span>
              <span class="bin-row__deadline" :class="{ 'is-overdue': isOverdue(t.taskDate) }">
                {{ isOverdue(t.taskDate) ? '已逾期' : `${fmtDate(t.taskDate)}` }}
              </span>
              <button class="bin-cta">{{ actionLabel(t) }} →</button>
            </div>
          </li>
        </ul>
        <div v-else class="bin-empty">
          <CheckCircle2 :size="14" class="bin-empty__icon" />
          <span>无待执行任务</span>
        </div>
        <button v-if="myTasks.length > 5" class="bin-more" @click="router.push('/inspection/my-tasks')">
          查看全部 {{ myTasks.length }} 条 →
        </button>
      </section>

      <!-- Bin 2 · 待我审核 (Tasks) -->
      <section class="bin" v-if="canReviewTasks">
        <header class="bin-head">
          <div class="bin-head__lead">
            <Inbox :size="14" class="bin-icon" />
            <span class="bin-title">待我审核 · 任务</span>
          </div>
          <span class="bin-count" :class="{ 'is-empty': reviewQueue.length === 0 }">
            {{ reviewQueue.length }}
          </span>
        </header>
        <div v-if="reviewQueue.length" class="bin-summary">
          <div class="summary-line">
            <span class="summary-num insp-num">{{ reviewQueue.length }}</span>
            <span class="summary-text">个任务等你裁决</span>
          </div>
          <button class="bin-primary-cta" @click="goReviewTasks">
            进入审核工作台 →
          </button>
        </div>
        <div v-else class="bin-empty">
          <CheckCircle2 :size="14" class="bin-empty__icon" />
          <span>队列已清空</span>
        </div>
      </section>

      <!-- Bin 3 · 待我审核 (Appeals) -->
      <section class="bin" v-if="canReviewAppeals">
        <header class="bin-head">
          <div class="bin-head__lead">
            <Gavel :size="14" class="bin-icon" />
            <span class="bin-title">待我审核 · 申诉</span>
          </div>
          <span class="bin-count" :class="{ 'is-empty': pendingAppeals.length === 0 }">
            {{ pendingAppeals.length }}
          </span>
        </header>
        <div v-if="pendingAppeals.length" class="bin-summary">
          <div class="summary-line">
            <span class="summary-num insp-num">{{ pendingAppeals.length }}</span>
            <span class="summary-text">条申诉待审</span>
          </div>
          <button class="bin-primary-cta" @click="goReviewAppeals">
            进入申诉审核台 →
          </button>
        </div>
        <div v-else class="bin-empty">
          <CheckCircle2 :size="14" class="bin-empty__icon" />
          <span>队列已清空</span>
        </div>
      </section>

      <!-- Bin 4 · 我负责的整改 -->
      <section class="bin">
        <header class="bin-head">
          <div class="bin-head__lead">
            <Wrench :size="14" class="bin-icon" />
            <span class="bin-title">我负责的整改</span>
          </div>
          <span class="bin-count" :class="{ 'is-empty': myCaseTodos.length === 0 }">
            {{ myCaseTodos.length }}
          </span>
        </header>
        <ul class="bin-list" v-if="myCaseTodos.length">
          <li v-for="c in myCaseTodos.slice(0, 5)" :key="c.id" class="bin-row" @click="goCase(c)">
            <div class="bin-row__main">
              <span class="bin-row__title">{{ c.caseCode }}</span>
              <span class="insp-chip" :class="`insp-chip--${statusVariant(c.status)}`">
                {{ ({ ASSIGNED:'待整改', IN_PROGRESS:'整改中', REJECTED:'被驳回' } as any)[c.status] }}
              </span>
            </div>
            <div class="bin-row__sub">{{ c.issueDescription }}</div>
            <div class="bin-row__meta">
              <span class="bin-row__deadline" :class="{ 'is-overdue': isOverdue(c.deadline) }">
                <Clock :size="11" /> {{ deadlineLabel(c.deadline) }}
              </span>
              <button class="bin-cta">处理 →</button>
            </div>
          </li>
        </ul>
        <div v-else class="bin-empty">
          <CheckCircle2 :size="14" class="bin-empty__icon" />
          <span>无待整改案件</span>
        </div>
      </section>

      <!-- Bin 5 · 我的申诉 -->
      <section class="bin" v-if="canCreateAppeal">
        <header class="bin-head">
          <div class="bin-head__lead">
            <FileWarning :size="14" class="bin-icon" />
            <span class="bin-title">我的申诉</span>
          </div>
          <button class="bin-link" @click="router.push('/inspection/appeals/my')">查看全部 →</button>
        </header>
        <div class="bin-stats-row" v-if="myAppeals.length">
          <div class="stat-cell">
            <span class="stat-cell__num insp-num">{{ myAppealsActive.length }}</span>
            <span class="stat-cell__label">待审中</span>
          </div>
          <div class="stat-cell">
            <span class="stat-cell__num insp-num">{{ myAppeals.filter(a => a.status === 'APPROVED').length }}</span>
            <span class="stat-cell__label">通过</span>
          </div>
          <div class="stat-cell">
            <span class="stat-cell__num insp-num">{{ myAppeals.filter(a => a.status === 'REJECTED').length }}</span>
            <span class="stat-cell__label">驳回</span>
          </div>
        </div>
        <ul class="bin-list bin-list--mini" v-if="myAppealsRecentClosed.length">
          <li v-for="a in myAppealsRecentClosed" :key="a.id" class="bin-row bin-row--mini" @click="goAppeal(a)">
            <span class="bin-row__title">{{ a.appealCode }}</span>
            <span class="insp-chip" :class="a.status === 'APPROVED' ? 'insp-chip--pass' : 'insp-chip--fail'">
              {{ a.status === 'APPROVED' ? '通过' : '驳回' }}
            </span>
          </li>
        </ul>
        <div v-if="myAppeals.length === 0" class="bin-empty">
          <span>无申诉记录</span>
        </div>
      </section>

      <!-- Bin 6 · 关于我 (subject view) -->
      <section class="bin">
        <header class="bin-head">
          <div class="bin-head__lead">
            <Sparkles :size="14" class="bin-icon" />
            <span class="bin-title">关于我的检查</span>
          </div>
          <button class="bin-link" @click="goAboutMe">完整记录 →</button>
        </header>
        <p class="bin-hint">
          查看针对你或你所在组织的检查记录, 不服扣分可发起申诉.
        </p>
        <button class="bin-primary-cta" @click="goAboutMe">
          打开我的检查记录 →
        </button>
      </section>

      <!-- Bin 7 · 平台健康度 (管理员) -->
      <section class="bin bin--wide" v-if="isPlatformAdmin">
        <header class="bin-head">
          <div class="bin-head__lead">
            <Settings2 :size="14" class="bin-icon" />
            <span class="bin-title">平台健康度</span>
          </div>
          <span class="insp-eyebrow">Admin</span>
        </header>
        <div class="health-grid">
          <button class="health-cell" :class="{ 'is-bad': platformHealth.overdueCases > 0 }"
                  @click="router.push('/inspection/corrective')">
            <span class="health-cell__num insp-num">{{ platformHealth.overdueCases }}</span>
            <span class="health-cell__label">逾期整改</span>
          </button>
          <button class="health-cell" :class="{ 'is-bad': platformHealth.escalatedCases > 0 }"
                  @click="router.push('/inspection/corrective')">
            <span class="health-cell__num insp-num">{{ platformHealth.escalatedCases }}</span>
            <span class="health-cell__label">已升级案件</span>
          </button>
          <button class="health-cell" @click="router.push('/inspection/projects')">
            <span class="health-cell__num insp-num">{{ platformHealth.draftProjects }}</span>
            <span class="health-cell__label">草稿项目</span>
          </button>
          <button class="health-cell" @click="goReviewTasks">
            <span class="health-cell__num insp-num">{{ platformHealth.pendingReviews }}</span>
            <span class="health-cell__label">待审任务</span>
          </button>
          <button class="health-cell" @click="goReviewAppeals">
            <span class="health-cell__num insp-num">{{ platformHealth.pendingAppeals }}</span>
            <span class="health-cell__label">待审申诉</span>
          </button>
          <button class="health-cell" @click="goReassign">
            <span class="health-cell__num insp-num">→</span>
            <span class="health-cell__label">离职重派</span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.deck { padding: 12px 16px; }

/* ─ Head ─────── */
.deck-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 12px 18px;
  margin-bottom: 12px;
}

.deck-greet { display: flex; flex-direction: column; gap: 2px; }
.deck-greet__name {
  font-size: 17px;
  font-weight: 700;
  color: var(--insp-ink-primary);
  margin: 2px 0 0;
}
.deck-greet__sub {
  font-size: 12px;
  color: var(--insp-ink-tertiary);
  margin: 0;
}
.deck-greet__sub strong {
  color: var(--insp-accent);
  font-weight: 700;
  font-family: var(--insp-font-mono);
}
.deck-greet__zero { color: var(--insp-pass); font-weight: 500; }

.deck-quick {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.quick-btn {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid var(--insp-border-strong);
  border-radius: var(--insp-radius-sm);
  background: var(--insp-bg-surface);
  color: var(--insp-ink-secondary);
  cursor: pointer;
  font-family: inherit;
  transition: all var(--insp-t-fast);
}
.quick-btn:hover { border-color: var(--insp-accent); color: var(--insp-accent); }
.quick-btn--alert {
  color: var(--insp-fail);
  border-color: var(--insp-fail-border);
  background: var(--insp-fail-pale);
}
.quick-btn--alert:hover { background: var(--insp-fail); color: white; }

/* ─ Grid ─────── */
.deck-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  align-items: start;
}

.bin {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 140px;
}

.bin--wide { grid-column: span 3; }

/* ─ Bin head ─────── */
.bin-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--insp-border-subtle);
}

.bin-head__lead {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bin-icon { color: var(--insp-ink-tertiary); }
.bin-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--insp-ink-primary);
}

.bin-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 20px;
  padding: 0 6px;
  font-family: var(--insp-font-mono);
  font-size: 11px;
  font-weight: 600;
  color: white;
  background: var(--insp-accent);
  border-radius: 10px;
}
.bin-count.is-empty {
  color: var(--insp-ink-quaternary);
  background: transparent;
  border: 1px solid var(--insp-border-default);
}

.bin-link {
  background: transparent;
  border: 0;
  color: var(--insp-accent);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.bin-link:hover { text-decoration: underline; }

/* ─ Bin list ─────── */
.bin-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.bin-row {
  padding: 8px 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
}
.bin-row:last-child { border-bottom: 0; }
.bin-row:hover {
  background: var(--insp-bg-subtle);
  margin: 0 -10px;
  padding-left: 10px;
  padding-right: 10px;
}

.bin-row__main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.bin-row__title {
  font-family: var(--insp-font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--insp-ink-primary);
}
.bin-row__sub {
  font-size: 12px;
  color: var(--insp-ink-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.bin-row__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--insp-ink-tertiary);
  flex-wrap: wrap;
}
.bin-row__progress {
  font-family: var(--insp-font-mono);
  font-weight: 600;
  color: var(--insp-ink-secondary);
}
.bin-row__deadline {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.bin-row__deadline.is-overdue {
  color: var(--insp-fail);
  font-weight: 600;
}

.bin-cta {
  margin-left: auto;
  background: transparent;
  border: 0;
  color: var(--insp-accent);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  padding: 0;
}
.bin-cta:hover { text-decoration: underline; }

.bin-list--mini .bin-row {
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

/* ─ Bin summary (single big metric) ─────── */
.bin-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 8px 0;
}
.summary-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.summary-num {
  font-family: var(--insp-font-mono);
  font-size: 32px;
  font-weight: 700;
  color: var(--insp-accent);
  line-height: 1;
}
.summary-text {
  font-size: 12px;
  color: var(--insp-ink-secondary);
}

.bin-primary-cta {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 32px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 500;
  background: var(--insp-accent);
  color: white;
  border: 0;
  border-radius: var(--insp-radius-sm);
  cursor: pointer;
  font-family: inherit;
  transition: background var(--insp-t-fast);
}
.bin-primary-cta:hover { background: var(--insp-accent-hover); }

/* ─ Bin stats row ─────── */
.bin-stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 4px 0;
}
.stat-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px;
  background: var(--insp-bg-subtle);
  border-radius: var(--insp-radius-sm);
  text-align: center;
}
.stat-cell__num {
  font-family: var(--insp-font-mono);
  font-size: 16px;
  font-weight: 700;
  color: var(--insp-ink-primary);
  line-height: 1.1;
}
.stat-cell__label {
  font-size: 10px;
  color: var(--insp-ink-tertiary);
}

/* ─ Bin empty ─────── */
.bin-empty {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 0;
  font-size: 12px;
  color: var(--insp-ink-quaternary);
}
.bin-empty__icon { color: var(--insp-pass); }

.bin-hint {
  font-size: 12px;
  color: var(--insp-ink-secondary);
  line-height: 1.5;
  margin: 0 0 6px;
}

.bin-more {
  background: transparent;
  border: 0;
  color: var(--insp-accent);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
  padding: 4px 0;
}
.bin-more:hover { text-decoration: underline; }

/* ─ Health grid ─────── */
.health-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}
.health-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 10px;
  background: var(--insp-bg-subtle);
  border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-sm);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: all var(--insp-t-fast);
}
.health-cell:hover {
  background: var(--insp-accent-paler);
  border-color: var(--insp-accent);
}
.health-cell.is-bad {
  background: var(--insp-fail-pale);
  border-color: var(--insp-fail-border);
}
.health-cell.is-bad .health-cell__num { color: var(--insp-fail); }

.health-cell__num {
  font-family: var(--insp-font-mono);
  font-size: 22px;
  font-weight: 700;
  color: var(--insp-ink-primary);
  line-height: 1;
}
.health-cell__label {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}

/* ─ Responsive ─────── */
@media (max-width: 1100px) {
  .deck-grid { grid-template-columns: repeat(2, 1fr); }
  .bin--wide { grid-column: span 2; }
  .health-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 720px) {
  .deck-head { flex-direction: column; align-items: flex-start; }
  .deck-grid { grid-template-columns: 1fr; }
  .bin--wide { grid-column: span 1; }
  .health-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
