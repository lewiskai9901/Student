<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 页面头部 -->
    <view class="page-header">
      <view class="header-main">
        <text class="page-title">检查打分</text>
        <text class="page-desc">选择检查计划进行打分</text>
      </view>
    </view>

    <!-- 日期筛选 -->
    <view class="filter-bar">
      <picker mode="date" :value="selectedDate" @change="onDateChange">
        <view class="date-picker">
          <text class="date-label">检查日期</text>
          <view class="date-value">
            <text>{{ selectedDate || '全部' }}</text>
            <text class="arrow">></text>
          </view>
        </view>
      </picker>
      <view v-if="selectedDate" class="clear-btn" @click="clearDate">
        <text>清除</text>
      </view>
    </view>

    <!-- 列表内容 -->
    <view class="list-container">
      <!-- 加载状态 -->
      <view v-if="loading" class="state-box">
        <text class="state-text">加载中...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="checkList.length === 0" class="state-box">
        <text class="state-title">暂无检查计划</text>
        <text class="state-desc">当前日期没有可打分的检查计划</text>
      </view>

      <!-- 检查列表 -->
      <view v-else class="check-list">
        <view
          v-for="check in checkList"
          :key="check.id"
          class="check-card"
          @click="goScoring(check)"
        >
          <view class="card-header">
            <view class="card-title-row">
              <text class="card-title">{{ check.checkName }}</text>
              <view class="status-badge" :class="getStatusClass(check.status)">
                {{ getStatusText(check.status) }}
              </view>
            </view>
            <text class="card-date">{{ check.checkDate }}</text>
          </view>

          <view class="card-body">
            <view class="info-row" v-if="check.templateName">
              <text class="info-label">检查模板</text>
              <text class="info-value">{{ check.templateName }}</text>
            </view>
            <view class="info-row" v-if="check.totalClasses">
              <text class="info-label">目标班级</text>
              <text class="info-value">{{ check.totalClasses }} 个班级</text>
            </view>
          </view>

          <view class="card-footer">
            <view class="progress-info" v-if="check.scoredClasses !== undefined">
              <text class="progress-text">
                打分进度: {{ check.scoredClasses }}/{{ check.totalClasses }}
              </text>
              <view class="progress-bar">
                <view
                  class="progress-fill"
                  :style="{ width: getProgress(check) + '%' }"
                ></view>
              </view>
            </view>
            <view class="action-hint">
              <text>点击进入打分</text>
              <text class="arrow">></text>
            </view>
          </view>
        </view>

        <!-- 加载更多 -->
        <view v-if="hasMore" class="load-more" @click="loadMore">
          <text>{{ loadingMore ? '加载中...' : '加载更多' }}</text>
        </view>

        <!-- 没有更多 -->
        <view v-else-if="checkList.length > 0" class="no-more">
          <text>— 没有更多了 —</text>
        </view>
      </view>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getScoringPlans } from '@/api/scoring'
import type { CheckPlanItem } from '@/types/scoring'

// 状态
const loading = ref(true)
const loadingMore = ref(false)
const checkList = ref<CheckPlanItem[]>([])
const selectedDate = ref<string>('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const hasMore = ref(false)

// 初始化
onMounted(() => {
  // 默认显示今天
  const today = new Date()
  selectedDate.value = formatDate(today)
  loadData()
})

// 格式化日期
function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 加载数据
async function loadData(append = false) {
  if (!append) {
    loading.value = true
    pageNum.value = 1
  } else {
    loadingMore.value = true
  }

  try {
    const result = await getScoringPlans({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      checkDate: selectedDate.value || undefined
    })

    if (append) {
      checkList.value.push(...result.records)
    } else {
      checkList.value = result.records
    }

    total.value = result.total
    hasMore.value = checkList.value.length < result.total
  } catch (error) {
    console.error('加载检查列表失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 日期改变
function onDateChange(e: any) {
  selectedDate.value = e.detail.value
  loadData()
}

// 清除日期
function clearDate() {
  selectedDate.value = ''
  loadData()
}

// 加载更多
function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  pageNum.value++
  loadData(true)
}

// 跳转打分页面
function goScoring(check: CheckPlanItem) {
  uni.navigateTo({
    url: `/pages/scoring/scoring?id=${check.id}`
  })
}

// 获取进度百分比
function getProgress(check: CheckPlanItem): number {
  if (!check.totalClasses || check.totalClasses === 0) return 0
  return Math.round(((check.scoredClasses || 0) / check.totalClasses) * 100)
}

// 获取状态样式
function getStatusClass(status: number): string {
  const classes: Record<number, string> = {
    0: 'draft',
    1: 'progress',
    2: 'finished'
  }
  return classes[status] || 'draft'
}

// 获取状态文本
function getStatusText(status: number): string {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '进行中',
    2: '已完成'
  }
  return texts[status] || '未知'
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
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid $border;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.date-picker {
  display: flex;
  align-items: center;
  gap: 16rpx;

  .date-label {
    font-size: 28rpx;
    color: $text-secondary;
  }

  .date-value {
    display: flex;
    align-items: center;
    gap: 8rpx;
    padding: 12rpx 20rpx;
    background: $bg;
    border-radius: 8rpx;
    font-size: 28rpx;
    color: $text-primary;

    .arrow {
      color: $text-muted;
      transform: rotate(90deg);
    }
  }
}

.clear-btn {
  padding: 12rpx 20rpx;
  font-size: 26rpx;
  color: $primary;
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

// 检查卡片
.check-card {
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

  .card-date {
    font-size: 24rpx;
    color: $text-muted;
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
    }
  }
}

.card-footer {
  padding: 20rpx 24rpx;
  background: $bg;
  border-top: 1rpx solid $border;
}

.progress-info {
  margin-bottom: 16rpx;

  .progress-text {
    display: block;
    font-size: 24rpx;
    color: $text-secondary;
    margin-bottom: 8rpx;
  }

  .progress-bar {
    height: 8rpx;
    background: $border;
    border-radius: 4rpx;
    overflow: hidden;

    .progress-fill {
      height: 100%;
      background: $success;
      border-radius: 4rpx;
      transition: width 0.3s ease;
    }
  }
}

.action-hint {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8rpx;

  text {
    font-size: 26rpx;
    color: $primary;
  }

  .arrow {
    color: $primary;
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
