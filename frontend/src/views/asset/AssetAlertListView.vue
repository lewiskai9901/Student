<script setup lang="ts">
/**
 * 资产预警列表视图
 * UI优化版本 - 使用设计系统组件
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { Bell, CheckCircle, Eye, AlertTriangle, Clock, ShieldAlert, Package, AlertOctagon } from 'lucide-vue-next'
import { assetAlertApi } from '@/api/assetAlert'
import type { AssetAlert } from '@/types/asset'
import {
  AlertType,
  AlertTypeMap,
  AlertLevel,
  AlertLevelMap
} from '@/types/asset'

// 设计系统组件
import StatCard from '@/components/design-system/cards/StatCard.vue'
import EmptyState from '@/components/design-system/feedback/EmptyState.vue'

// ============ 状态 ============
const loading = ref(false)
const alertList = ref<AssetAlert[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const statistics = ref({
  unhandledCount: 0,
  overdueCount: 0,
  nearOverdueCount: 0,
  warrantyExpireCount: 0,
  lowStockCount: 0
})

// 筛选条件
const filterType = ref<number | undefined>()
const filterIsRead = ref<boolean | undefined>()
const filterIsHandled = ref<boolean | undefined>(false) // 默认显示未处理

// 详情对话框
const detailDialogVisible = ref(false)
const selectedAlert = ref<AssetAlert | null>(null)
const handleRemark = ref('')

// ============ 计算属性 ============
const alertTypeOptions = computed(() => {
  return Object.entries(AlertTypeMap).map(([value, label]) => ({
    value: Number(value),
    label
  }))
})

// ============ 方法 ============
async function loadAlerts() {
  loading.value = true
  try {
    const res = await assetAlertApi.queryAlerts({
      alertType: filterType.value,
      isRead: filterIsRead.value,
      isHandled: filterIsHandled.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    alertList.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to load alerts:', error)
    ElMessage.error('加载预警列表失败')
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try {
    const res = await assetAlertApi.getStatistics()
    statistics.value = res as any
  } catch (error) {
    console.error('Failed to load statistics:', error)
  }
}

function handleFilter() {
  currentPage.value = 1
  loadAlerts()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadAlerts()
}

function handleViewDetail(alert: AssetAlert) {
  selectedAlert.value = alert
  handleRemark.value = ''
  detailDialogVisible.value = true

  // 自动标记为已读
  if (!alert.isRead) {
    assetAlertApi.markAsRead(alert.id).then(() => {
      alert.isRead = true
    })
  }
}

async function handleMarkAsRead(alert: AssetAlert) {
  try {
    await assetAlertApi.markAsRead(alert.id)
    alert.isRead = true
    ElMessage.success('已标记为已读')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

async function handleMarkAllAsRead() {
  try {
    await assetAlertApi.markAllAsRead()
    ElMessage.success('已全部标记为已读')
    loadAlerts()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

async function handleHandle(alert: AssetAlert) {
  try {
    await ElMessageBox.confirm(
      `确定要处理预警 "${alert.alertContent}" 吗？`,
      '处理确认',
      { type: 'info' }
    )

    await assetAlertApi.handleAlert(alert.id, handleRemark.value)
    ElMessage.success('处理成功')
    detailDialogVisible.value = false
    loadAlerts()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

function getAlertTypeIcon(type: AlertType) {
  switch (type) {
    case AlertType.OVERDUE: return 'text-red-500'
    case AlertType.NEAR_OVERDUE: return 'text-amber-500'
    case AlertType.WARRANTY_EXPIRE: return 'text-blue-500'
    case AlertType.LOW_STOCK: return 'text-orange-500'
    default: return 'text-gray-500'
  }
}

function getAlertLevelTag(level: AlertLevel) {
  switch (level) {
    case AlertLevel.URGENT: return 'danger'
    case AlertLevel.IMPORTANT: return 'warning'
    default: return 'info'
  }
}

// ============ 生命周期 ============
onMounted(() => {
  loadAlerts()
  loadStatistics()
})
</script>

<template>
  <div class="min-h-full bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-8">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center shadow-lg shadow-red-500/20">
          <Bell class="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">预警中心</h1>
          <p class="text-sm text-gray-500">查看和处理资产相关预警信息</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 - 使用设计系统 -->
    <div class="grid grid-cols-1 md:grid-cols-5 gap-4 mb-8">
      <StatCard
        title="未处理"
        :value="statistics.unhandledCount"
        color="purple"
        subtitle="待处理预警"
      >
        <template #icon><AlertOctagon /></template>
      </StatCard>
      <StatCard
        title="逾期预警"
        :value="statistics.overdueCount"
        color="orange"
        subtitle="已超过预期"
      >
        <template #icon><AlertTriangle /></template>
      </StatCard>
      <StatCard
        title="即将逾期"
        :value="statistics.nearOverdueCount"
        color="orange"
        subtitle="3天内到期"
      >
        <template #icon><Clock /></template>
      </StatCard>
      <StatCard
        title="保修到期"
        :value="statistics.warrantyExpireCount"
        color="blue"
        subtitle="需续保处理"
      >
        <template #icon><ShieldAlert /></template>
      </StatCard>
      <StatCard
        title="库存不足"
        :value="statistics.lowStockCount"
        color="cyan"
        subtitle="需补充库存"
      >
        <template #icon><Package /></template>
      </StatCard>
    </div>

    <!-- 筛选和操作栏 - 优化样式 -->
    <div class="bg-white rounded-xl border border-gray-200 p-4 mb-6 shadow-sm">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <el-select
            v-model="filterType"
            placeholder="预警类型"
            clearable
            style="width: 140px"
            @change="handleFilter"
          >
            <el-option
              v-for="item in alertTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-select
            v-model="filterIsHandled"
            placeholder="处理状态"
            clearable
            style="width: 120px"
            @change="handleFilter"
          >
            <el-option label="未处理" :value="false" />
            <el-option label="已处理" :value="true" />
          </el-select>
        </div>

        <div class="flex items-center gap-3">
          <el-button @click="handleMarkAllAsRead" class="!border-gray-200">全部标记已读</el-button>
          <el-button :icon="Refresh" @click="loadAlerts" class="!border-gray-200">刷新</el-button>
        </div>
      </div>
    </div>

    <!-- 预警列表 - 优化样式 -->
    <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <div class="p-4 border-b border-gray-100">
        <h3 class="text-base font-semibold text-gray-900">预警列表</h3>
      </div>

      <div v-loading="loading" class="p-4">
        <!-- 有数据时显示列表 -->
        <div v-if="alertList.length > 0" class="space-y-3">
          <div
            v-for="row in alertList"
            :key="row.id"
            class="border rounded-xl p-4 transition-all hover:shadow-md cursor-pointer"
            :class="row.isRead ? 'border-gray-200 bg-gray-50/50' : 'border-red-200 bg-red-50/30'"
            @click="handleViewDetail(row)"
          >
            <div class="flex items-start gap-4">
              <!-- 图标 -->
              <div
                class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                :class="row.isRead ? 'bg-gray-100' : 'bg-gradient-to-br from-red-400 to-rose-500'"
              >
                <AlertTriangle :class="row.isRead ? 'w-5 h-5 text-gray-400' : 'w-5 h-5 text-white'" />
              </div>

              <!-- 内容 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-1">
                  <span :class="row.isRead ? 'text-gray-600' : 'font-semibold text-gray-900'">
                    {{ row.alertContent }}
                  </span>
                  <el-tag v-if="!row.isRead" type="danger" size="small" effect="dark">未读</el-tag>
                </div>
                <div class="flex items-center gap-3 text-sm text-gray-500">
                  <span class="font-mono text-xs text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">{{ row.assetCode }}</span>
                  <span>{{ row.assetName }}</span>
                </div>
              </div>

              <!-- 标签和时间 -->
              <div class="flex flex-col items-end gap-2">
                <div class="flex items-center gap-2">
                  <el-tag size="small">{{ AlertTypeMap[row.alertType as AlertType] }}</el-tag>
                  <el-tag :type="getAlertLevelTag(row.alertLevel)" size="small">
                    {{ AlertLevelMap[row.alertLevel as AlertLevel] }}
                  </el-tag>
                </div>
                <span class="text-xs text-gray-400">{{ row.alertTime }}</span>
              </div>

              <!-- 操作按钮 -->
              <div class="flex items-center gap-2 ml-4">
                <el-button text type="primary" size="small" @click.stop="handleViewDetail(row)">
                  <Eye class="w-4 h-4 mr-1" />
                  查看
                </el-button>
                <el-button
                  v-if="!row.isHandled"
                  text
                  type="success"
                  size="small"
                  @click.stop="handleHandle(row)"
                >
                  <CheckCircle class="w-4 h-4 mr-1" />
                  处理
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 - 使用设计系统 -->
        <EmptyState
          v-else-if="!loading"
          title="暂无预警信息"
          description="当前没有需要处理的预警，系统运行正常"
        >
          <template #icon><Bell /></template>
        </EmptyState>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="mt-6 flex justify-center">
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

    <!-- 详情对话框 - 优化样式 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="预警详情"
      width="520px"
      class="modern-dialog"
    >
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center">
            <Bell class="w-4 h-4 text-white" />
          </div>
          <div>
            <h3 class="text-lg font-semibold text-gray-900">预警详情</h3>
            <p class="text-sm text-gray-500">查看预警信息并进行处理</p>
          </div>
        </div>
      </template>

      <div v-if="selectedAlert" class="space-y-5">
        <div class="p-4 rounded-xl" :class="selectedAlert.isHandled ? 'bg-gradient-to-br from-green-50 to-emerald-50 border border-green-100' : 'bg-gradient-to-br from-amber-50 to-orange-50 border border-amber-100'">
          <div class="flex items-center gap-2 mb-3">
            <AlertTriangle :class="selectedAlert.isHandled ? 'w-5 h-5 text-green-600' : 'w-5 h-5 text-amber-600'" />
            <el-tag :type="getAlertLevelTag(selectedAlert.alertLevel)" size="small">
              {{ AlertLevelMap[selectedAlert.alertLevel as AlertLevel] }}
            </el-tag>
            <el-tag size="small">
              {{ AlertTypeMap[selectedAlert.alertType as AlertType] }}
            </el-tag>
          </div>
          <p class="text-gray-900 font-medium">{{ selectedAlert.alertContent }}</p>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="bg-gray-50 rounded-lg p-3">
            <label class="text-xs text-gray-500 block mb-1">资产编号</label>
            <p class="font-mono text-sm text-blue-600">{{ selectedAlert.assetCode }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <label class="text-xs text-gray-500 block mb-1">资产名称</label>
            <p class="text-sm font-medium text-gray-900">{{ selectedAlert.assetName }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <label class="text-xs text-gray-500 block mb-1">预警时间</label>
            <p class="text-sm text-gray-700">{{ selectedAlert.alertTime }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <label class="text-xs text-gray-500 block mb-1">处理状态</label>
            <el-tag :type="selectedAlert.isHandled ? 'success' : 'warning'" size="small">
              {{ selectedAlert.isHandled ? '已处理' : '未处理' }}
            </el-tag>
          </div>
        </div>

        <div v-if="selectedAlert.handlerName" class="bg-green-50 rounded-lg p-3 border border-green-100">
          <label class="text-xs text-green-600 block mb-1">处理人</label>
          <p class="text-sm text-gray-700">{{ selectedAlert.handlerName }} · {{ selectedAlert.handleTime }}</p>
        </div>

        <div v-if="selectedAlert.handleRemark" class="bg-gray-50 rounded-lg p-3">
          <label class="text-xs text-gray-500 block mb-1">处理备注</label>
          <p class="text-sm text-gray-700">{{ selectedAlert.handleRemark }}</p>
        </div>

        <!-- 处理操作（未处理时） -->
        <div v-if="!selectedAlert.isHandled" class="pt-4 border-t border-gray-100">
          <label class="text-sm font-medium text-gray-700 block mb-2">处理备注</label>
          <el-input
            v-model="handleRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注（可选）"
          />
        </div>
      </div>

      <template #footer>
        <template v-if="selectedAlert && !selectedAlert.isHandled">
          <el-button @click="detailDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleHandle(selectedAlert!)" class="!bg-gradient-to-r !from-green-500 !to-emerald-600 !border-none">
            <CheckCircle class="w-4 h-4 mr-1" />
            确认处理
          </el-button>
        </template>
        <template v-else>
          <el-button @click="detailDialogVisible = false" class="!px-6">关闭</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 现代对话框样式 */
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

/* 卡片悬停效果 */
.hover\:shadow-md:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
</style>
