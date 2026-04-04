<template>
  <view class="home-page">
    <view class="status-bar-placeholder" :style="{ height: statusBarHeight + 'px' }" />

    <!-- Header -->
    <view class="home-header">
      <view class="header-left">
        <text class="header-org">{{ orgName }}</text>
        <text class="header-name">Hi, {{ authStore.realName || '用户' }}</text>
      </view>
      <view class="header-right">
        <view class="header-icon-btn" @tap="onNotificationTap">
          <wd-icon name="bell" size="36rpx" color="#8a95a5" />
        </view>
        <view class="header-icon-btn" @tap="onScanTap">
          <wd-icon name="scan" size="36rpx" color="#8a95a5" />
        </view>
      </view>
    </view>

    <!-- Search bar -->
    <view class="search-bar" @tap="onSearchTap">
      <wd-icon name="search" size="28rpx" color="#b0bac4" />
      <text class="search-placeholder">搜索功能、人员、通知...</text>
    </view>

    <!-- Module grid -->
    <view class="section-row">
      <text class="section-title">功能</text>
      <text class="section-more" @tap="onAllModulesTap">全部 ></text>
    </view>
    <view class="module-grid">
      <view
        v-for="mod in visibleModules"
        :key="mod.key"
        class="module-item"
        @tap="onModuleTap(mod)"
      >
        <view class="module-icon" :style="{ background: mod.iconColor + '12' }">
          <wd-icon :name="mod.icon" size="40rpx" :color="mod.iconColor" />
        </view>
        <text class="module-name">{{ mod.label }}</text>
      </view>
    </view>

    <!-- Notices -->
    <view class="section-row">
      <text class="section-title">通知</text>
      <text class="section-more" @tap="onMoreNotices">更多 ></text>
    </view>
    <view class="notice-card">
      <view v-if="notices.length === 0" class="notice-empty">
        <text class="notice-empty-text">暂无通知</text>
      </view>
      <view
        v-for="(notice, idx) in notices"
        :key="idx"
        class="notice-item"
      >
        <view class="notice-dot" :class="{ warn: notice.type === 'warn' }" />
        <text class="notice-text">{{ notice.title }}</text>
        <text class="notice-time">{{ notice.time }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { moduleRegistry } from '@/config/modules'
import type { AppModule } from '@/types/module'

const authStore = useAuthStore()
const statusBarHeight = ref(44)
const orgName = ref('通用管理平台')

uni.getSystemInfo({
  success: (info) => {
    statusBarHeight.value = info.statusBarHeight || 44
  },
})

const visibleModules = computed(() => {
  if (!authStore.permissions.length) return moduleRegistry
  return moduleRegistry.filter((m) => authStore.hasPermission(m.key))
})

const notices = ref([
  { title: '系统升级通知：v2.1 版本已发布', time: '10分钟', type: 'info' },
  { title: '新功能上线：支持扫码快捷操作', time: '1小时', type: 'warn' },
  { title: '4月份工作安排已发布，请查阅', time: '3小时', type: 'info' },
])

function onModuleTap(mod: AppModule) {
  if (!mod.path) {
    uni.showToast({ title: '功能开发中', icon: 'none' })
    return
  }
  uni.navigateTo({ url: mod.path })
}

function onNotificationTap() {
  uni.switchTab({ url: '/pages/message/index' })
}

function onScanTap() {
  uni.scanCode({
    success: (res) => {
      uni.showToast({ title: `扫码结果: ${res.result}`, icon: 'none' })
    },
  })
}

function onSearchTap() {
  uni.showToast({ title: '搜索功能开发中', icon: 'none' })
}

function onAllModulesTap() {
  uni.showToast({ title: '全部功能页开发中', icon: 'none' })
}

function onMoreNotices() {
  uni.switchTab({ url: '/pages/message/index' })
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 20rpx;
}

.status-bar-placeholder {
  width: 100%;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx $spacing-page 0;
}

.header-org {
  display: block;
  font-size: $font-xs;
  color: $text-placeholder;
  letter-spacing: 2rpx;
}

.header-name {
  display: block;
  font-size: $font-lg;
  font-weight: 700;
  color: $text-primary;
  margin-top: 4rpx;
}

.header-right {
  display: flex;
  gap: 16rpx;
}

.header-icon-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: $radius-btn;
  background: $bg-card;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-search;
}

.search-bar {
  margin: 24rpx $spacing-page;
  height: 80rpx;
  background: $bg-card;
  border-radius: $radius-btn;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  gap: 16rpx;
  box-shadow: $shadow-search;
}

.search-placeholder {
  font-size: $font-sm;
  color: $text-placeholder;
}

.section-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx $spacing-page 16rpx;
}

.section-title {
  font-size: $font-md;
  font-weight: 700;
  color: $text-primary;
}

.section-more {
  font-size: $font-sm;
  color: $color-primary;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  padding: 0 $spacing-page;
}

.module-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 0;
}

.module-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: $radius-icon;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-card;
  box-shadow: $shadow-card;
}

.module-name {
  font-size: $font-sm;
  color: $text-secondary;
  font-weight: 500;
}

.notice-card {
  margin: 0 $spacing-page;
  background: $bg-card;
  border-radius: $radius-card;
  overflow: hidden;
  box-shadow: $shadow-card;
}

.notice-empty {
  padding: 40rpx;
  text-align: center;
}

.notice-empty-text {
  font-size: $font-sm;
  color: $text-placeholder;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 28rpx;

  & + & {
    border-top: 1rpx solid $border-light;
  }
}

.notice-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 4rpx;
  background: $color-primary;
  flex-shrink: 0;

  &.warn {
    background: $color-danger;
  }
}

.notice-text {
  flex: 1;
  font-size: $font-sm;
  color: $text-secondary;
}

.notice-time {
  font-size: $font-xs;
  color: $text-placeholder;
  flex-shrink: 0;
}
</style>
