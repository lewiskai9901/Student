<template>
  <el-dialog
    v-model="visible"
    title=""
    width="960px"
    :close-on-click-modal="false"
    @close="handleClose"
    class="weight-tree-dialog"
  >
    <div v-loading="loading" class="weight-tree-container">
      <!-- 顶部标题区 -->
      <div class="header-section">
        <div class="header-main">
          <div class="header-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 3v18M3 12h18M5.5 5.5l13 13M18.5 5.5l-13 13" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="header-text">
            <h2>加权规则配置</h2>
            <p>了解各类别扣分项的加权计算方式</p>
          </div>
        </div>
        <div class="header-badge" :class="{ 'enabled': treeData.weightEnabled }">
          <span class="badge-dot"></span>
          <span>{{ treeData.weightEnabled ? '已启用' : '未启用' }}</span>
        </div>
      </div>

      <!-- 核心公式卡片 -->
      <div class="formula-card" v-if="treeData.weightEnabled && treeData.globalConfig">
        <div class="formula-inner">
          <div class="formula-label">核心公式</div>
          <div class="formula-content">
            <span class="formula-text">加权扣分</span>
            <span class="formula-eq">=</span>
            <span class="formula-text">原始扣分</span>
            <span class="formula-eq">×</span>
            <div class="formula-fraction">
              <span class="fraction-top">班级人数</span>
              <span class="fraction-line"></span>
              <span class="fraction-bottom">{{ treeData.globalConfig.standardSize || 50 }}人 <span class="fraction-note">(标准)</span></span>
            </div>
          </div>
        </div>
        <div class="formula-examples">
          <div class="example-item">
            <span class="example-label">60人班</span>
            <span class="example-value up">×1.20</span>
          </div>
          <div class="example-item highlight">
            <span class="example-label">50人班</span>
            <span class="example-value">×1.00</span>
          </div>
          <div class="example-item">
            <span class="example-label">40人班</span>
            <span class="example-value down">×0.80</span>
          </div>
        </div>
      </div>

      <!-- 树形结构 -->
      <div class="tree-wrapper">
        <!-- 根节点 -->
        <div class="tree-root-section">
          <div class="root-node" @click="showConfigDetail(treeData.globalConfig, '全局配置')">
            <div class="root-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"/>
                <path d="M12 1v6m0 6v10M1 12h6m6 0h10"/>
              </svg>
            </div>
            <div class="root-info">
              <span class="root-title">{{ treeData.checkName || '检查模板' }}</span>
              <span class="root-config" v-if="treeData.globalConfig?.configName">
                {{ treeData.globalConfig.configName }}
              </span>
            </div>
            <div class="root-arrow" v-if="treeData.globalConfig">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 18l6-6-6-6"/>
              </svg>
            </div>
          </div>
          <div class="root-connector" v-if="treeData.categories?.length">
            <div class="connector-v"></div>
          </div>
        </div>

        <!-- 类别层 -->
        <div class="categories-section" v-if="treeData.categories?.length">
          <div class="categories-h-line"></div>
          <div class="categories-grid">
            <div
              v-for="(category, idx) in treeData.categories"
              :key="category.categoryId"
              class="category-branch"
            >
              <!-- 分支连接线 -->
              <div class="branch-connector">
                <div class="branch-v"></div>
              </div>

              <!-- 类别节点 -->
              <div
                class="category-node"
                :class="{ 'expanded': expandedCategories.includes(category.categoryId) }"
                @click="toggleCategory(category.categoryId)"
              >
                <div class="category-icon" :style="{ background: getCategoryGradient(idx) }">
                  <component :is="getCategoryIconComponent(category.categoryName)" />
                </div>
                <div class="category-info">
                  <span class="category-name">{{ category.categoryName }}</span>
                  <span class="category-meta">
                    {{ category.items?.length || 0 }}项
                    <template v-if="category.effectiveConfig?.configName">
                      · {{ category.effectiveConfig.configName }}
                    </template>
                  </span>
                </div>
                <div class="category-toggle" v-if="category.items?.length">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M19 9l-7 7-7-7"/>
                  </svg>
                </div>
              </div>

              <!-- 扣分项展开区 -->
              <transition name="expand">
                <div
                  v-if="category.items?.length && expandedCategories.includes(category.categoryId)"
                  class="items-section"
                >
                  <div class="items-connector"></div>
                  <div class="items-list">
                    <div
                      v-for="item in category.items"
                      :key="item.itemId"
                      class="item-node"
                      @click.stop="showConfigDetail(item.effectiveConfig, item.itemName)"
                    >
                      <div class="item-dot"></div>
                      <div class="item-content">
                        <span class="item-name">{{ item.itemName }}</span>
                        <div class="item-score">
                          <span class="score-base">{{ formatScore(item.baseScore) }}分</span>
                          <span class="score-mode">/ {{ item.deductModeDesc || '固定扣分' }}</span>
                        </div>
                      </div>
                      <div class="item-config" v-if="item.effectiveConfig">
                        <span class="config-badge" :class="{ 'inherited': item.inherited }">
                          {{ item.inherited ? '继承' : '自定义' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </transition>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
          <span>暂无类别配置</span>
        </div>
      </div>

      <!-- 底部说明 -->
      <div class="footer-note" v-if="treeData.weightEnabled">
        <div class="note-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 16v-4M12 8h.01"/>
          </svg>
        </div>
        <div class="note-text">
          人数较少的班级加权系数较低，扣分自动减少，确保公平评比
        </div>
      </div>
    </div>

    <!-- 配置详情弹窗 -->
    <Teleport to="body">
      <transition name="fade">
        <div v-if="configDetailVisible" class="config-detail-overlay" @click="configDetailVisible = false">
          <div class="config-detail-panel" @click.stop>
            <div class="detail-header">
              <h3>{{ configDetailTitle }}</h3>
              <button class="close-btn" @click="configDetailVisible = false">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18M6 6l12 12"/>
                </svg>
              </button>
            </div>
            <div class="detail-body" v-if="configDetailData">
              <div class="detail-row">
                <span class="detail-label">配置名称</span>
                <span class="detail-value">{{ configDetailData.configName || '默认配置' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">加权模式</span>
                <span class="detail-value">{{ configDetailData.weightModeDesc || '标准人数模式' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">标准人数</span>
                <span class="detail-value highlight">{{ configDetailData.standardSize || 50 }} 人</span>
              </div>
              <div class="detail-row" v-if="configDetailData.enableWeightLimit">
                <span class="detail-label">系数范围</span>
                <span class="detail-value">{{ configDetailData.minWeight }} ~ {{ configDetailData.maxWeight }}</span>
              </div>
              <div class="detail-row" v-if="configDetailData.formulaDescription">
                <span class="detail-label">计算公式</span>
                <span class="detail-value code">{{ configDetailData.formulaDescription }}</span>
              </div>
            </div>
            <div class="detail-body empty" v-else>
              <span>该项使用默认加权规则</span>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <template #footer>
      <button class="dialog-btn" @click="handleClose">关闭</button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, markRaw, type Component } from 'vue'
import { getWeightConfigTree } from '@/api/checkRecord'
import { ElMessage } from 'element-plus'
import {
  Sparkles,
  ClipboardCheck,
  BookOpen,
  Moon,
  AlertCircle
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

interface WeightConfigTreeData {
  recordId?: string
  checkName?: string
  weightEnabled?: boolean
  globalConfig?: WeightConfigNode
  categories?: CategoryWeightNode[]
}

const props = defineProps<{
  modelValue: boolean
  recordId: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = ref(false)
const loading = ref(false)
const treeData = ref<WeightConfigTreeData>({})
const expandedCategories = ref<string[]>([])

// 配置详情弹窗
const configDetailVisible = ref(false)
const configDetailTitle = ref('')
const configDetailData = ref<WeightConfigNode | null>(null)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.recordId) {
    loadTreeData()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadTreeData = async () => {
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

const handleClose = () => {
  visible.value = false
  expandedCategories.value = []
  configDetailVisible.value = false
}

const toggleCategory = (categoryId: string) => {
  const index = expandedCategories.value.indexOf(categoryId)
  if (index === -1) {
    expandedCategories.value.push(categoryId)
  } else {
    expandedCategories.value.splice(index, 1)
  }
}

const showConfigDetail = (config: WeightConfigNode | undefined, title: string) => {
  configDetailTitle.value = title
  configDetailData.value = config || null
  configDetailVisible.value = true
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

const formatScore = (num?: number) => {
  if (num === undefined || num === null) return '0'
  return Number(num).toFixed(1)
}
</script>

<style scoped>
/* 容器 */
.weight-tree-container {
  padding: 24px;
  background: #fafbfc;
  min-height: 400px;
}

/* 头部区域 */
.header-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.25);
}

.header-icon svg {
  width: 24px;
  height: 24px;
  color: white;
}

.header-text h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px 0;
}

.header-text p {
  font-size: 13px;
  color: #8b8b9e;
  margin: 0;
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f0f0f5;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #8b8b9e;
}

.header-badge.enabled {
  background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
  color: #1a5928;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

/* 公式卡片 */
.formula-card {
  background: white;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #eef0f5;
}

.formula-inner {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px dashed #e8e8ee;
}

.formula-label {
  font-size: 12px;
  font-weight: 600;
  color: #8b8b9e;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.formula-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.formula-text {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.formula-eq {
  font-size: 18px;
  color: #667eea;
  font-weight: 300;
}

.formula-fraction {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 16px;
  background: #f5f5fa;
  border-radius: 8px;
}

.fraction-top {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a2e;
}

.fraction-line {
  width: 100%;
  height: 1px;
  background: #c8c8d8;
  margin: 4px 0;
}

.fraction-bottom {
  font-size: 13px;
  font-weight: 500;
  color: #667eea;
}

.fraction-note {
  font-size: 11px;
  color: #8b8b9e;
  font-weight: 400;
}

.formula-examples {
  display: flex;
  gap: 12px;
}

.example-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f8f8fb;
  border-radius: 10px;
  transition: all 0.2s;
}

.example-item.highlight {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border: 1px solid #667eea30;
}

.example-label {
  font-size: 13px;
  color: #5a5a6e;
}

.example-value {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  font-family: 'SF Mono', 'Monaco', monospace;
}

.example-value.up {
  color: #f5576c;
}

.example-value.down {
  color: #43e97b;
}

/* 树形结构 */
.tree-wrapper {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #eef0f5;
}

/* 根节点 */
.tree-root-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.root-node {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.root-node:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.root-icon {
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.root-icon svg {
  width: 20px;
  height: 20px;
  color: white;
}

.root-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.root-title {
  font-size: 15px;
  font-weight: 600;
  color: white;
}

.root-config {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.root-arrow {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.root-arrow svg {
  width: 16px;
  height: 16px;
  color: rgba(255, 255, 255, 0.6);
}

.root-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.connector-v {
  width: 2px;
  height: 24px;
  background: linear-gradient(180deg, #667eea 0%, #e0e0e8 100%);
}

/* 类别区域 */
.categories-section {
  position: relative;
  padding-top: 8px;
}

.categories-h-line {
  position: absolute;
  top: 8px;
  left: 10%;
  right: 10%;
  height: 2px;
  background: #e0e0e8;
}

.categories-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  padding-top: 8px;
}

.category-branch {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 160px;
  max-width: 200px;
}

.branch-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.branch-v {
  width: 2px;
  height: 16px;
  background: #e0e0e8;
}

/* 类别节点 */
.category-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: white;
  border: 2px solid #eef0f5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s;
  width: 100%;
}

.category-node:hover {
  border-color: #d0d0e0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.category-node.expanded {
  border-color: #667eea;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
}

.category-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
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
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.category-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  transition: transform 0.25s;
}

.category-node.expanded .category-toggle {
  transform: rotate(180deg);
}

.category-toggle svg {
  width: 14px;
  height: 14px;
  color: #a0a0b0;
}

/* 扣分项展开区 */
.items-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  margin-top: 4px;
}

.items-connector {
  width: 2px;
  height: 12px;
  background: #e0e0e8;
}

.items-list {
  width: 100%;
  background: #f8f8fb;
  border-radius: 10px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.item-node:hover {
  background: #f0f0f8;
}

.item-dot {
  width: 6px;
  height: 6px;
  background: #c0c0d0;
  border-radius: 50%;
  flex-shrink: 0;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #3a3a4e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-score {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.score-base {
  font-size: 12px;
  font-weight: 600;
  color: #f5576c;
}

.score-mode {
  font-size: 11px;
  color: #a0a0b0;
}

.item-config {
  flex-shrink: 0;
}

.config-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea20 0%, #764ba220 100%);
  color: #667eea;
}

.config-badge.inherited {
  background: #f0f0f5;
  color: #8b8b9e;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: #a0a0b0;
}

.empty-state svg {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  color: #d0d0e0;
}

.empty-state span {
  font-size: 14px;
}

/* 底部说明 */
.footer-note {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #667eea10 0%, #764ba210 100%);
  border-radius: 12px;
  margin-top: 20px;
  border: 1px solid #667eea20;
}

.note-icon {
  width: 32px;
  height: 32px;
  background: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.note-icon svg {
  width: 18px;
  height: 18px;
  color: #667eea;
}

.note-text {
  font-size: 13px;
  color: #5a5a7e;
  line-height: 1.5;
}

/* 配置详情弹窗 */
.config-detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.config-detail-panel {
  background: white;
  border-radius: 16px;
  width: 380px;
  max-width: 90vw;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.detail-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin: 0;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.close-btn svg {
  width: 16px;
  height: 16px;
  color: white;
}

.detail-body {
  padding: 20px;
}

.detail-body.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
  color: #a0a0b0;
  font-size: 14px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f5;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 13px;
  color: #8b8b9e;
}

.detail-value {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a2e;
  text-align: right;
}

.detail-value.highlight {
  color: #667eea;
  font-size: 16px;
  font-weight: 600;
}

.detail-value.code {
  font-family: 'SF Mono', 'Monaco', monospace;
  font-size: 12px;
  background: #f5f5fa;
  padding: 4px 8px;
  border-radius: 4px;
}

/* 弹窗按钮 */
.dialog-btn {
  padding: 10px 32px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  background: #f0f0f5;
  color: #5a5a6e;
  cursor: pointer;
  transition: all 0.2s;
}

.dialog-btn:hover {
  background: #e8e8ee;
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
  max-height: 500px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
/* 弹窗样式覆盖 */
.weight-tree-dialog .el-dialog__header {
  display: none;
}

.weight-tree-dialog .el-dialog__body {
  padding: 0;
}

.weight-tree-dialog .el-dialog__footer {
  padding: 16px 24px;
  border-top: 1px solid #eef0f5;
  display: flex;
  justify-content: flex-end;
}
</style>
