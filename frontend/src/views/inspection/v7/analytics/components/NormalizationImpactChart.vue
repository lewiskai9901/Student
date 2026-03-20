<template>
  <div class="normalization-impact-chart">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div ref="chartRef" class="chart-container" />
    <div v-if="!data.length" class="empty-overlay">
      <el-empty description="暂无归一化数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, MarkLineComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'

use([BarChart, GridComponent, TooltipComponent, LegendComponent, MarkLineComponent, CanvasRenderer])

export interface NormalizationItem {
  name: string
  rawScore: number
  normalizedScore: number
}

const props = withDefaults(defineProps<{
  data: NormalizationItem[]
  title?: string
  rawColor?: string
  normalizedColor?: string
}>(), {
  rawColor: '#909399',
  normalizedColor: '#409EFF',
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

  const names = props.data.map(d => d.name)
  const rawScores = props.data.map(d => d.rawScore)
  const normalizedScores = props.data.map(d => d.normalizedScore)

  const allScores = [...rawScores, ...normalizedScores]
  const minScore = Math.max(0, Math.floor(Math.min(...allScores) * 0.85))

  // Calculate average impact
  const avgDiff = props.data.reduce((sum, d) => sum + (d.normalizedScore - d.rawScore), 0) / props.data.length

  chart.setOption({
    grid: { top: 40, right: 20, bottom: 50, left: 50 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        if (!Array.isArray(params) || params.length === 0) return ''
        const name = params[0].axisValue
        let html = `<b>${name}</b><br/>`
        let raw = 0
        let norm = 0
        params.forEach((p: any) => {
          const marker = `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color};margin-right:4px;"></span>`
          html += `${marker}${p.seriesName}: <b>${p.value.toFixed(1)}</b><br/>`
          if (p.seriesName === '原始分') raw = p.value
          if (p.seriesName === '归一化分') norm = p.value
        })
        const diff = norm - raw
        const diffSign = diff >= 0 ? '+' : ''
        html += `<span style="color:${diff >= 0 ? '#67C23A' : '#F56C6C'}">差值: ${diffSign}${diff.toFixed(1)}</span>`
        return html
      },
    },
    legend: {
      data: ['原始分', '归一化分'],
      top: 0,
      textStyle: { fontSize: 11 },
    },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        fontSize: 10,
        rotate: names.length > 8 ? 35 : 0,
        width: 60,
        overflow: 'truncate',
      },
    },
    yAxis: {
      type: 'value',
      min: minScore,
      axisLabel: { fontSize: 11 },
    },
    series: [
      {
        name: '原始分',
        type: 'bar',
        data: rawScores,
        itemStyle: { color: props.rawColor, borderRadius: [2, 2, 0, 0] },
        barMaxWidth: 20,
        barGap: '10%',
      },
      {
        name: '归一化分',
        type: 'bar',
        data: normalizedScores,
        itemStyle: { color: props.normalizedColor, borderRadius: [2, 2, 0, 0] },
        barMaxWidth: 20,
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { type: 'dashed', color: '#E6A23C' },
          data: [{
            yAxis: +(normalizedScores.reduce((a, b) => a + b, 0) / normalizedScores.length).toFixed(1),
            label: {
              formatter: (p: any) => `avg: ${p.value}`,
              fontSize: 10,
              position: 'end',
            },
          }],
        },
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
.normalization-impact-chart {
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
  height: 300px;
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
