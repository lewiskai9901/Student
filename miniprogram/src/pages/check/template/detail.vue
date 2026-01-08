<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 导航栏 -->
    <view class="nav-bar">
      <view class="nav-back" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#111827" />
      </view>
      <text class="nav-title">模板详情</text>
      <view class="nav-placeholder"></view>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="loading-container">
      <u-loading-icon size="40" color="#4F46E5" />
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 内容 -->
    <scroll-view v-else scroll-y class="content">
      <!-- 基本信息 -->
      <view class="info-card">
        <view class="card-header">
          <view class="title-row">
            <text class="card-title">{{ template.templateName }}</text>
            <view class="status-badge" :class="template.status === 1 ? 'enabled' : 'disabled'">
              {{ template.status === 1 ? '启用' : '禁用' }}
            </view>
          </view>
          <view class="meta-row">
            <text class="meta-code">{{ template.templateCode }}</text>
            <view class="type-tag" :class="'type-' + template.templateType">
              {{ getTypeName(template.templateType) }}
            </view>
          </view>
        </view>

        <view class="card-body">
          <view v-if="template.description" class="description-section">
            <text class="section-label">描述</text>
            <text class="description-text">{{ template.description }}</text>
          </view>

          <view class="stat-grid">
            <view class="stat-item">
              <text class="stat-value">{{ template.categoryCount || 0 }}</text>
              <text class="stat-label">检查类别</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ template.itemCount || 0 }}</text>
              <text class="stat-label">扣分项数</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ template.totalScore || 100 }}</text>
              <text class="stat-label">满分</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 类别列表 -->
      <view class="section-card">
        <view class="section-header">
          <text class="section-title">检查类别</text>
          <text class="section-count">共 {{ categories.length }} 个类别</text>
        </view>

        <view class="category-list">
          <view
            v-for="category in categories"
            :key="category.id"
            class="category-item"
          >
            <view class="category-header" @click="toggleCategory(category.id)">
              <view class="category-info">
                <view class="category-name-row">
                  <text class="category-name">{{ category.categoryName }}</text>
                  <text class="category-code">{{ category.categoryCode }}</text>
                </view>
                <view class="category-meta">
                  <text class="meta-item">{{ category.itemCount || 0 }} 个扣分项</text>
                  <text v-if="category.weight" class="meta-item">权重: {{ category.weight }}%</text>
                  <text v-if="category.maxScore" class="meta-item">最高扣: {{ category.maxScore }}分</text>
                </view>
              </view>
              <view class="expand-icon" :class="{ expanded: expandedCategories.includes(category.id) }">
                <u-icon name="arrow-down" size="16" color="#9CA3AF" />
              </view>
            </view>

            <!-- 扣分项列表 -->
            <view
              v-if="expandedCategories.includes(category.id)"
              class="deduction-list"
            >
              <view v-if="!category.items?.length" class="empty-items">
                <text>暂无扣分项</text>
              </view>
              <view
                v-for="item in category.items"
                :key="item.id"
                class="deduction-item"
              >
                <view class="item-header">
                  <text class="item-name">{{ item.itemName }}</text>
                  <view class="deduction-mode" :class="'mode-' + item.deductionMode">
                    {{ getDeductionModeName(item.deductionMode) }}
                  </view>
                </view>
                <view v-if="item.description" class="item-desc">
                  {{ item.description }}
                </view>
                <view class="item-score">
                  <template v-if="item.deductionMode === 1">
                    <text class="score-text">扣分: </text>
                    <text class="score-value">-{{ item.baseScore }}分</text>
                  </template>
                  <template v-else-if="item.deductionMode === 2">
                    <text class="score-text">每人扣: </text>
                    <text class="score-value">-{{ item.perPersonScore || item.baseScore }}分</text>
                    <text v-if="item.maxScore" class="score-text"> (最高{{ item.maxScore }}分)</text>
                  </template>
                  <template v-else-if="item.deductionMode === 3">
                    <text class="score-text">分数区间: </text>
                    <text class="score-value">{{ item.minScore || 0 }} ~ {{ item.maxScore }}分</text>
                  </template>
                </view>
              </view>
            </view>
          </view>

          <!-- 空状态 -->
          <view v-if="!categories.length" class="empty-categories">
            <text>暂无检查类别</text>
          </view>
        </view>
      </view>

      <!-- 时间信息 -->
      <view class="time-info">
        <view class="time-item">
          <text class="time-label">创建时间</text>
          <text class="time-value">{{ template.createdAt || '-' }}</text>
        </view>
        <view class="time-item">
          <text class="time-label">更新时间</text>
          <text class="time-value">{{ template.updatedAt || '-' }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTemplateById, getTemplateCategories, getCategoryItems, type CheckTemplate, type TemplateCategory } from '@/api/checkTemplate'

// 状态
const loading = ref(true)
const templateId = ref<string>('')
const template = ref<CheckTemplate>({
  id: '',
  templateCode: '',
  templateName: '',
  status: 0
})
const categories = ref<TemplateCategory[]>([])
const expandedCategories = ref<(string | number)[]>([])

// 初始化
onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  templateId.value = (currentPage as any).options?.id || ''

  if (templateId.value) {
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
    // 加载模板基本信息
    const templateData = await getTemplateById(templateId.value)
    template.value = templateData

    // 加载类别列表
    const categoryData = await getTemplateCategories(templateId.value)
    categories.value = categoryData || []

    // 默认展开第一个类别
    if (categories.value.length > 0) {
      expandedCategories.value = [categories.value[0].id]
      await loadCategoryItems(categories.value[0].id)
    }
  } catch (error) {
    console.error('加载模板详情失败:', error)
    // 使用模拟数据
    template.value = {
      id: templateId.value,
      templateCode: 'TPL001',
      templateName: '日常卫生检查模板',
      templateType: 1,
      description: '用于日常卫生检查的标准模板，包含卫生、纪律、安全等检查类别',
      categoryCount: 5,
      itemCount: 25,
      totalScore: 100,
      status: 1,
      createdAt: '2024-12-01 10:00:00',
      updatedAt: '2024-12-10 10:00:00'
    }

    categories.value = [
      {
        id: 1,
        templateId: Number(templateId.value),
        categoryCode: 'CAT001',
        categoryName: '卫生检查',
        sort: 1,
        weight: 40,
        maxScore: 40,
        itemCount: 8,
        items: [
          {
            id: 1,
            categoryId: 1,
            itemCode: 'ITEM001',
            itemName: '地面不干净',
            description: '地面有明显灰尘、污渍或垃圾',
            deductionMode: 1,
            baseScore: 2,
            sort: 1,
            status: 1
          },
          {
            id: 2,
            categoryId: 1,
            itemCode: 'ITEM002',
            itemName: '床铺不整齐',
            description: '被子未叠好、床单有褶皱',
            deductionMode: 1,
            baseScore: 2,
            sort: 2,
            status: 1
          },
          {
            id: 3,
            categoryId: 1,
            itemCode: 'ITEM003',
            itemName: '物品摆放杂乱',
            description: '桌面、柜子物品摆放不整齐',
            deductionMode: 2,
            baseScore: 1,
            perPersonScore: 1,
            maxScore: 5,
            sort: 3,
            status: 1
          }
        ]
      },
      {
        id: 2,
        templateId: Number(templateId.value),
        categoryCode: 'CAT002',
        categoryName: '纪律检查',
        sort: 2,
        weight: 30,
        maxScore: 30,
        itemCount: 5,
        items: []
      },
      {
        id: 3,
        templateId: Number(templateId.value),
        categoryCode: 'CAT003',
        categoryName: '安全检查',
        sort: 3,
        weight: 30,
        maxScore: 30,
        itemCount: 6,
        items: []
      }
    ]

    expandedCategories.value = [1]
  } finally {
    loading.value = false
  }
}

// 加载类别扣分项
async function loadCategoryItems(categoryId: string | number) {
  const category = categories.value.find(c => c.id === categoryId)
  if (!category || category.items?.length) return

  try {
    const items = await getCategoryItems(categoryId)
    category.items = items || []
  } catch (error) {
    console.error('加载扣分项失败:', error)
  }
}

// 展开/收起类别
async function toggleCategory(categoryId: string | number) {
  const index = expandedCategories.value.indexOf(categoryId)
  if (index > -1) {
    expandedCategories.value.splice(index, 1)
  } else {
    expandedCategories.value.push(categoryId)
    await loadCategoryItems(categoryId)
  }
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

// 获取扣分模式名称
function getDeductionModeName(mode: number): string {
  const modeMap: Record<number, string> = {
    1: '固定扣分',
    2: '按人数扣',
    3: '分数区间'
  }
  return modeMap[mode] || '未知'
}

// 返回
function goBack() {
  uni.navigateBack()
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
$danger: #DC2626;

.page {
  min-height: 100vh;
  background: $bg;
}

.safe-top {
  height: env(safe-area-inset-top);
  background: $white;
}

.safe-bottom {
  height: calc(40rpx + env(safe-area-inset-bottom));
}

// 导航栏
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 24rpx;
  background: $white;
  border-bottom: 1rpx solid $border;

  .nav-back, .nav-placeholder {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .nav-title {
    font-size: 32rpx;
    font-weight: 600;
    color: $text-primary;
  }
}

// 加载中
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;

  .loading-text {
    font-size: 28rpx;
    color: $text-muted;
    margin-top: 20rpx;
  }
}

// 内容
.content {
  height: calc(100vh - 88rpx - env(safe-area-inset-top) - 40rpx - env(safe-area-inset-bottom));
  padding: 24rpx;
}

// 基本信息卡片
.info-card {
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;
  overflow: hidden;
  margin-bottom: 24rpx;

  .card-header {
    padding: 24rpx;
    border-bottom: 1rpx solid $border;

    .title-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12rpx;
    }

    .card-title {
      flex: 1;
      font-size: 34rpx;
      font-weight: 600;
      color: $text-primary;
    }

    .meta-row {
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

  .card-body {
    padding: 24rpx;

    .description-section {
      margin-bottom: 20rpx;

      .section-label {
        display: block;
        font-size: 24rpx;
        color: $text-muted;
        margin-bottom: 8rpx;
      }

      .description-text {
        font-size: 28rpx;
        color: $text-secondary;
        line-height: 1.6;
      }
    }

    .stat-grid {
      display: flex;
      justify-content: space-around;
      padding: 20rpx;
      background: $bg;
      border-radius: 8rpx;
    }

    .stat-item {
      text-align: center;

      .stat-value {
        display: block;
        font-size: 36rpx;
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

// 类别卡片
.section-card {
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;
  overflow: hidden;
  margin-bottom: 24rpx;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 24rpx;
    border-bottom: 1rpx solid $border;

    .section-title {
      font-size: 30rpx;
      font-weight: 600;
      color: $text-primary;
    }

    .section-count {
      font-size: 24rpx;
      color: $text-muted;
    }
  }
}

// 类别列表
.category-list {
  .category-item {
    border-bottom: 1rpx solid $border;

    &:last-child {
      border-bottom: none;
    }
  }

  .category-header {
    display: flex;
    align-items: center;
    padding: 20rpx 24rpx;

    .category-info {
      flex: 1;
    }

    .category-name-row {
      display: flex;
      align-items: center;
      gap: 12rpx;
      margin-bottom: 8rpx;
    }

    .category-name {
      font-size: 28rpx;
      font-weight: 500;
      color: $text-primary;
    }

    .category-code {
      font-size: 22rpx;
      color: $text-muted;
      font-family: monospace;
    }

    .category-meta {
      display: flex;
      gap: 16rpx;

      .meta-item {
        font-size: 24rpx;
        color: $text-muted;
      }
    }

    .expand-icon {
      width: 48rpx;
      height: 48rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: transform 0.2s;

      &.expanded {
        transform: rotate(180deg);
      }
    }
  }

  .empty-categories {
    padding: 60rpx 0;
    text-align: center;
    color: $text-muted;
    font-size: 26rpx;
  }
}

// 扣分项列表
.deduction-list {
  background: $bg;
  padding: 16rpx 24rpx;

  .empty-items {
    padding: 40rpx 0;
    text-align: center;
    color: $text-muted;
    font-size: 24rpx;
  }

  .deduction-item {
    background: $white;
    border-radius: 8rpx;
    padding: 16rpx 20rpx;
    margin-bottom: 12rpx;

    &:last-child {
      margin-bottom: 0;
    }

    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8rpx;
    }

    .item-name {
      font-size: 26rpx;
      font-weight: 500;
      color: $text-primary;
    }

    .deduction-mode {
      font-size: 20rpx;
      padding: 4rpx 10rpx;
      border-radius: 4rpx;

      &.mode-1 {
        background: #FEF3C7;
        color: #D97706;
      }

      &.mode-2 {
        background: #DBEAFE;
        color: #2563EB;
      }

      &.mode-3 {
        background: #F3E8FF;
        color: #9333EA;
      }
    }

    .item-desc {
      font-size: 24rpx;
      color: $text-muted;
      line-height: 1.5;
      margin-bottom: 8rpx;
    }

    .item-score {
      .score-text {
        font-size: 24rpx;
        color: $text-secondary;
      }

      .score-value {
        font-size: 24rpx;
        font-weight: 500;
        color: $danger;
      }
    }
  }
}

// 时间信息
.time-info {
  padding: 20rpx 24rpx;

  .time-item {
    display: flex;
    justify-content: space-between;
    padding: 8rpx 0;

    .time-label {
      font-size: 24rpx;
      color: $text-muted;
    }

    .time-value {
      font-size: 24rpx;
      color: $text-secondary;
    }
  }
}
</style>
