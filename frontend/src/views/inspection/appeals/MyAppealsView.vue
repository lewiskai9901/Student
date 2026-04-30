<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as appealApi from '@/api/inspection/appeal'
import type { InspAppeal } from '@/types/insp/appeal'
import { AppealStatusConfig, type AppealStatus } from '@/types/insp/enums'

const loading = ref(false)
const appeals = ref<InspAppeal[]>([])
const filter = ref<'all' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'>('all')

const filtered = computed(() => {
  if (filter.value === 'all') return appeals.value
  return appeals.value.filter(a => a.status === filter.value)
})

const counts = computed(() => ({
  all: appeals.value.length,
  PENDING: appeals.value.filter(a => a.status === 'PENDING').length,
  APPROVED: appeals.value.filter(a => a.status === 'APPROVED').length,
  REJECTED: appeals.value.filter(a => a.status === 'REJECTED').length,
  WITHDRAWN: appeals.value.filter(a => a.status === 'WITHDRAWN').length,
}))

const totalRecovered = computed(() =>
  appeals.value
    .filter(a => a.status === 'APPROVED' && a.finalAdjustment != null)
    .reduce((s, a) => s + Number(a.finalAdjustment ?? 0), 0)
)

async function loadData() {
  loading.value = true
  try {
    appeals.value = await appealApi.getMyAppeals()
  } catch (e: any) {
    ElMessage.error(e.message || '加载申诉失败')
  } finally {
    loading.value = false
  }
}

async function handleWithdraw(appeal: InspAppeal) {
  if (appeal.status !== 'PENDING') return
  try {
    await ElMessageBox.confirm(`撤回申诉「${appeal.appealCode}」?`, '撤回申诉', {
      type: 'warning', confirmButtonText: '撤回', cancelButtonText: '取消',
    })
    await appealApi.withdrawAppeal(appeal.id)
    ElMessage.success('已撤回')
    loadData()
  } catch { /* cancelled */ }
}

function statusVariant(s: AppealStatus): string {
  return ({ PENDING: 'warn', APPROVED: 'pass', REJECTED: 'fail', WITHDRAWN: 'pending' } as const)[s] || 'pending'
}

function fmtTime(s?: string) {
  if (!s) return '—'
  const d = new Date(s)
  const M = String(d.getMonth() + 1).padStart(2, '0')
  const D = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${d.getFullYear()}.${M}.${D} ${h}:${m}`
}

onMounted(() => loadData())
</script>

<template>
  <div class="insp-shell my-appeals">
    <!-- ── Editorial header ─────────────────────────────────── -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">个人卷宗 / Personal Docket</div>
        <h1 class="insp-display page-title">我的申诉</h1>
      </div>
      <div class="head-stats">
        <div class="insp-stat">
          <span class="insp-stat__value">{{ counts.all }}</span>
          <span class="insp-stat__label">总申诉</span>
        </div>
        <div class="insp-rule head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value">{{ counts.PENDING }}</span>
          <span class="insp-stat__label">待审</span>
        </div>
        <div class="insp-rule head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value">+{{ totalRecovered.toFixed(1) }}</span>
          <span class="insp-stat__label">累计追回</span>
        </div>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong head-divider" />

    <!-- ── Filter rail ─────────────────────────────────── -->
    <nav class="filter-rail">
      <button
        v-for="opt in [
          { key: 'all', label: '全部' },
          { key: 'PENDING', label: '待审' },
          { key: 'APPROVED', label: '通过' },
          { key: 'REJECTED', label: '驳回' },
          { key: 'WITHDRAWN', label: '撤回' },
        ]" :key="opt.key"
        class="filter-tab" :class="{ 'is-active': filter === opt.key }"
        @click="filter = opt.key as any"
      >
        <span class="filter-tab__label">{{ opt.label }}</span>
        <span class="filter-tab__count">{{ counts[opt.key as keyof typeof counts] }}</span>
      </button>
    </nav>

    <!-- ── Document list ─────────────────────────────────── -->
    <section v-loading="loading" class="docket">
      <article v-for="a in filtered" :key="a.id" class="docket-row">
        <div class="row-num insp-num">{{ String(a.id).padStart(3, '0') }}</div>

        <div class="row-meta">
          <div class="row-code-line">
            <span class="row-code">{{ a.appealCode }}</span>
            <span class="insp-chip" :class="`insp-chip--${statusVariant(a.status)}`">
              {{ AppealStatusConfig[a.status]?.label || a.status }}
            </span>
          </div>
          <div class="row-time">{{ fmtTime(a.createdAt) }}</div>
        </div>

        <div class="row-reason">
          <div class="row-reason__title">{{ a.reason }}</div>
          <div v-if="a.reviewerComment" class="row-reason__verdict">
            <span class="insp-caps">审核意见</span> {{ a.reviewerComment }}
          </div>
        </div>

        <div class="row-adj">
          <div v-if="a.expectedAdjustment != null" class="adj-pair">
            <span class="adj-label">期望</span>
            <span class="adj-value insp-num">{{ Number(a.expectedAdjustment).toFixed(1) }}</span>
          </div>
          <div v-if="a.status === 'APPROVED' && a.finalAdjustment != null" class="adj-pair adj-pair--final">
            <span class="adj-label">实际</span>
            <span class="adj-value insp-num">{{ Number(a.finalAdjustment).toFixed(1) }}</span>
          </div>
        </div>

        <div class="row-action">
          <button
            v-if="a.status === 'PENDING'"
            class="insp-btn insp-btn--sm" @click="handleWithdraw(a)"
          >撤回</button>
          <span v-else class="row-action__placeholder">—</span>
        </div>
      </article>

      <div v-if="!loading && filtered.length === 0" class="empty">
        <div class="insp-stamp">无记录</div>
        <p class="empty-hint">
          {{ filter === 'all' ? '尚未提交过申诉' : '此分类下没有申诉记录' }}
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.my-appeals {
  padding: 32px 48px 64px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100vh;
}

/* ─ Header ─────────────── */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--insp-sp-7);
  margin-bottom: var(--insp-sp-4);
}

.page-title {
  font-size: 44px;
  margin: 0;
  font-weight: 500;
  color: var(--insp-ink-primary);
}

.head-stats {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-6);
  padding: 4px 0;
}

.head-rule {
  width: 1px;
  height: 32px;
  background: var(--insp-border-default);
}

.head-divider {
  margin: 0 0 var(--insp-sp-7);
}

/* ─ Filter rail ─────────────── */
.filter-rail {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: var(--insp-sp-6);
  border-bottom: 1px solid var(--insp-border-subtle);
}

.filter-tab {
  position: relative;
  display: inline-flex;
  align-items: baseline;
  gap: var(--insp-sp-2);
  padding: 10px 18px 12px;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  font-size: var(--insp-text-md);
  font-weight: 500;
  color: var(--insp-ink-tertiary);
  transition: color var(--insp-t-fast);
}

.filter-tab:hover { color: var(--insp-ink-primary); }

.filter-tab.is-active {
  color: var(--insp-ink-primary);
}

.filter-tab.is-active::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: -1px;
  height: 2px;
  background: var(--insp-accent);
}

.filter-tab__count {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  font-weight: 500;
  color: var(--insp-ink-quaternary);
  font-feature-settings: 'tnum' 1;
}

.filter-tab.is-active .filter-tab__count {
  color: var(--insp-accent);
}

/* ─ Docket rows ─────────────── */
.docket {
  display: flex;
  flex-direction: column;
}

.docket-row {
  display: grid;
  grid-template-columns: 56px 220px 1fr 200px 88px;
  gap: var(--insp-sp-5);
  align-items: start;
  padding: var(--insp-sp-5) 0;
  border-bottom: 1px solid var(--insp-border-subtle);
  transition: background var(--insp-t-fast);
}

.docket-row:hover {
  background: linear-gradient(to right, var(--insp-bg-subtle), transparent 80%);
}

.row-num {
  font-size: 26px;
  font-weight: 500;
  color: var(--insp-ink-quaternary);
  letter-spacing: -0.02em;
  line-height: 1;
  padding-top: 2px;
}

.row-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row-code-line {
  display: flex;
  align-items: center;
  gap: var(--insp-sp-2);
  flex-wrap: wrap;
}

.row-code {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-md);
  font-weight: 600;
  color: var(--insp-ink-primary);
  font-feature-settings: 'tnum' 1;
}

.row-time {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
}

.row-reason {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.row-reason__title {
  font-size: var(--insp-text-md);
  line-height: var(--insp-leading-snug);
  color: var(--insp-ink-primary);
  word-break: break-word;
}

.row-reason__verdict {
  font-size: var(--insp-text-sm);
  line-height: var(--insp-leading-snug);
  color: var(--insp-ink-secondary);
  padding-left: 10px;
  border-left: 2px solid var(--insp-border-default);
}

.row-reason__verdict .insp-caps {
  display: inline;
  margin-right: 6px;
}

/* ─ Adjustments column ─────────────── */
.row-adj {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
  text-align: right;
  font-feature-settings: 'tnum' 1;
}

.adj-pair {
  display: flex;
  align-items: baseline;
  gap: var(--insp-sp-2);
}

.adj-label {
  font-size: var(--insp-text-2xs);
  font-weight: 500;
  letter-spacing: var(--insp-tracking-caps);
  text-transform: uppercase;
  color: var(--insp-ink-tertiary);
}

.adj-value {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-lg);
  font-weight: 500;
  color: var(--insp-ink-secondary);
  letter-spacing: -0.02em;
}

.adj-pair--final .adj-value {
  color: var(--insp-pass);
  font-weight: 600;
}

/* ─ Actions ─────────────── */
.row-action {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  padding-top: 2px;
}

.row-action__placeholder {
  color: var(--insp-ink-quaternary);
  font-family: var(--insp-font-mono);
}

/* ─ Empty ─────────────── */
.empty {
  padding: 80px 0;
  text-align: center;
}

.empty-hint {
  margin-top: var(--insp-sp-4);
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-sm);
}

/* ─ Responsive ─────────────── */
@media (max-width: 960px) {
  .my-appeals { padding: 20px 16px 64px; }
  .page-title { font-size: 32px; }
  .head-stats { gap: var(--insp-sp-4); }
  .docket-row {
    grid-template-columns: 44px 1fr;
    gap: var(--insp-sp-3);
  }
  .row-reason, .row-adj, .row-action {
    grid-column: 2;
  }
}
</style>
