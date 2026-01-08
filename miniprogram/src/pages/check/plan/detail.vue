<template>
  <view class="page">
    <!-- 加载状态 -->
    <view v-if="loading" class="state-box">
      <text class="state-text">加载中...</text>
    </view>

    <!-- 加载失败 -->
    <view v-else-if="!plan" class="state-box">
      <text class="state-title">加载失败</text>
      <text class="state-desc">请检查网络后重试</text>
      <view class="state-btn" @click="loadData">
        <text>重新加载</text>
      </view>
    </view>

    <!-- 计划详情 -->
    <template v-else>
      <!-- 顶部信息区 -->
      <view class="header-section">
        <view class="header-main">
          <text class="plan-name">{{ plan.planName }}</text>
          <view class="status-badge" :class="getStatusClass(plan.status)">
            {{ getStatusText(plan.status) }}
          </view>
        </view>
        <text class="plan-code">{{ plan.planCode }}</text>
      </view>

      <!-- 基本信息 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">基本信息</text>
        </view>
        <view class="info-list">
          <view class="info-item">
            <text class="info-label">检查模板</text>
            <text class="info-value">{{ plan.templateName || '-' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">开始日期</text>
            <text class="info-value">{{ formatDate(plan.startDate) }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">结束日期</text>
            <text class="info-value">{{ formatDate(plan.endDate) }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">加权配置</text>
            <text class="info-value" :class="{ highlight: plan.enableWeight }">
              {{ plan.enableWeight ? '已启用' : '未启用' }}
            </text>
          </view>
          <view class="info-item" v-if="plan.description">
            <text class="info-label">计划描述</text>
            <text class="info-value">{{ plan.description }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">创建人</text>
            <text class="info-value">{{ plan.creatorName || '-' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">创建时间</text>
            <text class="info-value">{{ formatDateTime(plan.createdAt) }}</text>
          </view>
        </view>
      </view>

      <!-- 检查统计 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">检查统计</text>
        </view>
        <view class="stat-grid">
          <view class="stat-card">
            <text class="stat-value">{{ plan.totalChecks || 0 }}</text>
            <text class="stat-label">检查次数</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ plan.totalRecords || 0 }}</text>
            <text class="stat-label">记录条数</text>
          </view>
          <view class="stat-card">
            <text class="stat-value deduct">{{ plan.totalDeductionScore || 0 }}</text>
            <text class="stat-label">总扣分</text>
          </view>
        </view>
      </view>

      <!-- 检查记录 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">检查记录</text>
          <text class="section-more" @click="viewAllRecords" v-if="records.length > 0">查看全部</text>
        </view>

        <view v-if="recordsLoading" class="loading-box">
          <text>加载中...</text>
        </view>

        <view v-else-if="records.length === 0" class="empty-box">
          <text>暂无检查记录</text>
        </view>

        <view v-else class="record-list">
          <view
            class="record-item"
            v-for="record in records"
            :key="record.id"
            @click="goToRecordDetail(record.id)"
          >
            <view class="record-main">
              <view class="record-title-row">
                <text class="record-name">{{ record.checkName }}</text>
                <view class="record-status" :class="record.status === 1 ? 'published' : 'archived'">
                  {{ record.status === 1 ? '已发布' : '已归档' }}
                </view>
              </view>
              <text class="record-meta">{{ formatDate(record.checkDate) }} · {{ record.checkerName }}</text>
            </view>
            <view class="record-stats">
              <text class="stat-item">{{ record.totalClasses }}班</text>
              <text class="stat-item deduct">-{{ record.totalDeductionScore || record.totalScore || 0 }}</text>
            </view>
          </view>
        </view>
      </view>
    </template>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCheckPlanDetail, type CheckPlan, PlanStatus } from '@/api/checkPlan'
import { getCheckRecordList, type CheckRecord } from '@/api/checkRecord'

// 计划ID
const planId = ref<string>('')

// 数据
const plan = ref<CheckPlan | null>(null)
const records = ref<CheckRecord[]>([])

// 加载状态
const loading = ref(true)
const recordsLoading = ref(false)

// 页面加载
onLoad((options) => {
  if (options?.id) {
    planId.value = options.id
    loadData()
  } else {
    loading.value = false
    uni.showToast({ title: '参数错误', icon: 'none' })
  }
})

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await getCheckPlanDetail(planId.value)
    if (res) {
      plan.value = res
      uni.setNavigationBarTitle({ title: res.planName || '计划详情' })
      loadRecords()
    }
  } catch (error: any) {
    console.error('加载计划详情失败:', error)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 加载检查记录
async function loadRecords() {
  recordsLoading.value = true
  try {
    console.log('开始加载检查记录, planId:', planId.value)
    // 注意：planId 必须作为字符串传递，避免 JavaScript 大数字精度丢失
    const res = await getCheckRecordList({
      planId: planId.value as any,  // 保持字符串格式传递
      pageNum: 1,
      pageSize: 5
    })
    console.log('检查记录API响应:', res)
    records.value = res?.records || []
    console.log('解析后的记录:', records.value)
  } catch (error: any) {
    console.error('加载检查记录失败:', error)
    records.value = []
    // 显示具体错误信息以便调试
    uni.showToast({ title: error.message || '加载记录失败', icon: 'none', duration: 3000 })
  } finally {
    recordsLoading.value = false
  }
}

// 获取状态文本
function getStatusText(status: PlanStatus): string {
  const statusMap: Record<PlanStatus, string> = {
    [PlanStatus.DRAFT]: '草稿',
    [PlanStatus.IN_PROGRESS]: '进行中',
    [PlanStatus.FINISHED]: '已结束',
    [PlanStatus.ARCHIVED]: '已归档'
  }
  return statusMap[status] || '未知'
}

// 获取状态样式类
function getStatusClass(status: PlanStatus): string {
  const classMap: Record<PlanStatus, string> = {
    [PlanStatus.DRAFT]: 'draft',
    [PlanStatus.IN_PROGRESS]: 'progress',
    [PlanStatus.FINISHED]: 'finished',
    [PlanStatus.ARCHIVED]: 'archived'
  }
  return classMap[status] || ''
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

// 查看全部记录
function viewAllRecords() {
  uni.navigateTo({ url: `/pages/check/record/index?planId=${planId.value}` })
}

// 跳转到记录详情
function goToRecordDetail(id: string | number) {
  uni.navigateTo({ url: `/pages/check/record/detail?id=${id}` })
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

.safe-bottom {
  height: calc(40rpx + env(safe-area-inset-bottom));
}

// 状态盒子
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 40rpx;

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
    margin-bottom: 32rpx;
  }

  .state-btn {
    padding: 16rpx 48rpx;
    background: $primary;
    border-radius: 8rpx;

    text {
      font-size: 28rpx;
      color: $white;
    }
  }
}

// 顶部信息区
.header-section {
  background: $white;
  padding: 32rpx;
  border-bottom: 1rpx solid $border;

  .header-main {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12rpx;
  }

  .plan-name {
    flex: 1;
    font-size: 36rpx;
    font-weight: 600;
    color: $text-primary;
    line-height: 1.3;
  }

  .plan-code {
    font-size: 26rpx;
    color: $text-muted;
    font-family: monospace;
  }
}

.status-badge {
  flex-shrink: 0;
  margin-left: 16rpx;
  padding: 8rpx 20rpx;
  border-radius: 6rpx;
  font-size: 24rpx;
  font-weight: 500;

  &.draft {
    background: #F3F4F6;
    color: #6B7280;
  }

  &.progress {
    background: #D1FAE5;
    color: #059669;
  }

  &.finished {
    background: #DBEAFE;
    color: #2563EB;
  }

  &.archived {
    background: #F3F4F6;
    color: #9CA3AF;
  }
}

// 通用 section
.section {
  margin: 24rpx;
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid $border;

  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    color: $text-primary;
  }

  .section-more {
    font-size: 24rpx;
    color: $primary;
  }
}

// 信息列表
.info-list {
  padding: 8rpx 24rpx;

  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 16rpx 0;
    border-bottom: 1rpx solid $border;

    &:last-child {
      border-bottom: none;
    }
  }

  .info-label {
    flex-shrink: 0;
    width: 160rpx;
    font-size: 26rpx;
    color: $text-muted;
  }

  .info-value {
    flex: 1;
    font-size: 26rpx;
    color: $text-secondary;
    text-align: right;

    &.highlight {
      color: $primary;
    }
  }
}

// 统计网格
.stat-grid {
  display: flex;
  padding: 24rpx;
  gap: 16rpx;
}

.stat-card {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  background: $bg;
  border-radius: 8rpx;

  .stat-value {
    display: block;
    font-size: 40rpx;
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
    margin-top: 8rpx;
  }
}

// 加载和空状态
.loading-box,
.empty-box {
  padding: 60rpx;
  text-align: center;

  text {
    font-size: 26rpx;
    color: $text-muted;
  }
}

// 记录列表
.record-list {
  .record-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 24rpx;
    border-bottom: 1rpx solid $border;

    &:last-child {
      border-bottom: none;
    }

    &:active {
      background: $bg;
    }
  }
}

.record-main {
  flex: 1;
  min-width: 0;

  .record-title-row {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 8rpx;
  }

  .record-name {
    font-size: 28rpx;
    font-weight: 500;
    color: $text-primary;
  }

  .record-status {
    padding: 4rpx 12rpx;
    border-radius: 4rpx;
    font-size: 20rpx;

    &.published {
      background: #DBEAFE;
      color: #2563EB;
    }

    &.archived {
      background: #F3F4F6;
      color: #9CA3AF;
    }
  }

  .record-meta {
    font-size: 24rpx;
    color: $text-muted;
  }
}

.record-stats {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-left: 16rpx;

  .stat-item {
    font-size: 26rpx;
    color: $text-secondary;

    &.deduct {
      font-weight: 600;
      color: $danger;
    }
  }
}
</style>
