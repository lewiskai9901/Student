<template>
  <view class="myclass-detail">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading-container">
      <view class="loading-spinner"></view>
      <text class="loading-text">加载中...</text>
    </view>

    <template v-else-if="detailData">
      <!-- 顶部信息卡片 -->
      <view class="header-section">
        <view class="header-bg"></view>
        <view class="class-card">
          <view class="class-header">
            <view class="class-info">
              <text class="class-name">{{ detailData.myClassStats.className }}</text>
              <text class="grade-name">{{ detailData.myClassStats.gradeName }}</text>
            </view>
            <view class="ranking-info">
              <view class="ranking-badge" :class="getRankingClass(detailData.myClassStats.ranking)">
                <text class="ranking-num">{{ detailData.myClassStats.ranking }}</text>
                <text class="ranking-label">名</text>
              </view>
              <text class="ranking-total">/ {{ totalClasses }}班</text>
            </view>
          </view>

          <view class="score-level">
            <text class="level-label">综合评价:</text>
            <text class="level-value" :class="getLevelClass(detailData.myClassStats.scoreLevel)">
              {{ detailData.myClassStats.scoreLevel || '未评定' }}
            </text>
          </view>
        </view>
      </view>

      <!-- 记录信息 -->
      <view class="record-info-card">
        <view class="info-title">检查信息</view>
        <view class="info-row">
          <text class="info-label">检查名称</text>
          <text class="info-value">{{ detailData.recordInfo.checkName }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">检查日期</text>
          <text class="info-value">{{ formatDate(detailData.recordInfo.checkDate) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">检查人</text>
          <text class="info-value">{{ detailData.recordInfo.checkerName }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">权重配置</text>
          <text class="info-value" :class="detailData.recordInfo.weightEnabled ? 'text-primary' : ''">
            {{ detailData.recordInfo.weightEnabled ? '已启用' : '未启用' }}
          </text>
        </view>
      </view>

      <!-- 分数统计 -->
      <view class="stats-card">
        <view class="stats-title">本班统计</view>
        <view class="stats-grid">
          <view class="stat-item">
            <text class="stat-value negative">{{ detailData.myClassStats.totalScore }}</text>
            <text class="stat-label">总扣分</text>
          </view>
          <view class="stat-item" v-if="detailData.recordInfo.weightEnabled">
            <text class="stat-value">{{ detailData.myClassStats.weightedTotalScore?.toFixed(1) || '-' }}</text>
            <text class="stat-label">加权分</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ detailData.myClassStats.totalItems }}</text>
            <text class="stat-label">扣分项</text>
          </view>
          <view class="stat-item">
            <text class="stat-value" :class="getCompareClass(detailData.myClassStats.vsAvgScore)">
              {{ formatCompare(detailData.myClassStats.vsAvgScore) }}
            </text>
            <text class="stat-label">对比平均</text>
          </view>
        </view>
      </view>

      <!-- 分类得分 -->
      <view class="category-card">
        <view class="category-title">分类扣分</view>
        <view class="category-list">
          <view class="category-item" v-if="detailData.myClassStats.hygieneScore > 0">
            <view class="category-icon hygiene">&#128167;</view>
            <view class="category-info">
              <text class="category-name">卫生类</text>
              <text class="category-score negative">-{{ detailData.myClassStats.hygieneScore }}</text>
            </view>
          </view>
          <view class="category-item" v-if="detailData.myClassStats.disciplineScore > 0">
            <view class="category-icon discipline">&#128220;</view>
            <view class="category-info">
              <text class="category-name">纪律类</text>
              <text class="category-score negative">-{{ detailData.myClassStats.disciplineScore }}</text>
            </view>
          </view>
          <view class="category-item" v-if="detailData.myClassStats.attendanceScore > 0">
            <view class="category-icon attendance">&#128197;</view>
            <view class="category-info">
              <text class="category-name">考勤类</text>
              <text class="category-score negative">-{{ detailData.myClassStats.attendanceScore }}</text>
            </view>
          </view>
          <view class="category-item" v-if="detailData.myClassStats.otherScore > 0">
            <view class="category-icon other">&#128203;</view>
            <view class="category-info">
              <text class="category-name">其他类</text>
              <text class="category-score negative">-{{ detailData.myClassStats.otherScore }}</text>
            </view>
          </view>
          <view class="no-category" v-if="!hasAnyCategory">
            <text>本次检查无扣分，表现优秀！</text>
          </view>
        </view>
      </view>

      <!-- 申诉统计 -->
      <view class="appeal-stats-card" v-if="detailData.myClassStats.appealCount > 0">
        <view class="appeal-title">申诉情况</view>
        <view class="appeal-stats">
          <view class="appeal-item">
            <text class="appeal-num">{{ detailData.myClassStats.appealCount }}</text>
            <text class="appeal-label">总申诉</text>
          </view>
          <view class="appeal-item">
            <text class="appeal-num success">{{ detailData.myClassStats.appealPassed }}</text>
            <text class="appeal-label">已通过</text>
          </view>
          <view class="appeal-item">
            <text class="appeal-num warning">{{ detailData.myClassStats.appealPending }}</text>
            <text class="appeal-label">待审核</text>
          </view>
        </view>
      </view>

      <!-- 扣分明细 -->
      <view class="deduction-section">
        <view class="section-header">
          <text class="section-title">扣分明细</text>
          <text class="section-count">共{{ deductionItems.length }}项</text>
        </view>

        <view v-if="deductionItems.length > 0" class="deduction-list">
          <view
            class="deduction-item"
            v-for="item in deductionItems"
            :key="item.id"
            @click="viewItemDetail(item)"
          >
            <view class="item-main">
              <view class="item-header">
                <view class="category-tag">{{ item.categoryName }}</view>
                <text class="item-score negative">-{{ item.deductScore }}分</text>
              </view>
              <text class="item-name">{{ item.itemName }}</text>
              <view class="item-meta">
                <text v-if="item.linkName" class="meta-text">{{ item.linkNo }} {{ item.linkName }}</text>
                <text v-if="item.studentNames?.length" class="meta-text">
                  涉及: {{ item.studentNames.join('、') }}
                </text>
              </view>
              <view class="item-remark" v-if="item.remark">
                <text>{{ item.remark }}</text>
              </view>
            </view>

            <!-- 申诉状态/按钮 -->
            <view class="item-action">
              <view v-if="item.appealStatus > 0" class="appeal-badge" :class="getAppealStatusClass(item.appealStatus)">
                {{ getAppealStatusText(item.appealStatus) }}
              </view>
              <view v-else class="appeal-btn" @click.stop="goToAppeal(item)">
                <text>申诉</text>
              </view>
            </view>
          </view>
        </view>

        <view v-else class="no-deduction">
          <view class="success-icon">&#127881;</view>
          <text class="success-title">太棒了！</text>
          <text class="success-desc">本次检查无扣分项，继续保持！</text>
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
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getMyClassDetail, type MyClassDetail, type DeductionItem } from '@/api/checkRecord'

// 参数
const recordId = ref<string>('')
const classId = ref<string>('')

// 数据
const detailData = ref<MyClassDetail | null>(null)
const totalClasses = ref(0)

// 加载状态
const loading = ref(true)

// 页面加载
onLoad((options) => {
  if (options?.id) {
    recordId.value = options.id
    if (options.classId) {
      classId.value = options.classId
    }
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
    const res = await getMyClassDetail(recordId.value)
    if (res.code === 200) {
      detailData.value = res.data
      totalClasses.value = res.data.recordInfo?.totalClasses || 0
      // 设置页面标题
      uni.setNavigationBarTitle({
        title: res.data.myClassStats?.className || '本班详情'
      })
    } else {
      uni.showToast({
        title: res.message || '加载失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('加载本班详情失败:', error)
    uni.showToast({
      title: '网络错误',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 扣分明细列表
const deductionItems = computed(() => {
  return detailData.value?.myClassStats?.items || []
})

// 是否有分类扣分
const hasAnyCategory = computed(() => {
  if (!detailData.value?.myClassStats) return false
  const stats = detailData.value.myClassStats
  return stats.hygieneScore > 0 || stats.disciplineScore > 0 ||
         stats.attendanceScore > 0 || stats.otherScore > 0
})

// 获取排名样式
function getRankingClass(rank: number): string {
  if (rank <= 3) return 'top'
  if (rank <= 10) return 'good'
  return ''
}

// 获取等级样式
function getLevelClass(level: string): string {
  if (!level) return ''
  if (level.includes('优')) return 'excellent'
  if (level.includes('良')) return 'good'
  if (level.includes('中')) return 'medium'
  return 'poor'
}

// 获取对比样式
function getCompareClass(value: number): string {
  if (value > 0) return 'worse'  // 比平均多扣分，表现差
  if (value < 0) return 'better' // 比平均少扣分，表现好
  return ''
}

// 格式化对比数值
function formatCompare(value: number): string {
  if (value > 0) return `+${value}`
  if (value < 0) return `${value}`
  return '持平'
}

// 格式化日期
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

// 获取申诉状态文本
function getAppealStatusText(status: number): string {
  const statusMap: Record<number, string> = {
    1: '待审核',
    2: '已通过',
    3: '已驳回',
    4: '已撤销',
    5: '已过期',
    6: '公示中',
    7: '已生效'
  }
  return statusMap[status] || '未知'
}

// 获取申诉状态样式
function getAppealStatusClass(status: number): string {
  const classMap: Record<number, string> = {
    1: 'pending',
    2: 'approved',
    3: 'rejected',
    4: 'withdrawn',
    5: 'expired',
    6: 'publicity',
    7: 'effective'
  }
  return classMap[status] || ''
}

// 查看扣分项详情
function viewItemDetail(item: DeductionItem) {
  // 如果有申诉，跳转到申诉详情
  if (item.appealId) {
    uni.navigateTo({
      url: `/pages/appeal/detail?id=${item.appealId}`
    })
  }
}

// 去申诉
function goToAppeal(item: DeductionItem) {
  uni.navigateTo({
    url: `/pages/appeal/create?recordId=${recordId.value}&itemId=${item.id}`
  })
}
</script>

<style lang="scss" scoped>
.myclass-detail {
  min-height: 100vh;
  background: #f5f6fa;
  padding-bottom: 40rpx;
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

// 头部区域
.header-section {
  position: relative;
  padding: 30rpx;
  padding-top: calc(30rpx + env(safe-area-inset-top));
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 280rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.class-card {
  position: relative;
  z-index: 1;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.class-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20rpx;
}

.class-info {
  .class-name {
    display: block;
    font-size: 40rpx;
    font-weight: 700;
    color: #333;
    margin-bottom: 8rpx;
  }

  .grade-name {
    font-size: 26rpx;
    color: #999;
  }
}

.ranking-info {
  display: flex;
  align-items: flex-end;
}

.ranking-badge {
  display: flex;
  align-items: baseline;
  padding: 12rpx 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;

  .ranking-num {
    font-size: 48rpx;
    font-weight: 700;
    color: #333;
  }

  .ranking-label {
    font-size: 24rpx;
    color: #666;
    margin-left: 4rpx;
  }

  &.top {
    background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);

    .ranking-num, .ranking-label {
      color: #fff;
    }
  }

  &.good {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

    .ranking-num, .ranking-label {
      color: #fff;
    }
  }
}

.ranking-total {
  font-size: 24rpx;
  color: #999;
  margin-left: 8rpx;
  margin-bottom: 8rpx;
}

.score-level {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;

  .level-label {
    font-size: 26rpx;
    color: #999;
  }

  .level-value {
    font-size: 28rpx;
    font-weight: 600;

    &.excellent {
      color: #4caf50;
    }

    &.good {
      color: #2196f3;
    }

    &.medium {
      color: #ff9800;
    }

    &.poor {
      color: #f44336;
    }
  }
}

// 记录信息卡片
.record-info-card {
  margin: 0 30rpx 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.info-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;

  .info-label {
    font-size: 26rpx;
    color: #999;
  }

  .info-value {
    font-size: 26rpx;
    color: #333;

    &.text-primary {
      color: #667eea;
    }
  }
}

// 统计卡片
.stats-card {
  margin: 0 30rpx 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.stats-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}

.stats-grid {
  display: flex;
  flex-wrap: wrap;
}

.stat-item {
  width: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 0;

  .stat-value {
    font-size: 40rpx;
    font-weight: 700;
    color: #333;

    &.negative {
      color: #f44336;
    }

    &.better {
      color: #4caf50;
    }

    &.worse {
      color: #f44336;
    }
  }

  .stat-label {
    font-size: 24rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

// 分类卡片
.category-card {
  margin: 0 30rpx 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.category-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  min-width: 45%;
}

.category-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;

  &.hygiene {
    background: #e3f2fd;
  }

  &.discipline {
    background: #fff3e0;
  }

  &.attendance {
    background: #e8f5e9;
  }

  &.other {
    background: #f3e5f5;
  }
}

.category-info {
  display: flex;
  flex-direction: column;

  .category-name {
    font-size: 24rpx;
    color: #666;
  }

  .category-score {
    font-size: 28rpx;
    font-weight: 600;

    &.negative {
      color: #f44336;
    }
  }
}

.no-category {
  width: 100%;
  padding: 30rpx;
  text-align: center;

  text {
    font-size: 28rpx;
    color: #4caf50;
  }
}

// 申诉统计卡片
.appeal-stats-card {
  margin: 0 30rpx 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.appeal-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}

.appeal-stats {
  display: flex;
  justify-content: space-around;
}

.appeal-item {
  display: flex;
  flex-direction: column;
  align-items: center;

  .appeal-num {
    font-size: 36rpx;
    font-weight: 700;
    color: #333;

    &.success {
      color: #4caf50;
    }

    &.warning {
      color: #ff9800;
    }
  }

  .appeal-label {
    font-size: 24rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

// 扣分明细
.deduction-section {
  margin: 0 30rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.section-count {
  font-size: 26rpx;
  color: #999;
}

.deduction-list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.deduction-item {
  display: flex;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background: #f9f9f9;
  }
}

.item-main {
  flex: 1;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.category-tag {
  padding: 6rpx 16rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12rpx;
  font-size: 22rpx;
  color: #fff;
}

.item-score {
  font-size: 30rpx;
  font-weight: 600;

  &.negative {
    color: #f44336;
  }
}

.item-name {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.item-meta {
  .meta-text {
    display: block;
    font-size: 24rpx;
    color: #666;
    margin-bottom: 4rpx;
  }
}

.item-remark {
  margin-top: 8rpx;
  padding: 12rpx;
  background: #f9f9f9;
  border-radius: 8rpx;

  text {
    font-size: 24rpx;
    color: #999;
  }
}

.item-action {
  display: flex;
  align-items: center;
  margin-left: 16rpx;
}

.appeal-badge {
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
  font-size: 22rpx;

  &.pending {
    background: #fff3e0;
    color: #ff9800;
  }

  &.approved, &.effective {
    background: #e8f5e9;
    color: #4caf50;
  }

  &.rejected {
    background: #ffebee;
    color: #f44336;
  }

  &.withdrawn, &.expired {
    background: #f5f5f5;
    color: #999;
  }

  &.publicity {
    background: #e3f2fd;
    color: #2196f3;
  }
}

.appeal-btn {
  padding: 12rpx 24rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20rpx;

  text {
    font-size: 24rpx;
    color: #fff;
  }
}

// 无扣分状态
.no-deduction {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 40rpx;
  background: #fff;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  .success-icon {
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  .success-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #4caf50;
    margin-bottom: 12rpx;
  }

  .success-desc {
    font-size: 26rpx;
    color: #999;
  }
}
</style>
