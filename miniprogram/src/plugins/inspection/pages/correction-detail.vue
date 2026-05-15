<script setup lang="ts">
import type { LongId } from '@core/types'
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import { uploadWrapped } from '@core/api/upload'
import { capability } from '@core/platform/auto'
import { useAuth } from '@core/stores/auth'
import { usePluginRegistry } from '@core/stores/plugin-registry'
import type { CorrectiveCase, EvidenceType } from '../api/types'
import { caseStatusLabel, caseStatusColor, formatDateTime } from '../utils/format'

declare const uni: any

const auth = useAuth()
const registry = usePluginRegistry()
const c = ref<CorrectiveCase | null>(null)
const loading = ref(true)
const submitting = ref(false)
const errMsg = ref('')
const note = ref('')
interface AttachedEvidence {
  evidenceId: LongId
  fileUrl: string
  fileName: string
}
const evidences = ref<AttachedEvidence[]>([])
const adding = ref(false)
let caseId: LongId = ''

onLoad(async (query: any) => {
  caseId = String(query?.id ?? '')
  if (!caseId) {
    errMsg.value = '整改单 ID 缺失或非法'
    loading.value = false
    return
  }
  await reload()
})

async function reload() {
  loading.value = true
  errMsg.value = ''
  try {
    c.value = await inspectionApi.caseById(caseId)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
}

const action = computed<'start' | 'submit' | null>(() => {
  switch (c.value?.status) {
    case 'PENDING':
    case 'ASSIGNED':
      return 'start'
    case 'IN_PROGRESS':
    case 'REJECTED':
      return 'submit'
    default:
      return null
  }
})

async function doStart() {
  if (submitting.value) return
  if (!auth.user) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  submitting.value = true
  try {
    await inspectionApi.startCaseWork(caseId)
    uni.showToast({ title: '已开始处理', icon: 'none' })
    await reload()
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

function formatNow(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function addPhoto() {
  if (adding.value || submitting.value) return
  if (!auth.user) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  if (!c.value?.submissionId) { uni.showToast({ title: '案例数据不完整', icon: 'none' }); return }

  // 1. takePhoto — silent on cancel
  let local
  try {
    const files = await capability.takePhoto({ count: 1, sourceType: ['camera'], sizeType: ['compressed'] })
    if (!files || files.length === 0) return
    local = files[0]
  } catch {
    return // user cancel / permission denied — silent
  }

  adding.value = true
  try {
    // 2. watermark
    const text = `${formatNow()} ${auth.user.name ?? auth.user.username}`
    let watermarked
    try {
      watermarked = await capability.watermarkImage(local, { canvasId: 'watermark-canvas', text })
    } catch {
      uni.showToast({ title: '水印生成失败,请重试', icon: 'none' })
      return
    }

    // 3. upload
    let uploaded
    try {
      uploaded = await uploadWrapped<{ fileName: string; fileUrl: string; size: number }>(
        watermarked,
        { url: '/files/upload', formData: { directory: 'inspection' } }
      )
    } catch (e) {
      uni.showToast({
        title: e instanceof BizError ? e.bizMessage : '上传失败,请检查网络',
        icon: 'none'
      })
      return
    }

    // 4. addEvidence
    let evidence
    try {
      evidence = await inspectionApi.addEvidence(c.value.submissionId, {
        detailId: c.value.detailId,
        evidenceType: 'PHOTO' as EvidenceType,
        fileName: uploaded.fileName,
        fileUrl: uploaded.fileUrl
      })
    } catch (e) {
      uni.showToast({
        title: e instanceof BizError ? e.bizMessage : '附件登记失败',
        icon: 'none'
      })
      return
    }

    // 5. push to local
    evidences.value.push({
      evidenceId: evidence.id,
      fileUrl: evidence.fileUrl,
      fileName: evidence.fileName
    })
    uni.showToast({ title: '已添加', icon: 'none' })
  } finally {
    adding.value = false
  }
}

function removeEvidence(idx: number) {
  evidences.value.splice(idx, 1)
}

async function doSubmit() {
  if (submitting.value) return
  if (!auth.user) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  const trimmed = note.value.trim()
  if (trimmed.length < 5) { uni.showToast({ title: '整改说明至少 5 字', icon: 'none' }); return }
  if (trimmed.length > 1000) { uni.showToast({ title: '整改说明最多 1000 字', icon: 'none' }); return }
  submitting.value = true
  try {
    const evidenceIds = evidences.value.map(e => e.evidenceId)
    await inspectionApi.submitCorrection(caseId, trimmed, evidenceIds)
    registry.bus.emit('inspection.case.processed', { caseId, action: 'submitted' })
    uni.showToast({ title: '已提交', icon: 'none' })
    uni.reLaunch({ url: '/plugins/inspection/pages/my-corrections' })
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

      <view v-if="action === 'start'" class="actions">
        <wd-button block :loading="submitting" @click="doStart">开始处理</wd-button>
      </view>

      <view v-else-if="action === 'submit'" class="submit-card">
        <view class="card-title">整改说明 <text class="hint">(5-1000 字)</text></view>
        <textarea
          v-model="note"
          class="note"
          placeholder="描述本次整改情况"
          maxlength="1000"
          auto-height
        />
        <view class="card-title attachments-title">附件 <text class="hint">(可选,拍照自动加水印)</text></view>
        <scroll-view scroll-x class="attachments">
          <view v-for="(e, idx) in evidences" :key="e.evidenceId" class="thumb">
            <image :src="e.fileUrl" class="thumb-img" mode="aspectFill" />
            <view class="thumb-remove" @click.stop="removeEvidence(idx)">✕</view>
          </view>
          <view class="thumb thumb-add" :class="{ 'thumb-loading': adding }" @click="addPhoto">
            <text v-if="adding">...</text>
            <text v-else class="plus">+</text>
          </view>
        </scroll-view>
        <view class="actions">
          <wd-button block :loading="submitting" @click="doSubmit">提交整改</wd-button>
        </view>
      </view>
    </template>

    <canvas
      canvas-id="watermark-canvas"
      class="watermark-canvas"
    />
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
.submit-card { background: #fff; border-radius: 14px; padding: 24rpx; margin-top: 16rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.hint { color: #a0aab4; font-weight: 400; font-size: 22rpx; margin-left: 8rpx; }
.note { width: 100%; min-height: 200rpx; padding: 16rpx; border: 1rpx solid #e0e6ee; border-radius: 8px; font-size: 26rpx; color: #1a2840; box-sizing: border-box; }
.attachments-title { margin-top: 16rpx; }
.attachments {
  white-space: nowrap;
  margin-top: 12rpx;
}
.thumb {
  display: inline-block;
  width: 160rpx;
  height: 160rpx;
  margin-right: 12rpx;
  border-radius: 8px;
  position: relative;
  vertical-align: top;
}
.thumb-img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
}
.thumb-remove {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  width: 36rpx;
  height: 36rpx;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border-radius: 50%;
  text-align: center;
  line-height: 36rpx;
  font-size: 22rpx;
}
.thumb-add {
  border: 2rpx dashed #c0c8d2;
  background: #f7f9fc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #a0aab4;
}
.thumb-add .plus { font-size: 44rpx; line-height: 1; }
.thumb-loading { color: #5a6a7a; opacity: 0.6; }
.watermark-canvas {
  position: fixed;
  left: -9999rpx;
  top: -9999rpx;
  width: 1080rpx;
  height: 1080rpx;
  pointer-events: none;
}
</style>
