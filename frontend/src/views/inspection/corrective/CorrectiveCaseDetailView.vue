<script setup lang="ts">
import type { LongId } from '@/types/common'
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, UserPlus, PlayCircle, Send, Check, X, AlertTriangle, Lock } from 'lucide-vue-next'
import { useInspCorrectiveStore } from '@/stores/inspection/inspCorrectiveStore'
import { CaseStatusConfig, CasePriorityConfig, type CaseStatus, type CasePriority } from '@/types/insp/enums'
import StatusTimeline from '@/views/inspection/shared/StatusTimeline.vue'

const route = useRoute()
const router = useRouter()
const store = useInspCorrectiveStore()

const caseId = route.params.id as string
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

// V110: 解析 explain_trace_json
interface TraceEntry { layer: string; rule: string; input: string; output: string }
const parsedTrace = computed<TraceEntry[]>(() => {
  const raw = currentCase.value?.explainTraceJson
  if (!raw) return []
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    const arr = Array.isArray(obj) ? obj : (obj?.trace || [])
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

function traceLayerLabel(layer: string): string {
  const map: Record<string, string> = {
    normalize:  'L1 标准化',
    itemRule:   'L2 检查项规则',
    threshold:  'L3 阈值判定',
    recurrence: 'L4 复发增强',
    policy:     '项目策略',
  }
  return map[layer] || layer
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

// ── Timeline ──
const caseTimeline = computed(() => {
  if (!currentCase.value) return []
  const c = currentCase.value
  const status = c.status as string
  return [
    {
      code: 'OPEN', label: '待分配责任人',
      hint: '管理员需指定整改责任人',
      at: c.createdAt,
      by: c.createdBy ? `创建于 #${c.createdBy}` : null,
    },
    {
      code: 'ASSIGNED', label: '已分配',
      hint: status === 'ASSIGNED' ? `${c.assigneeName || '责任人'} 待开始整改` : '',
      at: c.assigneeId ? c.updatedAt : null,
      by: c.assigneeName ? `分配给 ${c.assigneeName}` : null,
    },
    {
      code: 'IN_PROGRESS', label: '整改中',
      hint: status === 'IN_PROGRESS' ? `${c.assigneeName || '责任人'} 正在执行` : '',
    },
    {
      code: 'SUBMITTED', label: '已提交整改',
      hint: status === 'SUBMITTED' ? '等待验证' : '',
      at: c.correctedAt,
    },
    {
      code: 'VERIFIED', label: '验证通过',
      at: c.verifiedAt, by: c.verifierName,
    },
    {
      code: 'CLOSED', label: '已关闭',
      at: status === 'CLOSED' ? c.updatedAt : null,
    },
  ].filter(s => {
    // 如果当前是 REJECTED, 则在 SUBMITTED 后插入 REJECTED 节点
    if (status === 'REJECTED' && s.code === 'VERIFIED') return false
    return true
  })
})

const timelineCurrentCode = computed(() => {
  if (!currentCase.value) return 'OPEN'
  const status = currentCase.value.status as string
  if (status === 'REJECTED') return 'IN_PROGRESS'  // 驳回 = 回到待整改
  if (status === 'ESCALATED') return 'IN_PROGRESS'
  return status
})

onMounted(() => loadData())
</script>

<template>
  <div class="insp-shell case-detail" v-loading="loading">
    <!-- Header (Audit Hub style) -->
    <header class="cd-head">
      <button class="cd-back" @click="router.push('/inspection/corrective')" title="返回">
        <ArrowLeft :size="14" />
      </button>
      <div class="cd-head__lead">
        <span class="insp-eyebrow">整改案例 · {{ currentCase?.caseCode || '加载中' }}</span>
        <h1 class="cd-title">{{ currentCase?.issueDescription || '整改详情' }}</h1>
        <div class="cd-meta">
          <span v-if="currentCase" class="insp-chip"
                :class="`insp-chip--${({OPEN:'pending',ASSIGNED:'info',IN_PROGRESS:'warn',SUBMITTED:'info',VERIFIED:'pass',REJECTED:'fail',CLOSED:'pass',ESCALATED:'fail'} as any)[currentCase.status]}`">
            {{ getStatusConfig(currentCase.status).label }}
          </span>
          <span v-if="currentCase" class="insp-chip"
                :class="`insp-chip--${({LOW:'pending',MEDIUM:'info',HIGH:'warn',CRITICAL:'fail'} as any)[currentCase.priority]}`">
            优先级 · {{ getPriorityConfig(currentCase.priority).label }}
          </span>
          <span v-if="isOverdue()" class="insp-chip insp-chip--fail">已逾期</span>
          <span v-if="currentCase?.escalationLevel" class="insp-stamp">升级 L{{ currentCase.escalationLevel }}</span>
        </div>
      </div>
    </header>

    <template v-if="currentCase">
      <!-- Status Timeline (新加) -->
      <section class="cd-card cd-timeline-card">
        <header class="cd-card__head">
          <span class="cd-card__title">流转进度</span>
          <span class="cd-card__hint">当前: {{ getStatusConfig(currentCase.status).label }}</span>
        </header>
        <div class="cd-card__body">
          <StatusTimeline :steps="caseTimeline as any" :current="timelineCurrentCode" />
        </div>
      </section>

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

        <!-- V110: 引擎决策审计链 -->
        <el-card v-if="currentCase.suggestedBySystem === 1 || currentCase.explainTraceJson"
                 shadow="never" class="trace-card">
          <template #header>
            <div class="trace-header">
              <span class="font-medium">引擎判定追溯</span>
              <span v-if="currentCase.suggestedBySystem === 1" class="trace-tag">系统建议</span>
              <span v-if="currentCase.severityScore != null" class="trace-score">
                严重度 {{ (currentCase.severityScore * 100).toFixed(0) }}%
              </span>
            </div>
          </template>
          <div v-if="currentCase.suggestionReason" class="trace-reason">
            {{ currentCase.suggestionReason }}
          </div>
          <ul v-if="parsedTrace.length" class="trace-list">
            <li v-for="(t, i) in parsedTrace" :key="i" class="trace-row">
              <span class="trace-step">{{ i + 1 }}</span>
              <div class="trace-body">
                <div class="trace-line">
                  <span class="trace-layer">{{ traceLayerLabel(t.layer) }}</span>
                  <span class="trace-rule">{{ t.rule }}</span>
                </div>
                <div class="trace-io">
                  <span class="trace-input">{{ t.input }}</span>
                  <span class="trace-arrow">→</span>
                  <span class="trace-output">{{ t.output }}</span>
                </div>
              </div>
            </li>
          </ul>
          <div v-else class="trace-empty">未记录决策链 (可能为手动建单)</div>
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

<style scoped>
.case-detail { padding: 12px 16px; }

/* V110 引擎追溯 */
.trace-card { border-color: #ddd6fe !important; }
.trace-header { display: flex; align-items: center; gap: 10px; }
.trace-tag {
  background: #ede9fe; color: #6d28d9;
  padding: 2px 8px; border-radius: 4px;
  font-size: 11px; font-weight: 600;
}
.trace-score {
  font-size: 11px; color: #6b7280;
  margin-left: auto;
}
.trace-reason {
  background: #f5f3ff; border: 1px solid #e9d5ff;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 13px; color: #4c1d95;
  margin-bottom: 12px;
}
.trace-list { list-style: none; padding: 0; margin: 0; }
.trace-row {
  display: flex; gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #f3f4f6;
}
.trace-row:last-child { border-bottom: none; }
.trace-step {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px;
  background: #ede9fe; color: #6d28d9;
  border-radius: 50%;
  font-size: 11px; font-weight: 600;
  flex-shrink: 0;
}
.trace-body { flex: 1; min-width: 0; }
.trace-line { display: flex; gap: 8px; align-items: center; }
.trace-layer { font-size: 12px; color: #6b7280; }
.trace-rule {
  font-size: 12px; color: #1f2937; font-weight: 500;
  font-family: ui-monospace, monospace;
  background: #f9fafb; padding: 1px 6px; border-radius: 3px;
}
.trace-io {
  font-size: 11px; color: #6b7280;
  margin-top: 2px;
  font-family: ui-monospace, monospace;
  word-break: break-all;
}
.trace-input { color: #94a3b8; }
.trace-arrow { color: #cbd5e1; margin: 0 4px; }
.trace-output { color: #1e40af; font-weight: 500; }
.trace-empty { padding: 12px; color: #9ca3af; font-size: 12px; text-align: center; }

.cd-head {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 12px 16px;
  margin-bottom: 10px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.cd-back {
  display: inline-flex;
  align-items: center; justify-content: center;
  width: 28px; height: 28px;
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-strong);
  border-radius: var(--insp-radius-sm);
  color: var(--insp-ink-tertiary);
  cursor: pointer;
  transition: all var(--insp-t-fast);
  margin-top: 2px;
}
.cd-back:hover { color: var(--insp-accent); border-color: var(--insp-accent); }
.cd-head__lead { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.cd-title {
  font-size: 16px; font-weight: 700;
  margin: 0;
  color: var(--insp-ink-primary);
}
.cd-meta {
  display: flex; align-items: center; gap: 6px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.cd-card {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  margin-bottom: 10px;
}
.cd-card__head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
}
.cd-card__title {
  font-size: 13px; font-weight: 600;
  color: var(--insp-ink-primary);
}
.cd-card__hint {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}
.cd-card__body { padding: 12px 14px; }

.cd-timeline-card {
  background: var(--insp-bg-surface);
}
</style>
