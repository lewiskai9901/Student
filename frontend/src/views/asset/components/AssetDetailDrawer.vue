<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Edit, Position, Clock, Tools, Printer } from '@element-plus/icons-vue'
import { assetApi } from '@/api/asset'
import { assetCodeApi } from '@/api/assetCode'
import type { Asset, AssetHistory, AssetMaintenance } from '@/types/asset'
import { AssetStatus, AssetStatusMap, MaintenanceTypeMap, MaintenanceStatusMap } from '@/types/asset'
import AssetLabelDialog from '@/components/asset/AssetLabelDialog.vue'

const props = defineProps<{
  visible: boolean
  asset: Asset | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  edit: [asset: Asset]
  transfer: [asset: Asset]
}>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const activeTab = ref('info')
const historyLoading = ref(false)
const maintenanceLoading = ref(false)
const historyList = ref<AssetHistory[]>([])
const maintenanceList = ref<AssetMaintenance[]>([])

// 标签打印
const labelDialogVisible = ref(false)
const qrCodeUrl = ref('')
const qrCodeLoading = ref(false)

// 监听 visible 和 asset 变化
watch([() => props.visible, () => props.asset], ([visible, asset]) => {
  if (visible && asset) {
    activeTab.value = 'info'
    loadHistory()
    loadMaintenance()
    loadQRCode()
  } else {
    qrCodeUrl.value = ''
  }
})

async function loadHistory() {
  if (!props.asset) return
  historyLoading.value = true
  try {
    const res = await assetApi.getAssetHistory(props.asset.id)
    historyList.value = res.data || []
  } catch (error) {
    console.error('Failed to load history:', error)
  } finally {
    historyLoading.value = false
  }
}

async function loadMaintenance() {
  if (!props.asset) return
  maintenanceLoading.value = true
  try {
    const res = await assetApi.getAssetMaintenanceRecords(props.asset.id)
    maintenanceList.value = res.data || []
  } catch (error) {
    console.error('Failed to load maintenance:', error)
  } finally {
    maintenanceLoading.value = false
  }
}

async function loadQRCode() {
  if (!props.asset) return
  qrCodeLoading.value = true
  try {
    const res = await assetCodeApi.generateQRCode(props.asset.assetCode, props.asset.id, 120)
    qrCodeUrl.value = res.qrcode || ''
  } catch (error) {
    console.error('Failed to load QR code:', error)
  } finally {
    qrCodeLoading.value = false
  }
}

// 打开标签打印对话框
function handlePrintLabel() {
  labelDialogVisible.value = true
}

// 计算标签打印所需的资产数据
const labelAssets = computed(() => {
  if (!props.asset) return []
  return [{
    id: props.asset.id,
    assetCode: props.asset.assetCode,
    assetName: props.asset.assetName,
    location: props.asset.locationName
  }]
})

function getStatusClass(status: number) {
  switch (status) {
    case AssetStatus.IN_USE:
      return 'bg-green-100 text-green-700'
    case AssetStatus.IDLE:
      return 'bg-gray-100 text-gray-700'
    case AssetStatus.REPAIRING:
      return 'bg-amber-100 text-amber-700'
    case AssetStatus.SCRAPPED:
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

function getChangeTypeIcon(type: string) {
  switch (type) {
    case 'CREATE':
      return '➕'
    case 'UPDATE':
      return '✏️'
    case 'TRANSFER':
      return '📦'
    case 'REPAIR':
      return '🔧'
    case 'SCRAP':
      return '🗑️'
    default:
      return '📝'
  }
}

function handleEdit() {
  if (props.asset) {
    emit('edit', props.asset)
    drawerVisible.value = false
  }
}

function handleTransfer() {
  if (props.asset) {
    emit('transfer', props.asset)
    drawerVisible.value = false
  }
}
</script>

<template>
  <el-drawer
    v-model="drawerVisible"
    title="资产详情"
    size="500px"
    :close-on-click-modal="true"
  >
    <template v-if="asset">
      <!-- 基本信息头部 -->
      <div class="mb-6 p-4 bg-gray-50 rounded-lg">
        <div class="flex items-start justify-between gap-4">
          <!-- 左侧：二维码 + 基本信息 -->
          <div class="flex items-start gap-4">
            <!-- 二维码缩略图 -->
            <div
              v-loading="qrCodeLoading"
              class="flex-shrink-0 w-20 h-20 bg-white rounded-lg border border-gray-200 flex items-center justify-center cursor-pointer hover:shadow-md transition-shadow"
              @click="handlePrintLabel"
              title="点击打印标签"
            >
              <img
                v-if="qrCodeUrl"
                :src="qrCodeUrl"
                alt="QR Code"
                class="w-16 h-16"
              />
              <el-icon v-else class="text-2xl text-gray-300"><Printer /></el-icon>
            </div>
            <!-- 资产信息 -->
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ asset.assetName }}</h3>
              <p class="text-sm text-gray-500 mt-1 font-mono">{{ asset.assetCode }}</p>
            </div>
          </div>
          <!-- 右侧：状态标签 -->
          <span
            class="px-3 py-1 rounded-full text-sm flex-shrink-0"
            :class="getStatusClass(asset.status)"
          >
            {{ asset.statusDesc }}
          </span>
        </div>

        <div class="mt-4 flex gap-2">
          <el-button size="small" :icon="Edit" @click="handleEdit">编辑</el-button>
          <el-button
            size="small"
            :icon="Position"
            :disabled="asset.status === AssetStatus.SCRAPPED || asset.status === AssetStatus.REPAIRING"
            @click="handleTransfer"
          >
            调拨
          </el-button>
          <el-button size="small" :icon="Printer" @click="handlePrintLabel">
            打印标签
          </el-button>
        </div>
      </div>

      <!-- 选项卡 -->
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="分类">
              {{ asset.categoryName }}
            </el-descriptions-item>
            <el-descriptions-item label="管理模式">
              <el-tag
                :type="asset.managementMode === 1 ? 'primary' : 'success'"
                size="small"
              >
                {{ asset.managementModeDesc || (asset.managementMode === 1 ? '单品管理' : '批量管理') }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="品牌">
              {{ asset.brand || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="型号">
              {{ asset.model || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="数量">
              {{ asset.quantity }} {{ asset.unit }}
            </el-descriptions-item>
            <el-descriptions-item label="原值">
              {{ asset.originalValue ? `¥${asset.originalValue.toFixed(2)}` : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="净值">
              {{ asset.netValue ? `¥${asset.netValue.toFixed(2)}` : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="购置日期">
              {{ asset.purchaseDate || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="保修截止">
              <span :class="{ 'text-red-500': asset.warrantyDate && !asset.underWarranty }">
                {{ asset.warrantyDate || '-' }}
                <span v-if="asset.warrantyDate" class="ml-2 text-xs">
                  {{ asset.underWarranty ? '(保修中)' : '(已过保)' }}
                </span>
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="供应商">
              {{ asset.supplier || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="位置">
              {{ asset.locationName || '未分配' }}
              <span v-if="asset.locationTypeDesc" class="text-xs text-gray-500 ml-1">
                ({{ asset.locationTypeDesc }})
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="责任人">
              {{ asset.responsibleUserName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="备注">
              {{ asset.remark || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ asset.createdAt }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- 变更历史 -->
        <el-tab-pane label="变更历史" name="history">
          <div v-loading="historyLoading">
            <el-timeline v-if="historyList.length > 0">
              <el-timeline-item
                v-for="item in historyList"
                :key="item.id"
                :timestamp="item.operateTime"
                placement="top"
              >
                <div class="flex items-start gap-2">
                  <span class="text-lg">{{ getChangeTypeIcon(item.changeType) }}</span>
                  <div>
                    <div class="font-medium text-gray-900">
                      {{ item.changeTypeDesc }}
                    </div>
                    <div v-if="item.changeType === 'TRANSFER'" class="text-sm text-gray-600 mt-1">
                      {{ item.oldLocationName || '未分配' }} → {{ item.newLocationName }}
                    </div>
                    <div v-if="item.remark" class="text-sm text-gray-500 mt-1">
                      {{ item.remark }}
                    </div>
                    <div class="text-xs text-gray-400 mt-1">
                      操作人：{{ item.operatorName || '系统' }}
                    </div>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无操作记录" />
          </div>
        </el-tab-pane>

        <!-- 维修记录 -->
        <el-tab-pane label="维修记录" name="maintenance">
          <div v-loading="maintenanceLoading">
            <div v-if="maintenanceList.length > 0" class="space-y-3">
              <div
                v-for="item in maintenanceList"
                :key="item.id"
                class="p-3 border rounded-lg"
              >
                <div class="flex items-center justify-between">
                  <span class="font-medium">{{ item.maintenanceTypeDesc }}</span>
                  <span
                    class="px-2 py-0.5 rounded text-xs"
                    :class="item.status === 1 ? 'bg-amber-100 text-amber-700' : 'bg-green-100 text-green-700'"
                  >
                    {{ item.statusDesc }}
                  </span>
                </div>
                <div v-if="item.faultDesc" class="text-sm text-gray-600 mt-2">
                  故障描述：{{ item.faultDesc }}
                </div>
                <div class="text-sm text-gray-500 mt-2 flex gap-4">
                  <span>开始：{{ item.startDate }}</span>
                  <span v-if="item.endDate">结束：{{ item.endDate }}</span>
                  <span v-if="item.cost">费用：¥{{ item.cost }}</span>
                </div>
                <div v-if="item.result" class="text-sm text-gray-600 mt-1">
                  结果：{{ item.result }}
                </div>
                <div v-if="item.maintainer" class="text-xs text-gray-400 mt-1">
                  维修人：{{ item.maintainer }}
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无维修记录" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 标签打印对话框 -->
    <AssetLabelDialog
      v-model="labelDialogVisible"
      :assets="labelAssets"
    />
  </el-drawer>
</template>
