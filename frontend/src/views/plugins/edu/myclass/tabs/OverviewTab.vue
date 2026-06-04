<template>
  <div class="overview-tab">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else>
      <!-- 统计条 -->
      <StatBar
        class="mb-6"
        :items="[
          { label: '学生人数', value: overview?.studentCount ?? '-' },
          { label: '班级排名', value: overview?.classRank ? `${overview.classRank}${overview.totalClasses ? ' / ' + overview.totalClasses : ''}` : '-' },
          { label: '平均分', value: overview?.averageScore != null ? overview.averageScore.toFixed(1) : '-' },
          { label: '待处理申诉', value: overview?.pendingAppeals ?? 0 },
        ]"
      />

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
import type { LongId } from '@/types/common'
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Users,
  Download,
  Building,
  BarChart3,
  ChevronRight
} from 'lucide-vue-next'
import { getClassOverview } from '@/api-generated/sdk.gen'
import type { MyClassOverview, RecentCheckRecord } from '@/types/myClass'
import TrendChart from '../components/TrendChart.vue'
import StatBar from '@/components/common/StatBar.vue'

const props = defineProps<{
  orgUnitId: LongId
}>()

const emit = defineEmits<{
  (e: 'change-tab', tab: string): void
}>()

const router = useRouter()
const loading = ref(false)
const overview = ref<MyClassOverview | null>(null)

// 加载概览数据
const loadOverview = async () => {
  if (!props.orgUnitId) return

  loading.value = true
  try {
    const res = await getClassOverview({ path: { orgUnitId: props.orgUnitId } })
    overview.value = (res.data?.data ?? null) as MyClassOverview | null
  } catch (error: any) {
    console.error('加载概览数据失败:', error)
    ElMessage.error(error.response?.data?.message || '加载概览数据失败')
  } finally {
    loading.value = false
  }
}

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

// 监听 orgUnitId 变化
watch(() => props.orgUnitId, () => {
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
