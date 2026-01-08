<template>
  <div class="round-card">
    <div class="round-card__header">
      <span class="round-card__title">{{ title }}</span>
      <el-button v-if="showMore" link type="primary" @click="$emit('more')">
        详情 <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <div class="round-card__body" v-loading="loading">
      <div v-if="!loading && maxRounds <= 1" class="round-card__single">
        <el-empty description="本计划检查均为单轮检查" :image-size="60" />
      </div>

      <template v-else-if="!loading">
        <div class="round-card__summary">
          <div class="summary-item">
            <span class="summary-item__label">最大轮次</span>
            <span class="summary-item__value">{{ maxRounds }}轮</span>
          </div>
          <div class="summary-item">
            <span class="summary-item__label">整体改善率</span>
            <span class="summary-item__value" :class="improvementClass">
              {{ overallImprovementRate > 0 ? '+' : '' }}{{ overallImprovementRate.toFixed(1) }}%
            </span>
          </div>
        </div>

        <div class="round-card__comparison">
          <div
            v-for="item in roundComparison"
            :key="item.round"
            class="round-item"
          >
            <div class="round-item__label">
              <span class="round-item__number">第{{ item.round }}轮</span>
              <span class="round-item__count">{{ item.classCount }}班</span>
            </div>
            <div class="round-item__bar-container">
              <div
                class="round-item__bar"
                :style="{ width: getBarWidth(item.avgScore) + '%' }"
                :class="{ 'improved': item.round > 1 && item.vsPreRound < 0 }"
              ></div>
            </div>
            <div class="round-item__score">
              <span class="round-item__avg">{{ item.avgScore.toFixed(1) }}</span>
              <span v-if="item.round > 1" class="round-item__change" :class="item.vsPreRound < 0 ? 'better' : 'worse'">
                {{ item.vsPreRound < 0 ? '' : '+' }}{{ item.vsPreRound.toFixed(1) }}
              </span>
            </div>
          </div>
        </div>

        <div class="round-card__stats">
          <div class="stat-item stat-item--success">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>{{ improvedClasses }}个班级改善</span>
          </div>
          <div class="stat-item stat-item--danger">
            <el-icon><CircleCloseFilled /></el-icon>
            <span>{{ worsenedClasses }}个班级变差</span>
          </div>
          <div class="stat-item stat-item--info">
            <el-icon><RemoveFilled /></el-icon>
            <span>{{ stableClasses }}个班级持平</span>
          </div>
        </div>
      </template>
    </div>

    <div class="round-card__footer" v-if="insights.length > 0">
      <div v-for="(insight, index) in insights" :key="index" class="round-card__insight">
        <el-icon><InfoFilled /></el-icon>
        {{ insight }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowRight, CircleCheckFilled, CircleCloseFilled, RemoveFilled, InfoFilled } from '@element-plus/icons-vue'
import type { RoundComparisonVO } from '@/api/smartStatistics'

interface Props {
  title?: string
  maxRounds: number
  roundComparison: RoundComparisonVO[]
  overallImprovementRate: number
  improvedClasses: number
  worsenedClasses: number
  stableClasses: number
  insights?: string[]
  loading?: boolean
  showMore?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '轮次分析',
  insights: () => [],
  loading: false,
  showMore: true
})

defineEmits<{
  more: []
}>()

const improvementClass = computed(() => {
  if (props.overallImprovementRate > 0) return 'improvement--positive'
  if (props.overallImprovementRate < 0) return 'improvement--negative'
  return 'improvement--neutral'
})

const maxAvgScore = computed(() => {
  if (props.roundComparison.length === 0) return 1
  return Math.max(...props.roundComparison.map(r => r.avgScore)) || 1
})

const getBarWidth = (score: number) => {
  return Math.min(100, (score / maxAvgScore.value) * 100)
}
</script>

<style scoped lang="scss">
.round-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid #f0f2f5;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  &__body {
    padding: 16px 20px;
    min-height: 200px;
  }

  &__single {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 200px;
  }

  &__summary {
    display: flex;
    gap: 24px;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px dashed #ebeef5;
  }

  &__comparison {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 20px;
  }

  &__stats {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
  }

  &__footer {
    padding: 12px 20px;
    background: #f5f7fa;
    border-top: 1px solid #f0f2f5;
  }

  &__insight {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #606266;

    .el-icon {
      color: #409eff;
    }

    & + & {
      margin-top: 6px;
    }
  }
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  &__label {
    font-size: 13px;
    color: #909399;
  }

  &__value {
    font-size: 20px;
    font-weight: 600;
    color: #303133;

    &.improvement--positive {
      color: #67c23a;
    }

    &.improvement--negative {
      color: #f56c6c;
    }
  }
}

.round-item {
  display: flex;
  align-items: center;
  gap: 12px;

  &__label {
    width: 80px;
    display: flex;
    flex-direction: column;
  }

  &__number {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }

  &__count {
    font-size: 12px;
    color: #909399;
  }

  &__bar-container {
    flex: 1;
    height: 16px;
    background: #f0f2f5;
    border-radius: 8px;
    overflow: hidden;
  }

  &__bar {
    height: 100%;
    background: linear-gradient(90deg, #409eff, #79bbff);
    border-radius: 8px;
    transition: width 0.3s ease;

    &.improved {
      background: linear-gradient(90deg, #67c23a, #95d475);
    }
  }

  &__score {
    width: 80px;
    text-align: right;
  }

  &__avg {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }

  &__change {
    font-size: 12px;
    margin-left: 4px;

    &.better {
      color: #67c23a;
    }

    &.worse {
      color: #f56c6c;
    }
  }
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 4px;

  &--success {
    color: #67c23a;
    background: #f0f9eb;
  }

  &--danger {
    color: #f56c6c;
    background: #fef0f0;
  }

  &--info {
    color: #909399;
    background: #f4f4f5;
  }
}
</style>
