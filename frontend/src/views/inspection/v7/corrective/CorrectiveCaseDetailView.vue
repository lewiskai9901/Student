<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, UserPlus, PlayCircle, Send, Check, X, AlertTriangle, Lock } from 'lucide-vue-next'
import { useInspCorrectiveStore } from '@/stores/insp/inspCorrectiveStore'
import { CaseStatusConfig, CasePriorityConfig, type CaseStatus, type CasePriority } from '@/types/insp/enums'

const route = useRoute()
const router = useRouter()
const store = useInspCorrectiveStore()

const caseId = Number(route.params.id)
const loading = ref(false)

// Dialogs
const assignDialogVisible = ref(false)
const correctionDialogVisible = ref(false)
const verifyDialogVisible = ref(false)
const rejectDialogVisible = ref(false)

const assignForm = ref({ assigneeId: undefined as number | undefined, assigneeName: '' })
const correctionForm = ref({ correctionNote: '', evidenceIds: [] as number[] })
const verifyForm = ref({ verifierName: '', note: '' })
const rejectForm = ref({ verifierName: '', reason: '' })

const currentCase = computed(() => store.currentCase)

function getStatusConfig(status: CaseStatus) {
  return CaseStatusConfig[status] || { label: status, type: 'info' }
}

function getPriorityConfig(priority: CasePriority) {
  return CasePriorityConfig[priority] || { label: priority, type: 'info' }
}

function isOverdue(): boolean {
  const c = currentCase.value
  if (!c?.deadline) return false
  if (c.status === 'CLOSED' || c.status === 'VERIFIED') return false
  return new Date() > new Date(c.deadline)
}

async function loadData() {
  loading.value = true
  try {
    await store.fetchCase(caseId)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// Lifecycle actions
async function handleAssign() {
  if (!assignForm.value.assigneeId || !assignForm.value.assigneeName) {
    ElMessage.warning('请填写责任人信息')
    return
  }
  try {
    await store.assignCase(caseId, {
      assigneeId: assignForm.value.assigneeId,
      assigneeName: assignForm.value.assigneeName,
    })
    ElMessage.success('分配成功')
    assignDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '分配失败')
  }
}

async function handleStartWork() {
  try {
    await store.startWork(caseId)
    ElMessage.success('已开始整改')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleSubmitCorrection() {
  if (!correctionForm.value.correctionNote) {
    ElMessage.warning('请填写整改说明')
    return
  }
  try {
    await store.submitCorrection(caseId, correctionForm.value)
    ElMessage.success('整改已提交')
    correctionDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  }
}

async function handleVerify() {
  try {
    await store.verifyCase(caseId, verifyForm.value)
    ElMessage.success('验证通过')
    verifyDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleReject() {
  if (!rejectForm.value.reason) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await store.rejectCase(caseId, rejectForm.value)
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleClose() {
  try {
    await ElMessageBox.confirm('确定关闭此案例？', '确认', { type: 'info' })
    await store.closeCase(caseId)
    ElMessage.success('已关闭')
    loadData()
  } catch { /* cancelled */ }
}

async function handleEscalate() {
  try {
    await ElMessageBox.confirm('确定升级此案例？', '确认升级', { type: 'warning' })
    await store.escalateCase(caseId)
    ElMessage.success('已升级')
    loadData()
  } catch { /* cancelled */ }
}

onMounted(() => loadData())
</script>

<template>
  <div class="p-5" v-loading="loading">
    <!-- Header -->
    <div class="flex items-center gap-3 mb-5">
      <el-button @click="router.push('/inspection/v7/corrective')" text>
        <ArrowLeft class="w-4 h-4 mr-1" />返回
      </el-button>
      <h2 class="text-lg font-semibold">{{ currentCase?.caseCode || '整改详情' }}</h2>
      <el-tag v-if="currentCase" :type="getStatusConfig(currentCase.status).type" class="ml-2">
        {{ getStatusConfig(currentCase.status).label }}
      </el-tag>
      <el-tag v-if="currentCase" :type="getPriorityConfig(currentCase.priority).type" class="ml-1">
        {{ getPriorityConfig(currentCase.priority).label }}
      </el-tag>
      <el-tag v-if="isOverdue()" type="danger" class="ml-1">逾期</el-tag>
    </div>

    <template v-if="currentCase">
      <!-- Action Buttons -->
      <div class="flex gap-2 mb-5">
        <el-button v-if="currentCase.status === 'OPEN' || currentCase.status === 'REJECTED'"
                   type="primary" @click="assignDialogVisible = true">
          <UserPlus class="w-4 h-4 mr-1" />分配责任人
        </el-button>
        <el-button v-if="currentCase.status === 'ASSIGNED'"
                   type="warning" @click="handleStartWork">
          <PlayCircle class="w-4 h-4 mr-1" />开始整改
        </el-button>
        <el-button v-if="currentCase.status === 'IN_PROGRESS'"
                   type="primary" @click="correctionDialogVisible = true">
          <Send class="w-4 h-4 mr-1" />提交整改
        </el-button>
        <el-button v-if="currentCase.status === 'SUBMITTED'"
                   type="success" @click="verifyDialogVisible = true">
          <Check class="w-4 h-4 mr-1" />验证通过
        </el-button>
        <el-button v-if="currentCase.status === 'SUBMITTED'"
                   type="danger" @click="rejectDialogVisible = true">
          <X class="w-4 h-4 mr-1" />驳回
        </el-button>
        <el-button v-if="currentCase.status === 'VERIFIED'"
                   type="success" @click="handleClose">
          <Lock class="w-4 h-4 mr-1" />关闭
        </el-button>
        <el-button v-if="currentCase.status !== 'CLOSED' && currentCase.status !== 'ESCALATED'"
                   type="warning" @click="handleEscalate">
          <AlertTriangle class="w-4 h-4 mr-1" />升级
        </el-button>
      </div>

      <!-- Info Cards -->
      <div class="grid grid-cols-2 gap-4 mb-5">
        <!-- Basic Info -->
        <el-card shadow="never">
          <template #header><span class="font-medium">基本信息</span></template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="案例编号">{{ currentCase.caseCode }}</el-descriptions-item>
            <el-descriptions-item label="检查对象">{{ currentCase.targetName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="问题描述">{{ currentCase.issueDescription }}</el-descriptions-item>
            <el-descriptions-item label="要求措施">{{ currentCase.requiredAction || '-' }}</el-descriptions-item>
            <el-descriptions-item label="截止时间">
              <span :class="{ 'text-red-500': isOverdue() }">
                {{ currentCase.deadline ? new Date(currentCase.deadline).toLocaleString() : '-' }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="升级层级">{{ currentCase.escalationLevel }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- Assignment & Correction -->
        <el-card shadow="never">
          <template #header><span class="font-medium">整改信息</span></template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="责任人">{{ currentCase.assigneeName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="整改说明">{{ currentCase.correctionNote || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ currentCase.correctedAt ? new Date(currentCase.correctedAt).toLocaleString() : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="验证人">{{ currentCase.verifierName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="验证说明">{{ currentCase.verificationNote || '-' }}</el-descriptions-item>
            <el-descriptions-item label="验证时间">
              {{ currentCase.verifiedAt ? new Date(currentCase.verifiedAt).toLocaleString() : '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>
    </template>

    <!-- Assign Dialog -->
    <el-dialog v-model="assignDialogVisible" title="分配责任人" width="400px">
      <el-form label-width="80px">
        <el-form-item label="责任人ID">
          <el-input-number v-model="assignForm.assigneeId" :min="1" class="w-full" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="assignForm.assigneeName" placeholder="请输入责任人姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- Correction Dialog -->
    <el-dialog v-model="correctionDialogVisible" title="提交整改" width="500px">
      <el-form label-width="80px">
        <el-form-item label="整改说明">
          <el-input v-model="correctionForm.correctionNote" type="textarea" :rows="4"
                    placeholder="请描述整改措施和完成情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitCorrection">提交</el-button>
      </template>
    </el-dialog>

    <!-- Verify Dialog -->
    <el-dialog v-model="verifyDialogVisible" title="验证通过" width="400px">
      <el-form label-width="80px">
        <el-form-item label="验证人">
          <el-input v-model="verifyForm.verifierName" placeholder="请输入验证人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="verifyForm.note" type="textarea" :rows="3" placeholder="验证备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleVerify">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="驳回" width="400px">
      <el-form label-width="80px">
        <el-form-item label="验证人">
          <el-input v-model="rejectForm.verifierName" placeholder="请输入验证人姓名" />
        </el-form-item>
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>
