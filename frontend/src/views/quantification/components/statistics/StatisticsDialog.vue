<template>
  <el-dialog
    v-model="dialogVisible"
    title="统计分析"
    width="90%"
    top="5vh"
    :close-on-click-modal="false"
    destroy-on-close
    class="statistics-dialog"
  >
    <!-- 筛选器 -->
    <div class="filters-section">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="班级">
          <el-select
            v-model="filters.classIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部班级"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="cls in classList"
              :key="cls.classId"
              :label="cls.className"
              :value="cls.classId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filters.sortBy" style="width: 120px">
            <el-option label="总扣分" value="totalScore" />
            <el-option label="平均扣分" value="avgScore" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refreshData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计概览 -->
    <div class="overview-section" v-loading="loading.overview">
      <div class="overview-cards">
        <div class="overview-card">
          <div class="card-value">{{ overview?.totalChecks || 0 }}</div>
          <div class="card-label">检查次数</div>
        </div>
        <div class="overview-card">
          <div class="card-value">{{ overview?.totalClasses || 0 }}</div>
          <div class="card-label">涉及班级</div>
        </div>
        <div class="overview-card">
          <div class="card-value">{{ formatScore(overview?.totalScore) }}</div>
          <div class="card-label">总扣分</div>
        </div>
        <div class="overview-card">
          <div class="card-value">{{ formatScore(overview?.avgScore) }}</div>
          <div class="card-label">平均扣分</div>
        </div>
        <div class="overview-card">
          <div class="card-value">{{ overview?.totalItems || 0 }}</div>
          <div class="card-label">扣分项次</div>
        </div>
        <div class="overview-card">
          <div class="card-value">{{ overview?.totalPersons || 0 }}</div>
          <div class="card-label">涉及人次</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="chart-card" v-loading="loading.ranking">
            <div class="chart-title">班级扣分排名TOP10</div>
            <ClassRankingChart :data="classRanking" />
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-card" v-loading="loading.category">
            <div class="chart-title">检查类别分布</div>
            <CategoryPieChart :data="categoryStats" />
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="24">
          <div class="chart-card" v-loading="loading.trend">
            <div class="chart-title">扣分趋势</div>
            <TrendLineChart :data="trendData" />
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 表格区域 -->
    <div class="tables-section">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="班级排名" name="ranking">
          <ClassRankingTable :data="classRanking" :loading="loading.ranking" />
        </el-tab-pane>
        <el-tab-pane label="扣分项统计" name="items">
          <ItemStatisticsTable :data="itemStats" :loading="loading.items" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleExport" :loading="exporting">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { Refresh, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ClassRankingChart from './ClassRankingChart.vue'
import CategoryPieChart from './CategoryPieChart.vue'
import TrendLineChart from './TrendLineChart.vue'
import ClassRankingTable from './ClassRankingTable.vue'
import ItemStatisticsTable from './ItemStatisticsTable.vue'
import {
  getStatisticsOverview,
  getClassRanking,
  getCategoryStatistics,
  getItemStatistics,
  getTrendData,
  exportStatistics,
  type PlanStatisticsOverview,
  type ClassRanking,
  type CategoryStatistics,
  type ItemStatistics,
  type TrendData
} from '@/api/v2/quantification-extra'

const props = defineProps<{
  planId: string | number
}>()

const dialogVisible = defineModel<boolean>('visible', { default: false })

const loading = reactive({
  overview: false,
  ranking: false,
  category: false,
  items: false,
  trend: false
})

const exporting = ref(false)
const activeTab = ref('ranking')

// 数据
const overview = ref<PlanStatisticsOverview | null>(null)
const classRanking = ref<ClassRanking[]>([])
const categoryStats = ref<CategoryStatistics[]>([])
const itemStats = ref<ItemStatistics[]>([])
const trendData = ref<TrendData | null>(null)

// 班级列表（从排名数据中提取）
const classList = computed(() => {
  return classRanking.value.map(c => ({
    classId: c.classId,
    className: c.className
  }))
})

// 筛选条件
const dateRange = ref<string[]>([])
const filters = reactive({
  classIds: [] as number[],
  sortBy: 'totalScore' as 'totalScore' | 'avgScore'
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
    text: '最近一月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '最近三月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    }
  }
]

// 格式化分数
function formatScore(score?: number): string {
  if (score === undefined || score === null) return '0'
  return score.toFixed(1)
}

// 构建请求参数
function buildParams() {
  const params: Record<string, any> = {
    sortBy: filters.sortBy,
    sortOrder: 'asc'
  }

  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }

  if (filters.classIds.length > 0) {
    params.classIds = filters.classIds
  }

  return params
}

// 加载概览数据
async function loadOverview() {
  loading.overview = true
  try {
    const res = await getStatisticsOverview(props.planId, buildParams())
    // request.ts 拦截器已解包数据，res 就是实际数据
    overview.value = res as unknown as PlanStatisticsOverview
  } catch {
    // 加载失败
  } finally {
    loading.overview = false
  }
}

// 加载班级排名
async function loadClassRanking() {
  loading.ranking = true
  try {
    const res = await getClassRanking(props.planId, {
      ...buildParams(),
      topN: 20
    })
    classRanking.value = (res as unknown as ClassRanking[]) || []
  } catch {
    // 加载失败
  } finally {
    loading.ranking = false
  }
}

// 加载类别统计
async function loadCategoryStats() {
  loading.category = true
  try {
    const res = await getCategoryStatistics(props.planId, buildParams())
    categoryStats.value = (res as unknown as CategoryStatistics[]) || []
  } catch {
    // 加载失败
  } finally {
    loading.category = false
  }
}

// 加载扣分项统计
async function loadItemStats() {
  loading.items = true
  try {
    const res = await getItemStatistics(props.planId, {
      ...buildParams(),
      topN: 50
    })
    itemStats.value = (res as unknown as ItemStatistics[]) || []
  } catch {
    // 加载失败
  } finally {
    loading.items = false
  }
}

// 加载趋势数据
async function loadTrendData() {
  loading.trend = true
  try {
    const res = await getTrendData(props.planId, buildParams())
    trendData.value = res as unknown as TrendData
  } catch {
    // 加载失败
  } finally {
    loading.trend = false
  }
}

// 刷新所有数据
function refreshData() {
  loadOverview()
  loadClassRanking()
  loadCategoryStats()
  loadItemStats()
  loadTrendData()
}

// 导出
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    await exportStatistics(props.planId, {
      planId: props.planId,
      startDate: params.startDate,
      endDate: params.endDate,
      classIds: filters.classIds.length > 0 ? filters.classIds : undefined,
      sortBy: filters.sortBy,
      sortOrder: 'asc'
    })
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// 监听弹窗打开
watch(dialogVisible, (val) => {
  if (val) {
    refreshData()
  }
})
</script>

<style scoped lang="scss">
.statistics-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
    max-height: calc(90vh - 120px);
    overflow-y: auto;
  }
}

.filters-section {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;

  .filter-form {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;

    .el-form-item {
      margin-bottom: 0;
    }
  }
}

.overview-section {
  margin-bottom: 20px;

  .overview-cards {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 16px;
  }

  .overview-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 20px;
    border-radius: 12px;
    text-align: center;
    color: white;

    &:nth-child(2) {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    }

    &:nth-child(3) {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }

    &:nth-child(4) {
      background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    }

    &:nth-child(5) {
      background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
    }

    &:nth-child(6) {
      background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
      color: #333;
    }

    .card-value {
      font-size: 28px;
      font-weight: bold;
      margin-bottom: 8px;
    }

    .card-label {
      font-size: 14px;
      opacity: 0.9;
    }
  }
}

.charts-section {
  margin-bottom: 20px;

  .chart-card {
    background: white;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 16px;

    .chart-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #ebeef5;
    }
  }
}

.tables-section {
  background: white;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
