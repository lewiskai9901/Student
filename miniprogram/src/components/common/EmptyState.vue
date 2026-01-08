<template>
  <view class="empty-state">
    <image
      class="empty-image"
      :src="imageSrc"
      mode="aspectFit"
    />
    <text class="empty-text">{{ text }}</text>
    <text v-if="subText" class="empty-subtext">{{ subText }}</text>
    <view v-if="showAction" class="empty-action" @click="onAction">
      <text>{{ actionText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, defineProps, defineEmits } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default', // default, search, error, network
  },
  text: {
    type: String,
    default: ''
  },
  subText: {
    type: String,
    default: ''
  },
  image: {
    type: String,
    default: ''
  },
  showAction: {
    type: Boolean,
    default: false
  },
  actionText: {
    type: String,
    default: '重试'
  }
})

const emit = defineEmits(['action'])

const defaultImages: Record<string, string> = {
  default: '/static/images/empty-default.png',
  search: '/static/images/empty-search.png',
  error: '/static/images/empty-error.png',
  network: '/static/images/empty-network.png',
}

const defaultTexts: Record<string, string> = {
  default: '暂无数据',
  search: '未找到相关结果',
  error: '加载失败',
  network: '网络连接失败',
}

const imageSrc = computed(() => {
  if (props.image) return props.image
  return defaultImages[props.type] || defaultImages.default
})

const displayText = computed(() => {
  if (props.text) return props.text
  return defaultTexts[props.type] || defaultTexts.default
})

const onAction = () => {
  emit('action')
}
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 40rpx;

  .empty-image {
    width: 240rpx;
    height: 240rpx;
    margin-bottom: 32rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #6B7280;
    margin-bottom: 12rpx;
  }

  .empty-subtext {
    font-size: 24rpx;
    color: #9CA3AF;
    text-align: center;
    line-height: 1.5;
  }

  .empty-action {
    margin-top: 32rpx;
    padding: 16rpx 48rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 40rpx;

    text {
      font-size: 28rpx;
      color: #fff;
    }
  }
}
</style>
