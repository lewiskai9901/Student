<template>
  <div class="corrective-action-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">V6整改管理</span>
          <div class="actions">
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 120px; margin-right: 10px"
                       @change="loadActions">
              <el-option v-for="s in statuses" :key="s.code" :label="s.name" :value="s.code" />
            </el-select>
            <el-button type="warning" @click="loadOverdue">查看逾期</el-button>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-statistic title="待整改" :value="stats.pending">
            <template #suffix>
              <el-tag type="warning" size="small">项</el-tag>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="已提交" :value="stats.submitted">
            <template #suffix>
              <el-tag type="primary" size="small">项</el-tag>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="已验收" :value="stats.verified">
            <template #suffix>
              <el-tag type="success" size="small">项</el-tag>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="已驳回" :value="stats.rejected">
            <template #suffix>
              <el-tag type="danger" size="small">项</el-tag>
            </template>
          </el-statistic>
        </el-col>
      </el-row>

      <!-- 整改列表 -->
      <el-table :data="actions" stripe border v-loading="loading">
        <el-table-column prop="actionCode" label="整改单号" width="150" />
        <el-table-column prop="issueDescription" label="问题描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requiredAction" label="整改要求" min-width="150" show-overflow-tooltip />
        <el-table-column prop="assigneeName" label="责任人" width="100" />
        <el-table-column label="截止日期" width="120">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.overdue }">{{ row.deadline || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ row.statusName }}
            </el-tag>
            <el-tag v-if="row.overdue" type="danger" size="small" class="overdue-tag">逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING' || row.status === 'REJECTED'"
                       link type="success" size="small" @click="showSubmitDialog(row)">提交整改</el-button>
            <el-button v-if="row.status === 'SUBMITTED'"
                       link type="success" size="small" @click="showVerifyDialog(row, true)">验收</el-button>
            <el-button v-if="row.status === 'SUBMITTED'"
                       link type="danger" size="small" @click="showVerifyDialog(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="整改详情" width="600px">
      <el-descriptions :column="2" border v-if="currentAction">
        <el-descriptions-item label="整改单号">{{ currentAction.actionCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentAction.status)">{{ currentAction.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">{{ currentAction.issueDescription }}</el-descriptions-item>
        <el-descriptions-item label="整改要求" :span="2">{{ currentAction.requiredAction }}</el-descriptions-item>
        <el-descriptions-item label="责任人">{{ currentAction.assigneeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="截止日期">{{ currentAction.deadline || '-' }}</el-descriptions-item>
        <el-descriptions-item label="整改说明" :span="2" v-if="currentAction.correctionNote">
          {{ currentAction.correctionNote }}
        </el-descriptions-item>
        <el-descriptions-item label="整改时间" v-if="currentAction.correctedAt">
          {{ currentAction.correctedAt }}
        </el-descriptions-item>
        <el-descriptions-item label="验收人" v-if="currentAction.verifierName">
          {{ currentAction.verifierName }}
        </el-descriptions-item>
        <el-descriptions-item label="验收说明" :span="2" v-if="currentAction.verificationNote">
          {{ currentAction.verificationNote }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 提交整改对话框 -->
    <el-dialog v-model="submitDialogVisible" title="提交整改" width="500px">
      <el-form :model="submitForm" label-width="80px">
        <el-form-item label="整改说明" required>
          <el-input v-model="submitForm.correctionNote" type="textarea" rows="4"
                    placeholder="请描述整改情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doSubmit" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 验收对话框 -->
    <el-dialog v-model="verifyDialogVisible" :title="isVerify ? '验收通过' : '验收驳回'" width="500px">
      <el-form :model="verifyForm" label-width="80px">
        <el-form-item label="验收说明">
          <el-input v-model="verifyForm.verificationNote" type="textarea" rows="3"
                    :placeholder="isVerify ? '可选：验收通过说明' : '请填写驳回原因'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyDialogVisible = false">取消</el-button>
        <el-button :type="isVerify ? 'success' : 'danger'" @click="doVerify" :loading="submitting">
          {{ isVerify ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  correctiveActionApi,
  type CorrectiveAction, type CorrectiveActionStatus, type StatusInfo, type CorrectiveStats
} from '@/api/v6CorrectiveAction'

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)

const actions = ref<CorrectiveAction[]>([])
const statuses = ref<StatusInfo[]>([])
const stats = ref<CorrectiveStats>({ pending: 0, submitted: 0, verified: 0, rejected: 0, total: 0 })
const filterStatus = ref<CorrectiveActionStatus | ''>('')

const detailDialogVisible = ref(false)
const submitDialogVisible = ref(false)
const verifyDialogVisible = ref(false)
const currentAction = ref<CorrectiveAction | null>(null)
const isVerify = ref(true)

const submitForm = reactive({
  correctionNote: ''
})

const verifyForm = reactive({
  verificationNote: ''
})

const getStatusTagType = (status: string) => {
  const types: Record<string, string> = {
    'PENDING': 'warning',
    'SUBMITTED': 'primary',
    'VERIFIED': 'success',
    'REJECTED': 'danger',
    'CANCELLED': 'info'
  }
  return types[status] || ''
}

const loadStatuses = async () => {
  try {
    const res = await correctiveActionApi.getStatuses()
    statuses.value = res.data
  } catch (error) {
    console.error('加载状态失败:', error)
  }
}

const loadActions = async () => {
  try {
    loading.value = true
    // 这里简化处理，实际应该根据当前用户权限获取对应数据
    // 暂时获取所有逾期的整改作为演示
    if (filterStatus.value) {
      // 需要项目ID，这里暂时留空
      actions.value = []
    } else {
      const res = await correctiveActionApi.getOverdue()
      actions.value = res.data
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadOverdue = async () => {
  try {
    loading.value = true
    filterStatus.value = ''
    const res = await correctiveActionApi.getOverdue()
    actions.value = res.data
  } catch (error) {
    console.error('加载逾期数据失败:', error)
  } finally {
    loading.value = false
  }
}

const viewDetail = (action: CorrectiveAction) => {
  currentAction.value = action
  detailDialogVisible.value = true
}

const showSubmitDialog = (action: CorrectiveAction) => {
  currentAction.value = action
  submitForm.correctionNote = ''
  submitDialogVisible.value = true
}

const showVerifyDialog = (action: CorrectiveAction, verify: boolean) => {
  currentAction.value = action
  isVerify.value = verify
  verifyForm.verificationNote = ''
  verifyDialogVisible.value = true
}

const doSubmit = async () => {
  if (!currentAction.value) return
  if (!submitForm.correctionNote.trim()) {
    ElMessage.warning('请填写整改说明')
    return
  }

  try {
    submitting.value = true
    await correctiveActionApi.submit(currentAction.value.id, submitForm.correctionNote)
    ElMessage.success('提交成功')
    submitDialogVisible.value = false
    await loadActions()
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const doVerify = async () => {
  if (!currentAction.value) return
  const user = authStore.user

  try {
    submitting.value = true
    if (isVerify.value) {
      await correctiveActionApi.verify(
        currentAction.value.id,
        user?.id || 0,
        user?.nickname || user?.username || '',
        verifyForm.verificationNote
      )
      ElMessage.success('验收通过')
    } else {
      if (!verifyForm.verificationNote.trim()) {
        ElMessage.warning('请填写驳回原因')
        return
      }
      await correctiveActionApi.reject(
        currentAction.value.id,
        user?.id || 0,
        user?.nickname || user?.username || '',
        verifyForm.verificationNote
      )
      ElMessage.success('已驳回')
    }
    verifyDialogVisible.value = false
    await loadActions()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadStatuses()
  loadActions()
})
</script>

<style scoped>
.corrective-action-management {
  padding: 20px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: 500;
}

.stats-row {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.text-danger {
  color: #f56c6c;
}

.overdue-tag {
  margin-left: 4px;
}
</style>
