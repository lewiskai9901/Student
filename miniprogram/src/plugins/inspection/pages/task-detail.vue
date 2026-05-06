<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import { useAuth } from '@core/stores/auth'
import { usePluginRegistry } from '@core/stores/plugin-registry'
import type { InspTask } from '../api/types'
import { taskStatusLabel, taskStatusColor, formatDateTime } from '../utils/format'

declare const uni: any

const auth = useAuth()
const registry = usePluginRegistry()
const task = ref<InspTask | null>(null)
const loading = ref(true)
const submitting = ref(false)
const errMsg = ref('')
let taskId = 0

onLoad(async (query: any) => {
  taskId = Number(query?.id)
  if (!Number.isFinite(taskId) || taskId <= 0) {
    errMsg.value = '任务 ID 缺失或非法'
    loading.value = false
    return
  }
  await reload()
})

async function reload() {
  loading.value = true
  errMsg.value = ''
  try {
    task.value = await inspectionApi.taskById(taskId)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
}

const action = computed<'claim' | 'start' | 'submit' | null>(() => {
  switch (task.value?.status) {
    case 'PENDING': return 'claim'
    case 'CLAIMED': return 'start'
    case 'IN_PROGRESS': return 'submit'
    default: return null
  }
})

const actionLabel = computed(() => {
  if (action.value === 'claim') return '认领任务'
  if (action.value === 'start') return '开始执行'
  if (action.value === 'submit') return '提交检查'
  return ''
})

async function doAction() {
  if (!action.value || submitting.value) return
  if (!auth.user) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  const name = auth.user.name || auth.user.username || ''
  if (!name) { uni.showToast({ title: '未取到当前用户名', icon: 'none' }); return }
  submitting.value = true
  try {
    if (action.value === 'claim') {
      await inspectionApi.claimTask(taskId, name)
      uni.showToast({ title: '已认领', icon: 'none' })
      await reload()
    } else if (action.value === 'start') {
      await inspectionApi.startTask(taskId)
      uni.showToast({ title: '已开始', icon: 'none' })
      await reload()
    } else if (action.value === 'submit') {
      await inspectionApi.submitTask(taskId)
      registry.bus.emit('inspection.task.submitted', {
        taskId,
        submitterId: auth.user.id
      })
      uni.showToast({ title: '已提交', icon: 'none' })
      uni.reLaunch({ url: '/plugins/inspection/pages/my-tasks' })
    }
  } catch (e) {
    if (e instanceof BizError) {
      uni.showToast({ title: e.bizMessage, icon: 'none' })
      await reload()
    } else {
      uni.showToast({ title: '操作失败', icon: 'none' })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="!task" class="state empty">任务不存在</view>

    <template v-else>
      <view class="hero">
        <view class="title">{{ task.title || task.projectName || `任务 #${task.id}` }}</view>
        <view class="badge" :style="{ color: taskStatusColor(task.status), borderColor: taskStatusColor(task.status) }">
          {{ taskStatusLabel(task.status) }}
        </view>
      </view>

      <view class="card">
        <view class="kv"><text class="k">项目</text><text class="v">{{ task.projectName || '-' }}</text></view>
        <view class="kv"><text class="k">模板版本</text><text class="v">v{{ task.templateVersion ?? '-' }}</text></view>
        <view class="kv"><text class="k">检查对象</text><text class="v">{{ task.targetName || '-' }}</text></view>
        <view class="kv"><text class="k">所属单位</text><text class="v">{{ task.orgUnitName || '-' }}</text></view>
        <view class="kv"><text class="k">检查员</text><text class="v">{{ task.inspectorName || '-' }}</text></view>
        <view class="kv"><text class="k">计划开始</text><text class="v">{{ formatDateTime(task.scheduledStartAt) }}</text></view>
        <view class="kv"><text class="k">计划结束</text><text class="v">{{ formatDateTime(task.scheduledEndAt) }}</text></view>
        <view class="kv"><text class="k">截止时间</text><text class="v">{{ formatDateTime(task.deadline) }}</text></view>
      </view>

      <view v-if="action" class="actions">
        <wd-button block :loading="submitting" @click="doAction">{{ actionLabel }}</wd-button>
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
.card { background: #fff; border-radius: 14px; padding: 24rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.kv { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f0f3f6; font-size: 26rpx; }
.kv:last-child { border-bottom: 0; }
.k { width: 160rpx; color: #5a6a7a; }
.v { flex: 1; color: #1a2840; word-break: break-all; }
.actions { margin-top: 24rpx; }
</style>
