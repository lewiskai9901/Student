<template>
  <view class="load-more">
    <view v-if="status === 'loading'" class="loading">
      <u-loading-icon size="24" color="#667eea" />
      <text>{{ loadingText }}</text>
    </view>
    <view v-else-if="status === 'nomore'" class="nomore">
      <view class="line"></view>
      <text>{{ nomoreText }}</text>
      <view class="line"></view>
    </view>
    <view v-else-if="status === 'loadmore'" class="loadmore" @click="onLoadMore">
      <text>{{ loadmoreText }}</text>
    </view>
    <view v-else-if="status === 'error'" class="error" @click="onLoadMore">
      <text>{{ errorText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'

export type LoadMoreStatus = 'loading' | 'loadmore' | 'nomore' | 'error'

const props = defineProps({
  status: {
    type: String as () => LoadMoreStatus,
    default: 'loadmore'
  },
  loadingText: {
    type: String,
    default: '正在加载...'
  },
  loadmoreText: {
    type: String,
    default: '点击加载更多'
  },
  nomoreText: {
    type: String,
    default: '没有更多了'
  },
  errorText: {
    type: String,
    default: '加载失败，点击重试'
  }
})

const emit = defineEmits(['loadmore'])

const onLoadMore = () => {
  if (props.status === 'loadmore' || props.status === 'error') {
    emit('loadmore')
  }
}
</script>

<style lang="scss" scoped>
.load-more {
  padding: 32rpx 24rpx;

  .loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16rpx;

    text {
      font-size: 24rpx;
      color: #9CA3AF;
    }
  }

  .nomore {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 24rpx;

    .line {
      width: 80rpx;
      height: 1rpx;
      background: #E5E7EB;
    }

    text {
      font-size: 24rpx;
      color: #9CA3AF;
    }
  }

  .loadmore {
    text-align: center;

    text {
      font-size: 26rpx;
      color: #667eea;
    }
  }

  .error {
    text-align: center;

    text {
      font-size: 26rpx;
      color: #EF4444;
    }
  }
}
</style>
