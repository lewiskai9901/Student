<template>
  <div class="metric-card" :class="[`metric-card--${type}`, { 'metric-card--clickable': clickable }]" @click="handleClick">
    <div class="metric-card__header">
      <span class="metric-card__label">{{ label }}</span>
      <el-tooltip v-if="tooltip" :content="tooltip" placement="top">
        <el-icon class="metric-card__info"><InfoFilled /></el-icon>
      </el-tooltip>
    </div>

    <div class="metric-card__body">
      <div class="metric-card__value">
        <span v-if="loading" class="metric-card__loading">--</span>
        <template v-else>
          <span class="metric-card__prefix" v-if="prefix">{{ prefix }}</span>
          <span class="metric-card__number">{{ formattedValue }}</span>
          <span class="metric-card__suffix" v-if="suffix">{{ suffix }}</span>
        </template>
      </div>

      <div class="metric-card__trend" v-if="trend && !loading">
        <span :class="['metric-card__trend-icon', `metric-card__trend-icon--${trendDirection}`]">
          <el-icon v-if="trendDirection === 'up'"><Top /></el-icon>
          <el-icon v-else-if="trendDirection === 'down'"><Bottom /></el-icon>
          <el-icon v-else><Minus /></el-icon>
        </span>
        <span class="metric-card__trend-text">{{ trendText }}</span>
      </div>
    </div>

    <div class="metric-card__progress" v-if="showProgress && !loading">
      <el-progress
        :percentage="progressPercentage"
        :stroke-width="4"
        :show-text="false"
        :color="progressColor"
      />
    </div>

    <div class="metric-card__footer" v-if="$slots.footer || description">
      <slot name="footer">
        <span class="metric-card__description">{{ description }}</span>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { InfoFilled, Top, Bottom, Minus } from '@element-plus/icons-vue'

interface Props {
  label: string
  value: number | string
  prefix?: string
  suffix?: string
  tooltip?: string
  description?: string
  type?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info'
  trend?: {
    direction: 'up' | 'down' | 'stable'
    value?: number | string
    text?: string
  }
  showProgress?: boolean
  progressValue?: number
  progressMax?: number
  loading?: boolean
  clickable?: boolean
  decimals?: number
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  showProgress: false,
  progressValue: 0,
  progressMax: 100,
  loading: false,
  clickable: false,
  decimals: 2
})

const emit = defineEmits<{
  click: []
}>()

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    // 如果是整数，不显示小数
    if (Number.isInteger(props.value)) {
      return props.value.toLocaleString()
    }
    return props.value.toFixed(props.decimals)
  }
  return props.value
})

const trendDirection = computed(() => {
  return props.trend?.direction || 'stable'
})

const trendText = computed(() => {
  if (props.trend?.text) return props.trend.text
  if (props.trend?.value !== undefined) {
    const val = typeof props.trend.value === 'number'
      ? (props.trend.value > 0 ? '+' : '') + props.trend.value.toFixed(1) + '%'
      : props.trend.value
    return val
  }
  return ''
})

const progressPercentage = computed(() => {
  if (props.progressMax === 0) return 0
  return Math.min(100, Math.max(0, (props.progressValue / props.progressMax) * 100))
})

const progressColor = computed(() => {
  const percentage = progressPercentage.value
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
})

const handleClick = () => {
  if (props.clickable) {
    emit('click')
  }
}
</script>

<style scoped lang="scss">
.metric-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;

  &--clickable {
    cursor: pointer;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }
  }

  &--primary {
    border-left: 4px solid #409eff;
  }

  &--success {
    border-left: 4px solid #67c23a;
  }

  &--warning {
    border-left: 4px solid #e6a23c;
  }

  &--danger {
    border-left: 4px solid #f56c6c;
  }

  &--info {
    border-left: 4px solid #909399;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
  }

  &__label {
    font-size: 14px;
    color: #909399;
    font-weight: 400;
  }

  &__info {
    font-size: 14px;
    color: #c0c4cc;
    cursor: help;
  }

  &__body {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 12px;
  }

  &__value {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }

  &__loading {
    font-size: 28px;
    color: #c0c4cc;
  }

  &__prefix {
    font-size: 16px;
    color: #606266;
  }

  &__number {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
    line-height: 1;
  }

  &__suffix {
    font-size: 14px;
    color: #909399;
    margin-left: 2px;
  }

  &__trend {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    white-space: nowrap;
  }

  &__trend-icon {
    display: flex;
    align-items: center;

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

  &__trend-text {
    color: #606266;
  }

  &__progress {
    margin-top: 12px;
  }

  &__footer {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px dashed #ebeef5;
  }

  &__description {
    font-size: 12px;
    color: #909399;
  }
}
</style>
