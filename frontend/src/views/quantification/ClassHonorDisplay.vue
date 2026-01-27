<template>
  <div class="class-honor-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-banner">
        <div class="banner-content">
          <h1 class="class-name">{{ classInfo?.className }}</h1>
          <p class="class-info">
            {{ classInfo?.gradeName }} | {{ classInfo?.orgUnitName }}
          </p>
        </div>
        <div class="banner-stats">
          <div class="stat-item">
            <div class="stat-value">{{ badgeRecords.length }}</div>
            <div class="stat-label">获得徽章</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ ratingHistory?.totalRatings || 0 }}</div>
            <div class="stat-label">参评次数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ ratingHistory?.awardRate?.toFixed(1) || 0 }}%</div>
            <div class="stat-label">获奖率</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="选择班级">
          <el-select
            v-model="selectedClassId"
            placeholder="选择班级"
            style="width: 200px"
            filterable
            @change="loadClassData"
          >
            <el-option
              v-for="cls in classList"
              :key="cls.id"
              :label="cls.className"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="检查计划">
          <el-select
            v-model="selectedPlanId"
            placeholder="选择检查计划"
            style="width: 200px"
            @change="loadRatingHistory"
          >
            <el-option
              v-for="plan in checkPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 荣誉徽章展示区 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon" :size="20"><Medal /></el-icon>
            <span class="card-title">荣誉徽章</span>
          </div>
          <el-tag type="info">共 {{ badgeRecords.length }} 个</el-tag>
        </div>
      </template>

      <div v-if="badgeRecords.length > 0" class="badge-showcase">
        <div
          v-for="record in badgeRecords"
          :key="record.id"
          class="badge-showcase-item"
          @click="viewBadgeDetail(record)"
        >
          <div
            class="badge-medal"
            :class="getBadgeLevelClass(record.badgeLevel)"
          >
            <el-icon :size="48"><Medal /></el-icon>
          </div>
          <div class="badge-info">
            <h4 class="badge-title">{{ record.badgeName }}</h4>
            <p class="badge-date">
              {{ new Date(record.grantedAt).toLocaleDateString() }}
            </p>
            <el-tag
              size="small"
              :type="getBadgeLevelType(record.badgeLevel)"
            >
              {{ getBadgeLevelLabel(record.badgeLevel) }}
            </el-tag>
          </div>
          <div v-if="record.achievementData" class="badge-achievement">
            <div class="achievement-item" v-if="record.achievementData.frequency">
              <span class="label">获奖次数</span>
              <span class="value">{{ record.achievementData.frequency }}</span>
            </div>
            <div class="achievement-item" v-if="record.achievementData.consecutiveCount">
              <span class="label">连续次数</span>
              <span class="value">{{ record.achievementData.consecutiveCount }}</span>
            </div>
          </div>
          <div v-if="record.certificateGenerated" class="certificate-badge">
            <el-icon><Document /></el-icon>
            证书已生成
          </div>
        </div>
      </div>

      <el-empty
        v-else
        description="暂无荣誉徽章"
        :image-size="120"
      />
    </el-card>

    <!-- 评级历史统计 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :md="12">
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" :size="20"><DataAnalysis /></el-icon>
                <span class="card-title">等级分布</span>
              </div>
            </div>
          </template>

          <div v-if="ratingHistory" class="level-distribution">
            <div
              v-for="(count, level) in ratingHistory.levelFrequencies"
              :key="level"
              class="level-bar-item"
            >
              <div class="level-bar-header">
                <span class="level-name">{{ level }}</span>
                <span class="level-count">{{ count }} 次</span>
              </div>
              <el-progress
                :percentage="calculatePercentage(count, ratingHistory.totalRatings)"
                :color="getLevelColor(level)"
              />
            </div>
          </div>

          <el-empty v-else description="暂无数据" :image-size="100" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon" :size="20"><TrendCharts /></el-icon>
                <span class="card-title">最近评级记录</span>
              </div>
            </div>
          </template>

          <el-timeline v-if="ratingHistory?.recentResults?.length > 0">
            <el-timeline-item
              v-for="(result, index) in ratingHistory.recentResults.slice(0, 10)"
              :key="index"
              :timestamp="result.checkDate"
              placement="top"
            >
              <el-card class="timeline-card" shadow="hover">
                <div class="timeline-content">
                  <el-tag
                    :type="getLevelTagType(result.levelName)"
                    :color="result.levelColor"
                    size="small"
                    effect="dark"
                  >
                    {{ result.levelName }}
                  </el-tag>
                  <span class="timeline-score">得分: {{ result.score }}</span>
                  <el-tag
                    v-if="result.levelChange && result.levelChange !== 'SAME'"
                    size="small"
                    :type="result.levelChange === 'UP' ? 'success' : 'danger'"
                  >
                    {{ result.levelChange === 'UP' ? '↑ 上升' : '↓ 下降' }}
                  </el-tag>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>

          <el-empty v-else description="暂无记录" :image-size="100" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 评级趋势图表 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon" :size="20"><TrendCharts /></el-icon>
            <span class="card-title">评级趋势</span>
          </div>
          <el-radio-group v-model="trendGranularity" size="small" @change="loadTrendData">
            <el-radio-button value="DAY">按天</el-radio-button>
            <el-radio-button value="WEEK">按周</el-radio-button>
            <el-radio-button value="MONTH">按月</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <RatingTrendChart
        v-if="trendData.length > 0"
        :trend-data="trendData"
        :height="300"
      />

      <el-empty v-else description="暂无趋势数据" :image-size="100" />
    </el-card>

    <!-- 徽章详情对话框 -->
    <BadgeDetailDialog
      v-model="badgeDetailVisible"
      :badge-record="currentBadgeRecord"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Medal, TrendCharts, Trophy } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import {
  getClassBadgeRecords,
  type ClassBadgeRecordVO,
  BADGE_LEVEL_LABELS,
  BADGE_LEVEL_COLORS
} from '@/api/v2/rating'
import {
  getClassHistory,
  getRatingTrend,
  type ClassRatingHistoryVO,
  type RatingTrendVO
} from '@/api/v2/rating'
import { getClassList } from '@/api/v2/organization'
import { getCheckPlanPage } from '@/api/v2/quantification'
import RatingTrendChart from './components/charts/RatingTrendChart.vue'
import BadgeDetailDialog from './components/badge/BadgeDetailDialog.vue'

// 路由
const route = useRoute()

// 响应式数据
const selectedClassId = ref<number>()
const selectedPlanId = ref<number>()
const classList = ref<any[]>([])
const checkPlans = ref<any[]>([])
const classInfo = ref<any>()
const badgeRecords = ref<ClassBadgeRecordVO[]>([])
const ratingHistory = ref<ClassRatingHistoryVO>()
const trendData = ref<RatingTrendVO[]>([])
const trendGranularity = ref('WEEK')

const badgeDetailVisible = ref(false)
const currentBadgeRecord = ref<ClassBadgeRecordVO | null>(null)

const loading = ref(false)

// 加载班级列表
const loadClassList = async () => {
  try {
    const res = await getClassList({ pageNum: 1, pageSize: 100 })
    classList.value = res?.data?.records || []

    // 如果路由携带了classId参数
    const classIdFromRoute = route.query.classId
    if (classIdFromRoute) {
      selectedClassId.value = Number(classIdFromRoute)
    } else if (classList.value.length > 0) {
      selectedClassId.value = classList.value[0].id
    }

    if (selectedClassId.value) {
      await loadClassData()
    }
  } catch (error) {
    console.error('加载班级列表失败:', error)
    ElMessage.error('加载班级列表失败，请检查后端服务')
  }
}

// 加载检查计划列表
const loadCheckPlans = async () => {
  try {
    const res = await getCheckPlanPage({ pageNum: 1, pageSize: 100 })
    checkPlans.value = res?.data?.records || []
    if (checkPlans.value.length > 0) {
      selectedPlanId.value = checkPlans.value[0].id
      await loadRatingHistory()
    }
  } catch (error) {
    console.error('加载检查计划失败:', error)
    ElMessage.error('加载检查计划失败，请检查后端服务')
  }
}

// 加载班级数据
const loadClassData = async () => {
  if (!selectedClassId.value) return

  // 加载班级信息
  classInfo.value = classList.value.find((c) => c.id === selectedClassId.value)

  // 加载徽章记录
  await loadBadgeRecords()

  // 加载评级历史
  if (selectedPlanId.value) {
    await loadRatingHistory()
  }
}

// 加载徽章记录
const loadBadgeRecords = async () => {
  if (!selectedClassId.value) return

  try {
    const res = await getClassBadgeRecords(selectedClassId.value)
    badgeRecords.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载徽章记录失败')
  }
}

// 加载评级历史
const loadRatingHistory = async () => {
  if (!selectedClassId.value || !selectedPlanId.value) return

  loading.value = true
  try {
    const res = await getClassHistory({
      classId: selectedClassId.value,
      checkPlanId: selectedPlanId.value
    })
    ratingHistory.value = res.data

    // 加载趋势数据
    await loadTrendData()
  } catch (error: any) {
    ElMessage.error(error.message || '加载评级历史失败')
  } finally {
    loading.value = false
  }
}

// 加载趋势数据
const loadTrendData = async () => {
  if (!selectedClassId.value || !selectedPlanId.value) return

  try {
    // 默认查询最近3个月
    const now = new Date()
    const periodEnd = now.toISOString().split('T')[0]
    const periodStart = new Date(now.getFullYear(), now.getMonth() - 3, 1)
      .toISOString()
      .split('T')[0]

    const res = await getRatingTrend({
      checkPlanId: selectedPlanId.value,
      periodStart,
      periodEnd,
      granularity: trendGranularity.value as any
    })

    trendData.value = res.data || []
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  }
}

// 查看徽章详情
const viewBadgeDetail = (record: ClassBadgeRecordVO) => {
  currentBadgeRecord.value = record
  badgeDetailVisible.value = true
}

// 计算百分比
const calculatePercentage = (count: number, total: number) => {
  if (total === 0) return 0
  return Math.round((count / total) * 100)
}

// 获取徽章等级类名
const getBadgeLevelClass = (level: string) => {
  return `badge-level-${level.toLowerCase()}`
}

// 获取徽章等级类型
const getBadgeLevelType = (level: string) => {
  const typeMap: Record<string, any> = {
    GOLD: 'warning',
    SILVER: 'info',
    BRONZE: 'success'
  }
  return typeMap[level] || 'info'
}

// 获取徽章等级标签
const getBadgeLevelLabel = (level: string) => {
  return BADGE_LEVEL_LABELS[level] || level
}

// 获取等级颜色
const getLevelColor = (levelName: string) => {
  // 这里可以根据实际的等级配置返回颜色
  const colorMap: Record<string, string> = {
    优秀: '#67C23A',
    良好: '#409EFF',
    合格: '#E6A23C',
    待改进: '#F56C6C'
  }
  return colorMap[levelName] || '#909399'
}

// 获取等级标签类型
const getLevelTagType = (levelName: string) => {
  const typeMap: Record<string, any> = {
    优秀: 'success',
    良好: 'primary',
    合格: 'warning',
    待改进: 'danger'
  }
  return typeMap[levelName] || 'info'
}

// 初始化
onMounted(() => {
  loadClassList()
  loadCheckPlans()
})
</script>

<style scoped lang="scss">
.class-honor-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  .header-banner {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    padding: 40px;
    color: white;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);

    .banner-content {
      .class-name {
        margin: 0;
        font-size: 32px;
        font-weight: 600;
      }

      .class-info {
        margin: 10px 0 0 0;
        font-size: 16px;
        opacity: 0.9;
      }
    }

    .banner-stats {
      display: flex;
      gap: 40px;

      .stat-item {
        text-align: center;

        .stat-value {
          font-size: 36px;
          font-weight: 700;
          line-height: 1;
          margin-bottom: 8px;
        }

        .stat-label {
          font-size: 14px;
          opacity: 0.9;
        }
      }
    }
  }
}

.filter-card {
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding: 15px 20px;
  }
}

.section-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .header-icon {
        color: #409eff;
      }

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}

.badge-showcase {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;

  .badge-showcase-item {
    position: relative;
    padding: 20px;
    border: 2px solid #e4e7ed;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
      transform: translateY(-4px);
    }

    .badge-medal {
      width: 80px;
      height: 80px;
      margin: 0 auto 15px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 3px solid;

      &.badge-level-gold {
        background: linear-gradient(135deg, #ffd89b 0%, #19547b 100%);
        border-color: #ffd700;
        color: #ffd700;
      }

      &.badge-level-silver {
        background: linear-gradient(135deg, #c9d6ff 0%, #e2e2e2 100%);
        border-color: #c0c0c0;
        color: #c0c0c0;
      }

      &.badge-level-bronze {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        border-color: #cd7f32;
        color: #cd7f32;
      }
    }

    .badge-info {
      text-align: center;
      margin-bottom: 15px;

      .badge-title {
        margin: 0 0 8px 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .badge-date {
        margin: 0 0 8px 0;
        font-size: 13px;
        color: #909399;
      }
    }

    .badge-achievement {
      display: flex;
      justify-content: space-around;
      padding: 12px 0;
      border-top: 1px solid #ebeef5;
      margin-bottom: 10px;

      .achievement-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .label {
          font-size: 12px;
          color: #909399;
          margin-bottom: 4px;
        }

        .value {
          font-size: 18px;
          font-weight: 600;
          color: #409eff;
        }
      }
    }

    .certificate-badge {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 8px;
      background: #f0f9ff;
      border-radius: 6px;
      font-size: 13px;
      color: #409eff;
    }
  }
}

.stats-row {
  margin-bottom: 20px;
}

.level-distribution {
  .level-bar-item {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }

    .level-bar-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;

      .level-name {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
      }

      .level-count {
        font-size: 14px;
        color: #606266;
      }
    }
  }
}

.timeline-card {
  :deep(.el-card__body) {
    padding: 12px;
  }

  .timeline-content {
    display: flex;
    align-items: center;
    gap: 12px;

    .timeline-score {
      font-size: 14px;
      color: #606266;
    }
  }
}
</style>
