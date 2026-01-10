<template>
  <div ref="chartRef" class="distribution-pie-chart" />
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([PieChart, CanvasRenderer, TooltipComponent, LegendComponent])
import * as echarts from 'echarts/core'
import type { DistributionItem } from '@/api/v2/quantification-extra'

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

  const pieData = props.data.map(item => ({
    name: item.name,
    value: Number(item.value).toFixed(2)
  }))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const item = props.data?.find(d => d.name === params.name)
        return `${params.name}<br/>
          扣分: ${params.value}<br/>
          占比: ${Number(item?.percentage || 0).toFixed(1)}%`
      }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
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
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: pieData
      }
    ],
    color: ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#a0d911', '#722ed1', '#eb2f96']
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
.distribution-pie-chart {
  width: 100%;
  height: 300px;
}
</style>
