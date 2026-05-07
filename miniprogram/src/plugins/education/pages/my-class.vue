<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { myClassApi } from '../api/my-class'
import { BizError } from '@core/api/request'
import type {
  MyClass,
  MyClassOverview,
  MyClassStudent,
  DormitoryDistribution
} from '../api/types'

declare const uni: any

const classes = ref<MyClass[]>([])
const selectedIdx = ref(0)
const overview = ref<MyClassOverview | null>(null)
const students = ref<MyClassStudent[]>([])
const dorms = ref<DormitoryDistribution[]>([])
const loading = ref(true)
const errMsg = ref('')
const keyword = ref('')
const tab = ref<'students' | 'dorm'>('students')

const current = computed(() => classes.value[selectedIdx.value])

const filteredStudents = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return students.value
  return students.value.filter(s =>
    (s.name || '').toLowerCase().includes(q) ||
    (s.studentNo || '').toLowerCase().includes(q)
  )
})

onShow(async () => {
  loading.value = true
  errMsg.value = ''
  try {
    classes.value = await myClassApi.list()
    if (classes.value.length > 0) {
      await loadCurrent()
    }
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
})

async function loadCurrent() {
  if (!current.value) return
  const id = current.value.id
  try {
    const [ov, st, dm] = await Promise.all([
      myClassApi.overview(id).catch(() => null),
      myClassApi.students(id).catch(() => [] as MyClassStudent[]),
      myClassApi.dormitoryDistribution(id).catch(() => [] as DormitoryDistribution[])
    ])
    overview.value = ov
    students.value = st as MyClassStudent[]
    dorms.value = dm as DormitoryDistribution[]
  } catch (e) {
    uni.showToast({
      title: e instanceof BizError ? e.bizMessage : '加载失败',
      icon: 'none'
    })
  }
}

function onPickerChange(e: any) {
  selectedIdx.value = Number(e.detail.value)
}

function onKeywordInput(e: any) {
  keyword.value = (e?.detail?.value ?? '') as string
}

watch(selectedIdx, () => loadCurrent())
</script>

<template>
  <view class="page">
    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="errMsg" class="state err">{{ errMsg }}</view>
    <view v-else-if="classes.length === 0" class="state empty">您当前不是任何班级的负责人</view>

    <template v-else>
      <view v-if="classes.length > 1" class="selector">
        <picker
          :range="classes.map(c => c.className)"
          :value="selectedIdx"
          @change="onPickerChange"
        >
          <view class="selector-display">{{ current?.className ?? '-' }} ▾</view>
        </picker>
      </view>
      <view v-else class="title-row">
        <text class="class-name">{{ current?.className ?? '-' }}</text>
        <text v-if="current?.myRole" class="my-role">{{ current.myRole }}</text>
      </view>

      <view v-if="overview" class="card overview">
        <view class="stat-grid">
          <view class="stat">
            <view class="num">{{ overview.studentCount ?? 0 }}</view>
            <view class="label">总人数</view>
          </view>
          <view class="stat">
            <view class="num">{{ overview.maleCount ?? 0 }}</view>
            <view class="label">男生</view>
          </view>
          <view class="stat">
            <view class="num">{{ overview.femaleCount ?? 0 }}</view>
            <view class="label">女生</view>
          </view>
          <view class="stat">
            <view class="num">{{ overview.averageScore != null ? overview.averageScore.toFixed(1) : '-' }}</view>
            <view class="label">平均分</view>
          </view>
        </view>
        <view v-if="overview.classRank != null" class="rank-row">
          <text>班级排名 </text>
          <text class="rank">{{ overview.classRank }}/{{ overview.totalClasses ?? '-' }}</text>
          <text
            v-if="overview.scoreTrend != null"
            class="trend"
            :class="{ up: overview.scoreTrend > 0, down: overview.scoreTrend < 0 }"
          >
            {{ overview.scoreTrend > 0 ? '▲' : overview.scoreTrend < 0 ? '▼' : '–' }}
            {{ Math.abs(overview.scoreTrend ?? 0).toFixed(1) }}
          </text>
        </view>
      </view>

      <view class="tabs">
        <view
          class="tab"
          :class="{ active: tab === 'students' }"
          @click="tab = 'students'"
        >学生 ({{ students.length }})</view>
        <view
          class="tab"
          :class="{ active: tab === 'dorm' }"
          @click="tab = 'dorm'"
        >宿舍</view>
      </view>

      <view v-if="tab === 'students'" class="tab-content">
        <input
          class="search"
          type="text"
          placeholder="搜索姓名 / 学号"
          :value="keyword"
          @input="onKeywordInput"
        />
        <view v-if="filteredStudents.length === 0" class="state empty">暂无学生</view>
        <view
          v-for="s in filteredStudents"
          :key="s.id"
          class="row"
        >
          <view class="row-name">{{ s.name }}</view>
          <view class="row-meta">
            {{ s.studentNo }} · {{ s.gender || '-' }} · {{ s.dormitoryName || '未分配' }}
            <text v-if="s.bedNo"> 床{{ s.bedNo }}</text>
          </view>
        </view>
      </view>

      <view v-if="tab === 'dorm'" class="tab-content">
        <view v-if="dorms.length === 0" class="state empty">暂无宿舍分布</view>
        <view v-for="b in dorms" :key="b.buildingId" class="building">
          <view class="building-title">
            <text class="b-name">{{ b.buildingName }}</text>
            <text v-if="b.buildingType" class="b-type">{{ b.buildingType }}</text>
            <text class="b-count">{{ b.studentCount ?? 0 }} 人</text>
          </view>
          <view v-for="r in b.rooms" :key="r.dormitoryId" class="room">
            <view class="room-title">{{ r.roomNo }} ({{ r.studentCount ?? 0 }} 人)</view>
            <view class="bed-row">
              <text
                v-for="bed in r.user_student"
                :key="bed.id"
                class="bed"
              >{{ bed.name }} ({{ bed.bedNo || '-' }})</text>
            </view>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.page { padding: 24rpx; }

.state { padding: 60rpx 0; text-align: center; color: #a0aab4; font-size: 26rpx; }
.state.err { color: #e0592a; }

.selector {
  background: #fff;
  border-radius: 14px;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.selector-display { font-size: 30rpx; font-weight: 700; color: #1a2840; }

.title-row {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 24rpx;
  padding: 0 8rpx;
}
.class-name { font-size: 32rpx; font-weight: 700; color: #1a2840; }
.my-role { font-size: 22rpx; color: #5a6a7a; padding: 4rpx 12rpx; background: #eef3fa; border-radius: 999rpx; }

.card { background: #fff; border-radius: 14px; padding: 24rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.overview { margin-bottom: 24rpx; }
.stat-grid {
  display: flex;
  justify-content: space-between;
}
.stat { flex: 1; text-align: center; border-right: 1rpx solid #eef1f5; }
.stat:last-child { border-right: none; }
.stat .num { font-size: 36rpx; font-weight: 700; color: #1a2840; }
.stat .label { font-size: 22rpx; color: #5a6a7a; margin-top: 6rpx; }
.rank-row {
  margin-top: 20rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #eef1f5;
  font-size: 26rpx;
  color: #5a6a7a;
}
.rank { font-weight: 700; color: #1a2840; }
.trend { margin-left: 16rpx; font-weight: 700; }
.trend.up { color: #2aa876; }
.trend.down { color: #e0592a; }

.tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  background: #fff;
  border-radius: 999rpx;
  font-size: 28rpx;
  color: #5a6a7a;
}
.tab.active { background: #3a7bd5; color: #fff; font-weight: 700; }

.tab-content { display: flex; flex-direction: column; gap: 16rpx; }

.search {
  width: 100%;
  box-sizing: border-box;
  padding: 18rpx 24rpx;
  border-radius: 14px;
  background: #fff;
  font-size: 28rpx;
}

.row {
  background: #fff;
  border-radius: 14px;
  padding: 20rpx 24rpx;
  box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.row-name { font-size: 30rpx; font-weight: 700; color: #1a2840; }
.row-meta { margin-top: 8rpx; font-size: 24rpx; color: #5a6a7a; }

.building {
  background: #fff;
  border-radius: 14px;
  padding: 24rpx;
  box-shadow: 0 2px 6px rgba(58,123,213,0.06);
}
.building-title {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #eef1f5;
}
.b-name { font-size: 30rpx; font-weight: 700; color: #1a2840; }
.b-type { font-size: 22rpx; color: #5a6a7a; padding: 4rpx 12rpx; background: #eef3fa; border-radius: 999rpx; }
.b-count { margin-left: auto; font-size: 24rpx; color: #5a6a7a; }
.room { padding: 14rpx 0; border-bottom: 1rpx dashed #eef1f5; }
.room:last-child { border-bottom: none; }
.room-title { font-size: 26rpx; font-weight: 700; color: #1a2840; }
.bed-row { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 8rpx; }
.bed { font-size: 22rpx; color: #5a6a7a; padding: 4rpx 12rpx; background: #f4f6f9; border-radius: 999rpx; }
</style>
