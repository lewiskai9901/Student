<template>
  <view class="status-tag" :class="[`status-${type}`, { plain }]">
    <text>{{ text }}</text>
  </view>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default', // default, success, warning, error, info, primary
  },
  text: {
    type: String,
    required: true
  },
  plain: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  // 默认
  &.status-default {
    background: #F3F4F6;
    color: #6B7280;

    &.plain {
      background: transparent;
      border: 1rpx solid #D1D5DB;
    }
  }

  // 成功
  &.status-success {
    background: #D1FAE5;
    color: #059669;

    &.plain {
      background: transparent;
      border: 1rpx solid #10B981;
      color: #10B981;
    }
  }

  // 警告
  &.status-warning {
    background: #FEF3C7;
    color: #D97706;

    &.plain {
      background: transparent;
      border: 1rpx solid #F59E0B;
      color: #F59E0B;
    }
  }

  // 错误
  &.status-error {
    background: #FEE2E2;
    color: #DC2626;

    &.plain {
      background: transparent;
      border: 1rpx solid #EF4444;
      color: #EF4444;
    }
  }

  // 信息
  &.status-info {
    background: #DBEAFE;
    color: #2563EB;

    &.plain {
      background: transparent;
      border: 1rpx solid #3B82F6;
      color: #3B82F6;
    }
  }

  // 主题色
  &.status-primary {
    background: rgba(102, 126, 234, 0.1);
    color: #667eea;

    &.plain {
      background: transparent;
      border: 1rpx solid #667eea;
    }
  }
}
</style>
