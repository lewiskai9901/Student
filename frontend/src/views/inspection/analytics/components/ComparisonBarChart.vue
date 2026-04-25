<template>
  <div class="comparison-bar-chart">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div ref="chartRef" class="chart-container" />
    <div v-if="!data.length" class="empty-overlay">
      <el-empty description="暂无对比数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'

use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

export interface ComparisonItem {
  name: string
  value1: number
  value2: number
}

const props = withDefaults(defineProps<{
  data: ComparisonItem[]
  labels: [string, string]
  title?: string
  colors?: [string, string]
  horizontal?: boolean
}>(), {
  colors: () => ['#409EFF', '#67C23A'],
  horizontal: false,
})

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})

watch(() => props.data, () => {
  nextTick(() => updateChart())
}, { deep: true })

watch(() => props.labels, () => {
  nextTick(() => updateChart())
}, { deep: true })

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chart) return
  if (!props.data.length) {
    chart.clear()
    return
  }

  const names = props.data.map(d => d.name)
  const values1 = props.data.map(d => d.value1)
  const values2 = props.data.map(d => d.value2)

  const allValues = [...values1, ...values2].filter(v => v > 0)
  const minVal = allValues.length ? Math.max(0, Math.floor(Math.min(...allValues) * 0.85)) : 0

  const categoryAxis = {
    type: 'category' as const,
    data: names,
    axisLabel: {
      fontSize: 11,
      width: props.horizontal ? 70 : undefined,
      overflow: props.horizontal ? 'truncate' as const : undefined,
      rotate: !props.horizontal && names.length > 6 ? 30 : 0,
    },
  }

  const valueAxis = {
    type: 'value' as const,
    min: minVal,
    axisLabel: { fontSize: 11 },
  }

  chart.setOption({
    grid: {
      top: 35,
      right: 20,
      bottom: props.horizontal ? 30 : 50,
      left: props.horizontal ? 80 : 50,
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    legend: {
      data: [props.labels[0], props.labels[1]],
      top: 0,
      textStyle: { fontSize: 11 },
    },
    xAxis: props.horizontal ? valueAxis : categoryAxis,
    yAxis: props.horizontal ? categoryAxis : valueAxis,
    series: [
      {
        name: props.labels[0],
        type: 'bar',
        data: values1,
        itemStyle: { color: props.colors[0] },
        barMaxWidth: 24,
        barGap: '20%',
      },
      {
        name: props.labels[1],
        type: 'bar',
        data: values2,
        itemStyle: { color: props.colors[1] },
        barMaxWidth: 24,
      },
    ],
  }, true)
}

function handleResize() {
  chart?.resize()
}

defineExpose({ resize: handleResize })
</script>

<style scoped>
.comparison-bar-chart {
  position: relative;
  width: 100%;
}

.chart-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 280px;
}

.empty-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
}
</style>
