<template>
  <div class="v6-summary-dashboard">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-title">
          <h1>数据汇总</h1>
          <p>查看检查数据汇总与趋势分析</p>
        </div>
        <div class="header-actions">
          <button class="action-btn secondary" @click="exportData">
            <el-icon><Download /></el-icon>
            导出报表
          </button>
          <button class="action-btn primary" @click="generateSummary">
            <el-icon><Refresh /></el-icon>
            生成汇总
          </button>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <div class="filter-item">
        <label>检查项目</label>
        <el-select v-model="queryParams.projectId" placeholder="选择项目" @change="loadData" class="filter-select">
          <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </div>
      <div class="filter-item">
        <label>日期范围</label>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          class="filter-date"
        />
      </div>
      <div class="filter-item">
        <button class="query-btn" @click="loadData">
          <el-icon><Search /></el-icon>
          查询数据
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card gradient-purple">
        <div class="stat-icon">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ orgSummary.length }}</span>
          <span class="stat-label">组织数量</span>
        </div>
        <div class="stat-decoration"></div>
      </div>
      <div class="stat-card gradient-blue">
        <div class="stat-icon">
          <el-icon><School /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ classSummary.length }}</span>
          <span class="stat-label">班级数量</span>
        </div>
        <div class="stat-decoration"></div>
      </div>
      <div class="stat-card gradient-green">
        <div class="stat-icon">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ overallAvgScore.toFixed(1) }}</span>
          <span class="stat-label">整体平均分</span>
        </div>
        <div class="stat-decoration"></div>
      </div>
      <div class="stat-card gradient-orange">
        <div class="stat-icon">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ totalDays }}</span>
          <span class="stat-label">检查天数</span>
        </div>
        <div class="stat-decoration"></div>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 左侧：组织汇总 -->
      <div class="content-card">
        <div class="card-header">
          <div class="card-title">
            <el-icon><OfficeBuilding /></el-icon>
            <span>组织汇总</span>
          </div>
          <div class="card-actions">
            <button class="mini-btn" @click="sortOrgBy('avg_score')">
              <el-icon><Sort /></el-icon>
            </button>
          </div>
        </div>
        <div class="card-body" v-loading="loading">
          <div v-if="orgSummary.length > 0" class="org-list">
            <div v-for="(org, index) in orgSummary" :key="org.org_unit_id" class="org-item">
              <div class="org-rank">{{ index + 1 }}</div>
              <div class="org-info">
                <div class="org-avatar" :style="{ background: getGradientColor(index) }">
                  {{ org.org_unit_name?.charAt(0) }}
                </div>
                <div class="org-details">
                  <span class="org-name">{{ org.org_unit_name }}</span>
                  <span class="org-meta">检查{{ org.days }}天</span>
                </div>
              </div>
              <div class="org-score">
                <div class="score-circle" :class="getScoreLevel(org.avg_score)">
                  {{ Number(org.avg_score).toFixed(1) }}
                </div>
              </div>
              <div class="org-stats">
                <span class="stat-deduction">-{{ org.total_deduction }}</span>
                <span class="stat-bonus">+{{ org.total_bonus }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-icon><DataLine /></el-icon>
            <p>暂无数据</p>
          </div>
        </div>
      </div>

      <!-- 右侧：班级汇总 -->
      <div class="content-card">
        <div class="card-header">
          <div class="card-title">
            <el-icon><School /></el-icon>
            <span>班级汇总</span>
          </div>
          <div class="card-actions">
            <button class="mini-btn" @click="sortClassBy('avg_score')">
              <el-icon><Sort /></el-icon>
            </button>
          </div>
        </div>
        <div class="card-body" v-loading="loading">
          <div v-if="classSummary.length > 0" class="class-list">
            <div v-for="(cls, index) in classSummary" :key="cls.class_id" class="class-item">
              <div class="class-rank" :class="{ 'top-three': index < 3 }">
                {{ index + 1 }}
              </div>
              <div class="class-info">
                <span class="class-name">{{ cls.class_name }}</span>
                <span class="class-org">{{ cls.org_unit_name }}</span>
              </div>
              <div class="class-score-bar">
                <div class="score-value" :class="getScoreLevel(cls.avg_score)">
                  {{ Number(cls.avg_score).toFixed(1) }}
                </div>
                <div class="score-bar">
                  <div class="score-bar-fill" :style="{ width: `${cls.avg_score}%` }" :class="getScoreLevel(cls.avg_score)"></div>
                </div>
              </div>
              <div class="class-deduction">
                <span>-{{ cls.total_deduction }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-icon><DataLine /></el-icon>
            <p>暂无数据</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 趋势图表 -->
    <div class="chart-section">
      <div class="chart-card">
        <div class="card-header">
          <div class="card-title">
            <el-icon><TrendCharts /></el-icon>
            <span>分数趋势分析</span>
          </div>
          <div class="chart-legend">
            <span class="legend-item">
              <span class="legend-dot" style="background: #667eea"></span>
              平均分
            </span>
            <span class="legend-item">
              <span class="legend-dot" style="background: #f56c6c"></span>
              扣分
            </span>
          </div>
        </div>
        <div class="chart-body">
          <div class="chart-container" ref="trendChartRef"></div>
        </div>
      </div>

      <div class="chart-card pie-chart">
        <div class="card-header">
          <div class="card-title">
            <el-icon><PieChart /></el-icon>
            <span>分数分布</span>
          </div>
        </div>
        <div class="chart-body">
          <div class="chart-container" ref="pieChartRef"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Download, Refresh, Search, OfficeBuilding, School, TrendCharts,
  Calendar, Sort, DataLine, PieChart
} from '@element-plus/icons-vue'
import { v6ProjectApi, v6SummaryApi } from '@/api/v6Inspection'
import * as echarts from 'echarts'

const loading = ref(false)
const projects = ref<any[]>([])
const orgSummary = ref<any[]>([])
const classSummary = ref<any[]>([])
const dateRange = ref<string[]>([])
const trendChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const queryParams = reactive({
  projectId: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

// 计算属性
const overallAvgScore = computed(() => {
  if (orgSummary.value.length === 0) return 0
  const sum = orgSummary.value.reduce((acc, org) => acc + Number(org.avg_score), 0)
  return sum / orgSummary.value.length
})

const totalDays = computed(() => {
  if (orgSummary.value.length === 0) return 0
  return Math.max(...orgSummary.value.map(o => o.days || 0))
})

// 方法
const getGradientColor = (index: number) => {
  const colors = [
    'linear-gradient(135deg, #667eea, #764ba2)',
    'linear-gradient(135deg, #f093fb, #f5576c)',
    'linear-gradient(135deg, #4facfe, #00f2fe)',
    'linear-gradient(135deg, #43e97b, #38f9d7)',
    'linear-gradient(135deg, #fa709a, #fee140)',
  ]
  return colors[index % colors.length]
}

const getScoreLevel = (score: number) => {
  if (score >= 95) return 'excellent'
  if (score >= 85) return 'good'
  if (score >= 70) return 'normal'
  return 'poor'
}

const handleDateChange = (val: string[] | null) => {
  if (val && val.length === 2) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
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

const loadData = async () => {
  if (!queryParams.projectId || !queryParams.startDate || !queryParams.endDate) {
    return
  }

  loading.value = true
  try {
    const [org, cls] = await Promise.all([
      v6SummaryApi.getOrgSummary({
        projectId: queryParams.projectId,
        startDate: queryParams.startDate,
        endDate: queryParams.endDate
      }),
      v6SummaryApi.getClassSummary({
        projectId: queryParams.projectId,
        startDate: queryParams.startDate,
        endDate: queryParams.endDate
      })
    ])
    orgSummary.value = org
    classSummary.value = cls

    if (orgSummary.value.length > 0) {
      await nextTick()
      initTrendChart()
      initPieChart()
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const initTrendChart = () => {
  if (!trendChartRef.value) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8ecf1',
      borderWidth: 1,
      textStyle: { color: '#303133' },
      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: orgSummary.value.map(o => o.org_unit_name),
      axisLine: { lineStyle: { color: '#e8ecf1' } },
      axisLabel: { color: '#909399', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      min: 70,
      max: 100,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [{
      name: '平均分',
      type: 'bar',
      data: orgSummary.value.map(o => Number(o.avg_score).toFixed(2)),
      barWidth: '50%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ]),
        borderRadius: [8, 8, 0, 0]
      }
    }]
  }
  trendChart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return

  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }

  const excellent = classSummary.value.filter(c => Number(c.avg_score) >= 95).length
  const good = classSummary.value.filter(c => Number(c.avg_score) >= 85 && Number(c.avg_score) < 95).length
  const normal = classSummary.value.filter(c => Number(c.avg_score) >= 70 && Number(c.avg_score) < 85).length
  const poor = classSummary.value.filter(c => Number(c.avg_score) < 70).length

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8ecf1',
      borderWidth: 1,
      textStyle: { color: '#303133' }
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center',
      textStyle: { color: '#606266' }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: {
          show: true,
          fontSize: 16,
          fontWeight: 'bold'
        }
      },
      data: [
        { value: excellent, name: '优秀', itemStyle: { color: '#67c23a' } },
        { value: good, name: '良好', itemStyle: { color: '#409eff' } },
        { value: normal, name: '合格', itemStyle: { color: '#e6a23c' } },
        { value: poor, name: '不合格', itemStyle: { color: '#f56c6c' } }
      ].filter(d => d.value > 0)
    }]
  }
  pieChart.setOption(option)
}

const sortOrgBy = (field: string) => {
  orgSummary.value.sort((a, b) => Number(b[field]) - Number(a[field]))
}

const sortClassBy = (field: string) => {
  classSummary.value.sort((a, b) => Number(b[field]) - Number(a[field]))
}

const generateSummary = async () => {
  if (!queryParams.projectId) {
    ElMessage.warning('请选择项目')
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('请输入要生成汇总的日期 (YYYY-MM-DD)', '生成日汇总', {
      confirmButtonText: '生成',
      cancelButtonText: '取消',
      inputPattern: /^\d{4}-\d{2}-\d{2}$/,
      inputErrorMessage: '请输入正确的日期格式'
    })
    if (value) {
      await v6SummaryApi.generateDaily(queryParams.projectId, value)
      ElMessage.success('汇总生成成功')
      loadData()
    }
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('生成失败')
  }
}

const exportData = () => {
  ElMessage.info('导出功能开发中')
}

const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  await loadProjects()
  const today = new Date()
  const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)
  dateRange.value = [
    weekAgo.toISOString().split('T')[0],
    today.toISOString().split('T')[0]
  ]
  handleDateChange(dateRange.value)
  if (queryParams.projectId) {
    loadData()
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped lang="scss">
.v6-summary-dashboard {
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

  .header-actions {
    display: flex;
    gap: 12px;
  }

  .action-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;

    &.secondary {
      background: white;
      color: #606266;
      border: 1px solid #e8ecf1;

      &:hover {
        border-color: #667eea;
        color: #667eea;
      }
    }

    &.primary {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: white;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.35);
      }
    }
  }
}

/* 筛选区域 */
.filter-section {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;

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

  .filter-select {
    width: 200px;
  }

  .filter-date {
    width: 280px;
  }

  .query-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 20px;
    background: #667eea;
    border: none;
    border-radius: 8px;
    color: white;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #5a6fd6;
    }
  }
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: 16px;
  color: white;
  overflow: hidden;

  &.gradient-purple {
    background: linear-gradient(135deg, #667eea, #764ba2);
  }

  &.gradient-blue {
    background: linear-gradient(135deg, #4facfe, #00f2fe);
  }

  &.gradient-green {
    background: linear-gradient(135deg, #43e97b, #38f9d7);
  }

  &.gradient-orange {
    background: linear-gradient(135deg, #fa709a, #fee140);
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
  }

  .stat-content {
    .stat-value {
      display: block;
      font-size: 32px;
      font-weight: 700;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 14px;
      opacity: 0.9;
    }
  }

  .stat-decoration {
    position: absolute;
    right: -20px;
    bottom: -20px;
    width: 100px;
    height: 100px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
  }
}

/* 主体内容 */
.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.content-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid #e8ecf1;

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #1a1a2e;
    }
  }

  .card-body {
    padding: 16px;
    max-height: 400px;
    overflow-y: auto;
  }

  .mini-btn {
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 8px;
    background: #f5f7fa;
    color: #606266;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #e8ecf1;
      color: #667eea;
    }
  }
}

/* 组织列表 */
.org-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.org-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #fafbfc;
  border-radius: 10px;
  transition: all 0.2s;

  &:hover {
    background: #f0f2f5;
    transform: translateX(4px);
  }

  .org-rank {
    width: 28px;
    height: 28px;
    background: #e8ecf1;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 600;
    color: #606266;
  }

  .org-info {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
  }

  .org-avatar {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 16px;
    font-weight: 600;
  }

  .org-details {
    display: flex;
    flex-direction: column;

    .org-name {
      font-size: 14px;
      font-weight: 500;
      color: #303133;
    }

    .org-meta {
      font-size: 12px;
      color: #909399;
    }
  }

  .org-score {
    .score-circle {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 700;
      color: white;

      &.excellent {
        background: linear-gradient(135deg, #67c23a, #95d475);
      }

      &.good {
        background: linear-gradient(135deg, #409eff, #79bbff);
      }

      &.normal {
        background: linear-gradient(135deg, #e6a23c, #f5a623);
      }

      &.poor {
        background: linear-gradient(135deg, #f56c6c, #e74c3c);
      }
    }
  }

  .org-stats {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;

    .stat-deduction {
      font-size: 13px;
      font-weight: 500;
      color: #f56c6c;
    }

    .stat-bonus {
      font-size: 13px;
      font-weight: 500;
      color: #67c23a;
    }
  }
}

/* 班级列表 */
.class-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.class-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    background: #f5f7fa;
  }

  .class-rank {
    width: 24px;
    height: 24px;
    border-radius: 6px;
    background: #e8ecf1;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: #606266;

    &.top-three {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: white;
    }
  }

  .class-info {
    width: 140px;
    display: flex;
    flex-direction: column;

    .class-name {
      font-size: 14px;
      font-weight: 500;
      color: #303133;
    }

    .class-org {
      font-size: 12px;
      color: #909399;
    }
  }

  .class-score-bar {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12px;

    .score-value {
      width: 48px;
      font-size: 15px;
      font-weight: 600;
      text-align: right;

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
      flex: 1;
      height: 6px;
      background: #e8ecf1;
      border-radius: 3px;
      overflow: hidden;

      .score-bar-fill {
        height: 100%;
        border-radius: 3px;
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

  .class-deduction {
    width: 50px;
    text-align: right;
    font-size: 13px;
    font-weight: 500;
    color: #f56c6c;
  }
}

/* 图表区域 */
.chart-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid #e8ecf1;

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #1a1a2e;
    }
  }

  .chart-legend {
    display: flex;
    gap: 16px;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #606266;
    }

    .legend-dot {
      width: 10px;
      height: 10px;
      border-radius: 2px;
    }
  }

  .chart-body {
    padding: 20px;
  }

  .chart-container {
    height: 300px;
  }

  &.pie-chart .chart-container {
    height: 300px;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;

  .el-icon {
    font-size: 48px;
    color: #c0c4cc;
    margin-bottom: 12px;
  }

  p {
    margin: 0;
    font-size: 14px;
  }
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .main-content {
    grid-template-columns: 1fr;
  }

  .chart-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .filter-section {
    flex-direction: column;
    align-items: stretch;

    .filter-item {
      width: 100%;
    }

    .filter-select,
    .filter-date {
      width: 100%;
    }
  }
}
</style>
