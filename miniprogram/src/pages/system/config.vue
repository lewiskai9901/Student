<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">系统配置</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view
      scroll-y
      class="content"
      :style="{ marginTop: navBarHeight + 'px' }"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 加载中 -->
      <view v-if="loading" class="loading-wrapper">
        <u-loading-icon size="40" color="#667eea" />
      </view>

      <template v-else>
        <!-- 评级配置 -->
        <view class="config-section">
          <view class="section-header">
            <view class="section-icon rating">
              <u-icon name="star" size="20" color="#fff" />
            </view>
            <view class="section-info">
              <text class="section-title">评级配置</text>
              <text class="section-desc">检查结果评级规则设置</text>
            </view>
          </view>

          <view class="config-list">
            <view
              v-for="item in ratingConfigs"
              :key="item.id"
              class="config-item"
            >
              <view class="rating-badge" :class="'rating-' + item.level.toLowerCase()">
                {{ item.level }}
              </view>
              <view class="config-info">
                <text class="config-name">{{ item.levelName }}</text>
                <text class="config-range">分数范围: {{ item.minScore }} ~ {{ item.maxScore }}分</text>
              </view>
              <view class="config-status" :class="item.status === 1 ? 'enabled' : 'disabled'">
                {{ item.status === 1 ? '启用' : '禁用' }}
              </view>
            </view>

            <view v-if="!ratingConfigs.length" class="empty-config">
              <text>暂无评级配置</text>
            </view>
          </view>
        </view>

        <!-- 加权配置 -->
        <view class="config-section">
          <view class="section-header">
            <view class="section-icon weight">
              <u-icon name="setting" size="20" color="#fff" />
            </view>
            <view class="section-info">
              <text class="section-title">加权配置</text>
              <text class="section-desc">检查结果加权规则设置</text>
            </view>
          </view>

          <view class="config-list">
            <view
              v-for="item in weightConfigs"
              :key="item.id"
              class="config-item"
            >
              <view class="weight-icon">
                <u-icon name="bag" size="18" color="#667eea" />
              </view>
              <view class="config-info">
                <text class="config-name">{{ item.configName }}</text>
                <text class="config-range">权重值: {{ item.weightValue }}%</text>
              </view>
              <view class="config-status" :class="item.status === 1 ? 'enabled' : 'disabled'">
                {{ item.status === 1 ? '启用' : '禁用' }}
              </view>
            </view>

            <view v-if="!weightConfigs.length" class="empty-config">
              <text>暂无加权配置</text>
            </view>
          </view>
        </view>

        <!-- 系统参数 -->
        <view class="config-section">
          <view class="section-header">
            <view class="section-icon system">
              <u-icon name="more-circle" size="20" color="#fff" />
            </view>
            <view class="section-info">
              <text class="section-title">系统参数</text>
              <text class="section-desc">系统基础参数配置</text>
            </view>
          </view>

          <view class="param-list">
            <view class="param-item">
              <text class="param-label">基础分数</text>
              <text class="param-value">{{ systemParams.baseScore }}分</text>
            </view>
            <view class="param-item">
              <text class="param-label">申诉时限</text>
              <text class="param-value">{{ systemParams.appealDeadline }}天</text>
            </view>
            <view class="param-item">
              <text class="param-label">检查频率</text>
              <text class="param-value">{{ systemParams.checkFrequency }}</text>
            </view>
            <view class="param-item">
              <text class="param-label">数据保留</text>
              <text class="param-value">{{ systemParams.dataRetention }}天</text>
            </view>
          </view>
        </view>

        <!-- 关于系统 -->
        <view class="config-section">
          <view class="section-header">
            <view class="section-icon about">
              <u-icon name="info-circle" size="20" color="#fff" />
            </view>
            <view class="section-info">
              <text class="section-title">关于系统</text>
              <text class="section-desc">系统版本和相关信息</text>
            </view>
          </view>

          <view class="about-list">
            <view class="about-item">
              <text class="about-label">系统名称</text>
              <text class="about-value">学生管理系统</text>
            </view>
            <view class="about-item">
              <text class="about-label">系统版本</text>
              <text class="about-value">v2.0.0</text>
            </view>
            <view class="about-item">
              <text class="about-label">小程序版本</text>
              <text class="about-value">v1.0.0</text>
            </view>
            <view class="about-item">
              <text class="about-label">服务器状态</text>
              <view class="server-status online">
                <view class="status-dot"></view>
                <text>正常运行</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 提示信息 -->
        <view class="tip-section">
          <u-icon name="info-circle" size="16" color="#9CA3AF" />
          <text class="tip-text">如需修改配置，请前往Web端管理后台操作</text>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)

// 数据
const loading = ref(true)
const refreshing = ref(false)

// 评级配置
const ratingConfigs = ref<any[]>([])

// 加权配置
const weightConfigs = ref<any[]>([])

// 系统参数
const systemParams = reactive({
  baseScore: 100,
  appealDeadline: 3,
  checkFrequency: '每日',
  dataRetention: 365
})

onMounted(() => {
  initSystemInfo()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
}

const loadData = async () => {
  loading.value = true
  try {
    // TODO: 调用真实API
    await new Promise(resolve => setTimeout(resolve, 500))

    // 模拟评级配置数据
    ratingConfigs.value = [
      { id: 1, level: 'A', levelName: '优秀', minScore: 90, maxScore: 100, status: 1 },
      { id: 2, level: 'B', levelName: '良好', minScore: 80, maxScore: 89, status: 1 },
      { id: 3, level: 'C', levelName: '中等', minScore: 70, maxScore: 79, status: 1 },
      { id: 4, level: 'D', levelName: '及格', minScore: 60, maxScore: 69, status: 1 },
      { id: 5, level: 'E', levelName: '不及格', minScore: 0, maxScore: 59, status: 1 }
    ]

    // 模拟加权配置数据
    weightConfigs.value = [
      { id: 1, configName: '卫生检查权重', weightValue: 40, status: 1 },
      { id: 2, configName: '纪律检查权重', weightValue: 30, status: 1 },
      { id: 3, configName: '安全检查权重', weightValue: 30, status: 1 }
    ]
  } catch (error) {
    console.error('加载配置数据失败', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onRefresh = () => {
  refreshing.value = true
  loadData()
}

const goBack = () => {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: #F3F4F6;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .nav-content {
    display: flex;
    align-items: center;
    height: 88rpx;
    padding: 0 24rpx;

    .nav-back, .nav-placeholder {
      width: 64rpx;
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .nav-title {
      flex: 1;
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
      text-align: center;
    }
  }
}

.content {
  height: calc(100vh - var(--nav-height));
  padding: 20rpx 24rpx 40rpx;

  .loading-wrapper {
    display: flex;
    justify-content: center;
    padding: 100rpx 0;
  }
}

.config-section {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;

  .section-header {
    display: flex;
    align-items: center;
    padding: 24rpx;
    border-bottom: 1rpx solid #F3F4F6;

    .section-icon {
      width: 72rpx;
      height: 72rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 16rpx;
      margin-right: 20rpx;

      &.rating {
        background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
      }

      &.weight {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      &.system {
        background: linear-gradient(135deg, #10B981 0%, #059669 100%);
      }

      &.about {
        background: linear-gradient(135deg, #6B7280 0%, #4B5563 100%);
      }
    }

    .section-info {
      flex: 1;

      .section-title {
        display: block;
        font-size: 30rpx;
        font-weight: 600;
        color: #1F2937;
        margin-bottom: 4rpx;
      }

      .section-desc {
        font-size: 24rpx;
        color: #9CA3AF;
      }
    }
  }
}

.config-list {
  padding: 0 24rpx;

  .config-item {
    display: flex;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #F3F4F6;

    &:last-child {
      border-bottom: none;
    }

    .rating-badge {
      width: 48rpx;
      height: 48rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 26rpx;
      font-weight: 600;
      color: #fff;
      border-radius: 8rpx;
      margin-right: 16rpx;

      &.rating-a { background: #10B981; }
      &.rating-b { background: #3B82F6; }
      &.rating-c { background: #F59E0B; }
      &.rating-d { background: #F97316; }
      &.rating-e { background: #EF4444; }
    }

    .weight-icon {
      width: 48rpx;
      height: 48rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(102, 126, 234, 0.1);
      border-radius: 8rpx;
      margin-right: 16rpx;
    }

    .config-info {
      flex: 1;

      .config-name {
        display: block;
        font-size: 28rpx;
        color: #1F2937;
        margin-bottom: 4rpx;
      }

      .config-range {
        font-size: 24rpx;
        color: #9CA3AF;
      }
    }

    .config-status {
      font-size: 22rpx;
      padding: 4rpx 12rpx;
      border-radius: 4rpx;

      &.enabled {
        background: #D1FAE5;
        color: #059669;
      }

      &.disabled {
        background: #F3F4F6;
        color: #9CA3AF;
      }
    }
  }

  .empty-config {
    padding: 40rpx 0;
    text-align: center;
    color: #9CA3AF;
    font-size: 26rpx;
  }
}

.param-list, .about-list {
  padding: 0 24rpx;

  .param-item, .about-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #F3F4F6;

    &:last-child {
      border-bottom: none;
    }

    .param-label, .about-label {
      font-size: 28rpx;
      color: #6B7280;
    }

    .param-value, .about-value {
      font-size: 28rpx;
      color: #1F2937;
    }
  }

  .server-status {
    display: flex;
    align-items: center;
    gap: 8rpx;

    .status-dot {
      width: 16rpx;
      height: 16rpx;
      border-radius: 50%;
    }

    &.online {
      color: #059669;

      .status-dot {
        background: #10B981;
      }
    }

    &.offline {
      color: #DC2626;

      .status-dot {
        background: #EF4444;
      }
    }
  }
}

.tip-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 24rpx;

  .tip-text {
    font-size: 24rpx;
    color: #9CA3AF;
  }
}
</style>
