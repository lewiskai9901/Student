/**
 * V7 检查平台 - 整改案例工作流 Composable
 *
 * 职责：
 * 1. 加载整改案例及构建时间线
 * 2. SLA 倒计时 (剩余时间 / 超期天数)
 * 3. 基于当前状态计算可用操作
 * 4. 整改生命周期操作 (assign → startWork → submit → verify → close)
 */
import { ref, computed, watch, onUnmounted, type Ref } from 'vue'
import type { CorrectiveCase } from '@/types/insp/corrective'
import type { CaseStatus } from '@/types/insp/enums'
import {
  getCase,
  assignCase as apiAssignCase,
  startWork as apiStartWork,
  submitCorrection as apiSubmitCorrection,
  verifyCase as apiVerifyCase,
  rejectCase as apiRejectCase,
  closeCase as apiCloseCase,
  escalateCase as apiEscalateCase,
} from '@/api/insp/correctiveCase'

type CaseAction = 'assign' | 'startWork' | 'submitCorrection' | 'verify' | 'reject' | 'close' | 'escalate'

interface TimelineEntry {
  timestamp: string
  title: string
  description: string
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

/** Status transition map: which actions are available from each status */
const STATUS_ACTIONS: Record<CaseStatus, CaseAction[]> = {
  OPEN: ['assign', 'escalate'],
  ASSIGNED: ['startWork', 'escalate'],
  IN_PROGRESS: ['submitCorrection', 'escalate'],
  SUBMITTED: ['verify', 'reject'],
  VERIFIED: ['close'],
  REJECTED: ['startWork', 'escalate'],
  CLOSED: [],
  ESCALATED: ['assign'],
}

export function useCorrectiveCase(caseId: Ref<number | null>) {
  // ==================== State ====================

  const currentCase = ref<CorrectiveCase | null>(null)
  const isLoading = ref(false)
  const timeline = ref<TimelineEntry[]>([])
  const slaCountdown = ref<string>('')

  let slaTimer: ReturnType<typeof setInterval> | null = null

  // ==================== Computed ====================

  /** Available actions based on the current case status */
  const availableActions = computed<CaseAction[]>(() => {
    if (!currentCase.value) return []
    return STATUS_ACTIONS[currentCase.value.status] || []
  })

  // ==================== Load Case ====================

  /** Load a corrective case and build its timeline */
  async function loadCase(id: number) {
    isLoading.value = true
    try {
      const caseData = await getCase(id)
      currentCase.value = caseData
      timeline.value = buildTimeline(caseData)
      startSlaCountdown()
    } finally {
      isLoading.value = false
    }
  }

  // Watch caseId for auto-loading
  watch(caseId, (newId) => {
    if (newId) {
      loadCase(newId)
    } else {
      currentCase.value = null
      timeline.value = []
      slaCountdown.value = ''
      stopSlaCountdown()
    }
  }, { immediate: true })

  // ==================== SLA Countdown ====================

  function startSlaCountdown() {
    stopSlaCountdown()
    updateSlaCountdown()
    slaTimer = setInterval(updateSlaCountdown, 60_000) // Update every minute
  }

  function stopSlaCountdown() {
    if (slaTimer) {
      clearInterval(slaTimer)
      slaTimer = null
    }
  }

  /** Update SLA countdown display string */
  function updateSlaCountdown() {
    if (!currentCase.value?.deadline) {
      slaCountdown.value = ''
      return
    }

    // Cases that are already closed/verified don't need a countdown
    if (currentCase.value.status === 'CLOSED' || currentCase.value.status === 'VERIFIED') {
      slaCountdown.value = ''
      stopSlaCountdown()
      return
    }

    const deadline = new Date(currentCase.value.deadline)
    const now = new Date()
    const diffMs = deadline.getTime() - now.getTime()

    if (diffMs <= 0) {
      // Overdue
      const overdueDays = Math.ceil(Math.abs(diffMs) / (1000 * 60 * 60 * 24))
      slaCountdown.value = `超期 ${overdueDays}天`
    } else {
      const totalMinutes = Math.floor(diffMs / (1000 * 60))
      const days = Math.floor(totalMinutes / (60 * 24))
      const hours = Math.floor((totalMinutes % (60 * 24)) / 60)

      if (days > 0) {
        slaCountdown.value = `剩余 ${days}天 ${hours}小时`
      } else if (hours > 0) {
        const mins = totalMinutes % 60
        slaCountdown.value = `剩余 ${hours}小时 ${mins}分钟`
      } else {
        slaCountdown.value = `剩余 ${totalMinutes}分钟`
      }
    }
  }

  // ==================== Action Methods ====================

  /** Assign the case to a responsible person */
  async function assignCase(assigneeId: number, assigneeName: string) {
    if (!currentCase.value) return
    isLoading.value = true
    try {
      currentCase.value = await apiAssignCase(currentCase.value.id, {
        assigneeId,
        assigneeName,
      })
      timeline.value = buildTimeline(currentCase.value)
    } finally {
      isLoading.value = false
    }
  }

  /** Start working on the assigned case */
  async function startWork() {
    if (!currentCase.value) return
    isLoading.value = true
    try {
      currentCase.value = await apiStartWork(currentCase.value.id)
      timeline.value = buildTimeline(currentCase.value)
    } finally {
      isLoading.value = false
    }
  }

  /** Submit correction work with note and evidence */
  async function submitCorrection(data: { correctionNote: string; evidenceIds: number[] }) {
    if (!currentCase.value) return
    isLoading.value = true
    try {
      currentCase.value = await apiSubmitCorrection(currentCase.value.id, {
        correctionNote: data.correctionNote,
        evidenceIds: data.evidenceIds,
      })
      timeline.value = buildTimeline(currentCase.value)
    } finally {
      isLoading.value = false
    }
  }

  /** Verify the correction (pass or fail with notes) */
  async function verifyCase(passed: boolean, notes: string) {
    if (!currentCase.value) return
    isLoading.value = true
    try {
      if (passed) {
        currentCase.value = await apiVerifyCase(currentCase.value.id, {
          verifierName: '',
          note: notes,
        })
      } else {
        currentCase.value = await apiRejectCase(currentCase.value.id, {
          verifierName: '',
          reason: notes,
        })
      }
      timeline.value = buildTimeline(currentCase.value)
    } finally {
      isLoading.value = false
    }
  }

  /** Reject the submitted correction */
  async function rejectCase(reason: string) {
    if (!currentCase.value) return
    isLoading.value = true
    try {
      currentCase.value = await apiRejectCase(currentCase.value.id, {
        verifierName: '',
        reason,
      })
      timeline.value = buildTimeline(currentCase.value)
    } finally {
      isLoading.value = false
    }
  }

  /** Close a verified case */
  async function closeCase() {
    if (!currentCase.value) return
    isLoading.value = true
    try {
      currentCase.value = await apiCloseCase(currentCase.value.id)
      timeline.value = buildTimeline(currentCase.value)
      stopSlaCountdown()
    } finally {
      isLoading.value = false
    }
  }

  /** Escalate the case to a higher level */
  async function escalateCase() {
    if (!currentCase.value) return
    isLoading.value = true
    try {
      currentCase.value = await apiEscalateCase(currentCase.value.id)
      timeline.value = buildTimeline(currentCase.value)
    } finally {
      isLoading.value = false
    }
  }

  // ==================== Timeline Builder ====================

  /**
   * Build timeline entries from a corrective case's lifecycle data.
   * Each status transition and key event becomes a timeline entry.
   */
  function buildTimeline(caseData: CorrectiveCase): TimelineEntry[] {
    const entries: TimelineEntry[] = []

    // 1. Case created
    entries.push({
      timestamp: caseData.createdAt,
      title: '创建整改案例',
      description: `问题描述：${caseData.issueDescription}`,
      type: 'info',
    })

    // 2. Case assigned
    if (caseData.assigneeName) {
      entries.push({
        timestamp: caseData.updatedAt || caseData.createdAt,
        title: '分配责任人',
        description: `分配给：${caseData.assigneeName}`,
        type: 'primary',
      })
    }

    // 3. Correction submitted
    if (caseData.correctedAt) {
      entries.push({
        timestamp: caseData.correctedAt,
        title: '提交整改',
        description: caseData.correctionNote || '已提交整改结果',
        type: 'warning',
      })
    }

    // 4. Verification
    if (caseData.verifiedAt) {
      const isVerified = caseData.status === 'VERIFIED' || caseData.status === 'CLOSED'
      entries.push({
        timestamp: caseData.verifiedAt,
        title: isVerified ? '验证通过' : '验证驳回',
        description: caseData.verificationNote || (isVerified ? '整改通过验证' : '整改未通过验证'),
        type: isVerified ? 'success' : 'danger',
      })
    }

    // 5. Closed
    if (caseData.status === 'CLOSED') {
      entries.push({
        timestamp: caseData.updatedAt || caseData.verifiedAt || caseData.createdAt,
        title: '案例关闭',
        description: '整改流程已完成',
        type: 'success',
      })
    }

    // 6. Escalated
    if (caseData.status === 'ESCALATED') {
      entries.push({
        timestamp: caseData.updatedAt || caseData.createdAt,
        title: '案例升级',
        description: `升级至第 ${caseData.escalationLevel} 级`,
        type: 'danger',
      })
    }

    // Sort by timestamp ascending
    entries.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())

    return entries
  }

  // ==================== Cleanup ====================

  onUnmounted(() => stopSlaCountdown())

  return {
    // State
    currentCase,
    isLoading,
    timeline,
    slaCountdown,
    availableActions,
    // Actions
    loadCase,
    assignCase,
    startWork,
    submitCorrection,
    verifyCase,
    rejectCase,
    closeCase,
    escalateCase,
  }
}
