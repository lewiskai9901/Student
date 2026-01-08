<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">宿舍楼管理</text>
        <view class="nav-actions">
          <view class="action-btn" @click="goToOverview">
            <u-icon name="pie-chart" size="20" color="#fff" />
          </view>
        </view>
      </view>
    </view>

    <!-- 宿舍楼列表 -->
    <scroll-view
      scroll-y
      class="building-list"
      :style="{ marginTop: navBarHeight + 'px' }"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <!-- 加载中 -->
        <view v-if="loading" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 宿舍楼卡片 -->
        <view
          v-for="building in buildings"
          :key="building.id"
          class="building-card"
          @click="goToRooms(building)"
        >
          <view class="card-left">
            <view class="building-icon" :class="getBuildingTypeClass(building.buildingType)">
              <u-icon name="home" size="28" color="#fff" />
            </view>
          </view>
          <view class="card-center">
            <view class="building-name">{{ building.buildingName }}</view>
            <view class="building-info">
              <text class="info-tag" :class="getBuildingTypeClass(building.buildingType)">
                {{ building.buildingTypeName || getBuildingTypeName(building.buildingType) }}
              </text>
              <text class="info-text">{{ building.floorCount || 0 }}层</text>
              <text class="info-text">{{ building.totalRooms || 0 }}间</text>
            </view>
          </view>
          <view class="card-right">
            <view class="occupancy-ring">
              <u-loading-icon
                :size="60"
                :color="getOccupancyColor(building.occupancyRate)"
              />
              <text class="occupancy-text">{{ Math.round(building.occupancyRate || 0) }}%</text>
            </view>
            <text class="occupancy-label">入住率</text>
          </view>
        </view>

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !buildings.length"
          type="default"
          text="暂无宿舍楼数据"
        />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBuildingList, type Building } from '@/api/dormitory'
import EmptyState from '@/components/common/EmptyState.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 数据
const buildings = ref<Building[]>([])
const loading = ref(true)
const refreshing = ref(false)

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
    const res = await getBuildingList({ pageNum: 1, pageSize: 100 })
    buildings.value = res.records || []
  } catch (error: any) {
    console.error('加载宿舍楼数据失败', error)
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

const getBuildingTypeClass = (type?: number) => {
  switch (type) {
    case 1: return 'male'
    case 2: return 'female'
    default: return 'mixed'
  }
}

const getBuildingTypeName = (type?: number) => {
  switch (type) {
    case 1: return '男生宿舍'
    case 2: return '女生宿舍'
    default: return '混合'
  }
}

const getOccupancyColor = (rate?: number) => {
  const r = rate || 0
  if (r >= 90) return '#EF4444'
  if (r >= 70) return '#F59E0B'
  if (r >= 50) return '#10B981'
  return '#667eea'
}

const goBack = () => {
  uni.navigateBack()
}

const goToOverview = () => {
  uni.navigateTo({
    url: '/pages/dormitory/overview'
  })
}

const goToRooms = (building: Building) => {
  uni.navigateTo({
    url: `/pages/dormitory/room-list?buildingId=${building.id}&buildingName=${encodeURIComponent(building.buildingName)}`
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

    .nav-back {
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

    .nav-actions {
      .action-btn {
        width: 64rpx;
        height: 64rpx;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
}

.building-list {
  height: calc(100vh - var(--nav-height));

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

.building-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  .card-left {
    .building-icon {
      width: 80rpx;
      height: 80rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 16rpx;
      margin-right: 20rpx;

      &.male {
        background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
      }
      &.female {
        background: linear-gradient(135deg, #EC4899 0%, #BE185D 100%);
      }
      &.mixed {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
    }
  }

  .card-center {
    flex: 1;

    .building-name {
      font-size: 32rpx;
      font-weight: 600;
      color: #1F2937;
      margin-bottom: 12rpx;
    }

    .building-info {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .info-tag {
        font-size: 22rpx;
        padding: 4rpx 12rpx;
        border-radius: 8rpx;
        color: #fff;

        &.male {
          background: #3B82F6;
        }
        &.female {
          background: #EC4899;
        }
        &.mixed {
          background: #667eea;
        }
      }

      .info-text {
        font-size: 24rpx;
        color: #6B7280;
      }
    }
  }

  .card-right {
    display: flex;
    flex-direction: column;
    align-items: center;

    .occupancy-ring {
      position: relative;
      width: 80rpx;
      height: 80rpx;
      display: flex;
      align-items: center;
      justify-content: center;

      .occupancy-text {
        position: absolute;
        font-size: 22rpx;
        font-weight: 600;
        color: #1F2937;
      }
    }

    .occupancy-label {
      font-size: 20rpx;
      color: #9CA3AF;
      margin-top: 4rpx;
    }
  }
}
</style>
