/**
 * 量化检查状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 检查模板接口
export interface CheckTemplate {
  id: number
  templateName: string
  templateType: string
  description?: string
  baseScore: number
  minScore: number
  items: CheckTemplateItem[]
}

// 模板检查项接口
export interface CheckTemplateItem {
  id: number
  templateId: number
  itemName: string
  itemCode: string
  deductMode: 'FIXED_DEDUCT' | 'PER_PERSON_DEDUCT' | 'SCORE_RANGE'
  baseDeductScore: number
  maxDeductScore?: number
  scoreRanges?: ScoreRange[]
  description?: string
  sortOrder: number
}

// 分数范围接口
export interface ScoreRange {
  minValue: number
  maxValue: number
  score: number
  label: string
}

// 每日检查记录接口
export interface DailyCheck {
  id: number
  checkDate: string
  templateId: number
  templateName: string
  targetId: number
  targetName: string
  targetType: string
  checkerId: number
  checkerName: string
  baseScore: number
  deductScore: number
  finalScore: number
  status: 'DRAFT' | 'SUBMITTED' | 'REVIEWED' | 'APPEALED'
  remark?: string
  details: CheckDetail[]
  createdAt: string
}

// 检查详情接口
export interface CheckDetail {
  id: number
  dailyCheckId: number
  itemId: number
  itemName: string
  deductScore: number
  personCount?: number
  remark?: string
  imageUrls?: string[]
}

// 申诉记录接口
export interface CheckAppeal {
  id: number
  dailyCheckId: number
  checkDate: string
  targetName: string
  appealReason: string
  appealImages?: string[]
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  replyContent?: string
  createdAt: string
  repliedAt?: string
}

export const useCheckStore = defineStore('check', () => {
  // 状态
  const templates = ref<CheckTemplate[]>([])
  const currentTemplate = ref<CheckTemplate | null>(null)
  const todayChecks = ref<DailyCheck[]>([])
  const myAppeals = ref<CheckAppeal[]>([])
  const checkDraft = ref<Partial<DailyCheck> | null>(null)

  // 计算属性
  const hasTemplate = computed(() => templates.value.length > 0)
  const pendingAppeals = computed(() => myAppeals.value.filter(a => a.status === 'PENDING'))

  // 操作
  function setTemplates(list: CheckTemplate[]) {
    templates.value = list
  }

  function setCurrentTemplate(template: CheckTemplate | null) {
    currentTemplate.value = template
  }

  function setTodayChecks(list: DailyCheck[]) {
    todayChecks.value = list
  }

  function addTodayCheck(check: DailyCheck) {
    todayChecks.value.unshift(check)
  }

  function updateTodayCheck(check: DailyCheck) {
    const index = todayChecks.value.findIndex(c => c.id === check.id)
    if (index !== -1) {
      todayChecks.value[index] = check
    }
  }

  function setMyAppeals(list: CheckAppeal[]) {
    myAppeals.value = list
  }

  function saveDraft(draft: Partial<DailyCheck>) {
    checkDraft.value = draft
    uni.setStorageSync('check_draft', draft)
  }

  function loadDraft(): Partial<DailyCheck> | null {
    if (!checkDraft.value) {
      checkDraft.value = uni.getStorageSync('check_draft') || null
    }
    return checkDraft.value
  }

  function clearDraft() {
    checkDraft.value = null
    uni.removeStorageSync('check_draft')
  }

  // 计算扣分
  function calculateDeduction(
    item: CheckTemplateItem,
    value: number | { personCount?: number; rangeIndex?: number }
  ): number {
    switch (item.deductMode) {
      case 'FIXED_DEDUCT':
        return value ? item.baseDeductScore : 0
      case 'PER_PERSON_DEDUCT':
        const count = typeof value === 'object' ? (value.personCount || 0) : value
        const deduct = count * item.baseDeductScore
        return item.maxDeductScore ? Math.min(deduct, item.maxDeductScore) : deduct
      case 'SCORE_RANGE':
        if (typeof value === 'object' && value.rangeIndex !== undefined && item.scoreRanges) {
          const range = item.scoreRanges[value.rangeIndex]
          return range ? range.score : 0
        }
        return 0
      default:
        return 0
    }
  }

  // 重置状态
  function reset() {
    templates.value = []
    currentTemplate.value = null
    todayChecks.value = []
    myAppeals.value = []
    checkDraft.value = null
  }

  return {
    // 状态
    templates,
    currentTemplate,
    todayChecks,
    myAppeals,
    checkDraft,
    // 计算属性
    hasTemplate,
    pendingAppeals,
    // 操作
    setTemplates,
    setCurrentTemplate,
    setTodayChecks,
    addTodayCheck,
    updateTodayCheck,
    setMyAppeals,
    saveDraft,
    loadDraft,
    clearDraft,
    calculateDeduction,
    reset
  }
})
