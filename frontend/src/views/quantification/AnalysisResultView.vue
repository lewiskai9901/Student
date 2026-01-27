<template>
  <div class="analysis-result-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-page-header @back="goBack">
          <template #content>
            <span class="page-title">{{ result?.configName || '分析结果' }}</span>
          </template>
        </el-page-header>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button @click="refreshAnalysis" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新分析
          </el-button>
          <el-button @click="saveSnapshot" :loading="savingSnapshot">
            <el-icon><DocumentAdd /></el-icon>
            保存快照
          </el-button>
          <el-button @click="exportReport">
            <el-icon><Download /></el-icon>
            导出报告
          </el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <el-empty description="加载失败">
        <el-button type="primary" @click="loadAnalysis">重新加载</el-button>
      </el-empty>
    </div>

    <!-- 分析结果 -->
    <div v-else-if="result" class="result-content">
      <!-- 概览信息 -->
      <el-card class="overview-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>统计概览</span>
            <span class="meta-info">
              数据范围: {{ result.dateRangeStart || '-' }} ~ {{ result.dateRangeEnd || '至今' }}
              | 生成时间: {{ formatDateTime(result.generatedAt) }}
              <el-tag v-if="result.isDynamic" type="success" size="small" class="ml-10">动态更新</el-tag>
            </span>
          </div>
        </template>

        <div class="overview-stats">
          <div class="stat-item">
            <div class="stat-value">{{ result.overview?.recordCount || 0 }}</div>
            <div class="stat-label">检查次数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ result.overview?.classCount || 0 }}</div>
            <div class="stat-label">参与班级</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ formatNumber(result.overview?.totalScore) }}</div>
            <div class="stat-label">总扣分</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ formatNumber(result.overview?.avgScore) }}</div>
            <div class="stat-label">平均扣分</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ formatNumber(result.overview?.maxScore) }}</div>
            <div class="stat-label">最高扣分</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ formatNumber(result.overview?.minScore) }}</div>
            <div class="stat-label">最低扣分</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ result.overview?.totalItems || 0 }}</div>
            <div class="stat-label">扣分项数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ formatPercent(result.overview?.avgCoverageRate) }}</div>
            <div class="stat-label">平均覆盖率</div>
          </div>
        </div>
      </el-card>

      <!-- 指标结果列表 -->
      <div class="metrics-grid">
        <template v-for="metric in result.metricResults" :key="metric.metricId">
          <!-- 班级排名表格 -->
          <el-card v-if="metric.chartType === 'table'" class="metric-card wide-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>{{ metric.metricName }}</span>
              </div>
            </template>
            <ClassRankingTable :data="metric.data" />
          </el-card>

          <!-- 趋势折线图 -->
          <el-card v-else-if="metric.chartType === 'line'" class="metric-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>{{ metric.metricName }}</span>
              </div>
            </template>
            <TrendChart :data="metric.data" />
          </el-card>

          <!-- 分布饼图 -->
          <el-card v-else-if="metric.chartType === 'pie'" class="metric-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>{{ metric.metricName }}</span>
              </div>
            </template>
            <DistributionPieChart :data="metric.data" />
          </el-card>

          <!-- 柱状图 -->
          <el-card v-else-if="metric.chartType === 'bar'" class="metric-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>{{ metric.metricName }}</span>
              </div>
            </template>
            <ComparisonBarChart :data="metric.data" />
          </el-card>

          <!-- 数字卡片 -->
          <el-card v-else-if="metric.chartType === 'number'" class="metric-card number-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span>{{ metric.metricName }}</span>
              </div>
            </template>
            <div class="number-value">
              {{ formatMetricValue(metric.data) }}
            </div>
          </el-card>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, DocumentAdd, Download } from '@element-plus/icons-vue'
import { executeAnalysis, getSnapshotDetail, type AnalysisResult, type MetricResult } from '@/api/quantification-extra'
import { formatDateTime } from '@/utils/date'
import ClassRankingTable from './components/analysis/ClassRankingTable.vue'
import TrendChart from './components/analysis/TrendChart.vue'
import DistributionPieChart from './components/analysis/DistributionPieChart.vue'
import ComparisonBarChart from './components/analysis/ComparisonBarChart.vue'

const route = useRoute()
const router = useRouter()

// 状态
const loading = ref(false)
const error = ref(false)
const savingSnapshot = ref(false)
const result = ref<AnalysisResult | null>(null)

// 获取配置ID或快照ID
const configId = ref<number | null>(null)
const snapshotId = ref<number | null>(null)

// 加载分析结果
async function loadAnalysis() {
  loading.value = true
  error.value = false

  try {
    if (snapshotId.value) {
      // 加载快照
      const res = await getSnapshotDetail(snapshotId.value)
      result.value = res.data
    } else if (configId.value) {
      // 执行分析
      const res = await executeAnalysis(configId.value, false)
      result.value = res.data
    }
  } catch (err: any) {
    error.value = true
    ElMessage.error(err.message || '加载分析结果失败')
  } finally {
    loading.value = false
  }
}

// 刷新分析
async function refreshAnalysis() {
  if (!configId.value) {
    ElMessage.warning('快照模式无法刷新，请返回配置页面重新分析')
    return
  }

  loading.value = true
  try {
    const res = await executeAnalysis(configId.value, false)
    result.value = res.data
    ElMessage.success('分析完成')
  } catch (err: any) {
    ElMessage.error(err.message || '分析失败')
  } finally {
    loading.value = false
  }
}

// 保存快照
async function saveSnapshot() {
  if (!configId.value) {
    ElMessage.warning('快照模式无法保存新快照')
    return
  }

  savingSnapshot.value = true
  try {
    await executeAnalysis(configId.value, true)
    ElMessage.success('快照保存成功')
  } catch (err: any) {
    ElMessage.error(err.message || '保存快照失败')
  } finally {
    savingSnapshot.value = false
  }
}

// 导出报告
function exportReport() {
  if (!result.value) {
    ElMessage.warning('无数据可导出')
    return
  }

  try {
    // 构建CSV数据
    const rows: string[][] = []

    // 添加标题
    rows.push(['分析报告: ' + result.value.configName])
    rows.push(['生成时间: ' + formatDateTime(result.value.generatedAt)])
    rows.push(['数据范围: ' + (result.value.dateRangeStart || '-') + ' ~ ' + (result.value.dateRangeEnd || '至今')])
    rows.push([])

    // 添加概览数据
    rows.push(['统计概览'])
    rows.push(['检查次数', String(result.value.overview.recordCount)])
    rows.push(['班级数量', String(result.value.overview.classCount)])
    rows.push(['总扣分', formatNumber(result.value.overview.totalScore)])
    rows.push(['平均扣分', formatNumber(result.value.overview.avgScore)])
    rows.push(['最高扣分', formatNumber(result.value.overview.maxScore)])
    rows.push(['最低扣分', formatNumber(result.value.overview.minScore)])
    rows.push(['扣分项数', String(result.value.overview.totalItems)])
    if (result.value.overview.totalPersons) {
      rows.push(['涉及人数', String(result.value.overview.totalPersons)])
    }
    rows.push([])

    // 添加各指标结果
    for (const metric of result.value.metricResults) {
      rows.push([metric.metricName])

      if (metric.value !== undefined && metric.value !== null) {
        rows.push(['值', String(metric.value)])
      }

      // 如果有班级排名数据
      if (metric.rankings && metric.rankings.length > 0) {
        rows.push(['排名', '班级', '年级', '得分', '变化'])
        for (const rank of metric.rankings) {
          rows.push([
            String(rank.ranking),
            rank.className,
            rank.gradeName || '',
            formatNumber(rank.score),
            rank.change !== undefined ? formatNumber(rank.change) : ''
          ])
        }
      }

      // 如果有分布数据
      if (metric.distribution && metric.distribution.length > 0) {
        rows.push(['类别', '数量', '占比'])
        for (const item of metric.distribution) {
          rows.push([
            item.label,
            String(item.value),
            formatPercent(item.percentage) + '%'
          ])
        }
      }

      rows.push([])
    }

    // 转换为CSV
    const csvContent = rows.map(row =>
      row.map(cell => `"${(cell || '').replace(/"/g, '""')}"`).join(',')
    ).join('\n')

    // 添加BOM以支持中文
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8' })

    // 下载
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `分析报告_${result.value.configName}_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 返回
function goBack() {
  router.back()
}

// 格式化数字
function formatNumber(value: any): string {
  if (value === null || value === undefined) return '-'
  const num = Number(value)
  if (isNaN(num)) return '-'
  return num.toFixed(2)
}

// 格式化百分比
function formatPercent(value: any): string {
  if (value === null || value === undefined) return '-'
  const num = Number(value)
  if (isNaN(num)) return '-'
  return (num * 100).toFixed(1) + '%'
}

// 格式化指标值
function formatMetricValue(data: any): string {
  if (data === null || data === undefined) return '-'
  if (typeof data === 'object') {
    return JSON.stringify(data)
  }
  return String(data)
}

// 初始化
onMounted(() => {
  // 从路由获取参数
  if (route.params.configId) {
    configId.value = Number(route.params.configId)
  }
  if (route.params.snapshotId) {
    snapshotId.value = Number(route.params.snapshotId)
  }

  loadAnalysis()
})
</script>

<style scoped lang="scss">
.analysis-result-view {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .loading-container,
  .error-container {
    padding: 100px 0;
  }

  .overview-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .meta-info {
        font-size: 12px;
        color: #909399;

        .ml-10 {
          margin-left: 10px;
        }
      }
    }

    .overview-stats {
      display: grid;
      grid-template-columns: repeat(8, 1fr);
      gap: 20px;

      @media (max-width: 1400px) {
        grid-template-columns: repeat(4, 1fr);
      }

      @media (max-width: 768px) {
        grid-template-columns: repeat(2, 1fr);
      }

      .stat-item {
        text-align: center;
        padding: 15px;
        background: #f5f7fa;
        border-radius: 8px;

        .stat-value {
          font-size: 24px;
          font-weight: 600;
          color: #409eff;
        }

        .stat-label {
          margin-top: 8px;
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }

  .metrics-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;

    @media (max-width: 1200px) {
      grid-template-columns: 1fr;
    }

    .metric-card {
      min-height: 350px;

      &.wide-card {
        grid-column: 1 / -1;
      }

      &.number-card {
        min-height: 150px;

        .number-value {
          font-size: 48px;
          font-weight: 600;
          color: #409eff;
          text-align: center;
          padding: 40px 0;
        }
      }

      .card-header {
        font-weight: 600;
      }
    }
  }
}
</style>
