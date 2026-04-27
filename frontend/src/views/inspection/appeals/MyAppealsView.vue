<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Eye, RotateCcw } from 'lucide-vue-next'
import * as appealApi from '@/api/inspection/appeal'
import type { InspAppeal } from '@/types/insp/appeal'
import { AppealStatusConfig, type AppealStatus } from '@/types/insp/enums'
import InspEmptyState from '../shared/InspEmptyState.vue'

const loading = ref(false)
const appeals = ref<InspAppeal[]>([])

async function loadData() {
  loading.value = true
  try {
    appeals.value = await appealApi.getMyAppeals()
  } catch (e: any) {
    ElMessage.error(e.message || '加载申诉失败')
  } finally {
    loading.value = false
  }
}

async function handleWithdraw(appeal: InspAppeal) {
  if (appeal.status !== 'PENDING') {
    ElMessage.warning('只有待审核状态的申诉可以撤回')
    return
  }
  try {
    await ElMessageBox.confirm(`确定撤回申诉「${appeal.appealCode}」?`, '确认撤回', { type: 'warning' })
    await appealApi.withdrawAppeal(appeal.id)
    ElMessage.success('已撤回')
    loadData()
  } catch { /* cancelled */ }
}

function getStatusConfig(status: AppealStatus) {
  return AppealStatusConfig[status] || { label: status, type: 'info', color: '#909399' }
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5">
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-lg font-semibold">我的申诉</h2>
      <span class="text-sm text-gray-500">共 {{ appeals.length }} 条</span>
    </div>

    <el-table :data="appeals" v-loading="loading" stripe>
      <el-table-column prop="appealCode" label="申诉编号" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(getStatusConfig(row.status).type as any)" size="small">
            {{ getStatusConfig(row.status).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="申诉理由" min-width="220" show-overflow-tooltip />
      <el-table-column prop="expectedAdjustment" label="期望调整" width="100" align="right">
        <template #default="{ row }">
          {{ row.expectedAdjustment != null ? row.expectedAdjustment : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="finalAdjustment" label="实际调整" width="100" align="right">
        <template #default="{ row }">
          <span v-if="row.status === 'APPROVED'" class="text-green-600 font-medium">
            {{ row.finalAdjustment != null ? row.finalAdjustment : '-' }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="reviewerName" label="审核员" width="100">
        <template #default="{ row }">
          {{ row.reviewerName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="reviewerComment" label="审核意见" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.reviewerComment || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">
          {{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="warning" @click="handleWithdraw(row)">
            <RotateCcw class="w-4 h-4" />
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <InspEmptyState v-if="!loading && appeals.length === 0" message="暂无申诉记录" />
  </div>
</template>
