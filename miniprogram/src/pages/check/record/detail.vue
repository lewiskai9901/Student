<template>
  <view class="page">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading-container">
      <view class="loading-spinner"></view>
      <text class="loading-text">加载中...</text>
    </view>

    <template v-else-if="recordDetail">
      <!-- 顶部信息栏 -->
      <view class="header-bar">
        <view class="header-info">
          <text class="header-title">{{ recordDetail.checkName }}</text>
          <text class="header-meta">{{ formatDate(recordDetail.checkDate) }} · {{ recordDetail.checkerName }}</text>
        </view>
        <view class="header-stats">
          <view class="stat-box">
            <text class="stat-num">{{ classList.length }}</text>
            <text class="stat-label">班级</text>
          </view>
          <view class="stat-box">
            <text class="stat-num danger">-{{ getTotalScore() }}</text>
            <text class="stat-label">总扣分</text>
          </view>
        </view>
      </view>

      <!-- 视图切换Tab -->
      <view class="view-tabs">
        <view
          class="tab-item"
          :class="{ active: activeView === 'detail' }"
          @click="activeView = 'detail'"
        >
          <text>班级详情</text>
        </view>
        <view
          class="tab-item"
          :class="{ active: activeView === 'ranking' }"
          @click="activeView = 'ranking'"
        >
          <text>排名视图</text>
        </view>
      </view>

      <!-- 班级详情视图 -->
      <template v-if="activeView === 'detail'">
        <!-- 班级选择器 -->
        <scroll-view class="class-selector" scroll-x :show-scrollbar="false">
          <view class="class-list-h">
            <view
              v-for="cls in classList"
              :key="cls.classId"
              class="class-chip"
              :class="{ active: activeClassId === String(cls.classId) }"
              @click="selectClass(cls)"
            >
              <text class="chip-name">{{ cls.className }}</text>
              <view class="chip-score" v-if="cls.totalScore > 0">
                <text>-{{ cls.totalScore }}</text>
                <text v-if="cls.weightEnabled && cls.weightedTotalScore" class="weighted">/-{{ formatScore(cls.weightedTotalScore) }}</text>
              </view>
              <text v-else class="chip-perfect">满分</text>
            </view>
          </view>
        </scroll-view>

        <!-- 当前班级详情 -->
        <template v-if="activeClass">
          <!-- 班级信息卡片 -->
          <view class="class-info-card">
            <view class="info-left">
              <view class="class-avatar">{{ activeClass.className?.charAt(0) || '班' }}</view>
              <view class="class-detail">
                <text class="class-name">{{ activeClass.className }}</text>
                <text class="class-meta">{{ activeClass.teacherName || '暂无班主任' }}<text v-if="activeClass.classSize"> · {{ activeClass.classSize }}人</text></text>
              </view>
            </view>
            <view class="info-right">
              <view class="score-block">
                <text class="score-value danger">-{{ activeClass.totalScore }}</text>
                <text class="score-label">扣分</text>
              </view>
              <view class="score-block" v-if="activeClass.weightEnabled && activeClass.weightedTotalScore">
                <text class="score-value primary">-{{ formatScore(activeClass.weightedTotalScore) }}</text>
                <text class="score-label">加权</text>
              </view>
              <view class="score-block">
                <text class="score-value">{{ getDeductionCount(activeClass) }}</text>
                <text class="score-label">扣分项</text>
              </view>
            </view>
          </view>

          <!-- 加权配置卡片 -->
          <view class="weight-card" v-if="activeClass.weightEnabled" @click="weightExpanded = !weightExpanded">
            <view class="weight-header">
              <view class="weight-icon">⚖</view>
              <view class="weight-info">
                <text class="weight-title">加权配置</text>
                <text class="weight-desc">班级{{ activeClass.classSize || '-' }}人，系数 x{{ formatWeightFactor(activeClass.weightFactor) }}</text>
              </view>
              <view class="weight-result">
                <text class="original">-{{ activeClass.totalScore }}</text>
                <text class="arrow">→</text>
                <text class="weighted">-{{ formatScore(activeClass.weightedTotalScore) }}</text>
              </view>
              <view class="expand-icon" :class="{ expanded: weightExpanded }">▼</view>
            </view>
            <view class="weight-detail" v-if="weightExpanded">
              <view class="detail-row">
                <text class="detail-label">加权模式</text>
                <text class="detail-value">{{ activeClass.weightModeDesc || '标准加权' }}</text>
              </view>
              <view class="detail-row">
                <text class="detail-label">标准人数</text>
                <text class="detail-value">{{ activeClass.standardSize || '-' }}人</text>
              </view>
              <view class="detail-row">
                <text class="detail-label">加权系数</text>
                <text class="detail-value primary">x{{ formatWeightFactor(activeClass.weightFactor) }}</text>
              </view>
              <!-- 多配置加权 -->
              <view v-if="activeClass.multiConfigEnabled && activeClass.multiWeightConfigs?.length" class="multi-config">
                <text class="config-title">分类加权配置</text>
                <view class="config-item" v-for="config in activeClass.multiWeightConfigs" :key="config.configId">
                  <view class="config-color" :style="{ background: config.colorCode || '#667eea' }"></view>
                  <text class="config-name">{{ config.configName }}</text>
                  <text class="config-categories">{{ config.applyCategoryNames?.join('、') || '默认' }}</text>
                  <text class="config-factor">x{{ formatWeightFactor(config.weightFactor) }}</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 筛选区域 -->
          <view class="filter-section" v-if="hasFilters">
            <!-- 轮次筛选 -->
            <view class="filter-group" v-if="availableRounds.length > 1">
              <text class="filter-label">轮次</text>
              <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
                <view class="filter-tags">
                  <view
                    class="filter-tag"
                    :class="{ active: activeRoundTab === 0 }"
                    @click="activeRoundTab = 0"
                  >
                    <text>全部</text>
                    <text class="tag-count">{{ allDeductions.length }}</text>
                  </view>
                  <view
                    v-for="round in availableRounds"
                    :key="round"
                    class="filter-tag round"
                    :class="{ active: activeRoundTab === round }"
                    @click="activeRoundTab = round"
                  >
                    <text>第{{ round }}轮</text>
                    <text class="tag-count">{{ getRoundCount(round) }}</text>
                  </view>
                </view>
              </scroll-view>
            </view>

            <!-- 位置筛选 -->
            <view class="filter-group" v-if="hasLocations">
              <text class="filter-label">位置</text>
              <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
                <view class="filter-tags">
                  <view
                    class="filter-tag"
                    :class="{ active: activeLocationTab === 'all' }"
                    @click="activeLocationTab = 'all'"
                  >
                    <text>全部</text>
                  </view>
                  <view
                    class="filter-tag dorm"
                    :class="{ active: activeLocationTab === 'dormitory' }"
                    @click="activeLocationTab = 'dormitory'"
                    v-if="getDormitoryCount() > 0"
                  >
                    <text>宿舍</text>
                    <text class="tag-count">{{ getDormitoryCount() }}</text>
                  </view>
                  <view
                    class="filter-tag classroom"
                    :class="{ active: activeLocationTab === 'classroom' }"
                    @click="activeLocationTab = 'classroom'"
                    v-if="getClassroomCount() > 0"
                  >
                    <text>教室</text>
                    <text class="tag-count">{{ getClassroomCount() }}</text>
                  </view>
                </view>
              </scroll-view>
            </view>

            <view class="filter-result">
              <text>显示 {{ filteredDeductionsCount }} 项扣分</text>
            </view>
          </view>

          <!-- 扣分明细（按类别分组） -->
          <view class="category-list" v-if="filteredCategoryStats.length">
            <view
              class="category-card"
              v-for="cat in filteredCategoryStats"
              :key="cat.categoryId || cat.categoryName"
            >
              <!-- 类别头部 -->
              <view class="category-header" @click="toggleCategory(cat.categoryId || cat.categoryName)">
                <view class="category-indicator" :style="{ background: getCategoryColor(cat.categoryName) }"></view>
                <view class="category-info">
                  <text class="category-name">{{ cat.categoryName }}</text>
                  <text class="category-count">{{ cat.deductions?.length || 0 }}项扣分</text>
                </view>
                <view class="category-score">
                  <text class="score danger">-{{ formatScore(cat.totalScore) }}</text>
                  <text v-if="activeClass.weightEnabled && cat.weightedTotalScore" class="score primary">/-{{ formatScore(cat.weightedTotalScore) }}</text>
                </view>
                <view class="expand-arrow" :class="{ expanded: expandedCategories.includes(cat.categoryId || cat.categoryName) }">▼</view>
              </view>

              <!-- 类别内筛选Tab -->
              <view
                class="category-filters"
                v-if="expandedCategories.includes(cat.categoryId || cat.categoryName) && (hasMultipleRoundsForCategory(cat) || hasLinkedLocationsForCategory(cat))"
              >
                <!-- 轮次筛选 -->
                <view class="cat-filter-row" v-if="hasMultipleRoundsForCategory(cat)">
                  <text class="cat-filter-label">轮次</text>
                  <view
                    class="cat-filter-tag"
                    :class="{ active: getCategoryRoundTab(cat) === 0 }"
                    @click="setCategoryRoundTab(cat, 0)"
                  >
                    <text>全部</text>
                  </view>
                  <view
                    v-for="roundGroup in getRoundGroupsForCategory(cat)"
                    :key="roundGroup.round"
                    class="cat-filter-tag round"
                    :class="{ active: getCategoryRoundTab(cat) === roundGroup.round }"
                    @click="setCategoryRoundTab(cat, roundGroup.round)"
                  >
                    <text>{{ roundGroup.label }}</text>
                    <text class="mini-count">{{ roundGroup.count }}</text>
                  </view>
                </view>

                <!-- 宿舍/教室筛选 -->
                <view class="cat-filter-row" v-if="hasLinkedLocationsForCategory(cat)">
                  <text class="cat-filter-label">位置</text>
                  <view
                    class="cat-filter-tag"
                    :class="{ active: getCategoryLocationTab(cat) === 'all' }"
                    @click="setCategoryLocationTab(cat, 'all')"
                  >
                    <text>全部</text>
                  </view>
                  <!-- 宿舍分组 -->
                  <view
                    v-for="dorm in getDormitoryGroupsForCategory(cat)"
                    :key="dorm.linkId"
                    class="cat-filter-tag dorm"
                    :class="{ active: getCategoryLocationTab(cat) === dorm.linkId }"
                    @click="setCategoryLocationTab(cat, dorm.linkId)"
                  >
                    <text>{{ dorm.linkName }}</text>
                    <text class="mini-count">{{ dorm.count }}</text>
                  </view>
                  <!-- 教室分组 -->
                  <view
                    v-for="room in getClassroomGroupsForCategory(cat)"
                    :key="room.linkId"
                    class="cat-filter-tag classroom"
                    :class="{ active: getCategoryLocationTab(cat) === room.linkId }"
                    @click="setCategoryLocationTab(cat, room.linkId)"
                  >
                    <text>{{ room.linkName }}</text>
                    <text class="mini-count">{{ room.count }}</text>
                  </view>
                </view>
              </view>

              <!-- 扣分项网格（两列） -->
              <view class="deduction-grid" v-if="expandedCategories.includes(cat.categoryId || cat.categoryName)">
                <view
                  class="deduction-card"
                  v-for="item in getFilteredDeductionsForCategory(cat)"
                  :key="item.id"
                  :class="{ 'has-appeal': item.appealStatus > 0 }"
                >
                  <!-- 扣分项名称和分数 -->
                  <view class="card-top">
                    <text class="item-name">{{ item.deductionItemName || item.itemName }}</text>
                    <text class="item-score">-{{ formatScore(item.actualScore || item.deductScore) }}</text>
                  </view>

                  <!-- 标签行 -->
                  <view class="tag-row" v-if="item.checkRound > 1 || item.linkType || item.appealStatus > 0">
                    <text v-if="item.checkRound > 1" class="item-tag round">R{{ item.checkRound }}</text>
                    <text v-if="item.linkType === 1" class="item-tag dorm">{{ item.linkName || item.linkCode }}</text>
                    <text v-if="item.linkType === 2" class="item-tag classroom">{{ item.linkName || item.linkCode }}</text>
                    <text v-if="item.appealStatus === 1" class="item-tag appeal-pending">申诉中</text>
                    <text v-else-if="item.appealStatus === 2" class="item-tag appeal-approved">通过</text>
                    <text v-else-if="item.appealStatus === 3" class="item-tag appeal-rejected">驳回</text>
                  </view>

                  <!-- 操作按钮 -->
                  <view class="action-row" v-if="item.personCount > 0 || item.remark || (item.photoUrlList?.length || item.photoUrls?.length)">
                    <view class="action-btn" v-if="item.personCount > 0" @click.stop="showStudentList(item)">
                      <text>{{ item.personCount }}人</text>
                    </view>
                    <view class="action-btn" v-if="item.remark" @click.stop="showRemark(item)">
                      <text>备注</text>
                    </view>
                    <view class="action-btn" v-if="(item.photoUrlList?.length || item.photoUrls?.length)" @click.stop="viewPhotos(item)">
                      <text>{{ (item.photoUrlList || item.photoUrls)?.length }}图</text>
                    </view>
                  </view>
                </view>

                <!-- 空状态 -->
                <view v-if="getFilteredDeductionsForCategory(cat).length === 0" class="empty-hint">
                  <text>当前筛选条件下暂无扣分</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 无扣分 -->
          <view class="no-deduction" v-else>
            <text class="no-title">{{ hasAnyDeductions ? '当前筛选条件下无扣分' : '该班级本次检查无扣分' }}</text>
            <text class="no-desc">{{ hasAnyDeductions ? '请调整筛选条件' : '表现优秀！' }}</text>
          </view>
        </template>

        <!-- 未选择班级 -->
        <view v-else class="no-selection">
          <text>请从上方选择班级查看详情</text>
        </view>
      </template>

      <!-- 排名视图 -->
      <template v-else-if="activeView === 'ranking'">
        <view class="ranking-container">
          <!-- 排序方式 -->
          <view class="sort-bar">
            <text class="sort-label">排序:</text>
            <view class="sort-options">
              <view
                class="sort-option"
                :class="{ active: rankingSortBy === 'weighted' }"
                @click="rankingSortBy = 'weighted'"
              >
                <text>加权分数</text>
              </view>
              <view
                class="sort-option"
                :class="{ active: rankingSortBy === 'original' }"
                @click="rankingSortBy = 'original'"
              >
                <text>原始分数</text>
              </view>
            </view>
          </view>

          <!-- 排名列表 -->
          <view class="ranking-list">
            <view
              class="ranking-item"
              v-for="(item, index) in sortedRankingList"
              :key="item.classId"
              :class="{ 'top-1': index === 0, 'top-2': index === 1, 'top-3': index === 2 }"
            >
              <!-- 排名 -->
              <view class="rank-badge" :class="{ gold: index === 0, silver: index === 1, bronze: index === 2 }">
                {{ index + 1 }}
              </view>

              <!-- 班级信息 -->
              <view class="rank-info">
                <view class="rank-avatar">{{ item.className?.charAt(0) || '班' }}</view>
                <view class="rank-detail">
                  <text class="rank-name">{{ item.className }}</text>
                  <text class="rank-meta">{{ item.teacherName || '-' }} · {{ item.classSize || '-' }}人</text>
                </view>
              </view>

              <!-- 分数 -->
              <view class="rank-scores">
                <text class="rank-score danger">-{{ item.totalScore }}</text>
                <template v-if="item.weightEnabled && item.weightedTotalScore">
                  <text class="score-sep">/</text>
                  <text class="rank-score primary">-{{ formatScore(item.weightedTotalScore) }}</text>
                </template>
              </view>
            </view>
          </view>

          <!-- 加权说明 -->
          <view class="weight-hint" v-if="hasWeightEnabled">
            <view class="hint-content">
              <text class="hint-title">加权分数说明</text>
              <text class="hint-text">加权分数根据班级人数与标准人数的比例进行调整，使得不同规模班级的扣分可以公平比较。</text>
            </view>
          </view>
        </view>
      </template>
    </template>

    <!-- 加载失败 -->
    <view v-else class="error-container">
      <text class="error-text">加载失败</text>
      <view class="retry-btn" @click="loadData">
        <text>重新加载</text>
      </view>
    </view>

    <!-- 学生列表弹窗 -->
    <view class="popup-mask" v-if="studentDialogVisible" @click="studentDialogVisible = false">
      <view class="popup-content" @click.stop>
        <view class="popup-header">
          <text class="popup-title">关联学生</text>
          <text class="popup-close" @click="studentDialogVisible = false">×</text>
        </view>
        <view class="popup-body">
          <text class="popup-subtitle">扣分项: {{ currentItem?.deductionItemName || currentItem?.itemName }}</text>
          <view class="student-list">
            <view class="student-item" v-for="(name, idx) in currentStudentList" :key="idx">
              <view class="student-avatar">{{ name.charAt(0) }}</view>
              <text class="student-name">{{ name }}</text>
            </view>
            <view v-if="currentStudentList.length === 0" class="empty-students">
              <text>暂无学生信息</text>
            </view>
          </view>
        </view>
        <view class="popup-footer">
          <view class="popup-btn" @click="studentDialogVisible = false">
            <text>关闭</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 备注弹窗 -->
    <view class="popup-mask" v-if="remarkDialogVisible" @click="remarkDialogVisible = false">
      <view class="popup-content" @click.stop>
        <view class="popup-header">
          <text class="popup-title">扣分备注</text>
          <text class="popup-close" @click="remarkDialogVisible = false">×</text>
        </view>
        <view class="popup-body">
          <text class="popup-subtitle">扣分项: {{ currentItem?.deductionItemName || currentItem?.itemName }}</text>
          <view class="remark-box">
            <text>{{ currentRemark }}</text>
          </view>
        </view>
        <view class="popup-footer">
          <view class="popup-btn" @click="remarkDialogVisible = false">
            <text>关闭</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 照片预览弹窗 -->
    <view class="photo-preview" v-if="photoVisible" @click="photoVisible = false">
      <view class="photo-close">×</view>
      <swiper class="photo-swiper" :current="currentPhotoIndex" @change="onPhotoChange">
        <swiper-item v-for="(url, idx) in currentPhotos" :key="idx">
          <image :src="url" mode="aspectFit" class="preview-image" @click.stop />
        </swiper-item>
      </swiper>
      <view class="photo-indicator">{{ currentPhotoIndex + 1 }} / {{ currentPhotos.length }}</view>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getRecordDetail,
  type CheckRecordDetail,
  type ClassStats,
  type DeductionItem,
  type CategoryStats
} from '@/api/checkRecord'

// 记录ID
const recordId = ref<string>('')

// 数据
const recordDetail = ref<CheckRecordDetail | null>(null)

// 加载状态
const loading = ref(true)

// 视图状态
const activeView = ref<'detail' | 'ranking'>('detail')
const activeClassId = ref<string>('')
const expandedCategories = ref<(string | number)[]>([])
const weightExpanded = ref(false)

// 筛选状态
const activeRoundTab = ref<number>(0)
const activeLocationTab = ref<'all' | 'dormitory' | 'classroom'>('all')

// 每个类别的筛选状态
const categoryRoundTabs = ref<Record<string, number>>({})
const categoryLocationTabs = ref<Record<string, string>>({})

// 排名排序
const rankingSortBy = ref<'weighted' | 'original'>('weighted')

// 弹窗状态
const studentDialogVisible = ref(false)
const remarkDialogVisible = ref(false)
const photoVisible = ref(false)
const currentItem = ref<DeductionItem | null>(null)
const currentStudentList = ref<string[]>([])
const currentRemark = ref('')
const currentPhotos = ref<string[]>([])
const currentPhotoIndex = ref(0)

// 页面加载
onLoad((options) => {
  if (options?.id) {
    recordId.value = options.id
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
    const data = await getRecordDetail(recordId.value)
    console.log('检查记录详情:', data)
    if (data) {
      recordDetail.value = data
      uni.setNavigationBarTitle({ title: data.checkName || '记录详情' })
      // 默认选中第一个班级
      if (data.classStats?.length) {
        activeClassId.value = String(data.classStats[0].classId)
        // 默认展开所有类别
        const firstClass = data.classStats[0]
        const cats = getCategoryStats(firstClass)
        expandedCategories.value = cats.map(c => c.categoryId || c.categoryName)
      }
    }
  } catch (error: any) {
    console.error('加载失败:', error)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 班级列表
const classList = computed(() => recordDetail.value?.classStats || [])

// 当前选中班级
const activeClass = computed(() =>
  classList.value.find(c => String(c.classId) === activeClassId.value)
)

// 选择班级
function selectClass(cls: ClassStats) {
  activeClassId.value = String(cls.classId)
  // 重置筛选
  activeRoundTab.value = 0
  activeLocationTab.value = 'all'
  categoryRoundTabs.value = {}
  categoryLocationTabs.value = {}
  // 默认展开所有类别
  const cats = getCategoryStats(cls)
  expandedCategories.value = cats.map(c => c.categoryId || c.categoryName)
}

// 获取类别统计
function getCategoryStats(cls: ClassStats): CategoryStats[] {
  if (cls.categoryStats?.length) {
    return cls.categoryStats
  }
  // 从deductions分组
  if (cls.deductions?.length) {
    const map = new Map<string, CategoryStats>()
    cls.deductions.forEach(d => {
      const name = d.categoryName || '其他'
      if (!map.has(name)) {
        map.set(name, {
          categoryId: name,
          categoryName: name,
          totalScore: 0,
          deductions: []
        })
      }
      const cat = map.get(name)!
      cat.totalScore += Number(d.actualScore || d.deductScore || 0)
      cat.deductions!.push(d)
    })
    return Array.from(map.values())
  }
  return []
}

// 获取扣分项总数
function getDeductionCount(cls: ClassStats): number {
  const stats = getCategoryStats(cls)
  return stats.reduce((sum, cat) => sum + (cat.deductions?.length || 0), 0)
}

// 所有扣分项
const allDeductions = computed(() => {
  if (!activeClass.value) return []
  return getCategoryStats(activeClass.value).flatMap(cat => cat.deductions || [])
})

// 是否有扣分
const hasAnyDeductions = computed(() => allDeductions.value.length > 0)

// 可用轮次
const availableRounds = computed(() => {
  const rounds = new Set<number>()
  allDeductions.value.forEach(d => rounds.add(d.checkRound || 1))
  return Array.from(rounds).sort((a, b) => a - b)
})

// 是否有筛选器
const hasFilters = computed(() => availableRounds.value.length > 1 || hasLocations.value)

// 是否有位置关联
const hasLocations = computed(() =>
  allDeductions.value.some(d => d.linkType === 1 || d.linkType === 2)
)

// 轮次数量
function getRoundCount(round: number): number {
  return allDeductions.value.filter(d => (d.checkRound || 1) === round).length
}

// 宿舍数量
function getDormitoryCount(): number {
  return allDeductions.value.filter(d => d.linkType === 1).length
}

// 教室数量
function getClassroomCount(): number {
  return allDeductions.value.filter(d => d.linkType === 2).length
}

// 筛选后的扣分项数量
const filteredDeductionsCount = computed(() =>
  filteredCategoryStats.value.reduce((sum, cat) => sum + (cat.deductions?.length || 0), 0)
)

// 筛选后的类别统计
const filteredCategoryStats = computed(() => {
  if (!activeClass.value) return []
  const stats = getCategoryStats(activeClass.value)

  return stats.map(cat => {
    let deductions = cat.deductions || []

    // 位置筛选
    if (activeLocationTab.value === 'dormitory') {
      deductions = deductions.filter(d => d.linkType === 1)
    } else if (activeLocationTab.value === 'classroom') {
      deductions = deductions.filter(d => d.linkType === 2)
    }

    // 轮次筛选
    if (activeRoundTab.value > 0) {
      deductions = deductions.filter(d => (d.checkRound || 1) === activeRoundTab.value)
    }

    return {
      ...cat,
      deductions,
      totalScore: deductions.reduce((sum, d) => sum + Number(d.actualScore || d.deductScore || 0), 0)
    }
  }).filter(cat => cat.deductions.length > 0)
})

// 切换类别展开
function toggleCategory(catKey: string | number) {
  const idx = expandedCategories.value.indexOf(catKey)
  if (idx === -1) {
    expandedCategories.value.push(catKey)
  } else {
    expandedCategories.value.splice(idx, 1)
  }
}

// 类别轮次Tab
function getCategoryRoundTab(cat: CategoryStats): number {
  return categoryRoundTabs.value[String(cat.categoryId || cat.categoryName)] ?? 0
}

function setCategoryRoundTab(cat: CategoryStats, round: number) {
  categoryRoundTabs.value[String(cat.categoryId || cat.categoryName)] = round
}

// 类别位置Tab
function getCategoryLocationTab(cat: CategoryStats): string {
  return categoryLocationTabs.value[String(cat.categoryId || cat.categoryName)] || 'all'
}

function setCategoryLocationTab(cat: CategoryStats, location: string) {
  categoryLocationTabs.value[String(cat.categoryId || cat.categoryName)] = location
}

// 判断类别是否有多轮次
function hasMultipleRoundsForCategory(cat: CategoryStats): boolean {
  return getRoundGroupsForCategory(cat).length > 1
}

// 判断类别是否有位置关联
function hasLinkedLocationsForCategory(cat: CategoryStats): boolean {
  const deductions = cat.deductions || []
  return deductions.some(d => d.linkType === 1 || d.linkType === 2)
}

// 获取类别的轮次分组
function getRoundGroupsForCategory(cat: CategoryStats): { round: number; label: string; count: number }[] {
  const deductions = cat.deductions || []
  const map = new Map<number, number>()
  deductions.forEach(d => {
    const round = d.checkRound || 1
    map.set(round, (map.get(round) || 0) + 1)
  })
  return Array.from(map.entries())
    .map(([round, count]) => ({ round, label: `第${round}轮`, count }))
    .sort((a, b) => a.round - b.round)
}

// 获取类别的宿舍分组
function getDormitoryGroupsForCategory(cat: CategoryStats): { linkId: string; linkName: string; count: number }[] {
  const deductions = cat.deductions || []
  const selectedRound = getCategoryRoundTab(cat)

  let filtered = deductions.filter(d => d.linkType === 1 && d.linkId)
  if (selectedRound !== 0) {
    filtered = filtered.filter(d => (d.checkRound || 1) === selectedRound)
  }

  const map = new Map<string, { linkId: string; linkName: string; count: number }>()
  filtered.forEach(d => {
    const id = String(d.linkId)
    if (!map.has(id)) {
      map.set(id, { linkId: id, linkName: d.linkName || d.linkCode || `宿舍${id}`, count: 0 })
    }
    map.get(id)!.count++
  })

  return Array.from(map.values()).sort((a, b) => a.linkName.localeCompare(b.linkName))
}

// 获取类别的教室分组
function getClassroomGroupsForCategory(cat: CategoryStats): { linkId: string; linkName: string; count: number }[] {
  const deductions = cat.deductions || []
  const selectedRound = getCategoryRoundTab(cat)

  let filtered = deductions.filter(d => d.linkType === 2 && d.linkId)
  if (selectedRound !== 0) {
    filtered = filtered.filter(d => (d.checkRound || 1) === selectedRound)
  }

  const map = new Map<string, { linkId: string; linkName: string; count: number }>()
  filtered.forEach(d => {
    const id = String(d.linkId)
    if (!map.has(id)) {
      map.set(id, { linkId: id, linkName: d.linkName || d.linkCode || `教室${id}`, count: 0 })
    }
    map.get(id)!.count++
  })

  return Array.from(map.values()).sort((a, b) => a.linkName.localeCompare(b.linkName))
}

// 获取类别筛选后的扣分项
function getFilteredDeductionsForCategory(cat: CategoryStats): DeductionItem[] {
  let deductions = cat.deductions || []
  const catKey = String(cat.categoryId || cat.categoryName)

  // 位置筛选
  const location = categoryLocationTabs.value[catKey] || 'all'
  if (location !== 'all') {
    deductions = deductions.filter(d => String(d.linkId) === location)
  }

  // 轮次筛选
  const round = categoryRoundTabs.value[catKey] ?? 0
  if (round !== 0) {
    deductions = deductions.filter(d => (d.checkRound || 1) === round)
  }

  return deductions
}

// 排名列表（排序后）
const sortedRankingList = computed(() => {
  const list = [...classList.value]
  if (rankingSortBy.value === 'weighted') {
    return list.sort((a, b) => {
      const scoreA = a.weightedTotalScore ?? a.totalScore
      const scoreB = b.weightedTotalScore ?? b.totalScore
      return scoreA - scoreB
    })
  }
  return list.sort((a, b) => a.totalScore - b.totalScore)
})

// 是否有班级启用加权
const hasWeightEnabled = computed(() => classList.value.some(c => c.weightEnabled))

// 格式化方法
function formatDate(d: string): string {
  if (!d) return '-'
  return d.substring(0, 10)
}

function formatScore(s: number | null | undefined): string {
  if (s === null || s === undefined) return '0'
  const n = Number(s)
  return n % 1 === 0 ? String(n) : n.toFixed(1)
}

function formatWeightFactor(f: number | null | undefined): string {
  if (!f) return '1.00'
  return Number(f).toFixed(2)
}

function getTotalScore(): number {
  return recordDetail.value?.totalDeductionScore || recordDetail.value?.totalScore || 0
}

// 计算加权分数
function calculateWeightedScore(item: DeductionItem, categoryName?: string): number {
  const score = item.actualScore || item.deductScore || 0
  if (!activeClass.value?.weightEnabled) return score

  // 多配置模式
  if (activeClass.value.multiConfigEnabled && activeClass.value.multiWeightConfigs?.length && categoryName) {
    const config = activeClass.value.multiWeightConfigs.find(c =>
      c.applyCategoryNames?.includes(categoryName)
    ) || activeClass.value.multiWeightConfigs.find(c => c.isDefault)

    if (config?.weightFactor) {
      return score * config.weightFactor
    }
  }

  // 单配置模式
  return score * (activeClass.value.weightFactor || 1)
}

// 类别颜色
const categoryColors: Record<string, string> = {
  '卫生': '#10B981',
  '纪律': '#F59E0B',
  '考勤': '#3B82F6',
  '宿舍': '#8B5CF6',
  '行为': '#EC4899',
  '其他': '#6B7280'
}

function getCategoryColor(name: string): string {
  const lower = name?.toLowerCase() || ''
  for (const [key, color] of Object.entries(categoryColors)) {
    if (lower.includes(key)) return color
  }
  return '#4F46E5'
}

// 弹窗操作
function showStudentList(item: DeductionItem) {
  currentItem.value = item
  currentStudentList.value = item.studentNameList || item.studentNames || []
  studentDialogVisible.value = true
}

function showRemark(item: DeductionItem) {
  currentItem.value = item
  currentRemark.value = item.remark || ''
  remarkDialogVisible.value = true
}

function viewPhotos(item: DeductionItem) {
  currentPhotos.value = item.photoUrlList || item.photoUrls || []
  currentPhotoIndex.value = 0
  photoVisible.value = true
}

function onPhotoChange(e: any) {
  currentPhotoIndex.value = e.detail.current
}
</script>

<style lang="scss" scoped>
// 变量
$primary: #4F46E5;
$primary-light: #EEF2FF;
$danger: #DC2626;
$danger-light: #FEE2E2;
$success: #059669;
$success-light: #D1FAE5;
$warning: #F59E0B;
$warning-light: #FEF3C7;
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border: #E5E7EB;
$bg: #F3F4F6;
$white: #FFFFFF;

.page {
  min-height: 100vh;
  background: $bg;
}

.safe-bottom {
  height: calc(20rpx + env(safe-area-inset-bottom));
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
  border: 4rpx solid $border;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.loading-text {
  margin-top: 20rpx;
  color: $text-muted;
  font-size: 28rpx;
}

// 错误状态
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 40rpx;
}

.error-text { color: $text-muted; font-size: 28rpx; margin-bottom: 30rpx; }

.retry-btn {
  padding: 16rpx 48rpx;
  background: $primary;
  border-radius: 8rpx;
  text { color: $white; font-size: 28rpx; }
}

// 顶部信息栏
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;
  background: linear-gradient(135deg, $primary 0%, #7C3AED 100%);
}

.header-info {
  flex: 1;
  .header-title {
    display: block;
    font-size: 32rpx;
    font-weight: 600;
    color: $white;
    margin-bottom: 8rpx;
  }
  .header-meta {
    font-size: 24rpx;
    color: rgba(255,255,255,0.8);
  }
}

.header-stats {
  display: flex;
  gap: 24rpx;
}

.stat-box {
  text-align: center;
  .stat-num {
    display: block;
    font-size: 36rpx;
    font-weight: 700;
    color: $white;
    &.danger { color: #FCA5A5; }
  }
  .stat-label {
    font-size: 20rpx;
    color: rgba(255,255,255,0.7);
  }
}

// 视图Tab
.view-tabs {
  display: flex;
  margin: 24rpx;
  background: $white;
  border-radius: 12rpx;
  padding: 8rpx;
  border: 1rpx solid $border;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx;
  border-radius: 8rpx;
  text { font-size: 26rpx; color: $text-secondary; }
  &.active {
    background: $primary;
    text { color: $white; font-weight: 500; }
  }
}

// 班级选择器
.class-selector {
  white-space: nowrap;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
}

.class-list-h {
  display: inline-flex;
  gap: 16rpx;
}

.class-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 24rpx;
  background: $white;
  border-radius: 12rpx;
  border: 2rpx solid $border;
  min-width: 120rpx;

  &.active {
    background: $primary;
    border-color: $primary;
    .chip-name { color: $white; }
    .chip-score text { color: rgba(255,255,255,0.9); }
    .chip-perfect { color: rgba(255,255,255,0.9); }
  }
}

.chip-name {
  font-size: 26rpx;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 4rpx;
}

.chip-score {
  text {
    font-size: 22rpx;
    font-weight: 600;
    color: $danger;
  }
  .weighted { color: $primary; }
}

.chip-perfect {
  font-size: 22rpx;
  color: $success;
}

// 班级信息卡片
.class-info-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 24rpx 24rpx;
  padding: 24rpx;
  background: $white;
  border-radius: 16rpx;
  border: 1rpx solid $border;
}

.info-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.class-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, $primary 0%, #7C3AED 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: $white;
  font-size: 32rpx;
  font-weight: 700;
}

.class-detail {
  .class-name {
    display: block;
    font-size: 32rpx;
    font-weight: 600;
    color: $text-primary;
  }
  .class-meta {
    font-size: 24rpx;
    color: $text-muted;
  }
}

.info-right {
  display: flex;
  gap: 24rpx;
}

.score-block {
  text-align: center;
  .score-value {
    display: block;
    font-size: 40rpx;
    font-weight: 700;
    color: $text-primary;
    &.danger { color: $danger; }
    &.primary { color: $primary; }
  }
  .score-label {
    font-size: 20rpx;
    color: $text-muted;
  }
}

// 加权配置卡片
.weight-card {
  margin: 0 24rpx 24rpx;
  background: $white;
  border-radius: 16rpx;
  border: 1rpx solid $border;
  overflow: hidden;
}

.weight-header {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  gap: 16rpx;
}

.weight-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, $primary 0%, #7C3AED 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
}

.weight-info {
  flex: 1;
  .weight-title {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: $text-primary;
  }
  .weight-desc {
    font-size: 22rpx;
    color: $text-muted;
  }
}

.weight-result {
  display: flex;
  align-items: center;
  gap: 8rpx;
  .original { font-size: 26rpx; color: $danger; font-weight: 600; }
  .arrow { font-size: 20rpx; color: $text-muted; }
  .weighted { font-size: 26rpx; color: $primary; font-weight: 600; }
}

.expand-icon {
  font-size: 20rpx;
  color: $text-muted;
  transition: transform 0.2s;
  &.expanded { transform: rotate(180deg); }
}

.weight-detail {
  padding: 20rpx 24rpx;
  background: #F9FAFB;
  border-top: 1rpx solid $border;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
  .detail-label { font-size: 24rpx; color: $text-muted; }
  .detail-value { font-size: 24rpx; color: $text-primary; &.primary { color: $primary; font-weight: 600; } }
}

.multi-config {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed $border;
}

.config-title {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 12rpx;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 0;
}

.config-color {
  width: 12rpx;
  height: 40rpx;
  border-radius: 4rpx;
}

.config-name {
  font-size: 24rpx;
  font-weight: 500;
  color: $text-primary;
}

.config-categories {
  flex: 1;
  font-size: 22rpx;
  color: $text-muted;
}

.config-factor {
  font-size: 24rpx;
  font-weight: 600;
  color: $primary;
}

// 筛选区域
.filter-section {
  margin: 0 24rpx 24rpx;
  padding: 20rpx;
  background: $white;
  border-radius: 16rpx;
  border: 1rpx solid $border;
}

.filter-group {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
  &:last-of-type { margin-bottom: 0; }
}

.filter-label {
  font-size: 22rpx;
  color: $text-muted;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.filter-scroll {
  flex: 1;
  white-space: nowrap;
}

.filter-tags {
  display: inline-flex;
  gap: 12rpx;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 16rpx;
  background: #F3F4F6;
  border-radius: 8rpx;
  text { font-size: 22rpx; color: $text-secondary; }
  .tag-count {
    font-size: 20rpx;
    padding: 2rpx 8rpx;
    background: rgba(0,0,0,0.1);
    border-radius: 4rpx;
  }
  &.active {
    background: $primary;
    text { color: $white; }
    .tag-count { background: rgba(255,255,255,0.2); }
  }
  &.round.active { background: #6366F1; }
  &.dorm.active { background: #0EA5E9; }
  &.classroom.active { background: #10B981; }
}

.filter-result {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $border;
  text { font-size: 22rpx; color: $text-muted; }
}

// 类别卡片
.category-list {
  padding: 0 24rpx;
}

.category-card {
  background: $white;
  border-radius: 16rpx;
  border: 1rpx solid $border;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.category-header {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  gap: 16rpx;
}

.category-indicator {
  width: 8rpx;
  height: 40rpx;
  border-radius: 4rpx;
}

.category-info {
  flex: 1;
  .category-name {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: $text-primary;
  }
  .category-count {
    font-size: 22rpx;
    color: $text-muted;
  }
}

.category-score {
  .score {
    font-size: 28rpx;
    font-weight: 700;
    &.danger { color: $danger; }
    &.primary { color: $primary; }
  }
}

.expand-arrow {
  font-size: 20rpx;
  color: $text-muted;
  transition: transform 0.2s;
  &.expanded { transform: rotate(180deg); }
}

// 类别内筛选
.category-filters {
  padding: 16rpx 24rpx;
  background: #F9FAFB;
  border-top: 1rpx solid $border;
}

.cat-filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 12rpx;
  &:last-child { margin-bottom: 0; }
}

.cat-filter-label {
  font-size: 22rpx;
  color: $text-muted;
}

.cat-filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  padding: 8rpx 14rpx;
  background: $white;
  border: 1rpx solid $border;
  border-radius: 6rpx;
  text { font-size: 20rpx; color: $text-secondary; }
  .mini-count {
    font-size: 18rpx;
    padding: 1rpx 6rpx;
    background: #F3F4F6;
    border-radius: 4rpx;
    color: $text-muted;
  }
  &.active {
    background: #374151;
    border-color: #374151;
    text { color: $white; }
    .mini-count { background: rgba(255,255,255,0.2); }
  }
  &.round.active { background: #6366F1; border-color: #6366F1; }
  &.dorm.active { background: #0EA5E9; border-color: #0EA5E9; }
  &.classroom.active { background: #10B981; border-color: #10B981; }
}

// 扣分项网格（两列紧凑卡片）
.deduction-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 16rpx 20rpx;
  gap: 12rpx;
  border-top: 1rpx solid $border;
}

.deduction-card {
  width: calc(50% - 6rpx);
  background: #FAFAFA;
  border: 1rpx solid $border;
  border-radius: 10rpx;
  padding: 12rpx;
  box-sizing: border-box;

  &.has-appeal {
    border-left: 3rpx solid $warning;
  }
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.item-name {
  flex: 1;
  font-size: 24rpx;
  font-weight: 500;
  color: $text-primary;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-score {
  font-size: 26rpx;
  font-weight: 700;
  color: $danger;
  flex-shrink: 0;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6rpx;
  margin-bottom: 8rpx;
}

.item-tag {
  padding: 2rpx 8rpx;
  border-radius: 4rpx;
  font-size: 18rpx;

  &.round { background: #EEF2FF; color: #6366F1; }
  &.dorm { background: #E0F2FE; color: #0284C7; }
  &.classroom { background: #D1FAE5; color: #059669; }
  &.appeal-pending { background: #FEF3C7; color: #D97706; }
  &.appeal-approved { background: #D1FAE5; color: #059669; }
  &.appeal-rejected { background: #FEE2E2; color: #DC2626; }
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  padding-top: 8rpx;
  border-top: 1rpx dashed $border;
}

.action-btn {
  padding: 4rpx 10rpx;
  background: $white;
  border: 1rpx solid $border;
  border-radius: 4rpx;
  text { font-size: 18rpx; color: $text-secondary; }
}

.empty-hint {
  width: 100%;
  padding: 40rpx;
  text-align: center;
  text { font-size: 24rpx; color: $text-muted; }
}

// 无扣分状态
.no-deduction {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 40rpx;

  .no-title { font-size: 30rpx; font-weight: 500; color: $text-primary; margin-bottom: 8rpx; }
  .no-desc { font-size: 24rpx; color: $text-muted; }
}

.no-selection {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 150rpx 40rpx;
  text { font-size: 26rpx; color: $text-muted; }
}

// 排名视图
.ranking-container {
  padding: 24rpx;
}

.sort-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.sort-label {
  font-size: 24rpx;
  color: $text-muted;
}

.sort-options {
  display: flex;
  background: $white;
  border-radius: 8rpx;
  padding: 6rpx;
  border: 1rpx solid $border;
}

.sort-option {
  padding: 12rpx 20rpx;
  border-radius: 6rpx;
  text { font-size: 24rpx; color: $text-secondary; }
  &.active {
    background: $primary;
    text { color: $white; }
  }
}

.ranking-list {
  background: $white;
  border-radius: 16rpx;
  border: 1rpx solid $border;
  overflow: hidden;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid $border;
  gap: 16rpx;

  &:last-child { border-bottom: none; }
  &.top-1 { background: linear-gradient(90deg, #FFFBEB 0%, transparent 100%); }
  &.top-2 { background: linear-gradient(90deg, #F1F5F9 0%, transparent 100%); }
  &.top-3 { background: linear-gradient(90deg, #FEF3C7 0%, transparent 100%); }
}

.rank-badge {
  width: 56rpx;
  height: 56rpx;
  border-radius: 12rpx;
  background: #F3F4F6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 700;
  color: $text-secondary;

  &.gold { background: linear-gradient(135deg, #FCD34D 0%, #F59E0B 100%); color: $white; }
  &.silver { background: linear-gradient(135deg, #E5E7EB 0%, #9CA3AF 100%); color: $white; }
  &.bronze { background: linear-gradient(135deg, #FDBA74 0%, #EA580C 100%); color: $white; }
}

.rank-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
}

.rank-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, $primary 0%, #7C3AED 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: $white;
  font-size: 26rpx;
  font-weight: 600;
}

.rank-detail {
  .rank-name {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: $text-primary;
  }
  .rank-meta {
    font-size: 22rpx;
    color: $text-muted;
  }
}

.rank-scores {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.rank-score {
  font-size: 28rpx;
  font-weight: 700;
  &.danger { color: $danger; }
  &.primary { color: $primary; }
}

.score-sep {
  font-size: 24rpx;
  color: $text-muted;
}

.weight-hint {
  margin-top: 24rpx;
  padding: 20rpx;
  background: linear-gradient(135deg, $primary-light 0%, #EDE9FE 100%);
  border-radius: 16rpx;
  border: 1rpx solid rgba(79, 70, 229, 0.2);
}

.hint-content {
  .hint-title {
    display: block;
    font-size: 26rpx;
    font-weight: 600;
    color: $primary;
    margin-bottom: 8rpx;
  }
  .hint-text {
    font-size: 24rpx;
    color: #6366F1;
    line-height: 1.5;
  }
}

// 弹窗
.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 40rpx;
}

.popup-content {
  width: 100%;
  max-width: 600rpx;
  background: $white;
  border-radius: 24rpx;
  overflow: hidden;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  background: $primary;

  .popup-title { font-size: 30rpx; font-weight: 600; color: $white; }
  .popup-close { font-size: 48rpx; color: rgba(255,255,255,0.8); line-height: 1; }
}

.popup-body {
  padding: 24rpx;
}

.popup-subtitle {
  display: block;
  font-size: 24rpx;
  color: $text-muted;
  margin-bottom: 20rpx;
}

.student-list {
  max-height: 400rpx;
  overflow-y: auto;
}

.student-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  background: #F9FAFB;
  border-radius: 12rpx;
  margin-bottom: 12rpx;
}

.student-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #9333EA 0%, $primary 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: $white;
  font-size: 24rpx;
  font-weight: 500;
}

.student-name {
  font-size: 28rpx;
  color: $text-primary;
}

.empty-students {
  padding: 60rpx;
  text-align: center;
  text { font-size: 26rpx; color: $text-muted; }
}

.remark-box {
  padding: 20rpx;
  background: #FEF3C7;
  border-radius: 12rpx;
  border: 1rpx solid #FDE68A;
  text {
    font-size: 26rpx;
    color: $text-primary;
    line-height: 1.6;
    white-space: pre-wrap;
  }
}

.popup-footer {
  padding: 20rpx 24rpx;
  background: #F9FAFB;
  border-top: 1rpx solid $border;
}

.popup-btn {
  padding: 20rpx;
  background: #E5E7EB;
  border-radius: 12rpx;
  text-align: center;
  text { font-size: 28rpx; color: $text-primary; font-weight: 500; }
}

// 照片预览
.photo-preview {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.95);
  z-index: 2000;
  display: flex;
  flex-direction: column;
}

.photo-close {
  position: absolute;
  top: calc(40rpx + env(safe-area-inset-top));
  right: 40rpx;
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: $white;
  font-size: 40rpx;
  z-index: 10;
}

.photo-swiper {
  flex: 1;
  width: 100%;
}

.preview-image {
  width: 100%;
  height: 100%;
}

.photo-indicator {
  position: absolute;
  bottom: calc(60rpx + env(safe-area-inset-bottom));
  left: 50%;
  transform: translateX(-50%);
  padding: 12rpx 24rpx;
  background: rgba(0,0,0,0.6);
  border-radius: 20rpx;
  color: $white;
  font-size: 24rpx;
}
</style>
