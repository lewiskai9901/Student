<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { classApi } from '../api/class'
import { studentApi } from '../api/student'
import { BizError } from '@core/api/request'
import type { SchoolClass, Student } from '../api/types'
import { statusLabel } from '../utils/format'

declare const uni: any

const klass = ref<SchoolClass | null>(null)
const students = ref<Student[]>([])
const loading = ref(true)
const studentsLoading = ref(false)
const errMsg = ref('')

onLoad(async (query: any) => {
  const id = Number(query?.id)
  if (!Number.isFinite(id) || id <= 0) {
    errMsg.value = '班级 ID 缺失或非法'
    loading.value = false
    return
  }
  try {
    klass.value = await classApi.byId(id)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
    loading.value = false
    return
  }
  loading.value = false
  studentsLoading.value = true
  try {
    students.value = await studentApi.byClass(id)
  } catch {
    students.value = []
  } finally {
    studentsLoading.value = false
  }
})

function goStudent(sid: number) {
  uni.navigateTo({ url: `/plugins/education/pages/student-detail?id=${sid}` })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <template v-else-if="klass">
      <view class="card">
        <view class="header">
          <view class="name">{{ klass.className }}</view>
          <view v-if="klass.status" class="badge">{{ klass.status }}</view>
        </view>
        <view class="rows">
          <view class="kv"><text class="k">班级编码</text><text class="v">{{ klass.classCode }}</text></view>
          <view class="kv"><text class="k">所属组织</text><text class="v">{{ klass.orgUnitName || '-' }}</text></view>
          <view class="kv"><text class="k">入学年份</text><text class="v">{{ klass.enrollmentYear || '-' }}</text></view>
          <view class="kv"><text class="k">年级级别</text><text class="v">{{ klass.gradeLevel ?? '-' }}</text></view>
          <view class="kv"><text class="k">专业</text><text class="v">{{ klass.majorName || '-' }}</text></view>
          <view class="kv"><text class="k">学制方向</text><text class="v">{{ klass.majorDirectionName || '-' }}</text></view>
          <view class="kv"><text class="k">班主任</text><text class="v">{{ klass.headTeacherName || '-' }}</text></view>
          <view class="kv"><text class="k">人数</text><text class="v">{{ klass.currentSize ?? '-' }} / {{ klass.standardSize ?? '-' }}</text></view>
        </view>
      </view>

      <view class="section-title">班级学生</view>
      <view v-if="studentsLoading" class="state">加载学生…</view>
      <view v-else-if="students.length === 0" class="state empty">暂无学生</view>
      <view v-else class="list">
        <view v-for="s in students" :key="s.id" class="item" @click="goStudent(s.id)">
          <view class="row">
            <view class="title">{{ s.name }}</view>
            <view class="badge">{{ statusLabel(s.status, s.statusText) }}</view>
          </view>
          <view class="meta">
            <text>{{ s.studentNo }}</text>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.state { padding: 60rpx 0; text-align: center; color: #a0aab4; }
.state.err { color: #e0592a; }
.card { background: #fff; border-radius: 14px; padding: 32rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; padding-bottom: 24rpx; border-bottom: 1rpx solid #eef2f7; }
.name { font-size: 38rpx; font-weight: 700; color: #1a2840; }
.badge { font-size: 22rpx; padding: 4rpx 16rpx; border: 1rpx solid #c7d3e1; border-radius: 999rpx; color: #5a6a7a; }
.rows { display: flex; flex-direction: column; gap: 18rpx; }
.kv { display: flex; font-size: 28rpx; }
.k { width: 160rpx; color: #5a6a7a; }
.v { flex: 1; color: #1a2840; }
.section-title { margin: 32rpx 0 16rpx; font-size: 28rpx; font-weight: 700; color: #1a2840; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.item { background: #fff; border-radius: 14px; padding: 20rpx 24rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.row { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 28rpx; font-weight: 600; color: #1a2840; }
.meta { margin-top: 6rpx; font-size: 24rpx; color: #5a6a7a; }
</style>
