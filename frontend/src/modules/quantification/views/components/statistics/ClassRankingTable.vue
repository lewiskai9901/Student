<template>
  <el-table
    :data="data"
    :loading="loading"
    stripe
    style="width: 100%"
    max-height="400"
  >
    <el-table-column type="index" label="排名" width="70" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.ranking <= 3" :type="getRankingType(row.ranking)" effect="dark" round>
          {{ row.ranking }}
        </el-tag>
        <span v-else>{{ row.ranking }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="className" label="班级" min-width="150" show-overflow-tooltip>
      <template #default="{ row }">
        <div class="class-info">
          <span class="class-name">{{ row.className }}</span>
          <span class="grade-name" v-if="row.gradeName">{{ row.gradeName }}</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column prop="teacherName" label="班主任" width="100" />
    <el-table-column prop="checkCount" label="检查次数" width="90" align="center" />
    <el-table-column prop="totalScore" label="总扣分" width="90" align="center" sortable>
      <template #default="{ row }">
        <span class="score-value" :class="getScoreClass(row.totalScore)">
          {{ row.totalScore.toFixed(1) }}
        </span>
      </template>
    </el-table-column>
    <el-table-column prop="avgScorePerCheck" label="平均扣分" width="90" align="center">
      <template #default="{ row }">
        {{ row.avgScorePerCheck.toFixed(1) }}
      </template>
    </el-table-column>
    <el-table-column prop="scoreLevel" label="等级" width="80" align="center">
      <template #default="{ row }">
        <el-tag :type="getLevelType(row.scoreLevel)" size="small">
          {{ row.scoreLevel }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="vsAvg" label="与均值差距" width="100" align="center">
      <template #default="{ row }">
        <span :class="row.vsAvg > 0 ? 'text-danger' : 'text-success'">
          {{ row.vsAvg > 0 ? '+' : '' }}{{ row.vsAvg.toFixed(1) }}
        </span>
      </template>
    </el-table-column>
    <el-table-column label="类别扣分分布" min-width="200">
      <template #default="{ row }">
        <div class="category-bar" v-if="row.categoryScores?.length">
          <div
            v-for="(cat, index) in row.categoryScores"
            :key="cat.categoryId"
            class="category-segment"
            :style="{
              width: cat.percentage + '%',
              backgroundColor: getCategoryColor(index)
            }"
            :title="`${cat.categoryName}: ${cat.score.toFixed(1)}分 (${cat.percentage.toFixed(1)}%)`"
          ></div>
        </div>
        <span v-else class="text-muted">-</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { ClassRanking } from '@/api/v2/quantification-extra'

defineProps<{
  data: ClassRanking[]
  loading?: boolean
}>()

const categoryColors = [
  '#5470c6',
  '#91cc75',
  '#fac858',
  '#ee6666',
  '#73c0de',
  '#3ba272',
  '#fc8452',
  '#9a60b4'
]

function getRankingType(ranking: number): 'warning' | 'success' | 'primary' {
  if (ranking === 1) return 'warning'
  if (ranking === 2) return 'success'
  return 'primary'
}

function getScoreClass(score: number): string {
  if (score <= 5) return 'score-good'
  if (score <= 15) return 'score-medium'
  return 'score-bad'
}

function getLevelType(level: string): 'success' | 'primary' | 'warning' | 'danger' | 'info' {
  switch (level) {
    case '优秀':
      return 'success'
    case '良好':
      return 'primary'
    case '中等':
      return 'warning'
    case '较差':
      return 'danger'
    default:
      return 'info'
  }
}

function getCategoryColor(index: number): string {
  return categoryColors[index % categoryColors.length]
}
</script>

<style scoped lang="scss">
.class-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-width: 100%;
  overflow: hidden;

  .class-name {
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .grade-name {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.score-value {
  font-weight: 600;

  &.score-good {
    color: #67c23a;
  }

  &.score-medium {
    color: #e6a23c;
  }

  &.score-bad {
    color: #f56c6c;
  }
}

.text-danger {
  color: #f56c6c;
}

.text-success {
  color: #67c23a;
}

.text-muted {
  color: #909399;
}

.category-bar {
  display: flex;
  height: 16px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;

  .category-segment {
    min-width: 4px;
    transition: all 0.3s;

    &:hover {
      opacity: 0.8;
      transform: scaleY(1.2);
    }
  }
}
</style>
