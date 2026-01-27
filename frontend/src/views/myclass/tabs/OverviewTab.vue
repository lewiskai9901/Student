<template>
  <div class="overview-tab">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else>
      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon student-icon">
            <Users :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overview?.studentCount ?? '-' }}</div>
            <div class="stat-label">学生人数</div>
            <div v-if="overview" class="stat-extra">
              男 {{ overview.maleCount }} / 女 {{ overview.femaleCount }}
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon rank-icon">
            <Trophy :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              <span v-if="overview?.classRank">{{ overview.classRank }}</span>
              <span v-else>-</span>
              <span v-if="overview?.totalClasses" class="stat-suffix">
                / {{ overview.totalClasses }}
              </span>
            </div>
            <div class="stat-label">班级排名</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon score-icon">
            <Star :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-value">
              {{ overview?.averageScore?.toFixed(1) ?? '-' }}
              <span class="stat-unit">分</span>
            </div>
            <div class="stat-label">平均分</div>
            <div v-if="overview?.scoreTrend !== undefined" class="stat-extra" :class="trendClass">
              <TrendingUp v-if="overview.scoreTrend > 0" :size="14" />
              <TrendingDown v-else-if="overview.scoreTrend < 0" :size="14" />
              <Minus v-else :size="14" />
              {{ Math.abs(overview.scoreTrend).toFixed(1) }}%
            </div>
          </div>
        </div>

        <div class="stat-card clickable" @click="goToAppeals">
          <div class="stat-icon appeal-icon">
            <AlertCircle :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overview?.pendingAppeals ?? 0 }}</div>
            <div class="stat-label">待处理申诉</div>
            <div class="stat-extra">点击查看</div>
          </div>
        </div>
      </div>

      <!-- 趋势图区域 -->
      <div class="trend-section">
        <div class="section-header">
          <h3>成绩趋势</h3>
          <span class="section-subtitle">近期班级量化成绩走势</span>
        </div>
        <div class="trend-chart-container">
          <TrendChart
            v-if="overview?.scoreTrendList && overview.scoreTrendList.length > 0"
            :data="overview.scoreTrendList"
          />
          <div v-else class="trend-chart-empty">
            <BarChart3 :size="48" class="placeholder-icon" />
            <p>暂无趋势数据</p>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="quick-actions-section">
        <div class="section-header">
          <h3>快捷操作</h3>
        </div>
        <div class="quick-actions-grid">
          <div class="quick-action-item" @click="handleQuickAction('students')">
            <div class="action-icon">
              <Users :size="24" />
            </div>
            <span class="action-label">查看学生</span>
          </div>
          <div class="quick-action-item" @click="handleExport">
            <div class="action-icon">
              <Download :size="24" />
            </div>
            <span class="action-label">导出名单</span>
          </div>
          <div class="quick-action-item" @click="handleQuickAction('dormitory')">
            <div class="action-icon">
              <Building :size="24" />
            </div>
            <span class="action-label">宿舍分布</span>
          </div>
          <div class="quick-action-item" @click="handleQuickAction('analytics')">
            <div class="action-icon">
              <BarChart3 :size="24" />
            </div>
            <span class="action-label">数据分析</span>
          </div>
        </div>
      </div>

      <!-- 最近检查记录 -->
      <div class="recent-records-section">
        <div class="section-header">
          <h3>最近检查记录</h3>
          <el-button type="primary" link @click="handleViewAllRecords">
            查看全部
            <ChevronRight :size="16" />
          </el-button>
        </div>
        <el-table
          :data="overview?.recentRecords || []"
          stripe
          class="records-table"
          empty-text="暂无检查记录"
        >
          <el-table-column prop="checkDate" label="检查日期" width="120">
            <template #default="{ row }">
              {{ formatDate(row.checkDate) }}
            </template>
          </el-table-column>
          <el-table-column prop="checkType" label="检查类型" width="120" />
          <el-table-column prop="score" label="得分" width="100">
            <template #default="{ row }">
              <span :class="getScoreClass(row.score)">{{ row.score.toFixed(1) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="rank" label="排名" width="100">
            <template #default="{ row }">
              <span class="rank-badge" :class="getRankClass(row.rank)">
                {{ row.rank }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleViewRecord(row)">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Users,
  Trophy,
  Star,
  AlertCircle,
  TrendingUp,
  TrendingDown,
  Minus,
  Download,
  Building,
  BarChart3,
  ChevronRight
} from 'lucide-vue-next'
import { getClassOverview } from '@/api/v2/myClass'
import type { MyClassOverview, RecentCheckRecord } from '@/types/v2/myClass'
import TrendChart from '../components/TrendChart.vue'

const props = defineProps<{
  classId: string | number
}>()

const emit = defineEmits<{
  (e: 'change-tab', tab: string): void
}>()

const router = useRouter()
const loading = ref(false)
const overview = ref<MyClassOverview | null>(null)

// 加载概览数据
const loadOverview = async () => {
  if (!props.classId) return

  loading.value = true
  try {
    overview.value = await getClassOverview(props.classId)
  } catch (error: any) {
    console.error('加载概览数据失败:', error)
    ElMessage.error(error.response?.data?.message || '加载概览数据失败')
  } finally {
    loading.value = false
  }
}

// 趋势样式
const trendClass = computed(() => {
  if (!overview.value?.scoreTrend) return ''
  if (overview.value.scoreTrend > 0) return 'trend-up'
  if (overview.value.scoreTrend < 0) return 'trend-down'
  return 'trend-flat'
})

// 格式化日期
const formatDate = (dateStr: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 分数样式
const getScoreClass = (score: number): string => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

// 排名样式
const getRankClass = (rank: number): string => {
  if (rank === 1) return 'rank-first'
  if (rank === 2) return 'rank-second'
  if (rank === 3) return 'rank-third'
  return ''
}

// 快捷操作
const handleQuickAction = (tab: string) => {
  emit('change-tab', tab)
}

// 导出名单
const handleExport = () => {
  ElMessage.info('导出功能开发中...')
}

// 查看全部记录
const handleViewAllRecords = () => {
  router.push('/inspection/check-records')
}

// 查看记录详情
const handleViewRecord = (record: RecentCheckRecord) => {
  router.push(`/inspection/check-record/${record.id}`)
}

// 跳转到申诉管理
const goToAppeals = () => {
  router.push('/inspection/appeals')
}

// 监听 classId 变化
watch(() => props.classId, () => {
  loadOverview()
})

onMounted(() => {
  loadOverview()
})
</script>

<style lang="scss" scoped>
.overview-tab {
  padding: 0;
}

// 加载状态
.loading-container {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
}

// 统计卡片
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &.clickable {
    cursor: pointer;

    &:hover {
      border-color: #409eff;
      transform: translateY(-2px);
    }
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.student-icon {
    background: #e8f4ff;
    color: #409eff;
  }

  &.rank-icon {
    background: #fdf6ec;
    color: #e6a23c;
  }

  &.score-icon {
    background: #f0f9eb;
    color: #67c23a;
  }

  &.appeal-icon {
    background: #fef0f0;
    color: #f56c6c;
  }
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;

  .stat-suffix {
    font-size: 16px;
    font-weight: 400;
    color: #909399;
  }

  .stat-unit {
    font-size: 14px;
    font-weight: 400;
    color: #909399;
  }
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-extra {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;

  &.trend-up {
    color: #67c23a;
  }

  &.trend-down {
    color: #f56c6c;
  }

  &.trend-flat {
    color: #909399;
  }
}

// 趋势图区域
.trend-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }

  .section-subtitle {
    font-size: 12px;
    color: #909399;
    margin-left: 8px;
  }
}

.trend-chart-container {
  min-height: 200px;
}

.trend-chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #909399;

  .placeholder-icon {
    color: #c0c4cc;
    margin-bottom: 12px;
  }

  p {
    margin: 0;
    font-size: 14px;
  }
}

// 快捷操作
.quick-actions-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  border-radius: 8px;
  background: #f5f7fa;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: #e8f4ff;

    .action-icon {
      color: #409eff;
      transform: scale(1.1);
    }

    .action-label {
      color: #409eff;
    }
  }

  .action-icon {
    color: #606266;
    transition: all 0.3s;
  }

  .action-label {
    font-size: 14px;
    color: #606266;
    transition: color 0.3s;
  }
}

// 最近检查记录
.recent-records-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
}

.records-table {
  margin-top: 8px;
}

.score-excellent {
  color: #67c23a;
  font-weight: 600;
}

.score-good {
  color: #409eff;
  font-weight: 600;
}

.score-pass {
  color: #e6a23c;
  font-weight: 600;
}

.score-fail {
  color: #f56c6c;
  font-weight: 600;
}

.rank-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background: #f5f7fa;
  color: #606266;

  &.rank-first {
    background: linear-gradient(135deg, #ffd700 0%, #ffb800 100%);
    color: white;
  }

  &.rank-second {
    background: linear-gradient(135deg, #c0c0c0 0%, #a0a0a0 100%);
    color: white;
  }

  &.rank-third {
    background: linear-gradient(135deg, #cd7f32 0%, #b06020 100%);
    color: white;
  }
}
</style>
