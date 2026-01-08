<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">数据统计</text>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view
      scroll-y
      class="content"
      :style="{ marginTop: navBarHeight + 'px' }"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 加载中 -->
      <view v-if="loading" class="loading-wrapper">
        <u-loading-icon size="40" color="#667eea" />
      </view>

      <template v-else>
        <!-- 检查次数统计 -->
        <view class="stats-card">
          <view class="card-header">
            <text class="card-title">检查次数</text>
          </view>
          <view class="stats-grid">
            <view class="stat-item">
              <text class="stat-value">{{ stats.todayChecks || 0 }}</text>
              <text class="stat-label">今日</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ stats.weekChecks || 0 }}</text>
              <text class="stat-label">本周</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ stats.monthChecks || 0 }}</text>
              <text class="stat-label">本月</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ stats.totalChecks || 0 }}</text>
              <text class="stat-label">累计</text>
            </view>
          </view>
        </view>

        <!-- 扣分统计 -->
        <view class="stats-card">
          <view class="card-header">
            <text class="card-title">扣分情况</text>
          </view>
          <view class="deduction-stats">
            <view class="deduction-item">
              <view class="deduction-icon avg">
                <u-icon name="minus-circle" size="20" color="#fff" />
              </view>
              <view class="deduction-info">
                <text class="deduction-value">{{ stats.avgDeduction?.toFixed(1) || '0.0' }}</text>
                <text class="deduction-label">平均扣分</text>
              </view>
            </view>
            <view class="deduction-item">
              <view class="deduction-icon max">
                <u-icon name="arrow-up" size="20" color="#fff" />
              </view>
              <view class="deduction-info">
                <text class="deduction-value">{{ stats.maxDeduction || 0 }}</text>
                <text class="deduction-label">最大扣分</text>
              </view>
            </view>
            <view class="deduction-item">
              <view class="deduction-icon total">
                <u-icon name="file-text" size="20" color="#fff" />
              </view>
              <view class="deduction-info">
                <text class="deduction-value">{{ stats.totalDeductions || 0 }}</text>
                <text class="deduction-label">总扣分</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 申诉统计 -->
        <view class="stats-card">
          <view class="card-header">
            <text class="card-title">申诉情况</text>
          </view>
          <view class="appeal-stats">
            <view class="appeal-item">
              <text class="appeal-value total">{{ stats.totalAppeals || 0 }}</text>
              <text class="appeal-label">总申诉</text>
            </view>
            <view class="appeal-divider"></view>
            <view class="appeal-item">
              <text class="appeal-value pending">{{ stats.pendingAppeals || 0 }}</text>
              <text class="appeal-label">待处理</text>
            </view>
            <view class="appeal-divider"></view>
            <view class="appeal-item">
              <text class="appeal-value approved">{{ stats.approvedAppeals || 0 }}</text>
              <text class="appeal-label">已通过</text>
            </view>
            <view class="appeal-divider"></view>
            <view class="appeal-item">
              <text class="appeal-value rejected">{{ stats.rejectedAppeals || 0 }}</text>
              <text class="appeal-label">已驳回</text>
            </view>
          </view>
        </view>

        <!-- 评级分布 -->
        <view class="stats-card" v-if="stats.ratingDistribution?.length">
          <view class="card-header">
            <text class="card-title">评级分布</text>
          </view>
          <view class="rating-list">
            <view
              v-for="rating in stats.ratingDistribution"
              :key="rating.rating"
              class="rating-item"
            >
              <view class="rating-label">
                <text class="rating-badge" :class="'rating-' + rating.rating.toLowerCase()">
                  {{ rating.rating }}
                </text>
                <text class="rating-name">{{ rating.ratingName }}</text>
              </view>
              <view class="rating-bar-wrapper">
                <view
                  class="rating-bar"
                  :class="'rating-' + rating.rating.toLowerCase()"
                  :style="{ width: rating.percentage + '%' }"
                ></view>
              </view>
              <text class="rating-count">{{ rating.count }} ({{ rating.percentage.toFixed(1) }}%)</text>
            </view>
          </view>
        </view>

        <!-- 快捷入口 -->
        <view class="quick-links">
          <view class="link-item" @click="goToRanking">
            <view class="link-icon">
              <u-icon name="order" size="24" color="#667eea" />
            </view>
            <view class="link-text">
              <text class="link-title">班级排名</text>
              <text class="link-desc">查看各班级量化排名</text>
            </view>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
          <view class="link-item" @click="goToTrend">
            <view class="link-icon">
              <u-icon name="pie-chart" size="24" color="#10B981" />
            </view>
            <view class="link-text">
              <text class="link-title">趋势分析</text>
              <text class="link-desc">查看扣分趋势变化</text>
            </view>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStatisticsOverview, type StatisticsOverview } from '@/api/statistics'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 数据
const loading = ref(true)
const refreshing = ref(false)
const stats = ref<StatisticsOverview>({
  totalChecks: 0,
  weekChecks: 0,
  monthChecks: 0,
  todayChecks: 0,
  totalDeductions: 0,
  avgDeduction: 0,
  maxDeduction: 0,
  minDeduction: 0,
  totalAppeals: 0,
  pendingAppeals: 0,
  approvedAppeals: 0,
  rejectedAppeals: 0,
  ratingDistribution: []
})

onMounted(() => {
  initSystemInfo()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getStatisticsOverview()
    stats.value = res || stats.value
  } catch (error: any) {
    console.error('加载统计数据失败', error)
    // 使用模拟数据展示
    stats.value = {
      totalChecks: 156,
      weekChecks: 23,
      monthChecks: 78,
      todayChecks: 5,
      totalDeductions: 1250,
      avgDeduction: 8.5,
      maxDeduction: 45,
      minDeduction: 0,
      totalAppeals: 32,
      pendingAppeals: 5,
      approvedAppeals: 20,
      rejectedAppeals: 7,
      ratingDistribution: [
        { rating: 'A', ratingName: '优秀', count: 15, percentage: 25 },
        { rating: 'B', ratingName: '良好', count: 24, percentage: 40 },
        { rating: 'C', ratingName: '中等', count: 12, percentage: 20 },
        { rating: 'D', ratingName: '及格', count: 6, percentage: 10 },
        { rating: 'E', ratingName: '不及格', count: 3, percentage: 5 },
      ]
    }
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onRefresh = () => {
  refreshing.value = true
  loadData()
}

const goToRanking = () => {
  uni.navigateTo({
    url: '/pages/statistics/class-ranking'
  })
}

const goToTrend = () => {
  uni.navigateTo({
    url: '/pages/statistics/trend'
  })
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: #F3F4F6;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .nav-content {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 88rpx;
    padding: 0 24rpx;

    .nav-title {
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
    }
  }
}

.content {
  height: calc(100vh - var(--nav-height));
  padding: 20rpx 24rpx;

  .loading-wrapper {
    display: flex;
    justify-content: center;
    padding: 100rpx 0;
  }
}

.stats-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;

  .card-header {
    margin-bottom: 20rpx;

    .card-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #1F2937;
    }
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16rpx;

    .stat-item {
      text-align: center;

      .stat-value {
        font-size: 40rpx;
        font-weight: 600;
        color: #667eea;
        display: block;
      }

      .stat-label {
        font-size: 22rpx;
        color: #9CA3AF;
      }
    }
  }

  .deduction-stats {
    display: flex;
    gap: 16rpx;

    .deduction-item {
      flex: 1;
      display: flex;
      align-items: center;
      padding: 16rpx;
      background: #F9FAFB;
      border-radius: 12rpx;

      .deduction-icon {
        width: 48rpx;
        height: 48rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 12rpx;
        margin-right: 12rpx;

        &.avg {
          background: #667eea;
        }
        &.max {
          background: #EF4444;
        }
        &.total {
          background: #F59E0B;
        }
      }

      .deduction-info {
        .deduction-value {
          font-size: 28rpx;
          font-weight: 600;
          color: #1F2937;
          display: block;
        }

        .deduction-label {
          font-size: 20rpx;
          color: #9CA3AF;
        }
      }
    }
  }

  .appeal-stats {
    display: flex;
    align-items: center;

    .appeal-item {
      flex: 1;
      text-align: center;

      .appeal-value {
        font-size: 36rpx;
        font-weight: 600;
        display: block;

        &.total {
          color: #667eea;
        }
        &.pending {
          color: #F59E0B;
        }
        &.approved {
          color: #10B981;
        }
        &.rejected {
          color: #EF4444;
        }
      }

      .appeal-label {
        font-size: 22rpx;
        color: #9CA3AF;
      }
    }

    .appeal-divider {
      width: 1rpx;
      height: 48rpx;
      background: #E5E7EB;
    }
  }

  .rating-list {
    .rating-item {
      display: flex;
      align-items: center;
      margin-bottom: 16rpx;

      &:last-child {
        margin-bottom: 0;
      }

      .rating-label {
        width: 120rpx;
        display: flex;
        align-items: center;
        gap: 8rpx;

        .rating-badge {
          width: 36rpx;
          height: 36rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 20rpx;
          font-weight: 600;
          color: #fff;
          border-radius: 8rpx;

          &.rating-a {
            background: #10B981;
          }
          &.rating-b {
            background: #3B82F6;
          }
          &.rating-c {
            background: #F59E0B;
          }
          &.rating-d {
            background: #F97316;
          }
          &.rating-e {
            background: #EF4444;
          }
        }

        .rating-name {
          font-size: 24rpx;
          color: #6B7280;
        }
      }

      .rating-bar-wrapper {
        flex: 1;
        height: 16rpx;
        background: #F3F4F6;
        border-radius: 8rpx;
        margin: 0 16rpx;
        overflow: hidden;

        .rating-bar {
          height: 100%;
          border-radius: 8rpx;
          transition: width 0.3s;

          &.rating-a {
            background: #10B981;
          }
          &.rating-b {
            background: #3B82F6;
          }
          &.rating-c {
            background: #F59E0B;
          }
          &.rating-d {
            background: #F97316;
          }
          &.rating-e {
            background: #EF4444;
          }
        }
      }

      .rating-count {
        width: 120rpx;
        text-align: right;
        font-size: 24rpx;
        color: #6B7280;
      }
    }
  }
}

.quick-links {
  .link-item {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 16rpx;

    .link-icon {
      width: 64rpx;
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #F3F4F6;
      border-radius: 16rpx;
      margin-right: 20rpx;
    }

    .link-text {
      flex: 1;

      .link-title {
        font-size: 30rpx;
        font-weight: 500;
        color: #1F2937;
        display: block;
      }

      .link-desc {
        font-size: 24rpx;
        color: #9CA3AF;
      }
    }
  }
}
</style>
