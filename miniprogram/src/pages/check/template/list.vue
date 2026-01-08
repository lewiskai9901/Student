<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 页面头部 -->
    <view class="page-header">
      <view class="header-main">
        <text class="page-title">检查模板</text>
        <text class="page-desc">量化检查模板管理</text>
      </view>
    </view>

    <!-- 搜索和筛选 -->
    <view class="search-bar">
      <view class="search-input-wrap">
        <u-icon name="search" size="18" color="#9CA3AF" />
        <input
          type="text"
          v-model="keyword"
          placeholder="搜索模板名称"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <u-icon v-if="keyword" name="close-circle-fill" size="18" color="#9CA3AF" @click="clearSearch" />
      </view>
    </view>

    <!-- 类型筛选 -->
    <view class="filter-bar">
      <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
        <view class="filter-list">
          <view
            class="filter-item"
            :class="{ active: selectedType === undefined }"
            @click="filterByType(undefined)"
          >
            <text class="filter-text">全部</text>
          </view>
          <view
            v-for="item in templateTypes"
            :key="item.value"
            class="filter-item"
            :class="{ active: selectedType === item.value }"
            @click="filterByType(item.value)"
          >
            <text class="filter-text">{{ item.label }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 列表内容 -->
    <scroll-view
      scroll-y
      class="list-container"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 加载状态 -->
      <view v-if="loading && !templates.length" class="state-box">
        <u-loading-icon size="40" color="#4F46E5" />
        <text class="state-text">加载中...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="templates.length === 0" class="state-box">
        <u-icon name="file-text" size="64" color="#9CA3AF" />
        <text class="state-title">暂无检查模板</text>
        <text class="state-desc">请在Web端创建检查模板</text>
      </view>

      <!-- 模板列表 -->
      <view v-else class="template-list">
        <view
          v-for="item in templates"
          :key="item.id"
          class="template-card"
          @click="goDetail(item.id)"
        >
          <view class="card-header">
            <view class="card-title-row">
              <text class="card-title">{{ item.templateName }}</text>
              <view class="status-badge" :class="item.status === 1 ? 'enabled' : 'disabled'">
                {{ item.status === 1 ? '启用' : '禁用' }}
              </view>
            </view>
            <view class="card-meta">
              <text class="meta-code">{{ item.templateCode }}</text>
              <view class="type-tag" :class="'type-' + item.templateType">
                {{ getTypeName(item.templateType) }}
              </view>
            </view>
          </view>

          <view class="card-body">
            <text v-if="item.description" class="description">{{ item.description }}</text>
            <view class="stat-row">
              <view class="stat-item">
                <text class="stat-value">{{ item.categoryCount || 0 }}</text>
                <text class="stat-label">类别</text>
              </view>
              <view class="stat-item">
                <text class="stat-value">{{ item.itemCount || 0 }}</text>
                <text class="stat-label">扣分项</text>
              </view>
              <view class="stat-item">
                <text class="stat-value">{{ item.totalScore || 100 }}</text>
                <text class="stat-label">满分</text>
              </view>
            </view>
          </view>

          <view class="card-footer">
            <text class="time-text">更新于 {{ formatTime(item.updatedAt) }}</text>
            <view class="arrow-icon">
              <u-icon name="arrow-right" size="16" color="#9CA3AF" />
            </view>
          </view>
        </view>

        <!-- 加载更多 -->
        <view v-if="hasMore" class="load-more">
          <text v-if="loadingMore">加载中...</text>
          <text v-else @click="loadMore">加载更多</text>
        </view>

        <!-- 没有更多 -->
        <view v-else-if="templates.length > 0" class="no-more">
          <text>— 没有更多了 —</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTemplateList, type CheckTemplate } from '@/api/checkTemplate'

// 状态
const loading = ref(true)
const loadingMore = ref(false)
const refreshing = ref(false)
const templates = ref<CheckTemplate[]>([])
const keyword = ref('')
const selectedType = ref<number | undefined>(undefined)
const pageNum = ref(1)
const pageSize = ref(10)
const hasMore = ref(false)

// 模板类型
const templateTypes = [
  { label: '日常检查', value: 1 },
  { label: '专项检查', value: 2 },
  { label: '临时检查', value: 3 }
]

// 初始化
onMounted(() => {
  loadData()
})

// 加载数据
async function loadData(append = false) {
  if (!append) {
    loading.value = true
    pageNum.value = 1
  } else {
    loadingMore.value = true
  }

  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    if (selectedType.value !== undefined) {
      params.templateType = selectedType.value
    }

    const result = await getTemplateList(params)
    const newData = result.records || []

    if (append) {
      templates.value.push(...newData)
    } else {
      templates.value = newData
    }

    hasMore.value = templates.value.length < (result.total || 0)
  } catch (error) {
    console.error('加载模板列表失败:', error)
    // 使用模拟数据
    templates.value = [
      {
        id: 1,
        templateCode: 'TPL001',
        templateName: '日常卫生检查模板',
        templateType: 1,
        description: '用于日常卫生检查的标准模板，包含卫生、纪律、安全等检查类别',
        categoryCount: 5,
        itemCount: 25,
        totalScore: 100,
        status: 1,
        updatedAt: '2024-12-10 10:00:00'
      },
      {
        id: 2,
        templateCode: 'TPL002',
        templateName: '专项安全检查模板',
        templateType: 2,
        description: '用于宿舍安全专项检查',
        categoryCount: 3,
        itemCount: 15,
        totalScore: 100,
        status: 1,
        updatedAt: '2024-12-08 14:30:00'
      },
      {
        id: 3,
        templateCode: 'TPL003',
        templateName: '期末综合检查模板',
        templateType: 3,
        description: '期末综合检查使用的模板',
        categoryCount: 6,
        itemCount: 30,
        totalScore: 100,
        status: 0,
        updatedAt: '2024-12-05 09:00:00'
      }
    ]
    hasMore.value = false
  } finally {
    loading.value = false
    loadingMore.value = false
    refreshing.value = false
  }
}

// 下拉刷新
function onRefresh() {
  refreshing.value = true
  loadData()
}

// 加载更多
function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  pageNum.value++
  loadData(true)
}

// 搜索
function handleSearch() {
  loadData()
}

// 清除搜索
function clearSearch() {
  keyword.value = ''
  loadData()
}

// 类型筛选
function filterByType(type: number | undefined) {
  selectedType.value = type
  loadData()
}

// 获取类型名称
function getTypeName(type?: number): string {
  const typeMap: Record<number, string> = {
    1: '日常检查',
    2: '专项检查',
    3: '临时检查'
  }
  return type ? typeMap[type] || '未知' : '未知'
}

// 格式化时间
function formatTime(time?: string): string {
  if (!time) return '-'
  return time.substring(0, 10)
}

// 跳转详情
function goDetail(id: string | number) {
  uni.navigateTo({
    url: `/pages/check/template/detail?id=${id}`
  })
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
$success: #059669;
$warning: #D97706;

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
  background: $white;
  padding: 32rpx;
  border-bottom: 1rpx solid $border;

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

// 搜索栏
.search-bar {
  background: $white;
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid $border;

  .search-input-wrap {
    display: flex;
    align-items: center;
    gap: 12rpx;
    background: $bg;
    border-radius: 8rpx;
    padding: 16rpx 20rpx;

    input {
      flex: 1;
      font-size: 28rpx;
      color: $text-primary;
    }
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
  transition: all 0.2s;

  .filter-text {
    font-size: 26rpx;
    color: $text-secondary;
  }

  &.active {
    background: $primary;

    .filter-text {
      color: $white;
    }
  }
}

// 列表容器
.list-container {
  height: calc(100vh - 400rpx);
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
    margin-top: 20rpx;
  }

  .state-title {
    font-size: 32rpx;
    font-weight: 500;
    color: $text-primary;
    margin-top: 24rpx;
    margin-bottom: 12rpx;
  }

  .state-desc {
    font-size: 26rpx;
    color: $text-muted;
  }
}

// 模板卡片
.template-card {
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
    margin-bottom: 12rpx;
  }

  .card-title {
    flex: 1;
    font-size: 30rpx;
    font-weight: 600;
    color: $text-primary;
    line-height: 1.4;
  }

  .card-meta {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  .meta-code {
    font-size: 24rpx;
    color: $text-muted;
    font-family: monospace;
  }

  .type-tag {
    font-size: 22rpx;
    padding: 4rpx 12rpx;
    border-radius: 4rpx;

    &.type-1 {
      background: #DBEAFE;
      color: #2563EB;
    }

    &.type-2 {
      background: #FEF3C7;
      color: #D97706;
    }

    &.type-3 {
      background: #F3E8FF;
      color: #9333EA;
    }
  }
}

.status-badge {
  flex-shrink: 0;
  margin-left: 16rpx;
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.enabled {
    background: #D1FAE5;
    color: #059669;
  }

  &.disabled {
    background: #F3F4F6;
    color: #9CA3AF;
  }
}

.card-body {
  padding: 20rpx 24rpx;

  .description {
    display: block;
    font-size: 26rpx;
    color: $text-secondary;
    line-height: 1.5;
    margin-bottom: 16rpx;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .stat-row {
    display: flex;
    justify-content: space-around;
    padding: 16rpx 0;
    background: $bg;
    border-radius: 8rpx;
  }

  .stat-item {
    text-align: center;

    .stat-value {
      display: block;
      font-size: 32rpx;
      font-weight: 600;
      color: $primary;
    }

    .stat-label {
      display: block;
      font-size: 22rpx;
      color: $text-muted;
      margin-top: 4rpx;
    }
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 24rpx;
  border-top: 1rpx solid $border;
  background: $bg;

  .time-text {
    font-size: 24rpx;
    color: $text-muted;
  }

  .arrow-icon {
    width: 40rpx;
    height: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
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
</style>
