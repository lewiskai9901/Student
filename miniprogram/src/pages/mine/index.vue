<template>
  <view class="mine-page">
    <view class="status-bar-placeholder" :style="{ height: statusBarHeight + 'px' }" />

    <!-- Profile card -->
    <view class="profile-card">
      <view class="profile-avatar">
        <text class="avatar-text">{{ avatarLetter }}</text>
      </view>
      <view class="profile-info">
        <text class="profile-name">{{ authStore.realName || '未登录' }}</text>
        <text class="profile-role">{{ roleDisplay }}</text>
      </view>
      <wd-icon name="arrow-right" size="32rpx" color="#c0c8d0" />
    </view>

    <!-- Menu -->
    <view class="menu-card">
      <view class="menu-item" @tap="onMenuTap('账号信息')">
        <wd-icon name="user" size="36rpx" color="#3a7bd5" />
        <text class="menu-label">账号信息</text>
        <wd-icon name="arrow-right" size="28rpx" color="#c0c8d0" />
      </view>
      <view class="menu-item" @tap="onMenuTap('设置')">
        <wd-icon name="setting" size="36rpx" color="#5a6a7a" />
        <text class="menu-label">设置</text>
        <wd-icon name="arrow-right" size="28rpx" color="#c0c8d0" />
      </view>
      <view class="menu-item" @tap="onMenuTap('关于')">
        <wd-icon name="info-outline" size="36rpx" color="#5a6a7a" />
        <text class="menu-label">关于</text>
        <wd-icon name="arrow-right" size="28rpx" color="#c0c8d0" />
      </view>
    </view>

    <!-- Logout -->
    <view class="logout-area" v-if="authStore.isLoggedIn">
      <wd-button type="error" plain block custom-class="logout-btn" @click="handleLogout">
        退出登录
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const statusBarHeight = ref(44)
uni.getSystemInfo({ success: (info) => { statusBarHeight.value = info.statusBarHeight || 44 } })

const avatarLetter = computed(() => {
  const name = authStore.realName
  return name ? name.charAt(0) : '?'
})

const roleDisplay = computed(() => {
  const roles = authStore.roles
  return roles.length > 0 ? roles.join(', ') : '暂无角色'
})

function onMenuTap(label: string) {
  uni.showToast({ title: `${label}开发中`, icon: 'none' })
}

function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) authStore.logout()
    },
  })
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background: $bg-page;
}

.status-bar-placeholder { width: 100%; }

.profile-card {
  margin: 16rpx $spacing-page;
  background: $bg-card;
  border-radius: $radius-card;
  padding: 32rpx 28rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  box-shadow: $shadow-card;
}

.profile-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: $color-primary-light;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-text {
  font-size: $font-xl;
  font-weight: 700;
  color: $color-primary;
}

.profile-info {
  flex: 1;
}

.profile-name {
  display: block;
  font-size: $font-lg;
  font-weight: 700;
  color: $text-primary;
}

.profile-role {
  display: block;
  font-size: $font-sm;
  color: $text-placeholder;
  margin-top: 4rpx;
}

.menu-card {
  margin: 20rpx $spacing-page;
  background: $bg-card;
  border-radius: $radius-card;
  overflow: hidden;
  box-shadow: $shadow-card;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx;
  gap: 20rpx;

  & + & {
    border-top: 1rpx solid $border-light;
  }
}

.menu-label {
  flex: 1;
  font-size: $font-md;
  color: $text-primary;
}

.logout-area {
  margin: 40rpx $spacing-page;
}

.logout-btn {
  border-radius: $radius-btn !important;
}
</style>
