<template>
  <view class="appeal-page">
    <!-- 顶部统计 -->
    <view class="stats-header">
      <view class="stats-bg"></view>
      <view class="stats-content">
        <text class="page-title">申诉管理</text>
        <view class="stats-cards">
          <view class="stat-card" @click="filterByTab('my')">
            <text class="stat-num">{{ stats.myAppeals || 0 }}</text>
            <text class="stat-label">我的申诉</text>
          </view>
          <view class="stat-card" @click="filterByTab('pending')">
            <text class="stat-num warning">{{ stats.pendingReview || 0 }}</text>
            <text class="stat-label">待审核</text>
          </view>
          <view class="stat-card" @click="filterByTab('publicity')">
            <text class="stat-num primary">{{ stats.inPublicity || 0 }}</text>
            <text class="stat-label">公示中</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Tab切换 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'my' }"
        @click="switchTab('my')"
      >
        <text>我的申诉</text>
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'pending' }"
        @click="switchTab('pending')"
      >
        <text>待审核</text>
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'publicity' }"
        @click="switchTab('publicity')"
      >
        <text>公示中</text>
      </view>
    </view>

    <!-- 申诉列表 -->
    <scroll-view
      class="appeal-list"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <!-- 加载中 -->
      <view v-if="loading && appeals.length === 0" class="loading-state">
        <view class="loading-spinner"></view>
        <text>加载中...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="appeals.length === 0" class="empty-state">
        <text class="empty-icon">&#128203;</text>
        <text class="empty-text">{{ getEmptyText() }}</text>
      </view>

      <!-- 申诉卡片列表 -->
      <template v-else>
        <view
          class="appeal-card"
          v-for="appeal in appeals"
          :key="appeal.id"
          @click="goToDetail(appeal.id)"
        >
          <view class="card-header">
            <view class="appeal-info">
              <text class="appeal-code">{{ appeal.appealCode }}</text>
              <view class="status-badge" :class="getStatusClass(appeal.status)">
                {{ getStatusText(appeal.status) }}
              </view>
            </view>
            <text class="appeal-time">{{ formatDateTime(appeal.appealTime) }}</text>
          </view>

          <view class="card-body">
            <view class="class-info">
              <text class="class-name">{{ appeal.className }}</text>
              <text class="category-name">{{ appeal.categoryName }}</text>
            </view>
            <view class="item-info">
              <text class="item-name">{{ appeal.itemName }}</text>
              <view class="score-change">
                <text class="original">原扣分: -{{ appeal.originalScore }}</text>
                <text class="arrow" v-if="appeal.adjustedScore !== undefined">&#8594;</text>
                <text class="adjusted" v-if="appeal.adjustedScore !== undefined">-{{ appeal.adjustedScore }}</text>
              </view>
            </view>
            <view class="appeal-reason">
              <text class="reason-label">申诉理由:</text>
              <text class="reason-text">{{ appeal.appealReason }}</text>
            </view>
          </view>

          <view class="card-footer">
            <view class="footer-left">
              <text class="appellant">申诉人: {{ appeal.appellantName }}</text>
              <text class="appeal-type">{{ getAppealTypeText(appeal.appealType) }}</text>
            </view>
            <view class="footer-right">
              <text class="view-detail">查看详情</text>
              <text class="arrow-icon">&#10148;</text>
            </view>
          </view>
        </view>

        <!-- 加载更多 -->
        <view class="load-more" v-if="hasMore">
          <view v-if="loadingMore" class="loading-more">
            <view class="loading-spinner small"></view>
            <text>加载中...</text>
          </view>
          <text v-else class="load-more-text">上拉加载更多</text>
        </view>

        <!-- 没有更多 -->
        <view class="no-more" v-else-if="appeals.length > 0">
          <text>- 没有更多了 -</text>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getAppealList,
  getPendingAppeals,
  getPublicityAppeals,
  getAppealSummary,
  type Appeal,
  type AppealStats,
  AppealStatus,
  AppealType
} from '@/api/appeal'

// Tab状态
const activeTab = ref<'my' | 'pending' | 'publicity'>('my')

// 统计数据
const stats = ref<AppealStats>({
  myAppeals: 0,
  pendingReview: 0,
  inPublicity: 0,
  totalAppeals: 0
})

// 申诉列表
const appeals = ref<Appeal[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const refreshing = ref(false)

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const hasMore = computed(() => appeals.value.length < total.value)

// 初始化
onShow(() => {
  loadStats()
  loadAppeals(true)
})

// 加载统计数据
async function loadStats() {
  try {
    const res = await getAppealSummary()
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

// 加载申诉列表
async function loadAppeals(isRefresh = false) {
  if (isRefresh) {
    pageNum.value = 1
    appeals.value = []
  }

  loading.value = true
  try {
    let res
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }

    // 根据Tab调用不同API
    switch (activeTab.value) {
      case 'pending':
        res = await getPendingAppeals(params)
        break
      case 'publicity':
        res = await getPublicityAppeals(params)
        break
      default:
        res = await getAppealList(params)
    }

    if (res.code === 200) {
      if (isRefresh) {
        appeals.value = res.data.records || []
      } else {
        appeals.value = [...appeals.value, ...(res.data.records || [])]
      }
      total.value = res.data.total || 0
    } else {
      uni.showToast({
        title: res.message || '加载失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('加载申诉列表失败:', error)
    uni.showToast({
      title: '网络错误',
      icon: 'none'
    })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 加载更多
async function loadMore() {
  if (loadingMore.value || !hasMore.value) return

  loadingMore.value = true
  pageNum.value++

  try {
    let res
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }

    switch (activeTab.value) {
      case 'pending':
        res = await getPendingAppeals(params)
        break
      case 'publicity':
        res = await getPublicityAppeals(params)
        break
      default:
        res = await getAppealList(params)
    }

    if (res.code === 200) {
      appeals.value = [...appeals.value, ...(res.data.records || [])]
    }
  } catch (error) {
    console.error('加载更多失败:', error)
    pageNum.value--
  } finally {
    loadingMore.value = false
  }
}

// 下拉刷新
function onRefresh() {
  refreshing.value = true
  loadStats()
  loadAppeals(true)
}

// 切换Tab
function switchTab(tab: 'my' | 'pending' | 'publicity') {
  if (activeTab.value === tab) return
  activeTab.value = tab
  loadAppeals(true)
}

// 通过统计卡片筛选
function filterByTab(tab: 'my' | 'pending' | 'publicity') {
  switchTab(tab)
}

// 获取状态文本
function getStatusText(status: AppealStatus): string {
  const statusMap: Record<AppealStatus, string> = {
    [AppealStatus.PENDING]: '待审核',
    [AppealStatus.APPROVED]: '已通过',
    [AppealStatus.REJECTED]: '已驳回',
    [AppealStatus.WITHDRAWN]: '已撤销',
    [AppealStatus.EXPIRED]: '已过期',
    [AppealStatus.PUBLICITY]: '公示中',
    [AppealStatus.EFFECTIVE]: '已生效'
  }
  return statusMap[status] || '未知'
}

// 获取状态样式
function getStatusClass(status: AppealStatus): string {
  const classMap: Record<AppealStatus, string> = {
    [AppealStatus.PENDING]: 'pending',
    [AppealStatus.APPROVED]: 'approved',
    [AppealStatus.REJECTED]: 'rejected',
    [AppealStatus.WITHDRAWN]: 'withdrawn',
    [AppealStatus.EXPIRED]: 'expired',
    [AppealStatus.PUBLICITY]: 'publicity',
    [AppealStatus.EFFECTIVE]: 'effective'
  }
  return classMap[status] || ''
}

// 获取申诉类型文本
function getAppealTypeText(type: AppealType): string {
  const typeMap: Record<AppealType, string> = {
    [AppealType.SCORE]: '分数异议',
    [AppealType.FACT]: '事实异议',
    [AppealType.PROCEDURE]: '程序异议'
  }
  return typeMap[type] || '未知'
}

// 获取空状态文本
function getEmptyText(): string {
  switch (activeTab.value) {
    case 'pending':
      return '暂无待审核的申诉'
    case 'publicity':
      return '暂无公示中的申诉'
    default:
      return '暂无申诉记录'
  }
}

// 格式化日期时间
function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

// 跳转到详情
function goToDetail(id: string | number) {
  uni.navigateTo({
    url: `/pages/appeal/detail?id=${id}`
  })
}
</script>

<style lang="scss" scoped>
.appeal-page {
  min-height: 100vh;
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
}

// 统计头部
.stats-header {
  position: relative;
  padding: 30rpx;
  padding-top: calc(30rpx + env(safe-area-inset-top));
}

.stats-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 320rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stats-content {
  position: relative;
  z-index: 1;
}

.page-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 30rpx;
}

.stats-cards {
  display: flex;
  gap: 16rpx;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 16rpx;
  text-align: center;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);

  &:active {
    background: #f9f9f9;
  }

  .stat-num {
    display: block;
    font-size: 44rpx;
    font-weight: 700;
    color: #333;

    &.warning {
      color: #ff9800;
    }

    &.primary {
      color: #667eea;
    }
  }

  .stat-label {
    font-size: 24rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

// Tab栏
.tab-bar {
  display: flex;
  margin: 20rpx 30rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.tab-item {
  flex: 1;
  padding: 20rpx;
  text-align: center;
  border-radius: 12rpx;
  transition: all 0.3s;

  text {
    font-size: 28rpx;
    color: #666;
  }

  &.active {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

    text {
      color: #fff;
      font-weight: 500;
    }
  }
}

// 申诉列表
.appeal-list {
  flex: 1;
  padding: 0 30rpx 30rpx;
}

// 加载状态
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;

  text {
    margin-top: 20rpx;
    color: #999;
    font-size: 28rpx;
  }
}

.loading-spinner {
  width: 50rpx;
  height: 50rpx;
  border: 4rpx solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;

  &.small {
    width: 32rpx;
    height: 32rpx;
    border-width: 3rpx;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

// 申诉卡片
.appeal-card {
  background: #fff;
  border-radius: 20rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);

  &:active {
    background: #fafafa;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.appeal-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.appeal-code {
  font-size: 26rpx;
  color: #666;
  font-family: monospace;
}

.status-badge {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;

  &.pending {
    background: #fff3e0;
    color: #ff9800;
  }

  &.approved, &.effective {
    background: #e8f5e9;
    color: #4caf50;
  }

  &.rejected {
    background: #ffebee;
    color: #f44336;
  }

  &.withdrawn, &.expired {
    background: #f5f5f5;
    color: #999;
  }

  &.publicity {
    background: #e3f2fd;
    color: #2196f3;
  }
}

.appeal-time {
  font-size: 24rpx;
  color: #999;
}

.card-body {
  padding: 24rpx;
}

.class-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;

  .class-name {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
  }

  .category-name {
    padding: 4rpx 12rpx;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    border-radius: 8rpx;
    font-size: 22rpx;
    color: #667eea;
  }
}

.item-info {
  margin-bottom: 16rpx;

  .item-name {
    display: block;
    font-size: 28rpx;
    color: #333;
    margin-bottom: 8rpx;
  }

  .score-change {
    display: flex;
    align-items: center;
    gap: 12rpx;

    .original {
      font-size: 26rpx;
      color: #f44336;
    }

    .arrow {
      font-size: 24rpx;
      color: #999;
    }

    .adjusted {
      font-size: 26rpx;
      color: #4caf50;
      font-weight: 500;
    }
  }
}

.appeal-reason {
  padding: 16rpx;
  background: #f9f9f9;
  border-radius: 12rpx;

  .reason-label {
    font-size: 24rpx;
    color: #999;
    margin-right: 8rpx;
  }

  .reason-text {
    font-size: 26rpx;
    color: #666;
    line-height: 1.5;
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #f9f9f9;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16rpx;

  .appellant {
    font-size: 24rpx;
    color: #666;
  }

  .appeal-type {
    padding: 4rpx 12rpx;
    background: #fff;
    border-radius: 8rpx;
    font-size: 22rpx;
    color: #999;
  }
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 8rpx;

  .view-detail {
    font-size: 26rpx;
    color: #667eea;
  }

  .arrow-icon {
    font-size: 22rpx;
    color: #667eea;
  }
}

// 加载更多
.load-more,
.no-more {
  text-align: center;
  padding: 30rpx 0;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;

  text {
    font-size: 26rpx;
    color: #999;
  }
}

.load-more-text {
  font-size: 26rpx;
  color: #999;
}

.no-more text {
  font-size: 26rpx;
  color: #ccc;
}
</style>
