<template>
  <div class="score-trend-chart">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div ref="chartRef" class="chart-container" />
    <div v-if="!data.length" class="empty-overlay">
      <el-empty description="暂无趋势数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  MarkLineComponent,
  DataZoomComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'

use([LineChart, GridComponent, TooltipComponent, MarkLineComponent, DataZoomComponent, CanvasRenderer])

export interface TrendDataPoint {
  date: string
  score: number
}

const props = withDefaults(defineProps<{
  data: TrendDataPoint[]
  title?: string
  color?: string
  showArea?: boolean
}>(), {
  color: '#409EFF',
  showArea: true,
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

  const dates = props.data.map(d => d.date)
  const scores = props.data.map(d => d.score)
  const avgScore = scores.reduce((a, b) => a + b, 0) / scores.length

  chart.setOption({
    grid: { top: 20, right: 20, bottom: 40, left: 50 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.axisValue}<br/>得分: <b>${p.value.toFixed(1)}</b>`
      },
    },
    xAxis: {
      type: 'category',
      data: dates.map(d => d.slice(5)),
      axisLabel: { fontSize: 11, rotate: dates.length > 14 ? 45 : 0 },
      boundaryGap: false,
    },
    yAxis: {
      type: 'value',
      min: Math.max(0, Math.floor(Math.min(...scores) * 0.9)),
      axisLabel: { fontSize: 11 },
    },
    dataZoom: dates.length > 30
      ? [{ type: 'inside', start: 70, end: 100 }]
      : [],
    series: [{
      type: 'line',
      data: scores,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      itemStyle: { color: props.color },
      lineStyle: { width: 2 },
      areaStyle: props.showArea
        ? {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: `${props.color}40` },
              { offset: 1, color: `${props.color}05` },
            ]),
          }
        : undefined,
      markLine: {
        silent: true,
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#E6A23C' },
        data: [{ yAxis: +avgScore.toFixed(1), label: { formatter: `avg: ${avgScore.toFixed(1)}`, fontSize: 10 } }],
      },
    }],
  }, true)
}

function handleResize() {
  chart?.resize()
}

defineExpose({ resize: handleResize })
</script>

<style scoped>
.score-trend-chart {
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
