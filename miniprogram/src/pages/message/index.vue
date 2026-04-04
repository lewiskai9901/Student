<template>
  <view class="message-page">
    <view class="status-bar-placeholder" :style="{ height: statusBarHeight + 'px' }" />
    <view class="page-title">
      <text class="title-text">消息</text>
    </view>
    <view class="empty-state">
      <wd-icon name="chat" size="80rpx" color="#c0c8d0" />
      <text class="empty-text">暂无消息</text>
      <text class="empty-sub">新的通知和消息将显示在这里</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const statusBarHeight = ref(44)
uni.getSystemInfo({ success: (info) => { statusBarHeight.value = info.statusBarHeight || 44 } })
</script>

<style lang="scss" scoped>
.message-page {
  min-height: 100vh;
  background: $bg-page;
}

.status-bar-placeholder { width: 100%; }

.page-title {
  padding: 16rpx $spacing-page;
}

.title-text {
  font-size: $font-xl;
  font-weight: 700;
  color: $text-primary;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
  gap: 16rpx;
}

.empty-text {
  font-size: $font-md;
  color: $text-secondary;
  font-weight: 500;
}

.empty-sub {
  font-size: $font-sm;
  color: $text-placeholder;
}
</style>
