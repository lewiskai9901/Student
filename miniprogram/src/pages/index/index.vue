<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 页面头部 -->
    <view class="page-header">
      <view class="header-left">
        <text class="page-title">学生管理系统</text>
        <text class="page-subtitle" v-if="userStore.isLoggedIn">
          欢迎回来，{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}
        </text>
        <text class="page-subtitle" v-else>量化检查管理平台</text>
      </view>
      <view class="header-right">
        <view class="message-btn" @click="goToMessages" v-if="userStore.isLoggedIn">
          <u-icon name="bell" size="22" color="#6B7280" />
          <view class="badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
        </view>
        <view class="avatar-wrap" @click="goMine">
          <image
            class="avatar"
            :src="userStore.userAvatar || '/static/default-avatar.png'"
            mode="aspectFill"
          />
        </view>
      </view>
    </view>

    <!-- 未登录提示 -->
    <view class="login-card" v-if="!userStore.isLoggedIn" @click="goLogin">
      <view class="login-content">
        <text class="login-title">登录后查看完整功能</text>
        <text class="login-desc">点击此处进行登录</text>
      </view>
      <text class="login-arrow">&#8250;</text>
    </view>

    <!-- 已登录内容 -->
    <scroll-view
      v-else
      scroll-y
      class="main-content"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 快捷操作 -->
      <view class="quick-actions">
        <view class="action-row">
          <view class="action-item" @click="goToStudents">
            <view class="action-icon student">
              <u-icon name="account" size="24" color="#fff" />
            </view>
            <text class="action-name">学生</text>
          </view>
          <view class="action-item" @click="goToClasses">
            <view class="action-icon class">
              <u-icon name="grid" size="24" color="#fff" />
            </view>
            <text class="action-name">班级</text>
          </view>
          <view class="action-item" @click="goToDormitory">
            <view class="action-icon dorm">
              <u-icon name="home" size="24" color="#fff" />
            </view>
            <text class="action-name">宿舍</text>
          </view>
          <view class="action-item" @click="goToPlans">
            <view class="action-icon plan">
              <u-icon name="calendar" size="24" color="#fff" />
            </view>
            <text class="action-name">量化</text>
          </view>
        </view>
        <view class="action-row">
          <view class="action-item" @click="goToScoring">
            <view class="action-icon scoring">
              <u-icon name="edit-pen" size="24" color="#fff" />
            </view>
            <text class="action-name">打分</text>
          </view>
          <view class="action-item" @click="goToAppeals">
            <view class="action-icon appeal">
              <u-icon name="chat" size="24" color="#fff" />
            </view>
            <text class="action-name">申诉</text>
          </view>
          <view class="action-item" @click="goToStatistics">
            <view class="action-icon stats">
              <u-icon name="pie-chart" size="24" color="#fff" />
            </view>
            <text class="action-name">统计</text>
          </view>
          <view class="action-item" @click="showMoreMenu = true">
            <view class="action-icon more">
              <u-icon name="more-dot-fill" size="24" color="#fff" />
            </view>
            <text class="action-name">更多</text>
          </view>
        </view>
      </view>

      <!-- 今日概览 -->
      <view class="overview-section">
        <view class="overview-grid">
          <view class="overview-card" @click="goToScoring">
            <view class="overview-icon orange">
              <u-icon name="edit-pen" size="20" color="#fff" />
            </view>
            <view class="overview-info">
              <text class="overview-value">{{ stats.pendingScore }}</text>
              <text class="overview-label">待打分</text>
            </view>
          </view>
          <view class="overview-card" @click="goToAppeals">
            <view class="overview-icon red">
              <u-icon name="chat" size="20" color="#fff" />
            </view>
            <view class="overview-info">
              <text class="overview-value">{{ stats.pendingAppeal }}</text>
              <text class="overview-label">待审核</text>
            </view>
          </view>
          <view class="overview-card" @click="goToPlans">
            <view class="overview-icon blue">
              <u-icon name="calendar" size="20" color="#fff" />
            </view>
            <view class="overview-info">
              <text class="overview-value">{{ stats.planCount }}</text>
              <text class="overview-label">进行中</text>
            </view>
          </view>
          <view class="overview-card" @click="goToRecords">
            <view class="overview-icon green">
              <u-icon name="file-text" size="20" color="#fff" />
            </view>
            <view class="overview-info">
              <text class="overview-value">{{ stats.todayRecords }}</text>
              <text class="overview-label">今日检查</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 进行中的计划 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">进行中的检查计划</text>
          <text class="section-more" @click="goToPlans">查看全部</text>
        </view>

        <view v-if="loadingPlans" class="loading-box">
          <u-loading-icon size="30" color="#667eea" />
        </view>

        <view v-else-if="activePlans.length === 0" class="empty-box">
          <u-icon name="calendar" size="40" color="#D1D5DB" />
          <text class="empty-text">暂无进行中的计划</text>
        </view>

        <view v-else class="plan-list">
          <view
            class="plan-item"
            v-for="plan in activePlans"
            :key="plan.id"
            @click="goToPlanDetail(plan.id)"
          >
            <view class="plan-main">
              <text class="plan-name">{{ plan.planName }}</text>
              <view class="plan-meta">
                <text class="meta-template">{{ plan.templateName }}</text>
                <text class="meta-date">{{ formatDate(plan.startDate) }} - {{ formatDate(plan.endDate) }}</text>
              </view>
            </view>
            <view class="plan-progress">
              <view class="progress-bar">
                <view class="progress-fill" :style="{ width: getProgress(plan) + '%' }"></view>
              </view>
              <text class="progress-text">{{ plan.totalChecks || 0 }}/{{ plan.totalTargets || 0 }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 最新公告 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">最新公告</text>
          <text class="section-more" @click="goToAnnouncements">查看全部</text>
        </view>

        <view v-if="loadingAnnouncements" class="loading-box">
          <u-loading-icon size="30" color="#667eea" />
        </view>

        <view v-else-if="announcements.length === 0" class="empty-box">
          <u-icon name="volume" size="40" color="#D1D5DB" />
          <text class="empty-text">暂无公告</text>
        </view>

        <view v-else class="announcement-list">
          <view
            class="announcement-item"
            v-for="item in announcements"
            :key="item.id"
            @click="goToAnnouncementDetail(item)"
          >
            <view class="announcement-content">
              <view class="announcement-header">
                <view class="top-tag" v-if="item.isTop">置顶</view>
                <text class="announcement-title">{{ item.title }}</text>
              </view>
              <text class="announcement-time">{{ formatTime(item.publishTime) }}</text>
            </view>
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>
      </view>

      <!-- 最近检查记录 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">最近检查记录</text>
          <text class="section-more" @click="goToRecords">查看全部</text>
        </view>

        <view v-if="loadingRecords" class="loading-box">
          <u-loading-icon size="30" color="#667eea" />
        </view>

        <view v-else-if="recentRecords.length === 0" class="empty-box">
          <u-icon name="file-text" size="40" color="#D1D5DB" />
          <text class="empty-text">暂无检查记录</text>
        </view>

        <view v-else class="record-list">
          <view
            class="record-item"
            v-for="record in recentRecords"
            :key="record.id"
            @click="goToRecordDetail(record.id)"
          >
            <view class="record-date">
              <text class="date-day">{{ getDay(record.checkDate) }}</text>
              <text class="date-month">{{ getMonth(record.checkDate) }}月</text>
            </view>
            <view class="record-main">
              <text class="record-name">{{ record.checkName }}</text>
              <text class="record-meta">{{ record.checkerName }} · {{ record.totalClasses }}个班级</text>
            </view>
            <view class="record-score">
              <text class="score-num">-{{ record.totalScore }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部留白 -->
      <view class="bottom-space"></view>
    </scroll-view>

    <!-- 更多菜单弹窗 -->
    <u-popup v-model:show="showMoreMenu" mode="bottom" round="20">
      <view class="more-menu">
        <view class="menu-header">
          <text class="menu-title">更多功能</text>
          <view class="menu-close" @click="showMoreMenu = false">
            <u-icon name="close" size="20" color="#6B7280" />
          </view>
        </view>
        <view class="menu-grid">
          <view class="menu-item" @click="goToTemplates">
            <view class="menu-icon">
              <u-icon name="list" size="22" color="#667eea" />
            </view>
            <text class="menu-name">检查模板</text>
          </view>
          <view class="menu-item" @click="goToUserList">
            <view class="menu-icon">
              <u-icon name="account" size="22" color="#667eea" />
            </view>
            <text class="menu-name">用户管理</text>
          </view>
          <view class="menu-item" @click="goToSystemConfig">
            <view class="menu-icon">
              <u-icon name="setting" size="22" color="#667eea" />
            </view>
            <text class="menu-name">系统配置</text>
          </view>
          <view class="menu-item" @click="goToAnnouncements">
            <view class="menu-icon">
              <u-icon name="volume" size="22" color="#667eea" />
            </view>
            <text class="menu-name">公告管理</text>
          </view>
          <view class="menu-item" @click="goToClassRanking">
            <view class="menu-icon">
              <u-icon name="order" size="22" color="#667eea" />
            </view>
            <text class="menu-name">班级排名</text>
          </view>
          <view class="menu-item" @click="goToTrend">
            <view class="menu-icon">
              <u-icon name="bar-chart" size="22" color="#667eea" />
            </view>
            <text class="menu-name">趋势分析</text>
          </view>
        </view>
      </view>
    </u-popup>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getCheckPlanList, getCheckPlanStatistics, PlanStatus, type CheckPlan } from '@/api/checkPlan'
import { getCheckRecordList, type CheckRecord } from '@/api/checkRecord'
import { getPendingAppeals } from '@/api/appeal'
import { getPublishedAnnouncements, type Announcement } from '@/api/announcement'

const userStore = useUserStore()

// 刷新状态
const refreshing = ref(false)

// 更多菜单
const showMoreMenu = ref(false)

// 未读消息数
const unreadCount = ref(0)

// 统计数据
const stats = reactive({
  planCount: 0,
  pendingScore: 0,
  pendingAppeal: 0,
  todayRecords: 0
})

// 进行中的计划
const activePlans = ref<CheckPlan[]>([])
const loadingPlans = ref(false)

// 最近检查记录
const recentRecords = ref<CheckRecord[]>([])
const loadingRecords = ref(false)

// 公告
const announcements = ref<Announcement[]>([])
const loadingAnnouncements = ref(false)

// 页面显示时加载数据
onShow(() => {
  if (userStore.isLoggedIn) {
    loadData()
  }
})

// 下拉刷新
function onRefresh() {
  refreshing.value = true
  loadData().finally(() => {
    refreshing.value = false
  })
}

// 加载所有数据
async function loadData() {
  await Promise.all([
    loadStats(),
    loadActivePlans(),
    loadRecentRecords(),
    loadAnnouncements()
  ])
}

// 加载统计数据
async function loadStats() {
  try {
    const planStats = await getCheckPlanStatistics()
    stats.planCount = planStats?.inProgressCount || 0
    stats.pendingScore = planStats?.pendingScoringCount || 0

    try {
      const pendingAppeals = await getPendingAppeals({ pageNum: 1, pageSize: 1 })
      stats.pendingAppeal = pendingAppeals?.total || pendingAppeals?.length || 0
    } catch {
      stats.pendingAppeal = 0
    }

    // 获取今日记录数
    const today = new Date().toISOString().split('T')[0]
    const todayRecords = await getCheckRecordList({ pageNum: 1, pageSize: 1, checkDate: today })
    stats.todayRecords = todayRecords?.total || 0
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 模拟数据
    stats.planCount = 3
    stats.pendingScore = 5
    stats.pendingAppeal = 2
    stats.todayRecords = 8
  }
}

// 加载进行中的计划
async function loadActivePlans() {
  loadingPlans.value = true
  try {
    const res = await getCheckPlanList({
      status: PlanStatus.IN_PROGRESS,
      pageNum: 1,
      pageSize: 3
    })
    activePlans.value = res?.records || []
  } catch (error) {
    console.error('加载计划失败:', error)
    // 模拟数据
    activePlans.value = [
      {
        id: 1,
        planCode: 'PLAN001',
        planName: '日常卫生检查',
        templateId: 1,
        templateName: '日常检查模板',
        startDate: '2024-12-01',
        endDate: '2024-12-31',
        status: 1,
        totalChecks: 15,
        totalTargets: 30,
        totalRecords: 15,
        totalDeductionScore: 45
      }
    ]
  } finally {
    loadingPlans.value = false
  }
}

// 加载最近检查记录
async function loadRecentRecords() {
  loadingRecords.value = true
  try {
    const res = await getCheckRecordList({
      pageNum: 1,
      pageSize: 5
    })
    recentRecords.value = res?.records || []
  } catch (error) {
    console.error('加载记录失败:', error)
    // 模拟数据
    recentRecords.value = [
      {
        id: 1,
        checkName: '日常卫生检查',
        checkDate: '2024-12-13',
        checkerName: '张老师',
        totalClasses: 5,
        totalScore: 12
      },
      {
        id: 2,
        checkName: '纪律检查',
        checkDate: '2024-12-12',
        checkerName: '李老师',
        totalClasses: 3,
        totalScore: 8
      }
    ]
  } finally {
    loadingRecords.value = false
  }
}

// 加载公告
async function loadAnnouncements() {
  loadingAnnouncements.value = true
  try {
    const res = await getPublishedAnnouncements({ pageNum: 1, pageSize: 3 })
    announcements.value = res?.records || []
  } catch (error) {
    console.error('加载公告失败:', error)
    // 模拟数据
    announcements.value = [
      {
        id: 1,
        title: '关于开展期末卫生大检查的通知',
        publishTime: '2024-12-10 10:00',
        isTop: true,
        status: 1
      },
      {
        id: 2,
        title: '宿舍安全用电须知',
        publishTime: '2024-12-08 14:30',
        status: 1
      }
    ]
  } finally {
    loadingAnnouncements.value = false
  }
}

// 计算进度
function getProgress(plan: CheckPlan): number {
  if (!plan.totalTargets || plan.totalTargets === 0) return 0
  return Math.min(100, Math.round((plan.totalChecks || 0) / plan.totalTargets * 100))
}

// 格式化日期
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.substring(5, 10).replace('-', '/')
}

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return time.split(' ')[0]
}

function getDay(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.substring(8, 10)
}

function getMonth(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.substring(5, 7)
}

// 页面跳转
function goMine() {
  uni.switchTab({ url: '/pages/mine/index' })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}

function goToMessages() {
  uni.navigateTo({ url: '/pages/message/index' })
}

function goToStudents() {
  uni.navigateTo({ url: '/pages/student/list' })
}

function goToClasses() {
  uni.navigateTo({ url: '/pages/class/list' })
}

function goToDormitory() {
  uni.navigateTo({ url: '/pages/dormitory/building-list' })
}

function goToPlans() {
  uni.navigateTo({ url: '/pages/check/plan/index' })
}

function goToRecords() {
  uni.navigateTo({ url: '/pages/check/record/index' })
}

function goToAppeals() {
  uni.navigateTo({ url: '/pages/appeal/index' })
}

function goToScoring() {
  uni.navigateTo({ url: '/pages/scoring/plan-list' })
}

function goToStatistics() {
  uni.switchTab({ url: '/pages/statistics/overview' })
}

function goToTemplates() {
  showMoreMenu.value = false
  uni.navigateTo({ url: '/pages/check/template/list' })
}

function goToUserList() {
  showMoreMenu.value = false
  uni.navigateTo({ url: '/pages/system/user-list' })
}

function goToSystemConfig() {
  showMoreMenu.value = false
  uni.navigateTo({ url: '/pages/system/config' })
}

function goToAnnouncements() {
  showMoreMenu.value = false
  uni.navigateTo({ url: '/pages/system/announcement' })
}

function goToClassRanking() {
  showMoreMenu.value = false
  uni.navigateTo({ url: '/pages/statistics/class-ranking' })
}

function goToTrend() {
  showMoreMenu.value = false
  uni.navigateTo({ url: '/pages/statistics/trend' })
}

function goToPlanDetail(id: string | number) {
  uni.navigateTo({ url: `/pages/check/plan/detail?id=${id}` })
}

function goToRecordDetail(id: string | number) {
  uni.navigateTo({ url: `/pages/check/record/detail?id=${id}` })
}

function goToAnnouncementDetail(item: Announcement) {
  uni.showToast({ title: '详情页开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
// 变量
$primary: #667eea;
$primary-dark: #764ba2;
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border: #E5E7EB;
$bg: #F3F4F6;
$white: #FFFFFF;
$danger: #DC2626;
$success: #10B981;
$warning: #F59E0B;

.page {
  min-height: 100vh;
  background: $bg;
}

.safe-top {
  height: env(safe-area-inset-top);
  background: $white;
}

.safe-bottom {
  height: calc(120rpx + env(safe-area-inset-bottom));
}

// 页面头部
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;
  background: $white;
  border-bottom: 1rpx solid $border;
}

.header-left {
  .page-title {
    display: block;
    font-size: 36rpx;
    font-weight: 600;
    color: $text-primary;
    line-height: 1.2;
  }

  .page-subtitle {
    display: block;
    font-size: 24rpx;
    color: $text-muted;
    margin-top: 4rpx;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20rpx;

  .message-btn {
    position: relative;
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    .badge {
      position: absolute;
      top: 4rpx;
      right: 4rpx;
      min-width: 32rpx;
      height: 32rpx;
      padding: 0 8rpx;
      font-size: 20rpx;
      color: #fff;
      background: $danger;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}

.avatar-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  overflow: hidden;
  border: 2rpx solid $border;

  .avatar {
    width: 100%;
    height: 100%;
  }
}

// 登录提示卡片
.login-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 24rpx;
  padding: 32rpx;
  background: $white;
  border-radius: 16rpx;
  border: 1rpx solid $border;

  .login-content {
    .login-title {
      display: block;
      font-size: 30rpx;
      font-weight: 500;
      color: $text-primary;
    }

    .login-desc {
      display: block;
      font-size: 24rpx;
      color: $text-muted;
      margin-top: 4rpx;
    }
  }

  .login-arrow {
    font-size: 40rpx;
    color: $text-muted;
  }
}

// 主内容区
.main-content {
  height: calc(100vh - 120rpx - env(safe-area-inset-top) - env(safe-area-inset-bottom));
}

// 快捷操作
.quick-actions {
  background: $white;
  padding: 24rpx;
  margin: 20rpx 24rpx;
  border-radius: 16rpx;
}

.action-row {
  display: flex;
  margin-bottom: 20rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.action-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;

  &:active {
    opacity: 0.7;
  }
}

.action-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  &.student { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
  &.class { background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%); }
  &.dorm { background: linear-gradient(135deg, #10B981 0%, #059669 100%); }
  &.plan { background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%); }
  &.scoring { background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%); }
  &.appeal { background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%); }
  &.stats { background: linear-gradient(135deg, #06B6D4 0%, #0891B2 100%); }
  &.more { background: linear-gradient(135deg, #6B7280 0%, #4B5563 100%); }
}

.action-name {
  font-size: 24rpx;
  color: $text-secondary;
}

// 今日概览
.overview-section {
  padding: 0 24rpx;
  margin-bottom: 20rpx;
}

.overview-grid {
  display: flex;
  gap: 16rpx;
}

.overview-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: $white;
  border-radius: 12rpx;
  padding: 16rpx;

  .overview-icon {
    width: 48rpx;
    height: 48rpx;
    border-radius: 12rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    &.orange { background: $warning; }
    &.red { background: $danger; }
    &.blue { background: #3B82F6; }
    &.green { background: $success; }
  }

  .overview-info {
    .overview-value {
      display: block;
      font-size: 32rpx;
      font-weight: 600;
      color: $text-primary;
      line-height: 1;
    }

    .overview-label {
      font-size: 20rpx;
      color: $text-muted;
    }
  }
}

// 通用 section
.section {
  margin: 0 24rpx 20rpx;
  background: $white;
  border-radius: 16rpx;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
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

// 加载和空状态
.loading-box,
.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48rpx 0;
  gap: 12rpx;

  .empty-text {
    font-size: 26rpx;
    color: $text-muted;
  }
}

// 计划列表
.plan-list {
  .plan-item {
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

.plan-main {
  flex: 1;
  min-width: 0;

  .plan-name {
    display: block;
    font-size: 28rpx;
    font-weight: 500;
    color: $text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .plan-meta {
    display: flex;
    gap: 12rpx;
    margin-top: 6rpx;

    .meta-template, .meta-date {
      font-size: 22rpx;
      color: $text-muted;
    }
  }
}

.plan-progress {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: 20rpx;

  .progress-bar {
    width: 100rpx;
    height: 8rpx;
    background: #E5E7EB;
    border-radius: 4rpx;
    overflow: hidden;

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, $primary 0%, $primary-dark 100%);
      border-radius: 4rpx;
    }
  }

  .progress-text {
    font-size: 20rpx;
    color: $text-muted;
    margin-top: 4rpx;
  }
}

// 公告列表
.announcement-list {
  .announcement-item {
    display: flex;
    align-items: center;
    padding: 20rpx 24rpx;
    border-bottom: 1rpx solid $border;

    &:last-child {
      border-bottom: none;
    }

    &:active {
      background: $bg;
    }

    .announcement-content {
      flex: 1;

      .announcement-header {
        display: flex;
        align-items: center;
        gap: 8rpx;
        margin-bottom: 4rpx;

        .top-tag {
          font-size: 20rpx;
          color: #fff;
          background: $danger;
          padding: 2rpx 8rpx;
          border-radius: 4rpx;
        }

        .announcement-title {
          font-size: 28rpx;
          color: $text-primary;
        }
      }

      .announcement-time {
        font-size: 22rpx;
        color: $text-muted;
      }
    }
  }
}

// 记录列表
.record-list {
  .record-item {
    display: flex;
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

.record-date {
  width: 72rpx;
  text-align: center;
  margin-right: 20rpx;

  .date-day {
    display: block;
    font-size: 36rpx;
    font-weight: 600;
    color: $text-primary;
    line-height: 1;
  }

  .date-month {
    display: block;
    font-size: 20rpx;
    color: $text-muted;
    margin-top: 4rpx;
  }
}

.record-main {
  flex: 1;
  min-width: 0;

  .record-name {
    display: block;
    font-size: 28rpx;
    font-weight: 500;
    color: $text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .record-meta {
    display: block;
    font-size: 22rpx;
    color: $text-muted;
    margin-top: 4rpx;
  }
}

.record-score {
  margin-left: 20rpx;

  .score-num {
    font-size: 30rpx;
    font-weight: 600;
    color: $danger;
  }
}

.bottom-space {
  height: 40rpx;
}

// 更多菜单
.more-menu {
  .menu-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx;
    border-bottom: 1rpx solid $border;

    .menu-title {
      font-size: 32rpx;
      font-weight: 600;
      color: $text-primary;
    }

    .menu-close {
      width: 60rpx;
      height: 60rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .menu-grid {
    display: flex;
    flex-wrap: wrap;
    padding: 24rpx;

    .menu-item {
      width: 33.33%;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24rpx 0;

      .menu-icon {
        width: 80rpx;
        height: 80rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(102, 126, 234, 0.1);
        border-radius: 16rpx;
        margin-bottom: 12rpx;
      }

      .menu-name {
        font-size: 26rpx;
        color: $text-secondary;
      }
    }
  }
}
</style>
