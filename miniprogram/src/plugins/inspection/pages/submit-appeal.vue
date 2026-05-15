<script setup lang="ts">
import type { LongId } from '@core/types'
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import { useAuth } from '@core/stores/auth'
import { usePluginRegistry } from '@core/stores/plugin-registry'

declare const uni: any

const auth = useAuth()
const registry = usePluginRegistry()
const reason = ref('')
const submitting = ref(false)
const itemName = ref('')
let detailId: LongId = ''

onLoad((query: any) => {
  detailId = String(query?.detailId ?? '')
  itemName.value = String(query?.itemName ?? '')
})

const valid = () => {
  if (!detailId) return '评分项 ID 缺失'
  if (!auth.user) return '请先登录'
  const t = reason.value.trim()
  if (t.length < 5) return '申诉理由至少 5 字'
  if (t.length > 500) return '申诉理由最多 500 字'
  return null
}

async function doSubmit() {
  if (submitting.value) return
  const err = valid()
  if (err) { uni.showToast({ title: err, icon: 'none' }); return }
  submitting.value = true
  try {
    const r = await inspectionApi.submitAppeal({
      submissionDetailId: detailId,
      submitterName: auth.user!.name ?? auth.user!.username,
      reason: reason.value.trim()
    })
    registry.bus.emit('inspection.appeal.created', {
      appealId: r.id,
      submissionDetailId: detailId
    })
    uni.showToast({ title: '已提交申诉', icon: 'none' })
    uni.navigateBack({ delta: 1 })
  } catch (e) {
    uni.showToast({
      title: e instanceof BizError ? e.bizMessage : '提交失败',
      icon: 'none'
    })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <view v-if="!detailId" class="state err">评分项 ID 缺失</view>

    <template v-else>
      <view class="hero">
        <view class="title">申诉评分</view>
        <view class="hint">对评分项「{{ itemName || '-' }}」的评分提出异议,管理员审核后会调整</view>
      </view>

      <view class="card">
        <view class="card-title">申诉理由 <text class="hint-inline">(5-500 字)</text></view>
        <textarea
          v-model="reason"
          class="textarea"
          placeholder="详细说明为什么这一项评分不合理"
          maxlength="500"
          auto-height
        />
      </view>

      <view class="actions">
        <wd-button block :loading="submitting" @click="doSubmit">提交申诉</wd-button>
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.state { padding: 80rpx 0; text-align: center; color: #a0aab4; font-size: 26rpx; }
.state.err { color: #e0592a; }
.hero {
  background: #fff; border-radius: 14px; padding: 24rpx;
  margin-bottom: 16rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.title { font-size: 32rpx; font-weight: 700; color: #1a2840; margin-bottom: 12rpx; }
.hint { font-size: 24rpx; color: #5a6a7a; line-height: 1.5; }
.card {
  background: #fff; border-radius: 14px; padding: 24rpx;
  margin-bottom: 16rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.card-title { font-size: 26rpx; font-weight: 700; color: #1a2840; margin-bottom: 12rpx; }
.hint-inline { color: #a0aab4; font-weight: 400; font-size: 22rpx; margin-left: 8rpx; }
.textarea {
  width: 100%; min-height: 280rpx; padding: 16rpx;
  border: 1rpx solid #e0e6ee; border-radius: 8px;
  font-size: 26rpx; color: #1a2840; box-sizing: border-box;
}
.actions { margin-top: 24rpx; }
</style>
