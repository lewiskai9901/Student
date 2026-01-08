<template>
  <div ref="chartRef" :style="{ height: height + 'px' }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  AxisPointerComponent
} from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([LineChart, CanvasRenderer, GridComponent, TooltipComponent, LegendComponent, AxisPointerComponent])
import * as echarts from 'echarts/core'
import type { RatingTrendVO } from '@/api/ratingStatistics'

// Props
const props = withDefaults(
  defineProps<{
    trendData: RatingTrendVO[]
    height?: number
  }>(),
  {
    height: 400
  }
)

// 响应式数据
const chartRef = ref<HTMLElement>()
let chartInstance: ECharts | null = null

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option = getChartOption()
  chartInstance.setOption(option)

  // 响应式调整
  window.addEventListener('resize', handleResize)
}

// 获取图表配置
const getChartOption = () => {
  const periods = props.trendData.map((item) => item.periodLabel)

  // 提取所有等级
  const allLevels = new Set<string>()
  props.trendData.forEach((item) => {
    Object.keys(item.levelCounts).forEach((level) => allLevels.add(level))
  })

  // 构建系列数据
  const series = Array.from(allLevels).map((level) => {
    const data = props.trendData.map((item) => item.levelCounts[level] || 0)

    return {
      name: level,
      type: 'line',
      smooth: true,
      data: data,
      emphasis: {
        focus: 'series'
      },
      lineStyle: {
        width: 3
      },
      areaStyle: {
        opacity: 0.2
      }
    }
  })

  // 等级颜色映射
  const levelColors: Record<string, string> = {
    优秀: '#67C23A',
    良好: '#409EFF',
    合格: '#E6A23C',
    待改进: '#F56C6C'
  }

  return {
    color: Array.from(allLevels).map((level) => levelColors[level] || '#909399'),
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      },
      formatter: (params: any) => {
        let result = `<div style="font-weight: 600; margin-bottom: 8px;">${params[0].axisValue}</div>`
        params.forEach((param: any) => {
          result += `
            <div style="display: flex; align-items: center; margin-bottom: 4px;">
              <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${param.color}; margin-right: 8px;"></span>
              <span style="flex: 1;">${param.seriesName}：</span>
              <span style="font-weight: 600;">${param.value} 次</span>
            </div>
          `
        })
        return result
      }
    },
    legend: {
      data: Array.from(allLevels),
      top: 10,
      textStyle: {
        fontSize: 13
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 60,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: periods,
      axisLabel: {
        fontSize: 12,
        color: '#606266'
      },
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '次数',
      nameTextStyle: {
        fontSize: 12,
        color: '#606266'
      },
      axisLabel: {
        fontSize: 12,
        color: '#606266'
      },
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#ebeef5',
          type: 'dashed'
        }
      }
    },
    series: series
  }
}

// 更新图表
const updateChart = () => {
  if (!chartInstance) return

  const option = getChartOption()
  chartInstance.setOption(option, true)
}

// 处理窗口大小变化
const handleResize = () => {
  chartInstance?.resize()
}

// 监听数据变化
watch(
  () => props.trendData,
  () => {
    nextTick(() => {
      updateChart()
    })
  },
  { deep: true }
)

// 生命周期
onMounted(() => {
  nextTick(() => {
    initChart()
  })
})

// 组件卸载时清理
import { onBeforeUnmount } from 'vue'

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped lang="scss">
// 样式在父组件中定义
</style>
