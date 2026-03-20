/**
 * V7 检查平台 - 任务执行流 Composable
 *
 * 职责：
 * 1. 加载任务及其提交列表
 * 2. 任务生命周期管理 (claim → start → navigate → complete → submit)
 * 3. 提交导航 (上一个/下一个/跳转)
 * 4. 进度追踪
 */
import { ref, computed } from 'vue'
import type { InspTask, InspSubmission, CompleteSubmissionRequest } from '@/types/insp/project'
import {
  getTask,
  claimTask as apiClaimTask,
  startTask as apiStartTask,
  submitTask as apiSubmitTask,
} from '@/api/insp/task'
import {
  getSubmissions,
  completeSubmission as apiCompleteSubmission,
  saveFormData,
  startFilling,
} from '@/api/insp/submission'

export function useTaskExecution() {
  // ==================== State ====================

  const currentTask = ref<InspTask | null>(null)
  const submissions = ref<InspSubmission[]>([])
  const currentSubmissionIndex = ref(0)
  const progress = ref(0)
  const isLoading = ref(false)

  // ==================== Computed ====================

  const currentSubmission = computed<InspSubmission | null>(
    () => submissions.value[currentSubmissionIndex.value] ?? null,
  )

  const totalTargets = computed(() => submissions.value.length)

  const completedTargets = computed(
    () => submissions.value.filter(s => s.status === 'COMPLETED').length,
  )

  // ==================== Task Lifecycle ====================

  /** Load a task and its submissions */
  async function loadTask(taskId: number) {
    isLoading.value = true
    try {
      currentTask.value = await getTask(taskId)
      submissions.value = await getSubmissions({ taskId })
      currentSubmissionIndex.value = 0
      updateProgress()
    } finally {
      isLoading.value = false
    }
  }

  /** Claim an available task for the current user */
  async function claimTask(taskId: number) {
    isLoading.value = true
    try {
      // inspectorName will be filled by the backend from the current user context
      currentTask.value = await apiClaimTask(taskId, { inspectorName: '' })
    } finally {
      isLoading.value = false
    }
  }

  /** Start executing a claimed task */
  async function startTask(taskId: number) {
    isLoading.value = true
    try {
      currentTask.value = await apiStartTask(taskId)
    } finally {
      isLoading.value = false
    }
  }

  /** Submit the entire task for review after all submissions are done */
  async function submitTask(taskId: number) {
    isLoading.value = true
    try {
      currentTask.value = await apiSubmitTask(taskId)
    } finally {
      isLoading.value = false
    }
  }

  // ==================== Submission Navigation ====================

  /** Navigate to a specific submission by index */
  function navigateToSubmission(index: number) {
    if (index >= 0 && index < submissions.value.length) {
      currentSubmissionIndex.value = index
    }
  }

  /** Move to the next submission */
  function nextSubmission() {
    if (currentSubmissionIndex.value < submissions.value.length - 1) {
      currentSubmissionIndex.value++
    }
  }

  /** Move to the previous submission */
  function previousSubmission() {
    if (currentSubmissionIndex.value > 0) {
      currentSubmissionIndex.value--
    }
  }

  // ==================== Submission Completion ====================

  /**
   * Complete a single submission with form data and scoring results.
   * This saves the form data first, then marks the submission as complete.
   */
  async function completeSubmission(submissionId: number, formData: any) {
    isLoading.value = true
    try {
      // Save form data
      const serialized = typeof formData === 'string' ? formData : JSON.stringify(formData)
      await saveFormData(submissionId, { formData: serialized })

      // Complete with scoring breakdown
      const completeData: CompleteSubmissionRequest = {
        baseScore: formData.baseScore ?? 0,
        finalScore: formData.finalScore ?? 0,
        deductionTotal: formData.deductionTotal ?? 0,
        bonusTotal: formData.bonusTotal ?? 0,
        scoreBreakdown: formData.scoreBreakdown ?? '{}',
        grade: formData.grade ?? '',
        passed: formData.passed ?? true,
      }
      const updated = await apiCompleteSubmission(submissionId, completeData)

      // Update local state
      const idx = submissions.value.findIndex(s => s.id === submissionId)
      if (idx >= 0) {
        submissions.value[idx] = updated
      }

      updateProgress()
      return updated
    } finally {
      isLoading.value = false
    }
  }

  // ==================== Progress ====================

  /** Recalculate progress percentage */
  function updateProgress() {
    const total = submissions.value.length
    if (total === 0) {
      progress.value = 0
      return
    }
    const done = submissions.value.filter(
      s => s.status === 'COMPLETED' || s.status === 'SKIPPED',
    ).length
    progress.value = Math.round((done / total) * 100)
  }

  return {
    // State
    currentTask,
    submissions,
    currentSubmission,
    currentSubmissionIndex,
    totalTargets,
    completedTargets,
    progress,
    isLoading,
    // Task lifecycle
    loadTask,
    claimTask,
    startTask,
    submitTask,
    // Submission navigation
    navigateToSubmission,
    nextSubmission,
    previousSubmission,
    // Submission completion
    completeSubmission,
  }
}
