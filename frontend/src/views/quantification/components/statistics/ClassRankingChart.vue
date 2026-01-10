<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, TooltipComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([BarChart, CanvasRenderer, GridComponent, TooltipComponent])
import * as echarts from 'echarts/core'
import type { ClassRanking } from '@/api/v2/quantification-extra'

const props = defineProps<{
  data: ClassRanking[]
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

  // 响应式
  window.addEventListener('resize', handleResize)
}

function handleResize() {
  chart?.resize()
}

function updateChart() {
  if (!chart) return

  const top10 = props.data.slice(0, 10)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const data = params[0]
        const item = top10[data.dataIndex]
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 8px;">${item.className}</div>
            <div>排名: 第${item.ranking}名</div>
            <div>总扣分: ${item.totalScore.toFixed(1)}分</div>
            <div>检查次数: ${item.checkCount}次</div>
            <div>平均扣分: ${item.avgScorePerCheck.toFixed(1)}分/次</div>
            <div>等级: ${item.scoreLevel}</div>
          </div>
        `
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '扣分',
      axisLabel: {
        formatter: '{value}分'
      }
    },
    yAxis: {
      type: 'category',
      data: top10.map(item => item.className).reverse(),
      axisLabel: {
        width: 120,
        overflow: 'truncate',
        fontSize: 12
      }
    },
    series: [
      {
        name: '扣分',
        type: 'bar',
        data: top10.map(item => ({
          value: item.totalScore,
          itemStyle: {
            color: getBarColor(item.scoreLevel)
          }
        })).reverse(),
        barWidth: '60%',
        label: {
          show: true,
          position: 'right',
          formatter: '{c}分'
        }
      }
    ]
  }

  chart.setOption(option)
}

function getBarColor(level: string): string {
  switch (level) {
    case '优秀':
      return '#67c23a'
    case '良好':
      return '#409eff'
    case '中等':
      return '#e6a23c'
    case '较差':
      return '#f56c6c'
    default:
      return '#909399'
  }
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
  height: 350px;
}
</style>
