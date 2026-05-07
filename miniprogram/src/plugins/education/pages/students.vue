<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { studentApi } from '../api/student'
import { BizError } from '@core/api/request'
import type { Student } from '../api/types'
import { statusLabel } from '../utils/format'

declare const uni: any

const keyword = ref('')
const records = ref<Student[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const loading = ref(false)
const errMsg = ref('')

let searchTimer: ReturnType<typeof setTimeout> | null = null

async function load(reset = false) {
  if (reset) {
    pageNum.value = 1
    records.value = []
    total.value = 0
  }
  loading.value = true
  errMsg.value = ''
  try {
    const r = await studentApi.list({
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize
    })
    if (reset) {
      records.value = r.records || []
    } else {
      records.value = records.value.concat(r.records || [])
    }
    total.value = r.total || 0
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败,请稍后重试'
  } finally {
    loading.value = false
  }
}

function onKeywordInput(e: any) {
  const v = (e?.detail?.value ?? '') as string
  keyword.value = v
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { load(true) }, 300)
}

function loadMore() {
  if (loading.value) return
  if (records.value.length >= total.value) return
  pageNum.value += 1
  load(false)
}

function go(id: number) {
  uni.navigateTo({ url: `/plugins/education/pages/student-detail?id=${id}` })
}

function initial(name?: string): string {
  if (!name) return '?'
  return name.charAt(0)
}

onShow(() => { load(true) })
</script>

<template>
  <view class="page">
    <view class="search">
      <input
        class="input"
        type="text"
        placeholder="搜索学号 / 姓名"
        :value="keyword"
        @input="onKeywordInput"
      />
    </view>

    <view v-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="!loading && records.length === 0" class="state empty">暂无学生</view>

    <view class="list">
      <view v-for="s in records" :key="s.id" class="item" @click="go(s.id)">
        <view class="avatar">{{ initial(s.name) }}</view>
        <view class="body">
          <view class="row">
            <view class="title">{{ s.name }}</view>
            <view class="badge">{{ statusLabel(s.status, s.statusText) }}</view>
          </view>
          <view class="meta">
            <text>{{ s.studentNo }}</text>
            <text class="dot">·</text>
            <text>{{ s.className || '-' }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="loading" class="state">加载中…</view>
    <view
      v-else-if="records.length > 0 && records.length < total"
      class="more"
      @click="loadMore"
    >加载更多</view>
    <view
      v-else-if="records.length > 0 && records.length >= total"
      class="state empty"
    >已加载全部 ({{ total }})</view>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.search { margin-bottom: 24rpx; }
.input { width: 100%; box-sizing: border-box; padding: 18rpx 24rpx; border-radius: 14px; background: #fff; font-size: 28rpx; }
.state { padding: 60rpx 0; text-align: center; color: #a0aab4; }
.state.err { color: #e0592a; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.item { display: flex; align-items: center; gap: 24rpx; background: #fff; border-radius: 14px; padding: 20rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; background: #d6e4fb; color: #3a7bd5; display: flex; align-items: center; justify-content: center; font-size: 32rpx; font-weight: 700; }
.body { flex: 1; min-width: 0; }
.row { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 30rpx; font-weight: 700; color: #1a2840; }
.badge { font-size: 22rpx; padding: 4rpx 16rpx; border: 1rpx solid #c7d3e1; border-radius: 999rpx; color: #5a6a7a; }
.meta { margin-top: 8rpx; font-size: 24rpx; color: #5a6a7a; }
.dot { margin: 0 12rpx; color: #a0aab4; }
.more { margin-top: 24rpx; padding: 24rpx 0; text-align: center; background: #fff; border-radius: 14px; color: #3a7bd5; font-size: 26rpx; }
</style>
