<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">房间详情</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view scroll-y class="content" :style="{ marginTop: navBarHeight + 'px' }">
      <!-- 加载中 -->
      <view v-if="loading" class="loading-wrapper">
        <u-loading-icon size="40" color="#667eea" />
      </view>

      <template v-else-if="room">
        <!-- 头部信息 -->
        <view class="header-section">
          <view class="room-info">
            <text class="room-no">{{ room.roomNo }}</text>
            <text class="building-name">{{ room.buildingName }} · {{ room.floor }}楼</text>
          </view>
          <StatusTag :type="statusType" :text="statusText" />
        </view>

        <!-- 床位展示 -->
        <view class="beds-section">
          <view class="section-header">
            <text class="section-title">床位信息</text>
            <text class="section-sub">{{ room.occupiedBeds || 0 }}/{{ room.bedCapacity }} 已入住</text>
          </view>
          <view class="beds-grid">
            <view
              v-for="bed in beds"
              :key="bed.bedNo"
              class="bed-item"
              :class="{ occupied: bed.status === 1 }"
              @click="onBedClick(bed)"
            >
              <view class="bed-icon">
                <u-icon
                  :name="bed.status === 1 ? 'account-fill' : 'account'"
                  size="32"
                  :color="bed.status === 1 ? '#667eea' : '#D1D5DB'"
                />
              </view>
              <text class="bed-no">{{ bed.bedNo }}号床</text>
              <text class="bed-status" v-if="bed.status === 1">{{ bed.studentName }}</text>
              <text class="bed-status empty" v-else>空闲</text>
            </view>
          </view>
        </view>

        <!-- 房间信息 -->
        <view class="info-section">
          <view class="section-title">房间信息</view>
          <view class="info-list">
            <view class="info-item">
              <text class="label">房间类型</text>
              <text class="value">{{ room.roomTypeName || '标准间' }}</text>
            </view>
            <view class="info-item">
              <text class="label">使用类型</text>
              <text class="value">{{ room.usageTypeName || '学生宿舍' }}</text>
            </view>
            <view class="info-item">
              <text class="label">所属院系</text>
              <text class="value">{{ room.departmentName || '未分配' }}</text>
            </view>
            <view class="info-item" v-if="room.facilities">
              <text class="label">设施</text>
              <text class="value">{{ room.facilities }}</text>
            </view>
          </view>
        </view>

        <!-- 入住学生列表 -->
        <view class="students-section" v-if="occupiedBeds.length">
          <view class="section-title">入住学生</view>
          <view class="student-list">
            <view
              v-for="bed in occupiedBeds"
              :key="bed.bedNo"
              class="student-item"
              @click="goToStudentDetail(bed)"
            >
              <view class="student-avatar">
                <u-icon name="account-fill" size="24" color="#667eea" />
              </view>
              <view class="student-info">
                <text class="student-name">{{ bed.studentName }}</text>
                <text class="student-no">{{ bed.studentNo }} · {{ bed.bedNo }}号床</text>
              </view>
              <u-icon name="arrow-right" size="16" color="#9CA3AF" />
            </view>
          </view>
        </view>
      </template>

      <EmptyState v-else type="error" text="房间信息不存在" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDormitoryDetail, getDormitoryBeds, type Dormitory, type BedInfo } from '@/api/dormitory'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 数据
const loading = ref(true)
const room = ref<Dormitory | null>(null)
const beds = ref<BedInfo[]>([])
const roomId = ref<string>('')

const statusType = computed(() => {
  const occupied = room.value?.occupiedBeds || 0
  const capacity = room.value?.bedCapacity || 0
  if (occupied === 0) return 'success'
  if (occupied < capacity) return 'warning'
  return 'error'
})

const statusText = computed(() => {
  const occupied = room.value?.occupiedBeds || 0
  const capacity = room.value?.bedCapacity || 0
  if (occupied === 0) return '空闲'
  if (occupied < capacity) return '部分入住'
  return '满员'
})

const occupiedBeds = computed(() => {
  return beds.value.filter(b => b.status === 1)
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
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  roomId.value = (currentPage as any).options?.id

  if (!roomId.value) {
    loading.value = false
    return
  }

  try {
    const [roomRes, bedsRes] = await Promise.all([
      getDormitoryDetail(roomId.value),
      getDormitoryBeds(roomId.value)
    ])
    room.value = roomRes
    beds.value = bedsRes || []

    // 如果没有返回床位数据，根据容量生成空床位
    if (!beds.value.length && room.value?.bedCapacity) {
      beds.value = Array.from({ length: room.value.bedCapacity }, (_, i) => ({
        bedNo: String(i + 1),
        status: 0,
        statusText: '空闲'
      }))
    }
  } catch (error: any) {
    console.error('加载房间详情失败', error)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  uni.navigateBack()
}

const onBedClick = (bed: BedInfo) => {
  if (bed.status === 1 && bed.studentId) {
    goToStudentDetail(bed)
  } else {
    // 空床位可以分配学生
    uni.showToast({ title: '分配功能开发中', icon: 'none' })
  }
}

const goToStudentDetail = (bed: BedInfo) => {
  if (bed.studentId) {
    uni.navigateTo({
      url: `/pages/student/detail?id=${bed.studentId}`
    })
  }
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

  .loading-wrapper {
    display: flex;
    justify-content: center;
    padding: 100rpx 0;
  }
}

.header-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  background: #fff;
  margin-bottom: 20rpx;

  .room-info {
    .room-no {
      font-size: 40rpx;
      font-weight: 600;
      color: #1F2937;
      display: block;
      margin-bottom: 8rpx;
    }

    .building-name {
      font-size: 26rpx;
      color: #6B7280;
    }
  }
}

.beds-section {
  background: #fff;
  padding: 24rpx 32rpx;
  margin-bottom: 20rpx;

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24rpx;

    .section-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #1F2937;
      padding-left: 16rpx;
      border-left: 6rpx solid #667eea;
    }

    .section-sub {
      font-size: 24rpx;
      color: #667eea;
    }
  }

  .beds-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16rpx;

    .bed-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20rpx 12rpx;
      background: #F9FAFB;
      border-radius: 12rpx;
      border: 2rpx solid transparent;

      &.occupied {
        background: rgba(102, 126, 234, 0.05);
        border-color: rgba(102, 126, 234, 0.2);
      }

      .bed-icon {
        width: 56rpx;
        height: 56rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #fff;
        border-radius: 50%;
        margin-bottom: 8rpx;
      }

      .bed-no {
        font-size: 22rpx;
        color: #4B5563;
        margin-bottom: 4rpx;
      }

      .bed-status {
        font-size: 20rpx;
        color: #667eea;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        &.empty {
          color: #10B981;
        }
      }
    }
  }
}

.info-section {
  background: #fff;
  margin-bottom: 20rpx;
  padding: 24rpx 32rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1F2937;
    margin-bottom: 24rpx;
    padding-left: 16rpx;
    border-left: 6rpx solid #667eea;
  }

  .info-list {
    .info-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #F3F4F6;

      &:last-child {
        border-bottom: none;
      }

      .label {
        font-size: 28rpx;
        color: #6B7280;
      }

      .value {
        font-size: 28rpx;
        color: #1F2937;
      }
    }
  }
}

.students-section {
  background: #fff;
  padding: 24rpx 32rpx 32rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1F2937;
    margin-bottom: 24rpx;
    padding-left: 16rpx;
    border-left: 6rpx solid #667eea;
  }

  .student-list {
    .student-item {
      display: flex;
      align-items: center;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #F3F4F6;

      &:last-child {
        border-bottom: none;
      }

      .student-avatar {
        width: 64rpx;
        height: 64rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(102, 126, 234, 0.1);
        border-radius: 50%;
        margin-right: 16rpx;
      }

      .student-info {
        flex: 1;

        .student-name {
          font-size: 28rpx;
          font-weight: 500;
          color: #1F2937;
          display: block;
        }

        .student-no {
          font-size: 24rpx;
          color: #9CA3AF;
        }
      }
    }
  }
}
</style>
