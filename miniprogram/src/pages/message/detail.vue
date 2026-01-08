<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">消息详情</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view scroll-y class="content" :style="{ marginTop: navBarHeight + 'px' }">
      <view v-if="message" class="message-detail">
        <view class="detail-header">
          <view class="type-badge" :class="'type-' + message.type">
            {{ getTypeName(message.type) }}
          </view>
          <text class="detail-title">{{ message.title }}</text>
          <text class="detail-time">{{ message.createTime }}</text>
        </view>

        <view class="detail-content">
          <text>{{ message.content }}</text>
        </view>

        <view v-if="message.relatedId" class="detail-action">
          <button class="action-btn" @click="goToRelated">
            查看详情
          </button>
        </view>
      </view>

      <EmptyState v-else type="error" text="消息不存在" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'

interface Message {
  id: number | string
  type: number
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

// 数据
const message = ref<Message | null>(null)
const messageId = ref<string>('')

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
  messageId.value = (currentPage as any).options?.id

  // 模拟数据
  message.value = {
    id: messageId.value,
    type: 2,
    title: '检查提醒',
    content: '今日有卫生检查计划，请做好准备。\n\n检查时间：2024年12月13日 14:00\n检查范围：全校宿舍\n检查重点：卫生清洁、物品摆放、安全用电\n\n请各班班主任提前通知学生做好准备工作。',
    isRead: true,
    createTime: '2024-12-13 08:00:00',
    relatedId: 1,
    relatedType: 'check-plan'
  }
}

const getTypeName = (type: number) => {
  switch (type) {
    case 1: return '系统通知'
    case 2: return '检查提醒'
    case 3: return '申诉通知'
    case 4: return '审批通知'
    default: return '通知'
  }
}

const goBack = () => {
  uni.navigateBack()
}

const goToRelated = () => {
  if (!message.value) return

  switch (message.value.relatedType) {
    case 'check-plan':
      uni.navigateTo({
        url: `/pages/check/plan/detail?id=${message.value.relatedId}`
      })
      break
    case 'check-record':
      uni.navigateTo({
        url: `/pages/check/record/detail?id=${message.value.relatedId}`
      })
      break
    case 'appeal':
      uni.navigateTo({
        url: `/pages/appeal/detail?id=${message.value.relatedId}`
      })
      break
    default:
      uni.showToast({ title: '暂不支持', icon: 'none' })
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
  padding: 20rpx 24rpx;
}

.message-detail {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;

  .detail-header {
    padding: 32rpx;
    border-bottom: 1rpx solid #F3F4F6;

    .type-badge {
      display: inline-block;
      font-size: 22rpx;
      padding: 6rpx 16rpx;
      border-radius: 8rpx;
      margin-bottom: 16rpx;

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
      &.type-4 {
        color: #3B82F6;
        background: rgba(59, 130, 246, 0.1);
      }
    }

    .detail-title {
      font-size: 36rpx;
      font-weight: 600;
      color: #1F2937;
      display: block;
      margin-bottom: 12rpx;
    }

    .detail-time {
      font-size: 24rpx;
      color: #9CA3AF;
    }
  }

  .detail-content {
    padding: 32rpx;

    text {
      font-size: 30rpx;
      color: #4B5563;
      line-height: 1.8;
      white-space: pre-wrap;
    }
  }

  .detail-action {
    padding: 32rpx;
    border-top: 1rpx solid #F3F4F6;

    .action-btn {
      width: 100%;
      height: 88rpx;
      line-height: 88rpx;
      font-size: 32rpx;
      font-weight: 500;
      color: #fff;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 44rpx;
      border: none;
    }
  }
}
</style>
