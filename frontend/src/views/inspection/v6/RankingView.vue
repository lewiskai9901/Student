<template>
  <div class="v6-ranking-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-title">
          <h1>排名榜单</h1>
          <p>查看检查目标的排名情况和分数变化</p>
        </div>
        <div class="header-actions">
          <button class="action-btn" @click="exportRanking">
            <el-icon><Download /></el-icon>
            导出榜单
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stat-card excellent">
        <div class="stat-icon">
          <el-icon><TrophyBase /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ excellentCount }}</span>
          <span class="stat-label">优秀 (≥95分)</span>
        </div>
      </div>
      <div class="stat-card good">
        <div class="stat-icon">
          <el-icon><Medal /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ goodCount }}</span>
          <span class="stat-label">良好 (85-94分)</span>
        </div>
      </div>
      <div class="stat-card normal">
        <div class="stat-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ normalCount }}</span>
          <span class="stat-label">合格 (70-84分)</span>
        </div>
      </div>
      <div class="stat-card poor">
        <div class="stat-icon">
          <el-icon><WarningFilled /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ poorCount }}</span>
          <span class="stat-label">不合格 (&lt;70分)</span>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <div class="filter-left">
        <div class="filter-item">
          <label>检查项目</label>
          <el-select v-model="queryParams.projectId" placeholder="选择项目" @change="loadRanking" class="filter-select">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </div>
        <div class="filter-item">
          <label>检查日期</label>
          <el-date-picker
            v-model="queryParams.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            @change="loadRanking"
            class="filter-date"
          />
        </div>
        <div class="filter-item">
          <label>目标类型</label>
          <div class="type-tabs">
            <button
              v-for="tab in typeTabs"
              :key="tab.value"
              :class="['type-tab', { active: queryParams.targetType === tab.value }]"
              @click="setTargetType(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>
      </div>
      <div class="filter-right">
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input v-model="searchKeyword" placeholder="搜索名称..." class="search-input" />
        </div>
      </div>
    </div>

    <!-- 排名榜单 -->
    <div class="ranking-section" v-loading="loading">
      <!-- 前三名展示 -->
      <div v-if="filteredRankingList.length >= 3" class="top-three">
        <div class="podium-item second" v-if="filteredRankingList[1]">
          <div class="podium-avatar">
            <span class="avatar-text">{{ filteredRankingList[1].target_name?.charAt(0) }}</span>
            <div class="rank-medal silver">2</div>
          </div>
          <div class="podium-name">{{ filteredRankingList[1].target_name }}</div>
          <div class="podium-score">{{ filteredRankingList[1].final_score }}分</div>
          <div class="podium-bar" style="height: 80px;"></div>
        </div>
        <div class="podium-item first" v-if="filteredRankingList[0]">
          <div class="podium-crown">
            <el-icon><Trophy /></el-icon>
          </div>
          <div class="podium-avatar gold">
            <span class="avatar-text">{{ filteredRankingList[0].target_name?.charAt(0) }}</span>
            <div class="rank-medal gold">1</div>
          </div>
          <div class="podium-name">{{ filteredRankingList[0].target_name }}</div>
          <div class="podium-score">{{ filteredRankingList[0].final_score }}分</div>
          <div class="podium-bar gold" style="height: 100px;"></div>
        </div>
        <div class="podium-item third" v-if="filteredRankingList[2]">
          <div class="podium-avatar">
            <span class="avatar-text">{{ filteredRankingList[2].target_name?.charAt(0) }}</span>
            <div class="rank-medal bronze">3</div>
          </div>
          <div class="podium-name">{{ filteredRankingList[2].target_name }}</div>
          <div class="podium-score">{{ filteredRankingList[2].final_score }}分</div>
          <div class="podium-bar bronze" style="height: 60px;"></div>
        </div>
      </div>

      <!-- 完整排名列表 -->
      <div class="ranking-table-wrapper">
        <table class="ranking-table">
          <thead>
            <tr>
              <th style="width: 80px">排名</th>
              <th style="min-width: 180px">名称</th>
              <th style="width: 100px">类型</th>
              <th style="width: 150px">所属组织</th>
              <th style="width: 120px">得分</th>
              <th style="width: 100px">扣分</th>
              <th style="width: 100px">加分</th>
              <th style="width: 120px">排名变化</th>
              <th style="width: 100px">检查次数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in filteredRankingList" :key="item.target_id" class="ranking-row">
              <td>
                <div class="rank-badge" :class="getRankClass(item.daily_rank)">
                  <template v-if="item.daily_rank <= 3">
                    <el-icon v-if="item.daily_rank === 1"><Trophy /></el-icon>
                    <span v-else>{{ item.daily_rank }}</span>
                  </template>
                  <template v-else>{{ item.daily_rank }}</template>
                </div>
              </td>
              <td>
                <div class="name-cell">
                  <div class="name-avatar" :style="{ background: getAvatarColor(index) }">
                    {{ item.target_name?.charAt(0) }}
                  </div>
                  <span class="name-text">{{ item.target_name }}</span>
                </div>
              </td>
              <td>
                <span class="type-tag" :class="item.target_type?.toLowerCase()">
                  {{ getTargetTypeLabel(item.target_type) }}
                </span>
              </td>
              <td>
                <span class="org-name">{{ item.org_unit_name || '-' }}</span>
              </td>
              <td>
                <div class="score-cell">
                  <span class="score-value" :class="getScoreClass(item.final_score)">{{ item.final_score }}</span>
                  <div class="score-bar">
                    <div class="score-bar-fill" :style="{ width: `${item.final_score}%` }" :class="getScoreClass(item.final_score)"></div>
                  </div>
                </div>
              </td>
              <td>
                <span class="deduction-value">-{{ item.total_deduction || 0 }}</span>
              </td>
              <td>
                <span class="bonus-value">+{{ item.total_bonus || 0 }}</span>
              </td>
              <td>
                <div class="rank-change" :class="getRankChangeClass(item.rank_change)">
                  <template v-if="item.rank_change > 0">
                    <el-icon><Top /></el-icon>
                    <span>{{ item.rank_change }}</span>
                  </template>
                  <template v-else-if="item.rank_change < 0">
                    <el-icon><Bottom /></el-icon>
                    <span>{{ Math.abs(item.rank_change) }}</span>
                  </template>
                  <template v-else>
                    <span class="no-change">-</span>
                  </template>
                </div>
              </td>
              <td>
                <span class="count-badge">{{ item.inspection_count || 0 }}次</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="filteredRankingList.length === 0" class="empty-state">
          <el-icon class="empty-icon"><DataAnalysis /></el-icon>
          <p>暂无排名数据</p>
          <span>请选择项目和日期查看排名</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Download, Search, TrophyBase, Medal, CircleCheck, WarningFilled,
  Trophy, Top, Bottom, DataAnalysis
} from '@element-plus/icons-vue'
import { v6ProjectApi, v6SummaryApi } from '@/api/v6Inspection'

const loading = ref(false)
const projects = ref<any[]>([])
const rankingList = ref<any[]>([])
const searchKeyword = ref('')

const queryParams = reactive({
  projectId: undefined as number | undefined,
  date: '',
  targetType: ''
})

const typeTabs = [
  { value: '', label: '全部' },
  { value: 'ORG', label: '组织' },
  { value: 'PLACE', label: '场所' },
  { value: 'USER', label: '用户' }
]

// 统计计算
const excellentCount = computed(() => rankingList.value.filter(r => r.final_score >= 95).length)
const goodCount = computed(() => rankingList.value.filter(r => r.final_score >= 85 && r.final_score < 95).length)
const normalCount = computed(() => rankingList.value.filter(r => r.final_score >= 70 && r.final_score < 85).length)
const poorCount = computed(() => rankingList.value.filter(r => r.final_score < 70).length)

// 过滤后的列表
const filteredRankingList = computed(() => {
  if (!searchKeyword.value) return rankingList.value
  const keyword = searchKeyword.value.toLowerCase()
  return rankingList.value.filter(r =>
    r.target_name?.toLowerCase().includes(keyword) ||
    r.org_unit_name?.toLowerCase().includes(keyword)
  )
})

const getRankClass = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return 'bronze'
  return ''
}

const getTargetTypeLabel = (type: string) => {
  const labels: Record<string, string> = { ORG: '组织', PLACE: '场所', USER: '用户' }
  return labels[type] || type
}

const getScoreClass = (score: number) => {
  if (score >= 95) return 'excellent'
  if (score >= 85) return 'good'
  if (score >= 70) return 'normal'
  return 'poor'
}

const getRankChangeClass = (change: number) => {
  if (change > 0) return 'up'
  if (change < 0) return 'down'
  return 'same'
}

const getAvatarColor = (index: number) => {
  const colors = [
    'linear-gradient(135deg, #667eea, #764ba2)',
    'linear-gradient(135deg, #f093fb, #f5576c)',
    'linear-gradient(135deg, #4facfe, #00f2fe)',
    'linear-gradient(135deg, #43e97b, #38f9d7)',
    'linear-gradient(135deg, #fa709a, #fee140)',
    'linear-gradient(135deg, #a8edea, #fed6e3)',
  ]
  return colors[index % colors.length]
}

const setTargetType = (type: string) => {
  queryParams.targetType = type
  loadRanking()
}

const loadProjects = async () => {
  try {
    const res = await v6ProjectApi.list({ status: 'ACTIVE', page: 1, size: 100 })
    projects.value = res.records || []
    if (projects.value.length > 0) {
      queryParams.projectId = projects.value[0].id
    }
  } catch (error) {
    console.error('加载项目失败:', error)
  }
}

const loadRanking = async () => {
  if (!queryParams.projectId || !queryParams.date) {
    return
  }

  loading.value = true
  try {
    rankingList.value = await v6SummaryApi.getDailyRanking(
      queryParams.projectId,
      queryParams.date,
      queryParams.targetType || undefined
    )
  } catch (error) {
    ElMessage.error('加载排名失败')
  } finally {
    loading.value = false
  }
}

const exportRanking = () => {
  ElMessage.info('导出功能开发中')
}

onMounted(async () => {
  await loadProjects()
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  queryParams.date = yesterday.toISOString().split('T')[0]
  if (queryParams.projectId) {
    loadRanking()
  }
})
</script>

<style scoped lang="scss">
.v6-ranking-view {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;

  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-title {
    h1 {
      margin: 0 0 4px 0;
      font-size: 24px;
      font-weight: 600;
      color: #1a1a2e;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .action-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
    border-radius: 8px;
    color: white;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.35);
    }
  }
}

/* 统计卡片 */
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
  }

  .stat-content {
    .stat-value {
      display: block;
      font-size: 28px;
      font-weight: 700;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: #909399;
    }
  }

  &.excellent {
    .stat-icon {
      background: rgba(103, 194, 58, 0.1);
      color: #67c23a;
    }
    .stat-value {
      color: #67c23a;
    }
  }

  &.good {
    .stat-icon {
      background: rgba(64, 158, 255, 0.1);
      color: #409eff;
    }
    .stat-value {
      color: #409eff;
    }
  }

  &.normal {
    .stat-icon {
      background: rgba(230, 162, 60, 0.1);
      color: #e6a23c;
    }
    .stat-value {
      color: #e6a23c;
    }
  }

  &.poor {
    .stat-icon {
      background: rgba(245, 108, 108, 0.1);
      color: #f56c6c;
    }
    .stat-value {
      color: #f56c6c;
    }
  }
}

/* 筛选区域 */
.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;

  .filter-left {
    display: flex;
    align-items: flex-end;
    gap: 24px;
  }

  .filter-item {
    display: flex;
    flex-direction: column;
    gap: 8px;

    label {
      font-size: 13px;
      font-weight: 500;
      color: #606266;
    }
  }

  .filter-select,
  .filter-date {
    width: 200px;
  }

  .type-tabs {
    display: flex;
    gap: 8px;

    .type-tab {
      padding: 8px 16px;
      border: 1px solid #e8ecf1;
      border-radius: 8px;
      background: white;
      color: #606266;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #667eea;
        color: #667eea;
      }

      &.active {
        background: #667eea;
        border-color: #667eea;
        color: white;
      }
    }
  }

  .search-box {
    position: relative;

    .search-icon {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
      color: #909399;
    }

    .search-input {
      width: 240px;
      padding: 10px 12px 10px 36px;
      border: 1px solid #e8ecf1;
      border-radius: 8px;
      font-size: 14px;
      transition: all 0.2s;

      &:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }
    }
  }
}

/* 排名区域 */
.ranking-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

/* 前三名展示 */
.top-three {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 40px;
  padding: 40px 20px;
  background: linear-gradient(180deg, #f8f9ff 0%, white 100%);
  border-bottom: 1px solid #e8ecf1;

  .podium-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    &.first {
      order: 2;
    }

    &.second {
      order: 1;
    }

    &.third {
      order: 3;
    }
  }

  .podium-crown {
    font-size: 32px;
    color: #ffd700;
    margin-bottom: 8px;
    animation: bounce 2s infinite;
  }

  .podium-avatar {
    position: relative;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea, #764ba2);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 12px;

    &.gold {
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #ffd700, #ffb347);
      box-shadow: 0 8px 24px rgba(255, 215, 0, 0.4);
    }

    .avatar-text {
      color: white;
      font-size: 24px;
      font-weight: 600;
    }

    .rank-medal {
      position: absolute;
      bottom: -8px;
      right: -8px;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 700;
      border: 2px solid white;

      &.gold {
        background: linear-gradient(135deg, #ffd700, #ff8c00);
        color: white;
      }

      &.silver {
        background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
        color: white;
      }

      &.bronze {
        background: linear-gradient(135deg, #cd7f32, #a0522d);
        color: white;
      }
    }
  }

  .podium-name {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 4px;
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .podium-score {
    font-size: 20px;
    font-weight: 700;
    color: #667eea;
    margin-bottom: 16px;
  }

  .podium-bar {
    width: 80px;
    background: linear-gradient(180deg, #e8ecf1, #d0d5dd);
    border-radius: 8px 8px 0 0;

    &.gold {
      background: linear-gradient(180deg, #ffd700, #ffb347);
    }

    &.bronze {
      background: linear-gradient(180deg, #cd7f32, #a0522d);
    }
  }
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

/* 排名表格 */
.ranking-table-wrapper {
  overflow-x: auto;
}

.ranking-table {
  width: 100%;
  border-collapse: collapse;

  th {
    padding: 16px 20px;
    text-align: left;
    font-size: 13px;
    font-weight: 600;
    color: #909399;
    background: #fafbfc;
    border-bottom: 1px solid #e8ecf1;
  }

  td {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f2f5;
    vertical-align: middle;
  }

  .ranking-row {
    transition: background 0.2s;

    &:hover {
      background: #f8f9ff;
    }
  }
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 600;
  background: #f0f2f5;
  color: #606266;

  &.gold {
    background: linear-gradient(135deg, #ffd700, #ffb347);
    color: white;
  }

  &.silver {
    background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
    color: white;
  }

  &.bronze {
    background: linear-gradient(135deg, #cd7f32, #a0522d);
    color: white;
  }
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .name-avatar {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 14px;
    font-weight: 600;
  }

  .name-text {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
}

.type-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;

  &.org {
    background: rgba(102, 126, 234, 0.1);
    color: #667eea;
  }

  &.place {
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
  }

  &.user {
    background: rgba(230, 162, 60, 0.1);
    color: #e6a23c;
  }
}

.org-name {
  font-size: 14px;
  color: #606266;
}

.score-cell {
  .score-value {
    display: block;
    font-size: 18px;
    font-weight: 700;
    margin-bottom: 6px;

    &.excellent {
      color: #67c23a;
    }

    &.good {
      color: #409eff;
    }

    &.normal {
      color: #e6a23c;
    }

    &.poor {
      color: #f56c6c;
    }
  }

  .score-bar {
    height: 4px;
    background: #e8ecf1;
    border-radius: 2px;
    overflow: hidden;

    .score-bar-fill {
      height: 100%;
      border-radius: 2px;
      transition: width 0.3s;

      &.excellent {
        background: linear-gradient(90deg, #67c23a, #95d475);
      }

      &.good {
        background: linear-gradient(90deg, #409eff, #79bbff);
      }

      &.normal {
        background: linear-gradient(90deg, #e6a23c, #f5a623);
      }

      &.poor {
        background: linear-gradient(90deg, #f56c6c, #e74c3c);
      }
    }
  }
}

.deduction-value {
  font-size: 14px;
  font-weight: 500;
  color: #f56c6c;
}

.bonus-value {
  font-size: 14px;
  font-weight: 500;
  color: #67c23a;
}

.rank-change {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;

  &.up {
    color: #67c23a;
  }

  &.down {
    color: #f56c6c;
  }

  &.same .no-change {
    color: #909399;
  }
}

.count-badge {
  display: inline-block;
  padding: 4px 10px;
  background: #f0f2f5;
  border-radius: 12px;
  font-size: 12px;
  color: #606266;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #909399;

  .empty-icon {
    font-size: 64px;
    color: #c0c4cc;
    margin-bottom: 16px;
  }

  p {
    margin: 0 0 8px 0;
    font-size: 16px;
    color: #606266;
  }

  span {
    font-size: 14px;
  }
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-section {
    flex-direction: column;
    align-items: stretch;

    .filter-left {
      flex-wrap: wrap;
    }

    .filter-right {
      margin-top: 16px;
    }
  }
}

@media (max-width: 768px) {
  .stats-section {
    grid-template-columns: 1fr;
  }

  .top-three {
    display: none;
  }
}
</style>
