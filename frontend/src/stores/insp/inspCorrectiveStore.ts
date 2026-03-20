/**
 * V7 检查平台 - 整改管理 Store
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CorrectiveCase, CreateCaseRequest, AssignCaseRequest, SubmitCorrectionRequest, VerifyCaseRequest, RejectCaseRequest } from '@/types/insp/corrective'
import type { CaseStatus } from '@/types/insp/enums'
import { inspCorrectiveCaseApi } from '@/api/insp/correctiveCase'

export const useInspCorrectiveStore = defineStore('inspCorrective', () => {
  const cases = ref<CorrectiveCase[]>([])
  const currentCase = ref<CorrectiveCase | null>(null)
  const myCases = ref<CorrectiveCase[]>([])
  const overdueCases = ref<CorrectiveCase[]>([])
  const loading = ref(false)

  // ========== CRUD ==========

  async function fetchCases(params?: { projectId?: number; submissionId?: number; taskId?: number; status?: CaseStatus }) {
    loading.value = true
    try {
      cases.value = await inspCorrectiveCaseApi.getCases(params)
    } finally {
      loading.value = false
    }
  }

  async function fetchCase(id: number) {
    loading.value = true
    try {
      currentCase.value = await inspCorrectiveCaseApi.getCase(id)
    } finally {
      loading.value = false
    }
  }

  async function addCase(data: CreateCaseRequest) {
    const created = await inspCorrectiveCaseApi.createCase(data)
    cases.value.unshift(created)
    return created
  }

  async function fetchMyCases() {
    loading.value = true
    try {
      myCases.value = await inspCorrectiveCaseApi.getMyCases()
    } finally {
      loading.value = false
    }
  }

  async function fetchOverdueCases() {
    loading.value = true
    try {
      overdueCases.value = await inspCorrectiveCaseApi.getOverdueCases()
    } finally {
      loading.value = false
    }
  }

  async function removeCase(id: number) {
    await inspCorrectiveCaseApi.deleteCase(id)
    cases.value = cases.value.filter(c => c.id !== id)
  }

  // ========== Lifecycle ==========

  async function assignCase(id: number, data: AssignCaseRequest) {
    const updated = await inspCorrectiveCaseApi.assignCase(id, data)
    updateInList(updated)
    return updated
  }

  async function startWork(id: number) {
    const updated = await inspCorrectiveCaseApi.startWork(id)
    updateInList(updated)
    return updated
  }

  async function submitCorrection(id: number, data: SubmitCorrectionRequest) {
    const updated = await inspCorrectiveCaseApi.submitCorrection(id, data)
    updateInList(updated)
    return updated
  }

  async function verifyCase(id: number, data: VerifyCaseRequest) {
    const updated = await inspCorrectiveCaseApi.verifyCase(id, data)
    updateInList(updated)
    return updated
  }

  async function rejectCase(id: number, data: RejectCaseRequest) {
    const updated = await inspCorrectiveCaseApi.rejectCase(id, data)
    updateInList(updated)
    return updated
  }

  async function closeCase(id: number) {
    const updated = await inspCorrectiveCaseApi.closeCase(id)
    updateInList(updated)
    return updated
  }

  async function escalateCase(id: number) {
    const updated = await inspCorrectiveCaseApi.escalateCase(id)
    updateInList(updated)
    return updated
  }

  function updateInList(updated: CorrectiveCase) {
    const idx = cases.value.findIndex(c => c.id === updated.id)
    if (idx !== -1) cases.value[idx] = updated
    if (currentCase.value?.id === updated.id) currentCase.value = updated
  }

  return {
    cases, currentCase, myCases, overdueCases, loading,
    fetchCases, fetchCase, addCase, fetchMyCases, fetchOverdueCases, removeCase,
    assignCase, startWork, submitCorrection, verifyCase, rejectCase, closeCase, escalateCase,
  }
})
