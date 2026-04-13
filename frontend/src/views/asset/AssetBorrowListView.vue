<script setup lang="ts">
/**
 * 资产借用记录列表
 * UI优化版本 - 使用设计系统组件
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import {
  Package,
  User,
  Calendar,
  AlertTriangle,
  CheckCircle,
  Clock,
  RotateCcw,
  FileText,
  XCircle
} from 'lucide-vue-next'
import { assetBorrowApi } from '@/api/assetBorrow'
import type { AssetBorrow, BorrowStatistics } from '@/types/asset'
import { BorrowTypeMap, BorrowStatus } from '@/types/asset'
import AssetReturnDialog from './components/AssetReturnDialog.vue'

// 设计系统组件
import StatCard from '@/components/design-system/cards/StatCard.vue'
import EmptyState from '@/components/design-system/feedback/EmptyState.vue'

// 状态
const loading = ref(false)
const borrowList = ref<AssetBorrow[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const statistics = ref<BorrowStatistics>({
  borrowedCount: 0,
  overdueCount: 0,
  usedCount: 0
})

// 筛选
const activeStatus = ref<'all' | number>('all')
const borrowType = ref<number | undefined>(undefined)
const keyword = ref('')

// 对话框
const returnDialogVisible = ref(false)
const selectedBorrow = ref<AssetBorrow | null>(null)

// 状态标签选项
const statusTabs = computed(() => [
  { label: '全部', value: 'all', count: null },
  { label: '借出中', value: BorrowStatus.BORROWED, count: statistics.value.borrowedCount, color: 'text-blue-600' },
  { label: '已逾期', value: BorrowStatus.OVERDUE, count: statistics.value.overdueCount, color: 'text-red-600' },
  { label: '已归还', value: BorrowStatus.RETURNED, count: null, color: 'text-green-600' },
  { label: '已取消', value: BorrowStatus.CANCELLED, count: null, color: 'text-gray-600' }
])

// 加载借用列表
async function loadBorrowList() {
  loading.value = true
  try {
    const params = {
      borrowType: borrowType.value,
      status: activeStatus.value === 'all' ? undefined : activeStatus.value,
      keyword: keyword.value || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    const result = await assetBorrowApi.listBorrows(params)
    borrowList.value = result.records || result.list || []
    total.value = result.total || 0
  } catch (error) {
    console.error('Load borrow list failed:', error)
    ElMessage.error('加载借用记录失败')
  } finally {
    loading.value = false
  }
}

// 加载统计
async function loadStatistics() {
  try {
    statistics.value = await assetBorrowApi.getBorrowStatistics()
  } catch (error) {
    console.error('Load statistics failed:', error)
  }
}

// 状态切换
function handleStatusChange(status: 'all' | number) {
  activeStatus.value = status
  currentPage.value = 1
  loadBorrowList()
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadBorrowList()
}

// 分页
function handlePageChange(page: number) {
  currentPage.value = page
  loadBorrowList()
}

// 归还
function handleReturn(borrow: AssetBorrow) {
  selectedBorrow.value = borrow
  returnDialogVisible.value = true
}

// 归还成功
function handleReturnSuccess() {
  returnDialogVisible.value = false
  loadBorrowList()
  loadStatistics()
}

// 取消借用
async function handleCancel(borrow: AssetBorrow) {
  try {
    await ElMessageBox.confirm(
      `确定要取消借用单 "${borrow.borrowNo}" 吗？`,
      '取消确认',
      { type: 'warning' }
    )

    await assetBorrowApi.cancelBorrow(borrow.id)
    ElMessage.success('取消成功')
    loadBorrowList()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// 刷新
function refresh() {
  loadBorrowList()
  loadStatistics()
}

// 获取状态样式
function getStatusStyle(status: number) {
  switch (status) {
    case BorrowStatus.BORROWED:
      return { bg: 'bg-blue-500', text: 'text-blue-600', light: 'bg-blue-50' }
    case BorrowStatus.OVERDUE:
      return { bg: 'bg-red-500', text: 'text-red-600', light: 'bg-red-50' }
    case BorrowStatus.RETURNED:
      return { bg: 'bg-green-500', text: 'text-green-600', light: 'bg-green-50' }
    case BorrowStatus.CANCELLED:
      return { bg: 'bg-gray-400', text: 'text-gray-600', light: 'bg-gray-50' }
    default:
      return { bg: 'bg-gray-400', text: 'text-gray-600', light: 'bg-gray-50' }
  }
}

// 初始化
onMounted(() => {
  loadBorrowList()
  loadStatistics()
})
</script>

<template>
  <div class="min-h-full bg-gray-50/50 p-6">
    <!-- 页面标题 -->
    <div class="mb-8">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
          <Package class="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">借用管理</h1>
          <p class="text-sm text-gray-500">管理资产的借用和领用记录</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 - 使用设计系统 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <StatCard
        title="借出中"
        :value="statistics.borrowedCount"
        color="blue"
        subtitle="当前借出未归还的资产"
      >
        <template #icon><Package /></template>
      </StatCard>
      <StatCard
        title="已逾期"
        :value="statistics.overdueCount"
        color="orange"
        subtitle="超出归还日期的借用"
      >
        <template #icon><AlertTriangle /></template>
      </StatCard>
      <StatCard
        title="已领用"
        :value="statistics.usedCount"
        color="emerald"
        subtitle="领用后不归还的资产"
      >
        <template #icon><CheckCircle /></template>
      </StatCard>
    </div>

    <!-- 主内容区 -->
    <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <!-- 工具栏 -->
      <div class="p-4 border-b border-gray-100 bg-gray-50/50">
        <div class="flex items-center justify-between">
          <!-- 状态标签 -->
          <div class="flex items-center gap-1 bg-gray-100 rounded-lg p-1">
            <button
              v-for="tab in statusTabs"
              :key="tab.value"
              class="px-4 py-2 rounded-lg text-sm font-medium transition-all"
              :class="activeStatus === tab.value
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'"
              @click="handleStatusChange(tab.value)"
            >
              {{ tab.label }}
              <span v-if="tab.count !== null" class="ml-1.5 px-1.5 py-0.5 text-xs rounded-full" :class="activeStatus === tab.value ? 'bg-blue-100 text-blue-600' : 'bg-gray-200 text-gray-600'">
                {{ tab.count }}
              </span>
            </button>
          </div>

          <div class="flex items-center gap-3">
            <!-- 借用类型筛选 -->
            <el-select
              v-model="borrowType"
              placeholder="借用类型"
              clearable
              style="width: 130px"
              @change="handleSearch"
            >
              <el-option
                v-for="(label, value) in BorrowTypeMap"
                :key="value"
                :label="label"
                :value="Number(value)"
              />
            </el-select>

            <!-- 搜索框 -->
            <el-input
              v-model="keyword"
              placeholder="搜索单号/资产/借用人..."
              style="width: 220px"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon class="text-gray-400"><Search /></el-icon>
              </template>
            </el-input>

            <!-- 刷新按钮 -->
            <el-button :icon="Refresh" @click="refresh" class="!border-gray-200" />
          </div>
        </div>
      </div>

      <!-- 列表 -->
      <div class="p-4" v-loading="loading">
        <!-- 空状态 - 使用设计系统 -->
        <EmptyState
          v-if="borrowList.length === 0 && !loading"
          title="暂无借用记录"
          description="当前没有任何借用或领用记录"
        >
          <template #icon><Package /></template>
        </EmptyState>

        <!-- 借用记录卡片列表 -->
        <div v-else class="space-y-3">
          <div
            v-for="borrow in borrowList"
            :key="borrow.id"
            class="border border-gray-200 rounded-xl p-4 hover:border-blue-200 hover:shadow-md transition-all"
          >
            <div class="flex items-start justify-between">
              <!-- 左侧信息 -->
              <div class="flex-1">
                <div class="flex items-center gap-3 mb-3">
                  <div :class="[
                    'w-10 h-10 rounded-xl flex items-center justify-center shadow-sm',
                    borrow.status === BorrowStatus.BORROWED ? 'bg-gradient-to-br from-blue-400 to-blue-600' :
                    borrow.status === BorrowStatus.OVERDUE ? 'bg-gradient-to-br from-red-400 to-red-600' :
                    borrow.status === BorrowStatus.RETURNED ? 'bg-gradient-to-br from-emerald-400 to-emerald-600' :
                    'bg-gradient-to-br from-gray-400 to-gray-600'
                  ]">
                    <Package class="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="font-mono text-sm text-blue-600 bg-blue-50 px-2 py-0.5 rounded">{{ borrow.borrowNo }}</span>
                      <span
                        :class="[
                          'px-2.5 py-1 rounded-full text-xs font-medium',
                          getStatusStyle(borrow.status).light,
                          getStatusStyle(borrow.status).text
                        ]"
                      >
                        {{ borrow.statusDesc }}
                      </span>
                      <span class="px-2 py-0.5 bg-gray-100 rounded text-xs text-gray-600">
                        {{ borrow.borrowTypeDesc }}
                      </span>
                    </div>
                    <div class="font-semibold text-gray-900 mt-1">{{ borrow.assetName }}</div>
                  </div>
                </div>

                <div class="flex items-center flex-wrap gap-4 text-sm">
                  <div class="flex items-center gap-1.5 text-gray-500 bg-gray-50 px-2.5 py-1 rounded-lg">
                    <Package class="w-4 h-4" />
                    <span>{{ borrow.assetCode }}</span>
                  </div>
                  <div class="flex items-center gap-1.5 text-gray-500 bg-gray-50 px-2.5 py-1 rounded-lg">
                    <User class="w-4 h-4" />
                    <span>{{ borrow.borrowerName }}</span>
                    <span v-if="borrow.borrowerDept" class="text-gray-400">· {{ borrow.borrowerDept }}</span>
                  </div>
                </div>

                <div class="flex items-center flex-wrap gap-4 mt-3 text-sm text-gray-500">
                  <div class="flex items-center gap-1.5">
                    <Calendar class="w-4 h-4" />
                    借出：{{ borrow.borrowDate?.slice(0, 10) }}
                  </div>
                  <div v-if="borrow.expectedReturnDate" class="flex items-center gap-1.5">
                    <Clock class="w-4 h-4" />
                    <span :class="borrow.overdue ? 'text-red-600 font-semibold' : ''">
                      预计归还：{{ borrow.expectedReturnDate }}
                      <template v-if="borrow.overdue">
                        <span class="ml-1 px-1.5 py-0.5 bg-red-100 text-red-600 rounded text-xs">逾期{{ borrow.overdueDays }}天</span>
                      </template>
                      <template v-else-if="borrow.remainingDays !== null && borrow.remainingDays <= 3">
                        <span class="ml-1 px-1.5 py-0.5 bg-amber-100 text-amber-600 rounded text-xs">剩余{{ borrow.remainingDays }}天</span>
                      </template>
                    </span>
                  </div>
                  <div v-if="borrow.actualReturnDate" class="flex items-center gap-1.5 text-emerald-600">
                    <CheckCircle class="w-4 h-4" />
                    归还：{{ borrow.actualReturnDate?.slice(0, 10) }}
                  </div>
                </div>

                <div v-if="borrow.purpose" class="mt-3 text-sm text-gray-600 bg-gray-50 rounded-lg p-3">
                  <FileText class="w-4 h-4 inline-block mr-1 text-gray-400" />
                  {{ borrow.purpose }}
                </div>
              </div>

              <!-- 右侧操作按钮 -->
              <div class="flex flex-col items-end gap-2 ml-4">
                <template v-if="borrow.status === BorrowStatus.BORROWED || borrow.status === BorrowStatus.OVERDUE">
                  <el-button type="primary" size="small" @click="handleReturn(borrow)" class="!rounded-lg !bg-gradient-to-r !from-blue-500 !to-cyan-600 !border-none">
                    <RotateCcw class="w-4 h-4 mr-1" />
                    归还
                  </el-button>
                  <el-button size="small" @click="handleCancel(borrow)" class="!rounded-lg !border-gray-200">
                    <XCircle class="w-4 h-4 mr-1" />
                    取消
                  </el-button>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="flex justify-center mt-6">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 归还对话框 -->
    <AssetReturnDialog
      v-model:visible="returnDialogVisible"
      :borrow="selectedBorrow"
      @returned="handleReturnSuccess"
    />
  </div>
</template>

<style scoped>
.transition-shadow {
  transition: box-shadow 0.2s ease;
}
</style>
