<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([LineChart, CanvasRenderer, GridComponent, TooltipComponent, LegendComponent])
import * as echarts from 'echarts/core'
import type { TrendData } from '@/api/checkPlanStatistics'

const props = defineProps<{
  data: TrendData | null
}>()

const chartRef = ref<HTMLElement>()
let chart: ECharts | null = null

function initChart() {
  if (!chartRef.value) return

  // 确保DOM有尺寸
  if (chartRef.value.clientWidth === 0 || chartRef.value.clientHeight === 0) {
    setTimeout(initChart, 100)
    return
  }

  chart = echarts.init(chartRef.value)
  updateChart()

  window.addEventListener('resize', handleResize)
}

function handleResize() {
  chart?.resize()
}

function updateChart() {
  if (!chart || !props.data) {
    // 无数据时显示空状态
    if (chart) {
      chart.setOption({
        title: {
          text: '暂无数据',
          left: 'center',
          top: 'center',
          textStyle: {
            color: '#909399',
            fontSize: 16
          }
        }
      })
    }
    return
  }

  const trendPoints = props.data.trendPoints

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      },
      formatter: (params: any) => {
        const point = trendPoints[params[0].dataIndex]
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 8px;">${point.dateLabel}</div>
            <div>检查次数: ${point.checkCount}次</div>
            <div>总扣分: ${point.totalScore.toFixed(1)}分</div>
            <div>平均扣分: ${point.avgScore.toFixed(1)}分</div>
            <div>涉及班级: ${point.classCount}个</div>
            <div>涉及人次: ${point.personCount}人</div>
          </div>
        `
      }
    },
    legend: {
      data: ['总扣分', '检查次数', '平均扣分'],
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
      boundaryGap: false,
      data: trendPoints.map(p => p.dateLabel)
    },
    yAxis: [
      {
        type: 'value',
        name: '扣分',
        position: 'left',
        axisLabel: {
          formatter: '{value}分'
        }
      },
      {
        type: 'value',
        name: '次数',
        position: 'right',
        axisLabel: {
          formatter: '{value}次'
        }
      }
    ],
    series: [
      {
        name: '总扣分',
        type: 'line',
        data: trendPoints.map(p => p.totalScore),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#409eff'
        },
        itemStyle: {
          color: '#409eff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        }
      },
      {
        name: '检查次数',
        type: 'bar',
        yAxisIndex: 1,
        data: trendPoints.map(p => p.checkCount),
        barWidth: '40%',
        itemStyle: {
          color: 'rgba(103, 194, 58, 0.6)',
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '平均扣分',
        type: 'line',
        data: trendPoints.map(p => p.avgScore),
        smooth: true,
        symbol: 'diamond',
        symbolSize: 6,
        lineStyle: {
          width: 2,
          color: '#e6a23c',
          type: 'dashed'
        },
        itemStyle: {
          color: '#e6a23c'
        }
      }
    ]
  }

  chart.setOption(option, true)
}

watch(() => props.data, () => {
  updateChart()
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    initChart()
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 300px;
}
</style>
