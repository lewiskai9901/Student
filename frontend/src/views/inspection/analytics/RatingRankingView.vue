<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, Medal, RefreshCw } from 'lucide-vue-next'
import { getProjects, listDimensions, getRankings } from '@/api/inspection/project'
import type { InspProject, RatingDimension, RatingResult } from '@/types/insp/project'

// ========== State ==========
const projects = ref<InspProject[]>([])
const selectedProjectId = ref<number | null>(null)
const selectedDate = ref('')
const dimensions = ref<RatingDimension[]>([])
const resultsByDimension = ref<Map<number, RatingResult[]>>(new Map())
const loadingProjects = ref(false)
const loadingDimensions = ref(false)
const loadingResults = ref<Set<number>>(new Set())

// ========== Load projects ==========
async function loadProjects() {
  loadingProjects.value = true
  try {
    projects.value = await getProjects()
    if (projects.value.length > 0 && !selectedProjectId.value) {
      selectedProjectId.value = projects.value[0].id
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载项目列表失败')
  } finally {
    loadingProjects.value = false
  }
}

// ========== Load dimensions for selected project ==========
async function loadDimensions() {
  if (!selectedProjectId.value) {
    dimensions.value = []
    resultsByDimension.value = new Map()
    return
  }
  loadingDimensions.value = true
  try {
    dimensions.value = await listDimensions(selectedProjectId.value)
    await loadAllResults()
  } catch (e: any) {
    ElMessage.error(e.message || '加载评级维度失败')
  } finally {
    loadingDimensions.value = false
  }
}

// ========== Load results for all dimensions ==========
async function loadAllResults() {
  const newMap = new Map<number, RatingResult[]>()
  const promises = dimensions.value.map(async (dim) => {
    loadingResults.value.add(dim.id)
    try {
      const results = await getRankings(dim.id, { cycleDate: selectedDate.value || undefined })
      newMap.set(dim.id, results)
    } catch {
      newMap.set(dim.id, [])
    } finally {
      loadingResults.value.delete(dim.id)
    }
  })
  await Promise.all(promises)
  resultsByDimension.value = newMap
}

// ========== Helpers ==========
function getRankColor(rank: number | null): string {
  if (rank === 1) return '#f59e0b'
  if (rank === 2) return '#94a3b8'
  if (rank === 3) return '#b45309'
  return '#6b7280'
}

function getRankIcon(rank: number | null): typeof Trophy | typeof Medal | null {
  if (rank === 1) return Trophy
  if (rank != null && rank <= 3) return Medal
  return null
}

function getGradeColor(grade: string | null): string {
  if (!grade) return '#6b7280'
  const map: Record<string, string> = {
    'A+': '#10b981', 'A': '#22c55e', 'B': '#3b82f6', 'C': '#f59e0b', 'D': '#ef4444', 'F': '#dc2626',
  }
  return map[grade] || '#6b7280'
}

function isDimensionLoading(dimId: number): boolean {
  return loadingResults.value.has(dimId)
}

function getDimensionResults(dimId: number): RatingResult[] {
  return resultsByDimension.value.get(dimId) || []
}

function handleRefresh() {
  loadDimensions()
}

// ========== Watchers ==========
watch(selectedProjectId, () => {
  loadDimensions()
})

watch(selectedDate, () => {
  if (dimensions.value.length > 0) {
    loadAllResults()
  }
})

// ========== Init ==========
onMounted(() => {
  loadProjects()
})
</script>

<template>
  <div class="insp-shell ranking-page">
    <!-- ── Editorial header ─────────── -->
    <header class="page-head">
      <div>
        <div class="insp-eyebrow">评级榜单 / Ratings Leaderboard</div>
        <h1 class="insp-display page-title">评级排名</h1>
      </div>
      <div class="filter-bar">
        <div class="filter-group">
          <span class="insp-caps">项目</span>
          <el-select
            v-model="selectedProjectId" placeholder="选择项目"
            class="w-52" :loading="loadingProjects" filterable size="small"
          >
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </div>
        <div class="filter-group">
          <span class="insp-caps">日期</span>
          <el-date-picker
            v-model="selectedDate" type="date" value-format="YYYY-MM-DD"
            placeholder="留空显示最新" clearable class="!w-44" size="small"
          />
        </div>
        <button class="insp-btn insp-btn--ghost" @click="handleRefresh">刷新</button>
      </div>
    </header>

    <hr class="insp-rule insp-rule--strong head-divider" />

    <!-- Empty / loading states -->
    <div v-if="loadingDimensions && dimensions.length === 0" class="state-line">
      <div class="insp-stamp">加载中</div>
    </div>
    <div v-else-if="!selectedProjectId" class="state-line">
      <div class="insp-stamp">请选择项目</div>
    </div>
    <div v-else-if="dimensions.length === 0 && !loadingDimensions" class="state-line">
      <div class="insp-stamp">无评级维度</div>
      <p class="state-hint">请先在项目详情中配置评级维度</p>
    </div>

    <!-- Dimensions ledger -->
    <div v-else class="dim-stack">
      <article v-for="dim in dimensions" :key="dim.id" class="dim-card">
        <header class="dim-card__head">
          <div class="dim-card__lead">
            <div class="insp-eyebrow">维度</div>
            <h2 class="dim-card__name">{{ dim.dimensionName }}</h2>
          </div>
          <div v-if="dim.awardName" class="dim-card__award">
            <span class="insp-caps">奖项</span>
            <span class="award-name">{{ dim.awardName }}</span>
          </div>
        </header>

        <div v-loading="isDimensionLoading(dim.id)" class="dim-body">
          <div v-if="getDimensionResults(dim.id).length === 0 && !isDimensionLoading(dim.id)" class="dim-empty">
            <span class="insp-stamp">无数据</span>
          </div>

          <ol v-else class="rank-list">
            <li v-for="result in getDimensionResults(dim.id)" :key="result.id"
                class="rank-row" :class="{
                  'is-rank1': result.rankNo === 1,
                  'is-rank2': result.rankNo === 2,
                  'is-rank3': result.rankNo === 3,
                }">
              <span class="rank-num insp-num">{{ result.rankNo ? String(result.rankNo).padStart(2, '0') : '—' }}</span>
              <span class="rank-target">{{ result.targetName }}</span>
              <span class="rank-score insp-num">
                {{ result.score != null ? result.score.toFixed(1) : '—' }}
              </span>
              <span v-if="result.grade" class="rank-grade" :style="{
                background: getGradeColor(result.grade),
              }">{{ result.grade }}</span>
              <span v-else class="rank-grade rank-grade--empty">—</span>
            </li>
          </ol>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.ranking-page {
  padding: 32px 48px 64px;
  max-width: 1200px;
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
.head-divider { margin: 0 0 var(--insp-sp-7); }

.filter-bar {
  display: flex; align-items: center; gap: var(--insp-sp-4); flex-wrap: wrap;
}
.filter-group { display: flex; align-items: center; gap: var(--insp-sp-2); }
.filter-group .insp-caps { color: var(--insp-ink-tertiary); }

/* ─ States ─────── */
.state-line {
  text-align: center; padding: 80px 0;
}
.state-hint {
  margin-top: var(--insp-sp-4);
  color: var(--insp-ink-tertiary);
  font-size: var(--insp-text-sm);
}

/* ─ Dimension cards ─────── */
.dim-stack {
  display: flex; flex-direction: column;
  gap: var(--insp-sp-6);
}

.dim-card {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-subtle);
  border-radius: var(--insp-radius-md);
  overflow: hidden;
}

.dim-card__head {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--insp-sp-4) var(--insp-sp-5);
  border-bottom: 1px solid var(--insp-border-subtle);
  background: var(--insp-bg-subtle);
}
.dim-card__lead { display: flex; flex-direction: column; gap: 2px; }
.dim-card__name {
  font-family: var(--insp-font-display);
  font-size: 22px; font-weight: 500;
  letter-spacing: var(--insp-tracking-display);
  margin: 0;
  color: var(--insp-ink-primary);
}
.dim-card__award {
  display: inline-flex; align-items: center; gap: var(--insp-sp-2);
  padding: 4px 10px;
  border: 1px solid var(--insp-accent);
  background: var(--insp-bg-surface);
}
.award-name {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-sm); font-weight: 600;
  color: var(--insp-accent);
}

.dim-body { min-height: 80px; }

.dim-empty {
  padding: var(--insp-sp-8); text-align: center;
}

/* ─ Rank rows ─────── */
.rank-list {
  list-style: none; margin: 0; padding: 0;
}
.rank-row {
  display: grid;
  grid-template-columns: 56px 1fr 100px 56px;
  gap: var(--insp-sp-4);
  align-items: center;
  padding: var(--insp-sp-3) var(--insp-sp-5);
  border-bottom: 1px solid var(--insp-border-subtle);
  transition: background var(--insp-t-fast);
}
.rank-row:last-child { border-bottom: 0; }
.rank-row:hover { background: var(--insp-bg-subtle); }

.rank-row.is-rank1 {
  background: linear-gradient(to right, var(--insp-accent-paler), transparent 50%);
}
.rank-row.is-rank1 .rank-num { color: var(--insp-accent); font-size: 26px; font-weight: 700; }
.rank-row.is-rank2 .rank-num { color: var(--insp-ink-secondary); font-size: 22px; font-weight: 600; }
.rank-row.is-rank3 .rank-num { color: var(--insp-warn); font-size: 22px; font-weight: 600; }

.rank-num {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-lg); font-weight: 500;
  color: var(--insp-ink-tertiary);
  letter-spacing: -0.02em;
}

.rank-target {
  font-size: var(--insp-text-md);
  font-weight: 500;
  color: var(--insp-ink-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-score {
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-lg); font-weight: 600;
  color: var(--insp-ink-primary);
  text-align: right;
  letter-spacing: -0.02em;
}

.rank-grade {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 24px;
  font-family: var(--insp-font-mono);
  font-size: var(--insp-text-xs); font-weight: 700;
  color: white;
  border-radius: var(--insp-radius-sm);
  letter-spacing: 0;
  margin: 0 auto;
}
.rank-grade--empty {
  background: transparent !important;
  color: var(--insp-ink-quaternary);
}

@media (max-width: 800px) {
  .ranking-page { padding: 20px 16px 64px; }
  .page-title { font-size: 32px; }
  .rank-row { grid-template-columns: 40px 1fr 80px 36px; gap: var(--insp-sp-2); padding: var(--insp-sp-3); }
}
</style>
