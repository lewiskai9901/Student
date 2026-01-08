<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 页面头部 -->
    <view class="page-header">
      <view class="header-main">
        <text class="page-title">检查计划</text>
        <text class="page-desc">管理量化检查计划</text>
      </view>
    </view>

    <!-- 状态筛选 -->
    <view class="filter-bar">
      <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
        <view class="filter-list">
          <view
            class="filter-item"
            :class="{ active: currentStatus === undefined }"
            @click="filterByStatus(undefined)"
          >
            <text class="filter-text">全部</text>
            <text class="filter-count">{{ statistics.totalPlans }}</text>
          </view>
          <view
            class="filter-item"
            :class="{ active: currentStatus === 0 }"
            @click="filterByStatus(0)"
          >
            <text class="filter-text">草稿</text>
            <text class="filter-count">{{ statistics.draftCount }}</text>
          </view>
          <view
            class="filter-item"
            :class="{ active: currentStatus === 1 }"
            @click="filterByStatus(1)"
          >
            <text class="filter-text">进行中</text>
            <text class="filter-count">{{ statistics.inProgressCount }}</text>
          </view>
          <view
            class="filter-item"
            :class="{ active: currentStatus === 2 }"
            @click="filterByStatus(2)"
          >
            <text class="filter-text">已结束</text>
            <text class="filter-count">{{ statistics.finishedCount }}</text>
          </view>
          <view
            class="filter-item"
            :class="{ active: currentStatus === 3 }"
            @click="filterByStatus(3)"
          >
            <text class="filter-text">已归档</text>
            <text class="filter-count">{{ statistics.archivedCount }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 列表内容 -->
    <view class="list-container">
      <!-- 加载状态 -->
      <view v-if="loading" class="state-box">
        <text class="state-text">加载中...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="planList.length === 0" class="state-box">
        <text class="state-title">暂无检查计划</text>
        <text class="state-desc">{{ currentStatus !== undefined ? '该状态下暂无计划' : '请在Web端创建检查计划' }}</text>
      </view>

      <!-- 计划列表 -->
      <view v-else class="plan-list">
        <view
          v-for="plan in planList"
          :key="plan.id"
          class="plan-card"
          @click="goDetail(plan.id)"
        >
          <view class="card-header">
            <view class="card-title-row">
              <text class="card-title">{{ plan.planName }}</text>
              <view class="status-badge" :class="getStatusClass(plan.status)">
                {{ getStatusText(plan.status) }}
              </view>
            </view>
            <text class="card-code">{{ plan.planCode }}</text>
          </view>

          <view class="card-body">
            <view class="info-row">
              <text class="info-label">检查模板</text>
              <text class="info-value">{{ plan.templateName }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">时间范围</text>
              <text class="info-value">{{ formatDateRange(plan.startDate, plan.endDate) }}</text>
            </view>
            <view class="info-row" v-if="plan.enableWeight === 1">
              <text class="info-label">加权配置</text>
              <text class="info-value highlight">已启用</text>
            </view>
          </view>

          <view class="card-footer">
            <view class="stat-group">
              <view class="stat-item">
                <text class="stat-value">{{ plan.totalChecks || 0 }}</text>
                <text class="stat-label">检查次数</text>
              </view>
              <view class="stat-item">
                <text class="stat-value">{{ plan.totalRecords || 0 }}</text>
                <text class="stat-label">记录数</text>
              </view>
              <view class="stat-item">
                <text class="stat-value deduct">{{ formatScore(plan.totalDeductionScore) }}</text>
                <text class="stat-label">总扣分</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 加载更多 -->
        <view v-if="hasMore" class="load-more" @click="loadMore">
          <text>{{ loadingMore ? '加载中...' : '加载更多' }}</text>
        </view>

        <!-- 没有更多 -->
        <view v-else class="no-more">
          <text>— 没有更多了 —</text>
        </view>
      </view>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getCheckPlanList, getCheckPlanStatistics, PlanStatus } from '@/api/checkPlan'
import type { CheckPlan, PlanStatistics } from '@/api/checkPlan'

// 状态
const loading = ref(true)
const loadingMore = ref(false)
const planList = ref<CheckPlan[]>([])
const currentStatus = ref<number | undefined>(undefined)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const hasMore = ref(false)

// 统计数据
const statistics = reactive<PlanStatistics>({
  totalPlans: 0,
  draftCount: 0,
  inProgressCount: 0,
  finishedCount: 0,
  archivedCount: 0
})

// 初始化
onMounted(() => {
  loadStatistics()
  loadData()
})

// 加载统计数据
async function loadStatistics() {
  try {
    const data = await getCheckPlanStatistics()
    Object.assign(statistics, data)
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载列表数据
async function loadData(append = false) {
  if (!append) {
    loading.value = true
    pageNum.value = 1
  } else {
    loadingMore.value = true
  }

  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (currentStatus.value !== undefined) {
      params.status = currentStatus.value
    }

    const result = await getCheckPlanList(params)

    if (append) {
      planList.value.push(...result.records)
    } else {
      planList.value = result.records
    }

    total.value = result.total
    hasMore.value = planList.value.length < result.total
  } catch (error) {
    console.error('加载计划列表失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 按状态筛选
function filterByStatus(status: number | undefined) {
  currentStatus.value = status
  loadData()
}

// 加载更多
function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  pageNum.value++
  loadData(true)
}

// 跳转详情
function goDetail(id: string | number) {
  uni.navigateTo({
    url: `/pages/check/plan/detail?id=${id}`
  })
}

// 获取状态样式类
function getStatusClass(status: number): string {
  const classes: Record<number, string> = {
    [PlanStatus.DRAFT]: 'draft',
    [PlanStatus.IN_PROGRESS]: 'progress',
    [PlanStatus.FINISHED]: 'finished',
    [PlanStatus.ARCHIVED]: 'archived'
  }
  return classes[status] || ''
}

// 获取状态文本
function getStatusText(status: number): string {
  const texts: Record<number, string> = {
    [PlanStatus.DRAFT]: '草稿',
    [PlanStatus.IN_PROGRESS]: '进行中',
    [PlanStatus.FINISHED]: '已结束',
    [PlanStatus.ARCHIVED]: '已归档'
  }
  return texts[status] || '未知'
}

// 格式化日期范围
function formatDateRange(startDate: string, endDate: string): string {
  if (!startDate || !endDate) return '-'
  return `${startDate.substring(0, 10)} ~ ${endDate.substring(0, 10)}`
}

// 格式化分数
function formatScore(score: number | undefined): string {
  if (score === undefined || score === null || score === 0) return '0'
  return `-${score}`
}
</script>

<style lang="scss" scoped>
// 变量
$primary: #4F46E5;
$primary-light: #EEF2FF;
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border: #E5E7EB;
$bg: #F9FAFB;
$white: #FFFFFF;
$danger: #DC2626;
$success: #059669;

.page {
  min-height: 100vh;
  background: $bg;
}

.safe-top {
  height: env(safe-area-inset-top);
  background: $white;
}

.safe-bottom {
  height: calc(120rpx + env(safe-area-inset-bottom));
}

// 页面头部
.page-header {
  background: $white;
  padding: 32rpx;
  border-bottom: 1rpx solid $border;

  .page-title {
    display: block;
    font-size: 40rpx;
    font-weight: 600;
    color: $text-primary;
  }

  .page-desc {
    display: block;
    font-size: 26rpx;
    color: $text-muted;
    margin-top: 8rpx;
  }
}

// 筛选栏
.filter-bar {
  background: $white;
  border-bottom: 1rpx solid $border;
}

.filter-scroll {
  white-space: nowrap;
}

.filter-list {
  display: inline-flex;
  padding: 16rpx 24rpx;
  gap: 12rpx;
}

.filter-item {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  border-radius: 8rpx;
  background: $bg;
  transition: all 0.2s;

  .filter-text {
    font-size: 26rpx;
    color: $text-secondary;
  }

  .filter-count {
    font-size: 24rpx;
    color: $text-muted;
    padding: 2rpx 12rpx;
    background: $white;
    border-radius: 20rpx;
  }

  &.active {
    background: $primary;

    .filter-text {
      color: $white;
    }

    .filter-count {
      background: rgba(255, 255, 255, 0.2);
      color: $white;
    }
  }
}

// 列表容器
.list-container {
  padding: 24rpx;
}

// 状态盒子
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;

  .state-text {
    font-size: 28rpx;
    color: $text-muted;
  }

  .state-title {
    font-size: 32rpx;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 12rpx;
  }

  .state-desc {
    font-size: 26rpx;
    color: $text-muted;
  }
}

// 计划卡片
.plan-card {
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;
  margin-bottom: 16rpx;
  overflow: hidden;

  &:active {
    background: $bg;
  }
}

.card-header {
  padding: 24rpx;
  border-bottom: 1rpx solid $border;

  .card-title-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 8rpx;
  }

  .card-title {
    flex: 1;
    font-size: 30rpx;
    font-weight: 600;
    color: $text-primary;
    line-height: 1.4;
  }

  .card-code {
    font-size: 24rpx;
    color: $text-muted;
    font-family: monospace;
  }
}

.status-badge {
  flex-shrink: 0;
  margin-left: 16rpx;
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.draft {
    background: #F3F4F6;
    color: #6B7280;
  }

  &.progress {
    background: #D1FAE5;
    color: #059669;
  }

  &.finished {
    background: #DBEAFE;
    color: #2563EB;
  }

  &.archived {
    background: #F3F4F6;
    color: #9CA3AF;
  }
}

.card-body {
  padding: 20rpx 24rpx;

  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 8rpx 0;

    .info-label {
      font-size: 26rpx;
      color: $text-muted;
    }

    .info-value {
      font-size: 26rpx;
      color: $text-secondary;

      &.highlight {
        color: $primary;
      }
    }
  }
}

.card-footer {
  padding: 20rpx 24rpx;
  background: $bg;
  border-top: 1rpx solid $border;

  .stat-group {
    display: flex;
    justify-content: space-around;
  }

  .stat-item {
    text-align: center;

    .stat-value {
      display: block;
      font-size: 32rpx;
      font-weight: 600;
      color: $text-primary;
      font-variant-numeric: tabular-nums;

      &.deduct {
        color: $danger;
      }
    }

    .stat-label {
      display: block;
      font-size: 22rpx;
      color: $text-muted;
      margin-top: 4rpx;
    }
  }
}

// 加载更多
.load-more,
.no-more {
  text-align: center;
  padding: 32rpx;

  text {
    font-size: 26rpx;
    color: $text-muted;
  }
}

.load-more text {
  color: $primary;
}
</style>
