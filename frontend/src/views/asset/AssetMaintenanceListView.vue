<script setup lang="ts">
/**
 * 资产维修记录列表
 * UI优化版本 - 使用设计系统组件
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { Wrench, Settings, CheckCircle, Clock, Package, AlertCircle } from 'lucide-vue-next'
import { assetApi } from '@/api/asset'
import type { Asset, AssetMaintenance, AssetQueryCriteria } from '@/types/asset'
import { MaintenanceStatus, MaintenanceStatusMap, MaintenanceType, MaintenanceTypeMap, AssetStatus } from '@/types/asset'
import AssetMaintenanceDialog from './components/AssetMaintenanceDialog.vue'
import AssetMaintenanceCompleteDialog from './components/AssetMaintenanceCompleteDialog.vue'

// 设计系统组件
import StatCard from '@/components/design-system/cards/StatCard.vue'
import EmptyState from '@/components/design-system/feedback/EmptyState.vue'

// 状态
const loading = ref(false)
const assetList = ref<Asset[]>([])  // 维修中的资产列表
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')

// 当前选中的资产维修记录
const currentAssetMaintenance = ref<{
  asset: Asset
  records: AssetMaintenance[]
} | null>(null)
const loadingRecords = ref(false)

// 对话框
const maintenanceDialogVisible = ref(false)
const selectedAssetForMaintenance = ref<Asset | null>(null)
const completeDialogVisible = ref(false)
const selectedMaintenance = ref<AssetMaintenance | null>(null)

// 统计
const statistics = ref({
  repairingCount: 0,
  totalMaintenanceCount: 0
})

// 加载维修中的资产列表
async function loadRepairingAssets() {
  loading.value = true
  try {
    const params: AssetQueryCriteria = {
      status: AssetStatus.REPAIRING,
      keyword: keyword.value || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    const result = await assetApi.getAssetList(params)
    assetList.value = result?.records || []
    total.value = result?.total || 0
    statistics.value.repairingCount = result?.total || 0
  } catch (error) {
    console.error('Load repairing assets failed:', error)
    ElMessage.error('加载维修资产失败')
  } finally {
    loading.value = false
  }
}

// 加载资产的维修记录
async function loadAssetMaintenanceRecords(asset: Asset) {
  if (currentAssetMaintenance.value?.asset.id === asset.id) {
    // 已选中，取消选中
    currentAssetMaintenance.value = null
    return
  }

  loadingRecords.value = true
  try {
    const records = await assetApi.getAssetMaintenanceRecords(asset.id)
    currentAssetMaintenance.value = {
      asset,
      records: records || []
    }
  } catch (error) {
    console.error('Load maintenance records failed:', error)
    ElMessage.error('加载维修记录失败')
  } finally {
    loadingRecords.value = false
  }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadRepairingAssets()
}

// 分页
function handlePageChange(page: number) {
  currentPage.value = page
  loadRepairingAssets()
}

// 创建维修记录
function handleCreateMaintenance(asset: Asset) {
  selectedAssetForMaintenance.value = asset
  maintenanceDialogVisible.value = true
}

// 完成维修
function handleCompleteMaintenance(maintenance: AssetMaintenance) {
  selectedMaintenance.value = maintenance
  completeDialogVisible.value = true
}

// 维修记录创建成功
function handleMaintenanceSaved() {
  maintenanceDialogVisible.value = false
  loadRepairingAssets()
  // 刷新当前资产的维修记录
  if (currentAssetMaintenance.value) {
    loadAssetMaintenanceRecords(currentAssetMaintenance.value.asset)
  }
}

// 维修完成成功
function handleMaintenanceCompleted() {
  completeDialogVisible.value = false
  loadRepairingAssets()
  // 刷新当前资产的维修记录
  if (currentAssetMaintenance.value) {
    loadAssetMaintenanceRecords(currentAssetMaintenance.value.asset)
  }
}

// 刷新
function refresh() {
  currentAssetMaintenance.value = null
  loadRepairingAssets()
}

// 获取维修状态样式
function getStatusStyle(status: number) {
  switch (status) {
    case MaintenanceStatus.IN_PROGRESS:
      return { bg: 'bg-amber-500', text: 'text-amber-600', light: 'bg-amber-50' }
    case MaintenanceStatus.COMPLETED:
      return { bg: 'bg-green-500', text: 'text-green-600', light: 'bg-green-50' }
    default:
      return { bg: 'bg-gray-400', text: 'text-gray-600', light: 'bg-gray-50' }
  }
}

// 初始化
onMounted(() => {
  loadRepairingAssets()
})
</script>

<template>
  <div class="min-h-full bg-gray-50/50 p-6">
    <!-- 页面标题 -->
    <div class="mb-8">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center shadow-lg shadow-amber-500/20">
          <Wrench class="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">维修管理</h1>
          <p class="text-sm text-gray-500">管理资产的维修和保养记录</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 - 使用设计系统 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <StatCard
        title="维修中"
        :value="statistics.repairingCount"
        color="orange"
        subtitle="当前正在维修的资产"
      >
        <template #icon><Wrench /></template>
      </StatCard>
      <StatCard
        title="本月已完成"
        value="-"
        color="emerald"
        subtitle="本月完成的维修任务"
      >
        <template #icon><CheckCircle /></template>
      </StatCard>
      <StatCard
        title="待处理"
        :value="statistics.repairingCount"
        color="purple"
        subtitle="等待维修处理"
      >
        <template #icon><AlertCircle /></template>
      </StatCard>
    </div>

    <div class="flex gap-6">
      <!-- 左侧：维修中的资产列表 -->
      <div class="flex-1 bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <!-- 工具栏 -->
        <div class="p-4 border-b border-gray-100 bg-gray-50/50">
          <div class="flex items-center justify-between">
            <h2 class="font-semibold text-gray-900">维修中的资产</h2>
            <div class="flex items-center gap-3">
              <!-- 搜索框 -->
              <el-input
                v-model="keyword"
                placeholder="搜索资产..."
                style="width: 200px"
                clearable
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix>
                  <el-icon class="text-gray-400"><Search /></el-icon>
                </template>
              </el-input>
              <el-button :icon="Refresh" @click="refresh" class="!border-gray-200" />
            </div>
          </div>
        </div>

        <!-- 列表 -->
        <div class="p-4" v-loading="loading">
          <!-- 空状态 - 使用设计系统 -->
          <EmptyState
            v-if="assetList.length === 0 && !loading"
            title="暂无维修中的资产"
            description="当前没有正在维修的资产，系统运行正常"
          >
            <template #icon><Wrench /></template>
          </EmptyState>

          <!-- 资产卡片列表 -->
          <div v-else class="space-y-3">
            <div
              v-for="asset in assetList"
              :key="asset.id"
              class="border rounded-xl p-4 cursor-pointer transition-all hover:shadow-md"
              :class="currentAssetMaintenance?.asset.id === asset.id
                ? 'border-blue-500 bg-blue-50/50 shadow-md'
                : 'border-gray-200 hover:border-blue-200'"
              @click="loadAssetMaintenanceRecords(asset)"
            >
              <div class="flex items-start justify-between">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 flex items-center justify-center shadow-sm">
                    <Wrench class="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <div class="font-semibold text-gray-900">{{ asset.assetName }}</div>
                    <div class="text-sm text-gray-500 mt-0.5">
                      <span class="font-mono text-xs text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded mr-2">{{ asset.assetCode }}</span>
                      {{ asset.categoryName || '未分类' }}
                    </div>
                  </div>
                </div>
                <span class="px-2.5 py-1 bg-amber-100 text-amber-700 text-xs font-medium rounded-full">
                  维修中
                </span>
              </div>
              <div v-if="asset.locationName" class="mt-3 text-sm text-gray-500 flex items-center gap-1">
                <Package class="w-4 h-4" />
                位置：{{ asset.locationName }}
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

      <!-- 右侧：维修记录详情 -->
      <div class="w-96 bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div class="p-4 border-b border-gray-100 bg-gray-50/50">
          <h2 class="font-semibold text-gray-900">维修记录</h2>
        </div>

        <div class="p-4" v-loading="loadingRecords">
          <!-- 未选择资产 - 使用设计系统空状态 -->
          <EmptyState
            v-if="!currentAssetMaintenance"
            title="选择资产"
            description="点击左侧资产查看维修记录"
          >
            <template #icon><Package /></template>
          </EmptyState>

          <!-- 维修记录列表 -->
          <div v-else>
            <!-- 资产信息 -->
            <div class="mb-4 p-4 bg-gradient-to-br from-gray-50 to-gray-100/50 rounded-xl border border-gray-100">
              <div class="font-semibold text-gray-900">{{ currentAssetMaintenance.asset.assetName }}</div>
              <div class="text-sm text-gray-500 mt-1 font-mono">{{ currentAssetMaintenance.asset.assetCode }}</div>
            </div>

            <!-- 操作按钮 -->
            <div class="mb-4">
              <el-button
                type="primary"
                size="small"
                @click="handleCreateMaintenance(currentAssetMaintenance.asset)"
                class="!bg-gradient-to-r !from-amber-500 !to-orange-600 !border-none shadow-sm"
              >
                <Wrench class="w-4 h-4 mr-1" />
                新增维修记录
              </el-button>
            </div>

            <!-- 记录列表 -->
            <div v-if="currentAssetMaintenance.records.length === 0" class="text-center py-12">
              <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 flex items-center justify-center">
                <Wrench class="w-8 h-8 text-gray-400" />
              </div>
              <p class="text-gray-500">暂无维修记录</p>
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="record in currentAssetMaintenance.records"
                :key="record.id"
                class="border border-gray-200 rounded-xl p-4 hover:border-gray-300 transition-colors"
              >
                <div class="flex items-center justify-between mb-3">
                  <div class="flex items-center gap-2">
                    <div :class="[
                      'w-8 h-8 rounded-lg flex items-center justify-center',
                      record.maintenanceType === MaintenanceType.REPAIR ? 'bg-amber-100' : 'bg-blue-100'
                    ]">
                      <Wrench
                        v-if="record.maintenanceType === MaintenanceType.REPAIR"
                        class="w-4 h-4 text-amber-600"
                      />
                      <Settings
                        v-else
                        class="w-4 h-4 text-blue-600"
                      />
                    </div>
                    <span class="font-medium text-gray-900">{{ record.maintenanceTypeDesc }}</span>
                  </div>
                  <span
                    :class="[
                      'px-2.5 py-1 rounded-full text-xs font-medium',
                      getStatusStyle(record.status).light,
                      getStatusStyle(record.status).text
                    ]"
                  >
                    {{ record.statusDesc }}
                  </span>
                </div>

                <div class="text-sm text-gray-600 mb-3 leading-relaxed">
                  {{ record.faultDesc || '无描述' }}
                </div>

                <div class="flex items-center flex-wrap gap-3 text-xs text-gray-500">
                  <span class="flex items-center gap-1 bg-gray-50 px-2 py-1 rounded">
                    <Clock class="w-3.5 h-3.5" />
                    {{ record.startDate }}
                  </span>
                  <span v-if="record.endDate" class="flex items-center gap-1 bg-green-50 text-green-600 px-2 py-1 rounded">
                    <CheckCircle class="w-3.5 h-3.5" />
                    {{ record.endDate }}
                  </span>
                  <span v-if="record.cost" class="flex items-center gap-1 bg-blue-50 text-blue-600 px-2 py-1 rounded">
                    ¥{{ record.cost }}
                  </span>
                </div>

                <div v-if="record.result" class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                  <span class="text-gray-400">结果：</span>{{ record.result }}
                </div>

                <!-- 完成按钮 -->
                <div v-if="record.status === MaintenanceStatus.IN_PROGRESS" class="mt-4 pt-3 border-t border-gray-100">
                  <el-button type="success" size="small" @click.stop="handleCompleteMaintenance(record)">
                    <CheckCircle class="w-4 h-4 mr-1" />
                    完成维修
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建维修记录对话框 -->
    <AssetMaintenanceDialog
      v-model:visible="maintenanceDialogVisible"
      :asset="selectedAssetForMaintenance"
      @saved="handleMaintenanceSaved"
    />

    <!-- 完成维修对话框 -->
    <AssetMaintenanceCompleteDialog
      v-model:visible="completeDialogVisible"
      :maintenance="selectedMaintenance"
      @completed="handleMaintenanceCompleted"
    />
  </div>
</template>

<style scoped>
.transition-all,
.transition-colors {
  transition: all 0.2s ease;
}

/* 列表卡片悬停动效 */
.hover\:shadow-md:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
</style>
