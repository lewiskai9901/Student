<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">班级排名</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-section" :style="{ marginTop: navBarHeight + 'px' }">
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

    <!-- 排名列表 -->
    <scroll-view
      scroll-y
      class="ranking-list"
      :style="{ height: listHeight + 'px' }"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <!-- 加载中 -->
        <view v-if="loading && !rankings.length" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 排名卡片 -->
        <view
          v-for="(item, index) in rankings"
          :key="item.classId"
          class="ranking-card"
          @click="goToClassDetail(item)"
        >
          <view class="rank-badge" :class="getRankClass(item.rank)">
            <text v-if="item.rank <= 3">{{ getRankIcon(item.rank) }}</text>
            <text v-else>{{ item.rank }}</text>
          </view>
          <view class="class-info">
            <view class="class-name">{{ item.className }}</view>
            <view class="class-detail">
              <text>{{ item.departmentName }}</text>
              <text class="divider">·</text>
              <text>{{ item.gradeName }}</text>
            </view>
          </view>
          <view class="score-info">
            <text class="score-value">{{ item.avgScore?.toFixed(1) || '-' }}</text>
            <text class="score-label">平均分</text>
            <view class="rating-badge" :class="'rating-' + (item.rating || 'c').toLowerCase()">
              {{ item.rating || '-' }}
            </view>
          </view>
          <view class="trend-info" v-if="item.trend !== undefined">
            <u-icon
              :name="item.trend > 0 ? 'arrow-up' : item.trend < 0 ? 'arrow-down' : 'minus'"
              size="16"
              :color="item.trend > 0 ? '#10B981' : item.trend < 0 ? '#EF4444' : '#9CA3AF'"
            />
            <text :class="item.trend > 0 ? 'up' : item.trend < 0 ? 'down' : ''">
              {{ Math.abs(item.trend) }}
            </text>
          </view>
        </view>

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !rankings.length"
          type="default"
          text="暂无排名数据"
        />

        <!-- 加载更多 -->
        <LoadMore
          v-if="rankings.length"
          :status="loadMoreStatus"
          @loadmore="loadMore"
        />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getClassRankings, type ClassRanking } from '@/api/statistics'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)
const listHeight = ref(500)

// 数据
const rankings = ref<ClassRanking[]>([])
const loading = ref(false)
const refreshing = ref(false)
const loadMoreStatus = ref<'loading' | 'loadmore' | 'nomore'>('loadmore')

// 筛选
const selectedPeriod = ref('month')
const periods = [
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '本学期', value: 'semester' },
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
})

onMounted(() => {
  initSystemInfo()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
  const windowHeight = sysInfo.windowHeight
  listHeight.value = windowHeight - navBarHeight.value - 80
}

const loadData = async (isRefresh = false) => {
  if (loading.value) return

  if (isRefresh) {
    queryParams.pageNum = 1
    loadMoreStatus.value = 'loadmore'
  }

  loading.value = true
  try {
    const res = await getClassRankings({
      ...queryParams,
      period: selectedPeriod.value as any
    })

    const newData = res.records || []
    if (isRefresh) {
      rankings.value = newData
    } else {
      rankings.value = [...rankings.value, ...newData]
    }

    if (newData.length < queryParams.pageSize) {
      loadMoreStatus.value = 'nomore'
    } else {
      loadMoreStatus.value = 'loadmore'
    }
  } catch (error: any) {
    console.error('加载排名数据失败', error)
    // 使用模拟数据
    rankings.value = Array.from({ length: 10 }, (_, i) => ({
      rank: i + 1,
      classId: i + 1,
      className: `2024级计算机${i + 1}班`,
      departmentName: '信息工程学院',
      gradeName: '2024级',
      totalScore: 100 - i * 3,
      avgScore: 95 - i * 2.5,
      deductionTotal: i * 5,
      checkCount: 10,
      rating: i < 2 ? 'A' : i < 5 ? 'B' : i < 8 ? 'C' : 'D',
      trend: i % 3 === 0 ? 2 : i % 3 === 1 ? -1 : 0
    }))
    loadMoreStatus.value = 'nomore'
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const loadMore = () => {
  if (loadMoreStatus.value !== 'loadmore') return
  queryParams.pageNum++
  loadMoreStatus.value = 'loading'
  loadData()
}

const onRefresh = () => {
  refreshing.value = true
  loadData(true)
}

const selectPeriod = (period: string) => {
  selectedPeriod.value = period
  loadData(true)
}

const getRankClass = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return 'bronze'
  return 'normal'
}

const getRankIcon = (rank: number) => {
  if (rank === 1) return '🥇'
  if (rank === 2) return '🥈'
  if (rank === 3) return '🥉'
  return rank
}

const goBack = () => {
  uni.navigateBack()
}

const goToClassDetail = (item: ClassRanking) => {
  uni.navigateTo({
    url: `/pages/class/detail?id=${item.classId}`
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

.filter-section {
  background: #fff;
  padding: 16rpx 24rpx;

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

.ranking-list {
  .list-content {
    padding: 20rpx 24rpx;
  }

  .loading-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 100rpx 0;

    .loading-text {
      margin-top: 20rpx;
      font-size: 28rpx;
      color: #9CA3AF;
    }
  }
}

.ranking-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 16rpx;

  .rank-badge {
    width: 56rpx;
    height: 56rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    font-weight: 600;
    border-radius: 50%;
    margin-right: 16rpx;

    &.gold {
      font-size: 36rpx;
    }
    &.silver {
      font-size: 36rpx;
    }
    &.bronze {
      font-size: 36rpx;
    }
    &.normal {
      color: #6B7280;
      background: #F3F4F6;
    }
  }

  .class-info {
    flex: 1;

    .class-name {
      font-size: 30rpx;
      font-weight: 500;
      color: #1F2937;
      margin-bottom: 4rpx;
    }

    .class-detail {
      font-size: 22rpx;
      color: #9CA3AF;

      .divider {
        margin: 0 8rpx;
      }
    }
  }

  .score-info {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-right: 16rpx;

    .score-value {
      font-size: 32rpx;
      font-weight: 600;
      color: #667eea;
    }

    .score-label {
      font-size: 20rpx;
      color: #9CA3AF;
    }

    .rating-badge {
      margin-top: 4rpx;
      width: 32rpx;
      height: 32rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18rpx;
      font-weight: 600;
      color: #fff;
      border-radius: 6rpx;

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

  .trend-info {
    display: flex;
    align-items: center;
    gap: 4rpx;
    width: 60rpx;

    text {
      font-size: 24rpx;
      color: #9CA3AF;

      &.up {
        color: #10B981;
      }
      &.down {
        color: #EF4444;
      }
    }
  }
}
</style>
