<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">公告管理</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 公告列表 -->
    <scroll-view
      scroll-y
      class="announcement-list"
      :style="{ marginTop: navBarHeight + 'px' }"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <!-- 加载中 -->
        <view v-if="loading && !announcements.length" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 公告卡片 -->
        <view
          v-for="item in announcements"
          :key="item.id"
          class="announcement-card"
          @click="goToDetail(item)"
        >
          <view class="card-header">
            <view class="title-row">
              <view v-if="item.isTop" class="top-badge">置顶</view>
              <view class="type-badge" :class="'type-' + item.type">
                {{ getTypeName(item.type) }}
              </view>
              <text class="title">{{ item.title }}</text>
            </view>
            <view class="priority-badge" :class="'priority-' + item.priority" v-if="item.priority > 1">
              {{ getPriorityName(item.priority) }}
            </view>
          </view>
          <view class="card-content">
            <text class="content-text">{{ item.content }}</text>
          </view>
          <view class="card-footer">
            <text class="publisher">{{ item.publisherName || '系统' }}</text>
            <text class="time">{{ formatTime(item.publishTime) }}</text>
            <text class="views" v-if="item.viewCount">{{ item.viewCount }}次阅读</text>
          </view>
        </view>

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !announcements.length"
          type="default"
          text="暂无公告"
        />

        <!-- 加载更多 -->
        <LoadMore
          v-if="announcements.length"
          :status="loadMoreStatus"
          @loadmore="loadMore"
        />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getPublishedAnnouncements, type Announcement } from '@/api/announcement'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 数据
const announcements = ref<Announcement[]>([])
const loading = ref(false)
const refreshing = ref(false)
const loadMoreStatus = ref<'loading' | 'loadmore' | 'nomore'>('loadmore')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
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

const loadData = async (isRefresh = false) => {
  if (loading.value) return

  if (isRefresh) {
    queryParams.pageNum = 1
    loadMoreStatus.value = 'loadmore'
  }

  loading.value = true
  try {
    const res = await getPublishedAnnouncements(queryParams)

    const newData = res.records || []
    if (isRefresh) {
      announcements.value = newData
    } else {
      announcements.value = [...announcements.value, ...newData]
    }

    if (newData.length < queryParams.pageSize) {
      loadMoreStatus.value = 'nomore'
    } else {
      loadMoreStatus.value = 'loadmore'
    }
  } catch (error: any) {
    console.error('加载公告数据失败', error)
    // 模拟数据
    announcements.value = [
      {
        id: 1,
        title: '关于开展期末卫生大检查的通知',
        content: '为做好期末学生宿舍卫生工作，学校决定于下周一开始进行全面卫生检查...',
        type: 1,
        priority: 2,
        publisherName: '教务处',
        publishTime: '2024-12-10 10:00',
        isTop: true,
        viewCount: 256,
        status: 1
      },
      {
        id: 2,
        title: '宿舍安全用电须知',
        content: '冬季来临，请各位同学注意用电安全，禁止使用违规电器...',
        type: 2,
        priority: 1,
        publisherName: '后勤处',
        publishTime: '2024-12-08 14:30',
        viewCount: 189,
        status: 1
      },
      {
        id: 3,
        title: '元旦放假通知',
        content: '根据国家规定，元旦放假时间为1月1日至1月3日，共三天...',
        type: 1,
        priority: 1,
        publisherName: '校办公室',
        publishTime: '2024-12-05 09:00',
        viewCount: 421,
        status: 1
      }
    ]
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

const getTypeName = (type: number) => {
  switch (type) {
    case 1: return '系统公告'
    case 2: return '通知'
    case 3: return '活动'
    default: return '公告'
  }
}

const getPriorityName = (priority: number) => {
  switch (priority) {
    case 2: return '重要'
    case 3: return '紧急'
    default: return ''
  }
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return time.split(' ')[0]
}

const goBack = () => {
  uni.navigateBack()
}

const goToDetail = (item: Announcement) => {
  uni.showToast({ title: '详情页开发中', icon: 'none' })
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

.announcement-list {
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

.announcement-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;

  .card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16rpx;

    .title-row {
      flex: 1;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8rpx;

      .top-badge {
        font-size: 20rpx;
        color: #fff;
        background: #EF4444;
        padding: 4rpx 12rpx;
        border-radius: 4rpx;
      }

      .type-badge {
        font-size: 20rpx;
        padding: 4rpx 12rpx;
        border-radius: 4rpx;

        &.type-1 {
          color: #667eea;
          background: rgba(102, 126, 234, 0.1);
        }
        &.type-2 {
          color: #10B981;
          background: rgba(16, 185, 129, 0.1);
        }
        &.type-3 {
          color: #F59E0B;
          background: rgba(245, 158, 11, 0.1);
        }
      }

      .title {
        font-size: 30rpx;
        font-weight: 600;
        color: #1F2937;
      }
    }

    .priority-badge {
      font-size: 20rpx;
      padding: 4rpx 12rpx;
      border-radius: 4rpx;
      flex-shrink: 0;

      &.priority-2 {
        color: #F59E0B;
        background: rgba(245, 158, 11, 0.1);
      }
      &.priority-3 {
        color: #EF4444;
        background: rgba(239, 68, 68, 0.1);
      }
    }
  }

  .card-content {
    margin-bottom: 16rpx;

    .content-text {
      font-size: 26rpx;
      color: #6B7280;
      line-height: 1.6;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    gap: 16rpx;

    .publisher, .time, .views {
      font-size: 22rpx;
      color: #9CA3AF;
    }

    .publisher::after {
      content: '·';
      margin-left: 16rpx;
    }
  }
}
</style>
