<script setup lang="ts">
import type { LongId } from '@core/types'
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { studentApi } from '../api/student'
import { BizError } from '@core/api/request'
import type { Student } from '../api/types'
import { genderLabel, statusLabel, formatDate } from '../utils/format'

const student = ref<Student | null>(null)
const loading = ref(true)
const errMsg = ref('')

onLoad(async (query: any) => {
  const id = String(query?.id ?? '')
  if (!id) {
    errMsg.value = '学生 ID 缺失或非法'
    loading.value = false
    return
  }
  try {
    student.value = await studentApi.byId(id)
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
    <view v-else-if="student" class="card">
      <view class="header">
        <view class="name">{{ student.name }}</view>
        <view class="badge">{{ statusLabel(student.status, student.statusText) }}</view>
      </view>
      <view class="rows">
        <view class="kv"><text class="k">学号</text><text class="v">{{ student.studentNo }}</text></view>
        <view class="kv"><text class="k">性别</text><text class="v">{{ student.genderText || genderLabel(student.gender) }}</text></view>
        <view class="kv"><text class="k">班级</text><text class="v">{{ student.className || '-' }}</text></view>
        <view class="kv"><text class="k">手机</text><text class="v">{{ student.phone || '-' }}</text></view>
        <view class="kv"><text class="k">邮箱</text><text class="v">{{ student.email || '-' }}</text></view>
        <view class="kv"><text class="k">入学日期</text><text class="v">{{ formatDate(student.enrollmentDate) }}</text></view>
        <view class="kv"><text class="k">预计毕业</text><text class="v">{{ formatDate(student.expectedGraduationDate) }}</text></view>
        <view v-if="student.remark" class="kv"><text class="k">备注</text><text class="v">{{ student.remark }}</text></view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.state { padding: 80rpx 0; text-align: center; color: #a0aab4; }
.state.err { color: #e0592a; }
.card { background: #fff; border-radius: 14px; padding: 32rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; padding-bottom: 24rpx; border-bottom: 1rpx solid #eef2f7; }
.name { font-size: 38rpx; font-weight: 700; color: #1a2840; }
.badge { font-size: 24rpx; padding: 6rpx 18rpx; border: 1rpx solid #c7d3e1; border-radius: 999rpx; color: #5a6a7a; }
.rows { display: flex; flex-direction: column; gap: 18rpx; }
.kv { display: flex; font-size: 28rpx; }
.k { width: 160rpx; color: #5a6a7a; }
.v { flex: 1; color: #1a2840; }
</style>
