<template>
  <div class="rating-statistics-container">
    <!-- 页面标题和操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2>评级统计中心</h2>
        <p class="subtitle">查看评级频次统计、院系对比和趋势分析</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleRefresh" :loading="refreshing">
          <el-icon><Refresh /></el-icon>
          刷新统计
        </el-button>
        <el-button @click="handleExport" :loading="exporting">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </div>
    </div>

    <!-- 筛选条件卡片 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filterForm" inline>
        <el-form-item label="检查计划">
          <el-select
            v-model="filterForm.checkPlanId"
            placeholder="选择检查计划"
            style="width: 200px"
            @change="loadStatistics"
          >
            <el-option
              v-for="plan in checkPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="评级规则">
          <el-select
            v-model="filterForm.ruleId"
            placeholder="全部规则"
            clearable
            style="width: 180px"
            @change="loadStatistics"
          >
            <el-option
              v-for="rule in ratingRules"
              :key="rule.id"
              :label="rule.ruleName"
              :value="rule.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="统计周期">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
            @change="loadStatistics"
          />
        </el-form-item>

        <el-form-item label="周期类型">
          <el-select
            v-model="filterForm.periodType"
            placeholder="周期类型"
            style="width: 120px"
            @change="loadStatistics"
          >
            <el-option label="周" value="WEEK" />
            <el-option label="月" value="MONTH" />
            <el-option label="季度" value="QUARTER" />
            <el-option label="年" value="YEAR" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计概览卡片 -->
    <el-row :gutter="20" class="overview-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon primary">
              <el-icon :size="32"><TrendCharts /></el-icon>
            </div>
            <div class="metric-info">
              <p class="metric-label">总评级次数</p>
              <p class="metric-value">{{ statistics?.totalRatings || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon success">
              <el-icon :size="32"><Star /></el-icon>
            </div>
            <div class="metric-info">
              <p class="metric-label">获奖班级数</p>
              <p class="metric-value">{{ statistics?.awardedClasses || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon warning">
              <el-icon :size="32"><School /></el-icon>
            </div>
            <div class="metric-info">
              <p class="metric-label">参评班级总数</p>
              <p class="metric-value">{{ statistics?.totalClasses || 0 }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-content">
            <div class="metric-icon danger">
              <el-icon :size="32"><DataAnalysis /></el-icon>
            </div>
            <div class="metric-info">
              <p class="metric-label">整体获奖率</p>
              <p class="metric-value">{{ (statistics?.overallAwardRate || 0).toFixed(1) }}%</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 等级分布和院系对比 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">等级分布统计</span>
            </div>
          </template>
          <div class="level-statistics">
            <div
              v-for="level in statistics?.levelStatistics || []"
              :key="level.levelId"
              class="level-item"
            >
              <div class="level-header">
                <div class="level-name">
                  <span
                    class="level-badge"
                    :style="{ backgroundColor: level.levelColor || '#409EFF' }"
                  >
                    {{ level.levelName }}
                  </span>
                  <span class="level-count">{{ level.classCount }} 个班级</span>
                </div>
                <div class="level-percentage">{{ level.percentage.toFixed(1) }}%</div>
              </div>
              <el-progress
                :percentage="level.percentage"
                :color="level.levelColor || '#409EFF'"
                :stroke-width="12"
              />
              <div class="level-details">
                <span>总获奖次数: {{ level.totalFrequency }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">院系对比分析</span>
            </div>
          </template>
          <el-table
            :data="statistics?.departmentComparison || []"
            style="width: 100%"
            stripe
            max-height="400"
          >
            <el-table-column prop="departmentName" label="院系" min-width="120" />
            <el-table-column prop="totalClasses" label="班级数" width="80" align="center" />
            <el-table-column prop="awardedClasses" label="获奖数" width="80" align="center" />
            <el-table-column prop="totalFrequency" label="总次数" width="80" align="center" />
            <el-table-column label="获奖率" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.awardRate >= 60 ? 'success' : row.awardRate >= 40 ? 'warning' : 'danger'"
                  size="small"
                >
                  {{ row.awardRate.toFixed(1) }}%
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 等级详情展开 -->
    <el-card v-if="selectedLevel" class="level-details-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            {{ selectedLevel.levelName }} - 班级详情
          </span>
          <el-button text @click="selectedLevel = null">
            <el-icon><Close /></el-icon>
            关闭
          </el-button>
        </div>
      </template>
      <el-table :data="selectedLevel.topClasses" stripe>
        <el-table-column type="index" label="排名" width="60" />
        <el-table-column prop="className" label="班级" min-width="120" />
        <el-table-column prop="gradeName" label="年级" width="100" />
        <el-table-column prop="departmentName" label="院系" width="120" />
        <el-table-column prop="frequency" label="获奖次数" width="100" align="center" />
        <el-table-column prop="frequencyRate" label="获奖率" width="100" align="center">
          <template #default="{ row }">
            {{ row.frequencyRate.toFixed(1) }}%
          </template>
        </el-table-column>
        <el-table-column prop="consecutiveCount" label="连续次数" width="100" align="center" />
        <el-table-column prop="bestStreak" label="最佳连续" width="100" align="center" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="viewClassHistory(row.classId)">
              查看历史
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
import { Refresh, Download, TrendCharts, Star, School, DataAnalysis } from '@element-plus/icons-vue'
import {
  getFrequencyStatistics,
  refreshStatistics,
  exportStatistics,
  type RatingFrequencyStatisticsVO,
  type LevelFrequencyVO
} from '@/api/v2/rating'
import { getCheckPlanPage } from '@/api/v2/quantification'
import { getRatingRulesByPlan } from '@/api/v2/rating'

// 响应式数据
const statistics = ref<RatingFrequencyStatisticsVO>()
const checkPlans = ref<any[]>([])
const ratingRules = ref<any[]>([])
const selectedLevel = ref<LevelFrequencyVO | null>(null)

const loading = ref(false)
const refreshing = ref(false)
const exporting = ref(false)

// 筛选表单
const filterForm = reactive({
  checkPlanId: undefined as number | undefined,
  ruleId: undefined as number | undefined,
  dateRange: [] as string[],
  periodType: 'MONTH' as string
})

// 加载检查计划列表
const loadCheckPlans = async () => {
  try {
    const res = await getCheckPlanPage({ pageNum: 1, pageSize: 100 })
    checkPlans.value = res?.data?.records || []
    if (checkPlans.value.length > 0) {
      filterForm.checkPlanId = checkPlans.value[0].id
      await loadRatingRules()
      await loadStatistics()
    }
  } catch {
    ElMessage.error('加载检查计划失败，请检查后端服务')
  }
}

// 加载评级规则
const loadRatingRules = async () => {
  if (!filterForm.checkPlanId) return
  try {
    const res = await getRatingRulesByPlan(filterForm.checkPlanId)
    ratingRules.value = res.data || []
  } catch {
    // 加载评级规则失败
  }
}

// 加载统计数据
const loadStatistics = async () => {
  if (!filterForm.checkPlanId) return

  loading.value = true
  try {
    const params: any = {
      checkPlanId: filterForm.checkPlanId,
      ruleId: filterForm.ruleId,
      periodType: filterForm.periodType
    }

    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.periodStart = filterForm.dateRange[0]
      params.periodEnd = filterForm.dateRange[1]
    }

    const res = await getFrequencyStatistics(params)
    statistics.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新统计
const handleRefresh = async () => {
  if (!filterForm.checkPlanId) {
    ElMessage.warning('请先选择检查计划')
    return
  }

  refreshing.value = true
  try {
    const params: any = {
      checkPlanId: filterForm.checkPlanId,
      ruleId: filterForm.ruleId
    }

    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.periodStart = filterForm.dateRange[0]
      params.periodEnd = filterForm.dateRange[1]
    }

    await refreshStatistics(params)
    ElMessage.success('统计数据刷新成功')
    await loadStatistics()
  } catch (error: any) {
    ElMessage.error(error.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

// 导出报表
const handleExport = async () => {
  if (!filterForm.checkPlanId) {
    ElMessage.warning('请先选择检查计划')
    return
  }

  exporting.value = true
  try {
    const params: any = {
      checkPlanId: filterForm.checkPlanId,
      ruleId: filterForm.ruleId
    }

    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.periodStart = filterForm.dateRange[0]
      params.periodEnd = filterForm.dateRange[1]
    }

    const res = await exportStatistics(params)
    ElMessage.success('报表导出成功')
    // 这里可以触发文件下载
    window.open(res.data, '_blank')
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// 查看班级历史
const viewClassHistory = (classId: number) => {
  // 跳转到班级评级历史详情页
  router.push({
    path: '/quantification/class-rating-history',
    query: {
      classId: classId.toString(),
      checkPlanId: filterForm.checkPlanId?.toString(),
      periodStart: filterForm.dateRange?.[0],
      periodEnd: filterForm.dateRange?.[1]
    }
  })
}

// 初始化
onMounted(() => {
  loadCheckPlans()
})
</script>

<style scoped lang="scss">
.rating-statistics-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .header-left {
    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 500;
      color: #303133;
    }

    .subtitle {
      margin: 5px 0 0 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .header-right {
    display: flex;
    gap: 10px;
  }
}

.filter-card {
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding: 15px 20px;
  }
}

.overview-row {
  margin-bottom: 20px;
}

.metric-card {
  :deep(.el-card__body) {
    padding: 20px;
  }

  .metric-content {
    display: flex;
    align-items: center;
    gap: 15px;

    .metric-icon {
      width: 60px;
      height: 60px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;

      &.primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      &.success {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }

      &.warning {
        background: linear-gradient(135deg, #ffa751 0%, #ffe259 100%);
      }

      &.danger {
        background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
      }
    }

    .metric-info {
      flex: 1;

      .metric-label {
        margin: 0;
        font-size: 14px;
        color: #909399;
      }

      .metric-value {
        margin: 5px 0 0 0;
        font-size: 28px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-size: 16px;
      font-weight: 500;
      color: #303133;
    }
  }

  .level-statistics {
    .level-item {
      margin-bottom: 20px;

      &:last-child {
        margin-bottom: 0;
      }

      .level-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .level-name {
          display: flex;
          align-items: center;
          gap: 10px;

          .level-badge {
            padding: 4px 12px;
            border-radius: 4px;
            color: white;
            font-size: 14px;
            font-weight: 500;
          }

          .level-count {
            font-size: 14px;
            color: #606266;
          }
        }

        .level-percentage {
          font-size: 18px;
          font-weight: 600;
          color: #303133;
        }
      }

      .level-details {
        margin-top: 8px;
        font-size: 13px;
        color: #909399;
      }
    }
  }
}

.level-details-card {
  margin-top: 20px;
}
</style>
