<template>
  <view class="page">
    <!-- 顶部安全区 -->
    <view class="safe-top"></view>

    <!-- 页面头部 -->
    <view class="page-header">
      <view class="header-left" @click="goBack">
        <text class="back-icon">&lt;</text>
        <text class="back-text">返回</text>
      </view>
      <view class="header-center">
        <text class="header-title">{{ scoringStore.currentCheckName }}</text>
        <text class="header-date">{{ scoringStore.currentCheckDate }}</text>
      </view>
      <view class="header-right">
        <view
          class="sync-status"
          :class="{ syncing: scoringStore.isSyncing, unsync: scoringStore.hasUnsyncedChanges }"
        >
          <text v-if="scoringStore.isSyncing">同步中...</text>
          <text v-else-if="scoringStore.hasUnsyncedChanges">未同步</text>
          <text v-else>已同步</text>
        </view>
      </view>
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="loading-box">
      <text>加载中...</text>
    </view>

    <!-- 主内容区 -->
    <view v-else class="main-content">
      <!-- 类别标签 -->
      <view class="category-tabs">
        <scroll-view scroll-x :show-scrollbar="false" class="tabs-scroll">
          <view class="tabs-list">
            <view
              v-for="cat in scoringStore.categories"
              :key="cat.categoryId"
              class="tab-item"
              :class="{ active: String(scoringStore.activeCategoryId) === String(cat.categoryId) }"
              @click="selectCategory(cat.categoryId)"
            >
              <text class="tab-name">{{ cat.categoryName }}</text>
              <text v-if="cat.linkType === 1" class="tab-badge">宿舍</text>
              <text v-else-if="cat.linkType === 2" class="tab-badge">教室</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 筛选栏 - 班级类别 -->
      <view v-if="!isLinkCategory" class="filter-section">
        <view class="filter-row">
          <picker
            :range="gradePickerData"
            range-key="name"
            @change="onGradeChange"
          >
            <view class="filter-picker">
              <text>年级: {{ selectedGradeName || '全部' }}</text>
              <text class="arrow">▼</text>
            </view>
          </picker>
          <picker
            :range="deptPickerData"
            range-key="name"
            @change="onDeptChange"
          >
            <view class="filter-picker">
              <text>部门: {{ selectedDeptName || '全部' }}</text>
              <text class="arrow">▼</text>
            </view>
          </picker>
        </view>
      </view>

      <!-- 筛选栏 - 宿舍/教室类别 -->
      <view v-else class="filter-section">
        <view class="filter-row">
          <picker :range="gradePickerData" range-key="name" @change="onLinkGradeChange">
            <view class="filter-picker small">
              <text>年级: {{ selectedLinkGradeName || '全部' }}</text>
              <text class="arrow">▼</text>
            </view>
          </picker>
          <picker :range="deptPickerData" range-key="name" @change="onLinkDeptChange">
            <view class="filter-picker small">
              <text>部门: {{ selectedLinkDeptName || '全部' }}</text>
              <text class="arrow">▼</text>
            </view>
          </picker>
        </view>
        <view class="filter-row">
          <picker :range="classPickerData" range-key="name" @change="onLinkClassChange">
            <view class="filter-picker small">
              <text>班级: {{ selectedLinkClassName || '全部' }}</text>
              <text class="arrow">▼</text>
            </view>
          </picker>
          <picker :range="buildingPickerData" range-key="name" @change="onBuildingChange">
            <view class="filter-picker small">
              <text>楼栋: {{ selectedBuildingName || '全部' }}</text>
              <text class="arrow">▼</text>
            </view>
          </picker>
        </view>
      </view>

      <!-- 目标选择 - 班级 -->
      <view v-if="!isLinkCategory" class="target-section">
        <scroll-view scroll-x :show-scrollbar="false" class="target-scroll">
          <view class="target-list">
            <view
              v-for="cls in scoringStore.filteredClasses"
              :key="cls.classId"
              class="target-item"
              :class="{ active: String(scoringStore.activeClassId) === String(cls.classId) }"
              @click="selectClass(cls.classId)"
            >
              <text class="target-name">{{ cls.className }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 目标选择 - 宿舍/教室 -->
      <view v-else class="target-section link-targets">
        <scroll-view scroll-y class="link-scroll" :style="{ height: '240rpx' }">
          <view class="building-groups">
            <view
              v-for="(group, building) in groupedLinkResources"
              :key="building"
              class="building-group"
            >
              <text class="building-name">{{ building }}</text>
              <view class="link-grid">
                <view
                  v-for="item in group"
                  :key="item.id"
                  class="link-item"
                  :class="{ active: String(activeLinkId) === String(item.id) }"
                  @click="selectLink(item)"
                >
                  <text>{{ isLinkCategory === 1 ? item.dormitoryNo : item.classroomNo }}</text>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 当前目标信息 -->
      <view class="current-target-info">
        <text class="target-label">当前目标:</text>
        <text class="target-value">{{ currentTargetName }}</text>
        <view class="round-picker" v-if="currentCategory && currentCategory.checkRounds > 1">
          <text>轮次:</text>
          <picker :range="roundOptions" @change="onRoundChange">
            <view class="round-value">{{ scoringStore.currentRound }}</view>
          </picker>
        </view>
      </view>

      <!-- 扣分项列表 -->
      <view class="deduction-list">
        <view
          v-for="(item, index) in scoringStore.currentDeductionItems"
          :key="item.id"
          class="deduction-card"
        >
          <view class="card-header">
            <text class="item-index">{{ index + 1 }}.</text>
            <text class="item-name">{{ item.itemName }}</text>
            <text class="item-mode">{{ getDeductModeText(item.deductMode) }}</text>
          </view>

          <view class="card-body">
            <!-- 固定扣分 / 按人数扣分 -->
            <view v-if="item.deductMode !== 3" class="input-row">
              <view class="number-input">
                <view class="btn minus" @click="decreaseValue(item)">-</view>
                <input
                  type="number"
                  class="input-value"
                  :value="getInputValue(item)"
                  @input="onInputChange($event, item)"
                />
                <view class="btn plus" @click="increaseValue(item)">+</view>
              </view>
              <view class="deduct-result">
                <text class="label">扣分:</text>
                <text class="value">{{ getDeductScore(item) }}</text>
              </view>
            </view>

            <!-- 范围扣分 -->
            <view v-else class="range-row">
              <picker
                :range="getRangeOptions(item)"
                range-key="label"
                @change="onRangeChange($event, item)"
              >
                <view class="range-picker">
                  <text>{{ getSelectedRangeLabel(item) }}</text>
                  <text class="arrow">▼</text>
                </view>
              </picker>
              <view class="deduct-result">
                <text class="label">扣分:</text>
                <text class="value">{{ getDeductScore(item) }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 空状态 -->
        <view v-if="scoringStore.currentDeductionItems.length === 0" class="empty-state">
          <text>该类别暂无扣分项</text>
        </view>
      </view>

      <!-- 底部汇总 -->
      <view class="footer-summary">
        <view class="summary-info">
          <text class="summary-label">当前目标总扣分:</text>
          <text class="summary-value">{{ currentTargetTotalDeduction }}</text>
        </view>
        <view class="action-buttons">
          <view class="btn-save" @click="handleSave">
            <text>{{ scoringStore.isSyncing ? '保存中...' : '保存' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useScoringStore } from '@/stores/scoring'
import type { DeductionItemInfo, DormitoryInfo, ClassroomInfo, ScoreRangeItem } from '@/types/scoring'

// Store
const scoringStore = useScoringStore()

// 状态
const loading = ref(true)
const checkId = ref<string>('')
const activeLinkId = ref<string | number>('')
const activeLinkItem = ref<DormitoryInfo | ClassroomInfo | null>(null)

// 自动保存定时器
let autoSaveTimer: number | null = null

// 获取路由参数
onMounted(async () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1] as any
  checkId.value = currentPage.options?.id || ''

  if (checkId.value) {
    await initData()
  } else {
    uni.showToast({ title: '参数错误', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
  }

  // 启动自动保存
  startAutoSave()
})

onUnmounted(() => {
  stopAutoSave()
})

// 初始化数据
async function initData() {
  loading.value = true
  try {
    const success = await scoringStore.initScoring(checkId.value)
    if (!success) {
      uni.showToast({ title: '加载失败', icon: 'none' })
    }
  } finally {
    loading.value = false
  }
}

// 自动保存
function startAutoSave() {
  autoSaveTimer = setInterval(() => {
    if (scoringStore.hasUnsyncedChanges && !scoringStore.isSyncing) {
      scoringStore.syncPendingChanges()
    }
  }, 3000) as unknown as number
}

function stopAutoSave() {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
}

// 计算属性
const currentCategory = computed(() => scoringStore.activeCategory)

const isLinkCategory = computed(() => {
  return currentCategory.value?.linkType || 0
})

// 年级选择器数据
const gradePickerData = computed(() => {
  return [{ id: '', name: '全部' }, ...scoringStore.gradeOptions]
})

// 部门选择器数据
const deptPickerData = computed(() => {
  return [{ id: '', name: '全部' }, ...scoringStore.departmentOptions]
})

// 班级选择器数据 (用于宿舍筛选)
const classPickerData = computed(() => {
  let classes = scoringStore.targetClasses
  if (scoringStore.filterLinkGrade) {
    classes = classes.filter(c => String(c.gradeId) === scoringStore.filterLinkGrade)
  }
  if (scoringStore.filterLinkDepartment) {
    classes = classes.filter(c => String(c.departmentId) === scoringStore.filterLinkDepartment)
  }
  return [{ id: '', name: '全部' }, ...classes.map(c => ({ id: String(c.classId), name: c.className }))]
})

// 楼栋选择器数据
const buildingPickerData = computed(() => {
  const buildings = new Set<string>()
  const linkRes = scoringStore.currentLinkResource
  if (linkRes?.classResources) {
    linkRes.classResources.forEach(cr => {
      if (linkRes.linkType === 1 && cr.dormitories) {
        cr.dormitories.forEach(d => buildings.add(d.buildingName))
      } else if (linkRes.linkType === 2 && cr.classrooms) {
        cr.classrooms.forEach(c => buildings.add(c.buildingName))
      }
    })
  }
  return [{ id: '', name: '全部' }, ...Array.from(buildings).map(b => ({ id: b, name: b }))]
})

// 分组后的关联资源
const groupedLinkResources = computed(() => {
  const linkRes = scoringStore.currentLinkResource
  if (!linkRes?.classResources) return {}

  const groups: Record<string, (DormitoryInfo | ClassroomInfo)[]> = {}

  linkRes.classResources.forEach(cr => {
    // 筛选班级
    if (scoringStore.filterLinkGrade) {
      const cls = scoringStore.targetClasses.find(c => String(c.classId) === String(cr.classId))
      if (!cls || String(cls.gradeId) !== scoringStore.filterLinkGrade) return
    }
    if (scoringStore.filterLinkDepartment) {
      const cls = scoringStore.targetClasses.find(c => String(c.classId) === String(cr.classId))
      if (!cls || String(cls.departmentId) !== scoringStore.filterLinkDepartment) return
    }
    if (scoringStore.filterLinkClass && String(cr.classId) !== scoringStore.filterLinkClass) return

    const items = linkRes.linkType === 1 ? cr.dormitories : cr.classrooms
    items?.forEach(item => {
      const building = item.buildingName
      if (scoringStore.filterLinkBuilding && building !== scoringStore.filterLinkBuilding) return
      if (!groups[building]) groups[building] = []
      groups[building].push(item)
    })
  })

  return groups
})

// 选中的筛选名称
const selectedGradeName = computed(() => {
  if (!scoringStore.filterGrade) return ''
  return scoringStore.gradeOptions.find(g => g.id === scoringStore.filterGrade)?.name || ''
})

const selectedDeptName = computed(() => {
  if (!scoringStore.filterDepartment) return ''
  return scoringStore.departmentOptions.find(d => d.id === scoringStore.filterDepartment)?.name || ''
})

const selectedLinkGradeName = computed(() => {
  if (!scoringStore.filterLinkGrade) return ''
  return scoringStore.gradeOptions.find(g => g.id === scoringStore.filterLinkGrade)?.name || ''
})

const selectedLinkDeptName = computed(() => {
  if (!scoringStore.filterLinkDepartment) return ''
  return scoringStore.departmentOptions.find(d => d.id === scoringStore.filterLinkDepartment)?.name || ''
})

const selectedLinkClassName = computed(() => {
  if (!scoringStore.filterLinkClass) return ''
  const cls = scoringStore.targetClasses.find(c => String(c.classId) === scoringStore.filterLinkClass)
  return cls?.className || ''
})

const selectedBuildingName = computed(() => scoringStore.filterLinkBuilding || '')

// 轮次选项
const roundOptions = computed(() => {
  const rounds = currentCategory.value?.checkRounds || 1
  return Array.from({ length: rounds }, (_, i) => String(i + 1))
})

// 当前目标名称
const currentTargetName = computed(() => {
  if (isLinkCategory.value) {
    if (activeLinkItem.value) {
      const item = activeLinkItem.value as any
      return item.dormitoryNo || item.classroomNo || '未选择'
    }
    return '请选择宿舍/教室'
  }
  const cls = scoringStore.targetClasses.find(c => String(c.classId) === String(scoringStore.activeClassId))
  return cls?.className || '请选择班级'
})

// 当前目标总扣分
const currentTargetTotalDeduction = computed(() => {
  if (isLinkCategory.value && activeLinkId.value) {
    return scoringStore.getTargetTotalDeduction(
      scoringStore.activeClassId,
      scoringStore.activeCategoryId,
      activeLinkId.value
    )
  }
  return scoringStore.getTargetTotalDeduction(scoringStore.activeClassId, scoringStore.activeCategoryId)
})

// 事件处理
function goBack() {
  // 保存后返回
  if (scoringStore.hasUnsyncedChanges) {
    scoringStore.syncPendingChanges().then(() => {
      uni.navigateBack()
    })
  } else {
    uni.navigateBack()
  }
}

function selectCategory(categoryId: string | number) {
  scoringStore.activeCategoryId = categoryId
  // 重置关联资源选择
  activeLinkId.value = ''
  activeLinkItem.value = null
}

function selectClass(classId: string | number) {
  scoringStore.activeClassId = classId
}

function selectLink(item: DormitoryInfo | ClassroomInfo) {
  activeLinkId.value = item.id
  activeLinkItem.value = item
  // 找到对应的班级
  const linkRes = scoringStore.currentLinkResource
  if (linkRes?.classResources) {
    for (const cr of linkRes.classResources) {
      const items = linkRes.linkType === 1 ? cr.dormitories : cr.classrooms
      if (items?.some(i => String(i.id) === String(item.id))) {
        scoringStore.activeClassId = cr.classId
        break
      }
    }
  }
}

// 筛选事件
function onGradeChange(e: any) {
  const index = e.detail.value
  scoringStore.filterGrade = gradePickerData.value[index]?.id || ''
}

function onDeptChange(e: any) {
  const index = e.detail.value
  scoringStore.filterDepartment = deptPickerData.value[index]?.id || ''
}

function onLinkGradeChange(e: any) {
  const index = e.detail.value
  scoringStore.filterLinkGrade = gradePickerData.value[index]?.id || ''
}

function onLinkDeptChange(e: any) {
  const index = e.detail.value
  scoringStore.filterLinkDepartment = deptPickerData.value[index]?.id || ''
}

function onLinkClassChange(e: any) {
  const index = e.detail.value
  scoringStore.filterLinkClass = classPickerData.value[index]?.id || ''
}

function onBuildingChange(e: any) {
  const index = e.detail.value
  scoringStore.filterLinkBuilding = buildingPickerData.value[index]?.id || ''
}

function onRoundChange(e: any) {
  scoringStore.currentRound = parseInt(roundOptions.value[e.detail.value]) || 1
}

// 扣分模式文本
function getDeductModeText(mode: number): string {
  const texts: Record<number, string> = {
    1: '固定扣分',
    2: '按人数扣分',
    3: '范围扣分'
  }
  return texts[mode] || ''
}

// 获取当前输入值
function getInputValue(item: DeductionItemInfo): number {
  const linkId = isLinkCategory.value ? activeLinkId.value : scoringStore.activeClassId
  const score = scoringStore.getScore(
    scoringStore.activeClassId,
    scoringStore.activeCategoryId,
    item.id,
    linkId,
    scoringStore.currentRound
  )
  return score?.inputValue || 0
}

// 获取扣分
function getDeductScore(item: DeductionItemInfo): number {
  const linkId = isLinkCategory.value ? activeLinkId.value : scoringStore.activeClassId
  const score = scoringStore.getScore(
    scoringStore.activeClassId,
    scoringStore.activeCategoryId,
    item.id,
    linkId,
    scoringStore.currentRound
  )
  return score?.deductScore || 0
}

// 更新分数
function updateItemScore(item: DeductionItemInfo, inputValue: number) {
  const linkId = isLinkCategory.value ? activeLinkId.value : scoringStore.activeClassId
  if (!linkId) {
    uni.showToast({ title: '请先选择目标', icon: 'none' })
    return
  }
  scoringStore.updateScore(
    scoringStore.activeClassId,
    scoringStore.activeCategoryId,
    item.id,
    isLinkCategory.value || 0,
    linkId,
    inputValue,
    item
  )
}

// 增加值
function increaseValue(item: DeductionItemInfo) {
  const current = getInputValue(item)
  updateItemScore(item, current + 1)
}

// 减少值
function decreaseValue(item: DeductionItemInfo) {
  const current = getInputValue(item)
  if (current > 0) {
    updateItemScore(item, current - 1)
  }
}

// 输入变化
function onInputChange(e: any, item: DeductionItemInfo) {
  const value = parseInt(e.detail.value) || 0
  updateItemScore(item, Math.max(0, value))
}

// 范围选项
function getRangeOptions(item: DeductionItemInfo): ScoreRangeItem[] {
  if (!item.rangeConfig) return []
  try {
    return JSON.parse(item.rangeConfig)
  } catch {
    return []
  }
}

// 获取选中的范围标签
function getSelectedRangeLabel(item: DeductionItemInfo): string {
  const inputValue = getInputValue(item)
  const ranges = getRangeOptions(item)
  if (inputValue >= 0 && inputValue < ranges.length) {
    return ranges[inputValue].label
  }
  return '请选择'
}

// 范围选择变化
function onRangeChange(e: any, item: DeductionItemInfo) {
  const index = e.detail.value
  updateItemScore(item, index)
}

// 手动保存
async function handleSave() {
  if (scoringStore.isSyncing) return

  const success = await scoringStore.syncPendingChanges()
  if (success) {
    uni.showToast({ title: '保存成功', icon: 'success' })
  } else {
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
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
$success: #059669;

.page {
  min-height: 100vh;
  background: $bg;
  display: flex;
  flex-direction: column;
}

.safe-top {
  height: env(safe-area-inset-top);
  background: $white;
}

.safe-bottom {
  height: calc(140rpx + env(safe-area-inset-bottom));
}

// 页面头部
.page-header {
  background: $white;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid $border;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx;

  .back-icon {
    font-size: 32rpx;
    color: $primary;
  }

  .back-text {
    font-size: 28rpx;
    color: $primary;
  }
}

.header-center {
  flex: 1;
  text-align: center;

  .header-title {
    display: block;
    font-size: 32rpx;
    font-weight: 600;
    color: $text-primary;
  }

  .header-date {
    display: block;
    font-size: 24rpx;
    color: $text-muted;
  }
}

.header-right {
  .sync-status {
    padding: 8rpx 16rpx;
    border-radius: 8rpx;
    font-size: 22rpx;
    background: #D1FAE5;
    color: $success;

    &.syncing {
      background: #FEF3C7;
      color: #D97706;
    }

    &.unsync {
      background: #FEE2E2;
      color: $danger;
    }
  }
}

// 加载状态
.loading-box {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    font-size: 28rpx;
    color: $text-muted;
  }
}

// 主内容
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// 类别标签
.category-tabs {
  background: $white;
  border-bottom: 1rpx solid $border;
}

.tabs-scroll {
  white-space: nowrap;
}

.tabs-list {
  display: inline-flex;
  padding: 16rpx 20rpx;
  gap: 12rpx;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 24rpx;
  border-radius: 8rpx;
  background: $bg;
  transition: all 0.2s;

  .tab-name {
    font-size: 26rpx;
    color: $text-secondary;
  }

  .tab-badge {
    font-size: 20rpx;
    padding: 2rpx 8rpx;
    background: rgba(79, 70, 229, 0.1);
    color: $primary;
    border-radius: 4rpx;
  }

  &.active {
    background: $primary;

    .tab-name {
      color: $white;
    }

    .tab-badge {
      background: rgba(255, 255, 255, 0.2);
      color: $white;
    }
  }
}

// 筛选区
.filter-section {
  background: $white;
  padding: 16rpx 20rpx;
  border-bottom: 1rpx solid $border;
}

.filter-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 12rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.filter-picker {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 20rpx;
  background: $bg;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: $text-primary;

  &.small {
    padding: 12rpx 16rpx;
    font-size: 24rpx;
  }

  .arrow {
    font-size: 20rpx;
    color: $text-muted;
  }
}

// 目标选择
.target-section {
  background: $white;
  border-bottom: 1rpx solid $border;
}

.target-scroll {
  white-space: nowrap;
}

.target-list {
  display: inline-flex;
  padding: 16rpx 20rpx;
  gap: 12rpx;
}

.target-item {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 20rpx;
  border-radius: 8rpx;
  background: $bg;
  border: 2rpx solid transparent;

  .target-name {
    font-size: 26rpx;
    color: $text-secondary;
  }

  &.active {
    background: $primary-light;
    border-color: $primary;

    .target-name {
      color: $primary;
      font-weight: 500;
    }
  }
}

// 关联资源目标
.link-targets {
  padding: 16rpx 20rpx;
}

.link-scroll {
  background: $bg;
  border-radius: 8rpx;
  padding: 12rpx;
}

.building-group {
  margin-bottom: 16rpx;

  &:last-child {
    margin-bottom: 0;
  }

  .building-name {
    display: block;
    font-size: 24rpx;
    color: $text-muted;
    margin-bottom: 8rpx;
    padding-left: 8rpx;
  }
}

.link-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.link-item {
  padding: 12rpx 20rpx;
  background: $white;
  border-radius: 8rpx;
  border: 2rpx solid $border;
  font-size: 26rpx;
  color: $text-secondary;

  &.active {
    background: $primary-light;
    border-color: $primary;
    color: $primary;
    font-weight: 500;
  }
}

// 当前目标信息
.current-target-info {
  background: $white;
  padding: 16rpx 20rpx;
  border-bottom: 1rpx solid $border;
  display: flex;
  align-items: center;
  gap: 12rpx;

  .target-label {
    font-size: 26rpx;
    color: $text-muted;
  }

  .target-value {
    font-size: 28rpx;
    color: $primary;
    font-weight: 500;
  }

  .round-picker {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8rpx;
    font-size: 26rpx;
    color: $text-secondary;

    .round-value {
      padding: 8rpx 16rpx;
      background: $bg;
      border-radius: 6rpx;
      color: $text-primary;
    }
  }
}

// 扣分项列表
.deduction-list {
  flex: 1;
  overflow-y: auto;
  padding: 16rpx 20rpx;
}

.deduction-card {
  background: $white;
  border-radius: 12rpx;
  border: 1rpx solid $border;
  margin-bottom: 16rpx;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  padding: 20rpx;
  border-bottom: 1rpx solid $border;
  gap: 12rpx;

  .item-index {
    font-size: 28rpx;
    color: $text-muted;
    font-weight: 500;
  }

  .item-name {
    flex: 1;
    font-size: 28rpx;
    color: $text-primary;
    font-weight: 500;
  }

  .item-mode {
    font-size: 22rpx;
    padding: 4rpx 12rpx;
    background: $bg;
    border-radius: 4rpx;
    color: $text-muted;
  }
}

.card-body {
  padding: 20rpx;
}

.input-row,
.range-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.number-input {
  display: flex;
  align-items: center;
  background: $bg;
  border-radius: 8rpx;
  overflow: hidden;

  .btn {
    width: 72rpx;
    height: 72rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 36rpx;
    color: $text-secondary;

    &.minus {
      border-right: 1rpx solid $border;
    }

    &.plus {
      border-left: 1rpx solid $border;
    }

    &:active {
      background: $border;
    }
  }

  .input-value {
    width: 120rpx;
    height: 72rpx;
    text-align: center;
    font-size: 32rpx;
    color: $text-primary;
    font-weight: 500;
  }
}

.range-picker {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 24rpx;
  background: $bg;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: $text-primary;

  .arrow {
    font-size: 20rpx;
    color: $text-muted;
  }
}

.deduct-result {
  display: flex;
  align-items: center;
  gap: 8rpx;

  .label {
    font-size: 26rpx;
    color: $text-muted;
  }

  .value {
    font-size: 32rpx;
    color: $danger;
    font-weight: 600;
    min-width: 60rpx;
    text-align: right;
  }
}

.empty-state {
  padding: 80rpx 40rpx;
  text-align: center;

  text {
    font-size: 28rpx;
    color: $text-muted;
  }
}

// 底部汇总
.footer-summary {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $white;
  border-top: 1rpx solid $border;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-info {
  display: flex;
  align-items: center;
  gap: 12rpx;

  .summary-label {
    font-size: 26rpx;
    color: $text-secondary;
  }

  .summary-value {
    font-size: 36rpx;
    color: $danger;
    font-weight: 600;
  }
}

.action-buttons {
  .btn-save {
    padding: 20rpx 48rpx;
    background: $primary;
    border-radius: 8rpx;

    text {
      font-size: 28rpx;
      color: $white;
      font-weight: 500;
    }

    &:active {
      opacity: 0.8;
    }
  }
}
</style>
