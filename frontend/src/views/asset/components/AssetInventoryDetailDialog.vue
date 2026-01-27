<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Warning, CircleCheck, CircleClose, Search } from '@element-plus/icons-vue'
import { getInventory, updateInventoryDetail } from '@/api/v2/assetInventory'
import type { AssetInventory, AssetInventoryDetail, UpdateInventoryDetailRequest } from '@/types/v2/asset'
import { InventoryStatus, InventoryResultType, InventoryResultTypeMap } from '@/types/v2/asset'

const props = defineProps<{
  visible: boolean
  inventoryId?: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'updated'): void
}>()

// 状态
const loading = ref(false)
const inventory = ref<AssetInventory | null>(null)
const searchKeyword = ref('')
const editingDetailId = ref<number | null>(null)
const editForm = ref<UpdateInventoryDetailRequest>({ actualQuantity: 0, remark: '' })
const saving = ref(false)

// 过滤后的明细列表
const filteredDetails = computed(() => {
  if (!inventory.value?.details) return []
  if (!searchKeyword.value) return inventory.value.details

  const keyword = searchKeyword.value.toLowerCase()
  return inventory.value.details.filter(d =>
    d.assetCode?.toLowerCase().includes(keyword) ||
    d.assetName?.toLowerCase().includes(keyword) ||
    d.locationName?.toLowerCase().includes(keyword)
  )
})

// 是否可编辑
const isEditable = computed(() => {
  return inventory.value?.status === InventoryStatus.IN_PROGRESS
})

// 统计数据
const stats = computed(() => {
  if (!inventory.value?.details) {
    return { total: 0, checked: 0, normal: 0, profit: 0, loss: 0, unchecked: 0 }
  }
  const details = inventory.value.details
  const total = details.length
  const checked = details.filter(d => d.actualQuantity !== null).length
  const normal = details.filter(d => d.resultType === InventoryResultType.NORMAL).length
  const profit = details.filter(d => d.resultType === InventoryResultType.PROFIT).length
  const loss = details.filter(d => d.resultType === InventoryResultType.LOSS).length
  return { total, checked, normal, profit, loss, unchecked: total - checked }
})

// 进度百分比
const progress = computed(() => {
  if (stats.value.total === 0) return 0
  return Math.round((stats.value.checked / stats.value.total) * 100)
})

// 监听弹窗打开
watch(() => props.visible, async (visible) => {
  if (visible && props.inventoryId) {
    await loadData()
  }
})

// 加载数据
async function loadData() {
  if (!props.inventoryId) return

  loading.value = true
  try {
    inventory.value = await getInventory(props.inventoryId)
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 关闭弹窗
function handleClose() {
  inventory.value = null
  searchKeyword.value = ''
  editingDetailId.value = null
  emit('update:visible', false)
}

// 开始编辑
function startEdit(detail: AssetInventoryDetail) {
  if (!isEditable.value) return

  editingDetailId.value = detail.id
  editForm.value = {
    actualQuantity: detail.actualQuantity ?? detail.expectedQuantity,
    remark: detail.remark || ''
  }
}

// 取消编辑
function cancelEdit() {
  editingDetailId.value = null
  editForm.value = { actualQuantity: 0, remark: '' }
}

// 保存编辑
async function saveEdit(detail: AssetInventoryDetail) {
  if (!inventory.value) return

  saving.value = true
  try {
    await updateInventoryDetail(inventory.value.id, detail.id, editForm.value)
    ElMessage.success('保存成功')
    editingDetailId.value = null
    await loadData()
    emit('updated')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 快捷盘点（数量不变）
async function quickCheck(detail: AssetInventoryDetail) {
  if (!inventory.value || !isEditable.value) return

  saving.value = true
  try {
    await updateInventoryDetail(inventory.value.id, detail.id, {
      actualQuantity: detail.expectedQuantity,
      remark: ''
    })
    ElMessage.success('盘点完成')
    await loadData()
    emit('updated')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    saving.value = false
  }
}

// 获取结果类型样式
function getResultStyle(resultType: number | null) {
  switch (resultType) {
    case InventoryResultType.NORMAL:
      return 'text-green-600 bg-green-50'
    case InventoryResultType.PROFIT:
      return 'text-blue-600 bg-blue-50'
    case InventoryResultType.LOSS:
      return 'text-red-600 bg-red-50'
    default:
      return 'text-gray-400 bg-gray-50'
  }
}

// 获取结果类型文本
function getResultText(resultType: number | null) {
  if (resultType === null) return '未盘'
  return InventoryResultTypeMap[resultType as InventoryResultType] || '未知'
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="inventory ? `盘点详情 - ${inventory.inventoryCode}` : '盘点详情'"
    width="1000px"
    top="5vh"
    @close="handleClose"
  >
    <div v-loading="loading" class="min-h-[400px]">
      <template v-if="inventory">
        <!-- 基本信息 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6 p-4 bg-gray-50 rounded-lg">
          <div>
            <p class="text-sm text-gray-500">盘点名称</p>
            <p class="font-medium">{{ inventory.inventoryName }}</p>
          </div>
          <div>
            <p class="text-sm text-gray-500">盘点范围</p>
            <p class="font-medium">{{ inventory.scopeTypeDesc }}</p>
          </div>
          <div>
            <p class="text-sm text-gray-500">时间范围</p>
            <p class="font-medium">{{ inventory.startDate }} ~ {{ inventory.endDate }}</p>
          </div>
          <div>
            <p class="text-sm text-gray-500">状态</p>
            <el-tag
              :type="inventory.status === InventoryStatus.IN_PROGRESS ? 'warning' :
                     inventory.status === InventoryStatus.COMPLETED ? 'success' : 'info'"
              size="small"
            >
              {{ inventory.statusDesc }}
            </el-tag>
          </div>
        </div>

        <!-- 统计信息 -->
        <div class="grid grid-cols-2 md:grid-cols-6 gap-3 mb-6">
          <div class="text-center p-3 bg-blue-50 rounded-lg">
            <p class="text-2xl font-bold text-blue-600">{{ stats.total }}</p>
            <p class="text-xs text-gray-500">总资产数</p>
          </div>
          <div class="text-center p-3 bg-green-50 rounded-lg">
            <p class="text-2xl font-bold text-green-600">{{ stats.checked }}</p>
            <p class="text-xs text-gray-500">已盘点</p>
          </div>
          <div class="text-center p-3 bg-gray-100 rounded-lg">
            <p class="text-2xl font-bold text-gray-600">{{ stats.unchecked }}</p>
            <p class="text-xs text-gray-500">未盘点</p>
          </div>
          <div class="text-center p-3 bg-emerald-50 rounded-lg">
            <p class="text-2xl font-bold text-emerald-600">{{ stats.normal }}</p>
            <p class="text-xs text-gray-500">正常</p>
          </div>
          <div class="text-center p-3 bg-cyan-50 rounded-lg">
            <p class="text-2xl font-bold text-cyan-600">{{ stats.profit }}</p>
            <p class="text-xs text-gray-500">盘盈</p>
          </div>
          <div class="text-center p-3 bg-red-50 rounded-lg">
            <p class="text-2xl font-bold text-red-600">{{ stats.loss }}</p>
            <p class="text-xs text-gray-500">盘亏</p>
          </div>
        </div>

        <!-- 进度条 -->
        <div class="mb-6">
          <div class="flex justify-between text-sm text-gray-600 mb-1">
            <span>盘点进度</span>
            <span>{{ progress }}%</span>
          </div>
          <el-progress
            :percentage="progress"
            :color="progress < 30 ? '#f56c6c' : progress < 70 ? '#e6a23c' : '#67c23a'"
            :stroke-width="12"
            :show-text="false"
          />
        </div>

        <!-- 搜索栏 -->
        <div class="mb-4">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索资产编号/名称/位置"
            clearable
            style="width: 300px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <!-- 明细列表 -->
        <el-table :data="filteredDetails" max-height="400px" stripe>
          <el-table-column prop="assetCode" label="资产编号" width="140" />
          <el-table-column prop="assetName" label="资产名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="locationName" label="存放位置" width="120" show-overflow-tooltip />
          <el-table-column prop="expectedQuantity" label="账面数量" width="90" align="center" />
          <el-table-column label="实盘数量" width="120" align="center">
            <template #default="{ row }">
              <template v-if="editingDetailId === row.id">
                <el-input-number
                  v-model="editForm.actualQuantity"
                  :min="0"
                  :max="9999"
                  size="small"
                  style="width: 90px"
                />
              </template>
              <template v-else>
                <span v-if="row.actualQuantity !== null">{{ row.actualQuantity }}</span>
                <span v-else class="text-gray-400">-</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="差异" width="70" align="center">
            <template #default="{ row }">
              <span v-if="row.difference !== null && row.difference !== 0"
                    :class="row.difference > 0 ? 'text-blue-600' : 'text-red-600'">
                {{ row.difference > 0 ? '+' : '' }}{{ row.difference }}
              </span>
              <span v-else-if="row.difference === 0" class="text-green-600">0</span>
              <span v-else class="text-gray-400">-</span>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="70" align="center">
            <template #default="{ row }">
              <span
                :class="[getResultStyle(row.resultType), 'px-2 py-0.5 rounded text-xs']"
              >
                {{ getResultText(row.resultType) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="checkerName" label="盘点人" width="90" />
          <el-table-column label="操作" width="160" fixed="right" v-if="isEditable">
            <template #default="{ row }">
              <template v-if="editingDetailId === row.id">
                <el-button link type="primary" :loading="saving" @click="saveEdit(row)">
                  保存
                </el-button>
                <el-button link @click="cancelEdit">取消</el-button>
              </template>
              <template v-else>
                <el-button link type="primary" @click="startEdit(row)">
                  编辑
                </el-button>
                <el-button
                  v-if="row.actualQuantity === null"
                  link
                  type="success"
                  :loading="saving"
                  @click="quickCheck(row)"
                >
                  <el-icon><Check /></el-icon> 正常
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>
