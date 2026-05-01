<script setup lang="ts">
/**
 * CorrectiveCaseListView — 整改案例总册 (Audit Console redesign)
 * 卷宗式列表 · 带统计刊头 · 标签栏(计数) · 行内紧急度+逾期提示
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useInspCorrectiveStore } from '@/stores/inspection/inspCorrectiveStore'
import {
  CaseStatusConfig, CasePriorityConfig,
  type CaseStatus, type CasePriority,
} from '@/types/insp/enums'
import type { CorrectiveCase } from '@/types/insp/corrective'

const router = useRouter()
const store = useInspCorrectiveStore()

const loading = ref(false)
const cases = ref<CorrectiveCase[]>([])
const activeTab = ref<'all' | 'my' | 'overdue'>('all')

// ── Loaders ──
async function loadData() {
  loading.value = true
  try {
    if (activeTab.value === 'my') {
      await store.fetchMyCases()
      cases.value = store.myCases
    } else if (activeTab.value === 'overdue') {
      await store.fetchOverdueCases()
      cases.value = store.overdueCases
    } else {
      await store.fetchCases({})
      cases.value = store.cases
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function changeTab(t: 'all' | 'my' | 'overdue') {
  activeTab.value = t
  loadData()
}

// ── Aggregate stats ──
const stats = computed(() => {
  const list = cases.value
  return {
    total: list.length,
    open: list.filter(c => ['OPEN', 'ASSIGNED', 'IN_PROGRESS'].includes(c.status)).length,
    overdue: list.filter(c => isOverdue(c)).length,
    closed: list.filter(c => c.status === 'CLOSED' || c.status === 'VERIFIED').length,
    critical: list.filter(c => c.priority === 'CRITICAL' || c.priority === 'HIGH').length,
  }
})

// ── Helpers ──
function isOverdue(c: CorrectiveCase): boolean {
  if (!c.deadline) return false
  if (c.status === 'CLOSED' || c.status === 'VERIFIED') return false
  return new Date() > new Date(c.deadline)
}

function statusVariant(s: CaseStatus): string {
  const v: Record<CaseStatus, string> = {
    OPEN: 'pending',
    ASSIGNED: 'info',
    IN_PROGRESS: 'warn',
    SUBMITTED: 'info',
    VERIFIED: 'pass',
    REJECTED: 'fail',
    CLOSED: 'pass',
    ESCALATED: 'fail',
  }
  return v[s] || 'pending'
}

function priorityVariant(p: CasePriority): string {
  const v: Record<CasePriority, string> = {
    LOW: 'pending',
    MEDIUM: 'info',
    HIGH: 'warn',
    CRITICAL: 'fail',
  }
  return v[p] || 'pending'
}

function fmtDate(s?: string | null) {
  if (!s) return '—'
  const d = new Date(s)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function daysUntil(deadline?: string | null): number | null {
  if (!deadline) return null
  const ms = new Date(deadline).getTime() - Date.now()
  return Math.ceil(ms / (1000 * 60 * 60 * 24))
}

function deadlineLabel(c: CorrectiveCase): string {
  if (!c.deadline) return '无期限'
  const d = daysUntil(c.deadline)
  if (d === null) return ''
  if (d < 0) return `逾期 ${Math.abs(d)} 天`
  if (d === 0) return '今日截止'
  if (d === 1) return '明日截止'
  return `剩余 ${d} 天`
}

function goDetail(c: CorrectiveCase) {
  router.push(`/inspection/corrective/${c.id}`)
}

async function handleEscalate(c: CorrectiveCase) {
  try {
    await ElMessageBox.confirm(`升级案例「${c.caseCode}」?`, '确认升级', { type: 'warning' })
    await store.escalateCase(c.id)
    ElMessage.success('已升级')
    loadData()
  } catch { /* cancelled */ }
}

async function handleDelete(c: CorrectiveCase) {
  try {
    await ElMessageBox.confirm(`删除整改案例「${c.caseCode}」?`, '确认删除', { type: 'warning' })
    await store.removeCase(c.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

onMounted(() => loadData())
</script>

<template>
  <div class="insp-shell case-register">
    <!-- ── Editorial header ─────────── -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">整改总册 / Corrective Register</div>
        <h1 class="page-title insp-display">整改案例</h1>
      </div>
      <div class="head-stats">
        <div class="insp-stat">
          <span class="insp-stat__value">{{ stats.total }}</span>
          <span class="insp-stat__label">案件总数</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: 'var(--insp-warn)' }">{{ stats.open }}</span>
          <span class="insp-stat__label">未结</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: stats.overdue > 0 ? 'var(--insp-fail)' : 'var(--insp-ink-primary)' }">{{ stats.overdue }}</span>
          <span class="insp-stat__label">逾期</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: 'var(--insp-pass)' }">{{ stats.closed }}</span>
          <span class="insp-stat__label">已结案</span>
        </div>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong head-divider" />

    <!-- ── Filter tabs ─────────── -->
    <nav class="filter-rail">
      <button class="filter-tab" :class="{ 'is-active': activeTab === 'all' }" @click="changeTab('all')">
        <span class="filter-tab__label">全部案件</span>
        <span class="filter-tab__count">{{ cases.length }}</span>
      </button>
      <button class="filter-tab" :class="{ 'is-active': activeTab === 'my' }" @click="changeTab('my')">
        <span class="filter-tab__label">我负责的</span>
      </button>
      <button class="filter-tab filter-tab--alert" :class="{ 'is-active': activeTab === 'overdue' }" @click="changeTab('overdue')">
        <span class="filter-tab__label">逾期案件</span>
        <span class="filter-tab__count" :style="{ color: 'var(--insp-fail)' }">{{ stats.overdue }}</span>
      </button>
      <div class="filter-spacer" />
      <button class="insp-btn insp-btn--ghost" @click="loadData">刷新</button>
    </nav>

    <!-- ── Document register ─────────── -->
    <section v-loading="loading" class="register">
      <article
        v-for="c in cases" :key="c.id"
        class="register-row"
        :class="{ 'is-overdue': isOverdue(c) }"
        @click="goDetail(c)"
      >
        <!-- Number -->
        <div class="row-num insp-num">{{ String(c.id).padStart(3, '0') }}</div>

        <!-- Code + status badges -->
        <div class="row-meta">
          <div class="row-code-line">
            <span class="row-code">{{ c.caseCode }}</span>
            <span class="insp-chip" :class="`insp-chip--${priorityVariant(c.priority)}`">
              {{ CasePriorityConfig[c.priority]?.label }}
            </span>
            <span class="insp-chip" :class="`insp-chip--${statusVariant(c.status)}`">
              {{ CaseStatusConfig[c.status]?.label }}
            </span>
            <span v-if="c.escalationLevel && c.escalationLevel > 0" class="insp-stamp">
              升级 L{{ c.escalationLevel }}
            </span>
          </div>
          <div class="row-target">
            <span v-if="c.targetName">{{ c.targetName }}</span>
            <span v-if="c.assigneeName" class="row-assignee">
              · 责任人 {{ c.assigneeName }}
            </span>
          </div>
        </div>

        <!-- Issue description -->
        <div class="row-issue">{{ c.issueDescription }}</div>

        <!-- Deadline column -->
        <div class="row-deadline" :class="{ 'is-overdue': isOverdue(c) }">
          <div class="row-deadline__date insp-num">{{ fmtDate(c.deadline) }}</div>
          <div class="row-deadline__rel">{{ deadlineLabel(c) }}</div>
        </div>

        <!-- Actions -->
        <div class="row-actions" @click.stop>
          <button class="insp-btn insp-btn--sm" @click="goDetail(c)">详情</button>
          <button
            v-if="c.status !== 'CLOSED' && c.status !== 'ESCALATED'"
            class="insp-btn insp-btn--sm" @click="handleEscalate(c)"
          >升级</button>
          <button class="insp-btn insp-btn--sm insp-btn--ghost" @click="handleDelete(c)" title="删除">
            ×
          </button>
        </div>
      </article>

      <div v-if="!loading && cases.length === 0" class="empty">
        <div class="insp-stamp">无记录</div>
        <p class="empty-hint">
          {{ activeTab === 'overdue' ? '当前没有逾期案件' :
             activeTab === 'my' ? '您未负责任何整改案件' : '尚未创建任何整改案件' }}
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.case-register {
  padding: 32px 48px 64px;
  max-width: 1500px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--insp-bg-page);
}

/* ─ Editorial header ─────── */
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
  border: 0; background: transparent;
  cursor: pointer;
  font-family: inherit;
  font-size: var(--insp-text-md); font-weight: 500;
  color: var(--insp-ink-tertiary);
  transition: color var(--insp-t-fast);
}
.filter-tab:hover { color: var(--insp-ink-primary); }
.filter-tab.is-active { color: var(--insp-ink-primary); }
.filter-tab.is-active::after {
  content: '';
  position: absolute; left: 18px; right: 18px; bottom: -1px;
  height: 2px; background: var(--insp-accent);
}
.filter-tab__count {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs); font-weight: 500;
  color: var(--insp-ink-quaternary);
}
.filter-tab.is-active .filter-tab__count { color: var(--insp-accent); }
.filter-spacer { flex: 1; }

/* ─ Register rows ─────── */
.register { display: flex; flex-direction: column; }

.register-row {
  display: grid;
  grid-template-columns: 56px 280px 1fr 140px 200px;
  gap: var(--insp-sp-5);
  align-items: start;
  padding: var(--insp-sp-5) 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  cursor: pointer;
  transition: background var(--insp-t-fast);
}
.register-row:hover {
  background: linear-gradient(to right, var(--insp-bg-subtle), transparent 80%);
}
.register-row.is-overdue {
  background: linear-gradient(to right, var(--insp-fail-pale), transparent 60%);
}

.row-num {
  font-size: 26px; font-weight: 500;
  color: var(--insp-ink-quaternary);
  letter-spacing: -0.02em; line-height: 1;
  padding-top: 2px;
}

.row-meta { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.row-code-line {
  display: flex; align-items: center; gap: var(--insp-sp-2);
  flex-wrap: wrap;
}
.row-code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md); font-weight: 600;
  color: var(--insp-ink-primary);
}
.row-target {
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-secondary);
}
.row-assignee { color: var(--insp-ink-tertiary); margin-left: 4px; }

.row-issue {
  font-size: var(--insp-text-md);
  line-height: var(--insp-leading-snug);
  color: var(--insp-ink-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.row-deadline {
  display: flex; flex-direction: column; gap: 4px;
  align-items: flex-end;
  text-align: right;
}
.row-deadline__date {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md); font-weight: 600;
  color: var(--insp-ink-primary);
}
.row-deadline__rel {
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}
.row-deadline.is-overdue .row-deadline__date,
.row-deadline.is-overdue .row-deadline__rel {
  color: var(--insp-fail);
}
.row-deadline.is-overdue .row-deadline__rel { font-weight: 500; }

.row-actions {
  display: flex; gap: var(--insp-sp-2);
  justify-content: flex-end;
  align-items: flex-start;
  padding-top: 2px;
}

/* ─ Empty ─────── */
.empty { padding: 80px 0; text-align: center; }
.empty-hint {
  margin-top: var(--insp-sp-4);
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-sm);
}

/* ─ Responsive ─────── */
@media (max-width: 1200px) {
  .register-row {
    grid-template-columns: 44px 240px 1fr 200px;
  }
  .row-deadline { display: none; }
  .row-actions { grid-column: 4; }
}
@media (max-width: 800px) {
  .case-register { padding: 20px 16px 64px; }
  .page-title { font-size: 32px; }
  .head-stats { gap: var(--insp-sp-4); flex-wrap: wrap; }
  .register-row {
    grid-template-columns: 44px 1fr;
    gap: var(--insp-sp-3);
  }
  .row-issue, .row-deadline, .row-actions { grid-column: 2; }
}
</style>
