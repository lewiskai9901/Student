<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import type { CorrectiveCase } from '../api/types'
import { caseStatusLabel, caseStatusColor, formatDateTime } from '../utils/format'

declare const uni: any

const c = ref<CorrectiveCase | null>(null)
const loading = ref(true)
const errMsg = ref('')

onLoad(async (query: any) => {
  const id = Number(query?.id)
  if (!Number.isFinite(id) || id <= 0) {
    errMsg.value = '整改单 ID 缺失或非法'
    loading.value = false
    return
  }
  try {
    c.value = await inspectionApi.caseById(id)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="!c" class="state empty">整改单不存在</view>

    <template v-else>
      <view class="hero">
        <view class="title">{{ c.itemName || c.caseCode || `整改 #${c.id}` }}</view>
        <view class="badge" :style="{ color: caseStatusColor(c.status), borderColor: caseStatusColor(c.status) }">
          {{ caseStatusLabel(c.status) }}
        </view>
      </view>

      <view v-if="c.observation" class="card">
        <view class="card-title">问题描述</view>
        <view class="long-text">{{ c.observation }}</view>
      </view>

      <view v-if="c.rootCause" class="card">
        <view class="card-title">根因分析</view>
        <view class="long-text">{{ c.rootCause }}</view>
      </view>

      <view class="card">
        <view class="kv"><text class="k">单号</text><text class="v">{{ c.caseCode || '-' }}</text></view>
        <view class="kv"><text class="k">项目</text><text class="v">{{ c.projectName || '-' }}</text></view>
        <view class="kv"><text class="k">关联任务</text><text class="v">{{ c.taskId ?? '-' }}</text></view>
        <view class="kv"><text class="k">责任人</text><text class="v">{{ c.assigneeName || '-' }}</text></view>
        <view class="kv"><text class="k">截止时间</text><text class="v">{{ formatDateTime(c.deadline) }}</text></view>
        <view class="kv"><text class="k">驳回次数</text><text class="v">{{ c.rejectCount ?? 0 }}</text></view>
        <view class="kv"><text class="k">升级层级</text><text class="v">{{ c.escalationLevel ?? 0 }}</text></view>
        <view class="kv"><text class="k">创建时间</text><text class="v">{{ formatDateTime(c.createdAt) }}</text></view>
        <view v-if="c.submittedAt" class="kv"><text class="k">提交时间</text><text class="v">{{ formatDateTime(c.submittedAt) }}</text></view>
        <view v-if="c.verifiedAt" class="kv"><text class="k">核实时间</text><text class="v">{{ formatDateTime(c.verifiedAt) }}</text></view>
      </view>

      <view class="actions">
        <wd-button block disabled>提交整改 (Phase D)</wd-button>
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.state { padding: 80rpx 0; text-align: center; color: #a0aab4; }
.state.err { color: #e0592a; }
.hero { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24rpx; padding: 24rpx; background: #fff; border-radius: 14px; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.title { font-size: 32rpx; font-weight: 700; color: #1a2840; flex: 1; margin-right: 16rpx; }
.badge { font-size: 22rpx; padding: 4rpx 16rpx; border: 1rpx solid; border-radius: 999rpx; }
.card { background: #fff; border-radius: 14px; padding: 24rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); margin-bottom: 16rpx; }
.card-title { font-size: 26rpx; font-weight: 700; color: #1a2840; margin-bottom: 12rpx; }
.long-text { font-size: 26rpx; color: #1a2840; line-height: 1.6; word-break: break-word; }
.kv { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f0f3f6; font-size: 26rpx; }
.kv:last-child { border-bottom: 0; }
.k { width: 160rpx; color: #5a6a7a; }
.v { flex: 1; color: #1a2840; word-break: break-all; }
.actions { margin-top: 24rpx; }
</style>
