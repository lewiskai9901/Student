<template>
  <div class="ranking-card">
    <div class="ranking-card__header">
      <div class="ranking-card__title">
        <span>{{ title }}</span>
        <el-tag v-if="compareMode" size="small" type="info">{{ compareModeText }}</el-tag>
      </div>
      <div class="ranking-card__actions">
        <el-button-group size="small" v-if="showModeSwitch">
          <el-button
            v-for="mode in modes"
            :key="mode.value"
            :type="currentMode === mode.value ? 'primary' : 'default'"
            @click="handleModeChange(mode.value)"
          >
            {{ mode.label }}
          </el-button>
        </el-button-group>
        <el-button v-if="showMore" link type="primary" @click="$emit('more')">
          更多 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="ranking-card__body" v-loading="loading">
      <div v-if="!loading && rankings.length === 0" class="ranking-card__empty">
        <el-empty description="暂无排名数据" :image-size="80" />
      </div>

      <div v-else class="ranking-card__list">
        <div
          v-for="(item, index) in displayRankings"
          :key="item.classId"
          class="ranking-item"
          :class="{ 'ranking-item--clickable': clickable }"
          @click="handleItemClick(item)"
        >
          <div class="ranking-item__rank">
            <span v-if="index < 3" class="ranking-item__medal" :class="`ranking-item__medal--${index + 1}`">
              {{ ['🥇', '🥈', '🥉'][index] }}
            </span>
            <span v-else class="ranking-item__number">{{ item.ranking }}</span>
          </div>

          <div class="ranking-item__info">
            <div class="ranking-item__name">
              {{ item.className }}
              <el-tag v-if="item.scoreLevel" size="small" :type="getLevelType(item.scoreLevel)">
                {{ item.scoreLevel }}
              </el-tag>
            </div>
            <div class="ranking-item__meta">
              <span v-if="item.gradeName">{{ item.gradeName }}</span>
              <span v-if="item.teacherName"> · {{ item.teacherName }}</span>
              <span v-if="showCheckCount"> · 检查{{ item.checkCount }}次</span>
            </div>
          </div>

          <div class="ranking-item__score">
            <div class="ranking-item__main-score">
              {{ formatScore(getDisplayScore(item)) }}
              <span class="ranking-item__score-unit">分</span>
            </div>
            <div class="ranking-item__sub-info" v-if="showSubInfo">
              <span v-if="showRounds" class="ranking-item__rounds">{{ item.totalRounds }}轮</span>
              <span v-if="item.vsAvg !== undefined" :class="['ranking-item__vs-avg', item.vsAvg > 0 ? 'worse' : 'better']">
                {{ item.vsAvg > 0 ? '+' : '' }}{{ item.vsAvg?.toFixed(1) }}
              </span>
            </div>
          </div>

          <div class="ranking-item__trend" v-if="showTrend && item.trend">
            <el-icon :class="['trend-icon', `trend-icon--${item.trend}`]">
              <Top v-if="item.trend === 'up'" />
              <Bottom v-else-if="item.trend === 'down'" />
              <Minus v-else />
            </el-icon>
          </div>
        </div>
      </div>
    </div>

    <div class="ranking-card__footer" v-if="warnings.length > 0 || showFooter">
      <div v-for="(warning, index) in warnings" :key="index" class="ranking-card__warning">
        <el-icon><WarningFilled /></el-icon>
        {{ warning }}
      </div>
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowRight, Top, Bottom, Minus, WarningFilled } from '@element-plus/icons-vue'
import type { SmartClassRankingVO } from '@/api/smartStatistics'

interface Props {
  title?: string
  rankings: SmartClassRankingVO[]
  loading?: boolean
  compareMode?: string
  showModeSwitch?: boolean
  showMore?: boolean
  showTrend?: boolean
  showCheckCount?: boolean
  showRounds?: boolean
  showSubInfo?: boolean
  showFooter?: boolean
  limit?: number
  clickable?: boolean
  warnings?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  title: '班级排名',
  loading: false,
  showModeSwitch: true,
  showMore: true,
  showTrend: true,
  showCheckCount: false,
  showRounds: true,
  showSubInfo: true,
  showFooter: true,
  limit: 10,
  clickable: true,
  warnings: () => []
})

const emit = defineEmits<{
  more: []
  modeChange: [mode: string]
  itemClick: [item: SmartClassRankingVO]
}>()

const modes = [
  { value: 'average', label: '平均分' },
  { value: 'total', label: '总分' },
  { value: 'weighted', label: '加权分' }
]

const currentMode = ref(props.compareMode || 'average')

const displayRankings = computed(() => {
  return props.rankings.slice(0, props.limit)
})

const compareModeText = computed(() => {
  const mode = modes.find(m => m.value === currentMode.value)
  return mode ? `按${mode.label}` : ''
})

const getDisplayScore = (item: SmartClassRankingVO) => {
  switch (currentMode.value) {
    case 'total':
      return item.totalScore
    case 'weighted':
      return item.weightedScore || item.totalScore
    case 'average':
    default:
      return item.avgScorePerRound
  }
}

const formatScore = (score: number | undefined) => {
  if (score === undefined) return '--'
  return score.toFixed(1)
}

const getLevelType = (level: string) => {
  switch (level) {
    case '优秀': return 'success'
    case '良好': return ''
    case '中等': return 'warning'
    case '较差': return 'danger'
    default: return 'info'
  }
}

const handleModeChange = (mode: string) => {
  currentMode.value = mode
  emit('modeChange', mode)
}

const handleItemClick = (item: SmartClassRankingVO) => {
  if (props.clickable) {
    emit('itemClick', item)
  }
}
</script>

<style scoped lang="scss">
.ranking-card {
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
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__body {
    padding: 12px 20px;
    min-height: 200px;
  }

  &__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 200px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__footer {
    padding: 12px 20px;
    background: #fafbfc;
    border-top: 1px solid #f0f2f5;
  }

  &__warning {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #e6a23c;

    .el-icon {
      font-size: 14px;
    }
  }
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #fafbfc;
  transition: all 0.2s ease;

  &--clickable {
    cursor: pointer;

    &:hover {
      background: #f0f2f5;
    }
  }

  &__rank {
    width: 36px;
    display: flex;
    justify-content: center;
  }

  &__medal {
    font-size: 24px;
    line-height: 1;
  }

  &__number {
    font-size: 16px;
    font-weight: 600;
    color: #909399;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }

  &__meta {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }

  &__score {
    text-align: right;
    min-width: 80px;
  }

  &__main-score {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  &__score-unit {
    font-size: 12px;
    color: #909399;
    font-weight: 400;
  }

  &__sub-info {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }

  &__rounds {
    color: #909399;
  }

  &__vs-avg {
    &.better {
      color: #67c23a;
    }
    &.worse {
      color: #f56c6c;
    }
  }

  &__trend {
    width: 24px;
    display: flex;
    justify-content: center;
  }
}

.trend-icon {
  font-size: 16px;

  &--up {
    color: #f56c6c;
  }

  &--down {
    color: #67c23a;
  }

  &--stable {
    color: #909399;
  }
}
</style>
