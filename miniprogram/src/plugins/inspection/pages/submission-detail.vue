<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import type { InspSubmission, SubmissionDetail, ScoringMode } from '../api/types'
import { scoringModeLabel } from '../utils/format'

declare const uni: any

interface SectionGroup {
  name: string
  items: SubmissionDetail[]
}

const submission = ref<InspSubmission | null>(null)
const details = ref<SubmissionDetail[]>([])
const loading = ref(true)
const submitting = ref(false)
const errMsg = ref('')
const saving = ref<Set<number>>(new Set())
let taskId = 0

onLoad(async (query: any) => {
  taskId = Number(query?.taskId)
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
    const list = await inspectionApi.submissionsByTask(taskId)
    if (!list || list.length === 0) {
      errMsg.value = '未找到检查记录,请先开始任务'
      return
    }
    submission.value = list[0]
    details.value = await inspectionApi.submissionDetails(submission.value.id)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
}

const sections = computed<SectionGroup[]>(() => {
  const map = new Map<string, SubmissionDetail[]>()
  for (const d of details.value) {
    const k = d.sectionName || '未分组'
    if (!map.has(k)) map.set(k, [])
    map.get(k)!.push(d)
  }
  return Array.from(map.entries()).map(([name, items]) => ({ name, items }))
})

const allComplete = computed(() => {
  if (details.value.length === 0) return false
  return details.value.every(d => d.responseValue !== undefined && d.responseValue !== null && d.responseValue !== '')
})

const isInteractive = (mode?: ScoringMode) =>
  mode === 'PASS_FAIL' || mode === 'DIRECT'

const isSaving = (id: number) => saving.value.has(id)

async function setPassFail(detail: SubmissionDetail, pass: boolean) {
  if (isSaving(detail.id)) return
  const responseValue = pass ? 'PASS' : 'FAIL'
  const score = pass ? 100 : 0
  await saveDetail(detail, { responseValue, scoringMode: 'PASS_FAIL', score })
}

async function setDirectScore(detail: SubmissionDetail, e: any) {
  if (isSaving(detail.id)) return
  const raw = e?.detail?.value ?? e?.target?.value ?? ''
  const trimmed = String(raw).trim()
  if (trimmed === '') return
  const num = Number(trimmed)
  if (!Number.isFinite(num)) {
    uni.showToast({ title: '请输入有效数字', icon: 'none' })
    return
  }
  await saveDetail(detail, {
    responseValue: String(num),
    scoringMode: 'DIRECT',
    score: num
  })
}

async function saveDetail(
  detail: SubmissionDetail,
  body: { responseValue?: string; scoringMode?: ScoringMode; score?: number; dimensions?: string }
) {
  saving.value.add(detail.id)
  saving.value = new Set(saving.value)
  try {
    const updated = await inspectionApi.updateDetailResponse(detail.id, body)
    const idx = details.value.findIndex(d => d.id === detail.id)
    if (idx >= 0) details.value[idx] = updated
  } catch (e) {
    uni.showToast({
      title: e instanceof BizError ? e.bizMessage : '保存失败',
      icon: 'none'
    })
  } finally {
    saving.value.delete(detail.id)
    saving.value = new Set(saving.value)
  }
}

async function doComplete() {
  if (!submission.value || submitting.value) return
  if (!allComplete.value) {
    uni.showToast({ title: '尚有评分项未填写', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await inspectionApi.completeSubmission(submission.value.id)
    uni.showToast({ title: '已提交检查表', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 600)
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
    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="!submission" class="state empty">未找到检查记录</view>

    <template v-else>
      <view class="hero">
        <view class="title">填写检查表</view>
        <view class="badge">{{ submission.completedCount ?? 0 }} / {{ submission.itemCount ?? details.length }}</view>
      </view>

      <view v-for="sec in sections" :key="sec.name" class="section">
        <view class="section-title">{{ sec.name }}</view>
        <view v-for="item in sec.items" :key="item.id" class="item">
          <view class="item-head">
            <view class="item-name">{{ item.itemName }}</view>
            <view class="item-mode">{{ scoringModeLabel(item.scoringMode) }}</view>
          </view>

          <view v-if="item.scoringMode === 'PASS_FAIL'" class="row pf">
            <view
              class="pill"
              :class="{ active: item.responseValue === 'PASS', disabled: isSaving(item.id) }"
              @click="setPassFail(item, true)"
            >通过</view>
            <view
              class="pill fail"
              :class="{ active: item.responseValue === 'FAIL', disabled: isSaving(item.id) }"
              @click="setPassFail(item, false)"
            >不通过</view>
          </view>

          <view v-else-if="item.scoringMode === 'DIRECT'" class="row direct">
            <input
              class="num"
              type="digit"
              :value="item.responseValue ?? ''"
              :disabled="isSaving(item.id)"
              placeholder="请输入分数"
              @blur="setDirectScore(item, $event)"
            />
            <text class="suffix">分</text>
          </view>

          <view v-else class="row disabled-row">
            <view class="hint-text">请用 web 端填写此评分项</view>
            <view v-if="item.responseValue" class="cur-val">当前: {{ item.responseValue }}</view>
          </view>
        </view>
      </view>

      <view class="actions">
        <wd-button
          block
          :loading="submitting"
          :disabled="!allComplete"
          @click="doComplete"
        >提交检查表</wd-button>
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; padding-bottom: 120rpx; }
.state { padding: 80rpx 0; text-align: center; color: #a0aab4; }
.state.err { color: #e0592a; }
.hero {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24rpx; padding: 24rpx;
  background: #fff; border-radius: 14px;
  box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.title { font-size: 32rpx; font-weight: 700; color: #1a2840; }
.badge {
  font-size: 24rpx; padding: 4rpx 16rpx;
  border: 1rpx solid #3a7bd5; color: #3a7bd5; border-radius: 999rpx;
}
.section {
  background: #fff; border-radius: 14px; padding: 24rpx;
  margin-bottom: 16rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.section-title {
  font-size: 26rpx; font-weight: 700; color: #3a7bd5;
  margin-bottom: 16rpx; padding-bottom: 8rpx;
  border-bottom: 1rpx solid #f0f3f6;
}
.item {
  padding: 16rpx 0; border-bottom: 1rpx solid #f0f3f6;
}
.item:last-child { border-bottom: 0; }
.item-head {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 12rpx;
}
.item-name { font-size: 26rpx; color: #1a2840; flex: 1; margin-right: 16rpx; }
.item-mode { font-size: 22rpx; color: #5a6a7a; flex-shrink: 0; }
.row { display: flex; align-items: center; gap: 16rpx; }
.row.pf .pill {
  flex: 1; text-align: center; padding: 16rpx 0;
  border: 1rpx solid #e0e6ee; border-radius: 8px;
  font-size: 26rpx; color: #5a6a7a; background: #f7f9fc;
}
.row.pf .pill.active { background: #3a7bd5; color: #fff; border-color: #3a7bd5; }
.row.pf .pill.fail.active { background: #e0592a; border-color: #e0592a; }
.row.pf .pill.disabled { opacity: 0.5; }
.row.direct .num {
  flex: 1; padding: 12rpx 16rpx;
  border: 1rpx solid #e0e6ee; border-radius: 8px;
  font-size: 26rpx; color: #1a2840; background: #fff;
}
.row.direct .suffix { font-size: 26rpx; color: #5a6a7a; }
.row.disabled-row {
  flex-direction: column; align-items: flex-start; gap: 4rpx;
  background: #f7f9fc; padding: 16rpx; border-radius: 8px;
}
.hint-text { font-size: 24rpx; color: #a0aab4; }
.cur-val { font-size: 22rpx; color: #5a6a7a; }
.actions { margin-top: 24rpx; }
</style>
