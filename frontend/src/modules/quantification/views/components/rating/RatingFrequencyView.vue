<template>
  <div class="rating-frequency-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header__left">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <div class="page-header__title">
          <h2>{{ planName }}</h2>
          <span class="page-header__subtitle">评级频次统计</span>
        </div>
      </div>
      <div class="page-header__right">
        <el-button :icon="Download" @click="handleExport">导出报表</el-button>
        <el-button type="primary" :icon="Refresh" :loading="calculating" @click="recalculateFrequency">
          重新计算
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-bar__left">
        <el-select
          v-model="periodType"
          placeholder="选择周期类型"
          style="width: 120px"
          @change="handlePeriodTypeChange"
        >
          <el-option
            v-for="(label, value) in PERIOD_TYPE_LABELS"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          :type="periodType === 'WEEK' ? 'week' : periodType === 'MONTH' ? 'month' : 'daterange'"
          :format="getDateFormat()"
          :value-format="periodType === 'WEEK' || periodType === 'MONTH' ? undefined : 'YYYY-MM-DD'"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleDateChange"
        />
        <el-select
          v-model="selectedRuleId"
          placeholder="选择评级规则"
          style="width: 180px"
          clearable
          @change="loadData"
        >
          <el-option
            v-for="rule in ruleOptions"
            :key="rule.id"
            :label="rule.name"
            :value="rule.id"
          />
        </el-select>
      </div>
      <div class="filter-bar__right">
        <span class="filter-bar__tip" v-if="lastUpdateTime">
          数据更新于 {{ lastUpdateTime }}
        </span>
      </div>
    </div>

    <!-- 汇总统计卡片 -->
    <div class="summary-section" v-loading="loadingSummary">
      <div class="summary-card">
        <div class="summary-card__icon primary">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__value">{{ summary?.periodLabel || '-' }}</div>
          <div class="summary-card__label">统计周期</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-card__icon success">
          <el-icon><School /></el-icon>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__value">{{ summary?.totalClasses || 0 }}</div>
          <div class="summary-card__label">参评班级数</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-card__icon warning">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__value">{{ summary?.totalRatings || 0 }}</div>
          <div class="summary-card__label">评级总次数</div>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <div class="content-row">
        <!-- 等级分布饼图 -->
        <div class="content-col">
          <div class="card">
            <div class="card-header">
              <span class="card-title">等级分布</span>
            </div>
            <div class="card-body" v-loading="loadingSummary">
              <div v-if="!summary?.levelDistribution?.length" class="card-empty">
                <el-empty description="暂无数据" :image-size="80" />
              </div>
              <div v-else ref="pieChartRef" class="chart-container"></div>
            </div>
          </div>
        </div>

        <!-- 各等级TOP班级 -->
        <div class="content-col content-col--large">
          <div class="card">
            <div class="card-header">
              <span class="card-title">各等级获得频次TOP班级</span>
            </div>
            <div class="card-body" v-loading="loadingSummary">
              <div v-if="!summary?.levelTopClasses?.length" class="card-empty">
                <el-empty description="暂无数据" :image-size="80" />
              </div>
              <div v-else class="level-top-container">
                <div
                  v-for="levelTop in summary.levelTopClasses"
                  :key="levelTop.levelId"
                  class="level-top-card"
                >
                  <div
                    class="level-top-header"
                    :style="{ backgroundColor: levelTop.levelColor || '#909399' }"
                  >
                    {{ levelTop.levelName }}
                  </div>
                  <div class="level-top-body">
                    <div
                      v-for="(cls, index) in levelTop.topClasses"
                      :key="cls.classId"
                      class="top-class-row"
                    >
                      <span class="rank" :class="getRankClass(index + 1)">{{ index + 1 }}</span>
                      <span class="name" :title="cls.className">{{ cls.className }}</span>
                      <span class="frequency">{{ cls.frequency }}次</span>
                    </div>
                    <div v-if="!levelTop.topClasses?.length" class="no-data">
                      暂无数据
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 评级频次详情表格 -->
      <div class="content-row">
        <div class="content-col--full">
          <div class="card">
            <div class="card-header">
              <span class="card-title">评级频次明细</span>
              <div class="card-header__actions">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索班级"
                  :prefix-icon="Search"
                  clearable
                  style="width: 200px"
                  @input="handleSearch"
                />
              </div>
            </div>
            <div class="card-body">
              <el-table
                :data="filteredFrequencyList"
                v-loading="loadingList"
                stripe
                :max-height="400"
              >
                <el-table-column prop="ranking" label="排名" width="70" align="center">
                  <template #default="{ row }">
                    <span :class="getRankClass(row.ranking)">{{ row.ranking }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="className" label="班级" min-width="120" />
                <el-table-column prop="gradeName" label="年级" width="100" />
                <el-table-column prop="ruleName" label="评级规则" min-width="120" />
                <el-table-column prop="levelName" label="等级" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :color="row.levelColor"
                      :style="{ color: '#fff', border: 'none' }"
                      size="small"
                    >
                      {{ row.levelName }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="frequency" label="获得次数" width="100" align="center">
                  <template #default="{ row }">
                    <span class="frequency-value">{{ row.frequency }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="totalRatings" label="参评次数" width="100" align="center" />
                <el-table-column prop="frequencyRate" label="获得率" width="100" align="center">
                  <template #default="{ row }">
                    <el-progress
                      :percentage="row.frequencyRate"
                      :stroke-width="8"
                      :format="() => `${row.frequencyRate?.toFixed(1)}%`"
                      style="width: 80px"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="showClassHistory(row)">
                      历史
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 班级历史记录抽屉 -->
    <el-drawer
      v-model="showHistoryDrawer"
      :title="`${selectedClass?.className || ''} - 评级频次历史`"
      size="500px"
      direction="rtl"
    >
      <div v-loading="loadingHistory">
        <div v-if="!classHistory.length" class="drawer-empty">
          <el-empty description="暂无历史数据" />
        </div>
        <div v-else class="history-list">
          <div v-for="item in classHistory" :key="item.id" class="history-item">
            <div class="history-item__header">
              <span class="period-label">{{ item.periodLabel }}</span>
              <el-tag
                :color="item.levelColor"
                :style="{ color: '#fff', border: 'none' }"
                size="small"
              >
                {{ item.levelName }}
              </el-tag>
            </div>
            <div class="history-item__body">
              <div class="stat-row">
                <span class="stat-label">获得次数</span>
                <span class="stat-value">{{ item.frequency }}次</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">参评次数</span>
                <span class="stat-value">{{ item.totalRatings }}次</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">获得率</span>
                <span class="stat-value">{{ item.frequencyRate?.toFixed(1) }}%</span>
              </div>
              <div class="stat-row" v-if="item.ranking">
                <span class="stat-label">排名</span>
                <span class="stat-value">#{{ item.ranking }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, Download, Refresh, Calendar, DataAnalysis, Search
} from '@element-plus/icons-vue'
import { School } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
// ECharts 按需导入
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'
import type { ECharts } from 'echarts/core'

use([PieChart, CanvasRenderer, TitleComponent, TooltipComponent, LegendComponent])
import * as echarts from 'echarts/core'
import {
  calculateFrequency,
  calculateCurrentPeriodFrequency,
  queryFrequency,
  getFrequencySummary,
  getClassFrequencyHistory,
  exportFrequency,
  PERIOD_TYPES,
  PERIOD_TYPE_LABELS,
  type RatingFrequencyVO,
  type RatingFrequencySummaryVO
} from '@/api/v2/rating'
import { downloadBlob, getFilenameFromContentDisposition } from '@/utils/export'
import { getRatingRulesByPlanId, type RatingRuleVO } from '@/api/v2/rating'

const route = useRoute()
const router = useRouter()

// Props
const props = defineProps<{
  checkPlanId?: string | number
  checkPlanName?: string
}>()

// 计划信息
const planId = computed(() => props.checkPlanId || route.params.planId as string)
const planName = computed(() => props.checkPlanName || route.query.planName as string || '检查计划')

// 筛选条件
const periodType = ref<string>(PERIOD_TYPES.MONTH)
const dateRange = ref<Date | [string, string] | null>(null)
const selectedRuleId = ref<number | string | null>(null)
const searchKeyword = ref('')

// 数据
const summary = ref<RatingFrequencySummaryVO | null>(null)
const frequencyList = ref<RatingFrequencyVO[]>([])
const ruleOptions = ref<RatingRuleVO[]>([])
const classHistory = ref<RatingFrequencyVO[]>([])
const selectedClass = ref<RatingFrequencyVO | null>(null)

// 加载状态
const loadingSummary = ref(false)
const loadingList = ref(false)
const loadingHistory = ref(false)
const calculating = ref(false)

// UI状态
const showHistoryDrawer = ref(false)
const lastUpdateTime = ref('')

// 图表
const pieChartRef = ref<HTMLElement>()
let pieChart: ECharts | null = null

// 计算属性
const filteredFrequencyList = computed(() => {
  if (!searchKeyword.value) return frequencyList.value
  const keyword = searchKeyword.value.toLowerCase()
  return frequencyList.value.filter(item =>
    item.className?.toLowerCase().includes(keyword) ||
    item.gradeName?.toLowerCase().includes(keyword)
  )
})

// 获取日期选择器格式
const getDateFormat = () => {
  switch (periodType.value) {
    case 'WEEK': return 'YYYY 第 ww 周'
    case 'MONTH': return 'YYYY-MM'
    default: return 'YYYY-MM-DD'
  }
}

// 获取周期日期范围
const getPeriodDates = (): { start: string, end: string } | null => {
  if (!dateRange.value) return null

  const formatDate = (date: Date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  if (periodType.value === 'WEEK') {
    // 周选择器返回的是周的某一天
    const date = dateRange.value as Date
    const dayOfWeek = date.getDay()
    const diff = date.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1)
    const monday = new Date(date.setDate(diff))
    const sunday = new Date(monday)
    sunday.setDate(monday.getDate() + 6)
    return {
      start: formatDate(monday),
      end: formatDate(sunday)
    }
  } else if (periodType.value === 'MONTH') {
    const date = dateRange.value as Date
    const firstDay = new Date(date.getFullYear(), date.getMonth(), 1)
    const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0)
    return {
      start: formatDate(firstDay),
      end: formatDate(lastDay)
    }
  } else {
    const range = dateRange.value as [string, string]
    return {
      start: range[0],
      end: range[1]
    }
  }
}

// 加载评级规则
const loadRules = async () => {
  try {
    const res = await getRatingRulesByPlanId(planId.value)
    ruleOptions.value = res || []
  } catch (e) {
    console.error('加载评级规则失败:', e)
  }
}

// 加载汇总数据
const loadSummary = async () => {
  const dates = getPeriodDates()
  if (!dates) return

  loadingSummary.value = true
  try {
    const res = await getFrequencySummary(
      planId.value,
      periodType.value,
      dates.start,
      dates.end,
      selectedRuleId.value || undefined
    )
    summary.value = res
    await nextTick()
    updatePieChart()
  } catch (e) {
    console.error('加载汇总数据失败:', e)
  } finally {
    loadingSummary.value = false
  }
}

// 加载频次列表
const loadFrequencyList = async () => {
  const dates = getPeriodDates()
  if (!dates) return

  loadingList.value = true
  try {
    const res = await queryFrequency({
      checkPlanId: planId.value,
      periodType: periodType.value,
      periodStart: dates.start,
      periodEnd: dates.end,
      ruleId: selectedRuleId.value || undefined,
      sortBy: 'frequency',
      sortOrder: 'desc'
    })
    frequencyList.value = res || []
  } catch (e) {
    console.error('加载频次列表失败:', e)
  } finally {
    loadingList.value = false
  }
}

// 加载所有数据
const loadData = async () => {
  await Promise.all([loadSummary(), loadFrequencyList()])
  lastUpdateTime.value = new Date().toLocaleTimeString()
}

// 重新计算频次
const recalculateFrequency = async () => {
  const dates = getPeriodDates()
  if (!dates) {
    ElMessage.warning('请先选择统计周期')
    return
  }

  calculating.value = true
  try {
    await calculateFrequency(planId.value, periodType.value, dates.start, dates.end)
    ElMessage.success('计算完成')
    await loadData()
  } catch (e) {
    console.error('计算频次失败:', e)
    ElMessage.error('计算失败')
  } finally {
    calculating.value = false
  }
}

// 显示班级历史
const showClassHistory = async (item: RatingFrequencyVO) => {
  selectedClass.value = item
  showHistoryDrawer.value = true

  loadingHistory.value = true
  try {
    const res = await getClassFrequencyHistory(
      item.classId,
      item.ruleId,
      periodType.value,
      12
    )
    classHistory.value = res || []
  } catch (e) {
    console.error('加载历史数据失败:', e)
  } finally {
    loadingHistory.value = false
  }
}

// 事件处理
const handlePeriodTypeChange = () => {
  dateRange.value = null
}

const handleDateChange = () => {
  if (dateRange.value) {
    loadData()
  }
}

const handleSearch = () => {
  // 搜索由 computed 处理
}

const handleExport = async () => {
  const periodDates = getPeriodDates()
  if (!periodDates) {
    ElMessage.warning('请先选择周期')
    return
  }

  try {
    ElMessage.info('正在导出...')
    const response = await exportFrequency({
      checkPlanId: planId.value,
      ruleId: selectedRuleId.value || undefined,
      periodType: periodType.value,
      periodStart: periodDates.start,
      periodEnd: periodDates.end,
      format: 'EXCEL'
    })

    // 处理blob响应
    let blob: Blob
    let filename = `评级频次统计_${new Date().toISOString().slice(0, 10)}.xlsx`

    if (response && (response as any).data instanceof Blob) {
      blob = (response as any).data
      const contentDisposition = (response as any).headers?.['content-disposition']
      if (contentDisposition) {
        filename = getFilenameFromContentDisposition(contentDisposition)
      }
    } else if (response instanceof Blob) {
      blob = response
    } else {
      throw new Error('导出响应格式错误')
    }

    downloadBlob(blob, filename)
    ElMessage.success('导出成功')
  } catch (error: any) {
    console.error('导出失败:', error)
    ElMessage.error(error.message || '导出失败')
  }
}

const goBack = () => {
  router.back()
}

// 获取排名样式类
const getRankClass = (rank: number) => {
  if (rank === 1) return 'rank-gold'
  if (rank === 2) return 'rank-silver'
  if (rank === 3) return 'rank-bronze'
  return ''
}

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value) return
  if (pieChartRef.value.clientWidth === 0 || pieChartRef.value.clientHeight === 0) {
    setTimeout(initPieChart, 100)
    return
  }
  pieChart = echarts.init(pieChartRef.value)
  window.addEventListener('resize', handleResize)
}

const handleResize = () => {
  pieChart?.resize()
}

// 更新饼图
const updatePieChart = () => {
  if (!pieChart || !summary.value?.levelDistribution?.length) return

  const pieData = summary.value.levelDistribution.map(item => ({
    name: item.levelName,
    value: item.totalFrequency,
    itemStyle: {
      color: item.levelColor || '#909399'
    },
    classCount: item.classCount,
    percentage: item.percentage
  }))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const data = params.data
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 8px;">${data.name}</div>
            <div>获得班级数: ${data.classCount}个</div>
            <div>总次数: ${data.value}次</div>
            <div>占比: ${data.percentage?.toFixed(1)}%</div>
          </div>
        `
      }
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '等级分布',
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
              return `${params.name}\n${params.data.percentage?.toFixed(1)}%`
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

  pieChart.setOption(option)
}

// 监听 summary 变化
watch(() => summary.value?.levelDistribution, () => {
  nextTick(() => {
    if (!pieChart && pieChartRef.value) {
      initPieChart()
    }
    updatePieChart()
  })
}, { deep: true })

// 初始化默认日期范围（当前月）
const initDefaultDateRange = () => {
  const now = new Date()
  dateRange.value = now
}

onMounted(async () => {
  await loadRules()
  initDefaultDateRange()
  await loadData()
  nextTick(() => {
    initPieChart()
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
})
</script>

<style scoped lang="scss">
.rating-frequency-view {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &__left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__title {
    h2 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
  }

  &__subtitle {
    font-size: 14px;
    color: #909399;
    margin-left: 8px;
  }

  &__right {
    display: flex;
    gap: 8px;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__tip {
    font-size: 12px;
    color: #909399;
  }
}

.summary-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &__icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #fff;

    &.primary { background: linear-gradient(135deg, #409eff, #66b1ff); }
    &.success { background: linear-gradient(135deg, #67c23a, #95d475); }
    &.warning { background: linear-gradient(135deg, #e6a23c, #eebe77); }
  }

  &__content {
    flex: 1;
  }

  &__value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }

  &__label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
  }
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-row {
  display: flex;
  gap: 20px;

  @media (max-width: 1200px) {
    flex-direction: column;
  }
}

.content-col {
  flex: 1;

  &--large {
    flex: 1.5;
  }

  &--full {
    width: 100%;
  }
}

.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f2f5;

  &__actions {
    display: flex;
    gap: 8px;
  }
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-body {
  padding: 16px 20px;
  min-height: 200px;
}

.card-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.level-top-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.level-top-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.level-top-header {
  padding: 10px 12px;
  color: #fff;
  font-weight: 600;
  text-align: center;
}

.level-top-body {
  padding: 12px;
}

.top-class-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f2f5;

  &:last-child {
    border-bottom: none;
  }
}

.rank {
  width: 20px;
  font-weight: 600;
  text-align: center;
}

.rank-gold { color: #e6a23c; }
.rank-silver { color: #909399; }
.rank-bronze { color: #b87333; }

.name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.frequency {
  font-size: 13px;
  color: #67c23a;
  font-weight: 500;
}

.no-data {
  text-align: center;
  color: #909399;
  padding: 20px;
  font-size: 13px;
}

.frequency-value {
  font-weight: 600;
  color: #67c23a;
}

.drawer-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #fafafa;
    border-bottom: 1px solid #e4e7ed;
  }

  &__body {
    padding: 12px 16px;
  }
}

.period-label {
  font-weight: 500;
  color: #303133;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;

  &:not(:last-child) {
    border-bottom: 1px solid #f0f2f5;
  }
}

.stat-label {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  color: #303133;
  font-weight: 500;
  font-size: 13px;
}
</style>
