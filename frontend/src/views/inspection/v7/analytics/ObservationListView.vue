<template>
  <div class="obs-page">
    <!-- Header -->
    <header class="obs-header">
      <div class="header-left">
        <h1 class="page-title">评分观察记录</h1>
        <div class="stats-row">
          <span class="stat">总数 <b>{{ total }}</b></span>
          <i class="stat-sep" />
          <span class="stat"><em class="dot dot-neg" /> 负面 <b class="c-neg">{{ negativeCount }}</b></span>
        </div>
      </div>
    </header>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="filter-group">
        <select v-model="filters.subjectType" class="filter-select" @change="loadData">
          <option value="">全部主体</option>
          <option value="USER">用户</option>
          <option value="ORG_UNIT">组织</option>
          <option value="PLACE">场所</option>
        </select>
        <select v-model="filters.severity" class="filter-select" @change="loadData">
          <option value="">全部程度</option>
          <option value="LOW">轻微</option>
          <option value="MEDIUM">中等</option>
          <option value="HIGH">严重</option>
          <option value="CRITICAL">危急</option>
        </select>
        <label class="neg-toggle" :class="{ active: filters.isNegative }">
          <input type="checkbox" v-model="filters.isNegative" @change="loadData" />
          仅负面
        </label>
        <button v-if="hasFilters" class="btn-reset" @click="resetFilters">清除筛选</button>
      </div>
    </div>

    <!-- Table -->
    <div class="table-container" v-loading="loading">
      <table class="obs-table">
        <colgroup>
          <col style="width: 160px" />
          <col style="width: 120px" />
          <col style="width: 100px" />
          <col style="width: 80px" />
          <col style="width: 80px" />
          <col style="width: 100px" />
          <col style="width: 140px" />
        </colgroup>
        <thead>
          <tr>
            <th>检查项</th>
            <th>主体</th>
            <th>班级</th>
            <th>分数</th>
            <th>程度</th>
            <th>事件类型</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.id" class="obs-row">
            <td class="cell-name">{{ row.itemName || '--' }}</td>
            <td>
              <span class="subject-chip" :class="'st-' + row.subjectType">{{ row.subjectName || '--' }}</span>
            </td>
            <td class="cell-muted">{{ row.className || '--' }}</td>
            <td>
              <span class="score-val" :class="{ negative: row.score < 0 }">{{ row.score }}</span>
            </td>
            <td>
              <span v-if="row.severity" class="severity-badge" :style="severityStyle(row.severity)">
                {{ severityLabel(row.severity) }}
              </span>
              <span v-else>--</span>
            </td>
            <td class="cell-muted">{{ row.linkedEventTypeCode || '--' }}</td>
            <td class="cell-muted">{{ formatTime(row.observedAt) }}</td>
          </tr>
          <tr v-if="!loading && records.length === 0">
            <td colspan="7" class="empty-cell">暂无评分观察数据</td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="total > filters.size" class="pagination">
        <button class="page-btn" :disabled="filters.page <= 1" @click="filters.page--; loadData()">上一页</button>
        <span class="page-info">{{ filters.page }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="filters.page >= totalPages" @click="filters.page++; loadData()">下一页</button>
      </div>
    </div>
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
function severityStyle(s: string) {
  const cfg = SEVERITY_CONFIG[s]
  return cfg ? { color: cfg.color, background: cfg.bg } : {}
}

function formatTime(t: string) {
  if (!t) return '--'
  return t.replace('T', ' ').substring(0, 16)
}

onMounted(() => loadData())
</script>

<style scoped>
.obs-page {
  display: flex; flex-direction: column; height: 100%;
  background: #f8f9fb; font-family: 'DM Sans', sans-serif;
}

.obs-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 20px 24px 16px; background: #fff; border-bottom: 1px solid #e8eaed;
}
.page-title { font-family: 'Plus Jakarta Sans', sans-serif; font-size: 20px; font-weight: 700; color: #111827; margin: 0; }
.stats-row { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.stat { font-size: 12.5px; color: #6b7280; }
.stat b { font-weight: 600; }
.c-neg { color: #dc2626; }
.stat-sep { display: block; width: 1px; height: 10px; background: #d1d5db; }
.dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 2px; vertical-align: middle; }
.dot-neg { background: #ef4444; }

.filter-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 24px; background: #fff; border-bottom: 1px solid #e8eaed;
}
.filter-group { display: flex; gap: 8px; align-items: center; }
.filter-select {
  padding: 7px 28px 7px 10px; border: 1px solid #e5e7eb; border-radius: 7px;
  font-size: 13px; color: #374151; background: #f9fafb url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 10px center no-repeat;
  appearance: none; cursor: pointer; outline: none; font-family: inherit;
}
.neg-toggle {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 12px; border: 1px solid #e5e7eb; border-radius: 7px;
  font-size: 13px; color: #6b7280; cursor: pointer; user-select: none;
}
.neg-toggle input { display: none; }
.neg-toggle.active { border-color: #dc2626; color: #dc2626; background: #fef2f2; }
.btn-reset {
  padding: 7px 12px; font-size: 12px; color: #6b7280; background: none;
  border: 1px dashed #d1d5db; border-radius: 6px; cursor: pointer; font-family: inherit;
}

.table-container { flex: 1; overflow-y: auto; padding: 16px 24px; }
.obs-table {
  width: 100%; table-layout: fixed; border-collapse: separate; border-spacing: 0;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; overflow: hidden;
}
.obs-table th, .obs-table td {
  padding: 10px 12px; vertical-align: middle; text-align: left;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.obs-table th {
  font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em;
  color: #6b7280; background: #f9fafb; border-bottom: 1px solid #e5e7eb;
}
.obs-table td { font-size: 13px; color: #374151; border-bottom: 1px solid #f3f4f6; }
.obs-row:hover { background: #f0f5ff; }
.obs-row:last-child td { border-bottom: none; }

.cell-name { font-weight: 500; color: #111827; }
.cell-muted { font-size: 12px; color: #9ca3af; }

.subject-chip { font-size: 12px; font-weight: 500; padding: 2px 8px; border-radius: 4px; }
.st-USER { background: #eff6ff; color: #1d4ed8; }
.st-ORG_UNIT { background: #f0fdf4; color: #15803d; }
.st-PLACE { background: #fefce8; color: #a16207; }

.score-val { font-family: 'JetBrains Mono', monospace; font-size: 13px; font-weight: 600; }
.score-val.negative { color: #dc2626; }

.severity-badge {
  display: inline-block; padding: 2px 8px; border-radius: 4px;
  font-size: 11px; font-weight: 600;
}

.empty-cell { text-align: center; padding: 40px 12px !important; color: #9ca3af; }

.pagination {
  display: flex; align-items: center; justify-content: center; gap: 12px;
  padding: 12px 0; margin-top: 8px;
}
.page-btn {
  padding: 6px 14px; font-size: 13px; border: 1px solid #e5e7eb; border-radius: 6px;
  background: #fff; color: #374151; cursor: pointer; font-family: inherit;
}
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-btn:not(:disabled):hover { background: #f9fafb; }
.page-info { font-size: 13px; color: #6b7280; }
</style>
