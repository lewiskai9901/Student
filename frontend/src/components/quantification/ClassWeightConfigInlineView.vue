<template>
  <div v-loading="loading" class="class-weight-inline-container">
    <!-- 顶部标题区 -->
    <div class="header-section">
      <div class="header-main">
        <div class="header-icon">
          <Scale class="w-5 h-5" />
        </div>
        <div class="header-text">
          <h2>{{ classInfo?.className }} - 加权配置</h2>
          <p>根据班级人数 {{ classInfo?.classSize || '-' }} 人计算的加权扣分</p>
        </div>
      </div>
      <div class="header-badge" :class="{ 'enabled': treeData.weightEnabled }">
        <span class="badge-dot"></span>
        <span>{{ treeData.weightEnabled ? '已启用' : '未启用' }}</span>
      </div>
    </div>

    <!-- 核心公式卡片（显示该班级的具体计算） -->
    <div class="formula-card" v-if="treeData.weightEnabled && treeData.globalConfig">
      <div class="formula-inner">
        <div class="formula-label">加权公式</div>
        <div class="formula-content">
          <span class="formula-text">加权扣分</span>
          <span class="formula-eq">=</span>
          <span class="formula-text">原始扣分</span>
          <span class="formula-eq">×</span>
          <div class="formula-fraction">
            <span class="fraction-top">{{ classInfo?.classSize || '-' }}人 <span class="fraction-note">(班级)</span></span>
            <span class="fraction-line"></span>
            <span class="fraction-bottom">{{ treeData.globalConfig.standardSize || 50 }}人 <span class="fraction-note">(标准)</span></span>
          </div>
          <span class="formula-eq">=</span>
          <span class="formula-text">原始扣分</span>
          <span class="formula-eq">×</span>
          <span class="formula-result">{{ formatWeight(classInfo?.weightFactor) }}</span>
        </div>
      </div>
      <!-- 加权汇总 -->
      <div class="weight-summary">
        <div class="summary-item">
          <span class="summary-label">原始扣分</span>
          <span class="summary-value original">-{{ formatScore(classInfo?.totalScore) }}</span>
        </div>
        <div class="summary-arrow">
          <ArrowRight class="w-4 h-4" />
        </div>
        <div class="summary-item">
          <span class="summary-label">加权扣分</span>
          <span class="summary-value weighted">-{{ formatScore(classInfo?.weightedTotalScore) }}</span>
        </div>
      </div>
    </div>

    <!-- 加权方案列表 -->
    <div class="schemes-section" v-if="treeData.weightEnabled && uniqueWeightSchemes.length > 0">
      <div class="section-header">
        <div class="section-icon">
          <Layers class="w-4 h-4" />
        </div>
        <h3>加权方案</h3>
        <span class="scheme-count">{{ uniqueWeightSchemes.length }} 个方案</span>
      </div>
      <div class="schemes-list">
        <div
          v-for="(scheme, idx) in uniqueWeightSchemes"
          :key="scheme.configId || idx"
          class="scheme-card"
          :style="{ borderLeftColor: getSchemeColor(scheme, idx) }"
        >
          <div class="scheme-header">
            <div class="scheme-color-dot" :style="{ background: getSchemeColor(scheme, idx) }"></div>
            <span class="scheme-name">{{ scheme.configName || '默认配置' }}</span>
            <el-tag v-if="scheme.isDefault" size="small" type="warning">默认</el-tag>
            <span class="scheme-tag">{{ scheme.weightModeDesc || '标准模式' }}</span>
          </div>
          <div class="scheme-details">
            <div class="scheme-detail-item">
              <span class="detail-label">标准人数</span>
              <span class="detail-value">{{ scheme.standardSize || '-' }}人</span>
              <span class="detail-sub" v-if="scheme.standardSizeModeDesc">({{ scheme.standardSizeModeDesc }})</span>
            </div>
            <div class="scheme-detail-item">
              <span class="detail-label">本班系数</span>
              <span class="detail-value highlight">x{{ calculateWeightFactor(scheme, classInfo?.classSize) }}</span>
            </div>
            <div class="scheme-detail-item" v-if="scheme.appliedCategories?.length">
              <span class="detail-label">应用类别</span>
              <span class="detail-value categories">{{ scheme.appliedCategories.join('、') }}</span>
            </div>
            <div class="scheme-detail-item" v-else-if="scheme.isDefault">
              <span class="detail-label">应用范围</span>
              <span class="detail-value categories">其他未指定分类</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 检查类别与扣分项 -->
    <div class="categories-deductions-section" v-if="treeData.categories?.length">
      <div class="section-header">
        <div class="section-icon">
          <GitBranch class="w-4 h-4" />
        </div>
        <h3>扣分项加权详情</h3>
      </div>

      <div class="categories-wrapper">
        <div
          v-for="(category, catIdx) in treeData.categories"
          :key="category.categoryId"
          class="category-block"
        >
          <!-- 类别头部 -->
          <div
            class="category-header"
            :style="{ borderLeftColor: getCategorySchemeColor(category) }"
            @click="toggleCategory(category.categoryId)"
          >
            <div class="category-left">
              <div class="category-icon" :style="{ background: getCategoryGradient(catIdx) }">
                <component :is="getCategoryIconComponent(category.categoryName)" />
              </div>
              <div class="category-info">
                <div class="category-name-row">
                  <span class="category-name">{{ category.categoryName }}</span>
                  <span
                    class="scheme-indicator"
                    :style="{ background: getCategorySchemeColor(category), color: '#fff' }"
                  >
                    {{ getCategorySchemeShortName(category) }}
                  </span>
                </div>
                <span class="category-meta">
                  {{ category.items?.length || 0 }}项扣分
                  <template v-if="getCategoryOriginalScore(category) > 0">
                    · 原始 {{ formatScore(getCategoryOriginalScore(category)) }}
                    → 加权 {{ formatScore(getCategoryWeightedScore(category)) }}
                  </template>
                </span>
              </div>
            </div>
            <div class="category-toggle" v-if="category.items?.length">
              <ChevronDown
                class="toggle-icon"
                :class="{ 'rotated': expandedCategories.includes(category.categoryId) }"
              />
            </div>
          </div>

          <!-- 扣分项列表 -->
          <transition name="expand">
            <div
              v-if="category.items?.length && expandedCategories.includes(category.categoryId)"
              class="items-grid"
            >
              <div
                v-for="item in category.items"
                :key="item.itemId"
                class="item-card"
                :style="{ borderTopColor: getItemSchemeColor(category, item) }"
              >
                <div class="item-header">
                  <span class="item-name">{{ item.itemName }}</span>
                  <span
                    class="item-scheme-dot"
                    :style="{ background: getItemSchemeColor(category, item) }"
                    :title="getItemSchemeTooltip(category, item)"
                  ></span>
                </div>
                <div class="item-body">
                  <div class="item-mode">{{ item.deductModeDesc || '固定扣分' }}</div>
                  <div class="item-scores">
                    <span class="score-base">基准: {{ formatScore(item.baseScore) }}分</span>
                  </div>
                  <!-- 使用班级加权系数计算的分数 -->
                  <div class="item-weighted" v-if="classInfo?.weightFactor">
                    <span class="weighted-label">加权后:</span>
                    <span class="weighted-value">{{ formatScore(calculateWeightedScore(item.baseScore)) }}分</span>
                  </div>
                </div>
                <div class="item-footer" v-if="item.deductionCount > 0">
                  <span class="deduction-badge">
                    扣分 {{ item.deductionCount }}次
                    <template v-if="item.originalScore">
                      · {{ formatScore(item.originalScore) }}
                      → {{ formatScore(calculateWeightedScore(item.originalScore)) }}
                    </template>
                  </span>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && !treeData.categories?.length" class="empty-state">
      <GitBranch class="w-12 h-12 text-slate-300" />
      <span>暂无类别配置</span>
    </div>

    <!-- 底部说明 -->
    <div class="footer-note" v-if="treeData.weightEnabled">
      <div class="note-icon">
        <Info class="w-4 h-4" />
      </div>
      <div class="note-text">
        本页显示的加权分数均根据 <strong>{{ classInfo?.className }}</strong> 的班级人数
        <strong>{{ classInfo?.classSize || '-' }}</strong> 人计算，加权系数为
        <strong>{{ formatWeight(classInfo?.weightFactor) }}</strong>。
        不同班级因人数不同，加权后的扣分也会不同。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed, markRaw, type Component } from 'vue'
import { getWeightConfigTree } from '@/api/checkRecord'
import type { ClassStats } from '@/api/checkRecord'
import { ElMessage } from 'element-plus'
import {
  Scale, ArrowRight, Layers, GitBranch, ChevronDown, Info,
  Sparkles, ClipboardCheck, BookOpen, Moon, AlertCircle
} from 'lucide-vue-next'

// TypeScript 接口定义
interface WeightConfigNode {
  configId?: string
  configName?: string
  configCode?: string
  weightMode?: string
  weightModeDesc?: string
  standardSizeMode?: string
  standardSizeModeDesc?: string
  standardSize?: number
  minWeight?: number
  maxWeight?: number
  enableWeightLimit?: boolean
  formulaDescription?: string
  appliedCategories?: string[]
}

interface ItemWeightNode {
  itemId: string
  itemName: string
  itemCode: string
  weightEnabled: boolean
  inherited: boolean
  inheritSource: string
  effectiveConfig?: WeightConfigNode
  baseScore: number
  deductMode: string
  deductModeDesc: string
  deductionCount?: number
  originalScore?: number
  weightedScore?: number
}

interface CategoryWeightNode {
  categoryId: string
  categoryName: string
  categoryCode: string
  categoryType: string
  weightEnabled: boolean
  inherited: boolean
  inheritSource: string
  effectiveConfig?: WeightConfigNode
  items?: ItemWeightNode[]
}

interface MultiWeightScheme {
  id?: string
  weightConfigId?: string
  configName?: string
  colorCode?: string
  colorName?: string
  applyScope?: string
  isDefault?: boolean
  priority?: number
  applyCategoryIds?: string[]
  applyCategoryNames?: string[]
  weightMode?: string
  weightModeDesc?: string
  standardSizeMode?: string
  standardSizeModeDesc?: string
  calculatedStandardSize?: number
  minWeight?: number
  maxWeight?: number
  formulaDescription?: string
}

interface WeightConfigTreeData {
  recordId?: string
  checkName?: string
  weightEnabled?: boolean
  globalConfig?: WeightConfigNode
  categories?: CategoryWeightNode[]
  multiConfigEnabled?: boolean
  multiWeightSchemes?: MultiWeightScheme[]
}

const props = defineProps<{
  recordId: string
  classInfo: ClassStats | null | undefined
}>()

const loading = ref(false)
const treeData = ref<WeightConfigTreeData>({})
const expandedCategories = ref<string[]>([])

// 加权方案颜色
const schemeColors = [
  '#667eea',  // 紫蓝
  '#f5576c',  // 玫红
  '#43e97b',  // 翠绿
  '#4facfe',  // 天蓝
  '#fa709a',  // 粉红
  '#a18cd1',  // 淡紫
  '#ffc107',  // 琥珀
  '#17a2b8',  // 青色
]

// 计算唯一的加权方案列表
const uniqueWeightSchemes = computed(() => {
  // 优先使用后端返回的multiWeightSchemes数据
  if (treeData.value.multiWeightSchemes && treeData.value.multiWeightSchemes.length > 0) {
    return treeData.value.multiWeightSchemes.map(scheme => ({
      configId: scheme.weightConfigId,
      configName: scheme.configName,
      colorCode: scheme.colorCode,
      colorName: scheme.colorName,
      weightMode: scheme.weightMode,
      weightModeDesc: scheme.weightModeDesc,
      standardSizeMode: scheme.standardSizeMode,
      standardSizeModeDesc: scheme.standardSizeModeDesc,
      standardSize: scheme.calculatedStandardSize,
      minWeight: scheme.minWeight,
      maxWeight: scheme.maxWeight,
      formulaDescription: scheme.formulaDescription,
      appliedCategories: scheme.applyCategoryNames || [],
      isDefault: scheme.isDefault,
      applyScope: scheme.applyScope
    }))
  }

  // 回退到从categories中提取配置的旧逻辑
  const schemeMap = new Map<string, WeightConfigNode & { appliedCategories: string[] }>()

  // 添加全局配置
  if (treeData.value.globalConfig) {
    const globalConfig = treeData.value.globalConfig
    const key = globalConfig.configId || 'global'
    schemeMap.set(key, {
      ...globalConfig,
      appliedCategories: []
    })
  }

  // 遍历类别收集配置
  treeData.value.categories?.forEach(category => {
    const config = category.effectiveConfig
    if (config) {
      const key = config.configId || config.configName || 'default'
      if (!schemeMap.has(key)) {
        schemeMap.set(key, {
          ...config,
          appliedCategories: [category.categoryName]
        })
      } else {
        const existing = schemeMap.get(key)!
        if (!existing.appliedCategories.includes(category.categoryName)) {
          existing.appliedCategories.push(category.categoryName)
        }
      }
    }

    // 遍历扣分项收集配置
    category.items?.forEach(item => {
      if (item.effectiveConfig && !item.inherited) {
        const itemConfig = item.effectiveConfig
        const key = itemConfig.configId || itemConfig.configName || 'item-default'
        if (!schemeMap.has(key)) {
          schemeMap.set(key, {
            ...itemConfig,
            appliedCategories: [`${category.categoryName} - ${item.itemName}`]
          })
        }
      }
    })
  })

  return Array.from(schemeMap.values())
})

// 获取方案颜色索引
const getSchemeColorIndex = (config?: WeightConfigNode): number => {
  if (!config) return 0
  const key = config.configId || config.configName || 'default'
  const index = uniqueWeightSchemes.value.findIndex(s =>
    (s.configId || s.configName || 'default') === key
  )
  return index >= 0 ? index : 0
}

// 获取方案的颜色（优先使用后端返回的颜色）
const getSchemeColor = (scheme: any, idx: number): string => {
  // 如果有后端返回的颜色代码，优先使用
  if (scheme.colorCode) {
    return scheme.colorCode
  }
  // 否则使用默认颜色数组
  return schemeColors[idx % schemeColors.length]
}

// 计算某个配置方案对于特定班级人数的加权系数
const calculateWeightFactor = (scheme: any, classSize?: number | null): string => {
  if (!classSize || !scheme.standardSize) {
    return '1.00'
  }
  const factor = classSize / scheme.standardSize
  // 应用权重限制
  let finalFactor = factor
  if (scheme.minWeight && factor < scheme.minWeight) {
    finalFactor = scheme.minWeight
  }
  if (scheme.maxWeight && factor > scheme.maxWeight) {
    finalFactor = scheme.maxWeight
  }
  return finalFactor.toFixed(2)
}

// 获取类别使用的方案颜色
const getCategorySchemeColor = (category: CategoryWeightNode): string => {
  const idx = getSchemeColorIndex(category.effectiveConfig)
  return schemeColors[idx % schemeColors.length]
}

// 获取类别方案简称
const getCategorySchemeShortName = (category: CategoryWeightNode): string => {
  if (!category.effectiveConfig) return '默认'
  const name = category.effectiveConfig.configName || '默认配置'
  return name.length > 6 ? name.substring(0, 6) + '..' : name
}

// 获取扣分项使用的方案颜色
const getItemSchemeColor = (category: CategoryWeightNode, item: ItemWeightNode): string => {
  const config = item.inherited ? category.effectiveConfig : item.effectiveConfig
  const idx = getSchemeColorIndex(config)
  return schemeColors[idx % schemeColors.length]
}

// 获取扣分项方案tooltip
const getItemSchemeTooltip = (category: CategoryWeightNode, item: ItemWeightNode): string => {
  const config = item.inherited ? category.effectiveConfig : item.effectiveConfig
  if (item.inherited) {
    return `继承自: ${category.categoryName} (${config?.configName || '默认配置'})`
  }
  return `自定义: ${config?.configName || '默认配置'}`
}

// 使用班级的加权系数计算加权分数
const calculateWeightedScore = (originalScore: number | null | undefined): number => {
  if (!originalScore || !props.classInfo?.weightFactor) return Number(originalScore) || 0
  return Number(originalScore) * Number(props.classInfo.weightFactor)
}

// 获取类别的原始扣分总和
const getCategoryOriginalScore = (category: CategoryWeightNode): number => {
  if (!category.items?.length) return 0
  return category.items.reduce((sum, item) => sum + (item.originalScore || 0), 0)
}

// 获取类别的加权扣分总和（使用班级系数计算）
const getCategoryWeightedScore = (category: CategoryWeightNode): number => {
  const originalScore = getCategoryOriginalScore(category)
  return calculateWeightedScore(originalScore)
}

const loadTreeData = async () => {
  if (!props.recordId) return

  loading.value = true
  try {
    const res = await getWeightConfigTree(props.recordId)
    treeData.value = res || {}
    // 默认展开所有类别
    if (treeData.value.categories?.length) {
      expandedCategories.value = treeData.value.categories.map(c => c.categoryId)
    }
  } catch (error) {
    console.error('获取配置树失败:', error)
    ElMessage.error('获取配置树失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.recordId, (newVal) => {
  if (newVal) {
    loadTreeData()
  }
}, { immediate: true })

onMounted(() => {
  if (props.recordId) {
    loadTreeData()
  }
})

const toggleCategory = (categoryId: string) => {
  const index = expandedCategories.value.indexOf(categoryId)
  if (index === -1) {
    expandedCategories.value.push(categoryId)
  } else {
    expandedCategories.value.splice(index, 1)
  }
}

const getCategoryGradient = (index: number) => {
  const gradients = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)'
  ]
  return gradients[index % gradients.length]
}

const getCategoryIconComponent = (name?: string): Component => {
  const lower = name?.toLowerCase() || ''
  if (lower.includes('卫生') || lower.includes('清洁')) return markRaw(Sparkles)
  if (lower.includes('纪律') || lower.includes('行为')) return markRaw(ClipboardCheck)
  if (lower.includes('考勤') || lower.includes('出勤')) return markRaw(BookOpen)
  if (lower.includes('宿舍') || lower.includes('寝室')) return markRaw(Moon)
  return markRaw(AlertCircle)
}

const formatScore = (num?: number | null) => {
  if (num === undefined || num === null) return '0'
  const n = Number(num)
  return n % 1 === 0 ? String(n) : n.toFixed(1)
}

const formatWeight = (num?: number | null) => {
  if (num === undefined || num === null) return '1.00'
  return Number(num).toFixed(2)
}
</script>

<style scoped>
/* 容器 */
.class-weight-inline-container {
  min-height: 300px;
}

/* 头部区域 */
.header-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #eef0f5;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.25);
  color: white;
}

.header-text h2 {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px 0;
}

.header-text p {
  font-size: 12px;
  color: #8b8b9e;
  margin: 0;
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #f0f0f5;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  color: #8b8b9e;
}

.header-badge.enabled {
  background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
  color: #1a5928;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* 公式卡片 */
.formula-card {
  background: white;
  border-radius: 14px;
  padding: 18px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #eef0f5;
}

.formula-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.formula-label {
  font-size: 11px;
  font-weight: 600;
  color: #8b8b9e;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.formula-content {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  flex-wrap: wrap;
}

.formula-text {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.formula-eq {
  font-size: 16px;
  color: #6366f1;
  font-weight: 300;
}

.formula-fraction {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 14px;
  background: #f5f5fa;
  border-radius: 8px;
}

.fraction-top {
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
}

.fraction-line {
  width: 100%;
  height: 1px;
  background: #c8c8d8;
  margin: 3px 0;
}

.fraction-bottom {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.fraction-note {
  font-size: 10px;
  color: #8b8b9e;
  font-weight: 400;
}

.formula-result {
  font-size: 15px;
  font-weight: 700;
  color: #6366f1;
  padding: 3px 10px;
  background: #eef2ff;
  border-radius: 6px;
}

/* 加权汇总 */
.weight-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.summary-label {
  font-size: 11px;
  color: #64748b;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
}

.summary-value.original {
  color: #ef4444;
}

.summary-value.weighted {
  color: #10b981;
}

.summary-arrow {
  color: #6366f1;
}

/* 加权方案列表 */
.schemes-section {
  background: white;
  border-radius: 14px;
  padding: 18px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #eef0f5;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.section-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #6366f115 0%, #8b5cf615 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366f1;
}

.section-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  flex: 1;
}

.scheme-count {
  font-size: 11px;
  color: #8b8b9e;
  padding: 3px 8px;
  background: #f5f5fa;
  border-radius: 10px;
}

.schemes-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}

.scheme-card {
  background: #f8f8fb;
  border-radius: 10px;
  padding: 12px 14px;
  border-left: 4px solid;
  transition: all 0.2s;
}

.scheme-card:hover {
  background: #f0f0f8;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.scheme-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.scheme-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.scheme-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a2e;
  flex: 1;
}

.scheme-tag {
  font-size: 10px;
  font-weight: 500;
  padding: 2px 6px;
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  border-radius: 6px;
}

.scheme-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.scheme-detail-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.scheme-detail-item .detail-label {
  font-size: 11px;
  color: #8b8b9e;
}

.scheme-detail-item .detail-value {
  font-size: 11px;
  font-weight: 500;
  color: #3a3a4e;
  text-align: right;
}

.scheme-detail-item .detail-value.highlight {
  color: #6366f1;
  font-weight: 700;
}

.scheme-detail-item .detail-value.categories {
  max-width: 140px;
  font-size: 10px;
  color: #6366f1;
}

/* 检查类别与扣分项 */
.categories-deductions-section {
  background: white;
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #eef0f5;
}

.categories-wrapper {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.category-block {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #eef0f5;
}

.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: #f8f8fb;
  cursor: pointer;
  border-left: 4px solid;
  transition: all 0.2s;
}

.category-header:hover {
  background: #f0f0f8;
}

.category-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.category-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.category-icon :deep(svg) {
  width: 18px;
  height: 18px;
  color: white;
}

.category-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.category-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.category-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.scheme-indicator {
  font-size: 9px;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 8px;
}

.category-meta {
  font-size: 11px;
  color: #8b8b9e;
}

.category-toggle {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toggle-icon {
  width: 16px;
  height: 16px;
  color: #a0a0b0;
  transition: transform 0.25s;
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

/* 扣分项网格 */
.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
  padding: 12px 14px;
  background: #fafafa;
}

.item-card {
  background: white;
  border-radius: 8px;
  padding: 10px 12px;
  border: 1px solid #e8e8ee;
  border-top: 3px solid;
  transition: all 0.2s;
}

.item-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 6px;
}

.item-name {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a2e;
  line-height: 1.3;
  flex: 1;
}

.item-scheme-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 3px;
}

.item-body {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.item-mode {
  font-size: 10px;
  color: #8b8b9e;
}

.item-scores {
  display: flex;
  align-items: center;
  gap: 4px;
}

.score-base {
  font-size: 12px;
  color: #64748b;
}

.item-weighted {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px dashed #e8e8ee;
}

.weighted-label {
  font-size: 10px;
  color: #8b8b9e;
}

.weighted-value {
  font-size: 13px;
  font-weight: 700;
  color: #10b981;
}

.item-footer {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #e8e8ee;
}

.deduction-badge {
  font-size: 10px;
  color: #6366f1;
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #a0a0b0;
}

.empty-state span {
  font-size: 13px;
  margin-top: 10px;
}

/* 底部说明 */
.footer-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #6366f110 0%, #8b5cf610 100%);
  border-radius: 10px;
  margin-top: 18px;
  border: 1px solid #6366f120;
}

.note-icon {
  width: 28px;
  height: 28px;
  background: white;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #6366f1;
}

.note-text {
  font-size: 12px;
  color: #5a5a7e;
  line-height: 1.5;
}

.note-text strong {
  color: #6366f1;
  font-weight: 600;
}

/* 过渡动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 1000px;
}
</style>
