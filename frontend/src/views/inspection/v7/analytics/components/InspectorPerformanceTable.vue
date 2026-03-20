<template>
  <div class="inspector-performance-table">
    <el-table
      :data="data"
      size="small"
      :max-height="maxHeight"
      :default-sort="{ prop: 'completedTasks', order: 'descending' }"
      stripe
    >
      <el-table-column label="检查员" prop="inspectorName" min-width="120" show-overflow-tooltip sortable />

      <el-table-column label="完成任务" prop="completedTasks" width="100" align="center" sortable>
        <template #default="{ row }">
          <span class="font-medium">{{ row.completedTasks }}</span>
        </template>
      </el-table-column>

      <el-table-column label="平均分" prop="avgScore" width="90" align="right" sortable>
        <template #default="{ row }">
          <span :class="scoreClass(row.avgScore)">{{ formatScore(row.avgScore) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="准时率" prop="onTimeRate" width="150" sortable>
        <template #default="{ row }">
          <div class="rate-cell">
            <el-progress
              :percentage="toPercent(row.onTimeRate)"
              :stroke-width="8"
              :color="rateColor(toPercent(row.onTimeRate))"
              :show-text="false"
              class="flex-1"
            />
            <span class="rate-text" :style="{ color: rateColor(toPercent(row.onTimeRate)) }">
              {{ formatPercent(row.onTimeRate) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="绩效等级" width="90" align="center">
        <template #default="{ row }">
          <el-tag
            :type="performanceTag(row).type"
            size="small"
            effect="plain"
          >
            {{ performanceTag(row).label }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!data.length" class="empty-hint">
      <el-empty description="暂无检查员数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
export interface InspectorPerformanceItem {
  inspectorName: string
  completedTasks: number
  avgScore: number
  onTimeRate: number
}

const props = withDefaults(defineProps<{
  data: InspectorPerformanceItem[]
  maxHeight?: number
}>(), {
  maxHeight: 400,
})

function formatScore(score: number | null | undefined): string {
  if (score == null) return '-'
  return score.toFixed(1)
}

function toPercent(rate: number | null | undefined): number {
  if (rate == null) return 0
  // If rate is between 0 and 1, convert to percentage; otherwise use as-is
  return rate <= 1 ? Math.round(rate * 100) : Math.round(rate)
}

function formatPercent(rate: number | null | undefined): string {
  if (rate == null) return '-'
  return `${toPercent(rate)}%`
}

function scoreClass(score: number | null | undefined): string {
  if (score == null) return 'text-gray-400'
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-average'
  return 'score-poor'
}

function rateColor(percent: number): string {
  if (percent >= 90) return '#67C23A'
  if (percent >= 70) return '#409EFF'
  if (percent >= 50) return '#E6A23C'
  return '#F56C6C'
}

function performanceTag(row: InspectorPerformanceItem): { label: string; type: string } {
  const score = row.avgScore ?? 0
  const rate = toPercent(row.onTimeRate)

  if (score >= 90 && rate >= 90) return { label: '优秀', type: 'success' }
  if (score >= 80 && rate >= 70) return { label: '良好', type: 'primary' }
  if (score >= 60 && rate >= 50) return { label: '合格', type: 'warning' }
  return { label: '待改进', type: 'danger' }
}
</script>

<style scoped>
.inspector-performance-table {
  width: 100%;
}

.rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rate-text {
  font-size: 12px;
  font-weight: 500;
  min-width: 38px;
  text-align: right;
}

.font-medium {
  font-weight: 500;
}

.score-excellent {
  color: #67C23A;
  font-weight: 500;
}

.score-good {
  color: #409EFF;
}

.score-average {
  color: #E6A23C;
}

.score-poor {
  color: #F56C6C;
  font-weight: 500;
}

.empty-hint {
  padding: 20px 0;
}
</style>
