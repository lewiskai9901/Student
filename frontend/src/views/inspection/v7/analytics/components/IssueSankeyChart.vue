<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getIssueFlow } from '@/api/insp/alert'
import type { SankeyData } from '@/types/insp/alert'

const props = defineProps<{
  projectId: number
  dateFrom: string
  dateTo: string
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
const loading = ref(false)

// Color palette consistent with app theme
const nodeColors: Record<string, string> = {
  // Source layer (blue tones)
  default_source: '#409EFF',
  // Category layer (warm tones)
  default_category: '#E6A23C',
  // Status layer (semantic)
  OPEN: '#909399',
  ASSIGNED: '#409EFF',
  IN_PROGRESS: '#E6A23C',
  SUBMITTED: '#409EFF',
  VERIFIED: '#67C23A',
  REJECTED: '#F56C6C',
  CLOSED: '#67C23A',
  ESCALATED: '#F56C6C',
  RESOLVED: '#67C23A',
  DISMISSED: '#909399',
  ACKNOWLEDGED: '#E6A23C',
}

function getNodeColor(name: string, index: number, total: number): string {
  // Check if the node name matches a known status
  const upperName = name.toUpperCase().replace(/\s+/g, '_')
  if (nodeColors[upperName]) return nodeColors[upperName]

  // Use position-based coloring
  const palette = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#B37FEB', '#36CFC9', '#FF7A45']
  return palette[index % palette.length]
}

async function loadAndRender() {
  if (!props.projectId || !props.dateFrom || !props.dateTo) return
  loading.value = true
  try {
    const data: SankeyData = await getIssueFlow({
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

function renderChart(data: SankeyData) {
  if (!chart) return
  if (!data.nodes.length || !data.links.length) {
    chart.clear()
    return
  }

  const nodes = data.nodes.map((node, idx) => ({
    name: node.name,
    itemStyle: { color: getNodeColor(node.name, idx, data.nodes.length) },
  }))

  chart.setOption(
    {
      tooltip: {
        trigger: 'item',
        triggerOn: 'mousemove',
      },
      series: [
        {
          type: 'sankey',
          layout: 'none',
          emphasis: { focus: 'adjacency' },
          nodeAlign: 'justify',
          data: nodes,
          links: data.links,
          lineStyle: {
            color: 'gradient',
            curveness: 0.5,
            opacity: 0.4,
          },
          label: {
            fontSize: 12,
            color: '#333',
          },
          nodeWidth: 20,
          nodeGap: 12,
          left: 40,
          right: 120,
          top: 20,
          bottom: 20,
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
  <div v-loading="loading" class="sankey-wrapper">
    <div ref="chartRef" class="sankey-chart" />
  </div>
</template>

<style scoped>
.sankey-wrapper {
  width: 100%;
  min-height: 300px;
}
.sankey-chart {
  width: 100%;
  height: 400px;
}
</style>
