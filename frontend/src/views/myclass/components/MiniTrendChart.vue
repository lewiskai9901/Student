<template>
  <div ref="chartRef" class="mini-trend-chart"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, CanvasRenderer])

const props = defineProps<{
  data: number[]
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chart || !props.data?.length) return

  chart.setOption({
    grid: {
      top: 5,
      right: 5,
      bottom: 5,
      left: 5
    },
    xAxis: {
      type: 'category',
      show: false,
      data: props.data.map((_, i) => i)
    },
    yAxis: {
      type: 'value',
      show: false,
      min: (value: { min: number }) => value.min * 0.95,
      max: (value: { max: number }) => value.max * 1.05
    },
    series: [{
      type: 'line',
      data: props.data,
      smooth: true,
      symbol: 'none',
      lineStyle: {
        color: '#409EFF',
        width: 2
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      }
    }]
  })
}

watch(() => props.data, updateChart, { deep: true })

onMounted(initChart)

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped>
.mini-trend-chart {
  width: 100%;
  height: 100%;
}
</style>
