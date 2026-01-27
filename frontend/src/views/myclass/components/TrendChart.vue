<template>
  <div ref="chartRef" class="trend-chart" />
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
// ECharts modular imports
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import * as echarts from 'echarts/core'
import type { ScoreTrendItem } from '@/types/myClass'

// Register ECharts components
use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = withDefaults(defineProps<{
  data: ScoreTrendItem[]
  color?: string
}>(), {
  color: '#409EFF'
})

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null

function initChart() {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()

  // Add resize listener
  window.addEventListener('resize', handleResize)
}

function updateChart() {
  if (!chart) return

  // Handle empty data
  if (!props.data || props.data.length === 0) {
    chart.clear()
    return
  }

  // Extract dates and scores
  const dates = props.data.map(item => formatDateLabel(item.date))
  const scores = props.data.map(item => item.score)

  // Calculate min value for y-axis (95% of lowest value, but not below 0)
  const minScore = Math.min(...scores)
  const yAxisMin = Math.max(0, Math.floor(minScore * 0.95))

  const option: echarts.EChartsOption = {
    grid: {
      top: 20,
      right: 20,
      bottom: 30,
      left: 50
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ebeef5',
      borderWidth: 1,
      textStyle: {
        color: '#303133'
      },
      formatter: (params: unknown) => {
        const dataParams = params as { dataIndex: number; value: number }[]
        if (Array.isArray(dataParams) && dataParams.length > 0) {
          const { dataIndex, value } = dataParams[0]
          const originalDate = props.data[dataIndex]?.date || ''
          return `
            <div style="padding: 4px 8px;">
              <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">${originalDate}</div>
              <div style="font-size: 14px; font-weight: 600; color: #303133;">
                <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background-color: ${props.color}; margin-right: 6px;"></span>
                得分: ${value.toFixed(1)} 分
              </div>
            </div>
          `
        }
        return ''
      }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#909399',
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      min: yAxisMin,
      max: 100,
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#909399',
        fontSize: 12,
        formatter: '{value}'
      },
      splitLine: {
        lineStyle: {
          color: '#ebeef5',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        type: 'line',
        data: scores,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: true,
        lineStyle: {
          width: 2,
          color: props.color
        },
        itemStyle: {
          color: props.color,
          borderColor: '#fff',
          borderWidth: 2
        },
        emphasis: {
          itemStyle: {
            color: props.color,
            borderColor: props.color,
            borderWidth: 2,
            shadowColor: 'rgba(64, 158, 255, 0.3)',
            shadowBlur: 8
          }
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: props.color + '30' },
              { offset: 0.5, color: props.color + '15' },
              { offset: 1, color: props.color + '05' }
            ]
          }
        }
      }
    ]
  }

  chart.setOption(option, true)
}

function formatDateLabel(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  return `${date.getMonth() + 1}/${date.getDate()}`
}

function handleResize() {
  chart?.resize()
}

watch(() => props.data, () => {
  updateChart()
}, { deep: true })

watch(() => props.color, () => {
  updateChart()
})

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style lang="scss" scoped>
.trend-chart {
  width: 100%;
  height: 200px;
}
</style>
