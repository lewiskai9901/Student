<script setup lang="ts">
/**
 * 资产盘点视图
 * UI优化版本 - 使用设计系统组件
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  ClipboardList,
  Plus,
  CheckCircle,
  XCircle,
  Eye,
  ListChecks,
  Clock,
  TrendingUp,
  TrendingDown,
  Calendar
} from 'lucide-vue-next'
import {
  getInventories,
  getInventoryStatistics,
  completeInventory,
  cancelInventory
} from '@/api/assetInventory'
import type { AssetInventory, InventoryStatistics } from '@/types/asset'
import { InventoryStatus } from '@/types/asset'
import AssetInventoryCreateDialog from './components/AssetInventoryCreateDialog.vue'
import AssetInventoryDetailDialog from './components/AssetInventoryDetailDialog.vue'

// 设计系统组件
import StatCard from '@/components/design-system/cards/StatCard.vue'
import EmptyState from '@/components/design-system/feedback/EmptyState.vue'

// 状态
const loading = ref(false)
const inventoryList = ref<AssetInventory[]>([])
const statistics = ref<InventoryStatistics>({ inProgressCount: 0, completedCount: 0 })
const total = ref(0)

// 查询参数
const queryParams = reactive({
  status: undefined as number | undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

// 弹窗控制
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentInventoryId = ref<number | undefined>()

// 状态选项
const statusOptions = [
  { value: InventoryStatus.IN_PROGRESS, label: '进行中' },
  { value: InventoryStatus.COMPLETED, label: '已完成' },
  { value: InventoryStatus.CANCELLED, label: '已取消' }
]

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      getInventories(queryParams),
      getInventoryStatistics()
    ])
    inventoryList.value = listRes.records || []
    total.value = listRes.total || 0
    statistics.value = statsRes
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1
  loadData()
}

// 重置
function handleReset() {
  queryParams.status = undefined
  queryParams.keyword = ''
  queryParams.pageNum = 1
  loadData()
}

// 分页变化
function handlePageChange(page: number) {
  queryParams.pageNum = page
  loadData()
}

// 每页条数变化
function handleSizeChange(size: number) {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  loadData()
}

// 创建盘点
function handleCreate() {
  createDialogVisible.value = true
}

// 创建成功
function handleCreateSuccess() {
  createDialogVisible.value = false
  loadData()
  ElMessage.success('盘点任务创建成功')
}

// 查看详情
function handleView(inventory: AssetInventory) {
  currentInventoryId.value = inventory.id
  detailDialogVisible.value = true
}

// 完成盘点
async function handleComplete(inventory: AssetInventory) {
  try {
    await ElMessageBox.confirm(
      `确认完成盘点任务【${inventory.inventoryName}】？完成后将无法继续修改盘点数据。`,
      '确认完成',
      { type: 'warning' }
    )
    await completeInventory(inventory.id)
    ElMessage.success('盘点已完成')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 取消盘点
async function handleCancel(inventory: AssetInventory) {
  try {
    await ElMessageBox.confirm(
      `确认取消盘点任务【${inventory.inventoryName}】？取消后将无法恢复。`,
      '确认取消',
      { type: 'warning' }
    )
    await cancelInventory(inventory.id)
    ElMessage.success('盘点已取消')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 获取状态标签类型
function getStatusType(status: number) {
  switch (status) {
    case InventoryStatus.IN_PROGRESS:
      return 'warning'
    case InventoryStatus.COMPLETED:
      return 'success'
    case InventoryStatus.CANCELLED:
      return 'info'
    default:
      return 'info'
  }
}

// 进度条颜色
function getProgressColor(progress: number) {
  if (progress < 30) return '#f56c6c'
  if (progress < 70) return '#e6a23c'
  return '#67c23a'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="min-h-full bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-8">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center shadow-lg shadow-violet-500/20">
          <ClipboardList class="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">资产盘点</h1>
          <p class="text-sm text-gray-500">创建盘点任务，核查资产实际情况</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 - 使用设计系统 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <StatCard
        title="进行中"
        :value="statistics.inProgressCount"
        color="orange"
        subtitle="当前正在进行的盘点任务"
      >
        <template #icon><Clock /></template>
      </StatCard>
      <StatCard
        title="已完成"
        :value="statistics.completedCount"
        color="emerald"
        subtitle="已完成的盘点任务"
      >
        <template #icon><CheckCircle /></template>
      </StatCard>
      <StatCard
        title="总任务数"
        :value="total"
        color="purple"
        subtitle="累计创建的盘点任务"
      >
        <template #icon><ListChecks /></template>
      </StatCard>
    </div>

    <!-- 搜索和操作栏 -->
    <div class="bg-white rounded-xl border border-gray-200 p-4 mb-6 shadow-sm">
      <div class="flex flex-wrap justify-between items-center gap-4">
        <div class="flex flex-wrap items-center gap-3">
          <el-select
            v-model="queryParams.status"
            placeholder="状态筛选"
            clearable
            style="width: 130px"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索盘点编号/名称"
            style="width: 220px"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon class="text-gray-400"><Search /></el-icon>
            </template>
          </el-input>
          <el-button @click="handleSearch" class="!border-gray-200">搜索</el-button>
          <el-button @click="handleReset" class="!border-gray-200">重置</el-button>
        </div>
        <el-button type="primary" @click="handleCreate" class="!bg-gradient-to-r !from-violet-500 !to-purple-600 !border-none shadow-lg shadow-violet-500/25 hover:shadow-violet-500/40 transition-shadow">
          <Plus class="w-4 h-4 mr-1" />
          新建盘点
        </el-button>
      </div>
    </div>

    <!-- 盘点列表 -->
    <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <div class="p-4 border-b border-gray-100">
        <h3 class="text-base font-semibold text-gray-900">盘点任务列表</h3>
      </div>

      <div v-loading="loading" class="p-4">
        <!-- 有数据时显示卡片列表 -->
        <div v-if="inventoryList.length > 0" class="space-y-3">
          <div
            v-for="inventory in inventoryList"
            :key="inventory.id"
            class="border border-gray-200 rounded-xl p-4 hover:border-violet-200 hover:shadow-md transition-all"
          >
            <div class="flex items-start justify-between">
              <!-- 左侧信息 -->
              <div class="flex-1">
                <div class="flex items-center gap-3 mb-3">
                  <div :class="[
                    'w-10 h-10 rounded-xl flex items-center justify-center shadow-sm',
                    inventory.status === InventoryStatus.IN_PROGRESS ? 'bg-gradient-to-br from-amber-400 to-orange-500' :
                    inventory.status === InventoryStatus.COMPLETED ? 'bg-gradient-to-br from-emerald-400 to-green-600' :
                    'bg-gradient-to-br from-gray-400 to-gray-600'
                  ]">
                    <ClipboardList class="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="font-mono text-sm text-violet-600 bg-violet-50 px-2 py-0.5 rounded">{{ inventory.inventoryCode }}</span>
                      <span :class="[
                        'px-2.5 py-1 rounded-full text-xs font-medium',
                        inventory.status === InventoryStatus.IN_PROGRESS ? 'bg-amber-100 text-amber-700' :
                        inventory.status === InventoryStatus.COMPLETED ? 'bg-emerald-100 text-emerald-700' :
                        'bg-gray-100 text-gray-700'
                      ]">
                        {{ inventory.statusDesc }}
                      </span>
                      <span class="px-2 py-0.5 bg-gray-100 rounded text-xs text-gray-600">
                        {{ inventory.scopeTypeDesc }}
                      </span>
                    </div>
                    <div class="font-semibold text-gray-900 mt-1">{{ inventory.inventoryName }}</div>
                  </div>
                </div>

                <div class="flex items-center flex-wrap gap-4 text-sm">
                  <div class="flex items-center gap-1.5 text-gray-500 bg-gray-50 px-2.5 py-1 rounded-lg">
                    <Calendar class="w-4 h-4" />
                    {{ inventory.startDate }} ~ {{ inventory.endDate }}
                  </div>
                </div>

                <!-- 进度条 -->
                <div class="mt-3 flex items-center gap-3">
                  <div class="flex-1 max-w-xs">
                    <div class="flex items-center justify-between text-xs text-gray-500 mb-1">
                      <span>盘点进度</span>
                      <span>{{ inventory.checkedCount }}/{{ inventory.totalCount }}</span>
                    </div>
                    <el-progress
                      :percentage="inventory.progress"
                      :color="getProgressColor(inventory.progress)"
                      :stroke-width="8"
                      :show-text="false"
                    />
                  </div>
                  <div v-if="inventory.profitCount > 0 || inventory.lossCount > 0" class="flex items-center gap-3 text-sm">
                    <span v-if="inventory.profitCount > 0" class="flex items-center gap-1 text-emerald-600 bg-emerald-50 px-2 py-1 rounded">
                      <TrendingUp class="w-4 h-4" />
                      盘盈 {{ inventory.profitCount }}
                    </span>
                    <span v-if="inventory.lossCount > 0" class="flex items-center gap-1 text-red-600 bg-red-50 px-2 py-1 rounded">
                      <TrendingDown class="w-4 h-4" />
                      盘亏 {{ inventory.lossCount }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- 右侧操作按钮 -->
              <div class="flex flex-col items-end gap-2 ml-4">
                <el-button size="small" @click="handleView(inventory)" class="!rounded-lg !border-gray-200">
                  <Eye class="w-4 h-4 mr-1" />
                  详情
                </el-button>
                <template v-if="inventory.status === InventoryStatus.IN_PROGRESS">
                  <el-button type="success" size="small" @click="handleComplete(inventory)" class="!rounded-lg">
                    <CheckCircle class="w-4 h-4 mr-1" />
                    完成
                  </el-button>
                  <el-button type="danger" size="small" @click="handleCancel(inventory)" class="!rounded-lg">
                    <XCircle class="w-4 h-4 mr-1" />
                    取消
                  </el-button>
                </template>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div class="flex justify-end mt-6 pt-4 border-t border-gray-100">
            <el-pagination
              v-model:current-page="queryParams.pageNum"
              v-model:page-size="queryParams.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </div>

        <!-- 空状态 - 使用设计系统 -->
        <EmptyState
          v-else-if="!loading"
          title="暂无盘点记录"
          description='点击"新建盘点"按钮创建第一个盘点任务'
          action-text="新建盘点"
          @action="handleCreate"
        >
          <template #icon><ClipboardList /></template>
        </EmptyState>
      </div>
    </div>

    <!-- 创建盘点弹窗 -->
    <AssetInventoryCreateDialog
      v-model:visible="createDialogVisible"
      @success="handleCreateSuccess"
    />

    <!-- 盘点详情弹窗 -->
    <AssetInventoryDetailDialog
      v-model:visible="detailDialogVisible"
      :inventory-id="currentInventoryId"
      @updated="loadData"
    />
  </div>
</template>
