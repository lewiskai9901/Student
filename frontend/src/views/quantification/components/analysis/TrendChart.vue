<template>
  <div ref="chartRef" class="trend-chart" />
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, TooltipComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([LineChart, CanvasRenderer, GridComponent, TooltipComponent])
import * as echarts from 'echarts/core'
import type { TrendPoint } from '@/api/v2/quantification-extra'

const props = defineProps<{
  data: TrendPoint[] | null
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null

function initChart() {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()

  // 响应式
  window.addEventListener('resize', handleResize)
}

function updateChart() {
  if (!chart || !props.data) return

  const xData = props.data.map(p => p.dateLabel)
  const checkCountData = props.data.map(p => p.checkCount)
  const avgScoreData = props.data.map(p => Number(p.avgScore).toFixed(2))
  const totalScoreData = props.data.map(p => Number(p.totalScore).toFixed(2))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['检查次数', '平均扣分', '总扣分'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: {
        rotate: xData.length > 10 ? 45 : 0
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '扣分',
        position: 'left',
        axisLine: {
          show: true,
          lineStyle: {
            color: '#409eff'
          }
        }
      },
      {
        type: 'value',
        name: '次数',
        position: 'right',
        axisLine: {
          show: true,
          lineStyle: {
            color: '#67c23a'
          }
        }
      }
    ],
    series: [
      {
        name: '检查次数',
        type: 'bar',
        yAxisIndex: 1,
        data: checkCountData,
        itemStyle: {
          color: '#67c23a'
        },
        barWidth: '30%'
      },
      {
        name: '平均扣分',
        type: 'line',
        data: avgScoreData,
        smooth: true,
        itemStyle: {
          color: '#409eff'
        },
        lineStyle: {
          width: 2
        },
        symbol: 'circle',
        symbolSize: 8
      },
      {
        name: '总扣分',
        type: 'line',
        data: totalScoreData,
        smooth: true,
        itemStyle: {
          color: '#e6a23c'
        },
        lineStyle: {
          width: 2,
          type: 'dashed'
        },
        symbol: 'diamond',
        symbolSize: 8
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
.trend-chart {
  width: 100%;
  height: 300px;
}
</style>
