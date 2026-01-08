<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">趋势分析</text>
        <view class="nav-placeholder"></view>
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
      <!-- 时间范围选择 -->
      <view class="period-section">
        <view class="period-tabs">
          <view
            v-for="period in periods"
            :key="period.value"
            class="period-tab"
            :class="{ active: selectedPeriod === period.value }"
            @click="selectPeriod(period.value)"
          >
            {{ period.label }}
          </view>
        </view>
      </view>

      <!-- 加载中 -->
      <view v-if="loading" class="loading-wrapper">
        <u-loading-icon size="40" color="#667eea" />
      </view>

      <template v-else>
        <!-- 扣分趋势图 -->
        <view class="chart-card">
          <view class="card-header">
            <text class="card-title">扣分趋势</text>
          </view>
          <view class="chart-container">
            <view class="simple-chart">
              <view class="chart-bars">
                <view
                  v-for="(item, index) in deductionTrend"
                  :key="index"
                  class="bar-item"
                >
                  <view
                    class="bar"
                    :style="{ height: getBarHeight(item.value, maxDeduction) + 'rpx' }"
                  ></view>
                  <text class="bar-value">{{ item.value }}</text>
                  <text class="bar-label">{{ formatDate(item.date) }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 检查次数趋势 -->
        <view class="chart-card">
          <view class="card-header">
            <text class="card-title">检查次数</text>
          </view>
          <view class="chart-container">
            <view class="simple-chart">
              <view class="chart-bars">
                <view
                  v-for="(item, index) in checkCountTrend"
                  :key="index"
                  class="bar-item check"
                >
                  <view
                    class="bar"
                    :style="{ height: getBarHeight(item.value, maxCheckCount) + 'rpx' }"
                  ></view>
                  <text class="bar-value">{{ item.value }}</text>
                  <text class="bar-label">{{ formatDate(item.date) }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 问题分类统计 -->
        <view class="category-card">
          <view class="card-header">
            <text class="card-title">问题分类统计</text>
          </view>
          <view class="category-list">
            <view
              v-for="(item, index) in categoryStats"
              :key="index"
              class="category-item"
            >
              <view class="category-info">
                <view class="category-rank">{{ index + 1 }}</view>
                <text class="category-name">{{ item.categoryName }}</text>
              </view>
              <view class="category-stats">
                <text class="category-count">{{ item.count }}次</text>
                <text class="category-deduction">-{{ item.totalDeduction }}分</text>
              </view>
              <view class="category-bar-wrapper">
                <view
                  class="category-bar"
                  :style="{ width: item.percentage + '%' }"
                ></view>
              </view>
            </view>
          </view>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDeductionTrend, getCheckCountTrend, getCategoryStatistics, type TrendData, type CategoryStats } from '@/api/statistics'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 数据
const loading = ref(true)
const refreshing = ref(false)
const deductionTrend = ref<TrendData[]>([])
const checkCountTrend = ref<TrendData[]>([])
const categoryStats = ref<CategoryStats[]>([])

// 筛选
const selectedPeriod = ref('week')
const periods = [
  { label: '近7天', value: 'week' },
  { label: '近30天', value: 'month' },
  { label: '近90天', value: 'quarter' },
]

const maxDeduction = computed(() => {
  return Math.max(...deductionTrend.value.map(d => d.value), 1)
})

const maxCheckCount = computed(() => {
  return Math.max(...checkCountTrend.value.map(d => d.value), 1)
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
    const [deductionRes, checkRes, categoryRes] = await Promise.all([
      getDeductionTrend({ period: selectedPeriod.value as any }),
      getCheckCountTrend({ period: selectedPeriod.value as any }),
      getCategoryStatistics({ period: selectedPeriod.value as any })
    ])

    deductionTrend.value = deductionRes || []
    checkCountTrend.value = checkRes || []
    categoryStats.value = categoryRes || []
  } catch (error: any) {
    console.error('加载趋势数据失败', error)
    // 使用模拟数据
    const days = selectedPeriod.value === 'week' ? 7 : selectedPeriod.value === 'month' ? 30 : 90
    const now = new Date()

    deductionTrend.value = Array.from({ length: Math.min(days, 7) }, (_, i) => {
      const date = new Date(now)
      date.setDate(date.getDate() - (days - 1 - i * Math.floor(days / 7)))
      return {
        date: date.toISOString().split('T')[0],
        value: Math.floor(Math.random() * 30) + 5
      }
    })

    checkCountTrend.value = Array.from({ length: Math.min(days, 7) }, (_, i) => {
      const date = new Date(now)
      date.setDate(date.getDate() - (days - 1 - i * Math.floor(days / 7)))
      return {
        date: date.toISOString().split('T')[0],
        value: Math.floor(Math.random() * 10) + 2
      }
    })

    categoryStats.value = [
      { categoryId: 1, categoryName: '卫生检查', count: 45, totalDeduction: 180, percentage: 35 },
      { categoryId: 2, categoryName: '纪律检查', count: 32, totalDeduction: 128, percentage: 25 },
      { categoryId: 3, categoryName: '安全检查', count: 28, totalDeduction: 112, percentage: 22 },
      { categoryId: 4, categoryName: '设施检查', count: 15, totalDeduction: 60, percentage: 12 },
      { categoryId: 5, categoryName: '其他', count: 8, totalDeduction: 32, percentage: 6 },
    ]
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onRefresh = () => {
  refreshing.value = true
  loadData()
}

const selectPeriod = (period: string) => {
  selectedPeriod.value = period
  loadData()
}

const getBarHeight = (value: number, max: number) => {
  return Math.max((value / max) * 120, 10)
}

const formatDate = (date: string) => {
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const goBack = () => {
  uni.navigateBack()
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
    height: 88rpx;
    padding: 0 24rpx;

    .nav-back, .nav-placeholder {
      width: 64rpx;
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .nav-title {
      flex: 1;
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
      text-align: center;
    }
  }
}

.content {
  height: calc(100vh - var(--nav-height));
  padding-bottom: 40rpx;

  .loading-wrapper {
    display: flex;
    justify-content: center;
    padding: 100rpx 0;
  }
}

.period-section {
  background: #fff;
  padding: 16rpx 24rpx;
  margin-bottom: 20rpx;

  .period-tabs {
    display: flex;
    gap: 16rpx;

    .period-tab {
      flex: 1;
      text-align: center;
      padding: 16rpx 0;
      font-size: 28rpx;
      color: #6B7280;
      background: #F3F4F6;
      border-radius: 8rpx;

      &.active {
        color: #fff;
        background: #667eea;
      }
    }
  }
}

.chart-card {
  background: #fff;
  border-radius: 16rpx;
  margin: 0 24rpx 20rpx;
  padding: 24rpx;

  .card-header {
    margin-bottom: 24rpx;

    .card-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #1F2937;
    }
  }

  .chart-container {
    .simple-chart {
      padding: 16rpx 0;

      .chart-bars {
        display: flex;
        align-items: flex-end;
        justify-content: space-around;
        height: 180rpx;
        padding-bottom: 48rpx;

        .bar-item {
          display: flex;
          flex-direction: column;
          align-items: center;

          .bar {
            width: 48rpx;
            background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
            border-radius: 8rpx 8rpx 0 0;
            transition: height 0.3s;
          }

          &.check .bar {
            background: linear-gradient(180deg, #10B981 0%, #059669 100%);
          }

          .bar-value {
            font-size: 22rpx;
            font-weight: 600;
            color: #667eea;
            margin-top: 8rpx;
          }

          &.check .bar-value {
            color: #10B981;
          }

          .bar-label {
            font-size: 20rpx;
            color: #9CA3AF;
            margin-top: 4rpx;
          }
        }
      }
    }
  }
}

.category-card {
  background: #fff;
  border-radius: 16rpx;
  margin: 0 24rpx 20rpx;
  padding: 24rpx;

  .card-header {
    margin-bottom: 24rpx;

    .card-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #1F2937;
    }
  }

  .category-list {
    .category-item {
      padding: 16rpx 0;
      border-bottom: 1rpx solid #F3F4F6;

      &:last-child {
        border-bottom: none;
      }

      .category-info {
        display: flex;
        align-items: center;
        margin-bottom: 12rpx;

        .category-rank {
          width: 36rpx;
          height: 36rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 22rpx;
          font-weight: 600;
          color: #fff;
          background: #667eea;
          border-radius: 50%;
          margin-right: 12rpx;
        }

        .category-name {
          font-size: 28rpx;
          color: #1F2937;
        }
      }

      .category-stats {
        display: flex;
        gap: 24rpx;
        margin-bottom: 8rpx;
        padding-left: 48rpx;

        .category-count {
          font-size: 24rpx;
          color: #6B7280;
        }

        .category-deduction {
          font-size: 24rpx;
          color: #EF4444;
        }
      }

      .category-bar-wrapper {
        height: 8rpx;
        background: #F3F4F6;
        border-radius: 4rpx;
        margin-left: 48rpx;
        overflow: hidden;

        .category-bar {
          height: 100%;
          background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
          border-radius: 4rpx;
          transition: width 0.3s;
        }
      }
    }
  }
}
</style>
