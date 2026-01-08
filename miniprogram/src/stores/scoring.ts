/**
 * 打分功能状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getScoringInitData, saveScoring } from '@/api/scoring'
import type {
  ScoringInitResponse,
  CategoryInfo,
  TargetClassInfo,
  DeductionItemInfo,
  LinkResourceInfo,
  ScoringDetailInfo,
  ScoreChange,
  ScoringDraft,
  DeductionMode,
  LinkType,
  ScoreRangeItem
} from '@/types/scoring'

// 生成打分记录的唯一key
function generateScoreKey(
  classId: string | number,
  categoryId: string | number,
  itemId: string | number,
  linkId: string | number,
  round: number
): string {
  return `${classId}_${categoryId}_${itemId}_${linkId}_${round}`
}

export const useScoringStore = defineStore('scoring', () => {
  // ========== 状态 ==========

  // 当前检查信息
  const currentCheckId = ref<string | number>('')
  const currentCheckName = ref<string>('')
  const currentCheckDate = ref<string>('')
  const currentCheckerId = ref<string | number>('')
  const currentCheckerName = ref<string>('')

  // 目标班级列表
  const targetClasses = ref<TargetClassInfo[]>([])

  // 检查类别列表
  const categories = ref<CategoryInfo[]>([])

  // 关联资源 (key: categoryId)
  const linkResources = ref<Record<string, LinkResourceInfo>>({})

  // 已有的打分明细
  const existingDetails = ref<ScoringDetailInfo[]>([])

  // 当前选中状态
  const activeCategoryId = ref<string | number>('')
  const activeClassId = ref<string | number>('')
  const currentRound = ref<number>(1)

  // 分数记录 (key: classId_categoryId_itemId_linkId_round)
  const scores = ref<Map<string, ScoreChange>>(new Map())

  // 待同步的变更
  const pendingChanges = ref<ScoreChange[]>([])

  // 同步状态
  const isSyncing = ref<boolean>(false)
  const lastSyncTime = ref<number | null>(null)

  // 筛选状态 - 班级
  const filterGrade = ref<string>('')
  const filterDepartment = ref<string>('')

  // 筛选状态 - 关联资源(宿舍/教室)
  const filterLinkGrade = ref<string>('')
  const filterLinkDepartment = ref<string>('')
  const filterLinkClass = ref<string>('')
  const filterLinkBuilding = ref<string>('')

  // ========== 计算属性 ==========

  // 当前选中的类别
  const activeCategory = computed<CategoryInfo | null>(() => {
    return categories.value.find(c => String(c.categoryId) === String(activeCategoryId.value)) || null
  })

  // 当前类别的扣分项
  const currentDeductionItems = computed<DeductionItemInfo[]>(() => {
    return activeCategory.value?.deductionItems || []
  })

  // 当前类别的关联资源
  const currentLinkResource = computed<LinkResourceInfo | null>(() => {
    if (!activeCategoryId.value) return null
    return linkResources.value[String(activeCategoryId.value)] || null
  })

  // 可用年级列表
  const gradeOptions = computed(() => {
    const grades = new Map<string, string>()
    targetClasses.value.forEach(c => {
      if (c.gradeId && c.gradeName) {
        grades.set(String(c.gradeId), c.gradeName)
      }
    })
    return Array.from(grades.entries()).map(([id, name]) => ({ id, name }))
  })

  // 可用部门列表
  const departmentOptions = computed(() => {
    const depts = new Map<string, string>()
    targetClasses.value.forEach(c => {
      if (c.departmentId && c.departmentName) {
        depts.set(String(c.departmentId), c.departmentName)
      }
    })
    return Array.from(depts.entries()).map(([id, name]) => ({ id, name }))
  })

  // 筛选后的班级列表
  const filteredClasses = computed(() => {
    let result = targetClasses.value
    if (filterGrade.value) {
      result = result.filter(c => String(c.gradeId) === filterGrade.value)
    }
    if (filterDepartment.value) {
      result = result.filter(c => String(c.departmentId) === filterDepartment.value)
    }
    return result
  })

  // 是否有未同步的变更
  const hasUnsyncedChanges = computed(() => pendingChanges.value.length > 0)

  // ========== 方法 ==========

  /**
   * 初始化打分数据
   */
  async function initScoring(checkId: string | number): Promise<boolean> {
    try {
      // 先尝试加载本地草稿
      loadDraft(checkId)

      const data = await getScoringInitData(checkId)

      currentCheckId.value = data.checkId
      currentCheckName.value = data.checkName
      currentCheckDate.value = data.checkDate
      currentCheckerId.value = data.checkerId || ''
      currentCheckerName.value = data.checkerName || ''
      targetClasses.value = data.targetClasses || []
      categories.value = data.categories || []
      linkResources.value = data.linkResources || {}
      existingDetails.value = data.existingDetails || []

      // 设置默认选中
      if (categories.value.length > 0 && !activeCategoryId.value) {
        activeCategoryId.value = categories.value[0].categoryId
      }
      if (targetClasses.value.length > 0 && !activeClassId.value) {
        activeClassId.value = targetClasses.value[0].classId
      }

      // 将已有明细转换为scores map
      existingDetails.value.forEach(detail => {
        const key = generateScoreKey(
          detail.classId,
          detail.categoryId,
          detail.deductionItemId,
          detail.linkId,
          detail.checkRound
        )
        scores.value.set(key, {
          targetKey: key,
          categoryId: detail.categoryId,
          classId: detail.classId,
          deductionItemId: detail.deductionItemId,
          linkType: detail.linkType,
          linkId: detail.linkId,
          checkRound: detail.checkRound,
          deductScore: detail.deductScore,
          personCount: detail.personCount,
          inputValue: detail.personCount || (detail.deductScore > 0 ? 1 : 0),
          timestamp: Date.now()
        })
      })

      return true
    } catch (error) {
      console.error('初始化打分数据失败:', error)
      return false
    }
  }

  /**
   * 计算扣分
   */
  function calculateDeduction(item: DeductionItemInfo, inputValue: number): number {
    const mode = item.deductMode as DeductionMode

    switch (mode) {
      case 1: // FIXED - 固定扣分
        return inputValue > 0 ? (item.fixedScore || 0) : 0

      case 2: // PER_PERSON - 按人数扣分
        return inputValue * (item.perPersonScore || item.baseScore || 0)

      case 3: // RANGE - 范围扣分
        if (item.rangeConfig) {
          try {
            const ranges: ScoreRangeItem[] = JSON.parse(item.rangeConfig)
            // inputValue在此作为选择的索引
            if (inputValue >= 0 && inputValue < ranges.length) {
              return ranges[inputValue].score
            }
          } catch (e) {
            console.error('解析范围配置失败:', e)
          }
        }
        return 0

      default:
        return 0
    }
  }

  /**
   * 更新分数
   */
  function updateScore(
    classId: string | number,
    categoryId: string | number,
    itemId: string | number,
    linkType: LinkType,
    linkId: string | number,
    inputValue: number,
    item: DeductionItemInfo
  ) {
    const round = currentRound.value
    const key = generateScoreKey(classId, categoryId, itemId, linkId, round)
    const deductScore = calculateDeduction(item, inputValue)

    const change: ScoreChange = {
      targetKey: key,
      categoryId,
      classId,
      deductionItemId: itemId,
      linkType,
      linkId,
      checkRound: round,
      deductScore,
      personCount: item.deductMode === 2 ? inputValue : undefined,
      inputValue,
      timestamp: Date.now()
    }

    scores.value.set(key, change)

    // 添加到待同步队列
    const existingIndex = pendingChanges.value.findIndex(c => c.targetKey === key)
    if (existingIndex >= 0) {
      pendingChanges.value[existingIndex] = change
    } else {
      pendingChanges.value.push(change)
    }

    // 保存草稿
    saveDraft()
  }

  /**
   * 获取分数
   */
  function getScore(
    classId: string | number,
    categoryId: string | number,
    itemId: string | number,
    linkId: string | number,
    round?: number
  ): ScoreChange | undefined {
    const key = generateScoreKey(classId, categoryId, itemId, linkId, round || currentRound.value)
    return scores.value.get(key)
  }

  /**
   * 获取目标的总扣分
   */
  function getTargetTotalDeduction(
    classId: string | number,
    categoryId?: string | number,
    linkId?: string | number
  ): number {
    let total = 0
    scores.value.forEach((score) => {
      if (String(score.classId) !== String(classId)) return
      if (categoryId && String(score.categoryId) !== String(categoryId)) return
      if (linkId && String(score.linkId) !== String(linkId)) return
      total += score.deductScore
    })
    return total
  }

  /**
   * 同步待变更到服务器
   */
  async function syncPendingChanges(): Promise<boolean> {
    if (pendingChanges.value.length === 0 || isSyncing.value) {
      return true
    }

    isSyncing.value = true

    try {
      // 构建保存请求
      const details = Array.from(scores.value.values()).map(score => ({
        checkRound: score.checkRound,
        categoryId: score.categoryId,
        classId: score.classId,
        deductionItemId: score.deductionItemId,
        linkType: score.linkType,
        linkId: score.linkId,
        deductScore: score.deductScore,
        personCount: score.personCount
      }))

      await saveScoring(currentCheckId.value, {
        checkId: currentCheckId.value,
        checkerId: currentCheckerId.value,
        checkerName: currentCheckerName.value,
        details
      })

      // 清空待同步队列
      pendingChanges.value = []
      lastSyncTime.value = Date.now()

      // 更新草稿
      saveDraft()

      return true
    } catch (error) {
      console.error('同步分数失败:', error)
      return false
    } finally {
      isSyncing.value = false
    }
  }

  /**
   * 保存草稿到本地
   */
  function saveDraft() {
    if (!currentCheckId.value) return

    const draft: ScoringDraft = {
      checkId: currentCheckId.value,
      scores: Array.from(scores.value.values()),
      pendingChanges: pendingChanges.value,
      timestamp: Date.now()
    }

    try {
      uni.setStorageSync(`scoring_draft_${currentCheckId.value}`, JSON.stringify(draft))
    } catch (e) {
      console.error('保存草稿失败:', e)
    }
  }

  /**
   * 加载本地草稿
   */
  function loadDraft(checkId: string | number) {
    try {
      const data = uni.getStorageSync(`scoring_draft_${checkId}`)
      if (data) {
        const draft: ScoringDraft = JSON.parse(data)

        // 恢复分数记录
        draft.scores.forEach(score => {
          scores.value.set(score.targetKey, score)
        })

        // 恢复待同步队列
        pendingChanges.value = draft.pendingChanges || []
      }
    } catch (e) {
      console.error('加载草稿失败:', e)
    }
  }

  /**
   * 清除草稿
   */
  function clearDraft() {
    if (!currentCheckId.value) return
    uni.removeStorageSync(`scoring_draft_${currentCheckId.value}`)
  }

  /**
   * 重置状态
   */
  function reset() {
    currentCheckId.value = ''
    currentCheckName.value = ''
    currentCheckDate.value = ''
    currentCheckerId.value = ''
    currentCheckerName.value = ''
    targetClasses.value = []
    categories.value = []
    linkResources.value = {}
    existingDetails.value = []
    activeCategoryId.value = ''
    activeClassId.value = ''
    currentRound.value = 1
    scores.value.clear()
    pendingChanges.value = []
    isSyncing.value = false
    lastSyncTime.value = null
    filterGrade.value = ''
    filterDepartment.value = ''
    filterLinkGrade.value = ''
    filterLinkDepartment.value = ''
    filterLinkClass.value = ''
    filterLinkBuilding.value = ''
  }

  return {
    // 状态
    currentCheckId,
    currentCheckName,
    currentCheckDate,
    currentCheckerId,
    currentCheckerName,
    targetClasses,
    categories,
    linkResources,
    existingDetails,
    activeCategoryId,
    activeClassId,
    currentRound,
    scores,
    pendingChanges,
    isSyncing,
    lastSyncTime,
    filterGrade,
    filterDepartment,
    filterLinkGrade,
    filterLinkDepartment,
    filterLinkClass,
    filterLinkBuilding,

    // 计算属性
    activeCategory,
    currentDeductionItems,
    currentLinkResource,
    gradeOptions,
    departmentOptions,
    filteredClasses,
    hasUnsyncedChanges,

    // 方法
    initScoring,
    calculateDeduction,
    updateScore,
    getScore,
    getTargetTotalDeduction,
    syncPendingChanges,
    saveDraft,
    loadDraft,
    clearDraft,
    reset
  }
})
