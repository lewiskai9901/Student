<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Box,
  Search,
  Plus,
  Position,
  Refresh,
  Grid,
  List,
  Folder,
  DocumentAdd,
  Upload,
  Download,
  Printer
} from '@element-plus/icons-vue'
import { assetApi } from '@/api/asset'
import type {
  Asset,
  AssetCategory,
  AssetStatistics,
  AssetQueryCriteria,
  BatchCreateResult,
  BatchTransferResult,
  ImportResult
} from '@/types/asset'
import { AssetStatus, ManagementMode, ManagementModeMap } from '@/types/asset'
import AssetFormDialog from './components/AssetFormDialog.vue'
import AssetTransferDialog from './components/AssetTransferDialog.vue'
import AssetDetailDrawer from './components/AssetDetailDrawer.vue'
import AssetBatchCreateDialog from './components/AssetBatchCreateDialog.vue'
import AssetBatchTransferDialog from './components/AssetBatchTransferDialog.vue'
import AssetImportDialog from './components/AssetImportDialog.vue'
import AssetBorrowDialog from './components/AssetBorrowDialog.vue'
import AssetMaintenanceDialog from './components/AssetMaintenanceDialog.vue'
import AssetLabelDialog from '@/components/asset/AssetLabelDialog.vue'

// ============ 状态 ============
const loading = ref(false)
const categoryLoading = ref(false)

// 分类数据
const categoryTree = ref<AssetCategory[]>([])
const selectedCategory = ref<AssetCategory | null>(null)
const expandedCategoryIds = ref<number[]>([])

// 资产数据
const assetList = ref<Asset[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = computed(() => viewDensity.value === 'compact' ? 100 : 20)

// 统计数据
const statistics = ref<AssetStatistics>({
  totalCount: 0,
  inUseCount: 0,
  idleCount: 0,
  repairingCount: 0,
  scrappedCount: 0
})

// 视图模式：table | card
const viewMode = ref<'table' | 'card'>('card')

// 视图密度：normal | compact
const viewDensity = ref<'normal' | 'compact'>('normal')

// 监听视图密度变化，重新加载数据
watch(viewDensity, () => {
  currentPage.value = 1
  loadAssetList()
})

// 状态筛选
const activeStatus = ref<'all' | number>('all')

// 搜索关键词
const keyword = ref('')

// 对话框控制
const formDialogVisible = ref(false)
const editingAsset = ref<Asset | null>(null)
const transferDialogVisible = ref(false)
const transferringAsset = ref<Asset | null>(null)
const detailDrawerVisible = ref(false)
const selectedAsset = ref<Asset | null>(null)
const batchCreateDialogVisible = ref(false)
const batchTransferDialogVisible = ref(false)
const importDialogVisible = ref(false)
const exportLoading = ref(false)
const selectedAssets = ref<Asset[]>([])
const borrowDialogVisible = ref(false)
const borrowingAsset = ref<Asset | null>(null)
const maintenanceDialogVisible = ref(false)
const maintenanceAsset = ref<Asset | null>(null)

// 标签打印
const labelDialogVisible = ref(false)
const labelAssets = ref<Array<{ id: number; assetCode: string; assetName: string; location?: string }>>([])

// ============ 计算属性 ============

// 状态标签选项
const statusTabs = computed(() => [
  { label: '全部', value: 'all', count: statistics.value.totalCount },
  { label: '在用', value: AssetStatus.IN_USE, count: statistics.value.inUseCount, color: 'text-green-600' },
  { label: '闲置', value: AssetStatus.IDLE, count: statistics.value.idleCount, color: 'text-gray-600' },
  { label: '维修', value: AssetStatus.REPAIRING, count: statistics.value.repairingCount, color: 'text-amber-600' },
  { label: '报废', value: AssetStatus.SCRAPPED, count: statistics.value.scrappedCount, color: 'text-red-600' }
])

// ============ 方法 ============

// 加载分类树
async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const res = await assetApi.getCategoryTree()
    categoryTree.value = res || []
    // 默认展开第一级
    if (categoryTree.value.length > 0) {
      expandedCategoryIds.value = categoryTree.value.map(c => c.id)
    }
  } catch (error) {
    console.error('Failed to load category tree:', error)
  } finally {
    categoryLoading.value = false
  }
}

// 加载资产列表
async function loadAssetList() {
  loading.value = true
  try {
    const params: AssetQueryCriteria = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: activeStatus.value === 'all' ? undefined : activeStatus.value,
      categoryId: selectedCategory.value?.id ?? undefined
    }
    const res = await assetApi.getAssetList(params)
    assetList.value = res?.records || []
    total.value = res?.total || 0
  } catch (error) {
    console.error('Failed to load asset list:', error)
  } finally {
    loading.value = false
  }
}

// 加载统计数据
async function loadStatistics() {
  try {
    const res = await assetApi.getAssetStatistics()
    statistics.value = res || statistics.value
  } catch (error) {
    console.error('Failed to load statistics:', error)
  }
}

// 选择分类
function handleCategoryClick(category: AssetCategory | null) {
  selectedCategory.value = category
  currentPage.value = 1
  loadAssetList()
}

// 切换分类展开
function toggleCategory(categoryId: number) {
  const idx = expandedCategoryIds.value.indexOf(categoryId)
  if (idx >= 0) {
    expandedCategoryIds.value.splice(idx, 1)
  } else {
    expandedCategoryIds.value.push(categoryId)
  }
}

// 状态筛选变化
function handleStatusChange(status: 'all' | number) {
  activeStatus.value = status
  currentPage.value = 1
  // 切换状态时清除分类筛选，让用户看到该状态下的所有资产
  if (selectedCategory.value !== null) {
    selectedCategory.value = null
  }
  loadAssetList()
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadAssetList()
}

// 新建资产
function handleCreate() {
  editingAsset.value = null
  formDialogVisible.value = true
}

// 批量入库
function handleBatchCreate() {
  batchCreateDialogVisible.value = true
}

// 下拉菜单命令处理
function handleDropdownCommand(command: string) {
  switch (command) {
    case 'batch':
      handleBatchCreate()
      break
    case 'import':
      handleImport()
      break
  }
}

// 批量入库成功
function handleBatchCreateSaved(result: BatchCreateResult) {
  batchCreateDialogVisible.value = false
  loadAssetList()
  loadStatistics()
  loadCategoryTree()
}

// 批量调拨
function handleBatchTransfer() {
  if (selectedAssets.value.length === 0) {
    ElMessage.warning('请先选择要调拨的资产')
    return
  }
  // 检查是否有不能调拨的资产
  const invalidAssets = selectedAssets.value.filter(a =>
    a.status === AssetStatus.SCRAPPED || a.status === AssetStatus.REPAIRING
  )
  if (invalidAssets.length > 0) {
    ElMessage.warning(`有 ${invalidAssets.length} 件资产状态不允许调拨，请重新选择`)
    return
  }
  batchTransferDialogVisible.value = true
}

// 批量调拨成功
function handleBatchTransferSaved(result: BatchTransferResult) {
  batchTransferDialogVisible.value = false
  selectedAssets.value = []
  loadAssetList()
}

// 打开导入对话框
function handleImport() {
  importDialogVisible.value = true
}

// 导入成功
function handleImportSaved(result: ImportResult) {
  importDialogVisible.value = false
  loadAssetList()
  loadStatistics()
}

// 导出资产
async function handleExport() {
  try {
    exportLoading.value = true

    // 构建查询参数（与当前筛选条件一致）
    const params: AssetQueryCriteria = {
      keyword: keyword.value || undefined,
      status: activeStatus.value === 'all' ? undefined : activeStatus.value,
      categoryId: selectedCategory.value?.id ?? undefined
    }

    const response = await assetApi.exportAssets(params)

    // 创建下载链接
    const blob = new Blob([response as unknown as BlobPart], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    // 生成文件名
    const timestamp = new Date().toISOString().slice(0, 10)
    link.download = `资产列表_${timestamp}.xlsx`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('Export failed:', error)
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

// 选择资产
function handleSelectionChange(selection: Asset[]) {
  selectedAssets.value = selection
}

// 清空选择
function clearSelection() {
  selectedAssets.value = []
}

// 编辑资产
function handleEdit(asset: Asset) {
  editingAsset.value = asset
  formDialogVisible.value = true
}

// 查看详情
function handleViewDetail(asset: Asset) {
  selectedAsset.value = asset
  detailDrawerVisible.value = true
}

// 调拨资产
function handleTransfer(asset: Asset) {
  if (asset.status === AssetStatus.SCRAPPED) {
    ElMessage.warning('已报废资产不能调拨')
    return
  }
  if (asset.status === AssetStatus.REPAIRING) {
    ElMessage.warning('维修中的资产不能调拨')
    return
  }
  transferringAsset.value = asset
  transferDialogVisible.value = true
}

// 报废资产
async function handleScrap(asset: Asset) {
  if (asset.status === AssetStatus.SCRAPPED) {
    ElMessage.warning('该资产已报废')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要报废资产 "${asset.assetName}" 吗？`,
      '报废确认',
      { type: 'warning' }
    )

    await assetApi.scrapAsset(asset.id, { reason: '用户手动报废' })
    ElMessage.success('报废成功')
    loadAssetList()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('报废失败')
    }
  }
}

// 删除资产
async function handleDelete(asset: Asset) {
  if (asset.status === AssetStatus.IN_USE) {
    ElMessage.warning('在用资产不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除资产 "${asset.assetName}" 吗？`,
      '删除确认',
      { type: 'warning' }
    )

    await assetApi.deleteAsset(asset.id)
    ElMessage.success('删除成功')
    loadAssetList()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 表单保存成功
function handleFormSaved() {
  formDialogVisible.value = false
  loadAssetList()
  loadStatistics()
  loadCategoryTree()
}

// 调拨成功
function handleTransferSaved() {
  transferDialogVisible.value = false
  loadAssetList()
}

// 借用资产
function handleBorrow(asset: Asset) {
  if (asset.status === AssetStatus.SCRAPPED) {
    ElMessage.warning('已报废资产不能借用')
    return
  }
  if (asset.status === AssetStatus.REPAIRING) {
    ElMessage.warning('维修中的资产不能借用')
    return
  }
  borrowingAsset.value = asset
  borrowDialogVisible.value = true
}

// 借用成功
function handleBorrowSaved() {
  borrowDialogVisible.value = false
  loadAssetList()
  loadStatistics()
}

// 维修资产
function handleMaintenance(asset: Asset) {
  if (asset.status === AssetStatus.SCRAPPED) {
    ElMessage.warning('已报废资产不能维修')
    return
  }
  if (asset.status === AssetStatus.REPAIRING) {
    ElMessage.warning('该资产已在维修中')
    return
  }
  maintenanceAsset.value = asset
  maintenanceDialogVisible.value = true
}

// 维修记录创建成功
function handleMaintenanceSaved() {
  maintenanceDialogVisible.value = false
  loadAssetList()
  loadStatistics()
}

// 打印单个资产标签
function handlePrintLabel(asset: Asset) {
  labelAssets.value = [{
    id: asset.id,
    assetCode: asset.assetCode,
    assetName: asset.assetName,
    location: asset.locationName
  }]
  labelDialogVisible.value = true
}

// 批量打印标签
function handleBatchPrintLabels() {
  if (selectedAssets.value.length === 0) {
    ElMessage.warning('请先选择要打印的资产')
    return
  }
  labelAssets.value = selectedAssets.value.map(asset => ({
    id: asset.id,
    assetCode: asset.assetCode,
    assetName: asset.assetName,
    location: asset.locationName
  }))
  labelDialogVisible.value = true
}

// 分页变化
function handlePageChange(page: number) {
  currentPage.value = page
  loadAssetList()
}

// 获取状态样式
function getStatusStyle(status: number) {
  switch (status) {
    case AssetStatus.IN_USE:
      return { bg: 'bg-green-500', text: 'text-green-600', light: 'bg-green-50' }
    case AssetStatus.IDLE:
      return { bg: 'bg-gray-400', text: 'text-gray-600', light: 'bg-gray-50' }
    case AssetStatus.REPAIRING:
      return { bg: 'bg-amber-500', text: 'text-amber-600', light: 'bg-amber-50' }
    case AssetStatus.SCRAPPED:
      return { bg: 'bg-red-500', text: 'text-red-600', light: 'bg-red-50' }
    default:
      return { bg: 'bg-gray-400', text: 'text-gray-600', light: 'bg-gray-50' }
  }
}

// 刷新
function refresh() {
  loadCategoryTree()
  loadAssetList()
  loadStatistics()
}

// ============ 生命周期 ============
onMounted(() => {
  loadCategoryTree()
  loadAssetList()
  loadStatistics()
})
</script>

<template>
  <div class="flex h-full bg-gray-50">
    <!-- 左侧分类面板 -->
    <div class="w-64 flex-shrink-0 border-r border-gray-200 bg-white flex flex-col">
      <!-- 标题 -->
      <div class="p-4 border-b border-gray-100">
        <div class="flex items-center justify-between">
          <h2 class="text-base font-semibold text-gray-900">资产分类</h2>
          <el-button text size="small" :icon="Refresh" @click="loadCategoryTree" />
        </div>
      </div>

      <!-- 分类列表 -->
      <div class="flex-1 overflow-y-auto py-2" v-loading="categoryLoading">
        <!-- 全部资产 -->
        <div
          class="mx-2 px-3 py-2.5 rounded-lg cursor-pointer flex items-center justify-between transition-colors"
          :class="selectedCategory === null
            ? 'bg-blue-50 text-blue-700'
            : 'hover:bg-gray-50 text-gray-700'"
          @click="handleCategoryClick(null)"
        >
          <span class="flex items-center gap-2">
            <el-icon><Box /></el-icon>
            <span class="font-medium">全部资产</span>
          </span>
          <span class="text-xs px-2 py-0.5 rounded-full"
            :class="selectedCategory === null ? 'bg-blue-100' : 'bg-gray-100'">
            {{ statistics.totalCount }}
          </span>
        </div>

        <!-- 分类树 -->
        <div class="mt-2">
          <template v-for="category in categoryTree" :key="category.id">
            <div
              class="mx-2 px-3 py-2 rounded-lg cursor-pointer flex items-center justify-between transition-colors"
              :class="selectedCategory?.id === category.id
                ? 'bg-blue-50 text-blue-700'
                : 'hover:bg-gray-50 text-gray-700'"
              @click="handleCategoryClick(category)"
            >
              <span class="flex items-center gap-2">
                <el-icon
                  v-if="category.children?.length"
                  class="text-gray-400 transition-transform cursor-pointer"
                  :class="{ 'rotate-90': expandedCategoryIds.includes(category.id) }"
                  @click.stop="toggleCategory(category.id)"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 6l6 6-6 6" />
                  </svg>
                </el-icon>
                <el-icon v-else class="text-gray-300"><Folder /></el-icon>
                <span class="text-sm">{{ category.categoryName }}</span>
              </span>
              <span class="text-xs text-gray-400">{{ category.assetCount || 0 }}</span>
            </div>

            <!-- 子分类 -->
            <template v-if="category.children?.length && expandedCategoryIds.includes(category.id)">
              <div
                v-for="child in category.children"
                :key="child.id"
                class="mx-2 ml-8 px-3 py-2 rounded-lg cursor-pointer flex items-center justify-between transition-colors"
                :class="selectedCategory?.id === child.id
                  ? 'bg-blue-50 text-blue-700'
                  : 'hover:bg-gray-50 text-gray-600'"
                @click="handleCategoryClick(child)"
              >
                <span class="flex items-center gap-2">
                  <el-icon class="text-gray-300"><Folder /></el-icon>
                  <span class="text-sm">{{ child.categoryName }}</span>
                </span>
                <span class="text-xs text-gray-400">{{ child.assetCount || 0 }}</span>
              </div>
            </template>
          </template>
        </div>
      </div>
    </div>

    <!-- 右侧内容区 -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <!-- 顶部操作栏 -->
      <div class="bg-white border-b border-gray-200">
        <!-- 状态标签 + 搜索 + 操作 -->
        <div class="px-4 py-3 flex items-center gap-4">
          <!-- 状态标签 -->
          <div class="flex items-center gap-1 bg-gray-100 rounded-lg p-1">
            <button
              v-for="tab in statusTabs"
              :key="tab.value"
              class="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              :class="activeStatus === tab.value
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'"
              @click="handleStatusChange(tab.value)"
            >
              {{ tab.label }}
              <span class="ml-1 text-xs" :class="tab.color || 'text-gray-400'">{{ tab.count }}</span>
            </button>
          </div>

          <div class="flex-1" />

          <!-- 搜索框 -->
          <el-input
            v-model="keyword"
            placeholder="搜索资产..."
            style="width: 220px"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon class="text-gray-400"><Search /></el-icon>
            </template>
          </el-input>

          <!-- 视图切换 -->
          <div class="flex items-center gap-2">
            <!-- 视图模式：卡片/表格 -->
            <div class="flex items-center border border-gray-200 rounded-lg overflow-hidden">
              <button
                class="p-2 transition-colors"
                :class="viewMode === 'card' ? 'bg-gray-100 text-gray-900' : 'text-gray-400 hover:text-gray-600'"
                @click="viewMode = 'card'"
                title="卡片视图"
              >
                <el-icon><Grid /></el-icon>
              </button>
              <button
                class="p-2 transition-colors"
                :class="viewMode === 'table' ? 'bg-gray-100 text-gray-900' : 'text-gray-400 hover:text-gray-600'"
                @click="viewMode = 'table'"
                title="表格视图"
              >
                <el-icon><List /></el-icon>
              </button>
            </div>
            <!-- 视图密度：普通/紧凑 -->
            <div class="flex items-center border border-gray-200 rounded-lg overflow-hidden">
              <button
                class="p-2 transition-colors text-xs"
                :class="viewDensity === 'normal' ? 'bg-gray-100 text-gray-900' : 'text-gray-400 hover:text-gray-600'"
                @click="viewDensity = 'normal'"
                title="普通密度"
              >
                <span class="font-medium">宽</span>
              </button>
              <button
                class="p-2 transition-colors text-xs"
                :class="viewDensity === 'compact' ? 'bg-gray-100 text-gray-900' : 'text-gray-400 hover:text-gray-600'"
                @click="viewDensity = 'compact'"
                title="紧凑密度"
              >
                <span class="font-medium">密</span>
              </button>
            </div>
          </div>

          <!-- 导出按钮 -->
          <el-button :loading="exportLoading" @click="handleExport">
            <el-icon class="mr-1"><Download /></el-icon>
            导出
          </el-button>

          <!-- 新建按钮组 -->
          <el-dropdown split-button type="primary" @click="handleCreate" @command="handleDropdownCommand">
            <el-icon class="mr-1"><Plus /></el-icon>
            新建资产
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="batch">
                  <el-icon class="mr-2"><DocumentAdd /></el-icon>
                  批量入库
                </el-dropdown-item>
                <el-dropdown-item divided command="import">
                  <el-icon class="mr-2"><Upload /></el-icon>
                  Excel导入
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 批量操作栏 (选中资产时显示) -->
      <div
        v-if="selectedAssets.length > 0"
        class="bg-blue-50 border-b border-blue-200 px-4 py-2 flex items-center justify-between"
      >
        <div class="flex items-center gap-3">
          <span class="text-blue-700 font-medium">
            已选择 {{ selectedAssets.length }} 件资产
          </span>
          <el-button text size="small" @click="clearSelection">取消选择</el-button>
        </div>
        <div class="flex items-center gap-2">
          <el-button size="small" @click="handleBatchPrintLabels">
            <el-icon class="mr-1"><Printer /></el-icon>
            批量打印标签
          </el-button>
          <el-button type="primary" size="small" @click="handleBatchTransfer">
            <el-icon class="mr-1"><Position /></el-icon>
            批量调拨
          </el-button>
        </div>
      </div>

      <!-- 资产列表区域 -->
      <div class="flex-1 overflow-auto p-4">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex items-center justify-center h-64">
          <el-icon class="is-loading text-4xl text-gray-400"><Refresh /></el-icon>
        </div>

        <!-- 空状态 -->
        <div v-else-if="assetList.length === 0" class="flex flex-col items-center justify-center h-64 text-gray-400">
          <el-icon class="text-6xl mb-4"><Box /></el-icon>
          <p class="text-lg">暂无资产数据</p>
          <p class="text-sm mt-1">点击右上角「新建资产」开始添加</p>
        </div>

        <!-- 卡片视图 -->
        <div
          v-else-if="viewMode === 'card'"
          class="grid"
          :class="viewDensity === 'compact'
            ? 'gap-1.5 grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 xl:grid-cols-8 2xl:grid-cols-10'
            : 'gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'"
        >
          <div
            v-for="asset in assetList"
            :key="asset.id"
            class="bg-white border border-gray-200 hover:shadow-md transition-shadow cursor-pointer group"
            :class="viewDensity === 'compact' ? 'rounded p-1.5' : 'rounded-xl p-4'"
            @click="handleViewDetail(asset)"
          >
            <!-- 紧凑模式 -->
            <template v-if="viewDensity === 'compact'">
              <div class="flex items-center gap-1 mb-0.5">
                <span class="w-1 h-1 rounded-full flex-shrink-0" :class="getStatusStyle(asset.status).bg" />
                <span class="text-[10px] text-gray-400 font-mono truncate leading-none">{{ asset.assetCode?.split('-').pop() }}</span>
              </div>
              <h3 class="text-xs font-medium text-gray-900 truncate leading-tight" :title="asset.assetName">{{ asset.assetName }}</h3>
              <p class="text-[10px] text-gray-400 truncate leading-tight">{{ asset.categoryName }}</p>
              <div class="mt-1 pt-1 border-t border-gray-100 flex items-center justify-between opacity-0 group-hover:opacity-100 transition-opacity">
                <el-button text size="small" class="!p-0 !text-[10px] !h-4 !min-h-0" @click.stop="handleEdit(asset)">编辑</el-button>
                <el-button text size="small" class="!p-0 !h-4 !min-h-0" @click.stop="handlePrintLabel(asset)">
                  <el-icon size="10"><Printer /></el-icon>
                </el-button>
              </div>
            </template>

            <!-- 普通模式 -->
            <template v-else>
              <!-- 头部：状态 + 管理模式 + 编号 -->
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center gap-2">
                  <span
                    class="w-2 h-2 rounded-full"
                    :class="getStatusStyle(asset.status).bg"
                  />
                  <el-tag
                    v-if="asset.managementMode === ManagementMode.BATCH"
                    type="warning"
                    size="small"
                    class="!py-0 !px-1.5 !text-xs"
                  >
                    批量
                  </el-tag>
                </div>
                <span class="text-xs text-gray-400 font-mono">{{ asset.assetCode }}</span>
              </div>

              <!-- 名称 -->
              <h3 class="font-medium text-gray-900 truncate mb-1">{{ asset.assetName }}</h3>

              <!-- 分类 + 品牌型号 -->
              <p class="text-sm text-gray-500 truncate">
                {{ asset.categoryName }}
                <span v-if="asset.brand"> · {{ asset.brand }}</span>
                <span v-if="asset.model"> {{ asset.model }}</span>
              </p>

              <!-- 数量/库存 -->
              <div class="mt-2 flex items-center gap-2 text-xs">
                <span class="text-gray-400">{{ asset.managementMode === ManagementMode.BATCH ? '库存:' : '数量:' }}</span>
                <template v-if="asset.managementMode === ManagementMode.BATCH">
                  <span :class="(asset.availableQuantity ?? 0) > 0 ? 'text-green-600 font-medium' : 'text-red-500 font-medium'">
                    {{ asset.availableQuantity ?? 0 }}/{{ asset.quantity ?? 0 }} {{ asset.unit || '个' }}
                  </span>
                </template>
                <template v-else>
                  <span class="text-gray-600 font-medium">1 {{ asset.unit || '件' }}</span>
                </template>
              </div>

              <!-- 位置和责任人 -->
              <div class="mt-3 pt-3 border-t border-gray-100 space-y-1">
                <div class="flex items-center gap-2 text-xs text-gray-500">
                  <el-icon><Position /></el-icon>
                  <span class="truncate">{{ asset.locationName || '未分配位置' }}</span>
                </div>
                <div v-if="asset.responsibleUserName" class="flex items-center gap-2 text-xs text-gray-500">
                  <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  <span>{{ asset.responsibleUserName }}</span>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="mt-3 pt-3 border-t border-gray-100 flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <el-button text size="small" @click.stop="handleEdit(asset)">编辑</el-button>
              <el-button text size="small" @click.stop="handleBorrow(asset)">借用</el-button>
              <el-dropdown @command="(cmd: string) => {
                if (cmd === 'transfer') handleTransfer(asset)
                else if (cmd === 'maintenance') handleMaintenance(asset)
                else if (cmd === 'print') handlePrintLabel(asset)
                else if (cmd === 'scrap') handleScrap(asset)
                else if (cmd === 'delete') handleDelete(asset)
              }">
                <el-button text size="small" @click.stop>更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="transfer">调拨</el-dropdown-item>
                    <el-dropdown-item command="maintenance">维修</el-dropdown-item>
                    <el-dropdown-item command="print">打印标签</el-dropdown-item>
                    <el-dropdown-item command="scrap" divided>报废</el-dropdown-item>
                    <el-dropdown-item command="delete">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            </template>
          </div>
        </div>

        <!-- 表格视图 -->
        <div v-else class="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <el-table
            :data="assetList"
            style="width: 100%"
            :size="viewDensity === 'compact' ? 'small' : 'default'"
            @row-click="handleViewDetail"
            @selection-change="handleSelectionChange"
            :row-class-name="() => 'cursor-pointer'"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column prop="assetCode" label="编号" width="140">
              <template #default="{ row }">
                <span class="font-mono text-sm">{{ row.assetCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="assetName" label="名称" min-width="160" />
            <el-table-column prop="categoryName" label="分类" width="100" />
            <el-table-column label="管理模式" width="90" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.managementMode === ManagementMode.BATCH ? 'warning' : 'info'"
                  size="small"
                >
                  {{ ManagementModeMap[row.managementMode as ManagementMode] || '单品' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="数量/库存" width="100" align="center">
              <template #default="{ row }">
                <template v-if="row.managementMode === ManagementMode.BATCH">
                  <span :class="(row.availableQuantity ?? 0) > 0 ? 'text-green-600' : 'text-red-500'">
                    {{ row.availableQuantity ?? 0 }}
                  </span>
                  <span class="text-gray-400">/{{ row.quantity ?? 0 }} {{ row.unit || '个' }}</span>
                </template>
                <template v-else>
                  <span class="text-gray-600">1</span>
                  <span class="text-gray-400"> {{ row.unit || '件' }}</span>
                </template>
              </template>
            </el-table-column>
            <el-table-column label="品牌/型号" width="140">
              <template #default="{ row }">
                <span class="text-sm text-gray-600">
                  {{ row.brand || '-' }}
                  <span v-if="row.model" class="text-gray-400"> / {{ row.model }}</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <span
                  class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs"
                  :class="getStatusStyle(row.status).light + ' ' + getStatusStyle(row.status).text"
                >
                  <span class="w-1.5 h-1.5 rounded-full" :class="getStatusStyle(row.status).bg" />
                  {{ row.statusDesc }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="locationName" label="位置" width="140">
              <template #default="{ row }">
                <span class="text-sm text-gray-600">{{ row.locationName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="responsibleUserName" label="责任人" width="90">
              <template #default="{ row }">
                <span class="text-sm text-gray-600">{{ row.responsibleUserName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="原值" width="100" align="right">
              <template #default="{ row }">
                <span class="text-sm">{{ row.originalValue ? `¥${row.originalValue.toFixed(0)}` : '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button text size="small" @click.stop="handleEdit(row)">编辑</el-button>
                <el-button text size="small" @click.stop="handleBorrow(row)">借用</el-button>
                <el-button text size="small" @click.stop="handlePrintLabel(row)">
                  <el-icon><Printer /></el-icon>
                </el-button>
                <el-button text size="small" @click.stop="handleTransfer(row)">调拨</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 分页 -->
        <div v-if="assetList.length > 0" class="mt-4 flex justify-center">
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

    <!-- 新建/编辑对话框 -->
    <AssetFormDialog
      v-model:visible="formDialogVisible"
      :asset="editingAsset"
      :categories="categoryTree"
      @saved="handleFormSaved"
    />

    <!-- 调拨对话框 -->
    <AssetTransferDialog
      v-model:visible="transferDialogVisible"
      :asset="transferringAsset"
      @saved="handleTransferSaved"
    />

    <!-- 详情抽屉 -->
    <AssetDetailDrawer
      v-model:visible="detailDrawerVisible"
      :asset="selectedAsset"
      @edit="handleEdit"
      @transfer="handleTransfer"
    />

    <!-- 批量入库对话框 -->
    <AssetBatchCreateDialog
      v-model:visible="batchCreateDialogVisible"
      :categories="categoryTree"
      @saved="handleBatchCreateSaved"
    />

    <!-- 批量调拨对话框 -->
    <AssetBatchTransferDialog
      v-model:visible="batchTransferDialogVisible"
      :assets="selectedAssets"
      @saved="handleBatchTransferSaved"
    />

    <!-- Excel导入对话框 -->
    <AssetImportDialog
      v-model:visible="importDialogVisible"
      @imported="handleImportSaved"
    />

    <!-- 借用对话框 -->
    <AssetBorrowDialog
      v-model:visible="borrowDialogVisible"
      :asset="borrowingAsset"
      @saved="handleBorrowSaved"
    />

    <!-- 维修对话框 -->
    <AssetMaintenanceDialog
      v-model:visible="maintenanceDialogVisible"
      :asset="maintenanceAsset"
      @saved="handleMaintenanceSaved"
    />

    <!-- 标签打印对话框 -->
    <AssetLabelDialog
      v-model="labelDialogVisible"
      :assets="labelAssets"
    />
  </div>
</template>

<style scoped>
/* 平滑过渡 */
.transition-colors {
  transition-property: color, background-color, border-color;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

.transition-shadow {
  transition-property: box-shadow;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

.transition-transform {
  transition-property: transform;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

.transition-opacity {
  transition-property: opacity;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

/* 表格行hover效果 */
:deep(.el-table__row:hover) {
  background-color: #f9fafb !important;
}

:deep(.el-table__row) {
  cursor: pointer;
}

/* 隐藏表格边框 */
:deep(.el-table--border) {
  border: none;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f9fafb;
  font-weight: 500;
  color: #6b7280;
  font-size: 13px;
}
</style>
