<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">入住统计</text>
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
      <!-- 加载中 -->
      <view v-if="loading" class="loading-wrapper">
        <u-loading-icon size="40" color="#667eea" />
      </view>

      <template v-else>
        <!-- 总体统计 -->
        <view class="summary-section">
          <view class="summary-header">
            <text class="summary-title">整体入住情况</text>
          </view>
          <view class="summary-content">
            <view class="occupancy-circle">
              <view class="circle-inner">
                <text class="rate">{{ stats.occupancyRate || 0 }}%</text>
                <text class="label">入住率</text>
              </view>
            </view>
            <view class="summary-stats">
              <view class="stat-row">
                <view class="stat-item">
                  <text class="stat-value">{{ stats.totalBuildings || 0 }}</text>
                  <text class="stat-label">宿舍楼</text>
                </view>
                <view class="stat-item">
                  <text class="stat-value">{{ stats.totalRooms || 0 }}</text>
                  <text class="stat-label">房间数</text>
                </view>
              </view>
              <view class="stat-row">
                <view class="stat-item">
                  <text class="stat-value occupied">{{ stats.occupiedBeds || 0 }}</text>
                  <text class="stat-label">已入住</text>
                </view>
                <view class="stat-item">
                  <text class="stat-value available">{{ stats.availableBeds || 0 }}</text>
                  <text class="stat-label">空闲床位</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 宿舍楼统计 -->
        <view class="building-stats-section" v-if="stats.buildingStats?.length">
          <view class="section-title">各宿舍楼入住情况</view>
          <view class="building-list">
            <view
              v-for="building in stats.buildingStats"
              :key="building.buildingId"
              class="building-stat-item"
            >
              <view class="building-info">
                <text class="building-name">{{ building.buildingName }}</text>
                <text class="building-detail">{{ building.occupiedBeds }}/{{ building.totalBeds }} 床位</text>
              </view>
              <view class="progress-bar">
                <view
                  class="progress-fill"
                  :style="{ width: building.occupancyRate + '%' }"
                  :class="getProgressClass(building.occupancyRate)"
                ></view>
              </view>
              <text class="occupancy-rate" :class="getProgressClass(building.occupancyRate)">
                {{ Math.round(building.occupancyRate) }}%
              </text>
            </view>
          </view>
        </view>

        <!-- 院系统计 -->
        <view class="dept-stats-section" v-if="stats.departmentStats?.length">
          <view class="section-title">各院系入住情况</view>
          <view class="dept-list">
            <view
              v-for="dept in stats.departmentStats"
              :key="dept.departmentId"
              class="dept-stat-item"
            >
              <view class="dept-info">
                <text class="dept-name">{{ dept.departmentName }}</text>
                <text class="dept-detail">{{ dept.occupiedBeds }}/{{ dept.totalBeds }}</text>
              </view>
              <view class="dept-bar">
                <view
                  class="bar-fill"
                  :style="{ width: dept.occupancyRate + '%' }"
                ></view>
              </view>
              <text class="dept-rate">{{ Math.round(dept.occupancyRate) }}%</text>
            </view>
          </view>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDormitoryOccupancyStats, type OccupancyStats } from '@/api/dormitory'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 数据
const loading = ref(true)
const refreshing = ref(false)
const stats = ref<OccupancyStats>({
  totalBuildings: 0,
  totalRooms: 0,
  totalBeds: 0,
  occupiedBeds: 0,
  availableBeds: 0,
  occupancyRate: 0,
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
    const res = await getDormitoryOccupancyStats()
    stats.value = res || {
      totalBuildings: 0,
      totalRooms: 0,
      totalBeds: 0,
      occupiedBeds: 0,
      availableBeds: 0,
      occupancyRate: 0,
    }
  } catch (error: any) {
    console.error('加载入住统计失败', error)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onRefresh = () => {
  refreshing.value = true
  loadData()
}

const getProgressClass = (rate: number) => {
  if (rate >= 90) return 'high'
  if (rate >= 70) return 'medium'
  return 'low'
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
  padding: 20rpx 24rpx;

  .loading-wrapper {
    display: flex;
    justify-content: center;
    padding: 100rpx 0;
  }
}

.summary-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;

  .summary-header {
    margin-bottom: 24rpx;

    .summary-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #1F2937;
    }
  }

  .summary-content {
    display: flex;
    align-items: center;
    gap: 32rpx;

    .occupancy-circle {
      width: 160rpx;
      height: 160rpx;
      border-radius: 50%;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      display: flex;
      align-items: center;
      justify-content: center;

      .circle-inner {
        width: 130rpx;
        height: 130rpx;
        background: #fff;
        border-radius: 50%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .rate {
          font-size: 36rpx;
          font-weight: 600;
          color: #667eea;
        }

        .label {
          font-size: 20rpx;
          color: #9CA3AF;
        }
      }
    }

    .summary-stats {
      flex: 1;

      .stat-row {
        display: flex;
        margin-bottom: 16rpx;

        &:last-child {
          margin-bottom: 0;
        }

        .stat-item {
          flex: 1;
          text-align: center;

          .stat-value {
            font-size: 36rpx;
            font-weight: 600;
            color: #1F2937;
            display: block;

            &.occupied {
              color: #667eea;
            }
            &.available {
              color: #10B981;
            }
          }

          .stat-label {
            font-size: 22rpx;
            color: #9CA3AF;
          }
        }
      }
    }
  }
}

.building-stats-section,
.dept-stats-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1F2937;
    margin-bottom: 24rpx;
    padding-left: 16rpx;
    border-left: 6rpx solid #667eea;
  }
}

.building-stat-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #F3F4F6;

  &:last-child {
    border-bottom: none;
  }

  .building-info {
    width: 180rpx;

    .building-name {
      font-size: 28rpx;
      font-weight: 500;
      color: #1F2937;
      display: block;
    }

    .building-detail {
      font-size: 22rpx;
      color: #9CA3AF;
    }
  }

  .progress-bar {
    flex: 1;
    height: 16rpx;
    background: #F3F4F6;
    border-radius: 8rpx;
    margin: 0 16rpx;
    overflow: hidden;

    .progress-fill {
      height: 100%;
      border-radius: 8rpx;
      transition: width 0.3s;

      &.low {
        background: #10B981;
      }
      &.medium {
        background: #F59E0B;
      }
      &.high {
        background: #EF4444;
      }
    }
  }

  .occupancy-rate {
    width: 80rpx;
    text-align: right;
    font-size: 28rpx;
    font-weight: 600;

    &.low {
      color: #10B981;
    }
    &.medium {
      color: #F59E0B;
    }
    &.high {
      color: #EF4444;
    }
  }
}

.dept-stat-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #F3F4F6;

  &:last-child {
    border-bottom: none;
  }

  .dept-info {
    width: 200rpx;

    .dept-name {
      font-size: 28rpx;
      font-weight: 500;
      color: #1F2937;
      display: block;
    }

    .dept-detail {
      font-size: 22rpx;
      color: #9CA3AF;
    }
  }

  .dept-bar {
    flex: 1;
    height: 12rpx;
    background: #F3F4F6;
    border-radius: 6rpx;
    margin: 0 16rpx;
    overflow: hidden;

    .bar-fill {
      height: 100%;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 6rpx;
    }
  }

  .dept-rate {
    width: 80rpx;
    text-align: right;
    font-size: 26rpx;
    color: #667eea;
    font-weight: 500;
  }
}
</style>
