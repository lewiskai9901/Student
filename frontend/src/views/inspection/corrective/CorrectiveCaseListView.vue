<script setup lang="ts">
/**
 * CorrectiveCaseListView — 整改案例总册 (Audit Console redesign)
 * 卷宗式列表 · 带统计刊头 · 标签栏(计数) · 行内紧急度+逾期提示
 */
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Keyboard } from 'lucide-vue-next'
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

// ============== S+ 设计样板 ==============
const searchKw = ref('')
function highlightHtml(text: string, kw: string): string {
  if (!kw) return text
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark class="cc-mark">$1</mark>')
}
const filteredCases = computed(() => {
  const q = searchKw.value.trim().toLowerCase()
  if (!q) return cases.value
  return cases.value.filter(c =>
    c.caseCode.toLowerCase().includes(q) ||
    (c.title || '').toLowerCase().includes(q) ||
    (c.assigneeName || '').toLowerCase().includes(q)
  )
})

const focusedIdx = ref<number>(-1)
function focusNext() { focusedIdx.value = Math.min(focusedIdx.value + 1, filteredCases.value.length - 1); nextTick(scrollFocused) }
function focusPrev() { focusedIdx.value = Math.max(0, focusedIdx.value - 1); nextTick(scrollFocused) }
function scrollFocused() {
  const cur = filteredCases.value[focusedIdx.value]
  if (!cur) return
  document.querySelector(`[data-cc-id="${cur.id}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

const showKbdHint = ref(localStorage.getItem('insp_cc_kbd_hint_dismissed') !== '1')
function dismissKbdHint() { showKbdHint.value = false; localStorage.setItem('insp_cc_kbd_hint_dismissed', '1') }

function onGlobalKeyCC(e: KeyboardEvent) {
  const t = e.target as HTMLElement
  if (t?.tagName === 'INPUT' || t?.tagName === 'TEXTAREA' || t?.isContentEditable) return
  if (e.metaKey || e.ctrlKey || e.altKey) return
  switch (e.key) {
    case 'j': case 'ArrowDown': e.preventDefault(); focusNext(); break
    case 'k': case 'ArrowUp': e.preventDefault(); focusPrev(); break
    case 'Enter': case 'e': {
      const cur = filteredCases.value[focusedIdx.value]
      if (cur) { e.preventDefault(); goDetail(cur) }
      break
    }
    case '/': e.preventDefault(); (document.querySelector('.cc-search-input') as HTMLElement)?.focus(); break
    case 'Escape': focusedIdx.value = -1; break
  }
}

// 悬浮预览
const previewVisible = ref(false)
const previewTarget = ref<CorrectiveCase | null>(null)
const previewPos = ref({ top: '0px', left: '0px' })
let previewShowTimer: any = null
let previewHideTimer: any = null
function showPreview(c: CorrectiveCase, e: MouseEvent) {
  clearTimeout(previewHideTimer)
  clearTimeout(previewShowTimer)
  const target = e.currentTarget as HTMLElement
  previewShowTimer = setTimeout(() => {
    previewTarget.value = c
    const rect = target.getBoundingClientRect()
    const pw = 320
    if (rect.right + pw + 16 < window.innerWidth) previewPos.value = { top: `${rect.top}px`, left: `${rect.right + 8}px` }
    else previewPos.value = { top: `${rect.bottom + 8}px`, left: `${rect.left}px` }
    previewVisible.value = true
  }, 600)
}
function hidePreview() {
  clearTimeout(previewShowTimer)
  previewHideTimer = setTimeout(() => { previewVisible.value = false }, 100)
}
function keepPreview() { clearTimeout(previewHideTimer) }

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

onMounted(() => {
  loadData()
  window.addEventListener('keydown', onGlobalKeyCC)
})
onUnmounted(() => window.removeEventListener('keydown', onGlobalKeyCC))
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

    <!-- 键盘快捷键提示 (S+) -->
    <div v-if="showKbdHint" class="cc-kbd-hint">
      <Keyboard :size="12" />
      <span class="cc-kbd-hint__group"><kbd class="insp-kbd">J</kbd><kbd class="insp-kbd">K</kbd> 浏览</span>
      <span class="cc-kbd-hint__group"><kbd class="insp-kbd">E</kbd> 详情</span>
      <span class="cc-kbd-hint__group"><kbd class="insp-kbd">/</kbd> 搜索</span>
      <input class="cc-search-input" v-model="searchKw" placeholder="搜索 编码 / 标题 / 责任人..." />
      <button class="cc-kbd-hint__close" @click="dismissKbdHint" title="不再显示">×</button>
    </div>

    <!-- ── Document register ─────────── -->
    <section v-loading="loading" class="register">
      <article
        v-for="(c, i) in filteredCases" :key="c.id"
        :data-cc-id="c.id"
        class="register-row"
        :class="{ 'is-overdue': isOverdue(c), 'is-focused': focusedIdx === i }"
        @click="goDetail(c)"
        @mouseenter="(e) => showPreview(c, e)"
        @mouseleave="hidePreview"
      >
        <!-- Number -->
        <div class="row-num insp-num">{{ String(c.id).padStart(3, '0') }}</div>

        <!-- Code + status badges -->
        <div class="row-meta">
          <div class="row-code-line">
            <span class="row-code" v-html="highlightHtml(c.caseCode, searchKw)"></span>
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

    <!-- 整改案例悬浮预览 (S+) -->
    <Teleport to="body">
      <Transition name="cc-popover">
        <div v-if="previewVisible && previewTarget"
             class="cc-popover"
             :style="{ top: previewPos.top, left: previewPos.left }"
             @mouseenter="keepPreview"
             @mouseleave="hidePreview">
          <div class="cc-popover__head">
            <div class="cc-popover__title-line">
              <span class="cc-popover__code">{{ previewTarget.caseCode }}</span>
              <span class="insp-chip" :class="`insp-chip--${priorityVariant(previewTarget.priority)}`">
                {{ CasePriorityConfig[previewTarget.priority]?.label }}
              </span>
              <span class="insp-chip" :class="`insp-chip--${statusVariant(previewTarget.status)}`">
                {{ CaseStatusConfig[previewTarget.status]?.label }}
              </span>
            </div>
            <div v-if="previewTarget.title" class="cc-popover__title">{{ previewTarget.title }}</div>
          </div>
          <div class="cc-popover__body">
            <div v-if="previewTarget.targetName" class="cc-popover__row">
              <span class="cc-popover__label">受检对象</span>
              <span>{{ previewTarget.targetName }}</span>
            </div>
            <div v-if="previewTarget.assigneeName" class="cc-popover__row">
              <span class="cc-popover__label">责任人</span>
              <span>{{ previewTarget.assigneeName }}</span>
            </div>
            <div v-if="previewTarget.deadline" class="cc-popover__row" :class="{ 'cc-popover__row--late': isOverdue(previewTarget) }">
              <span class="cc-popover__label">截止</span>
              <span class="insp-num">{{ fmtDate(previewTarget.deadline) }}</span>
              <span class="cc-popover__deadline-label" :class="{ 'is-urgent': isOverdue(previewTarget) }">
                {{ deadlineLabel(previewTarget) }}
              </span>
            </div>
            <div v-if="previewTarget.escalationLevel && previewTarget.escalationLevel > 0" class="cc-popover__row">
              <span class="cc-popover__label">升级</span>
              <span class="insp-stamp">L{{ previewTarget.escalationLevel }}</span>
            </div>
          </div>
          <div class="cc-popover__foot">点击查看完整详情 · 按 E 进入</div>
        </div>
      </Transition>
    </Teleport>
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

/* ─ S+ 搜索高亮 ─────── */
:deep(.cc-mark) {
  background: rgba(245, 200, 70, 0.4);
  color: var(--insp-ink-primary);
  padding: 0 2px;
  border-radius: 2px;
  font-weight: 600;
}

/* ─ S+ 键盘聚焦 ─────── */
.register-row.is-focused {
  outline: 2px solid var(--insp-accent);
  outline-offset: -2px;
}

/* ─ S+ 键盘提示条 + 搜索框 ─────── */
.cc-kbd-hint {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 6px 12px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, rgba(26, 109, 255, 0.06) 0%, transparent 100%);
  border: 1px solid rgba(26, 109, 255, 0.18);
  border-radius: var(--insp-radius-md);
  font-size: 11px;
  color: var(--insp-ink-secondary);
}
.cc-kbd-hint__group { display: inline-flex; align-items: center; gap: 4px; }
.cc-search-input {
  flex: 1;
  max-width: 240px;
  margin-left: auto;
  height: 24px;
  padding: 0 8px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-sm);
  font-size: 11px;
  background: var(--insp-bg-surface);
  font-family: inherit;
}
.cc-search-input:focus {
  outline: none;
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}
.cc-kbd-hint__close {
  width: 20px; height: 20px;
  border: 0;
  background: transparent;
  font-size: 16px;
  color: var(--insp-ink-quaternary);
  border-radius: 3px;
  cursor: pointer;
}
.cc-kbd-hint__close:hover { background: rgba(0,0,0,0.06); color: var(--insp-ink-primary); }

/* ─ S+ 整改悬浮预览卡 ─────── */
.cc-popover {
  position: fixed;
  z-index: 9500;
  width: 320px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-strong);
  border-radius: var(--insp-radius-lg);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.cc-popover__head {
  padding: 12px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.cc-popover__title-line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 5px;
}
.cc-popover__code {
  font-family: var(--insp-font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--insp-ink-primary);
}
.cc-popover__title {
  font-size: 12.5px;
  color: var(--insp-ink-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.cc-popover__body {
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.cc-popover__row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--insp-ink-secondary);
}
.cc-popover__label {
  width: 60px;
  color: var(--insp-ink-tertiary);
  font-size: 10.5px;
  letter-spacing: 0.04em;
  flex-shrink: 0;
}
.cc-popover__row--late {
  background: rgba(239, 68, 68, 0.08);
  margin: 0 -14px;
  padding: 4px 14px;
  color: var(--insp-fail);
  font-weight: 500;
}
.cc-popover__deadline-label {
  margin-left: auto;
  color: var(--insp-ink-tertiary);
  font-size: 10.5px;
}
.cc-popover__deadline-label.is-urgent { color: var(--insp-fail); font-weight: 600; }
.cc-popover__foot {
  padding: 8px 14px;
  background: var(--insp-bg-subtle);
  border-top: 1px solid var(--insp-border-subtle);
  font-size: 10.5px;
  color: var(--insp-ink-tertiary);
  text-align: center;
}
.cc-popover-enter-active, .cc-popover-leave-active { transition: all 0.15s ease; }
.cc-popover-enter-from { opacity: 0; transform: translateX(-4px) scale(0.98); }
.cc-popover-leave-to { opacity: 0; }
</style>
