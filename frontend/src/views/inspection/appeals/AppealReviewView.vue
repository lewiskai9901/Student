<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CheckCircle, XCircle } from 'lucide-vue-next'
import * as appealApi from '@/api/inspection/appeal'
import type { InspAppeal } from '@/types/insp/appeal'
import { AppealStatusConfig, type AppealStatus } from '@/types/insp/enums'
import InspEmptyState from '../shared/InspEmptyState.vue'

const loading = ref(false)
const appeals = ref<InspAppeal[]>([])

const approveDialog = ref(false)
const rejectDialog = ref(false)
const currentAppeal = ref<InspAppeal | null>(null)
const approveForm = ref({ comment: '', finalAdjustment: undefined as number | undefined })
const rejectForm = ref({ comment: '' })

async function loadData() {
  loading.value = true
  try {
    appeals.value = await appealApi.getPendingAppeals()
  } catch (e: any) {
    ElMessage.error(e.message || '加载待审清单失败')
  } finally {
    loading.value = false
  }
}

function openApprove(appeal: InspAppeal) {
  currentAppeal.value = appeal
  approveForm.value = { comment: '', finalAdjustment: appeal.expectedAdjustment }
  approveDialog.value = true
}

function openReject(appeal: InspAppeal) {
  currentAppeal.value = appeal
  rejectForm.value = { comment: '' }
  rejectDialog.value = true
}

async function submitApprove() {
  if (!currentAppeal.value) return
  try {
    await appealApi.approveAppeal(currentAppeal.value.id, {
      comment: approveForm.value.comment,
      finalAdjustment: approveForm.value.finalAdjustment,
    })
    ElMessage.success('已通过, 扣分已自动调整')
    approveDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '通过失败')
  }
}

async function submitReject() {
  if (!currentAppeal.value) return
  if (!rejectForm.value.comment?.trim()) {
    ElMessage.warning('驳回必须填写理由')
    return
  }
  try {
    await appealApi.rejectAppeal(currentAppeal.value.id, { comment: rejectForm.value.comment })
    ElMessage.success('已驳回')
    rejectDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '驳回失败')
  }
}

function getStatusConfig(status: AppealStatus) {
  return AppealStatusConfig[status] || { label: status, type: 'info', color: '#909399' }
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5">
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-lg font-semibold">申诉审核工作台</h2>
      <span class="text-sm text-gray-500">待审 {{ appeals.length }} 条</span>
    </div>

    <el-table :data="appeals" v-loading="loading" stripe>
      <el-table-column prop="appealCode" label="申诉编号" width="180" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="(getStatusConfig(row.status).type as any)" size="small">
            {{ getStatusConfig(row.status).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitterName" label="申诉人" width="110">
        <template #default="{ row }">{{ row.submitterName || `用户#${row.submitterUserId}` }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="申诉理由" min-width="240" show-overflow-tooltip />
      <el-table-column prop="expectedAdjustment" label="期望调整" width="100" align="right">
        <template #default="{ row }">
          {{ row.expectedAdjustment != null ? row.expectedAdjustment : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">
          {{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="openApprove(row)">
            <CheckCircle class="w-4 h-4 mr-1" />通过
          </el-button>
          <el-button link type="danger" @click="openReject(row)">
            <XCircle class="w-4 h-4 mr-1" />驳回
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <InspEmptyState v-if="!loading && appeals.length === 0" message="暂无待审申诉" />

    <!-- 通过对话框 -->
    <el-dialog v-model="approveDialog" title="审核通过 — 申诉" width="500px">
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="申诉编号">
          <span class="text-gray-700">{{ currentAppeal?.appealCode }}</span>
        </el-form-item>
        <el-form-item label="实际调整">
          <el-input-number v-model="approveForm.finalAdjustment" :precision="2" :step="0.5" class="w-full" />
          <div class="text-xs text-gray-500 mt-1">
            扣分项将按此值调整 (DEDUCTION 模式: 退回扣分; 其他: 直接覆盖)
          </div>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="approveForm.comment" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialog = false">取消</el-button>
        <el-button type="success" @click="submitApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectDialog" title="审核驳回 — 申诉" width="500px">
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="申诉编号">
          <span class="text-gray-700">{{ currentAppeal?.appealCode }}</span>
        </el-form-item>
        <el-form-item label="驳回理由" required>
          <el-input v-model="rejectForm.comment" type="textarea" :rows="4" placeholder="必填, 让申诉人理解为什么被驳回" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>
