<template>
  <el-dialog
    :model-value="visible"
    title="汇总评级"
    width="900px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div class="summary-dialog">
      <!-- 配置区域 -->
      <div class="config-section">
        <el-form :inline="true" label-width="80px">
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              :default-value="defaultDateRange"
            />
          </el-form-item>
          <el-form-item label="评级规则">
            <el-select v-model="selectedRuleId" placeholder="全部规则" clearable style="width: 200px">
              <el-option
                v-for="rule in summaryRules"
                :key="rule.id"
                :label="rule.ruleName"
                :value="rule.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="calculating" @click="handleCalculate">
              <el-icon><DataAnalysis /></el-icon>
              计算汇总评级
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 结果展示区域 -->
      <div class="result-section" v-loading="loading">
        <template v-if="results.length > 0">
          <!-- 统计图表 -->
          <div class="chart-container" v-if="statistics">
            <div class="chart-title">评级分布</div>
            <div class="chart-content">
              <div class="pie-chart">
                <div
                  v-for="stat in statistics.levelStatistics"
                  :key="stat.levelId"
                  class="pie-segment"
                  :style="getPieStyle(stat)"
                >
                  <span class="segment-label">{{ stat.levelName }}</span>
                  <span class="segment-value">{{ stat.classCount }}个班</span>
                  <span class="segment-percent">{{ stat.percentage.toFixed(1) }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 结果列表 -->
          <div class="result-table">
            <el-table :data="results" stripe max-height="400">
              <el-table-column label="排名" width="80" align="center">
                <template #default="{ row }">
                  <span :class="['rank-number', getRankClass(row.ranking)]">{{ row.ranking }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="className" label="班级" min-width="150" />
              <el-table-column prop="gradeName" label="年级" width="120" />
              <el-table-column label="平均扣分" width="120" align="center">
                <template #default="{ row }">
                  <span class="score-text">{{ row.score.toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="评级" width="120" align="center">
                <template #default="{ row }">
                  <el-tag :color="row.levelColor" effect="dark" style="border: none">
                    {{ row.levelName }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="recordCount" label="检查次数" width="100" align="center" />
            </el-table>
          </div>

          <!-- 信息提示 -->
          <div class="info-bar">
            <el-icon><InfoFilled /></el-icon>
            <span>
              统计周期: {{ results[0]?.periodStart }} 至 {{ results[0]?.periodEnd }}，
              共 {{ results[0]?.recordCount || 0 }} 次检查，
              {{ results.length }} 个班级参与评级
            </span>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="请选择时间范围后点击计算">
          <template #image>
            <el-icon style="font-size: 60px; color: #c0c4cc"><DataAnalysis /></el-icon>
          </template>
        </el-empty>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">关闭</el-button>
      <el-button v-if="results.length > 0" type="primary" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出结果
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { DataAnalysis, InfoFilled, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getSummaryRatingResults,
  calculateSummaryRating,
  getRatingStatistics,
  exportRatingResults,
  type RatingRuleVO,
  type RatingResultVO,
  type RatingStatisticsVO
} from '@/api/rating'
import { downloadBlob, getFilenameFromContentDisposition } from '@/utils/export'

const props = defineProps<{
  visible: boolean
  checkPlanId: string | number
  planStartDate?: string
  planEndDate?: string
  rules: RatingRuleVO[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const loading = ref(false)
const calculating = ref(false)
const results = ref<RatingResultVO[]>([])
const statistics = ref<RatingStatisticsVO | null>(null)
const dateRange = ref<[string, string] | null>(null)
const selectedRuleId = ref<string | number | null>(null)

// 默认日期范围
const defaultDateRange = computed(() => {
  const start = props.planStartDate || new Date().toISOString().split('T')[0]
  const end = props.planEndDate || new Date().toISOString().split('T')[0]
  return [new Date(start), new Date(end)]
})

// 筛选SUMMARY类型的规则
const summaryRules = computed(() => {
  return props.rules.filter(r => r.ruleType === 'SUMMARY' && r.enabled)
})

// 计算汇总评级
const handleCalculate = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }

  calculating.value = true
  try {
    const [periodStart, periodEnd] = dateRange.value
    await calculateSummaryRating(
      props.checkPlanId,
      periodStart,
      periodEnd,
      selectedRuleId.value || undefined,
      true
    )

    // 加载结果
    await loadResults()
    ElMessage.success('计算完成')
  } catch (error) {
    console.error('计算失败:', error)
  } finally {
    calculating.value = false
  }
}

// 加载结果
const loadResults = async () => {
  if (!dateRange.value) return

  loading.value = true
  try {
    const [periodStart, periodEnd] = dateRange.value
    const res = await getSummaryRatingResults(props.checkPlanId, periodStart, periodEnd)
    let data = res || []

    // 如果选择了特定规则，过滤结果
    if (selectedRuleId.value) {
      data = data.filter(r => String(r.ruleId) === String(selectedRuleId.value))

      // 加载统计
      if (data.length > 0) {
        const statsRes = await getRatingStatistics(
          selectedRuleId.value,
          undefined,
          periodStart,
          periodEnd
        )
        statistics.value = statsRes
      }
    } else if (data.length > 0) {
      // 获取第一个规则的统计
      const firstRuleId = data[0].ruleId
      const statsRes = await getRatingStatistics(
        firstRuleId,
        undefined,
        periodStart,
        periodEnd
      )
      statistics.value = statsRes
    }

    results.value = data
  } catch (error) {
    console.error('加载结果失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取排名样式
const getRankClass = (rank: number) => {
  if (rank === 1) return 'rank-gold'
  if (rank === 2) return 'rank-silver'
  if (rank === 3) return 'rank-bronze'
  return ''
}

// 获取饼图样式
const getPieStyle = (stat: any) => {
  return {
    backgroundColor: stat.levelColor || '#909399',
    flex: stat.percentage
  }
}

// 导出结果
const handleExport = async () => {
  if (!results.value || results.value.length === 0) {
    ElMessage.warning('无数据可导出，请先计算评级')
    return
  }

  try {
    ElMessage.info('正在导出...')
    const [periodStart, periodEnd] = dateRange.value || []
    const response = await exportRatingResults({
      checkPlanId: props.checkPlanId,
      ruleId: selectedRuleId.value || undefined,
      periodStart,
      periodEnd,
      format: 'EXCEL'
    })

    // 处理blob响应
    let blob: Blob
    let filename = `汇总评级结果_${new Date().toISOString().slice(0, 10)}.xlsx`

    if (response && (response as any).data instanceof Blob) {
      blob = (response as any).data
      const contentDisposition = (response as any).headers?.['content-disposition']
      if (contentDisposition) {
        filename = getFilenameFromContentDisposition(contentDisposition)
      }
    } else if (response instanceof Blob) {
      blob = response
    } else {
      throw new Error('导出响应格式错误')
    }

    downloadBlob(blob, filename)
    ElMessage.success('导出成功')
  } catch (error: any) {
    console.error('导出失败:', error)
    ElMessage.error(error.message || '导出失败')
  }
}

// 监听对话框打开
watch(() => props.visible, (val) => {
  if (val) {
    // 设置默认日期范围
    dateRange.value = [
      props.planStartDate || new Date().toISOString().split('T')[0],
      props.planEndDate || new Date().toISOString().split('T')[0]
    ]
    results.value = []
    statistics.value = null
    selectedRuleId.value = null
  }
})
</script>

<style scoped>
.summary-dialog {
  min-height: 400px;
}

.config-section {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.result-section {
  min-height: 300px;
}

.chart-container {
  margin-bottom: 20px;
}

.chart-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}

.chart-content {
  display: flex;
  justify-content: center;
}

.pie-chart {
  display: flex;
  width: 100%;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
}

.pie-segment {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 12px;
  color: #fff;
  font-size: 12px;
  min-width: 80px;
}

.segment-label {
  font-weight: 500;
}

.segment-value {
  opacity: 0.9;
}

.segment-percent {
  opacity: 0.8;
}

.result-table {
  margin-bottom: 16px;
}

.rank-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-weight: 600;
  font-size: 13px;
}

.rank-number.rank-gold {
  background: linear-gradient(135deg, #ffd700, #ffb800);
  color: #fff;
}

.rank-number.rank-silver {
  background: linear-gradient(135deg, #c0c0c0, #a8a8a8);
  color: #fff;
}

.rank-number.rank-bronze {
  background: linear-gradient(135deg, #cd7f32, #b8702c);
  color: #fff;
}

.score-text {
  color: #f56c6c;
  font-weight: 500;
}

.info-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #ecf5ff;
  border-radius: 6px;
  color: #409eff;
  font-size: 13px;
}
</style>
