<script setup lang="ts">
/**
 * 资产折旧管理视图
 * UI优化版本 - 使用设计系统组件
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { Calendar, FileCheck, Coins, History, Calculator } from 'lucide-vue-next'
import {
  assetDepreciationApi,
  type AssetDepreciationDTO,
  DepreciationMethodMap
} from '@/api/v2/assetDepreciation'

// 设计系统组件
import StatCard from '@/components/design-system/cards/StatCard.vue'
import EmptyState from '@/components/design-system/feedback/EmptyState.vue'

// ============ 状态 ============
const loading = ref(false)
const currentPeriod = ref(getCurrentPeriod())
const periodRecords = ref<AssetDepreciationDTO[]>([])
const periodSummary = ref({
  assetCount: 0,
  totalDepreciation: 0
})

// 历史记录对话框
const historyDialogVisible = ref(false)
const historyAssetId = ref<number | null>(null)
const historyLoading = ref(false)
const historyRecords = ref<AssetDepreciationDTO[]>([])
const historyTotal = ref(0)
const historyPage = ref(1)
const historyPageSize = ref(10)

// ============ 计算属性 ============
const periodOptions = computed(() => {
  const options = []
  const now = new Date()
  for (let i = 0; i < 12; i++) {
    const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const period = formatPeriod(date)
    options.push({
      value: period,
      label: formatPeriodLabel(period)
    })
  }
  return options
})

// ============ 方法 ============
function getCurrentPeriod(): string {
  const now = new Date()
  return formatPeriod(now)
}

function formatPeriod(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${year}${month}`
}

function formatPeriodLabel(period: string): string {
  const year = period.substring(0, 4)
  const month = period.substring(4, 6)
  return `${year}年${month}月`
}

async function loadPeriodData() {
  loading.value = true
  try {
    const res = await assetDepreciationApi.getPeriodSummary(currentPeriod.value)
    periodRecords.value = res.records || []
    periodSummary.value = {
      assetCount: res.assetCount || 0,
      totalDepreciation: res.totalDepreciation || 0
    }
  } catch (error) {
    console.error('Failed to load period data:', error)
  } finally {
    loading.value = false
  }
}

async function handleCalculateAll() {
  try {
    await ElMessageBox.confirm(
      `确定要计提 ${formatPeriodLabel(currentPeriod.value)} 的所有资产折旧吗？`,
      '批量计提确认',
      { type: 'warning' }
    )

    loading.value = true
    const res = await assetDepreciationApi.calculateAllDepreciation(currentPeriod.value)
    ElMessage.success(`计提完成，处理了 ${res.processedCount} 项资产`)
    loadPeriodData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('计提失败')
    }
  } finally {
    loading.value = false
  }
}

function handleViewHistory(assetId: number) {
  historyAssetId.value = assetId
  historyPage.value = 1
  historyDialogVisible.value = true
  loadHistory()
}

async function loadHistory() {
  if (!historyAssetId.value) return

  historyLoading.value = true
  try {
    const res = await assetDepreciationApi.getDepreciationHistoryPage(
      historyAssetId.value,
      historyPage.value,
      historyPageSize.value
    )
    historyRecords.value = res.records || []
    historyTotal.value = res.total || 0
  } catch (error) {
    console.error('Failed to load history:', error)
  } finally {
    historyLoading.value = false
  }
}

function handleHistoryPageChange(page: number) {
  historyPage.value = page
  loadHistory()
}

function formatMoney(value: number | undefined): string {
  if (value === undefined || value === null) return '-'
  return value.toLocaleString('zh-CN', {
    style: 'currency',
    currency: 'CNY'
  })
}

// ============ 生命周期 ============
onMounted(() => {
  loadPeriodData()
})
</script>

<template>
  <div class="min-h-full bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-8">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
          <Calculator class="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">折旧管理</h1>
          <p class="text-sm text-gray-500">管理固定资产折旧计提</p>
        </div>
      </div>
    </div>

    <!-- 汇总卡片 - 使用设计系统 StatCard -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <StatCard
        title="当前期间"
        :value="formatPeriodLabel(currentPeriod)"
        color="blue"
        subtitle="点击选择其他期间"
        clickable
        @click="() => {}"
      >
        <template #icon><Calendar /></template>
      </StatCard>
      <StatCard
        title="已计提资产数"
        :value="periodSummary.assetCount"
        color="emerald"
        :subtitle="`共 ${periodSummary.assetCount} 项资产已计提`"
      >
        <template #icon><FileCheck /></template>
      </StatCard>
      <StatCard
        title="本期折旧总额"
        :value="formatMoney(periodSummary.totalDepreciation)"
        color="orange"
        subtitle="本期累计折旧金额"
      >
        <template #icon><Coins /></template>
      </StatCard>
    </div>

    <!-- 操作栏 - 优化样式 -->
    <div class="bg-white rounded-xl border border-gray-200 p-4 mb-6 shadow-sm">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <span class="text-sm font-medium text-gray-600">选择期间</span>
            <el-select
              v-model="currentPeriod"
              placeholder="选择期间"
              @change="loadPeriodData"
              class="!w-[160px]"
            >
              <el-option
                v-for="opt in periodOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <el-button :icon="Refresh" @click="loadPeriodData" class="!border-gray-200">
            刷新
          </el-button>
          <el-button type="primary" @click="handleCalculateAll" class="!bg-gradient-to-r !from-blue-500 !to-indigo-600 !border-none shadow-lg shadow-blue-500/25 hover:shadow-blue-500/40 transition-shadow">
            <Calculator class="w-4 h-4 mr-1" />
            批量计提折旧
          </el-button>
        </div>
      </div>
    </div>

    <!-- 折旧记录列表 - 优化样式 -->
    <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <div class="p-4 border-b border-gray-100">
        <h3 class="text-base font-semibold text-gray-900">折旧记录明细</h3>
      </div>

      <div v-loading="loading" class="p-4">
        <!-- 有数据时显示表格 -->
        <el-table v-if="periodRecords.length > 0" :data="periodRecords" stripe class="modern-table">
          <el-table-column prop="assetCode" label="资产编码" width="140">
            <template #default="{ row }">
              <span class="font-mono text-sm text-blue-600 bg-blue-50 px-2 py-0.5 rounded">{{ row.assetCode }}</span>
            </template>
          </el-table-column>
          <el-table-column label="折旧方法" width="120">
            <template #default="{ row }">
              <el-tag type="info" size="small">
                {{ DepreciationMethodMap[row.depreciationMethod] || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="期初原值" width="120" align="right">
            <template #default="{ row }">
              <span class="text-gray-600">{{ formatMoney(row.beginningValue) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="期初累计折旧" width="130" align="right">
            <template #default="{ row }">
              <span class="text-gray-600">{{ formatMoney(row.beginningAccumulatedDepreciation) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="期初净值" width="120" align="right">
            <template #default="{ row }">
              <span class="text-gray-900 font-medium">{{ formatMoney(row.beginningNetValue) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="本期折旧" width="120" align="right">
            <template #default="{ row }">
              <span class="text-red-500 font-semibold">
                {{ formatMoney(row.depreciationAmount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="期末净值" width="120" align="right">
            <template #default="{ row }">
              <span class="text-emerald-600 font-medium">{{ formatMoney(row.endingNetValue) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="使用进度" width="140" align="center">
            <template #default="{ row }">
              <div class="flex flex-col items-center gap-1">
                <el-progress
                  :percentage="Math.round((row.usedMonths / (row.usedMonths + row.remainingMonths)) * 100)"
                  :stroke-width="6"
                  :show-text="false"
                  class="!w-20"
                />
                <span class="text-xs text-gray-500">{{ row.usedMonths }}月 / {{ row.usedMonths + row.remainingMonths }}月</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="depreciationDate" label="计提日期" width="110">
            <template #default="{ row }">
              <span class="text-sm text-gray-500">{{ row.depreciationDate }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="handleViewHistory(row.assetId)">
                <History class="w-4 h-4 mr-1" />
                历史
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 空状态 - 使用设计系统组件 -->
        <EmptyState
          v-else-if="!loading"
          title="本期暂无折旧记录"
          description='点击"批量计提折旧"按钮开始计提本期折旧'
          action-text="批量计提"
          @action="handleCalculateAll"
        >
          <template #icon><Calculator /></template>
        </EmptyState>
      </div>
    </div>

    <!-- 折旧历史对话框 - 优化样式 -->
    <el-dialog
      v-model="historyDialogVisible"
      title="折旧历史记录"
      width="950px"
      class="modern-dialog"
    >
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center">
            <History class="w-4 h-4 text-white" />
          </div>
          <div>
            <h3 class="text-lg font-semibold text-gray-900">折旧历史记录</h3>
            <p class="text-sm text-gray-500">查看资产历史折旧明细</p>
          </div>
        </div>
      </template>

      <el-table v-loading="historyLoading" :data="historyRecords" stripe size="small" class="modern-table">
        <el-table-column prop="depreciationPeriod" label="期间" width="100">
          <template #default="{ row }">
            <span class="text-sm font-medium text-gray-700">{{ formatPeriodLabel(row.depreciationPeriod) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="期初净值" width="110" align="right">
          <template #default="{ row }">
            <span class="text-gray-600">{{ formatMoney(row.beginningNetValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本期折旧" width="110" align="right">
          <template #default="{ row }">
            <span class="text-red-500 font-medium">{{ formatMoney(row.depreciationAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计折旧" width="110" align="right">
          <template #default="{ row }">
            <span class="text-orange-600">{{ formatMoney(row.endingAccumulatedDepreciation) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="期末净值" width="110" align="right">
          <template #default="{ row }">
            <span class="text-emerald-600 font-medium">{{ formatMoney(row.endingNetValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用进度" width="130" align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round((row.usedMonths / (row.usedMonths + row.remainingMonths)) * 100)"
              :stroke-width="6"
              :show-text="false"
              class="!w-16"
            />
          </template>
        </el-table-column>
        <el-table-column label="已用/剩余" width="100" align="center">
          <template #default="{ row }">
            <span class="text-xs text-gray-500">{{ row.usedMonths }} / {{ row.remainingMonths }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="depreciationDate" label="计提日期" width="110">
          <template #default="{ row }">
            <span class="text-sm text-gray-500">{{ row.depreciationDate }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="historyTotal > historyPageSize" class="mt-6 flex justify-center">
        <el-pagination
          v-model:current-page="historyPage"
          :page-size="historyPageSize"
          :total="historyTotal"
          layout="prev, pager, next"
          @current-change="handleHistoryPageChange"
        />
      </div>

      <template #footer>
        <el-button @click="historyDialogVisible = false" class="!px-6">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 现代表格样式 */
.modern-table :deep(.el-table__header th) {
  background: #f8fafc !important;
  font-weight: 600;
  color: #475569;
  font-size: 0.8125rem;
}

.modern-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.modern-table :deep(.el-table__row:hover > td) {
  background: #f1f5f9 !important;
}

/* 对话框样式 */
.modern-dialog :deep(.el-dialog__header) {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 0;
}

.modern-dialog :deep(.el-dialog__body) {
  padding: 1.5rem;
}

.modern-dialog :deep(.el-dialog__footer) {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
}
</style>
