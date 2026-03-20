<template>
  <div class="corrective-pie-chart">
    <div v-if="title" class="chart-title">{{ title }}</div>
    <div ref="chartRef" class="chart-container" />
    <div v-if="!filteredData.length" class="empty-overlay">
      <el-empty description="暂无整改数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'

use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer])

export interface PieDataItem {
  status: string
  count: number
  color?: string
}

const props = defineProps<{
  data: PieDataItem[]
  title?: string
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null

const defaultColors = [
  '#909399', '#409EFF', '#E6A23C', '#409EFF',
  '#67C23A', '#F56C6C', '#67C23A', '#F56C6C',
]

const filteredData = computed(() => {
  return props.data.filter(d => d.count > 0)
})

const total = computed(() => {
  return filteredData.value.reduce((sum, d) => sum + d.count, 0)
})

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
  if (!filteredData.value.length) {
    chart.clear()
    return
  }

  const seriesData = filteredData.value.map((d, i) => ({
    name: d.status,
    value: d.count,
    itemStyle: { color: d.color || defaultColors[i % defaultColors.length] },
  }))

  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const pct = total.value > 0 ? ((params.value / total.value) * 100).toFixed(1) : '0.0'
        return `${params.name}: <b>${params.value}</b> (${pct}%)`
      },
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { fontSize: 11 },
      formatter: (name: string) => {
        const item = filteredData.value.find(d => d.status === name)
        return item ? `${name}  ${item.count}` : name
      },
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      data: seriesData,
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 12, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.1)' },
      },
    }],
    graphic: [{
      type: 'text',
      left: '26%',
      top: '45%',
      style: {
        text: `${total.value}`,
        textAlign: 'center',
        fontSize: 22,
        fontWeight: 'bold',
        fill: '#303133',
      },
    }, {
      type: 'text',
      left: '27%',
      top: '55%',
      style: {
        text: '总计',
        textAlign: 'center',
        fontSize: 12,
        fill: '#909399',
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
.corrective-pie-chart {
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
