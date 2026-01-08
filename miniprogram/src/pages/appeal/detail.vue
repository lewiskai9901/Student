<template>
  <view class="appeal-detail">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading-container">
      <view class="loading-spinner"></view>
      <text class="loading-text">加载中...</text>
    </view>

    <template v-else-if="appeal">
      <!-- 状态卡片 -->
      <view class="status-card" :class="getStatusClass(appeal.status)">
        <view class="status-icon">
          {{ getStatusIcon(appeal.status) }}
        </view>
        <view class="status-info">
          <text class="status-text">{{ getStatusText(appeal.status) }}</text>
          <text class="status-desc">{{ getStatusDesc(appeal.status) }}</text>
        </view>
      </view>

      <!-- 申诉基本信息 -->
      <view class="info-card">
        <view class="card-title">申诉信息</view>
        <view class="info-list">
          <view class="info-item">
            <text class="label">申诉编号</text>
            <text class="value code">{{ appeal.appealCode }}</text>
          </view>
          <view class="info-item">
            <text class="label">申诉类型</text>
            <text class="value">{{ getAppealTypeText(appeal.appealType) }}</text>
          </view>
          <view class="info-item">
            <text class="label">申诉人</text>
            <text class="value">{{ appeal.appellantName }} ({{ appeal.appellantRole }})</text>
          </view>
          <view class="info-item">
            <text class="label">申诉时间</text>
            <text class="value">{{ formatDateTime(appeal.appealTime) }}</text>
          </view>
          <view class="info-item" v-if="appeal.appealDeadline">
            <text class="label">截止时间</text>
            <text class="value">{{ formatDateTime(appeal.appealDeadline) }}</text>
          </view>
        </view>
      </view>

      <!-- 原扣分信息 -->
      <view class="info-card">
        <view class="card-title">原扣分信息</view>
        <view class="deduction-info">
          <view class="deduction-header">
            <text class="class-name">{{ appeal.className }}</text>
            <text class="category-tag">{{ appeal.categoryName }}</text>
          </view>
          <text class="item-name">{{ appeal.itemName }}</text>
          <view class="score-row">
            <text class="score-label">原扣分:</text>
            <text class="score-value negative">-{{ appeal.originalScore }}分</text>
          </view>
          <view class="link-info" v-if="appeal.linkInfo">
            <text>关联信息: {{ appeal.linkInfo }}</text>
          </view>
          <view class="original-remark" v-if="appeal.originalRemark">
            <text class="remark-label">原备注:</text>
            <text class="remark-text">{{ appeal.originalRemark }}</text>
          </view>
        </view>

        <!-- 原图片 -->
        <view class="photo-section" v-if="appeal.originalPhotoUrls?.length">
          <text class="photo-title">原检查图片</text>
          <view class="photo-list">
            <image
              v-for="(url, index) in appeal.originalPhotoUrls"
              :key="index"
              :src="url"
              mode="aspectFill"
              @click="previewImage(appeal.originalPhotoUrls!, index)"
            />
          </view>
        </view>
      </view>

      <!-- 申诉内容 -->
      <view class="info-card">
        <view class="card-title">申诉内容</view>
        <view class="appeal-content">
          <view class="reason-section">
            <text class="section-label">申诉理由</text>
            <text class="reason-text">{{ appeal.appealReason }}</text>
          </view>
          <view class="expected-section" v-if="appeal.expectedScore !== undefined">
            <text class="section-label">期望分数</text>
            <text class="expected-score">-{{ appeal.expectedScore }}分</text>
          </view>
        </view>

        <!-- 证据图片 -->
        <view class="photo-section" v-if="appeal.evidenceUrls?.length">
          <text class="photo-title">申诉证据</text>
          <view class="photo-list">
            <image
              v-for="(url, index) in appeal.evidenceUrls"
              :key="index"
              :src="url"
              mode="aspectFill"
              @click="previewImage(appeal.evidenceUrls!, index)"
            />
          </view>
        </view>
      </view>

      <!-- 审核信息 -->
      <view class="info-card" v-if="appeal.status !== AppealStatus.PENDING">
        <view class="card-title">审核信息</view>
        <view class="review-info">
          <view class="info-item" v-if="appeal.finalReviewerName">
            <text class="label">审核人</text>
            <text class="value">{{ appeal.finalReviewerName }}</text>
          </view>
          <view class="info-item" v-if="appeal.finalReviewTime">
            <text class="label">审核时间</text>
            <text class="value">{{ formatDateTime(appeal.finalReviewTime) }}</text>
          </view>
          <view class="info-item" v-if="appeal.adjustedScore !== undefined">
            <text class="label">调整后分数</text>
            <text class="value success">-{{ appeal.adjustedScore }}分</text>
          </view>
          <view class="info-item" v-if="appeal.scoreChange !== undefined">
            <text class="label">分数变化</text>
            <text class="value" :class="appeal.scoreChange > 0 ? 'success' : ''">
              {{ appeal.scoreChange > 0 ? '+' : '' }}{{ appeal.scoreChange }}分
            </text>
          </view>
          <view class="review-opinion" v-if="appeal.finalReviewOpinion">
            <text class="opinion-label">审核意见</text>
            <text class="opinion-text">{{ appeal.finalReviewOpinion }}</text>
          </view>
        </view>
      </view>

      <!-- 公示信息 -->
      <view class="info-card" v-if="appeal.status === AppealStatus.PUBLICITY || appeal.status === AppealStatus.EFFECTIVE">
        <view class="card-title">公示信息</view>
        <view class="publicity-info">
          <view class="info-item">
            <text class="label">公示开始</text>
            <text class="value">{{ formatDateTime(appeal.publicityStartTime) }}</text>
          </view>
          <view class="info-item">
            <text class="label">公示结束</text>
            <text class="value">{{ formatDateTime(appeal.publicityEndTime) }}</text>
          </view>
          <view class="info-item">
            <text class="label">公示天数</text>
            <text class="value">{{ appeal.publicityDays }}天</text>
          </view>
          <view class="info-item" v-if="appeal.effectiveTime">
            <text class="label">生效时间</text>
            <text class="value success">{{ formatDateTime(appeal.effectiveTime) }}</text>
          </view>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-bar" v-if="appeal.status === AppealStatus.PENDING">
        <view class="withdraw-btn" @click="handleWithdraw">
          <text>撤销申诉</text>
        </view>
      </view>
    </template>

    <!-- 加载失败 -->
    <view v-else class="error-container">
      <text class="error-text">加载失败</text>
      <view class="retry-btn" @click="loadData">
        <text>重新加载</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getAppealDetail,
  withdrawAppeal,
  type Appeal,
  AppealStatus,
  AppealType
} from '@/api/appeal'

// 申诉ID
const appealId = ref<string>('')

// 数据
const appeal = ref<Appeal | null>(null)

// 加载状态
const loading = ref(true)

// 页面加载
onLoad((options) => {
  if (options?.id) {
    appealId.value = options.id
    loadData()
  } else {
    loading.value = false
    uni.showToast({
      title: '参数错误',
      icon: 'none'
    })
  }
})

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await getAppealDetail(appealId.value)
    if (res.code === 200) {
      appeal.value = res.data
      uni.setNavigationBarTitle({
        title: '申诉详情'
      })
    } else {
      uni.showToast({
        title: res.message || '加载失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('加载申诉详情失败:', error)
    uni.showToast({
      title: '网络错误',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 获取状态文本
function getStatusText(status: AppealStatus): string {
  const statusMap: Record<AppealStatus, string> = {
    [AppealStatus.PENDING]: '待审核',
    [AppealStatus.APPROVED]: '已通过',
    [AppealStatus.REJECTED]: '已驳回',
    [AppealStatus.WITHDRAWN]: '已撤销',
    [AppealStatus.EXPIRED]: '已过期',
    [AppealStatus.PUBLICITY]: '公示中',
    [AppealStatus.EFFECTIVE]: '已生效'
  }
  return statusMap[status] || '未知'
}

// 获取状态描述
function getStatusDesc(status: AppealStatus): string {
  const descMap: Record<AppealStatus, string> = {
    [AppealStatus.PENDING]: '您的申诉正在等待审核',
    [AppealStatus.APPROVED]: '您的申诉已通过审核',
    [AppealStatus.REJECTED]: '您的申诉已被驳回',
    [AppealStatus.WITHDRAWN]: '您已撤销本次申诉',
    [AppealStatus.EXPIRED]: '申诉已超时未处理',
    [AppealStatus.PUBLICITY]: '申诉结果正在公示中',
    [AppealStatus.EFFECTIVE]: '申诉结果已生效'
  }
  return descMap[status] || ''
}

// 获取状态图标
function getStatusIcon(status: AppealStatus): string {
  const iconMap: Record<AppealStatus, string> = {
    [AppealStatus.PENDING]: '&#9203;',     // 时钟
    [AppealStatus.APPROVED]: '&#9989;',    // 勾选
    [AppealStatus.REJECTED]: '&#10060;',   // 叉
    [AppealStatus.WITHDRAWN]: '&#128473;', // 返回
    [AppealStatus.EXPIRED]: '&#9200;',     // 闹钟
    [AppealStatus.PUBLICITY]: '&#128227;', // 喇叭
    [AppealStatus.EFFECTIVE]: '&#127881;'  // 彩带
  }
  return iconMap[status] || '&#128203;'
}

// 获取状态样式
function getStatusClass(status: AppealStatus): string {
  const classMap: Record<AppealStatus, string> = {
    [AppealStatus.PENDING]: 'pending',
    [AppealStatus.APPROVED]: 'approved',
    [AppealStatus.REJECTED]: 'rejected',
    [AppealStatus.WITHDRAWN]: 'withdrawn',
    [AppealStatus.EXPIRED]: 'expired',
    [AppealStatus.PUBLICITY]: 'publicity',
    [AppealStatus.EFFECTIVE]: 'effective'
  }
  return classMap[status] || ''
}

// 获取申诉类型文本
function getAppealTypeText(type: AppealType): string {
  const typeMap: Record<AppealType, string> = {
    [AppealType.SCORE]: '分数异议',
    [AppealType.FACT]: '事实异议',
    [AppealType.PROCEDURE]: '程序异议'
  }
  return typeMap[type] || '未知'
}

// 格式化日期时间
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

// 预览图片
function previewImage(urls: string[], current: number) {
  uni.previewImage({
    urls,
    current: urls[current]
  })
}

// 撤销申诉
function handleWithdraw() {
  uni.showModal({
    title: '确认撤销',
    content: '确定要撤销本次申诉吗？撤销后不可恢复。',
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '撤销中...' })
          const result = await withdrawAppeal(appealId.value)
          uni.hideLoading()
          if (result.code === 200) {
            uni.showToast({
              title: '撤销成功',
              icon: 'success'
            })
            // 重新加载数据
            loadData()
          } else {
            uni.showToast({
              title: result.message || '撤销失败',
              icon: 'none'
            })
          }
        } catch (error) {
          uni.hideLoading()
          uni.showToast({
            title: '网络错误',
            icon: 'none'
          })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.appeal-detail {
  min-height: 100vh;
  background: #f5f6fa;
  padding: 24rpx;
  padding-bottom: 140rpx;
  box-sizing: border-box;
}

// 加载状态
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;
}

.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 4rpx solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 20rpx;
  color: #999;
  font-size: 28rpx;
}

// 错误状态
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;
}

.error-text {
  color: #999;
  font-size: 28rpx;
  margin-bottom: 30rpx;
}

.retry-btn {
  padding: 16rpx 40rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 30rpx;

  text {
    color: #fff;
    font-size: 28rpx;
  }
}

// 状态卡片
.status-card {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-radius: 20rpx;
  margin-bottom: 20rpx;

  &.pending {
    background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  }

  &.approved, &.effective {
    background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  }

  &.rejected {
    background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
  }

  &.withdrawn, &.expired {
    background: linear-gradient(135deg, #f5f5f5 0%, #eeeeee 100%);
  }

  &.publicity {
    background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  }
}

.status-icon {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  margin-right: 20rpx;
}

.status-info {
  flex: 1;

  .status-text {
    display: block;
    font-size: 36rpx;
    font-weight: 700;
    color: #333;
    margin-bottom: 8rpx;
  }

  .status-desc {
    font-size: 26rpx;
    color: #666;
  }
}

// 信息卡片
.info-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  margin-bottom: 20rpx;
}

.info-list {
  .info-item {
    display: flex;
    justify-content: space-between;
    padding: 16rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }
  }

  .label {
    font-size: 28rpx;
    color: #999;
  }

  .value {
    font-size: 28rpx;
    color: #333;

    &.code {
      font-family: monospace;
      color: #667eea;
    }

    &.success {
      color: #4caf50;
    }
  }
}

// 扣分信息
.deduction-info {
  .deduction-header {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 12rpx;
  }

  .class-name {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
  }

  .category-tag {
    padding: 6rpx 16rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12rpx;
    font-size: 22rpx;
    color: #fff;
  }

  .item-name {
    display: block;
    font-size: 28rpx;
    color: #333;
    margin-bottom: 12rpx;
  }

  .score-row {
    display: flex;
    align-items: center;
    gap: 8rpx;
    margin-bottom: 12rpx;

    .score-label {
      font-size: 26rpx;
      color: #999;
    }

    .score-value {
      font-size: 30rpx;
      font-weight: 600;

      &.negative {
        color: #f44336;
      }
    }
  }

  .link-info {
    margin-bottom: 12rpx;

    text {
      font-size: 26rpx;
      color: #666;
    }
  }

  .original-remark {
    padding: 16rpx;
    background: #f9f9f9;
    border-radius: 12rpx;

    .remark-label {
      font-size: 24rpx;
      color: #999;
      margin-right: 8rpx;
    }

    .remark-text {
      font-size: 26rpx;
      color: #666;
    }
  }
}

// 图片区域
.photo-section {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;

  .photo-title {
    display: block;
    font-size: 26rpx;
    color: #999;
    margin-bottom: 16rpx;
  }

  .photo-list {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;

    image {
      width: 160rpx;
      height: 160rpx;
      border-radius: 12rpx;
      object-fit: cover;
    }
  }
}

// 申诉内容
.appeal-content {
  .reason-section,
  .expected-section {
    margin-bottom: 20rpx;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .section-label {
    display: block;
    font-size: 26rpx;
    color: #999;
    margin-bottom: 12rpx;
  }

  .reason-text {
    font-size: 28rpx;
    color: #333;
    line-height: 1.6;
    padding: 16rpx;
    background: #f9f9f9;
    border-radius: 12rpx;
  }

  .expected-score {
    font-size: 36rpx;
    font-weight: 600;
    color: #4caf50;
  }
}

// 审核信息
.review-info {
  .info-item {
    display: flex;
    justify-content: space-between;
    padding: 12rpx 0;

    .label {
      font-size: 26rpx;
      color: #999;
    }

    .value {
      font-size: 26rpx;
      color: #333;

      &.success {
        color: #4caf50;
        font-weight: 500;
      }
    }
  }

  .review-opinion {
    margin-top: 16rpx;
    padding: 16rpx;
    background: #f9f9f9;
    border-radius: 12rpx;

    .opinion-label {
      display: block;
      font-size: 24rpx;
      color: #999;
      margin-bottom: 8rpx;
    }

    .opinion-text {
      font-size: 28rpx;
      color: #333;
      line-height: 1.6;
    }
  }
}

// 公示信息
.publicity-info {
  .info-item {
    display: flex;
    justify-content: space-between;
    padding: 12rpx 0;

    .label {
      font-size: 26rpx;
      color: #999;
    }

    .value {
      font-size: 26rpx;
      color: #333;

      &.success {
        color: #4caf50;
        font-weight: 500;
      }
    }
  }
}

// 操作按钮
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx 30rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.withdraw-btn {
  padding: 28rpx;
  background: #f5f5f5;
  border-radius: 16rpx;
  text-align: center;

  text {
    font-size: 32rpx;
    color: #f44336;
  }

  &:active {
    background: #eee;
  }
}
</style>
