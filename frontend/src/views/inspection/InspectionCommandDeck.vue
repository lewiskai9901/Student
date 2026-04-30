<script setup lang="ts">
/**
 * InspectionCommandDeck — 检查平台总台 (Audit Console redesign)
 * 替代旧的零散入口, 用编辑式排版组织 "今天该做什么" 的工作流.
 *
 * 设计语言: Audit Console (Newsreader serif + Manrope + JetBrains Mono),
 * 朱砂红 #C9412B 印章意象, 等宽数字, 1px 硬边框, 紧凑信息密度.
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { getMyTasks } from '@/api/inspection/task'
import { getPendingAppeals, getMyAppeals } from '@/api/inspection/appeal'
import { getCases as getCorrectiveCases } from '@/api/inspection/correctiveCase'
import { getProjects } from '@/api/inspection/project'
import type { InspTask } from '@/types/insp/project'
import type { InspAppeal } from '@/types/insp/appeal'

const router = useRouter()
const authStore = useAuthStore()

// ── State ──
const loading = ref(true)
const myTasks = ref<InspTask[]>([])
const pendingReview = ref<InspTask[]>([])
const myAppeals = ref<InspAppeal[]>([])
const reviewQueue = ref<InspAppeal[]>([])
const myOpenCases = ref<any[]>([])
const allProjects = ref<any[]>([])

const userName = computed(() => authStore.user?.realName || authStore.user?.username || '检查员')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const today = computed(() => {
  const d = new Date()
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年 ${d.getMonth() + 1}月 ${d.getDate()}日 · 星期${days[d.getDay()]}`
})

// Permissions / role-aware
const canReviewTasks = computed(() => authStore.hasPermission('insp:task:review'))
const canReviewAppeals = computed(() => authStore.hasPermission('inspection:appeal:review'))

const myTodoCount = computed(() => {
  const t = myTasks.value.filter(t => t.status === 'PENDING' || t.status === 'CLAIMED' || t.status === 'IN_PROGRESS').length
  const r = canReviewTasks.value ? pendingReview.value.length : 0
  const a = canReviewAppeals.value ? reviewQueue.value.length : 0
  const c = myOpenCases.value.length
  return t + r + a + c
})

async function loadAll() {
  loading.value = true
  try {
    const promises: Promise<any>[] = []
    promises.push(getMyTasks().then(d => (myTasks.value = d || [])).catch(() => {}))
    promises.push(getMyAppeals().then(d => (myAppeals.value = d || [])).catch(() => {}))
    if (canReviewAppeals.value) {
      promises.push(getPendingAppeals().then(d => (reviewQueue.value = d || [])).catch(() => {}))
    }
    promises.push(getCorrectiveCases({}).then(d => (myOpenCases.value = (d || []).filter((c: any) =>
      c.status !== 'CLOSED' && c.status !== 'VERIFIED'
    ))).catch(() => {}))
    promises.push(getProjects({}).then(d => (allProjects.value = d || [])).catch(() => {}))
    await Promise.all(promises)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// ── Helpers ──
function fmtDate(s?: string | null) {
  if (!s) return '—'
  const d = new Date(s)
  return `${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function isOverdue(deadline?: string | null) {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

function taskUrgencyChip(t: InspTask): { label: string; variant: string } {
  const status = t.status
  if (status === 'PENDING') return { label: '待领取', variant: 'pending' }
  if (status === 'CLAIMED') return { label: '已领取', variant: 'info' }
  if (status === 'IN_PROGRESS') return { label: '进行中', variant: 'warn' }
  if (status === 'SUBMITTED') return { label: '已提交', variant: 'info' }
  return { label: status, variant: 'pending' }
}

function caseUrgencyChip(c: any): { label: string; variant: string } {
  if (isOverdue(c.deadline)) return { label: '已逾期', variant: 'fail' }
  if (c.priority === 'CRITICAL' || c.priority === 'HIGH') return { label: '高优先级', variant: 'fail' }
  if (c.status === 'IN_PROGRESS') return { label: '进行中', variant: 'warn' }
  return { label: c.priority || c.status, variant: 'info' }
}

const projectStats = computed(() => {
  const list = allProjects.value
  return {
    total: list.length,
    active: list.filter(p => p.status === 'PUBLISHED' || p.status === 'IN_PROGRESS').length,
    draft: list.filter(p => p.status === 'DRAFT').length,
  }
})

const myTasksTodo = computed(() =>
  myTasks.value
    .filter(t => t.status === 'PENDING' || t.status === 'CLAIMED' || t.status === 'IN_PROGRESS')
    .slice(0, 6)
)

const recentClosedAppeals = computed(() =>
  myAppeals.value
    .filter(a => a.status === 'APPROVED' || a.status === 'REJECTED')
    .slice(0, 4)
)

onMounted(() => loadAll())
</script>

<template>
  <div class="insp-shell command-deck">
    <!-- ── Editorial masthead ─────────────────────────── -->
    <header class="masthead">
      <div class="masthead__lead">
        <div class="insp-eyebrow">检查平台 · Operations Console · 第 {{ new Date().getDate() }} 期</div>
        <h1 class="masthead__title">
          <span class="insp-display masthead__greet">{{ greeting }}, {{ userName }}</span>
        </h1>
        <p class="masthead__date">{{ today }}</p>
      </div>
      <div class="masthead__cipher">
        <span class="cipher-num">{{ String(myTodoCount).padStart(2, '0') }}</span>
        <span class="cipher-label">待办事项</span>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong" />

    <!-- ── Operations grid ─────────────────────────────── -->
    <div class="ops-grid" v-loading="loading">

      <!-- ─ Slot 1 · 我的任务 (always visible) ─────── -->
      <section class="ops-card span-2">
        <div class="ops-card__head">
          <div>
            <div class="insp-eyebrow">EXECUTION</div>
            <h2 class="ops-card__title">我的检查任务</h2>
          </div>
          <button class="insp-btn insp-btn--ghost" @click="router.push('/inspection/tasks')">
            全部 →
          </button>
        </div>

        <ol class="task-list">
          <li
            v-for="t in myTasksTodo" :key="t.id"
            class="task-row"
            :class="{ 'is-overdue': isOverdue(t.taskDate) }"
            @click="router.push(`/inspection/tasks/${t.id}/execute`)"
          >
            <span class="task-row__date insp-num">{{ fmtDate(t.taskDate) }}</span>
            <span class="task-row__code">{{ t.taskCode }}</span>
            <span class="task-row__progress insp-num">
              {{ t.completedTargets || 0 }}<span class="dim">/{{ t.totalTargets || 0 }}</span>
            </span>
            <span class="insp-chip" :class="`insp-chip--${taskUrgencyChip(t).variant}`">
              {{ taskUrgencyChip(t).label }}
            </span>
          </li>
          <li v-if="!loading && myTasksTodo.length === 0" class="task-empty">
            <div class="insp-stamp">无任务</div>
            <p class="empty-line">今日无待办检查任务</p>
          </li>
        </ol>
      </section>

      <!-- ─ Slot 2 · 待我审 (任务/申诉合并) ─────── -->
      <section class="ops-card span-2" v-if="canReviewTasks || canReviewAppeals">
        <div class="ops-card__head">
          <div>
            <div class="insp-eyebrow">REVIEW</div>
            <h2 class="ops-card__title">待我审核</h2>
          </div>
          <span class="ops-card__pill insp-num">{{ pendingReview.length + reviewQueue.length }}</span>
        </div>

        <div class="review-split">
          <div v-if="canReviewTasks" class="review-bucket">
            <div class="review-bucket__head">
              <span class="insp-caps">任务审核</span>
              <span class="insp-num review-bucket__count">{{ pendingReview.length }}</span>
            </div>
            <button class="insp-btn review-bucket__cta"
                    :disabled="pendingReview.length === 0"
                    @click="router.push('/inspection/tasks/review')">
              进入工作台 →
            </button>
          </div>

          <div v-if="canReviewTasks && canReviewAppeals" class="review-divider" />

          <div v-if="canReviewAppeals" class="review-bucket">
            <div class="review-bucket__head">
              <span class="insp-caps">申诉审核</span>
              <span class="insp-num review-bucket__count">{{ reviewQueue.length }}</span>
            </div>
            <button class="insp-btn review-bucket__cta"
                    :disabled="reviewQueue.length === 0"
                    @click="router.push('/inspection/appeals/review')">
              进入工作台 →
            </button>
          </div>
        </div>
      </section>

      <!-- ─ Slot 3 · 整改责任 ─────── -->
      <section class="ops-card">
        <div class="ops-card__head">
          <div>
            <div class="insp-eyebrow">CORRECTIVE</div>
            <h2 class="ops-card__title">我负责的整改</h2>
          </div>
          <span class="ops-card__pill insp-num">{{ myOpenCases.length }}</span>
        </div>
        <ol class="case-list">
          <li
            v-for="c in myOpenCases.slice(0, 4)" :key="c.id"
            class="case-row"
            @click="router.push(`/inspection/corrective/${c.id}`)"
          >
            <div class="case-row__lead">
              <span class="case-row__code">{{ c.caseCode }}</span>
              <span class="insp-chip" :class="`insp-chip--${caseUrgencyChip(c).variant}`">
                {{ caseUrgencyChip(c).label }}
              </span>
            </div>
            <div class="case-row__issue">{{ c.issueDescription }}</div>
            <div class="case-row__foot insp-num">
              截止 {{ fmtDate(c.deadline) }}
            </div>
          </li>
          <li v-if="!loading && myOpenCases.length === 0" class="task-empty">
            <p class="empty-line">无待办整改</p>
          </li>
        </ol>
      </section>

      <!-- ─ Slot 4 · 我的申诉摘要 ─────── -->
      <section class="ops-card">
        <div class="ops-card__head">
          <div>
            <div class="insp-eyebrow">DOCKET</div>
            <h2 class="ops-card__title">我的申诉</h2>
          </div>
          <button class="insp-btn insp-btn--ghost" @click="router.push('/inspection/appeals/my')">
            查看全部 →
          </button>
        </div>
        <ol class="appeal-list">
          <li v-for="a in recentClosedAppeals" :key="a.id" class="appeal-row">
            <span class="appeal-row__code">{{ a.appealCode }}</span>
            <span class="insp-chip"
                  :class="a.status === 'APPROVED' ? 'insp-chip--pass' : 'insp-chip--fail'">
              {{ a.status === 'APPROVED' ? '通过' : '驳回' }}
            </span>
            <span v-if="a.finalAdjustment != null" class="appeal-row__adj insp-num">
              +{{ Number(a.finalAdjustment).toFixed(1) }}
            </span>
          </li>
          <li v-if="!loading && recentClosedAppeals.length === 0" class="task-empty">
            <p class="empty-line">无申诉记录</p>
          </li>
        </ol>
      </section>

      <!-- ─ Slot 5 · 项目概览 ─────── -->
      <section class="ops-card span-4">
        <div class="ops-card__head">
          <div>
            <div class="insp-eyebrow">CAMPAIGNS</div>
            <h2 class="ops-card__title">项目动态</h2>
          </div>
          <button class="insp-btn insp-btn--ghost" @click="router.push('/inspection/projects')">
            管理项目 →
          </button>
        </div>

        <div class="proj-stats">
          <div class="insp-stat">
            <span class="insp-stat__value">{{ projectStats.total }}</span>
            <span class="insp-stat__label">项目总数</span>
          </div>
          <div class="insp-rule proj-rule" />
          <div class="insp-stat">
            <span class="insp-stat__value">{{ projectStats.active }}</span>
            <span class="insp-stat__label">进行中</span>
          </div>
          <div class="insp-rule proj-rule" />
          <div class="insp-stat">
            <span class="insp-stat__value">{{ projectStats.draft }}</span>
            <span class="insp-stat__label">草稿</span>
          </div>
          <div class="proj-rule-fill" />
          <button class="insp-btn insp-btn--accent insp-btn--lg"
                  @click="router.push('/inspection/projects/create')">
            发起新项目
          </button>
        </div>
      </section>
    </div>

    <!-- ── Editorial colophon ─────────────────────── -->
    <footer class="colophon">
      <hr class="insp-rule" />
      <div class="colophon__row">
        <span class="insp-caps">版面 · Layout v1 · {{ new Date().toLocaleDateString('en-CA') }}</span>
        <span class="insp-caps">共 4 个工作流 · 1 个总台</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.command-deck {
  padding: var(--insp-sp-7) var(--insp-sp-9);
  max-width: 1440px;
  margin: 0 auto;
  min-height: 100vh;
  background:
    radial-gradient(circle at 90% -10%, var(--insp-accent-paler) 0%, transparent 35%),
    var(--insp-bg-page);
}

/* ─ Masthead ─────── */
.masthead {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: var(--insp-sp-7);
  padding-bottom: var(--insp-sp-6);
}

.masthead__title { margin: var(--insp-sp-2) 0 var(--insp-sp-2); }

.masthead__greet {
  font-size: 56px;
  font-weight: 500;
  letter-spacing: -0.025em;
  color: var(--insp-ink-primary);
  display: inline-block;
}

.masthead__date {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-tertiary);
  margin: 0;
}

.masthead__cipher {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: var(--insp-sp-4) var(--insp-sp-5);
  border: 2px solid var(--insp-accent);
  background: var(--insp-bg-surface);
  position: relative;
}

.masthead__cipher::before {
  content: '⌖';
  position: absolute;
  top: -12px;
  right: -12px;
  width: 28px; height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--insp-bg-surface);
  background: var(--insp-accent);
  border-radius: var(--insp-radius-pill);
}

.cipher-num {
  font-family: var(--insp-font-mono);
  font-size: 56px;
  font-weight: 700;
  line-height: 1;
  color: var(--insp-accent);
  letter-spacing: -0.04em;
  font-feature-settings: 'tnum' 1;
}

.cipher-label {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-2xs);
  font-weight: 600;
  letter-spacing: var(--insp-tracking-caps);
  text-transform: uppercase;
  color: var(--insp-accent);
  margin-top: 4px;
}

/* ─ Ops grid ─────── */
.ops-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--insp-sp-5);
  margin-top: var(--insp-sp-6);
}

.span-2 { grid-column: span 2; }
.span-4 { grid-column: span 4; }

.ops-card {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-subtle);
  padding: var(--insp-sp-5);
  display: flex;
  flex-direction: column;
  min-height: 200px;
  transition: border-color var(--insp-t-medium);
}

.ops-card:hover {
  border-color: var(--insp-border-default);
}

.ops-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--insp-sp-3);
  margin-bottom: var(--insp-sp-4);
}

.ops-card__title {
  font-family: var(--insp-font-display);
  font-size: 22px;
  font-weight: 500;
  letter-spacing: var(--insp-tracking-display);
  margin: 4px 0 0;
  color: var(--insp-ink-primary);
}

.ops-card__pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 24px;
  padding: 0 var(--insp-sp-2);
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  font-weight: 600;
  color: var(--insp-bg-surface);
  background: var(--insp-ink-primary);
  border-radius: var(--insp-radius-sm);
}

/* ─ Task list ─────── */
.task-list, .case-list, .appeal-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.task-row {
  display: grid;
  grid-template-columns: 56px 1fr auto auto;
  gap: var(--insp-sp-3);
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}

.task-row:hover { background: var(--insp-bg-subtle); padding-left: 6px; padding-right: 6px; margin-left: -6px; margin-right: -6px; }

.task-row.is-overdue .task-row__date { color: var(--insp-fail); }

.task-row__date {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  font-weight: 600;
  color: var(--insp-ink-secondary);
}

.task-row__code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-row__progress {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  font-weight: 600;
  color: var(--insp-ink-primary);
}

.task-row__progress .dim { color: var(--insp-ink-quaternary); font-weight: 400; }

/* ─ Review split ─────── */
.review-split {
  display: flex;
  align-items: stretch;
  flex: 1;
}

.review-bucket {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: var(--insp-sp-4) var(--insp-sp-3);
}

.review-bucket__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--insp-sp-4);
}

.review-bucket__count {
  font-family: var(--insp-font-mono);
  font-size: 36px;
  font-weight: 600;
  color: var(--insp-accent);
  letter-spacing: -0.04em;
  line-height: 1;
}

.review-bucket__cta {
  width: 100%;
  justify-content: space-between;
}

.review-divider {
  width: 1px;
  background: var(--insp-border-subtle);
  margin: 0 var(--insp-sp-3);
}

/* ─ Case list ─────── */
.case-row {
  padding: var(--insp-sp-3) 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}

.case-row:hover { background: var(--insp-bg-subtle); padding-left: 6px; padding-right: 6px; margin-left: -6px; margin-right: -6px; }

.case-row__lead {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-2);
  margin-bottom: 4px;
}

.case-row__code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  font-weight: 600;
  color: var(--insp-ink-primary);
}

.case-row__issue {
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-secondary);
  line-height: var(--insp-leading-snug);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.case-row__foot {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}

/* ─ Appeal list ─────── */
.appeal-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: var(--insp-sp-2);
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--insp-border-subtle);
}

.appeal-row__code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  font-weight: 500;
  color: var(--insp-ink-primary);
}

.appeal-row__adj {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md);
  font-weight: 600;
  color: var(--insp-pass);
}

/* ─ Project stats ─────── */
.proj-stats {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-6);
  padding: var(--insp-sp-3) 0;
}

.proj-rule {
  width: 1px;
  height: 48px;
  background: var(--insp-border-subtle);
}

.proj-rule-fill { flex: 1; }

.proj-stats .insp-stat__value {
  font-size: 36px;
}

/* ─ Empty ─────── */
.task-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--insp-sp-3);
  padding: var(--insp-sp-7);
}

.empty-line {
  margin: 0;
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-tertiary);
}

/* ─ Colophon ─────── */
.colophon {
  margin-top: var(--insp-sp-9);
}

.colophon__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--insp-sp-3);
  font-size: var(--insp-text-2xs);
  color: var(--insp-ink-tertiary);
}

/* ─ Responsive ─────── */
@media (max-width: 1280px) {
  .ops-grid { grid-template-columns: repeat(2, 1fr); }
  .span-4 { grid-column: span 2; }
}

@media (max-width: 768px) {
  .command-deck { padding: var(--insp-sp-5); }
  .masthead { grid-template-columns: 1fr; }
  .masthead__greet { font-size: 36px; }
  .cipher-num { font-size: 36px; }
  .ops-grid { grid-template-columns: 1fr; }
  .span-2, .span-4 { grid-column: span 1; }
  .proj-stats { flex-wrap: wrap; }
}
</style>
