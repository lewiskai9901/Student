<template>
  <el-dialog
    v-model="visible"
    title="加权配置详情"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading" class="weight-config-detail">
      <!-- 基本信息卡片 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-tag v-if="config.weightEnabled" type="success" size="small">已启用</el-tag>
            <el-tag v-else type="info" size="small">未启用</el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="配置名称">
            {{ config.configName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="配置编码">
            {{ config.configCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="加权模式">
            <el-tag :type="getWeightModeType(config.weightMode)">
              {{ config.weightModeDesc || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标准人数模式">
            {{ config.standardSizeModeDesc || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="标准人数">
            <span class="highlight-number">{{ config.standardSize || '-' }}</span> 人
          </el-descriptions-item>
          <el-descriptions-item label="权重限制">
            <template v-if="config.enableWeightLimit">
              {{ config.minWeight }} ~ {{ config.maxWeight }}
            </template>
            <template v-else>
              无限制
            </template>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 加权说明卡片 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <span>加权说明</span>
        </template>
        <div class="explanation-section">
          <div class="explanation-item">
            <div class="explanation-label">加权原理:</div>
            <div class="explanation-content">{{ config.weightModeExplanation }}</div>
          </div>
          <div class="explanation-item">
            <div class="explanation-label">计算公式:</div>
            <div class="explanation-content formula">{{ config.formulaDescription }}</div>
          </div>
          <div class="explanation-item">
            <div class="explanation-label">计算示例:</div>
            <div class="explanation-content example">
              <pre>{{ config.calculationExample }}</pre>
            </div>
          </div>
          <div v-if="config.description" class="explanation-item">
            <div class="explanation-label">配置说明:</div>
            <div class="explanation-content">{{ config.description }}</div>
          </div>
        </div>
      </el-card>

      <!-- 分段规则（仅当加权模式为SEGMENT时显示） -->
      <el-card v-if="config.weightMode === 'SEGMENT' && config.segmentRules?.length" class="info-card" shadow="never">
        <template #header>
          <span>分段规则</span>
        </template>
        <el-table :data="config.segmentRules" border size="small">
          <el-table-column prop="minSize" label="最小人数" align="center" width="100" />
          <el-table-column prop="maxSize" label="最大人数" align="center" width="100" />
          <el-table-column prop="weight" label="加权系数" align="center" width="100">
            <template #default="{ row }">
              <span class="highlight-number">{{ row.weight }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" />
        </el-table>
      </el-card>

      <!-- 多加权配置卡片（仅当启用多配置时显示） -->
      <el-card v-if="config.multiConfigEnabled && config.multiConfigs?.length" class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>多加权配置方案</span>
            <el-tag type="success" size="small">已启用多配置</el-tag>
            <span class="tips">共 {{ config.multiConfigs.length }} 个配置方案</span>
          </div>
        </template>
        <div class="multi-config-list">
          <div
            v-for="(mc, index) in config.multiConfigs"
            :key="index"
            class="multi-config-item"
            :style="{ borderLeftColor: mc.colorCode }"
          >
            <div class="mc-header">
              <span
                class="mc-color-dot"
                :style="{ backgroundColor: mc.colorCode }"
              ></span>
              <span class="mc-name">{{ mc.configName }}</span>
              <el-tag v-if="mc.isDefault" type="warning" size="small">默认</el-tag>
              <span class="mc-scope">{{ mc.applyScope === 'CATEGORY' ? '分类级别' : '扣分项级别' }}</span>
            </div>
            <div class="mc-details">
              <span class="mc-detail-item">
                <span class="label">加权模式:</span>
                <el-tag :type="getWeightModeType(mc.weightMode)" size="small">
                  {{ mc.weightModeDesc }}
                </el-tag>
              </span>
              <span class="mc-detail-item">
                <span class="label">标准人数:</span>
                <span class="highlight-number">{{ mc.calculatedStandardSize || '-' }}</span>人
                <span class="sub-info">({{ mc.standardSizeModeDesc }})</span>
              </span>
              <span v-if="mc.minWeight && mc.maxWeight" class="mc-detail-item">
                <span class="label">权重限制:</span>
                {{ mc.minWeight }} ~ {{ mc.maxWeight }}
              </span>
            </div>
            <div v-if="mc.applyCategoryNames?.length" class="mc-categories">
              <span class="label">应用分类:</span>
              <el-tag
                v-for="catName in mc.applyCategoryNames"
                :key="catName"
                size="small"
                type="info"
                class="category-tag"
              >
                {{ catName }}
              </el-tag>
            </div>
            <div class="mc-formula">
              <span class="label">计算公式:</span>
              <span class="formula-text">{{ mc.formulaDescription }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 班级加权系数列表 -->
      <el-card v-if="config.classWeightFactors?.length" class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>各班级加权系数</span>
            <span class="tips">共 {{ config.classWeightFactors.length }} 个班级</span>
          </div>
        </template>
        <el-table
          :data="config.classWeightFactors"
          border
          size="small"
          max-height="400"
          :default-sort="{ prop: 'weightFactor', order: 'ascending' }"
        >
          <el-table-column prop="className" label="班级名称" min-width="120">
            <template #default="{ row }">
              <span
                v-if="row.colorCode"
                class="color-indicator"
                :style="{ backgroundColor: row.colorCode }"
              ></span>
              {{ row.className }}
            </template>
          </el-table-column>
          <el-table-column prop="actualSize" label="实际人数" align="center" width="90">
            <template #default="{ row }">
              <span :class="{ 'text-warning': row.actualSize !== config.standardSize }">
                {{ row.actualSize }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="standardSize" label="标准人数" align="center" width="90" />
          <el-table-column prop="weightFactor" label="加权系数" align="center" width="100" sortable>
            <template #default="{ row }">
              <el-tag
                :type="getFactorTagType(row.weightFactor)"
                size="small"
              >
                {{ formatNumber(row.weightFactor) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="originalScore" label="原始扣分" align="center" width="90">
            <template #default="{ row }">
              {{ formatNumber(row.originalScore) }}
            </template>
          </el-table-column>
          <el-table-column prop="weightedScore" label="加权扣分" align="center" width="90">
            <template #default="{ row }">
              <span class="highlight-number">{{ formatNumber(row.weightedScore) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="calculationNote" label="计算说明" min-width="200" show-overflow-tooltip />
        </el-table>
      </el-card>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getWeightConfigDetail } from '@/api/v2/quantification'
import { ElMessage } from 'element-plus'

interface SegmentRule {
  minSize: number
  maxSize: number
  weight: number
  description: string
}

interface ClassWeightFactor {
  classId: string
  className: string
  actualSize: number
  standardSize: number
  weightFactor: number
  originalScore: number
  weightedScore: number
  calculationNote: string
  weightConfigId?: string
  colorCode?: string
}

interface MultiWeightConfig {
  configId?: string
  weightConfigId?: string
  configName?: string
  colorCode?: string
  colorName?: string
  applyScope?: string
  applyCategoryIds?: string[]
  applyCategoryNames?: string[]
  weightMode?: string
  weightModeDesc?: string
  standardSizeMode?: string
  standardSizeModeDesc?: string
  calculatedStandardSize?: number
  minWeight?: number
  maxWeight?: number
  isDefault?: boolean
  priority?: number
  formulaDescription?: string
}

interface WeightConfigDetail {
  configId?: string
  configName?: string
  configCode?: string
  weightEnabled?: boolean
  weightMode?: string
  weightModeDesc?: string
  weightModeExplanation?: string
  standardSizeMode?: string
  standardSizeModeDesc?: string
  standardSize?: number
  minWeight?: number
  maxWeight?: number
  enableWeightLimit?: boolean
  segmentRules?: SegmentRule[]
  classWeightFactors?: ClassWeightFactor[]
  formulaDescription?: string
  calculationExample?: string
  description?: string
  multiConfigEnabled?: boolean
  multiConfigs?: MultiWeightConfig[]
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
const config = ref<WeightConfigDetail>({})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.recordId) {
    loadWeightConfig()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadWeightConfig = async () => {
  loading.value = true
  try {
    const res = await getWeightConfigDetail(props.recordId)
    if (res.data?.code === 200) {
      config.value = res.data.data || {}
    } else {
      ElMessage.error(res.data?.message || '获取加权配置失败')
    }
  } catch (error) {
    console.error('获取加权配置失败:', error)
    ElMessage.error('获取加权配置失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  visible.value = false
}

const getWeightModeType = (mode?: string) => {
  switch (mode) {
    case 'STANDARD': return 'primary'
    case 'PER_CAPITA': return 'success'
    case 'SEGMENT': return 'warning'
    case 'NONE': return 'info'
    default: return 'info'
  }
}

const getFactorTagType = (factor?: number) => {
  if (!factor) return 'info'
  if (factor < 0.8) return 'danger'
  if (factor < 1) return 'warning'
  if (factor === 1) return 'info'
  if (factor <= 1.2) return 'success'
  return 'primary'
}

const formatNumber = (num?: number) => {
  if (num === undefined || num === null) return '-'
  return Number(num).toFixed(2)
}
</script>

<style scoped>
.weight-config-detail {
  padding: 0 10px;
}

.info-card {
  margin-bottom: 16px;
}

.info-card :deep(.el-card__header) {
  padding: 12px 16px;
  background-color: #f5f7fa;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-header .tips {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.highlight-number {
  font-weight: bold;
  color: #409EFF;
}

.explanation-section {
  padding: 0 10px;
}

.explanation-item {
  margin-bottom: 16px;
}

.explanation-item:last-child {
  margin-bottom: 0;
}

.explanation-label {
  font-weight: bold;
  color: #606266;
  margin-bottom: 6px;
}

.explanation-content {
  color: #909399;
  line-height: 1.6;
  padding-left: 12px;
}

.explanation-content.formula {
  font-family: 'Consolas', 'Monaco', monospace;
  background-color: #f5f7fa;
  padding: 10px 12px;
  border-radius: 4px;
  color: #409EFF;
}

.explanation-content.example {
  background-color: #fdf6ec;
  padding: 10px 12px;
  border-radius: 4px;
}

.explanation-content.example pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', monospace;
  color: #e6a23c;
}

.text-warning {
  color: #e6a23c;
}

/* 多加权配置样式 */
.multi-config-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.multi-config-item {
  padding: 12px 16px;
  background-color: #fafafa;
  border-radius: 6px;
  border-left: 4px solid #1890ff;
}

.mc-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.mc-color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}

.mc-name {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

.mc-scope {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.mc-details {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
}

.mc-detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.mc-detail-item .label {
  color: #909399;
}

.mc-detail-item .sub-info {
  font-size: 12px;
  color: #c0c4cc;
}

.mc-categories {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.mc-categories .label {
  font-size: 13px;
  color: #909399;
}

.category-tag {
  margin-left: 4px;
}

.mc-formula {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.mc-formula .label {
  color: #909399;
  white-space: nowrap;
}

.mc-formula .formula-text {
  color: #409EFF;
  font-family: 'Consolas', 'Monaco', monospace;
}

.color-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
</style>
