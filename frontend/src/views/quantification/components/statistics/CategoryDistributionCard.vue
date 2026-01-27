<template>
  <div class="category-card">
    <div class="category-card__header">
      <span class="category-card__title">{{ title }}</span>
      <el-button v-if="showMore" link type="primary" @click="$emit('more')">
        详情 <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <div class="category-card__body" v-loading="loading">
      <div v-if="!loading && categories.length === 0" class="category-card__empty">
        <el-empty description="暂无类别数据" :image-size="60" />
      </div>

      <template v-else>
        <div class="category-card__chart" ref="chartRef"></div>

        <div class="category-card__list">
          <div
            v-for="(item, index) in displayCategories"
            :key="item.categoryId"
            class="category-item"
          >
            <div class="category-item__dot" :style="{ backgroundColor: colors[index % colors.length] }"></div>
            <div class="category-item__info">
              <span class="category-item__name">{{ item.categoryName }}</span>
              <span class="category-item__meta">{{ item.triggerCount }}次 · {{ item.affectedClasses }}班</span>
            </div>
            <div class="category-item__score">
              <span class="category-item__value">{{ item.totalScore.toFixed(1) }}</span>
              <span class="category-item__percentage">({{ item.percentage.toFixed(0) }}%)</span>
            </div>
          </div>
        </div>
      </template>
    </div>

    <div class="category-card__footer" v-if="topCategory">
      <div class="category-card__insight">
        <el-icon><TrendCharts /></el-icon>
        <span><strong>{{ topCategory }}</strong>类问题占比最高({{ topCategoryPercentage?.toFixed(0) }}%)</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ArrowRight, TrendCharts } from '@element-plus/icons-vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([PieChart, CanvasRenderer, TooltipComponent, LegendComponent])
import * as echarts from 'echarts/core'
import type { CategoryStatsDetailVO } from '@/api/quantification-extra'

interface Props {
  title?: string
  categories: CategoryStatsDetailVO[]
  loading?: boolean
  showMore?: boolean
  limit?: number
  topCategory?: string
  topCategoryPercentage?: number
}

const props = withDefaults(defineProps<Props>(), {
  title: '扣分类别分布',
  loading: false,
  showMore: true,
  limit: 5
})

defineEmits<{
  more: []
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null

const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

const displayCategories = computed(() => {
  return props.categories.slice(0, props.limit)
})

const initChart = () => {
  if (!chartRef.value || props.categories.length === 0) return

  if (chart) {
    chart.dispose()
  }

  chart = echarts.init(chartRef.value)

  const data = props.categories.map((item, index) => ({
    name: item.categoryName,
    value: item.totalScore,
    itemStyle: {
      color: colors[index % colors.length]
    }
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}分 ({d}%)'
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data
      }
    ]
  }

  chart.setOption(option)
}

const handleResize = () => {
  chart?.resize()
}

watch(() => props.categories, () => {
  initChart()
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.category-card {
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

  &__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 200px;
  }

  &__chart {
    height: 180px;
    margin-bottom: 16px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 8px;
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

    strong {
      color: #303133;
    }
  }
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;

  &__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 14px;
    color: #303133;
  }

  &__meta {
    font-size: 12px;
    color: #909399;
    margin-left: 8px;
  }

  &__score {
    text-align: right;
  }

  &__value {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }

  &__percentage {
    font-size: 12px;
    color: #909399;
    margin-left: 4px;
  }
}
</style>
