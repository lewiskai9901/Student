<script setup lang="ts">
import { useAuth } from '../../stores/auth'

declare const uni: any

const auth = useAuth()

function logout() {
  auth.logout()
  uni.reLaunch({ url: '/core/pages/login/index' })
}
</script>

<template>
  <view class="mine">
    <view class="profile">
      <view class="avatar">{{ auth.user?.name?.charAt(0) ?? 'U' }}</view>
      <view class="name">{{ auth.user?.name ?? '未登录' }}</view>
      <view class="role">{{ auth.user?.username ?? '' }}</view>
    </view>
    <wd-button block @click="logout">退出登录</wd-button>
  </view>
</template>

<style lang="scss" scoped>
.mine { padding: 32rpx; }
.profile { background: #fff; border-radius: 14px; padding: 48rpx; text-align: center; margin-bottom: 32rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.avatar { width: 120rpx; height: 120rpx; line-height: 120rpx; border-radius: 50%; background: #3a7bd5; color: #fff; font-size: 48rpx; font-weight: 700; margin: 0 auto 16rpx; }
.name { font-size: 32rpx; font-weight: 700; color: #1a2840; }
.role { font-size: 24rpx; color: #5a6a7a; margin-top: 8rpx; }
</style>
