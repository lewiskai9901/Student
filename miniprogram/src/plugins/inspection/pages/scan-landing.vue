<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { capability } from '@core/platform/auto'
import { dispatcher } from '@core/plugin/dispatcher'
import type { ScanResolverContribution } from '@core/plugin/contribution'

declare const uni: any

const scanning = ref(false)
const errMsg = ref('')

async function doScan() {
  scanning.value = true
  errMsg.value = ''
  try {
    const r = await capability.scan()
    const resolvers = dispatcher.query<ScanResolverContribution>('scan-resolver')
      .sort((a, b) => b.priority - a.priority)
    for (const res of resolvers) {
      if (r.code.startsWith(res.prefix)) {
        const action = res.resolve(r.code)
        if (action) {
          const qs = action.params
            ? '?' + Object.entries(action.params).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
            : ''
          uni.navigateTo({ url: action.path + qs })
          return
        }
      }
    }
    errMsg.value = `未识别的二维码: ${r.code.slice(0, 24)}`
  } catch (e: any) {
    if (e?.errMsg && /cancel/i.test(e.errMsg)) {
      errMsg.value = '已取消扫码'
    } else {
      errMsg.value = '扫码失败'
    }
  } finally {
    scanning.value = false
  }
}

onShow(() => { /* user must tap to scan */ })
</script>

<template>
  <view class="page">
    <view class="title">扫码检查</view>
    <view class="hint">扫码贴在被检查对象上的二维码,自动跳转到对应任务详情。</view>
    <wd-button type="primary" block :loading="scanning" @click="doScan">开始扫码</wd-button>
    <view v-if="errMsg" class="err">{{ errMsg }}</view>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 32rpx; }
.title { font-size: 36rpx; font-weight: 700; color: #1a2840; }
.hint { font-size: 26rpx; color: #5a6a7a; margin: 16rpx 0 32rpx; }
.err { margin-top: 24rpx; color: #e0592a; font-size: 26rpx; text-align: center; }
</style>
