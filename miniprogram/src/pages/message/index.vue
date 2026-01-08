<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">消息中心</text>
        <view class="nav-actions">
          <view class="action-btn" @click="markAllRead" v-if="unreadCount > 0">
            <u-icon name="checkbox-mark" size="20" color="#fff" />
          </view>
        </view>
      </view>
    </view>

    <!-- 消息分类 -->
    <view class="tabs-section" :style="{ marginTop: navBarHeight + 'px' }">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: selectedTab === tab.value }"
        @click="selectTab(tab.value)"
      >
        <text>{{ tab.label }}</text>
        <view class="badge" v-if="tab.count">{{ tab.count > 99 ? '99+' : tab.count }}</view>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view
      scroll-y
      class="message-list"
      :style="{ height: listHeight + 'px' }"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <!-- 加载中 -->
        <view v-if="loading && !messages.length" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 消息卡片 -->
        <view
          v-for="item in messages"
          :key="item.id"
          class="message-card"
          :class="{ unread: !item.isRead }"
          @click="goToDetail(item)"
        >
          <view class="message-icon" :class="'type-' + item.type">
            <u-icon :name="getTypeIcon(item.type)" size="24" color="#fff" />
          </view>
          <view class="message-content">
            <view class="message-header">
              <text class="message-title">{{ item.title }}</text>
              <view class="unread-dot" v-if="!item.isRead"></view>
            </view>
            <text class="message-text">{{ item.content }}</text>
            <text class="message-time">{{ formatTime(item.createTime) }}</text>
          </view>
        </view>

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !messages.length"
          type="default"
          text="暂无消息"
        />

        <!-- 加载更多 -->
        <LoadMore
          v-if="messages.length"
          :status="loadMoreStatus"
          @loadmore="loadMore"
        />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

// 消息类型
interface Message {
  id: number | string
  type: number  // 1:系统 2:检查 3:申诉 4:审批
  title: string
  content: string
  isRead: boolean
  createTime: string
  relatedId?: number
  relatedType?: string
}

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)
const listHeight = ref(500)

// 数据
const messages = ref<Message[]>([])
const loading = ref(false)
const refreshing = ref(false)
const loadMoreStatus = ref<'loading' | 'loadmore' | 'nomore'>('loadmore')
const unreadCount = ref(5)

// 分类
const selectedTab = ref('all')
const tabs = computed(() => [
  { label: '全部', value: 'all', count: unreadCount.value },
  { label: '系统', value: 'system', count: 2 },
  { label: '检查', value: 'check', count: 2 },
  { label: '申诉', value: 'appeal', count: 1 },
])

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
    // TODO: 接入真实API
    await new Promise(resolve => setTimeout(resolve, 500))

    // 模拟数据
    const mockMessages: Message[] = [
      {
        id: 1,
        type: 1,
        title: '系统通知',
        content: '您的账户密码即将过期，请及时修改',
        isRead: false,
        createTime: '2024-12-13 10:30'
      },
      {
        id: 2,
        type: 2,
        title: '检查提醒',
        content: '今日有卫生检查计划，请做好准备',
        isRead: false,
        createTime: '2024-12-13 08:00'
      },
      {
        id: 3,
        type: 3,
        title: '申诉通知',
        content: '您提交的申诉已被审核通过',
        isRead: false,
        createTime: '2024-12-12 16:45'
      },
      {
        id: 4,
        type: 2,
        title: '检查结果',
        content: '12月11日卫生检查结果已发布，您的班级获评良好',
        isRead: true,
        createTime: '2024-12-12 10:00'
      },
      {
        id: 5,
        type: 1,
        title: '公告通知',
        content: '关于开展期末卫生大检查的通知已发布',
        isRead: true,
        createTime: '2024-12-10 09:00'
      }
    ]

    let filteredMessages = mockMessages
    if (selectedTab.value !== 'all') {
      const typeMap: Record<string, number> = { system: 1, check: 2, appeal: 3 }
      filteredMessages = mockMessages.filter(m => m.type === typeMap[selectedTab.value])
    }

    if (isRefresh) {
      messages.value = filteredMessages
    } else {
      messages.value = [...messages.value, ...filteredMessages]
    }

    loadMoreStatus.value = 'nomore'
  } catch (error: any) {
    console.error('加载消息失败', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
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

const selectTab = (tab: string) => {
  selectedTab.value = tab
  loadData(true)
}

const getTypeIcon = (type: number) => {
  switch (type) {
    case 1: return 'bell'
    case 2: return 'file-text'
    case 3: return 'chat'
    case 4: return 'checkmark-circle'
    default: return 'bell'
  }
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return time.split(' ')[0]
}

const markAllRead = () => {
  messages.value.forEach(m => m.isRead = true)
  unreadCount.value = 0
  uni.showToast({ title: '已全部标记已读', icon: 'success' })
}

const goBack = () => {
  uni.navigateBack()
}

const goToDetail = (item: Message) => {
  // 标记已读
  item.isRead = true

  uni.navigateTo({
    url: `/pages/message/detail?id=${item.id}`
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

.tabs-section {
  display: flex;
  background: #fff;
  padding: 16rpx 24rpx;

  .tab-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    padding: 16rpx 0;
    font-size: 28rpx;
    color: #6B7280;
    position: relative;

    &.active {
      color: #667eea;
      font-weight: 500;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background: #667eea;
        border-radius: 2rpx;
      }
    }

    .badge {
      font-size: 20rpx;
      color: #fff;
      background: #EF4444;
      padding: 2rpx 10rpx;
      border-radius: 16rpx;
      min-width: 32rpx;
      text-align: center;
    }
  }
}

.message-list {
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

.message-card {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;

  &.unread {
    background: linear-gradient(90deg, rgba(102, 126, 234, 0.05) 0%, #fff 20%);
  }

  .message-icon {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 16rpx;
    margin-right: 20rpx;
    flex-shrink: 0;

    &.type-1 {
      background: #667eea;
    }
    &.type-2 {
      background: #10B981;
    }
    &.type-3 {
      background: #F59E0B;
    }
    &.type-4 {
      background: #3B82F6;
    }
  }

  .message-content {
    flex: 1;
    min-width: 0;

    .message-header {
      display: flex;
      align-items: center;
      gap: 8rpx;
      margin-bottom: 8rpx;

      .message-title {
        font-size: 30rpx;
        font-weight: 500;
        color: #1F2937;
      }

      .unread-dot {
        width: 12rpx;
        height: 12rpx;
        background: #EF4444;
        border-radius: 50%;
      }
    }

    .message-text {
      font-size: 26rpx;
      color: #6B7280;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      margin-bottom: 8rpx;
    }

    .message-time {
      font-size: 22rpx;
      color: #9CA3AF;
    }
  }
}
</style>
