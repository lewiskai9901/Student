<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, RefreshCw, Zap } from 'lucide-vue-next'
import { getProjects, listDimensions, getRankings, calculateRatings } from '@/api/insp/project'
import type { InspProject, RatingDimension, RatingResult } from '@/types/insp/project'

// ========== State ==========
const projects = ref<InspProject[]>([])
const selectedProjectId = ref<number | null>(null)
const selectedDimensionId = ref<number | null>(null)
const cycleDate = ref('')
const dimensions = ref<RatingDimension[]>([])
const results = ref<RatingResult[]>([])
const loadingProjects = ref(false)
const loadingDimensions = ref(false)
const loadingResults = ref(false)
const computing = ref(false)

// ========== Computed ==========
const selectedProject = computed(() =>
  projects.value.find((p) => p.id === selectedProjectId.value) ?? null
)

const selectedDimension = computed(() =>
  dimensions.value.find((d) => d.id === selectedDimensionId.value) ?? null
)

const maxScore = computed(() => {
  if (results.value.length === 0) return 100
  return Math.max(...results.value.map((r) => r.score ?? 0), 100)
})

// Parse grade bands to get passing threshold
const passingScore = computed(() => {
  if (!selectedDimension.value?.gradeBands) return 60
  try {
    const bands = JSON.parse(selectedDimension.value.gradeBands) as Array<{ grade: string; min: number }>
    // Find the minimum score of any passing grade (not F)
    const nonF = bands.filter((b) => b.grade !== 'F').map((b) => b.min)
    return nonF.length > 0 ? Math.min(...nonF) : 60
  } catch {
    return 60
  }
})

const passingResults = computed(() => results.value.filter((r) => (r.score ?? 0) >= passingScore.value))
const failingResults = computed(() => results.value.filter((r) => (r.score ?? 0) < passingScore.value))

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

// ========== Load dimensions ==========
async function loadDimensions() {
  if (!selectedProjectId.value) {
    dimensions.value = []
    selectedDimensionId.value = null
    results.value = []
    return
  }
  loadingDimensions.value = true
  try {
    dimensions.value = await listDimensions(selectedProjectId.value)
    if (dimensions.value.length > 0) {
      selectedDimensionId.value = dimensions.value[0].id
    } else {
      selectedDimensionId.value = null
      results.value = []
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载评级维度失败')
  } finally {
    loadingDimensions.value = false
  }
}

// ========== Load results ==========
async function loadResults() {
  if (!selectedDimensionId.value) {
    results.value = []
    return
  }
  loadingResults.value = true
  try {
    results.value = await getRankings(selectedDimensionId.value, { cycleDate: cycleDate.value || undefined })
  } catch (e: any) {
    ElMessage.error(e.message || '加载排名数据失败')
  } finally {
    loadingResults.value = false
  }
}

// ========== Trigger compute ==========
async function triggerCompute() {
  if (!selectedDimensionId.value) return
  computing.value = true
  try {
    const today = new Date().toISOString().substring(0, 10)
    await calculateRatings(selectedDimensionId.value, { cycleDate: cycleDate.value || today })
    ElMessage.success('评级计算已触发，正在刷新...')
    await loadResults()
  } catch (e: any) {
    ElMessage.error(e.message || '触发计算失败')
  } finally {
    computing.value = false
  }
}

// ========== Helpers ==========
function getRankEmoji(rank: number | null): string {
  if (rank === 1) return '🥇'
  if (rank === 2) return '🥈'
  if (rank === 3) return '🥉'
  return ''
}

function getScoreBarWidth(score: number | null): number {
  if (score == null || maxScore.value === 0) return 0
  return Math.min(100, (score / maxScore.value) * 100)
}

function getGradeColor(grade: string | null): string {
  if (!grade) return '#6b7280'
  const map: Record<string, string> = {
    'A+': '#10b981',
    'A': '#10b981',
    'B': '#1a6dff',
    'C': '#f59e0b',
    'D': '#f97316',
    'F': '#ef4444',
  }
  return map[grade] || '#6b7280'
}

function getScoreBarColor(score: number | null): string {
  if (score == null) return '#d1d5db'
  if (score >= 90) return '#10b981'
  if (score >= 80) return '#22c55e'
  if (score >= 70) return '#84cc16'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

function getRankTextColor(rank: number | null): string {
  if (rank === 1) return '#d97706'
  if (rank === 2) return '#94a3b8'
  if (rank === 3) return '#b45309'
  return '#6b7280'
}

// ========== Watchers ==========
watch(selectedProjectId, () => {
  loadDimensions()
})

watch(selectedDimensionId, () => {
  loadResults()
})

watch(cycleDate, () => {
  loadResults()
})

// ========== Init ==========
onMounted(() => {
  loadProjects()
})
</script>

<template>
  <div class="leaderboard-page">
    <!-- Page header -->
    <div class="page-header">
      <Trophy class="header-icon" />
      <h2 class="page-title">评级排名</h2>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-group">
        <label class="filter-label">项目</label>
        <el-select
          v-model="selectedProjectId"
          placeholder="选择项目"
          :loading="loadingProjects"
          filterable
          class="filter-select"
        >
          <el-option
            v-for="p in projects"
            :key="p.id"
            :label="p.projectName"
            :value="p.id"
          />
        </el-select>
      </div>

      <div class="filter-group">
        <label class="filter-label">维度</label>
        <el-select
          v-model="selectedDimensionId"
          placeholder="选择维度"
          :loading="loadingDimensions"
          :disabled="dimensions.length === 0"
          class="filter-select"
        >
          <el-option
            v-for="d in dimensions"
            :key="d.id"
            :label="d.dimensionName"
            :value="d.id"
          />
        </el-select>
      </div>

      <div class="filter-group">
        <label class="filter-label">周期</label>
        <el-date-picker
          v-model="cycleDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="本月"
          clearable
          class="filter-date"
        />
      </div>
    </div>

    <!-- Content area -->
    <div class="content-area">
      <!-- Empty: no project -->
      <div v-if="!selectedProjectId" class="empty-state">
        <Trophy class="empty-icon-svg" />
        <div class="empty-text">请选择一个检查项目</div>
      </div>

      <!-- Empty: no dimensions -->
      <div v-else-if="!loadingDimensions && dimensions.length === 0" class="empty-state">
        <Trophy class="empty-icon-svg" />
        <div class="empty-text">该项目暂无评级维度</div>
        <div class="empty-hint">请先在项目详情中配置评级维度</div>
      </div>

      <!-- Ranking list -->
      <div v-else class="ranking-card" v-loading="loadingResults || loadingDimensions">
        <!-- Dimension header -->
        <div v-if="selectedDimension" class="dim-header">
          <div class="dim-name">{{ selectedDimension.dimensionName }}</div>
          <div v-if="selectedDimension.awardName" class="dim-award">
            🏆 {{ selectedDimension.awardName }}
          </div>
        </div>

        <!-- Empty results -->
        <div v-if="results.length === 0 && !loadingResults" class="empty-state">
          <div class="empty-text">暂无排名数据</div>
          <div class="empty-hint">请触发评级计算后刷新</div>
        </div>

        <!-- Passing results -->
        <template v-else>
          <div
            v-for="result in passingResults"
            :key="result.id"
            class="rank-row"
            :class="{ 'rank-top3': result.rankNo != null && result.rankNo <= 3 }"
          >
            <!-- Rank badge -->
            <div class="rank-badge">
              <span v-if="result.rankNo != null && result.rankNo <= 3" class="rank-emoji">
                {{ getRankEmoji(result.rankNo) }}
              </span>
              <span v-else class="rank-num" :style="{ color: getRankTextColor(result.rankNo) }">
                {{ result.rankNo ?? '-' }}
              </span>
            </div>

            <!-- Name -->
            <div class="rank-name">{{ result.targetName }}</div>

            <!-- Score -->
            <div class="rank-score" :style="{ color: getScoreBarColor(result.score) }">
              {{ result.score != null ? result.score.toFixed(1) : '-' }}
            </div>

            <!-- Grade badge -->
            <div class="rank-grade">
              <span
                v-if="result.grade"
                class="grade-badge"
                :style="{ background: getGradeColor(result.grade) }"
              >{{ result.grade }}</span>
              <span v-else class="grade-empty">-</span>
            </div>

            <!-- Score bar -->
            <div class="rank-bar-wrap">
              <div
                class="rank-bar-fill"
                :style="{
                  width: getScoreBarWidth(result.score) + '%',
                  background: getScoreBarColor(result.score),
                }"
              ></div>
            </div>
          </div>

          <!-- Passing line -->
          <div v-if="failingResults.length > 0" class="passing-line">
            <span class="passing-label">及格线 {{ passingScore }}分</span>
          </div>

          <!-- Failing results -->
          <div
            v-for="result in failingResults"
            :key="result.id"
            class="rank-row rank-row--fail"
          >
            <div class="rank-badge">
              <span class="rank-num" :style="{ color: '#ef4444' }">
                {{ result.rankNo ?? '-' }}
              </span>
            </div>
            <div class="rank-name">{{ result.targetName }}</div>
            <div class="rank-score" style="color: #ef4444;">
              {{ result.score != null ? result.score.toFixed(1) : '-' }}
            </div>
            <div class="rank-grade">
              <span
                v-if="result.grade"
                class="grade-badge"
                :style="{ background: getGradeColor(result.grade) }"
              >{{ result.grade }}</span>
              <span v-else class="grade-empty">-</span>
            </div>
            <div class="rank-bar-wrap">
              <div
                class="rank-bar-fill rank-bar-fill--fail"
                :style="{
                  width: getScoreBarWidth(result.score) + '%',
                }"
              ></div>
            </div>
          </div>
        </template>
      </div>

      <!-- Action row -->
      <div v-if="selectedDimensionId" class="action-row">
        <button
          class="compute-btn"
          @click="triggerCompute"
          :disabled="computing || loadingResults"
        >
          <Zap class="compute-icon" />
          {{ computing ? '计算中...' : '手动触发评级计算' }}
        </button>
        <button
          class="refresh-btn"
          @click="loadResults"
          :disabled="loadingResults"
        >
          <RefreshCw class="refresh-icon" :class="{ spinning: loadingResults }" />
          刷新
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.leaderboard-page {
  padding: 20px 24px;
  max-width: 900px;
  margin: 0 auto;
}

/* Header */
.page-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.header-icon {
  width: 20px;
  height: 20px;
  color: #d97706;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

/* Filter bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

.filter-select {
  width: 180px;
}

.filter-date {
  width: 160px;
}

/* Content area */
.content-area {
  min-height: 200px;
}

/* Empty state */
.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #9ca3af;
}

.empty-icon-svg {
  width: 40px;
  height: 40px;
  color: #d1d5db;
  margin: 0 auto 12px;
}

.empty-text {
  font-size: 13px;
  color: #6b7280;
}

.empty-hint {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}

/* Ranking card */
.ranking-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}

/* Dimension header */
.dim-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(to right, #fffbeb, #fef3c7);
  border-bottom: 1px solid #fde68a;
}

.dim-name {
  font-size: 13px;
  font-weight: 600;
  color: #92400e;
}

.dim-award {
  font-size: 12px;
  color: #d97706;
}

/* Rank rows */
.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.1s;
}

.rank-row:last-child {
  border-bottom: none;
}

.rank-row:hover {
  background: #fafafa;
}

.rank-row.rank-top3 {
  background: #fffbeb;
}

.rank-row.rank-top3:hover {
  background: #fef9ee;
}

.rank-row--fail {
  opacity: 0.75;
}

/* Rank badge */
.rank-badge {
  width: 32px;
  text-align: center;
  flex-shrink: 0;
}

.rank-emoji {
  font-size: 18px;
  line-height: 1;
}

.rank-num {
  font-size: 13px;
  font-weight: 600;
}

/* Name */
.rank-name {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Score */
.rank-score {
  font-size: 14px;
  font-weight: 700;
  width: 52px;
  text-align: right;
  flex-shrink: 0;
}

/* Grade badge */
.rank-grade {
  width: 32px;
  text-align: center;
  flex-shrink: 0;
}

.grade-badge {
  display: inline-block;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  padding: 1px 5px;
  border-radius: 4px;
}

.grade-empty {
  color: #d1d5db;
  font-size: 12px;
}

/* Score bar */
.rank-bar-wrap {
  flex: 1;
  max-width: 200px;
  height: 6px;
  background: #f3f4f6;
  border-radius: 3px;
  overflow: hidden;
  flex-shrink: 0;
}

.rank-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}

.rank-bar-fill--fail {
  background: #ef4444 !important;
}

/* Passing line */
.passing-line {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 16px;
  background: #fef2f2;
}

.passing-line::before,
.passing-line::after {
  content: '';
  flex: 1;
  height: 1px;
  background: repeating-linear-gradient(
    to right,
    #fca5a5 0,
    #fca5a5 4px,
    transparent 4px,
    transparent 8px
  );
}

.passing-label {
  font-size: 10px;
  color: #ef4444;
  font-weight: 600;
  white-space: nowrap;
}

/* Action row */
.action-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.compute-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #1a6dff;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  padding: 7px 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.compute-btn:hover:not(:disabled) {
  background: #1558d6;
}

.compute-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.compute-icon {
  width: 13px;
  height: 13px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: 1px solid #e5e7eb;
  color: #6b7280;
  border-radius: 6px;
  font-size: 12px;
  padding: 7px 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.refresh-btn:hover:not(:disabled) {
  border-color: #1a6dff;
  color: #1a6dff;
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-icon {
  width: 13px;
  height: 13px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spinning {
  animation: spin 1s linear infinite;
}
</style>
