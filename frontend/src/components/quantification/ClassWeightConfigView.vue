<template>
  <div class="class-weight-config" v-if="classInfo?.weightEnabled">
    <!-- 折叠头部 -->
    <div class="config-header" @click="isExpanded = !isExpanded">
      <div class="header-left">
        <div class="header-icon">
          <Scale class="w-4 h-4" />
        </div>
        <span class="header-title">加权配置详情</span>
        <!-- 多配置模式显示配置数量 -->
        <span v-if="isMultiConfig" class="config-count-badge">
          {{ multiWeightConfigs.length }}个配置
        </span>
        <!-- 单配置模式显示加权系数 -->
        <span v-else class="weight-badge">
          x{{ formatWeightFactor(classInfo.weightFactor) }}
        </span>
      </div>
      <div class="header-right">
        <span class="header-summary" v-if="!isExpanded">
          {{ classInfo.classSize || '-' }}人班 /
          <template v-if="isMultiConfig">多配置加权</template>
          <template v-else>标准{{ classInfo.standardSize || 50 }}人</template>
        </span>
        <ChevronDown
          class="toggle-icon"
          :class="{ 'rotated': isExpanded }"
        />
      </div>
    </div>

    <!-- 展开内容 -->
    <div v-show="isExpanded" class="config-content">
      <!-- ========== 多配置模式 ========== -->
      <template v-if="isMultiConfig">
        <!-- 多配置列表 -->
        <div class="multi-config-section">
          <div class="section-title">加权配置方案</div>
          <div class="config-list">
            <div
              v-for="config in multiWeightConfigs"
              :key="config.configId"
              class="config-card"
              :style="{ borderLeftColor: config.colorCode || '#6366f1' }"
            >
              <div class="config-card-header">
                <div class="config-info">
                  <span class="config-color-dot" :style="{ background: config.colorCode || '#6366f1' }"></span>
                  <span class="config-name">{{ config.configName || '加权配置' }}</span>
                  <span v-if="config.isDefault" class="default-tag">默认</span>
                </div>
                <div class="config-factor">
                  x{{ formatWeightFactor(config.weightFactor) }}
                </div>
              </div>
              <div class="config-card-body">
                <div class="config-detail-row">
                  <span class="detail-label">标准人数:</span>
                  <span class="detail-value">
                    {{ config.standardSize || '-' }}人
                    <span v-if="config.standardSizeModeDesc" class="size-mode-tag">{{ config.standardSizeModeDesc }}</span>
                  </span>
                </div>
                <!-- 系数上下限 -->
                <div class="config-detail-row" v-if="config.minWeight || config.maxWeight">
                  <span class="detail-label">系数限制:</span>
                  <span class="detail-value weight-limit">
                    {{ formatWeightFactor(config.minWeight) }} ~ {{ formatWeightFactor(config.maxWeight) }}
                  </span>
                </div>
                <!-- 系数计算公式 -->
                <div class="config-formula">
                  <span class="formula-label">系数 =</span>
                  <!-- STANDARD模式: 标准人数/班级人数 -->
                  <span v-if="config.weightMode === 'STANDARD'" class="formula-fraction-vertical">
                    <span class="fraction-numerator">
                      <span class="fraction-value">{{ config.standardSize || '-' }}</span>
                      <span class="fraction-desc">(标准人数)</span>
                    </span>
                    <span class="fraction-divider"></span>
                    <span class="fraction-denominator">
                      <span class="fraction-value">{{ classInfo?.classSize || '-' }}</span>
                      <span class="fraction-desc">(班级人数)</span>
                    </span>
                  </span>
                  <!-- PER_CAPITA模式: 班级人数/标准人数 -->
                  <span v-else class="formula-fraction-vertical">
                    <span class="fraction-numerator">
                      <span class="fraction-value">{{ classInfo?.classSize || '-' }}</span>
                      <span class="fraction-desc">(班级人数)</span>
                    </span>
                    <span class="fraction-divider"></span>
                    <span class="fraction-denominator">
                      <span class="fraction-value">{{ config.standardSize || '-' }}</span>
                      <span class="fraction-desc">(标准人数)</span>
                    </span>
                  </span>
                  <span class="formula-eq">=</span>
                  <span class="formula-result">{{ formatWeightFactor(config.weightFactor) }}</span>
                  <!-- 如果系数被限制，显示提示 -->
                  <span v-if="isWeightLimited(config)" class="weight-limited-hint">
                    (已限制)
                  </span>
                </div>
                <div class="config-score-change" v-if="config.originalScore || config.weightedScore">
                  <span class="score-label">扣分:</span>
                  <span class="score-original">{{ formatScore(config.originalScore) }}</span>
                  <span class="score-arrow">→</span>
                  <span class="score-weighted">{{ formatScore(config.weightedScore) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 扣分汇总对比 -->
        <div class="score-summary">
          <div class="summary-title">扣分汇总</div>
          <div class="summary-grid">
            <div class="summary-item">
              <span class="item-label">原始扣分</span>
              <span class="item-value original">-{{ formatScore(classInfo.totalScore) }}</span>
            </div>
            <div class="summary-arrow">
              <ArrowRight class="w-4 h-4" />
            </div>
            <div class="summary-item">
              <span class="item-label">加权扣分</span>
              <span class="item-value weighted">-{{ formatScore(classInfo.weightedTotalScore) }}</span>
            </div>
          </div>
        </div>

        <!-- 计算说明（多配置模式） -->
        <div class="calculation-note multi">
          <Info class="w-3.5 h-3.5" />
          <span>班级人数 {{ classInfo.classSize || '-' }} 人，使用 {{ multiWeightConfigs.length }} 个加权配置，不同分类应用不同的加权系数。</span>
        </div>
      </template>

      <!-- ========== 单配置模式 ========== -->
      <template v-else>
        <!-- 核心公式 -->
        <div class="formula-section">
          <div class="formula-label">加权公式</div>
          <div class="formula-display">
            <span class="formula-text">加权扣分</span>
            <span class="formula-eq">=</span>
            <span class="formula-text">原始扣分</span>
            <span class="formula-eq">×</span>
            <!-- STANDARD模式: 标准人数/班级人数 -->
            <div v-if="classInfo.weightMode === 'STANDARD'" class="formula-fraction-block">
              <div class="fraction-row numerator">
                <span class="fraction-value">{{ classInfo.standardSize || 50 }}</span>
                <span class="fraction-label">(标准人数)</span>
              </div>
              <div class="fraction-line"></div>
              <div class="fraction-row denominator">
                <span class="fraction-value">{{ classInfo.classSize || '-' }}</span>
                <span class="fraction-label">(班级人数)</span>
              </div>
            </div>
            <!-- PER_CAPITA模式(默认): 班级人数/标准人数 -->
            <div v-else class="formula-fraction-block">
              <div class="fraction-row numerator">
                <span class="fraction-value">{{ classInfo.classSize || '-' }}</span>
                <span class="fraction-label">(班级人数)</span>
              </div>
              <div class="fraction-line"></div>
              <div class="fraction-row denominator">
                <span class="fraction-value">{{ classInfo.standardSize || 50 }}</span>
                <span class="fraction-label">(标准人数)</span>
              </div>
            </div>
            <span class="formula-eq">=</span>
            <span class="formula-text">原始扣分</span>
            <span class="formula-eq">×</span>
            <span class="formula-result">{{ formatWeightFactor(classInfo.weightFactor) }}</span>
          </div>
        </div>

        <!-- 扣分汇总对比 -->
        <div class="score-summary">
          <div class="summary-title">扣分汇总</div>
          <div class="summary-grid">
            <div class="summary-item">
              <span class="item-label">原始扣分</span>
              <span class="item-value original">-{{ formatScore(classInfo.totalScore) }}</span>
            </div>
            <div class="summary-arrow">
              <ArrowRight class="w-4 h-4" />
            </div>
            <div class="summary-item">
              <span class="item-label">加权扣分</span>
              <span class="item-value weighted">-{{ formatScore(classInfo.weightedTotalScore) }}</span>
            </div>
          </div>
        </div>

        <!-- 类别扣分明细 -->
        <div class="category-scores" v-if="hasWeightedCategoryScores">
          <div class="category-title">类别扣分明细</div>
          <div class="category-list">
            <div
              v-for="cat in weightedCategories"
              :key="cat.categoryName"
              class="category-item"
            >
              <div class="category-name">
                <component :is="getCategoryIcon(cat.categoryName)" class="w-3.5 h-3.5" />
                <span>{{ cat.categoryName }}</span>
              </div>
              <div class="category-scores-display">
                <span class="score-original">{{ formatScore(cat.totalScore) }}</span>
                <span class="score-arrow">→</span>
                <span class="score-weighted">{{ formatScore(cat.weightedTotalScore) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 扣分项明细（显示加权后分数） -->
        <div class="deduction-items" v-if="showDeductionDetails && categoryStats?.length">
          <div class="deduction-title">
            <span>扣分项加权详情</span>
            <button class="toggle-details" @click="showItemDetails = !showItemDetails">
              {{ showItemDetails ? '收起' : '展开' }}
            </button>
          </div>
          <div v-show="showItemDetails" class="items-list">
            <div
              v-for="cat in categoryStats"
              :key="cat.categoryId || cat.categoryName"
              class="item-category"
            >
              <div class="item-category-header">
                <component :is="getCategoryIcon(cat.categoryName)" class="w-3 h-3" />
                <span>{{ cat.categoryName }}</span>
              </div>
              <div class="item-grid">
                <div
                  v-for="item in cat.deductions"
                  :key="item.id"
                  class="item-card"
                >
                  <div class="item-name" :title="item.deductionItemName">{{ item.deductionItemName }}</div>
                  <div class="item-score-change">
                    <span class="original-score">{{ formatScore(item.actualScore) }}</span>
                    <span class="score-arrow">→</span>
                    <span class="weighted-score">{{ formatScore(calculateWeightedScore(item.actualScore)) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 计算说明 -->
        <div class="calculation-note">
          <Info class="w-3.5 h-3.5" />
          <span>班级人数 {{ classInfo.classSize || '-' }} 人，标准人数 {{ classInfo.standardSize || 50 }} 人，加权系数 {{ formatWeightFactor(classInfo.weightFactor) }}，扣分按比例调整。</span>
        </div>
      </template>
    </div>
  </div>

  <!-- 未启用加权时显示 -->
  <div class="weight-disabled" v-else-if="showDisabledHint">
    <Scale class="w-4 h-4 text-slate-400" />
    <span>本次检查未启用加权</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Scale, ChevronDown, ArrowRight, Info,
  Sparkles, ClipboardCheck, BookOpen, Moon, AlertCircle
} from 'lucide-vue-next'
import type { ClassStats, CheckRecordCategoryStatsDTO, DeductionDetail } from '@/api/checkRecord'

// 多配置加权信息接口
interface MultiWeightConfigInfo {
  configId?: number
  configName?: string
  colorCode?: string
  colorName?: string
  isDefault?: boolean
  applyCategoryIds?: number[]
  applyCategoryNames?: string[]
  weightMode?: string
  weightModeDesc?: string
  standardSizeMode?: string
  standardSizeModeDesc?: string  // 标准人数获取方式描述
  standardSize?: number
  weightFactor?: number
  minWeight?: number  // 系数下限
  maxWeight?: number  // 系数上限
  originalScore?: number
  weightedScore?: number
}

// 扩展ClassStats类型
interface ExtendedClassStats extends ClassStats {
  multiConfigEnabled?: boolean
  multiWeightConfigs?: MultiWeightConfigInfo[]
}

const props = withDefaults(defineProps<{
  classInfo: ExtendedClassStats | null | undefined
  categoryStats?: CheckRecordCategoryStatsDTO[]
  showDeductionDetails?: boolean
  showDisabledHint?: boolean
  defaultExpanded?: boolean
}>(), {
  showDeductionDetails: true,
  showDisabledHint: false,
  defaultExpanded: false
})

const isExpanded = ref(props.defaultExpanded)
const showItemDetails = ref(false)

// 是否为多配置模式
const isMultiConfig = computed(() => {
  return props.classInfo?.multiConfigEnabled === true &&
         props.classInfo?.multiWeightConfigs &&
         props.classInfo.multiWeightConfigs.length > 1
})

// 多配置列表
const multiWeightConfigs = computed(() => {
  return props.classInfo?.multiWeightConfigs || []
})

// 计算有加权数据的类别
const weightedCategories = computed(() => {
  if (!props.categoryStats?.length) {
    // 从 classInfo 构造基本类别信息
    const categories: { categoryName: string; totalScore: number; weightedTotalScore: number }[] = []
    const classInfo = props.classInfo
    if (classInfo) {
      if (classInfo.hygieneScore) {
        categories.push({
          categoryName: '卫生检查',
          totalScore: Number(classInfo.hygieneScore) || 0,
          weightedTotalScore: Number(classInfo.weightedHygieneScore) || calculateWeightedScore(classInfo.hygieneScore)
        })
      }
      if (classInfo.disciplineScore) {
        categories.push({
          categoryName: '纪律检查',
          totalScore: Number(classInfo.disciplineScore) || 0,
          weightedTotalScore: Number(classInfo.weightedDisciplineScore) || calculateWeightedScore(classInfo.disciplineScore)
        })
      }
      if (classInfo.attendanceScore) {
        categories.push({
          categoryName: '考勤检查',
          totalScore: Number(classInfo.attendanceScore) || 0,
          weightedTotalScore: Number(classInfo.weightedAttendanceScore) || calculateWeightedScore(classInfo.attendanceScore)
        })
      }
      if (classInfo.dormitoryScore) {
        categories.push({
          categoryName: '宿舍检查',
          totalScore: Number(classInfo.dormitoryScore) || 0,
          weightedTotalScore: Number(classInfo.weightedDormitoryScore) || calculateWeightedScore(classInfo.dormitoryScore)
        })
      }
      if (classInfo.otherScore) {
        categories.push({
          categoryName: '其他',
          totalScore: Number(classInfo.otherScore) || 0,
          weightedTotalScore: Number(classInfo.weightedOtherScore) || calculateWeightedScore(classInfo.otherScore)
        })
      }
    }
    return categories
  }

  return props.categoryStats.map(cat => ({
    categoryName: cat.categoryName,
    totalScore: cat.totalScore || 0,
    weightedTotalScore: cat.weightedTotalScore || calculateWeightedScore(cat.totalScore)
  })).filter(c => c.totalScore > 0)
})

const hasWeightedCategoryScores = computed(() => weightedCategories.value.length > 0)

// 计算加权后分数
const calculateWeightedScore = (originalScore: number | null | undefined): number => {
  if (!originalScore || !props.classInfo?.weightFactor) return Number(originalScore) || 0
  return Number(originalScore) * Number(props.classInfo.weightFactor)
}

// 格式化分数
const formatScore = (score: number | null | undefined): string => {
  if (score === null || score === undefined) return '0'
  const n = Number(score)
  return n % 1 === 0 ? String(n) : n.toFixed(1)
}

// 格式化加权系数
const formatWeightFactor = (factor: number | null | undefined): string => {
  if (!factor) return '1.00'
  return Number(factor).toFixed(2)
}

// 判断系数是否被限制（是否触碰上下限）
const isWeightLimited = (config: MultiWeightConfigInfo): boolean => {
  if (!config.weightFactor || !props.classInfo?.classSize || !config.standardSize) return false

  // 计算原始系数
  const rawFactor = props.classInfo.classSize / config.standardSize

  // 检查是否被下限或上限限制
  if (config.minWeight && rawFactor < config.minWeight) return true
  if (config.maxWeight && rawFactor > config.maxWeight) return true

  return false
}

// 获取类别图标
const getCategoryIcon = (name: string) => {
  const lower = name?.toLowerCase() || ''
  if (lower.includes('卫生') || lower.includes('清洁')) return Sparkles
  if (lower.includes('纪律') || lower.includes('行为')) return ClipboardCheck
  if (lower.includes('考勤') || lower.includes('出勤')) return BookOpen
  if (lower.includes('宿舍') || lower.includes('寝室')) return Moon
  return AlertCircle
}
</script>

<style scoped>
.class-weight-config {
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  border-radius: 12px;
  border: 1px solid #e0e7ff;
  overflow: hidden;
}

.config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.config-header:hover {
  background: rgba(99, 102, 241, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.header-title {
  font-size: 13px;
  font-weight: 600;
  color: #3730a3;
}

.weight-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border-radius: 12px;
}

.config-count-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border-radius: 10px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-summary {
  font-size: 12px;
  color: #6b7280;
}

.toggle-icon {
  width: 16px;
  height: 16px;
  color: #9ca3af;
  transition: transform 0.25s;
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

.config-content {
  padding: 0 16px 16px;
  border-top: 1px solid #e0e7ff;
  background: white;
}

/* 多配置区域样式 */
.multi-config-section {
  margin-top: 14px;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.config-card {
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  border-left-width: 4px;
  padding: 12px;
}

.config-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e2e8f0;
}

.config-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.config-color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.config-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.default-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: #dbeafe;
  color: #1d4ed8;
  border-radius: 4px;
  font-weight: 500;
}

.config-factor {
  font-size: 14px;
  font-weight: 700;
  color: #6366f1;
  padding: 2px 8px;
  background: #eef2ff;
  border-radius: 6px;
}

.config-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-detail-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
}

.detail-label {
  color: #64748b;
  flex-shrink: 0;
  min-width: 60px;
}

.detail-value {
  color: #334155;
  font-weight: 500;
}

.detail-value.categories {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.category-tag {
  font-size: 11px;
  padding: 2px 6px;
  background: #f1f5f9;
  color: #475569;
  border-radius: 4px;
}

/* 系数计算公式 */
.config-formula {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  padding: 8px 10px;
  background: #f0f4ff;
  border-radius: 6px;
  font-size: 12px;
}

.config-formula .formula-label {
  color: #64748b;
  font-weight: 500;
}

.config-formula .formula-fraction {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.config-formula .fraction-num {
  color: #6366f1;
  font-weight: 600;
}

.config-formula .fraction-bar {
  color: #94a3b8;
}

.config-formula .fraction-den {
  color: #64748b;
  font-weight: 500;
}

/* 垂直分数显示样式 */
.config-formula .formula-fraction-vertical {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  min-width: 100px;
}

.config-formula .fraction-numerator,
.config-formula .fraction-denominator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
}

.config-formula .fraction-value {
  font-size: 14px;
  font-weight: 700;
  color: #6366f1;
}

.config-formula .fraction-desc {
  font-size: 10px;
  color: #94a3b8;
  font-weight: 400;
}

.config-formula .fraction-divider {
  width: 100%;
  height: 1px;
  background: #cbd5e1;
  margin: 2px 0;
}

.config-formula .fraction-denominator .fraction-value {
  color: #64748b;
}

.config-formula .formula-eq {
  color: #94a3b8;
}

.config-formula .formula-result {
  color: #6366f1;
  font-weight: 700;
  padding: 2px 6px;
  background: #eef2ff;
  border-radius: 4px;
}

.config-formula .weight-limited-hint {
  font-size: 10px;
  color: #f59e0b;
  font-weight: 500;
}

/* 标准人数获取方式标签 */
.size-mode-tag {
  font-size: 10px;
  padding: 1px 5px;
  background: #dbeafe;
  color: #1d4ed8;
  border-radius: 3px;
  margin-left: 6px;
}

/* 系数限制显示 */
.weight-limit {
  color: #7c3aed !important;
  font-weight: 600;
}

.config-score-change {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  font-size: 12px;
}

.score-label {
  color: #64748b;
}

/* 公式区域 */
.formula-section {
  padding: 14px;
  margin-top: 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px dashed #e2e8f0;
}

.formula-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.formula-display {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.formula-text {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
}

.formula-eq {
  font-size: 14px;
  color: #6366f1;
  font-weight: 300;
}

.formula-fraction {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.fraction-top {
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
}

.fraction-line {
  width: 100%;
  height: 1px;
  background: #cbd5e1;
  margin: 3px 0;
}

.fraction-bottom {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

/* 单配置模式分数块样式 */
.formula-fraction-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 14px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  min-width: 120px;
}

.formula-fraction-block .fraction-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 0;
}

.formula-fraction-block .fraction-value {
  font-size: 15px;
  font-weight: 700;
  color: #6366f1;
}

.formula-fraction-block .fraction-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
}

.formula-fraction-block .denominator .fraction-value {
  color: #64748b;
}

.formula-result {
  font-size: 14px;
  font-weight: 700;
  color: #6366f1;
  padding: 2px 8px;
  background: #eef2ff;
  border-radius: 4px;
}

/* 扣分汇总 */
.score-summary {
  margin-top: 14px;
}

.summary-title {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.summary-grid {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 10px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.item-label {
  font-size: 11px;
  color: #64748b;
}

.item-value {
  font-size: 18px;
  font-weight: 700;
}

.item-value.original {
  color: #ef4444;
}

.item-value.weighted {
  color: #10b981;
}

.summary-arrow {
  color: #6366f1;
}

/* 类别扣分 */
.category-scores {
  margin-top: 14px;
}

.category-title {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.category-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #475569;
}

.category-scores-display {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.score-original {
  color: #ef4444;
  font-weight: 500;
}

.score-arrow {
  color: #6366f1;
  font-size: 10px;
}

.score-weighted {
  color: #10b981;
  font-weight: 600;
}

/* 扣分项明细 */
.deduction-items {
  margin-top: 14px;
}

.deduction-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.toggle-details {
  font-size: 11px;
  color: #6366f1;
  font-weight: 500;
  background: none;
  border: none;
  cursor: pointer;
  text-transform: none;
  letter-spacing: normal;
}

.toggle-details:hover {
  text-decoration: underline;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-category {
  padding: 10px;
  background: #f8fafc;
  border-radius: 8px;
}

.item-category-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #e2e8f0;
}

.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 6px;
}

.item-card {
  background: white;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.item-name {
  font-size: 11px;
  color: #475569;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.item-score-change {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}

.original-score {
  color: #ef4444;
}

.weighted-score {
  color: #10b981;
  font-weight: 600;
}

/* 计算说明 */
.calculation-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 14px;
  padding: 10px 12px;
  background: #fefce8;
  border-radius: 8px;
  font-size: 11px;
  color: #854d0e;
  line-height: 1.4;
}

.calculation-note.multi {
  background: #ecfdf5;
  color: #047857;
}

.calculation-note svg {
  flex-shrink: 0;
  margin-top: 1px;
}

/* 未启用加权 */
.weight-disabled {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f1f5f9;
  border-radius: 8px;
  font-size: 12px;
  color: #64748b;
}
</style>
