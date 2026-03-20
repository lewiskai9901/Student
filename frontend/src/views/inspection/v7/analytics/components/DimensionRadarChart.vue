<template>
  <div class="dimension-radar-chart">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div ref="chartRef" class="chart-container" />
    <div v-if="!dimensions.length" class="empty-overlay">
      <el-empty description="暂无维度数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { use } from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'

use([RadarChart, TooltipComponent, LegendComponent, CanvasRenderer])

export interface DimensionData {
  name: string
  score: number
  maxScore: number
}

const props = withDefaults(defineProps<{
  dimensions: DimensionData[]
  title?: string
  color?: string
}>(), {
  color: '#409EFF',
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

watch(() => props.dimensions, () => {
  nextTick(() => updateChart())
}, { deep: true })

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chart) return
  if (!props.dimensions.length) {
    chart.clear()
    return
  }

  const indicators = props.dimensions.map(d => ({
    name: d.name,
    max: d.maxScore,
  }))

  const values = props.dimensions.map(d => d.score)

  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const data = params.data
        if (!data || !data.value) return ''
        let html = `${params.name}<br/>`
        props.dimensions.forEach((dim, i) => {
          html += `${dim.name}: <b>${data.value[i].toFixed(1)}</b> / ${dim.maxScore}<br/>`
        })
        return html
      },
    },
    radar: {
      indicator: indicators,
      radius: '65%',
      center: ['50%', '55%'],
      axisName: {
        fontSize: 11,
        color: '#606266',
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(64,158,255,0.02)', 'rgba(64,158,255,0.05)'],
        },
      },
      splitLine: {
        lineStyle: { color: '#E4E7ED' },
      },
      axisLine: {
        lineStyle: { color: '#DCDFE6' },
      },
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '当前得分',
        areaStyle: { color: `${props.color}30` },
        lineStyle: { color: props.color, width: 2 },
        itemStyle: { color: props.color },
        symbol: 'circle',
        symbolSize: 6,
      }],
    }],
  }, true)
}

function handleResize() {
  chart?.resize()
}

defineExpose({ resize: handleResize })
</script>

<style scoped>
.dimension-radar-chart {
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
