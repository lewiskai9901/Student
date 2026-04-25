<template>
  <div class="pareto-chart">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div ref="chartRef" class="chart-container" />
    <div v-if="!data.length" class="empty-overlay">
      <el-empty description="暂无频率数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'

use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

export interface ParetoItem {
  item: string
  count: number
  cumPercent: number
}

const props = withDefaults(defineProps<{
  data: ParetoItem[]
  title?: string
  barColor?: string
  lineColor?: string
}>(), {
  barColor: '#409EFF',
  lineColor: '#E6A23C',
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

  const items = props.data.map(d => d.item)
  const counts = props.data.map(d => d.count)
  const cumPercents = props.data.map(d => d.cumPercent)

  chart.setOption({
    grid: { top: 40, right: 50, bottom: 60, left: 50 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: any) => {
        if (!Array.isArray(params) || params.length === 0) return ''
        let html = `<b>${params[0].axisValue}</b><br/>`
        params.forEach((p: any) => {
          const marker = `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color};margin-right:4px;"></span>`
          if (p.seriesName === '发生次数') {
            html += `${marker}${p.seriesName}: <b>${p.value}</b><br/>`
          } else {
            html += `${marker}${p.seriesName}: <b>${p.value.toFixed(1)}%</b><br/>`
          }
        })
        return html
      },
    },
    legend: {
      data: ['发生次数', '累计百分比'],
      top: 0,
      textStyle: { fontSize: 11 },
    },
    xAxis: {
      type: 'category',
      data: items,
      axisLabel: {
        fontSize: 10,
        rotate: items.length > 6 ? 40 : 0,
        width: 60,
        overflow: 'truncate',
      },
    },
    yAxis: [
      {
        type: 'value',
        name: '次数',
        nameTextStyle: { fontSize: 11 },
        axisLabel: { fontSize: 11 },
      },
      {
        type: 'value',
        name: '累计%',
        nameTextStyle: { fontSize: 11 },
        min: 0,
        max: 100,
        axisLabel: { fontSize: 11, formatter: '{value}%' },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '发生次数',
        type: 'bar',
        data: counts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: props.barColor },
            { offset: 1, color: `${props.barColor}60` },
          ]),
        },
        barMaxWidth: 30,
      },
      {
        name: '累计百分比',
        type: 'line',
        yAxisIndex: 1,
        data: cumPercents,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: props.lineColor },
        lineStyle: { width: 2 },
        // 80% reference line for Pareto rule
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { type: 'dashed', color: '#F56C6C' },
          data: [{
            yAxis: 80,
            label: { formatter: '80%', fontSize: 10, position: 'end' },
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
.pareto-chart {
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
