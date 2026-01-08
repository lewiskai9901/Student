<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 页面头部 -->
    <view class="page-header">
      <view class="header-main">
        <text class="page-title">检查记录</text>
        <text class="page-desc">查看检查详情与扣分信息</text>
      </view>
      <view class="view-toggle" @click="toggleViewMode">
        <text>{{ viewMode === 'all' ? '全部' : '本班' }}</text>
      </view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-bar">
      <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
        <view class="filter-list">
          <view class="filter-item" @click="showPlanPicker = true">
            <text>{{ selectedPlanName || '选择计划' }}</text>
          </view>
          <view class="filter-item" @click="showStatusPicker = true">
            <text>{{ getStatusText(selectedStatus) || '全部状态' }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 列表内容 -->
    <view class="list-container">
      <!-- 加载状态 -->
      <view v-if="loading && records.length === 0" class="state-box">
        <text class="state-text">加载中...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="records.length === 0" class="state-box">
        <text class="state-title">暂无检查记录</text>
        <text class="state-desc">检查发布后将显示在这里</text>
      </view>

      <!-- 记录列表 -->
      <view v-else class="record-list">
        <view
          v-for="record in records"
          :key="record.id"
          class="record-card"
          @click="goToDetail(record)"
        >
          <view class="card-header">
            <view class="card-title-row">
              <text class="card-title">{{ record.checkName }}</text>
              <view class="status-badge" :class="record.status === 1 ? 'published' : 'archived'">
                {{ record.status === 1 ? '已发布' : '已归档' }}
              </view>
            </view>
            <text class="card-code">{{ record.recordCode }}</text>
          </view>

          <view class="card-body">
            <view class="info-row">
              <text class="info-label">检查日期</text>
              <text class="info-value">{{ formatDate(record.checkDate) }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">检查人</text>
              <text class="info-value">{{ record.checkerName }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">发布时间</text>
              <text class="info-value">{{ formatDateTime(record.publishTime) }}</text>
            </view>
          </view>

          <view class="card-footer">
            <view class="stat-item">
              <text class="stat-value">{{ record.totalClasses }}</text>
              <text class="stat-label">班级</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ record.totalDeductionCount || 0 }}</text>
              <text class="stat-label">扣分项</text>
            </view>
            <view class="stat-item">
              <text class="stat-value deduct">-{{ record.totalDeductionScore || record.totalScore || 0 }}</text>
              <text class="stat-label">总扣分</text>
            </view>
          </view>
        </view>

        <!-- 加载更多 -->
        <view v-if="hasMore" class="load-more" @click="loadMore">
          <text>{{ loadingMore ? '加载中...' : '加载更多' }}</text>
        </view>

        <!-- 没有更多 -->
        <view v-else class="no-more">
          <text>— 没有更多了 —</text>
        </view>
      </view>
    </view>

    <!-- 计划选择器 -->
    <uni-popup ref="planPopup" type="bottom" v-if="showPlanPicker">
      <view class="picker-popup">
        <view class="picker-header">
          <text class="picker-cancel" @click="showPlanPicker = false">取消</text>
          <text class="picker-title">选择检查计划</text>
          <text class="picker-confirm" @click="confirmPlanSelect">确定</text>
        </view>
        <scroll-view class="picker-content" scroll-y>
          <view
            class="picker-item"
            :class="{ active: tempSelectedPlanId === null }"
            @click="tempSelectedPlanId = null"
          >
            <text>全部计划</text>
          </view>
          <view
            class="picker-item"
            v-for="plan in planList"
            :key="plan.id"
            :class="{ active: String(tempSelectedPlanId) === String(plan.id) }"
            @click="tempSelectedPlanId = String(plan.id)"
          >
            <text>{{ plan.planName }}</text>
          </view>
        </scroll-view>
      </view>
    </uni-popup>

    <!-- 状态选择器 -->
    <view class="picker-mask" v-if="showStatusPicker" @click="showStatusPicker = false">
      <view class="picker-popup" @click.stop>
        <view class="picker-header">
          <text class="picker-cancel" @click="showStatusPicker = false">取消</text>
          <text class="picker-title">选择状态</text>
          <text class="picker-confirm" @click="confirmStatusSelect">确定</text>
        </view>
        <view class="picker-content">
          <view
            class="picker-item"
            :class="{ active: tempSelectedStatus === null }"
            @click="tempSelectedStatus = null"
          >
            <text>全部状态</text>
          </view>
          <view
            class="picker-item"
            :class="{ active: tempSelectedStatus === 1 }"
            @click="tempSelectedStatus = 1"
          >
            <text>已发布</text>
          </view>
          <view
            class="picker-item"
            :class="{ active: tempSelectedStatus === 2 }"
            @click="tempSelectedStatus = 2"
          >
            <text>已归档</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  getCheckRecordList,
  getMyClassRecords,
  type CheckRecord,
  type RecordQueryParams
} from '@/api/checkRecord'
import { getCheckPlanList, type CheckPlan } from '@/api/checkPlan'

// 视图模式: all-全部记录, myclass-本班记录
const viewMode = ref<'all' | 'myclass'>('all')

// 记录列表
const records = ref<CheckRecord[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const refreshing = ref(false)

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const hasMore = computed(() => records.value.length < total.value)

// 筛选条件
// 注意：planId 使用 string 类型，避免 JavaScript 大数字精度丢失
const selectedPlanId = ref<string | null>(null)
const selectedPlanName = ref('')
const selectedStatus = ref<number | null>(null)
const startDate = ref('')
const endDate = ref('')

// 临时选择（弹窗中使用）
const tempSelectedPlanId = ref<string | null>(null)
const tempSelectedStatus = ref<number | null>(null)

// 弹窗控制
const showPlanPicker = ref(false)
const showStatusPicker = ref(false)
const showDatePicker = ref(false)

// 计划列表（用于选择器）
const planList = ref<CheckPlan[]>([])

// URL参数中的planId
const urlPlanId = ref<string | null>(null)

// 日期范围文本
const dateRangeText = computed(() => {
  if (startDate.value && endDate.value) {
    return `${startDate.value} ~ ${endDate.value}`
  }
  return ''
})

// 页面加载
onLoad((options) => {
  if (options?.planId) {
    // 保持字符串格式，避免大数字精度丢失
    urlPlanId.value = options.planId
    selectedPlanId.value = options.planId
  }
  if (options?.viewMode === 'myclass') {
    viewMode.value = 'myclass'
  }
})

onShow(() => {
  loadPlanList()
  loadRecords()
})

// 加载计划列表
async function loadPlanList() {
  try {
    const res = await getCheckPlanList({ pageNum: 1, pageSize: 100 })
    if (res) {
      planList.value = res.records || []
      // 如果有URL参数，设置计划名称
      if (urlPlanId.value) {
        // 使用 String() 转换比较，确保类型匹配
        const plan = planList.value.find(p => String(p.id) === urlPlanId.value)
        if (plan) {
          selectedPlanName.value = plan.planName
        }
      }
    }
  } catch (error) {
    console.error('加载计划列表失败:', error)
  }
}

// 加载记录列表
async function loadRecords(isRefresh = false) {
  if (isRefresh) {
    pageNum.value = 1
    records.value = []
  }

  loading.value = true
  try {
    const params: RecordQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }

    if (selectedPlanId.value) {
      params.planId = selectedPlanId.value
    }
    if (selectedStatus.value) {
      params.status = selectedStatus.value
    }
    if (startDate.value) {
      params.startDate = startDate.value
    }
    if (endDate.value) {
      params.endDate = endDate.value
    }

    // 根据视图模式调用不同API
    const res = viewMode.value === 'myclass'
      ? await getMyClassRecords(params)
      : await getCheckRecordList(params)

    if (res) {
      if (isRefresh) {
        records.value = res.records || []
      } else {
        records.value = [...records.value, ...(res.records || [])]
      }
      total.value = res.total || 0
    }
  } catch (error: any) {
    console.error('加载记录列表失败:', error)
    // 不重复显示错误提示，request.ts 已经显示过了
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 加载更多
async function loadMore() {
  if (loadingMore.value || !hasMore.value) return

  loadingMore.value = true
  pageNum.value++

  try {
    const params: RecordQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }

    if (selectedPlanId.value) {
      params.planId = selectedPlanId.value
    }
    if (selectedStatus.value) {
      params.status = selectedStatus.value
    }

    const res = viewMode.value === 'myclass'
      ? await getMyClassRecords(params)
      : await getCheckRecordList(params)

    if (res) {
      records.value = [...records.value, ...(res.records || [])]
    }
  } catch (error) {
    console.error('加载更多失败:', error)
    pageNum.value-- // 回退页码
  } finally {
    loadingMore.value = false
  }
}

// 下拉刷新
function onRefresh() {
  refreshing.value = true
  loadRecords(true)
}

// 切换视图模式
function toggleViewMode() {
  viewMode.value = viewMode.value === 'all' ? 'myclass' : 'all'
  loadRecords(true)
}

// 确认计划选择
function confirmPlanSelect() {
  selectedPlanId.value = tempSelectedPlanId.value
  if (tempSelectedPlanId.value) {
    // 使用 String() 比较，避免类型不匹配
    const plan = planList.value.find(p => String(p.id) === tempSelectedPlanId.value)
    selectedPlanName.value = plan?.planName || ''
  } else {
    selectedPlanName.value = ''
  }
  showPlanPicker.value = false
  loadRecords(true)
}

// 确认状态选择
function confirmStatusSelect() {
  selectedStatus.value = tempSelectedStatus.value
  showStatusPicker.value = false
  loadRecords(true)
}

// 获取状态文本
function getStatusText(status: number | null): string {
  if (status === null) return ''
  return status === 1 ? '已发布' : '已归档'
}

// 格式化日期
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

// 格式化日期时间
function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

// 跳转到详情
function goToDetail(record: CheckRecord) {
  const url = viewMode.value === 'myclass'
    ? `/pages/check/record/myclass?id=${record.id}`
    : `/pages/check/record/detail?id=${record.id}`
  uni.navigateTo({ url })
}
</script>

<style lang="scss" scoped>
// 变量
$primary: #4F46E5;
$primary-light: #EEF2FF;
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border: #E5E7EB;
$bg: #F9FAFB;
$white: #FFFFFF;
$danger: #DC2626;

.page {
  min-height: 100vh;
  background: $bg;
}

.safe-top {
  height: env(safe-area-inset-top);
  background: $white;
}

// 页面头部
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: $white;
  padding: 32rpx;
  border-bottom: 1rpx solid $border;

  .header-main {
    .page-title {
      display: block;
      font-size: 40rpx;
      font-weight: 600;
      color: $text-primary;
    }

    .page-desc {
      display: block;
      font-size: 26rpx;
      color: $text-muted;
      margin-top: 8rpx;
    }
  }
}

.view-toggle {
  padding: 12rpx 24rpx;
  background: $primary-light;
  border-radius: 8rpx;

  text {
    font-size: 26rpx;
    color: $primary;
    font-weight: 500;
  }
}

// 筛选栏
.filter-bar {
  background: $white;
  border-bottom: 1rpx solid $border;
}

.filter-scroll {
  white-space: nowrap;
}

.filter-list {
  display: inline-flex;
  padding: 16rpx 24rpx;
  gap: 12rpx;
}

.filter-item {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 24rpx;
  border-radius: 8rpx;
  background: $bg;

  text {
    font-size: 26rpx;
    color: $text-secondary;
  }
}

// 列表容器
.list-container {
  padding: 24rpx;
}

// 状态盒子
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;

  .state-text {
    font-size: 28rpx;
    color: $text-muted;
  }

  .state-title {
    font-size: 32rpx;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 12rpx;
  }

  .state-desc {
    font-size: 26rpx;
    color: $text-muted;
  }
}

// 记录卡片
.record-card {
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;
  margin-bottom: 16rpx;
  overflow: hidden;

  &:active {
    background: $bg;
  }
}

.card-header {
  padding: 24rpx;
  border-bottom: 1rpx solid $border;

  .card-title-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 8rpx;
  }

  .card-title {
    flex: 1;
    font-size: 30rpx;
    font-weight: 600;
    color: $text-primary;
    line-height: 1.4;
  }

  .card-code {
    font-size: 24rpx;
    color: $text-muted;
    font-family: monospace;
  }
}

.status-badge {
  flex-shrink: 0;
  margin-left: 16rpx;
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.published {
    background: #DBEAFE;
    color: #2563EB;
  }

  &.archived {
    background: #F3F4F6;
    color: #9CA3AF;
  }
}

.card-body {
  padding: 16rpx 24rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;

  .info-label {
    font-size: 26rpx;
    color: $text-muted;
  }

  .info-value {
    font-size: 26rpx;
    color: $text-secondary;
  }
}

.card-footer {
  display: flex;
  padding: 20rpx 24rpx;
  background: $bg;
  border-top: 1rpx solid $border;
  justify-content: space-around;
}

.stat-item {
  text-align: center;

  .stat-value {
    display: block;
    font-size: 32rpx;
    font-weight: 600;
    color: $text-primary;
    font-variant-numeric: tabular-nums;

    &.deduct {
      color: $danger;
    }
  }

  .stat-label {
    display: block;
    font-size: 22rpx;
    color: $text-muted;
    margin-top: 4rpx;
  }
}

// 加载更多
.load-more,
.no-more {
  text-align: center;
  padding: 32rpx;

  text {
    font-size: 26rpx;
    color: $text-muted;
  }
}

.load-more text {
  color: $primary;
}

// 选择器弹窗
.picker-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 100;
  display: flex;
  align-items: flex-end;
}

.picker-popup {
  width: 100%;
  background: $white;
  border-radius: 16rpx 16rpx 0 0;
  max-height: 70vh;
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid $border;
}

.picker-cancel {
  font-size: 28rpx;
  color: $text-muted;
}

.picker-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}

.picker-confirm {
  font-size: 28rpx;
  color: $primary;
  font-weight: 500;
}

.picker-content {
  max-height: 500rpx;
  overflow-y: auto;
}

.picker-item {
  padding: 24rpx;
  border-bottom: 1rpx solid $border;

  text {
    font-size: 28rpx;
    color: $text-primary;
  }

  &.active {
    background: $primary-light;

    text {
      color: $primary;
      font-weight: 500;
    }
  }

  &:active {
    background: $bg;
  }
}
</style>
