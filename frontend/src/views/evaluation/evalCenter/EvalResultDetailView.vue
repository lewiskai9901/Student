<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBatchResults, getBatch } from '@/api/evalCenter'
import type { EvalBatch, EvalResult, ConditionDetail } from '@/types/evalCenter'

const route = useRoute()
const router = useRouter()

const batchId = computed(() => Number(route.params.batchId))

// ==================== State ====================
const loading = ref(false)
const batch = ref<EvalBatch | null>(null)
const results = ref<EvalResult[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 50

// ==================== Load ====================
async function load() {
  loading.value = true
  try {
    const [batchData, resultsData] = await Promise.all([
      getBatch(batchId.value),
      getBatchResults(batchId.value, { page: page.value, size: pageSize }),
    ])
    batch.value = batchData
    results.value = parseResults(resultsData.records ?? [])
    total.value = resultsData.total ?? 0
  } catch (e: any) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

function parseResults(raw: EvalResult[]): EvalResult[] {
  return raw.map(r => ({
    ...r,
    conditionDetailsParsed: parseConditionDetails(r.conditionDetails),
  }))
}

function parseConditionDetails(json?: string | null): ConditionDetail[] {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}

function parseSummary(batch: EvalBatch | null): Record<string, number> {
  if (!batch?.summary) return {}
  try { return JSON.parse(batch.summary) } catch { return {} }
}

onMounted(load)

// ==================== Computed ====================
const summaryEntries = computed(() => {
  const s = parseSummary(batch.value)
  return Object.entries(s).sort((a, b) => Number(a[0].replace(/\D/g, '')) - Number(b[0].replace(/\D/g, '')))
})

const levelColors = ['#f59e0b', '#94a3b8', '#cd7f32', '#60a5fa', '#a78bfa']

// ==================== Helpers ====================
function goBack() {
  if (batch.value?.campaignId) {
    router.push(`/eval-center/campaigns/${batch.value.campaignId}`)
  } else {
    router.push('/eval-center')
  }
}

function getRankMedal(rankNo: number | null | undefined): string {
  if (rankNo === 1) return '🥇'
  if (rankNo === 2) return '🥈'
  if (rankNo === 3) return '🥉'
  return ''
}

function getLevelColor(levelNum: number | null | undefined): string {
  if (!levelNum) return '#94a3b8'
  return levelColors[(levelNum - 1) % levelColors.length]
}

function formatDate(t?: string | null) {
  if (!t) return '-'
  return new Date(t).toLocaleDateString('zh-CN')
}

function handleExport() {
  // Placeholder - in real app call export API
  alert('导出功能需要后端接口支持')
}

function handlePublishHonor() {
  // Placeholder
  alert('发布荣誉事件功能需要后端接口支持')
}

function parseActualValue(actual: string): string {
  if (!actual) return '-'
  try {
    const v = JSON.parse(actual)
    if (Array.isArray(v)) return v.join('、')
    return String(v)
  } catch { return actual }
}
</script>

<template>
  <div class="erd">
    <!-- Header -->
    <div class="erd-header">
      <div class="erd-header-left">
        <button class="back-btn" @click="goBack">← 返回</button>
        <div class="erd-title-group">
          <h1 class="erd-title">评选结果</h1>
          <span v-if="batch" class="erd-subtitle">
            {{ batch.cycleStart }} ~ {{ batch.cycleEnd }}
          </span>
        </div>
      </div>
      <div class="erd-header-right">
        <button class="btn-ghost" @click="handleExport">导出 Excel</button>
        <button class="btn-primary" @click="handlePublishHonor">发布荣誉事件</button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="state-loading">
      <div class="spinner" />
      <span>加载中...</span>
    </div>

    <template v-else>
      <!-- Summary stats -->
      <div v-if="batch" class="erd-summary">
        <div class="summary-info">
          <span class="summary-label">共 {{ batch.totalTargets }} 个目标</span>
          <span class="summary-sep">·</span>
          <span class="summary-label">执行于 {{ formatDate(batch.executedAt) }}</span>
        </div>
        <div class="summary-dist" v-if="summaryEntries.length > 0">
          <span
            v-for="([key, count], idx) in summaryEntries"
            :key="key"
            class="dist-item"
          >
            <span
              class="dist-dot"
              :style="{ background: levelColors[idx % levelColors.length] }"
            />
            {{ key }}: {{ count }}
          </span>
        </div>
      </div>

      <!-- Result list -->
      <div class="erd-content">
        <div v-if="results.length === 0" class="state-empty">
          <div class="empty-icon">📋</div>
          <p class="empty-title">暂无结果数据</p>
          <p class="empty-sub">执行评选后才会产生结果</p>
        </div>

        <div v-else class="result-list">
          <div
            v-for="result in results"
            :key="result.id"
            class="result-card"
            :class="{ 'has-level': result.levelNum != null }"
          >
            <!-- Card header -->
            <div class="result-card-head">
              <!-- Rank -->
              <div class="result-rank">
                <span v-if="result.rankNo && result.rankNo <= 3" class="rank-medal">
                  {{ getRankMedal(result.rankNo) }}
                </span>
                <span v-else-if="result.rankNo" class="rank-num">#{{ result.rankNo }}</span>
                <span v-else class="rank-num">-</span>
              </div>

              <!-- Target info -->
              <div class="result-target">
                <span class="target-name">{{ result.targetName || `目标 #${result.targetId}` }}</span>
                <span class="target-type">{{ result.targetType }}</span>
              </div>

              <!-- Level badge -->
              <div
                class="result-level"
                :style="{
                  background: result.levelNum ? getLevelColor(result.levelNum) + '20' : '#f4f6f9',
                  color: result.levelNum ? getLevelColor(result.levelNum) : '#94a3b8',
                  borderColor: result.levelNum ? getLevelColor(result.levelNum) + '40' : '#e8ecf0',
                }"
              >
                {{ result.levelName ?? '未达标' }}
              </div>

              <!-- Score -->
              <div v-if="result.score != null" class="result-score">
                {{ result.score.toFixed(1) }} 分
              </div>
            </div>

            <!-- Condition breakdown -->
            <div
              v-if="result.conditionDetailsParsed && result.conditionDetailsParsed.length > 0"
              class="result-conditions"
            >
              <div class="conditions-title">条件达标明细</div>
              <div class="conditions-grid">
                <div
                  v-for="(detail, di) in result.conditionDetailsParsed"
                  :key="di"
                  class="cond-detail"
                  :class="{ passed: detail.passed, failed: !detail.passed }"
                >
                  <span class="cond-icon">{{ detail.passed ? '✓' : '✗' }}</span>
                  <span class="cond-text">{{ detail.description }}</span>
                  <span class="cond-actual">
                    实际: {{ parseActualValue(detail.actual) }}
                    <span class="cond-vs">vs</span>
                    阈值: {{ detail.threshold }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Upgrade hint -->
            <div v-if="result.upgradeHint" class="upgrade-hint">
              <span class="hint-icon">💡</span>
              <span class="hint-text">{{ result.upgradeHint }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* ==================== Root ==================== */
.erd {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== Header ==================== */
.erd-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  gap: 16px;
}
.erd-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  white-space: nowrap;
  font-family: inherit;
  flex-shrink: 0;
}
.back-btn:hover { background: #f4f6f9; }
.erd-title-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.erd-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e2a3a;
  margin: 0;
}
.erd-subtitle {
  font-size: 12px;
  color: #8c95a3;
}
.erd-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ==================== Summary ==================== */
.erd-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 12px;
}
.summary-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.summary-label {
  font-size: 13px;
  color: #5a6474;
}
.summary-sep { color: #dce1e8; }
.summary-dist {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.dist-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #1e2a3a;
}
.dist-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ==================== Content ==================== */
.erd-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ==================== Result list ==================== */
.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-card {
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 10px;
  overflow: hidden;
  transition: box-shadow 0.18s;
}
.result-card:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }
.result-card.has-level { border-left: 3px solid #1a6dff; }

/* Card head */
.result-card-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
}
.result-rank {
  width: 48px;
  text-align: center;
  flex-shrink: 0;
}
.rank-medal { font-size: 28px; }
.rank-num {
  font-size: 15px;
  font-weight: 700;
  color: #8c95a3;
}
.result-target {
  flex: 1;
  min-width: 0;
}
.target-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e2a3a;
  display: block;
}
.target-type {
  font-size: 11px;
  color: #b8c0cc;
}
.result-level {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid transparent;
  white-space: nowrap;
  flex-shrink: 0;
}
.result-score {
  font-size: 14px;
  font-weight: 700;
  color: #1e2a3a;
  white-space: nowrap;
  flex-shrink: 0;
}

/* Condition breakdown */
.result-conditions {
  padding: 12px 20px;
  border-top: 1px solid #f0f2f5;
  background: #fafbfc;
}
.conditions-title {
  font-size: 11px;
  font-weight: 600;
  color: #8c95a3;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}
.conditions-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.cond-detail {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
}
.cond-detail.passed { background: #ecfdf5; }
.cond-detail.failed { background: #fef2f2; }
.cond-icon {
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}
.cond-detail.passed .cond-icon { color: #10b981; }
.cond-detail.failed .cond-icon { color: #ef4444; }
.cond-text {
  flex: 1;
  color: #3d4757;
  line-height: 1.5;
}
.cond-actual {
  font-size: 11px;
  color: #8c95a3;
  white-space: nowrap;
  flex-shrink: 0;
}
.cond-vs {
  margin: 0 4px;
  color: #c8d0db;
}

/* Upgrade hint */
.upgrade-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 20px;
  background: #fffbeb;
  border-top: 1px solid #fde68a;
}
.hint-icon { font-size: 14px; flex-shrink: 0; }
.hint-text { font-size: 12px; color: #92400e; line-height: 1.6; }

/* ==================== State views ==================== */
.state-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 80px 0;
  color: #b8c0cc;
  font-size: 13px;
  flex: 1;
}
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e8ecf0;
  border-top-color: #1a6dff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.state-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 10px;
}
.empty-icon { font-size: 48px; opacity: 0.3; }
.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #6b7685;
  margin: 0;
}
.empty-sub { font-size: 12px; color: #b8c0cc; margin: 0; }

/* ==================== Buttons ==================== */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  background: #1a6dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.btn-primary:hover { background: #1558d6; }
.btn-ghost {
  padding: 8px 16px;
  background: none;
  border: 1px solid #dce1e8;
  border-radius: 8px;
  font-size: 13px;
  color: #5a6474;
  cursor: pointer;
  font-family: inherit;
}
.btn-ghost:hover { background: #f4f6f9; }
</style>
