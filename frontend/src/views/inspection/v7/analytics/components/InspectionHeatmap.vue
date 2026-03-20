<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getHeatmapData } from '@/api/insp/alert'
import type { HeatmapDataPoint } from '@/types/insp/alert'

const props = defineProps<{
  projectId: number
  dateFrom: string
  dateTo: string
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
const loading = ref(false)

async function loadAndRender() {
  if (!props.projectId || !props.dateFrom || !props.dateTo) return
  loading.value = true
  try {
    const data: HeatmapDataPoint[] = await getHeatmapData({
      projectId: props.projectId,
      dateFrom: props.dateFrom,
      dateTo: props.dateTo,
    })
    renderChart(data)
  } catch {
    // silent
  } finally {
    loading.value = false
  }
}

function renderChart(data: HeatmapDataPoint[]) {
  if (!chart) return
  if (!data.length) {
    chart.clear()
    return
  }

  // Extract unique dates and target names
  const dates = [...new Set(data.map(d => d.date))].sort()
  const targets = [...new Set(data.map(d => d.targetName))]

  // Build heatmap data: [xIndex, yIndex, value]
  const heatmapData = data.map(d => {
    const xIdx = dates.indexOf(d.date)
    const yIdx = targets.indexOf(d.targetName)
    return [xIdx, yIdx, d.score]
  })

  // Determine score range for visual map
  const scores = data.map(d => d.score)
  const minScore = Math.min(...scores)
  const maxScore = Math.max(...scores)

  chart.setOption(
    {
      tooltip: {
        position: 'top',
        formatter: (params: any) => {
          const [xIdx, yIdx, val] = params.data
          return `${targets[yIdx]}<br/>${dates[xIdx]}<br/>得分: <b>${val}</b>`
        },
      },
      grid: {
        top: 10,
        right: 80,
        bottom: 60,
        left: 120,
      },
      xAxis: {
        type: 'category',
        data: dates.map(d => d.slice(5)), // MM-DD
        splitArea: { show: true },
        axisLabel: { fontSize: 11, rotate: 45 },
      },
      yAxis: {
        type: 'category',
        data: targets,
        splitArea: { show: true },
        axisLabel: { fontSize: 11, width: 100, overflow: 'truncate' },
      },
      visualMap: {
        min: minScore,
        max: maxScore,
        calculable: true,
        orient: 'vertical',
        right: 0,
        top: 'center',
        itemHeight: 120,
        inRange: {
          color: ['#F56C6C', '#E6A23C', '#67C23A'], // red=low, yellow=mid, green=high
        },
        textStyle: { fontSize: 11 },
      },
      series: [
        {
          type: 'heatmap',
          data: heatmapData,
          label: { show: false },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0,0,0,0.2)',
            },
          },
        },
      ],
    },
    true,
  )
}

function handleResize() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
  }
  window.addEventListener('resize', handleResize)
  loadAndRender()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})

watch(
  () => [props.projectId, props.dateFrom, props.dateTo],
  () => loadAndRender(),
)
</script>

<template>
  <div v-loading="loading" class="heatmap-wrapper">
    <div ref="chartRef" class="heatmap-chart" />
  </div>
</template>

<style scoped>
.heatmap-wrapper {
  width: 100%;
  min-height: 300px;
}
.heatmap-chart {
  width: 100%;
  height: 400px;
}
</style>
