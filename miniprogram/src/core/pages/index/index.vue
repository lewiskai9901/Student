<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuth } from '../../stores/auth'
import { usePluginRegistry } from '../../stores/plugin-registry'
import { dispatcher } from '../../plugin/dispatcher'
import type { MenuContribution } from '../../plugin/contribution'

declare const uni: any

const auth = useAuth()
const registry = usePluginRegistry()
const activating = ref(false)

onShow(async () => {
  if (!auth.loggedIn) {
    uni.reLaunch({ url: '/core/pages/login/index' })
    return
  }
  if (registry.activePlugins.length === 0) {
    activating.value = true
    try {
      await registry.activate({
        user: auth.user!,
        permissions: auth.permissions,
        tenantPlugins: auth.enabledPlugins
      })
    } finally {
      activating.value = false
    }
  }
})

const homeMenus = computed<MenuContribution[]>(() => {
  const activeKeys = new Set(registry.activePluginKeys)
  return dispatcher.query<MenuContribution>('menu')
    .filter(m => activeKeys.has(m.key.split('.')[0]))
    .filter(m => m.group !== 'mine-extra')
    .filter(m => !m.perm || auth.permissions.includes(m.perm))
    .sort((a, b) => a.order - b.order)
})

function go(path: string) { uni.navigateTo({ url: path }) }
</script>

<template>
  <view class="home">
    <view class="hello">Hi, {{ auth.user?.name ?? '' }}</view>
    <view class="section-title">功能</view>
    <view class="grid">
      <view v-for="m in homeMenus" :key="m.key" class="grid-item" @click="go(m.path)">
        <view class="icon">{{ (m.icon || '?').charAt(0).toUpperCase() }}</view>
        <view class="label">{{ m.label }}</view>
      </view>
      <view v-if="!activating && homeMenus.length === 0" class="empty">暂无可用功能,请联系管理员开通插件</view>
      <view v-else-if="activating" class="empty">加载中…</view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.home { padding: 32rpx; }
.hello { font-size: 36rpx; font-weight: 700; color: #1a2840; }
.section-title { font-size: 28rpx; color: #5a6a7a; margin: 32rpx 0 16rpx; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16rpx; }
.grid-item { background: #fff; border-radius: 14px; padding: 24rpx; text-align: center; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.icon { width: 64rpx; height: 64rpx; line-height: 64rpx; margin: 0 auto 12rpx; background: rgba(58,123,213,0.1); color: #3a7bd5; border-radius: 14px; font-weight: 700; }
.label { font-size: 24rpx; color: #1a2840; }
.empty { grid-column: span 4; text-align: center; color: #a0aab4; padding: 48rpx 0; }
</style>
