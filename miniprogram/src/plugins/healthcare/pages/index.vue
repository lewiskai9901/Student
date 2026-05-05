<script setup lang="ts">
import { ref } from 'vue'
import { usePluginRegistry } from '@core/stores/plugin-registry'
import { capability } from '@core/platform/auto'

declare const uni: any

const registry = usePluginRegistry()
const lastScanned = ref<string>('')

async function scanPatient() {
  try {
    const r = await capability.scan()
    if (r.code.startsWith('PATIENT:')) {
      const patientId = r.code.slice(8)
      lastScanned.value = patientId
      registry.bus.emit('healthcare.patient.scanned', { patientId, scannedAt: Date.now() })
    } else {
      uni.showToast({ title: '不是患者码', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '扫码取消', icon: 'none' })
  }
}
</script>

<template>
  <view class="hc">
    <view class="title">Healthcare Plugin</view>
    <view class="hint">扫患者码触发跨插件事件 (demo 插件会收到)。</view>
    <wd-button type="primary" block @click="scanPatient">扫码</wd-button>
    <view v-if="lastScanned" class="last">最近扫描: {{ lastScanned }}</view>
  </view>
</template>

<style lang="scss" scoped>
.hc { padding: 32rpx; }
.title { font-size: 36rpx; font-weight: 700; color: #1a2840; }
.hint { font-size: 26rpx; color: #5a6a7a; margin: 16rpx 0 32rpx; }
.last { margin-top: 24rpx; color: #5a6a7a; font-size: 24rpx; }
</style>
