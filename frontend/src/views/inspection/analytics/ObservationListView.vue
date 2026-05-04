<template>
  <div class="insp-shell obs-ledger">
    <!-- ── Editorial header ─────────── -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">观察记录 / Observations</div>
        <h1 class="insp-display page-title">评分观察</h1>
      </div>
      <div class="head-stats">
        <div class="insp-stat">
          <span class="insp-stat__value">{{ total }}</span>
          <span class="insp-stat__label">观察总数</span>
        </div>
        <div class="head-rule" />
        <div class="insp-stat">
          <span class="insp-stat__value" :style="{ color: 'var(--insp-fail)' }">{{ negativeCount }}</span>
          <span class="insp-stat__label">负面记录</span>
        </div>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong head-divider" />

    <!-- ── Filter rail ─────────── -->
    <nav class="filter-rail">
      <div class="filter-group">
        <span class="insp-caps">主体</span>
        <select v-model="filters.subjectType" class="filter-select" @change="loadData">
          <option value="">全部</option>
          <option value="USER">用户</option>
          <option value="ORG_UNIT">组织</option>
          <option value="PLACE">场所</option>
        </select>
      </div>
      <div class="filter-group">
        <span class="insp-caps">严重度</span>
        <select v-model="filters.severity" class="filter-select" @change="loadData">
          <option value="">全部</option>
          <option value="LOW">轻微</option>
          <option value="MEDIUM">中等</option>
          <option value="HIGH">严重</option>
          <option value="CRITICAL">危急</option>
        </select>
      </div>
      <label class="neg-toggle" :class="{ 'is-active': filters.isNegative }">
        <input type="checkbox" v-model="filters.isNegative" @change="loadData" />
        仅显示负面
      </label>
      <div class="filter-spacer" />
      <button v-if="hasFilters" class="insp-btn insp-btn--ghost" @click="resetFilters">清除筛选</button>
      <button class="insp-btn insp-btn--ghost" @click="loadData">刷新</button>
    </nav>

    <!-- ── Ledger table ─────────── -->
    <section v-loading="loading" class="ledger-wrap">
      <table class="insp-table ledger-table">
        <thead>
          <tr>
            <th class="th-time">时间</th>
            <th>检查项</th>
            <th>主体</th>
            <th>班级</th>
            <th class="th-num">分数</th>
            <th>严重度</th>
            <th>事件类型</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.id" class="ledger-row" :class="{ 'is-neg': (row.score || 0) < 0 }">
            <td class="td-time insp-num">{{ formatTime(row.observedAt) }}</td>
            <td class="td-name">{{ row.itemName || '—' }}</td>
            <td>
              <span class="insp-chip" :class="'subject-' + row.subjectType">
                {{ row.subjectName || '—' }}
              </span>
            </td>
            <td class="td-muted">{{ row.className || '—' }}</td>
            <td class="td-num insp-num" :class="{ 'is-neg': (row.score || 0) < 0 }">{{ row.score }}</td>
            <td>
              <span v-if="row.severity" class="insp-chip" :class="`insp-chip--${severityChipVariant(row.severity)}`">
                {{ severityLabel(row.severity) }}
              </span>
              <span v-else class="td-muted">—</span>
            </td>
            <td class="td-muted insp-num">{{ row.linkedEventTypeCode || '—' }}</td>
          </tr>
          <tr v-if="!loading && records.length === 0">
            <td colspan="7" class="empty-cell">
              <div class="insp-stamp">无观察记录</div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="total > filters.size" class="pagination">
        <button class="insp-btn insp-btn--sm" :disabled="filters.page <= 1" @click="filters.page--; loadData()">< 上一页</button>
        <span class="page-info insp-num">{{ filters.page }} / {{ totalPages }}</span>
        <button class="insp-btn insp-btn--sm" :disabled="filters.page >= totalPages" @click="filters.page++; loadData()">下一页 ></button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { observationApi } from '@/api/observation'
import type { SubmissionObservation } from '@/types/observation'
import { SEVERITY_CONFIG } from '@/types/observation'

const loading = ref(false)
const records = ref<SubmissionObservation[]>([])
const total = ref(0)
const negativeCount = ref(0)

const filters = reactive({
  subjectType: '',
  severity: '',
  isNegative: true,
  page: 1,
  size: 50,
})

const hasFilters = computed(() => filters.subjectType || filters.severity || !filters.isNegative)
const totalPages = computed(() => Math.ceil(total.value / filters.size))

function resetFilters() {
  filters.subjectType = ''
  filters.severity = ''
  filters.isNegative = true
  filters.page = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: filters.page,
      size: filters.size,
    }
    if (filters.subjectType) params.subjectType = filters.subjectType
    if (filters.severity) params.severity = filters.severity
    if (filters.isNegative) params.isNegative = true

    const data = await observationApi.list(params)
    records.value = data.records || []
    total.value = data.total || 0

    // Count negatives
    if (!filters.isNegative) {
      const negData = await observationApi.list({ ...params, isNegative: true, size: 1 })
      negativeCount.value = negData.total || 0
    } else {
      negativeCount.value = total.value
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function severityLabel(s: string) { return SEVERITY_CONFIG[s]?.label || s }
function severityChipVariant(s: string): string {
  return ({ LOW: 'pending', MEDIUM: 'info', HIGH: 'warn', CRITICAL: 'fail' } as Record<string, string>)[s] || 'pending'
}

function formatTime(t: string) {
  if (!t) return '--'
  return t.replace('T', ' ').substring(0, 16)
}

onMounted(() => loadData())
</script>

<style scoped>
.obs-ledger {
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
  display: flex; align-items: center; gap: var(--insp-sp-4);
  padding: var(--insp-sp-3) 0;
  margin-bottom: var(--insp-sp-5);
  border-bottom: 1px solid var(--insp-border-subtle);
  flex-wrap: wrap;
}
.filter-group {
  display: flex; align-items: center; gap: var(--insp-sp-2);
}
.filter-group .insp-caps { color: var(--insp-ink-tertiary); }
.filter-select {
  padding: 6px 26px 6px 10px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-family: inherit;
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-primary);
  background: var(--insp-bg-surface) url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%238A8A82' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 8px center no-repeat;
  appearance: none; cursor: pointer; outline: none;
}
.filter-select:focus {
  border-color: var(--insp-accent);
  box-shadow: 0 0 0 3px var(--insp-accent-paler);
}
.neg-toggle {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-md);
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-secondary);
  cursor: pointer; user-select: none;
  transition: all var(--insp-t-fast);
}
.neg-toggle input { accent-color: var(--insp-accent); }
.neg-toggle.is-active {
  border-color: var(--insp-accent);
  color: var(--insp-accent);
  background: var(--insp-accent-paler);
}
.filter-spacer { flex: 1; }

/* ─ Ledger table ─────── */
.ledger-wrap {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-md);
  overflow: hidden;
}
.ledger-table { width: 100%; }
.ledger-table th { white-space: nowrap; }
.th-time { width: 130px; }
.th-num { text-align: right; width: 80px; }
.ledger-row { transition: background var(--insp-t-fast); }
.ledger-row.is-neg {
  background: linear-gradient(to right, var(--insp-fail-pale), transparent 70%);
}
.td-time {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs);
  color: var(--insp-ink-tertiary);
  white-space: nowrap;
}
.td-name {
  font-weight: 500;
  color: var(--insp-ink-primary);
}
.td-muted { color: var(--insp-ink-tertiary); }
.td-num {
  font-family: var(--insp-font-mono);
  font-weight: 600;
  text-align: right;
}
.td-num.is-neg { color: var(--insp-fail); }

.subject-USER { background: var(--insp-info-pale); color: var(--insp-info); border-color: color-mix(in oklab, var(--insp-info) 25%, transparent); }
.subject-ORG_UNIT { background: var(--insp-pass-pale); color: var(--insp-pass); border-color: color-mix(in oklab, var(--insp-pass) 25%, transparent); }
.subject-PLACE { background: var(--insp-warn-pale); color: var(--insp-warn); border-color: color-mix(in oklab, var(--insp-warn) 25%, transparent); }

.empty-cell { text-align: center; padding: 80px 12px !important; }

.pagination {
  display: flex; align-items: center; justify-content: center; gap: var(--insp-sp-4);
  padding: var(--insp-sp-4) 0;
  border-top: 1px solid var(--insp-border-subtle);
}
.page-info {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-secondary);
  font-weight: 500;
}
</style>
