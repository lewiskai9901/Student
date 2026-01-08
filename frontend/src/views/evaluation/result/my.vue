<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 周期选择 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex items-center gap-4">
        <label class="text-sm font-medium text-gray-700">综测周期</label>
        <select v-model="selectedPeriodId" class="h-9 w-64 rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" @change="loadResult">
          <option v-for="item in periodList" :key="item.id" :value="item.id">{{ item.periodName }}</option>
        </select>
      </div>
    </div>

    <!-- 无数据提示 -->
    <div v-if="!resultData" class="rounded-lg border border-gray-200 bg-white p-12 text-center">
      <div class="text-gray-400">暂无综测数据</div>
    </div>

    <!-- 综测结果展示 -->
    <template v-else>
      <!-- 总分卡片 -->
      <div class="mb-6 grid grid-cols-4 gap-4">
        <div class="rounded-lg border border-gray-200 bg-white p-5 text-center">
          <div class="text-3xl font-bold text-gray-900">{{ formatScore(resultData.totalScore) }}</div>
          <div class="mt-1 text-sm text-gray-500">综测总分</div>
          <div class="mt-2 text-xs text-gray-400">班级排名: {{ resultData.classRank }} | 年级排名: {{ resultData.gradeRank }}</div>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 text-center">
          <div class="text-3xl font-bold text-green-600">{{ formatScore(resultData.moralScore) }}</div>
          <div class="mt-1 text-sm text-gray-500">德育 (25%)</div>
          <div class="mt-2 text-xs text-gray-400">加权: {{ formatScore(resultData.moralWeightedScore) }}</div>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 text-center">
          <div class="text-3xl font-bold text-blue-600">{{ formatScore(resultData.intellectualScore) }}</div>
          <div class="mt-1 text-sm text-gray-500">智育 (40%)</div>
          <div class="mt-2 text-xs text-gray-400">加权: {{ formatScore(resultData.intellectualWeightedScore) }}</div>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 text-center">
          <div class="text-3xl font-bold text-amber-600">{{ formatScore(resultData.physicalScore) }}</div>
          <div class="mt-1 text-sm text-gray-500">体育 (10%)</div>
          <div class="mt-2 text-xs text-gray-400">加权: {{ formatScore(resultData.physicalWeightedScore) }}</div>
        </div>
      </div>

      <div class="mb-6 grid grid-cols-3 gap-4">
        <div class="rounded-lg border border-gray-200 bg-white p-5 text-center">
          <div class="text-3xl font-bold text-red-600">{{ formatScore(resultData.aestheticScore) }}</div>
          <div class="mt-1 text-sm text-gray-500">美育 (10%)</div>
          <div class="mt-2 text-xs text-gray-400">加权: {{ formatScore(resultData.aestheticWeightedScore) }}</div>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 text-center">
          <div class="text-3xl font-bold text-gray-600">{{ formatScore(resultData.laborScore) }}</div>
          <div class="mt-1 text-sm text-gray-500">劳育 (10%)</div>
          <div class="mt-2 text-xs text-gray-400">加权: {{ formatScore(resultData.laborWeightedScore) }}</div>
        </div>
        <div class="rounded-lg border border-gray-200 bg-white p-5 text-center">
          <div class="text-3xl font-bold text-purple-600">{{ formatScore(resultData.developmentScore) }}</div>
          <div class="mt-1 text-sm text-gray-500">发展素质 (5%)</div>
          <div class="mt-2 text-xs text-gray-400">加权: {{ formatScore(resultData.developmentWeightedScore) }}</div>
        </div>
      </div>

      <!-- 雷达图 -->
      <div class="mb-6 rounded-lg border border-gray-200 bg-white p-6">
        <h3 class="mb-4 font-medium text-gray-900">六维度分数分布</h3>
        <div ref="radarChartRef" class="h-80 w-full"></div>
      </div>

      <!-- 详细明细 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="font-medium text-gray-900">分数明细</h3>
        </div>
        <div class="p-4">
          <!-- Tab 切换 -->
          <div class="mb-4 flex gap-1 rounded-lg bg-gray-100 p-1">
            <button v-for="(tab, key) in tabs" :key="key" :class="['flex-1 rounded-md px-4 py-2 text-sm', activeTab === key ? 'bg-white font-medium text-gray-900 shadow-sm' : 'text-gray-600']" @click="activeTab = key">{{ tab }}</button>
          </div>

          <!-- 德育明细 -->
          <div v-if="activeTab === 'moral'">
            <div class="mb-4 grid grid-cols-3 gap-4 rounded-lg bg-gray-50 p-4">
              <div><span class="text-sm text-gray-500">基础分：</span><span class="font-medium">60</span></div>
              <div><span class="text-sm text-gray-500">奖励分：</span><span class="font-medium text-green-600">+{{ formatScore(resultData.moralBonusScore) }}</span></div>
              <div><span class="text-sm text-gray-500">扣分：</span><span class="font-medium text-red-600">-{{ formatScore(resultData.moralDeductScore) }}</span></div>
            </div>
            <table class="w-full">
              <thead class="bg-gray-50"><tr><th class="px-4 py-2 text-left text-sm font-medium text-gray-700">项目</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">类型</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">分数</th><th class="px-4 py-2 text-left text-sm font-medium text-gray-700">说明</th></tr></thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="(item, idx) in resultData.moralDetails || []" :key="idx" class="hover:bg-gray-50">
                  <td class="px-4 py-2 text-sm text-gray-900">{{ item.itemName }}</td>
                  <td class="px-4 py-2 text-center"><span :class="['inline-flex rounded-full px-2 py-0.5 text-xs', item.scoreType === 'bonus' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700']">{{ item.scoreType === 'bonus' ? '加分' : '扣分' }}</span></td>
                  <td class="px-4 py-2 text-center text-sm">{{ item.score }}</td>
                  <td class="px-4 py-2 text-sm text-gray-500">{{ item.remark || '-' }}</td>
                </tr>
                <tr v-if="!resultData.moralDetails?.length"><td colspan="4" class="px-4 py-8 text-center text-sm text-gray-500">暂无明细数据</td></tr>
              </tbody>
            </table>
          </div>

          <!-- 智育明细 -->
          <div v-if="activeTab === 'intellectual'">
            <div class="mb-4 grid grid-cols-2 gap-4 rounded-lg bg-gray-50 p-4">
              <div><span class="text-sm text-gray-500">学业成绩：</span><span class="font-medium">{{ formatScore(resultData.academicScore) }}</span></div>
              <div><span class="text-sm text-gray-500">学术荣誉加分：</span><span class="font-medium text-green-600">+{{ formatScore(resultData.intellectualBonusScore) }}</span></div>
            </div>
            <table class="w-full">
              <thead class="bg-gray-50"><tr><th class="px-4 py-2 text-left text-sm font-medium text-gray-700">课程</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">成绩</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">学分</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">绩点</th></tr></thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="(item, idx) in resultData.intellectualDetails || []" :key="idx" class="hover:bg-gray-50">
                  <td class="px-4 py-2 text-sm text-gray-900">{{ item.courseName }}</td>
                  <td class="px-4 py-2 text-center text-sm">{{ item.score }}</td>
                  <td class="px-4 py-2 text-center text-sm">{{ item.credit }}</td>
                  <td class="px-4 py-2 text-center text-sm">{{ item.gpa }}</td>
                </tr>
                <tr v-if="!resultData.intellectualDetails?.length"><td colspan="4" class="px-4 py-8 text-center text-sm text-gray-500">暂无成绩数据</td></tr>
              </tbody>
            </table>
          </div>

          <!-- 体育明细 -->
          <div v-if="activeTab === 'physical'">
            <div class="mb-4 grid grid-cols-3 gap-4 rounded-lg bg-gray-50 p-4">
              <div><span class="text-sm text-gray-500">基础分：</span><span class="font-medium">60</span></div>
              <div><span class="text-sm text-gray-500">奖励分：</span><span class="font-medium text-green-600">+{{ formatScore(resultData.physicalBonusScore) }}</span></div>
              <div><span class="text-sm text-gray-500">扣分：</span><span class="font-medium text-red-600">-{{ formatScore(resultData.physicalDeductScore) }}</span></div>
            </div>
            <table class="w-full">
              <thead class="bg-gray-50"><tr><th class="px-4 py-2 text-left text-sm font-medium text-gray-700">项目</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">类型</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">分数</th><th class="px-4 py-2 text-left text-sm font-medium text-gray-700">说明</th></tr></thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="(item, idx) in resultData.physicalDetails || []" :key="idx" class="hover:bg-gray-50">
                  <td class="px-4 py-2 text-sm text-gray-900">{{ item.itemName }}</td>
                  <td class="px-4 py-2 text-center text-sm">{{ item.scoreType }}</td>
                  <td class="px-4 py-2 text-center text-sm">{{ item.score }}</td>
                  <td class="px-4 py-2 text-sm text-gray-500">{{ item.remark || '-' }}</td>
                </tr>
                <tr v-if="!resultData.physicalDetails?.length"><td colspan="4" class="px-4 py-8 text-center text-sm text-gray-500">暂无明细数据</td></tr>
              </tbody>
            </table>
          </div>

          <!-- 其他维度 -->
          <div v-if="activeTab === 'other'">
            <div class="space-y-6">
              <div>
                <h4 class="mb-2 text-sm font-medium text-gray-700">美育明细</h4>
                <table class="w-full"><thead class="bg-gray-50"><tr><th class="px-4 py-2 text-left text-sm font-medium text-gray-700">项目</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">分数</th></tr></thead>
                  <tbody class="divide-y divide-gray-200">
                    <tr v-for="(item, idx) in resultData.aestheticDetails || []" :key="idx" class="hover:bg-gray-50"><td class="px-4 py-2 text-sm text-gray-900">{{ item.itemName }}</td><td class="px-4 py-2 text-center text-sm">{{ item.score }}</td></tr>
                    <tr v-if="!resultData.aestheticDetails?.length"><td colspan="2" class="px-4 py-4 text-center text-sm text-gray-500">暂无数据</td></tr>
                  </tbody>
                </table>
              </div>
              <div>
                <h4 class="mb-2 text-sm font-medium text-gray-700">劳育明细</h4>
                <table class="w-full"><thead class="bg-gray-50"><tr><th class="px-4 py-2 text-left text-sm font-medium text-gray-700">项目</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">分数</th></tr></thead>
                  <tbody class="divide-y divide-gray-200">
                    <tr v-for="(item, idx) in resultData.laborDetails || []" :key="idx" class="hover:bg-gray-50"><td class="px-4 py-2 text-sm text-gray-900">{{ item.itemName }}</td><td class="px-4 py-2 text-center text-sm">{{ item.score }}</td></tr>
                    <tr v-if="!resultData.laborDetails?.length"><td colspan="2" class="px-4 py-4 text-center text-sm text-gray-500">暂无数据</td></tr>
                  </tbody>
                </table>
              </div>
              <div>
                <h4 class="mb-2 text-sm font-medium text-gray-700">发展素质明细</h4>
                <table class="w-full"><thead class="bg-gray-50"><tr><th class="px-4 py-2 text-left text-sm font-medium text-gray-700">项目</th><th class="px-4 py-2 text-center text-sm font-medium text-gray-700">分数</th></tr></thead>
                  <tbody class="divide-y divide-gray-200">
                    <tr v-for="(item, idx) in resultData.developmentDetails || []" :key="idx" class="hover:bg-gray-50"><td class="px-4 py-2 text-sm text-gray-900">{{ item.itemName }}</td><td class="px-4 py-2 text-center text-sm">{{ item.score }}</td></tr>
                    <tr v-if="!resultData.developmentDetails?.length"><td colspan="2" class="px-4 py-4 text-center text-sm text-gray-500">暂无数据</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, onUnmounted } from 'vue'
// ECharts 按需导入
import { use } from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { RadarComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([RadarChart, CanvasRenderer, RadarComponent, TooltipComponent, LegendComponent])
import * as echarts from 'echarts/core'
import { getMyEvaluationResult, type EvaluationPeriod } from '@/api/evaluation'

const selectedPeriodId = ref<number>()
const periodList = ref<EvaluationPeriod[]>([])
const resultData = ref<any>(null)
const radarChartRef = ref<HTMLElement>()
const activeTab = ref<'moral' | 'intellectual' | 'physical' | 'other'>('moral')
const tabs = { moral: '德育明细', intellectual: '智育明细', physical: '体育明细', other: '美育/劳育/发展' }
let radarChart: ECharts | null = null

const formatScore = (score: number | undefined) => (score === undefined || score === null) ? '-' : score.toFixed(2)

const loadResult = async () => {
  if (!selectedPeriodId.value) return
  try {
    const res = await getMyEvaluationResult(selectedPeriodId.value)
    resultData.value = res.data
    nextTick(() => initRadarChart())
  } catch { resultData.value = null }
}

const initRadarChart = () => {
  if (!radarChartRef.value || !resultData.value) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)
  radarChart.setOption({
    radar: { indicator: [{ name: '德育', max: 100 }, { name: '智育', max: 100 }, { name: '体育', max: 100 }, { name: '美育', max: 100 }, { name: '劳育', max: 100 }, { name: '发展素质', max: 100 }], shape: 'polygon' },
    series: [{ type: 'radar', data: [{ value: [resultData.value.moralScore || 0, resultData.value.intellectualScore || 0, resultData.value.physicalScore || 0, resultData.value.aestheticScore || 0, resultData.value.laborScore || 0, resultData.value.developmentScore || 0], name: '我的综测', areaStyle: { color: 'rgba(59, 130, 246, 0.3)' }, lineStyle: { color: '#3b82f6' } }] }]
  })
}

const handleResize = () => radarChart?.resize()

onMounted(() => {
  periodList.value = [{ id: 1, periodCode: '2024-2025-1', periodName: '2024-2025学年第一学期', semesterId: 1 }]
  if (periodList.value.length > 0) { selectedPeriodId.value = periodList.value[0].id; loadResult() }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => { window.removeEventListener('resize', handleResize); radarChart?.dispose() })
</script>
