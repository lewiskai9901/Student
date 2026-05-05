<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import type { InspTask } from '../api/types'
import { taskStatusLabel, taskStatusColor, formatDateTime } from '../utils/format'

declare const uni: any

const tasks = ref<InspTask[]>([])
const loading = ref(false)
const errMsg = ref('')

async function load() {
  loading.value = true
  errMsg.value = ''
  try {
    tasks.value = await inspectionApi.availableTasks()
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
}

function go(id: number) {
  uni.navigateTo({ url: `/plugins/inspection/pages/task-detail?id=${id}` })
}

onShow(() => { load() })
</script>

<template>
  <view class="page">
    <view class="hint">下面是当前可领取的任务,认领功能将在后续版本上线。</view>

    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="tasks.length === 0" class="state empty">暂无可领任务</view>

    <view v-else class="list">
      <view v-for="t in tasks" :key="t.id" class="item" @click="go(t.id)">
        <view class="row">
          <view class="title">{{ t.title || t.projectName || `任务 #${t.id}` }}</view>
          <view class="badge" :style="{ color: taskStatusColor(t.status), borderColor: taskStatusColor(t.status) }">
            {{ taskStatusLabel(t.status) }}
          </view>
        </view>
        <view class="meta">
          <text>{{ t.orgUnitName || '-' }}</text>
          <text class="dot">·</text>
          <text>截止: {{ formatDateTime(t.deadline) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.hint { font-size: 24rpx; color: #5a6a7a; margin-bottom: 24rpx; }
.state { padding: 80rpx 0; text-align: center; color: #a0aab4; }
.state.err { color: #e0592a; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.item { background: #fff; border-radius: 14px; padding: 24rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.row { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 30rpx; font-weight: 700; color: #1a2840; flex: 1; margin-right: 16rpx; }
.badge { font-size: 22rpx; padding: 4rpx 16rpx; border: 1rpx solid; border-radius: 999rpx; }
.meta { margin-top: 12rpx; font-size: 24rpx; color: #5a6a7a; }
.dot { margin: 0 12rpx; color: #a0aab4; }
</style>
