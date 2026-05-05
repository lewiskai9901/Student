<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import type { InspAppeal } from '../api/types'
import { appealStatusLabel, appealStatusColor, formatDateTime } from '../utils/format'

declare const uni: any

const appeals = ref<InspAppeal[]>([])
const loading = ref(false)
const errMsg = ref('')

async function load() {
  loading.value = true
  errMsg.value = ''
  try {
    appeals.value = await inspectionApi.myAppeals()
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败,请稍后重试'
  } finally {
    loading.value = false
  }
}

onShow(() => { load() })
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="appeals.length === 0" class="state empty">暂无申诉</view>

    <view v-else class="list">
      <view v-for="a in appeals" :key="a.id" class="item">
        <view class="row">
          <view class="title">申诉 #{{ a.id }}</view>
          <view class="badge" :style="{ color: appealStatusColor(a.status), borderColor: appealStatusColor(a.status) }">
            {{ appealStatusLabel(a.status) }}
          </view>
        </view>
        <view class="reason">{{ a.reason }}</view>
        <view class="meta">
          <text>提交: {{ formatDateTime(a.createdAt) }}</text>
          <text v-if="a.reviewedAt" class="dot">·</text>
          <text v-if="a.reviewedAt">审核: {{ formatDateTime(a.reviewedAt) }}</text>
        </view>
        <view v-if="a.reviewComment" class="comment">审核意见: {{ a.reviewComment }}</view>
        <view v-if="a.finalAdjustment != null" class="meta">最终调整分: {{ a.finalAdjustment }}</view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.state { padding: 80rpx 0; text-align: center; color: #a0aab4; }
.state.err { color: #e0592a; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.item { background: #fff; border-radius: 14px; padding: 24rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.row { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 30rpx; font-weight: 700; color: #1a2840; }
.badge { font-size: 22rpx; padding: 4rpx 16rpx; border: 1rpx solid; border-radius: 999rpx; }
.reason { margin-top: 12rpx; font-size: 26rpx; color: #1a2840; line-height: 1.5; word-break: break-word; }
.meta { margin-top: 12rpx; font-size: 24rpx; color: #5a6a7a; }
.dot { margin: 0 12rpx; color: #a0aab4; }
.comment { margin-top: 12rpx; padding: 12rpx 16rpx; background: rgba(58,123,213,0.06); border-radius: 8rpx; font-size: 24rpx; color: #1a2840; }
</style>
