<script setup lang="ts">
import { ref } from 'vue'
import { usePluginRegistry } from '@core/stores/plugin-registry'

const registry = usePluginRegistry()
const clicked = ref(0)

function trigger() {
  registry.bus.emit('demo.hello.clicked', { at: Date.now() })
  clicked.value++
}
</script>

<template>
  <view class="hello">
    <view class="title">Demo Plugin</view>
    <view class="hint">这是一个验证插件骨架的最小演示页面。点击下方按钮触发跨插件事件。</view>
    <wd-button type="primary" block @click="trigger">触发事件</wd-button>
    <view v-if="clicked > 0" class="counter">已触发 {{ clicked }} 次</view>
  </view>
</template>

<style lang="scss" scoped>
.hello { padding: 32rpx; }
.title { font-size: 36rpx; font-weight: 700; color: #1a2840; }
.hint { font-size: 26rpx; color: #5a6a7a; margin: 16rpx 0 32rpx; }
.counter { margin-top: 24rpx; text-align: center; color: #5a6a7a; font-size: 24rpx; }
</style>
