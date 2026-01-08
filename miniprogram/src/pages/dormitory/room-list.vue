<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">{{ buildingName || '房间列表' }}</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-section" :style="{ marginTop: navBarHeight + 'px' }">
      <scroll-view scroll-x class="floor-scroll">
        <view class="floor-tabs">
          <view
            class="floor-tab"
            :class="{ active: selectedFloor === 0 }"
            @click="selectFloor(0)"
          >
            全部
          </view>
          <view
            v-for="floor in floors"
            :key="floor"
            class="floor-tab"
            :class="{ active: selectedFloor === floor }"
            @click="selectFloor(floor)"
          >
            {{ floor }}楼
          </view>
        </view>
      </scroll-view>

      <view class="status-filter">
        <view
          class="status-item"
          :class="{ active: selectedStatus === '' }"
          @click="selectStatus('')"
        >
          全部
        </view>
        <view
          class="status-item available"
          :class="{ active: selectedStatus === 'available' }"
          @click="selectStatus('available')"
        >
          有空位
        </view>
        <view
          class="status-item full"
          :class="{ active: selectedStatus === 'full' }"
          @click="selectStatus('full')"
        >
          已满
        </view>
      </view>
    </view>

    <!-- 房间列表 -->
    <scroll-view
      scroll-y
      class="room-list"
      :style="{ height: listHeight + 'px' }"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <!-- 统计信息 -->
        <view class="stats-bar" v-if="rooms.length">
          <text class="stats-text">共 {{ total }} 间房间</text>
        </view>

        <!-- 加载中 -->
        <view v-if="loading && !rooms.length" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 房间卡片 -->
        <RoomCard
          v-for="room in rooms"
          :key="room.id"
          :room="room"
          @click="goToDetail"
        />

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !rooms.length"
          type="default"
          text="暂无房间数据"
        />

        <!-- 加载更多 -->
        <LoadMore
          v-if="rooms.length"
          :status="loadMoreStatus"
          @loadmore="loadMore"
        />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getDormitoryList, type Dormitory } from '@/api/dormitory'
import RoomCard from '@/components/dormitory/RoomCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)
const listHeight = ref(500)

// 页面参数
const buildingId = ref<string>('')
const buildingName = ref<string>('')

// 数据
const rooms = ref<Dormitory[]>([])
const total = ref(0)
const loading = ref(false)
const refreshing = ref(false)
const loadMoreStatus = ref<'loading' | 'loadmore' | 'nomore'>('loadmore')

// 筛选
const selectedFloor = ref(0)
const selectedStatus = ref('')
const floors = computed(() => {
  // 生成楼层列表
  const maxFloor = 10 // 假设最多10层
  return Array.from({ length: maxFloor }, (_, i) => i + 1)
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

onMounted(() => {
  initSystemInfo()
  loadPageParams()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
  const windowHeight = sysInfo.windowHeight
  listHeight.value = windowHeight - navBarHeight.value - 120
}

const loadPageParams = () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  buildingId.value = (currentPage as any).options?.buildingId || ''
  buildingName.value = decodeURIComponent((currentPage as any).options?.buildingName || '')
}

const loadData = async (isRefresh = false) => {
  if (loading.value) return

  if (isRefresh) {
    queryParams.pageNum = 1
    loadMoreStatus.value = 'loadmore'
  }

  loading.value = true
  try {
    const params: any = {
      ...queryParams,
      buildingId: buildingId.value ? Number(buildingId.value) : undefined,
    }

    if (selectedFloor.value > 0) {
      params.floor = selectedFloor.value
    }

    if (selectedStatus.value === 'available') {
      params.hasAvailableBeds = true
    }

    const res = await getDormitoryList(params)
    let newData = res.records || []
    total.value = res.total || 0

    // 前端筛选满员状态
    if (selectedStatus.value === 'full') {
      newData = newData.filter(r => r.occupiedBeds === r.bedCapacity)
    }

    if (isRefresh) {
      rooms.value = newData
    } else {
      rooms.value = [...rooms.value, ...newData]
    }

    if (newData.length < queryParams.pageSize) {
      loadMoreStatus.value = 'nomore'
    } else {
      loadMoreStatus.value = 'loadmore'
    }
  } catch (error: any) {
    console.error('加载房间数据失败', error)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
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

const selectFloor = (floor: number) => {
  selectedFloor.value = floor
  loadData(true)
}

const selectStatus = (status: string) => {
  selectedStatus.value = status
  loadData(true)
}

const goBack = () => {
  uni.navigateBack()
}

const goToDetail = (room: Dormitory) => {
  uni.navigateTo({
    url: `/pages/dormitory/room-detail?id=${room.id}`
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
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.filter-section {
  background: #fff;
  padding: 16rpx 0;

  .floor-scroll {
    white-space: nowrap;
    padding: 0 24rpx;
    margin-bottom: 16rpx;

    .floor-tabs {
      display: inline-flex;
      gap: 16rpx;

      .floor-tab {
        display: inline-block;
        padding: 12rpx 24rpx;
        font-size: 26rpx;
        color: #6B7280;
        background: #F3F4F6;
        border-radius: 32rpx;

        &.active {
          color: #fff;
          background: #667eea;
        }
      }
    }
  }

  .status-filter {
    display: flex;
    gap: 16rpx;
    padding: 0 24rpx;

    .status-item {
      padding: 10rpx 20rpx;
      font-size: 24rpx;
      color: #6B7280;
      background: #F3F4F6;
      border-radius: 8rpx;

      &.active {
        color: #fff;
        background: #667eea;
      }

      &.available.active {
        background: #10B981;
      }

      &.full.active {
        background: #EF4444;
      }
    }
  }
}

.room-list {
  .list-content {
    padding: 20rpx 24rpx;
  }

  .stats-bar {
    margin-bottom: 16rpx;

    .stats-text {
      font-size: 26rpx;
      color: #6B7280;
    }
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
</style>
