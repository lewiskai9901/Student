<script setup lang="ts">
/**
 * 资产审批列表视图
 * UI优化版本 - 使用设计系统组件
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { CheckCircle, XCircle, Eye, ClipboardCheck, FileText, Clock } from 'lucide-vue-next'
import { assetApprovalApi } from '@/api/assetApproval'
import type { AssetApproval } from '@/types/asset'
import {
  ApprovalType,
  ApprovalTypeMap,
  ApprovalStatus,
  ApprovalStatusMap
} from '@/types/asset'

// 设计系统组件
import EmptyState from '@/components/design-system/feedback/EmptyState.vue'

// ============ 状态 ============
const loading = ref(false)
const approvalList = ref<AssetApproval[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 筛选条件
const activeTab = ref<'pending' | 'my' | 'all'>('pending')
const filterType = ref<number | undefined>()
const filterStatus = ref<number | undefined>()

// 详情对话框
const detailDialogVisible = ref(false)
const selectedApproval = ref<AssetApproval | null>(null)
const approvalRemark = ref('')

// ============ 计算属性 ============
const approvalTypeOptions = computed(() => {
  return Object.entries(ApprovalTypeMap).map(([value, label]) => ({
    value: Number(value),
    label
  }))
})

const approvalStatusOptions = computed(() => {
  return Object.entries(ApprovalStatusMap).map(([value, label]) => ({
    value: Number(value),
    label
  }))
})

// ============ 方法 ============
async function loadApprovals() {
  loading.value = true
  try {
    if (activeTab.value === 'pending') {
      approvalList.value = await assetApprovalApi.getPendingApprovals()
      total.value = approvalList.value.length
    } else if (activeTab.value === 'my') {
      approvalList.value = await assetApprovalApi.getMyApprovals()
      total.value = approvalList.value.length
    } else {
      const res = await assetApprovalApi.queryApprovals({
        approvalType: filterType.value,
        status: filterStatus.value,
        pageNum: currentPage.value,
        pageSize: pageSize.value
      })
      approvalList.value = res.records || []
      total.value = res.total || 0
    }
  } catch (error) {
    console.error('Failed to load approvals:', error)
    ElMessage.error('加载审批列表失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange(tab: 'pending' | 'my' | 'all') {
  activeTab.value = tab
  currentPage.value = 1
  loadApprovals()
}

function handleFilter() {
  currentPage.value = 1
  loadApprovals()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadApprovals()
}

function handleViewDetail(approval: AssetApproval) {
  selectedApproval.value = approval
  approvalRemark.value = ''
  detailDialogVisible.value = true
}

async function handleApprove(approval: AssetApproval) {
  try {
    await ElMessageBox.confirm(
      `确定要通过审批 "${approval.approvalNo}" 吗？`,
      '审批确认',
      { type: 'info' }
    )

    await assetApprovalApi.approve(approval.id, { remark: approvalRemark.value })
    ElMessage.success('审批通过')
    detailDialogVisible.value = false
    loadApprovals()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

async function handleReject(approval: AssetApproval) {
  try {
    await ElMessageBox.confirm(
      `确定要拒绝审批 "${approval.approvalNo}" 吗？`,
      '审批确认',
      { type: 'warning' }
    )

    await assetApprovalApi.reject(approval.id, { remark: approvalRemark.value })
    ElMessage.success('已拒绝')
    detailDialogVisible.value = false
    loadApprovals()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

async function handleCancel(approval: AssetApproval) {
  try {
    await ElMessageBox.confirm(
      `确定要取消申请 "${approval.approvalNo}" 吗？`,
      '取消确认',
      { type: 'warning' }
    )

    await assetApprovalApi.cancel(approval.id)
    ElMessage.success('已取消')
    loadApprovals()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

function getStatusType(status: ApprovalStatus) {
  switch (status) {
    case ApprovalStatus.PENDING: return 'warning'
    case ApprovalStatus.APPROVED: return 'success'
    case ApprovalStatus.REJECTED: return 'danger'
    case ApprovalStatus.CANCELLED: return 'info'
    default: return 'info'
  }
}

function getTypeColor(type: ApprovalType) {
  switch (type) {
    case ApprovalType.BORROW_APPLY: return 'primary'
    case ApprovalType.PROCUREMENT: return 'success'
    case ApprovalType.SCRAP: return 'danger'
    case ApprovalType.TRANSFER: return 'warning'
    default: return 'info'
  }
}

// ============ 生命周期 ============
onMounted(() => {
  loadApprovals()
})
</script>

<template>
  <div class="min-h-full bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-8">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20">
          <ClipboardCheck class="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">审批管理</h1>
          <p class="text-sm text-gray-500">管理资产相关的审批申请</p>
        </div>
      </div>
    </div>

    <!-- 操作栏 - 优化样式 -->
    <div class="bg-white rounded-xl border border-gray-200 p-4 mb-6 shadow-sm">
      <div class="flex items-center justify-between">
        <!-- 标签页 -->
        <div class="flex items-center gap-1 bg-gray-100 rounded-lg p-1">
          <button
            class="px-4 py-2 rounded-lg text-sm font-medium transition-all"
            :class="activeTab === 'pending'
              ? 'bg-white text-indigo-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'"
            @click="handleTabChange('pending')"
          >
            <Clock class="w-4 h-4 inline-block mr-1" />
            待我审批
          </button>
          <button
            class="px-4 py-2 rounded-lg text-sm font-medium transition-all"
            :class="activeTab === 'my'
              ? 'bg-white text-indigo-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'"
            @click="handleTabChange('my')"
          >
            <FileText class="w-4 h-4 inline-block mr-1" />
            我的申请
          </button>
          <button
            class="px-4 py-2 rounded-lg text-sm font-medium transition-all"
            :class="activeTab === 'all'
              ? 'bg-white text-indigo-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'"
            @click="handleTabChange('all')"
          >
            全部审批
          </button>
        </div>

        <div class="flex items-center gap-3">
          <!-- 筛选（全部标签页） -->
          <template v-if="activeTab === 'all'">
            <el-select
              v-model="filterType"
              placeholder="审批类型"
              clearable
              style="width: 140px"
              @change="handleFilter"
            >
              <el-option
                v-for="item in approvalTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select
              v-model="filterStatus"
              placeholder="审批状态"
              clearable
              style="width: 120px"
              @change="handleFilter"
            >
              <el-option
                v-for="item in approvalStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>

          <el-button :icon="Refresh" @click="loadApprovals" class="!border-gray-200">刷新</el-button>
        </div>
      </div>
    </div>

    <!-- 审批列表 -->
    <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
      <div class="p-4 border-b border-gray-100">
        <h3 class="text-base font-semibold text-gray-900">审批记录</h3>
      </div>

      <div v-loading="loading" class="p-4">
        <!-- 空状态 -->
        <EmptyState
          v-if="approvalList.length === 0 && !loading"
          title="暂无审批记录"
          :description="activeTab === 'pending' ? '当前没有待您审批的申请' : '暂无相关审批记录'"
        >
          <template #icon><ClipboardCheck /></template>
        </EmptyState>

        <!-- 审批卡片列表 -->
        <div v-else class="space-y-3">
          <div
            v-for="approval in approvalList"
            :key="approval.id"
            class="border border-gray-200 rounded-xl p-4 hover:border-indigo-200 hover:shadow-md transition-all cursor-pointer"
            @click="handleViewDetail(approval)"
          >
            <div class="flex items-start justify-between mb-3">
              <div class="flex items-center gap-3">
                <div :class="[
                  'w-10 h-10 rounded-xl flex items-center justify-center shadow-sm',
                  approval.approvalType === ApprovalType.BORROW_APPLY ? 'bg-gradient-to-br from-blue-400 to-blue-600' :
                  approval.approvalType === ApprovalType.PROCUREMENT ? 'bg-gradient-to-br from-emerald-400 to-emerald-600' :
                  approval.approvalType === ApprovalType.SCRAP ? 'bg-gradient-to-br from-red-400 to-red-600' :
                  'bg-gradient-to-br from-amber-400 to-amber-600'
                ]">
                  <ClipboardCheck class="w-5 h-5 text-white" />
                </div>
                <div>
                  <div class="font-semibold text-gray-900">{{ approval.assetName || '审批申请' }}</div>
                  <div class="text-sm text-gray-500 mt-0.5">
                    <span class="font-mono text-xs text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded mr-2">{{ approval.approvalNo }}</span>
                    <el-tag :type="getTypeColor(approval.approvalType)" size="small">
                      {{ ApprovalTypeMap[approval.approvalType as ApprovalType] }}
                    </el-tag>
                  </div>
                </div>
              </div>
              <span :class="[
                'px-2.5 py-1 rounded-full text-xs font-medium',
                approval.status === ApprovalStatus.PENDING ? 'bg-amber-100 text-amber-700' :
                approval.status === ApprovalStatus.APPROVED ? 'bg-emerald-100 text-emerald-700' :
                approval.status === ApprovalStatus.REJECTED ? 'bg-red-100 text-red-700' :
                'bg-gray-100 text-gray-700'
              ]">
                {{ ApprovalStatusMap[approval.status as ApprovalStatus] }}
              </span>
            </div>

            <div class="flex items-center gap-4 text-sm text-gray-500 mb-3">
              <span class="flex items-center gap-1">
                <FileText class="w-4 h-4" />
                {{ approval.applicantName }} · {{ approval.applicantDept || '-' }}
              </span>
              <span class="flex items-center gap-1">
                <Clock class="w-4 h-4" />
                {{ approval.applyTime }}
              </span>
            </div>

            <div v-if="approval.applyReason" class="text-sm text-gray-600 bg-gray-50 rounded-lg p-3 mb-3">
              {{ approval.applyReason }}
            </div>

            <!-- 操作按钮 -->
            <div v-if="approval.status === ApprovalStatus.PENDING" class="flex items-center gap-2 pt-3 border-t border-gray-100">
              <template v-if="activeTab === 'pending'">
                <el-button type="success" size="small" @click.stop="handleApprove(approval)" class="!rounded-lg">
                  <CheckCircle class="w-4 h-4 mr-1" />
                  通过
                </el-button>
                <el-button type="danger" size="small" @click.stop="handleReject(approval)" class="!rounded-lg">
                  <XCircle class="w-4 h-4 mr-1" />
                  拒绝
                </el-button>
              </template>
              <template v-if="activeTab === 'my'">
                <el-button type="warning" size="small" @click.stop="handleCancel(approval)" class="!rounded-lg">
                  取消申请
                </el-button>
              </template>
              <el-button size="small" @click.stop="handleViewDetail(approval)" class="!rounded-lg !border-gray-200">
                <Eye class="w-4 h-4 mr-1" />
                查看详情
              </el-button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="activeTab === 'all' && total > pageSize" class="mt-6 flex justify-center">
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      width="600px"
      class="modern-dialog"
    >
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg">
            <ClipboardCheck class="w-5 h-5 text-white" />
          </div>
          <div>
            <h3 class="text-lg font-semibold text-gray-900">审批详情</h3>
            <p class="text-sm text-gray-500">查看审批申请详细信息</p>
          </div>
        </div>
      </template>

      <div v-if="selectedApproval" class="space-y-6">
        <!-- 状态头部 -->
        <div :class="[
          'flex items-center justify-between p-4 rounded-xl',
          selectedApproval.status === ApprovalStatus.PENDING ? 'bg-amber-50 border border-amber-200' :
          selectedApproval.status === ApprovalStatus.APPROVED ? 'bg-emerald-50 border border-emerald-200' :
          selectedApproval.status === ApprovalStatus.REJECTED ? 'bg-red-50 border border-red-200' :
          'bg-gray-50 border border-gray-200'
        ]">
          <div class="flex items-center gap-3">
            <span class="font-mono text-sm text-indigo-600 bg-white px-2 py-1 rounded border">{{ selectedApproval.approvalNo }}</span>
            <el-tag :type="getTypeColor(selectedApproval.approvalType)" size="small">
              {{ ApprovalTypeMap[selectedApproval.approvalType as ApprovalType] }}
            </el-tag>
          </div>
          <span :class="[
            'px-3 py-1.5 rounded-lg text-sm font-semibold',
            selectedApproval.status === ApprovalStatus.PENDING ? 'bg-amber-500 text-white' :
            selectedApproval.status === ApprovalStatus.APPROVED ? 'bg-emerald-500 text-white' :
            selectedApproval.status === ApprovalStatus.REJECTED ? 'bg-red-500 text-white' :
            'bg-gray-500 text-white'
          ]">
            {{ ApprovalStatusMap[selectedApproval.status as ApprovalStatus] }}
          </span>
        </div>

        <!-- 基本信息 -->
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-gray-50 rounded-xl p-4">
            <label class="text-xs font-medium text-gray-400 uppercase tracking-wide">资产名称</label>
            <p class="mt-1 font-semibold text-gray-900">{{ selectedApproval.assetName || '-' }}</p>
          </div>
          <div class="bg-gray-50 rounded-xl p-4">
            <label class="text-xs font-medium text-gray-400 uppercase tracking-wide">申请人</label>
            <p class="mt-1 font-semibold text-gray-900">{{ selectedApproval.applicantName }}</p>
            <p class="text-sm text-gray-500">{{ selectedApproval.applicantDept || '-' }}</p>
          </div>
          <div class="bg-gray-50 rounded-xl p-4">
            <label class="text-xs font-medium text-gray-400 uppercase tracking-wide">申请时间</label>
            <p class="mt-1 font-semibold text-gray-900">{{ selectedApproval.applyTime }}</p>
          </div>
          <div v-if="selectedApproval.applyQuantity" class="bg-gray-50 rounded-xl p-4">
            <label class="text-xs font-medium text-gray-400 uppercase tracking-wide">申请数量</label>
            <p class="mt-1 font-semibold text-gray-900">{{ selectedApproval.applyQuantity }}</p>
          </div>
          <div v-if="selectedApproval.applyAmount" class="bg-blue-50 rounded-xl p-4">
            <label class="text-xs font-medium text-blue-400 uppercase tracking-wide">申请金额</label>
            <p class="mt-1 font-bold text-blue-600 text-lg">¥{{ selectedApproval.applyAmount }}</p>
          </div>
        </div>

        <!-- 申请原因 -->
        <div>
          <label class="text-xs font-medium text-gray-400 uppercase tracking-wide">申请原因</label>
          <p class="mt-2 p-4 bg-gray-50 rounded-xl text-gray-700 leading-relaxed">{{ selectedApproval.applyReason || '无' }}</p>
        </div>

        <!-- 审批信息 -->
        <div v-if="selectedApproval.approverName" class="border-t border-gray-100 pt-4">
          <div class="flex items-center gap-2 mb-3">
            <CheckCircle class="w-4 h-4 text-emerald-500" />
            <label class="text-xs font-medium text-gray-400 uppercase tracking-wide">审批信息</label>
          </div>
          <div class="bg-emerald-50 rounded-xl p-4">
            <p class="font-medium text-gray-900">{{ selectedApproval.approverName }}</p>
            <p class="text-sm text-gray-500">{{ selectedApproval.approvalTime }}</p>
            <p v-if="selectedApproval.approvalRemark" class="mt-2 text-gray-700">{{ selectedApproval.approvalRemark }}</p>
          </div>
        </div>

        <!-- 审批操作（待审批且在待我审批标签页） -->
        <div v-if="selectedApproval.status === ApprovalStatus.PENDING && activeTab === 'pending'" class="border-t border-gray-100 pt-4">
          <label class="text-xs font-medium text-gray-400 uppercase tracking-wide mb-2 block">审批意见</label>
          <el-input
            v-model="approvalRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入审批意见（可选）"
            class="!rounded-xl"
          />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3">
          <template v-if="selectedApproval?.status === ApprovalStatus.PENDING && activeTab === 'pending'">
            <el-button @click="detailDialogVisible = false" class="!px-5 !rounded-lg">取消</el-button>
            <el-button type="danger" @click="handleReject(selectedApproval!)" class="!px-5 !rounded-lg">
              <XCircle class="w-4 h-4 mr-1" />
              拒绝
            </el-button>
            <el-button type="primary" @click="handleApprove(selectedApproval!)" class="!px-5 !rounded-lg !bg-gradient-to-r !from-indigo-500 !to-purple-600 !border-none">
              <CheckCircle class="w-4 h-4 mr-1" />
              通过
            </el-button>
          </template>
          <template v-else>
            <el-button @click="detailDialogVisible = false" class="!px-6 !rounded-lg">关闭</el-button>
          </template>
        </div>
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

/* 过渡动效 */
.transition-all {
  transition: all 0.2s ease;
}
</style>
