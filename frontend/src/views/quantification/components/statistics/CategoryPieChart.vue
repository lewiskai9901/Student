<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([PieChart, CanvasRenderer, TooltipComponent, LegendComponent])
import * as echarts from 'echarts/core'
import type { CategoryStatistics } from '@/api/quantification-extra'

const props = defineProps<{
  data: CategoryStatistics[]
}>()

const chartRef = ref<HTMLElement>()
let chart: ECharts | null = null

// 颜色方案
const colors = [
  '#5470c6',
  '#91cc75',
  '#fac858',
  '#ee6666',
  '#73c0de',
  '#3ba272',
  '#fc8452',
  '#9a60b4',
  '#ea7ccc'
]

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
  if (!chart) return

  const pieData = props.data.map((item, index) => ({
    name: item.categoryName,
    value: item.totalScore,
    itemStyle: {
      color: colors[index % colors.length]
    },
    // 保存额外数据用于tooltip
    percentage: item.percentage,
    itemCount: item.itemCount,
    classCount: item.classCount
  }))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const data = params.data
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 8px;">${data.name}</div>
            <div>扣分: ${data.value.toFixed(1)}分</div>
            <div>占比: ${data.percentage.toFixed(1)}%</div>
            <div>扣分项次: ${data.itemCount}次</div>
            <div>涉及班级: ${data.classCount}个</div>
          </div>
        `
      }
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 10,
      top: 20,
      bottom: 20
    },
    series: [
      {
        name: '类别分布',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold',
            formatter: (params: any) => {
              return `${params.name}\n${params.data.percentage.toFixed(1)}%`
            }
          }
        },
        labelLine: {
          show: false
        },
        data: pieData
      }
    ]
  }

  chart.setOption(option)
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
