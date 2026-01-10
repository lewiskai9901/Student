<template>
  <div class="smart-statistics">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header__left">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <div class="page-header__title">
          <h2>{{ planName }}</h2>
          <span class="page-header__subtitle">统计分析</span>
        </div>
      </div>
      <div class="page-header__right">
        <el-button :icon="Download" @click="handleExport">导出报表</el-button>
        <el-button :icon="Setting" @click="showSettings = true">设置</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-bar__left">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
          @change="handleFilterChange"
        />
        <el-select
          v-model="selectedGrades"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择年级"
          style="width: 180px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="grade in gradeOptions"
            :key="grade.gradeId"
            :label="grade.gradeName"
            :value="grade.gradeId"
          />
        </el-select>
      </div>
      <div class="filter-bar__right">
        <span class="filter-bar__tip" v-if="lastUpdateTime">
          数据更新于 {{ lastUpdateTime }}
        </span>
        <el-button :icon="Refresh" :loading="refreshing" @click="refreshData">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 概览指标卡片 -->
    <div class="metrics-section">
      <MetricCard
        label="检查次数"
        :value="overview?.totalChecks || 0"
        suffix="次"
        type="primary"
        :trend="overview?.trend ? {
          direction: overview.trend.direction as any,
          value: overview.trend.percentage,
          text: `${overview.trend.direction === 'down' ? '↓' : overview.trend.direction === 'up' ? '↑' : ''} ${Math.abs(overview.trend.percentage || 0).toFixed(1)}%`
        } : undefined"
        :loading="loadingOverview"
        tooltip="统计周期内的检查记录总数"
      />
      <MetricCard
        label="班级覆盖率"
        :value="overview?.coverage?.coverageRate || 0"
        suffix="%"
        type="success"
        :show-progress="true"
        :progress-value="overview?.coverage?.coverageRate || 0"
        :progress-max="100"
        :loading="loadingOverview"
        tooltip="实际检查班级数 / 计划目标班级数"
        :description="`${overview?.coverage?.actualCheckedClasses || 0} / ${overview?.coverage?.planTargetClasses || 0} 班级`"
      />
      <MetricCard
        label="总扣分"
        :value="overview?.totalScore || 0"
        suffix="分"
        type="warning"
        :loading="loadingOverview"
        tooltip="所有检查记录的扣分总和"
      />
      <MetricCard
        label="平均扣分"
        :value="overview?.avgScorePerRound || 0"
        suffix="分/轮"
        type="info"
        :loading="loadingOverview"
        tooltip="每轮检查的平均扣分，用于不同轮次间的公平对比"
        :description="`共${overview?.totalRounds || 0}轮检查`"
      />
    </div>

    <!-- 警告和洞察 -->
    <div class="alerts-section" v-if="(overview?.warnings?.length || 0) > 0 || (overview?.insights?.length || 0) > 0">
      <el-alert
        v-for="(warning, index) in overview?.warnings || []"
        :key="'w' + index"
        :title="warning"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-alert
        v-for="(insight, index) in overview?.insights || []"
        :key="'i' + index"
        :title="insight"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <div class="content-row">
        <!-- 班级排名卡片 -->
        <div class="content-col content-col--large">
          <RankingCard
            title="班级排名"
            :rankings="rankings"
            :loading="loadingRanking"
            :compare-mode="compareMode"
            :warnings="rankingWarnings"
            :limit="10"
            @more="showRankingDrawer = true"
            @mode-change="handleCompareModeChange"
            @item-click="handleClassClick"
          />
        </div>

        <!-- 类别分布卡片 -->
        <div class="content-col">
          <CategoryDistributionCard
            title="扣分类别分布"
            :categories="categoryStats"
            :loading="loadingCategory"
            :top-category="dynamicCategoryData?.topCategory"
            :top-category-percentage="dynamicCategoryData?.topCategoryPercentage"
            @more="showCategoryDrawer = true"
          />
        </div>
      </div>

      <div class="content-row">
        <!-- 轮次分析卡片 -->
        <div class="content-col">
          <RoundAnalysisCard
            title="轮次分析"
            :max-rounds="roundAnalysis?.maxRounds || 0"
            :round-comparison="roundAnalysis?.roundComparison || []"
            :overall-improvement-rate="roundAnalysis?.overallImprovementRate || 0"
            :improved-classes="roundAnalysis?.improvedClasses || 0"
            :worsened-classes="roundAnalysis?.worsenedClasses || 0"
            :stable-classes="roundAnalysis?.stableClasses || 0"
            :insights="roundAnalysis?.insights || []"
            :loading="loadingRound"
            @more="showRoundDrawer = true"
          />
        </div>

        <!-- 高频问题卡片 -->
        <div class="content-col">
          <div class="frequent-items-card">
            <div class="card-header">
              <span class="card-title">高频扣分项 TOP 5</span>
              <el-button link type="primary" @click="showItemsDrawer = true">
                更多 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
            <div class="card-body" v-loading="loadingCategory">
              <div v-if="topItems.length === 0" class="card-empty">
                <el-empty description="暂无数据" :image-size="60" />
              </div>
              <div v-else class="items-list">
                <div v-for="(item, index) in topItems" :key="item.itemId" class="item-row">
                  <span class="item-rank">{{ index + 1 }}.</span>
                  <span class="item-name">{{ item.itemName }}</span>
                  <div class="item-bar-container">
                    <div
                      class="item-bar"
                      :style="{ width: getItemBarWidth(item.triggerCount) + '%' }"
                    ></div>
                  </div>
                  <span class="item-count">{{ item.triggerCount }}次</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作栏 -->
    <div class="quick-actions">
      <el-button type="primary" :icon="DataAnalysis" @click="showRankingDrawer = true">
        查看完整排名
      </el-button>
      <el-button :icon="User" @click="showClassTrackingDialog = true">
        班级追踪
      </el-button>
      <el-button :icon="Download" @click="handleExport">
        导出Excel
      </el-button>
    </div>

    <!-- 排名详情抽屉 -->
    <el-drawer
      v-model="showRankingDrawer"
      title="班级排名详情"
      size="60%"
      direction="rtl"
    >
      <RankingCard
        :rankings="fullRankings"
        :loading="loadingFullRanking"
        :compare-mode="compareMode"
        :warnings="rankingWarnings"
        :limit="100"
        :show-more="false"
        @mode-change="handleCompareModeChange"
        @item-click="handleClassClick"
      />
    </el-drawer>

    <!-- 班级追踪对话框 -->
    <el-dialog
      v-model="showClassTrackingDialog"
      title="班级追踪"
      width="600px"
    >
      <el-select
        v-model="trackingClassId"
        placeholder="选择要追踪的班级"
        filterable
        style="width: 100%"
      >
        <el-option
          v-for="cls in classOptions"
          :key="cls.classId"
          :label="cls.className"
          :value="cls.classId"
        />
      </el-select>
      <div v-if="classTracking" class="tracking-result">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="班级">{{ classTracking.className }}</el-descriptions-item>
          <el-descriptions-item label="年级">{{ classTracking.gradeName }}</el-descriptions-item>
          <el-descriptions-item label="班主任">{{ classTracking.teacherName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="被检查次数">{{ classTracking.totalChecks }}次</el-descriptions-item>
          <el-descriptions-item label="总扣分">{{ classTracking.totalScore?.toFixed(1) }}分</el-descriptions-item>
          <el-descriptions-item label="平均扣分">{{ classTracking.avgScorePerRound?.toFixed(2) }}分/轮</el-descriptions-item>
          <el-descriptions-item label="全校排名">第{{ classTracking.ranking }}名</el-descriptions-item>
          <el-descriptions-item label="年级排名">第{{ classTracking.gradeRanking }}名</el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag :type="getLevelType(classTracking.scoreLevel)">{{ classTracking.scoreLevel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="与平均差距">
            <span :style="{ color: (classTracking.vsOverallAvg || 0) > 0 ? '#f56c6c' : '#67c23a' }">
              {{ (classTracking.vsOverallAvg || 0) > 0 ? '+' : '' }}{{ classTracking.vsOverallAvg?.toFixed(2) }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="showClassTrackingDialog = false">关闭</el-button>
        <el-button type="primary" @click="loadClassTracking" :loading="loadingTracking">查询</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, Download, Setting, Refresh, ArrowRight,
  DataAnalysis, User
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MetricCard from './components/statistics/MetricCard.vue'
import RankingCard from './components/statistics/RankingCard.vue'
import CategoryDistributionCard from './components/statistics/CategoryDistributionCard.vue'
import RoundAnalysisCard from './components/statistics/RoundAnalysisCard.vue'
import {
  getSmartOverview,
  getSmartRanking,
  getDynamicCategoryStats,
  getRoundAnalysis,
  getClassTracking,
  type SmartStatisticsOverviewVO,
  type SmartClassRankingVO,
  type DynamicCategoryStatsVO,
  type RoundAnalysisVO,
  type ClassTrackingVO,
  type TopItemVO
} from '@/api/v2/quantification-extra'
import { exportClassRanking } from '@/api/v2/rating'
import { downloadBlob, getFilenameFromContentDisposition } from '@/utils/export'

const route = useRoute()
const router = useRouter()

// 计划信息
const planId = computed(() => route.params.id as string)
const planName = ref('检查计划')

// 筛选条件
const dateRange = ref<[string, string] | null>(null)
const selectedGrades = ref<number[]>([])
const compareMode = ref('average')

// 数据状态
const overview = ref<SmartStatisticsOverviewVO | null>(null)
const rankings = ref<SmartClassRankingVO[]>([])
const fullRankings = ref<SmartClassRankingVO[]>([])
const dynamicCategoryData = ref<DynamicCategoryStatsVO | null>(null)
const roundAnalysis = ref<RoundAnalysisVO | null>(null)
const classTracking = ref<ClassTrackingVO | null>(null)

// 加载状态
const loadingOverview = ref(false)
const loadingRanking = ref(false)
const loadingFullRanking = ref(false)
const loadingCategory = ref(false)
const loadingRound = ref(false)
const loadingTracking = ref(false)
const refreshing = ref(false)

// UI状态
const showSettings = ref(false)
const showRankingDrawer = ref(false)
const showCategoryDrawer = ref(false)
const showRoundDrawer = ref(false)
const showItemsDrawer = ref(false)
const showClassTrackingDialog = ref(false)
const trackingClassId = ref<number | string | null>(null)
const lastUpdateTime = ref('')

// 选项
const gradeOptions = computed(() => {
  return dynamicCategoryData.value?.detectedCategories?.map(c => ({
    gradeId: c.categoryId,
    gradeName: c.categoryName
  })) || []
})

const classOptions = computed(() => {
  return rankings.value.map(r => ({
    classId: r.classId,
    className: `${r.className} (${r.gradeName})`
  }))
})

const categoryStats = computed(() => {
  return dynamicCategoryData.value?.categoryStats || []
})

const topItems = computed(() => {
  const items: TopItemVO[] = []
  categoryStats.value.forEach(cat => {
    cat.topItems?.forEach(item => {
      items.push(item)
    })
  })
  return items.sort((a, b) => b.triggerCount - a.triggerCount).slice(0, 5)
})

const rankingWarnings = computed(() => {
  return overview.value?.warnings || []
})

const maxTriggerCount = computed(() => {
  return Math.max(...topItems.value.map(i => i.triggerCount), 1)
})

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '本学期',
    value: () => {
      const now = new Date()
      const month = now.getMonth()
      const year = now.getFullYear()
      let start: Date
      if (month >= 8) {
        start = new Date(year, 8, 1)
      } else if (month >= 1) {
        start = new Date(year, 1, 1)
      } else {
        start = new Date(year - 1, 8, 1)
      }
      return [start, now]
    }
  }
]

// 方法
const goBack = () => {
  router.back()
}

const handleFilterChange = () => {
  loadAllData()
}

const handleCompareModeChange = (mode: string) => {
  compareMode.value = mode
  loadRankingData()
}

const handleClassClick = (item: SmartClassRankingVO) => {
  trackingClassId.value = item.classId
  showClassTrackingDialog.value = true
  loadClassTracking()
}

const handleExport = async () => {
  try {
    ElMessage.info('正在导出...')
    const response = await exportClassRanking({
      checkPlanId: planId.value,
      periodStart: dateRange.value?.[0],
      periodEnd: dateRange.value?.[1],
      gradeIds: selectedGrades.value.length > 0 ? selectedGrades.value : undefined,
      format: 'EXCEL'
    })

    // 处理blob响应
    let blob: Blob
    let filename = `班级排名_${new Date().toISOString().slice(0, 10)}.xlsx`

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

const getItemBarWidth = (count: number) => {
  return (count / maxTriggerCount.value) * 100
}

const getLevelType = (level?: string) => {
  switch (level) {
    case '优秀': return 'success'
    case '良好': return ''
    case '中等': return 'warning'
    case '较差': return 'danger'
    default: return 'info'
  }
}

const getFilterParams = () => {
  const params: any = {}
  if (dateRange.value) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  if (selectedGrades.value.length > 0) {
    params.gradeIds = selectedGrades.value
  }
  return params
}

// 数据加载
const loadOverview = async () => {
  loadingOverview.value = true
  try {
    const res = await getSmartOverview(planId.value, getFilterParams())
    overview.value = res
    if (res?.planName) {
      planName.value = res.planName
    }
  } catch (e) {
    console.error('加载概览失败:', e)
  } finally {
    loadingOverview.value = false
  }
}

const loadRankingData = async () => {
  loadingRanking.value = true
  try {
    const res = await getSmartRanking(planId.value, {
      ...getFilterParams(),
      compareMode: compareMode.value,
      pageSize: 10
    })
    rankings.value = res?.rankings || []
  } catch (e) {
    console.error('加载排名失败:', e)
  } finally {
    loadingRanking.value = false
  }
}

const loadFullRanking = async () => {
  loadingFullRanking.value = true
  try {
    const res = await getSmartRanking(planId.value, {
      ...getFilterParams(),
      compareMode: compareMode.value,
      pageSize: 200
    })
    fullRankings.value = res?.rankings || []
  } catch (e) {
    console.error('加载完整排名失败:', e)
  } finally {
    loadingFullRanking.value = false
  }
}

const loadCategoryData = async () => {
  loadingCategory.value = true
  try {
    const res = await getDynamicCategoryStats(planId.value, getFilterParams())
    dynamicCategoryData.value = res
  } catch (e) {
    console.error('加载类别统计失败:', e)
  } finally {
    loadingCategory.value = false
  }
}

const loadRoundData = async () => {
  loadingRound.value = true
  try {
    const res = await getRoundAnalysis(planId.value, getFilterParams())
    roundAnalysis.value = res
  } catch (e) {
    console.error('加载轮次分析失败:', e)
  } finally {
    loadingRound.value = false
  }
}

const loadClassTracking = async () => {
  if (!trackingClassId.value) return

  loadingTracking.value = true
  try {
    const res = await getClassTracking(planId.value, trackingClassId.value)
    classTracking.value = res
  } catch (e) {
    console.error('加载班级追踪失败:', e)
  } finally {
    loadingTracking.value = false
  }
}

const loadAllData = async () => {
  await Promise.all([
    loadOverview(),
    loadRankingData(),
    loadCategoryData(),
    loadRoundData()
  ])
  lastUpdateTime.value = new Date().toLocaleTimeString()
}

const refreshData = async () => {
  refreshing.value = true
  await loadAllData()
  refreshing.value = false
  ElMessage.success('数据已刷新')
}

// 监听抽屉打开
watch(showRankingDrawer, (val) => {
  if (val && fullRankings.value.length === 0) {
    loadFullRanking()
  }
})

onMounted(() => {
  loadAllData()
})
</script>

<style scoped lang="scss">
.smart-statistics {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
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

.metrics-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.alerts-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
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
}

.frequent-items-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid #f0f2f5;
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

  .items-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .item-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .item-rank {
    width: 24px;
    font-size: 14px;
    font-weight: 500;
    color: #606266;
  }

  .item-name {
    width: 120px;
    font-size: 14px;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .item-bar-container {
    flex: 1;
    height: 12px;
    background: #f0f2f5;
    border-radius: 6px;
    overflow: hidden;
  }

  .item-bar {
    height: 100%;
    background: linear-gradient(90deg, #f56c6c, #fab6b6);
    border-radius: 6px;
    transition: width 0.3s ease;
  }

  .item-count {
    width: 50px;
    text-align: right;
    font-size: 13px;
    color: #909399;
  }
}

.quick-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.tracking-result {
  margin-top: 20px;
}
</style>
