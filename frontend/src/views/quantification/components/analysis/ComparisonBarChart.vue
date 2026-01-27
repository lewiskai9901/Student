<template>
  <div ref="chartRef" class="comparison-bar-chart" />
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([BarChart, CanvasRenderer, GridComponent, TooltipComponent, LegendComponent])
import * as echarts from 'echarts/core'
import type { DistributionItem } from '@/api/quantification-extra'

const props = defineProps<{
  data: DistributionItem[] | null
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null

function initChart() {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()

  window.addEventListener('resize', handleResize)
}

function updateChart() {
  if (!chart || !props.data) return

  // 按值排序
  const sortedData = [...props.data].sort((a, b) => Number(b.value) - Number(a.value))

  const names = sortedData.map(item => item.name)
  const values = sortedData.map(item => Number(item.value).toFixed(2))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const item = sortedData.find(d => d.name === params[0].name)
        return `${params[0].name}<br/>
          扣分: ${params[0].value}<br/>
          班级数: ${item?.count || 0}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        interval: 0,
        rotate: names.length > 5 ? 30 : 0
      }
    },
    yAxis: {
      type: 'value',
      name: '扣分'
    },
    series: [
      {
        type: 'bar',
        data: values,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#67c23a' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        label: {
          show: true,
          position: 'top',
          formatter: '{c}'
        },
        barMaxWidth: 50
      }
    ]
  }

  chart.setOption(option)
}

function handleResize() {
  chart?.resize()
}

watch(() => props.data, () => {
  updateChart()
}, { deep: true })

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.comparison-bar-chart {
  width: 100%;
  height: 300px;
}
</style>
